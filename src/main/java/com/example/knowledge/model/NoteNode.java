package com.example.knowledge.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteNode {
    private final Note note;
    private final List<NoteNode> children = new ArrayList<>();

    public NoteNode(Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }

    public List<NoteNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(NoteNode child) {
        if (child == null) return;
        children.add(child);
    }
}
