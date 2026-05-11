package com.example.knowledge.service;

import com.example.knowledge.model.Note;
import com.example.knowledge.model.NoteNode;
import com.example.knowledge.repository.NoteLinkRepository;
import com.example.knowledge.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteLinkRepository noteLinkRepository;

    @InjectMocks
    private NoteService noteService;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // default setup if needed
    }

    @Test
    void createNote_successfulCreation() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Note toCreate = new Note();
        toCreate.setUserId(userId);
        toCreate.setTitle("Title");
        toCreate.setContent("Content");
        toCreate.setParentId(null);
        toCreate.setCreatedAt(now);
        toCreate.setUpdatedAt(now);

        Note created = new Note(100L, userId, "Title", "Content", null, now, now);

        when(noteRepository.create(any(Note.class))).thenReturn(created);

        Note result = noteService.createNote(userId, "Title", "Content", null);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(noteRepository, times(1)).create(any(Note.class));
    }

    @Test
    void createNote_parentMustBelongToUser() throws Exception {
        Long parentId = 50L;
        Note parent = new Note(parentId, 2L, "Parent", "", null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        when(noteRepository.findById(parentId)).thenReturn(Optional.of(parent));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                noteService.createNote(userId, "Child", "", parentId));
        assertEquals("User does not own this note", ex.getMessage());
        verify(noteRepository, times(1)).findById(parentId);
        verify(noteRepository, never()).create(any());
    }

    @Test
    void createNote_emptyTitleValidation() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                noteService.createNote(userId, "   ", "content", null));
        assertEquals("title required", ex.getMessage());
        verifyNoInteractions(noteRepository);
    }

    @Test
    void ownershipCheck_throwsWhenNotOwner() throws Exception {
        Long noteId = 10L;
        Note note = new Note(noteId, 2L, "T", "C", null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                noteService.getNoteById(userId, noteId));
        assertEquals("User does not own this note", ex.getMessage());
        verify(noteRepository, times(1)).findById(noteId);
    }

    @Test
    void buildNoteTree_buildsHierarchy_noNPlusOne() throws Exception {
        Note root = new Note(1L, userId, "Root", "r", null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        Note child1 = new Note(2L, userId, "Child1", "c1", 1L, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        Note child2 = new Note(3L, userId, "Child2", "c2", 1L, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));

        when(noteRepository.findByUserId(userId)).thenReturn(Arrays.asList(root, child1, child2));

        List<NoteNode> roots = noteService.buildNoteTree(userId);

        assertNotNull(roots);
        assertEquals(1, roots.size());
        NoteNode rootNode = roots.get(0);
        assertEquals(2, rootNode.getChildren().size());

        verify(noteRepository, times(1)).findByUserId(userId);
        // Ensure no additional findById calls (no N+1)
        verify(noteRepository, never()).findById(anyLong());
    }

    @Test
    void linkNotes_checksOwnershipAndCreatesLink() throws Exception {
        Long fromId = 1L;
        Long toId = 2L;
        Note from = new Note(fromId, userId, "From", "", null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        Note to = new Note(toId, userId, "To", "", null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));

        when(noteRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(noteRepository.findById(toId)).thenReturn(Optional.of(to));

        noteService.linkNotes(userId, fromId, toId);

        verify(noteRepository, times(1)).findById(fromId);
        verify(noteRepository, times(1)).findById(toId);
        verify(noteLinkRepository, times(1)).createLink(fromId, toId);
    }

    @Test
    void linkNotes_throwsWhenNotOwner() throws Exception {
        Long fromId = 1L;
        Long toId = 2L;
        Note from = new Note(fromId, userId, "From", "", null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        Note to = new Note(toId, 99L, "To", "", null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));

        when(noteRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(noteRepository.findById(toId)).thenReturn(Optional.of(to));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                noteService.linkNotes(userId, fromId, toId));
        assertEquals("User does not own this note", ex.getMessage());
        verify(noteLinkRepository, never()).createLink(anyLong(), anyLong());
    }
}
