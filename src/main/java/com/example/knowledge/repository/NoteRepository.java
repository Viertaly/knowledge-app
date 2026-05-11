package com.example.knowledge.repository;

import com.example.knowledge.config.DatabaseConfig;
import com.example.knowledge.model.Note;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteRepository {

    public Note create(Note note) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, parent_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            if (note.getParentId() == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setLong(4, note.getParentId());
            }
            ps.setTimestamp(5, note.getCreatedAt() == null ? new Timestamp(System.currentTimeMillis()) : Timestamp.from(note.getCreatedAt().toInstant()));
            ps.setTimestamp(6, note.getUpdatedAt() == null ? new Timestamp(System.currentTimeMillis()) : Timestamp.from(note.getUpdatedAt().toInstant()));

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Creating note failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    note.setId(id);
                    // fetch timestamps and other fields
                    String sel = "SELECT created_at, updated_at FROM notes WHERE id = ?";
                    try (PreparedStatement ps2 = c.prepareStatement(sel)) {
                        ps2.setLong(1, id);
                        try (ResultSet rs = ps2.executeQuery()) {
                            if (rs.next()) {
                                Timestamp created = rs.getTimestamp("created_at");
                                Timestamp updated = rs.getTimestamp("updated_at");
                                if (created != null) note.setCreatedAt(created.toInstant().atOffset(ZoneOffset.UTC));
                                if (updated != null) note.setUpdatedAt(updated.toInstant().atOffset(ZoneOffset.UTC));
                            }
                        }
                    }
                    return note;
                } else {
                    throw new SQLException("Creating note failed, no ID obtained.");
                }
            }
        }
    }

    public void update(Note note) throws SQLException {
        if (note.getId() == null) throw new IllegalArgumentException("Note id is required for update");
        String sql = "UPDATE notes SET title = ?, content = ?, parent_id = ?, updated_at = ? WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            if (note.getParentId() == null) ps.setNull(3, java.sql.Types.INTEGER);
            else ps.setLong(3, note.getParentId());
            ps.setTimestamp(4, note.getUpdatedAt() == null ? new Timestamp(System.currentTimeMillis()) : Timestamp.from(note.getUpdatedAt().toInstant()));
            ps.setLong(5, note.getId());
            ps.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Note> findById(Long id) throws SQLException {
        String sql = "SELECT id, user_id, title, content, parent_id, created_at, updated_at FROM notes WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    public List<Note> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT id, user_id, title, content, parent_id, created_at, updated_at FROM notes WHERE user_id = ? ORDER BY created_at";
        List<Note> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Note> findByParentId(Long parentId) throws SQLException {
        String sql = "SELECT id, user_id, title, content, parent_id, created_at, updated_at FROM notes WHERE parent_id = ? ORDER BY created_at";
        List<Note> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Note> searchByTitleOrContent(Long userId, String query) throws SQLException {
        String sql = "SELECT id, user_id, title, content, parent_id, created_at, updated_at FROM notes " +
            "WHERE user_id = ? AND (title LIKE ? OR content LIKE ?) ORDER BY updated_at DESC";
        List<Note> list = new ArrayList<>();
        String like = "%" + query + "%";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        long parent = rs.getLong("parent_id");
        if (!rs.wasNull()) n.setParentId(parent);
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) n.setCreatedAt(created.toInstant().atOffset(ZoneOffset.UTC));
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) n.setUpdatedAt(updated.toInstant().atOffset(ZoneOffset.UTC));
        return n;
    }
}
