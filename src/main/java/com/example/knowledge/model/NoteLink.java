package com.example.knowledge.model;

import java.util.Objects;

public class NoteLink {
    private Long id;
    private Long fromNoteId;
    private Long toNoteId;

    public NoteLink() { }

    public NoteLink(Long id, Long fromNoteId, Long toNoteId) {
        this.id = id;
        this.fromNoteId = fromNoteId;
        this.toNoteId = toNoteId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromNoteId() {
        return fromNoteId;
    }

    public void setFromNoteId(Long fromNoteId) {
        this.fromNoteId = fromNoteId;
    }

    public Long getToNoteId() {
        return toNoteId;
    }

    public void setToNoteId(Long toNoteId) {
        this.toNoteId = toNoteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoteLink)) return false;
        NoteLink noteLink = (NoteLink) o;
        return Objects.equals(id, noteLink.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NoteLink{" +
                "id=" + id +
                ", fromNoteId=" + fromNoteId +
                ", toNoteId=" + toNoteId +
                '}';
    }
}
