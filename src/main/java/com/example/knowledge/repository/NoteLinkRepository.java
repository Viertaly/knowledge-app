package com.example.knowledge.repository;

import com.example.knowledge.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NoteLinkRepository {

    public void createLink(Long fromNoteId, Long toNoteId) throws SQLException {
        // Use INSERT IGNORE to avoid duplicate links; requires unique constraint on (from_note_id, to_note_id)
        String sql = "INSERT IGNORE INTO note_links (from_note_id, to_note_id) VALUES (?, ?)";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, fromNoteId);
            ps.setLong(2, toNoteId);
            ps.executeUpdate();
        }
    }

    public void deleteLink(Long fromNoteId, Long toNoteId) throws SQLException {
        String sql = "DELETE FROM note_links WHERE from_note_id = ? AND to_note_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, fromNoteId);
            ps.setLong(2, toNoteId);
            ps.executeUpdate();
        }
    }

    public List<Long> findLinksFrom(Long noteId) throws SQLException {
        String sql = "SELECT to_note_id FROM note_links WHERE from_note_id = ?";
        List<Long> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getLong("to_note_id"));
            }
        }
        return list;
    }

    public List<Long> findLinksTo(Long noteId) throws SQLException {
        String sql = "SELECT from_note_id FROM note_links WHERE to_note_id = ?";
        List<Long> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getLong("from_note_id"));
            }
        }
        return list;
    }
}
