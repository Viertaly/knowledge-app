package com.example.knowledge.service;

import com.example.knowledge.model.Note;
import com.example.knowledge.model.NoteNode;
import com.example.knowledge.repository.NoteLinkRepository;
import com.example.knowledge.repository.NoteRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class NoteService {
    private final NoteRepository noteRepository;
    private final NoteLinkRepository noteLinkRepository;

    public NoteService() {
        this.noteRepository = new NoteRepository();
        this.noteLinkRepository = new NoteLinkRepository();
    }

    public NoteService(NoteRepository noteRepository, NoteLinkRepository noteLinkRepository) {
        this.noteRepository = Objects.requireNonNull(noteRepository, "noteRepository");
        this.noteLinkRepository = Objects.requireNonNull(noteLinkRepository, "noteLinkRepository");
    }

    public Note createNote(Long userId, String title, String content, Long parentId) throws Exception {
        validateUserId(userId);
        validateTitle(title);

        if (parentId != null) {
            Note parent = noteRepository.findById(parentId).orElseThrow(() -> new IllegalArgumentException("Parent note not found"));
            requireOwnership(userId, parent);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title.trim());
        note.setContent(content == null ? "" : content);
        note.setParentId(parentId);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        return noteRepository.create(note);
    }

    public void updateNote(Long userId, Note note) throws Exception {
        validateUserId(userId);
        if (note == null) throw new IllegalArgumentException("note is required");
        if (note.getId() == null) throw new IllegalArgumentException("note id is required");

        Note existing = noteRepository.findById(note.getId()).orElseThrow(() -> new IllegalArgumentException("Note not found"));
        requireOwnership(userId, existing);

        if (note.getParentId() != null) {
            if (note.getParentId().equals(note.getId())) throw new IllegalArgumentException("Note cannot be parent of itself");
            Note parent = noteRepository.findById(note.getParentId()).orElseThrow(() -> new IllegalArgumentException("Parent note not found"));
            requireOwnership(userId, parent);
        }

        existing.setTitle(note.getTitle() == null ? existing.getTitle() : note.getTitle().trim());
        existing.setContent(note.getContent() == null ? existing.getContent() : note.getContent());
        existing.setParentId(note.getParentId());
        existing.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        noteRepository.update(existing);
    }

    public void deleteNote(Long userId, Long noteId) throws Exception {
        validateUserId(userId);
        if (noteId == null) throw new IllegalArgumentException("noteId required");
        Note existing = noteRepository.findById(noteId).orElseThrow(() -> new IllegalArgumentException("Note not found"));
        requireOwnership(userId, existing);
        noteRepository.delete(noteId);
    }

    public Note getNoteById(Long userId, Long noteId) throws Exception {
        validateUserId(userId);
        if (noteId == null) throw new IllegalArgumentException("noteId required");
        Note note = noteRepository.findById(noteId).orElseThrow(() -> new IllegalArgumentException("Note not found"));
        requireOwnership(userId, note);
        return note;
    }

    public List<Note> getAllUserNotes(Long userId) throws Exception {
        validateUserId(userId);
        return noteRepository.findByUserId(userId);
    }

    public List<Note> search(Long userId, String query) throws Exception {
        validateUserId(userId);
        if (query == null || query.isBlank()) return getAllUserNotes(userId);
        return noteRepository.searchByTitleOrContent(userId, query);
    }

    public List<NoteNode> buildNoteTree(Long userId) throws Exception {
        validateUserId(userId);
        List<Note> notes = noteRepository.findByUserId(userId);
        Map<Long, NoteNode> map = new LinkedHashMap<>();
        for (Note n : notes) {
            map.put(n.getId(), new NoteNode(n));
        }

        List<NoteNode> roots = new ArrayList<>();
        for (NoteNode node : map.values()) {
            Long parentId = node.getNote().getParentId();
            if (parentId == null) {
                roots.add(node);
            } else {
                NoteNode parentNode = map.get(parentId);
                if (parentNode != null) {
                    parentNode.addChild(node);
                } else {
                    // Orphaned node (parent not present) treat as root
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    public void linkNotes(Long userId, Long fromNoteId, Long toNoteId) throws Exception {
        validateUserId(userId);
        if (fromNoteId == null || toNoteId == null) throw new IllegalArgumentException("fromNoteId and toNoteId required");
        if (fromNoteId.equals(toNoteId)) throw new IllegalArgumentException("Cannot link note to itself");

        Note from = noteRepository.findById(fromNoteId).orElseThrow(() -> new IllegalArgumentException("From note not found"));
        Note to = noteRepository.findById(toNoteId).orElseThrow(() -> new IllegalArgumentException("To note not found"));
        requireOwnership(userId, from);
        requireOwnership(userId, to);

        noteLinkRepository.createLink(fromNoteId, toNoteId);
    }

    public void unlinkNotes(Long userId, Long fromNoteId, Long toNoteId) throws Exception {
        validateUserId(userId);
        if (fromNoteId == null || toNoteId == null) throw new IllegalArgumentException("fromNoteId and toNoteId required");

        Note from = noteRepository.findById(fromNoteId).orElseThrow(() -> new IllegalArgumentException("From note not found"));
        Note to = noteRepository.findById(toNoteId).orElseThrow(() -> new IllegalArgumentException("To note not found"));
        requireOwnership(userId, from);
        requireOwnership(userId, to);

        noteLinkRepository.deleteLink(fromNoteId, toNoteId);
    }

    public List<Note> getLinkedNotes(Long userId, Long noteId) throws Exception {
        validateUserId(userId);
        if (noteId == null) throw new IllegalArgumentException("noteId required");
        Note base = noteRepository.findById(noteId).orElseThrow(() -> new IllegalArgumentException("Note not found"));
        requireOwnership(userId, base);

        Set<Long> ids = new LinkedHashSet<>();
        ids.addAll(noteLinkRepository.findLinksFrom(noteId));
        ids.addAll(noteLinkRepository.findLinksTo(noteId));

        List<Note> result = new ArrayList<>();
        for (Long id : ids) {
            noteRepository.findById(id).ifPresent(n -> {
                if (Objects.equals(n.getUserId(), userId)) result.add(n);
            });
        }
        return result;
    }

    // helpers
    private void requireOwnership(Long userId, Note note) {
        if (!Objects.equals(note.getUserId(), userId)) throw new IllegalStateException("User does not own this note");
    }

    private void validateUserId(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId required");
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title required");
        if (title.length() > 255) throw new IllegalArgumentException("title too long (max 255)");
    }
}
