package com.example.knowledge.controller;

import com.example.knowledge.model.Note;
import com.example.knowledge.model.NoteNode;
import com.example.knowledge.model.User;
import com.example.knowledge.service.NoteService;
import javafx.scene.control.TextField;
import com.example.knowledge.service.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.HTMLEditor;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

import java.util.List;
import java.util.Objects;

public class MainController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private TreeView<Note> notesTree;
    @FXML
    private javafx.scene.control.TextField titleField;
    @FXML
    private HTMLEditor noteEditor;
    @FXML
    private TextField searchField;
    @FXML
    private Label createdAtLabel;
    @FXML
    private Label updatedAtLabel;
    @FXML
    private Label saveStatusLabel;
    @FXML
    private Button createButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button logoutButton;

    private NoteService noteService;

    // Autosave scheduler
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> pendingSave;
    private volatile String lastSavedContent = "";

    public MainController() {
        // noteService will be injected by LoginController; if not, create default
    }

    // Dependency injection via setter
    public void setNoteService(NoteService noteService) {
        this.noteService = noteService;
    }

    @FXML
    private void initialize() {
        User u = Session.getCurrentUser();
        if (u != null) {
            welcomeLabel.setText("Welcome, " + u.getUsername());
        } else {
            welcomeLabel.setText("Welcome");
        }

        // Ensure noteService exists
        if (this.noteService == null) {
            this.noteService = new NoteService();
        }

        // search listener: update tree on text change
        if (this.searchField != null) {
            this.searchField.textProperty().addListener((obs, oldText, newText) -> {
                try {
                    Long userId = getCurrentUserId();
                    if (userId == null) return;
                    if (newText == null || newText.isBlank()) {
                        refreshNotesTree();
                        return;
                    }
                    // perform search and render simple flat list of results
                    java.util.List<Note> results = noteService.search(userId, newText.trim());
                    Note rootNote = new Note();
                    rootNote.setId(null);
                    rootNote.setTitle("Search results");
                    javafx.scene.control.TreeItem<Note> rootItem = new javafx.scene.control.TreeItem<>(rootNote);
                    rootItem.setExpanded(true);
                    for (Note n : results) {
                        rootItem.getChildren().add(new javafx.scene.control.TreeItem<>(n));
                    }
                    notesTree.setRoot(rootItem);
                } catch (Exception e) {
                    // on error, fall back to full tree
                    refreshNotesTree();
                }
            });
        }

        loadNotesTree();

        // render title in tree cells
        notesTree.setCellFactory(tv -> new javafx.scene.control.TreeCell<Note>() {
            @Override
            public void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item.getTitle());
            }
        });

        // autosave listener with debounce (use key events because HTMLEditor lacks an htmlTextProperty)
        if (this.noteEditor != null) {
            javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyHandler = ev -> {
                try {
                    TreeItem<Note> sel = notesTree.getSelectionModel().getSelectedItem();
                    if (sel == null || sel.getValue() == null || sel.getValue().getId() == null) return;
                    String current = noteEditor.getHtmlText() == null ? "" : noteEditor.getHtmlText();
                    if (current.equals(lastSavedContent)) return;
                    if (pendingSave != null && !pendingSave.isDone()) pendingSave.cancel(false);
                    final long noteId = sel.getValue().getId();
                    final Long parentId = sel.getValue().getParentId();
                    final String titleSnapshot = titleField.getText();
                    final String contentSnapshot = current;
                    if (saveStatusLabel != null) {
                        Platform.runLater(() -> saveStatusLabel.setText("Saving..."));
                    }
                    pendingSave = scheduler.schedule(() -> {
                        try {
                            Long userId = getCurrentUserId();
                            if (userId == null) return;
                            Note note = new Note();
                            note.setId(noteId);
                            note.setTitle(titleSnapshot == null ? "" : titleSnapshot);
                            note.setContent(contentSnapshot);
                            note.setParentId(parentId);
                            noteService.updateNote(userId, note);
                            lastSavedContent = contentSnapshot;
                            if (saveStatusLabel != null) {
                                Platform.runLater(() -> saveStatusLabel.setText("Saved"));
                            }
                        } catch (Exception ignored) {
                            // ignore autosave errors
                        }
                    }, 1500, TimeUnit.MILLISECONDS);
                } catch (Exception ignored) {
                }
            };
            this.noteEditor.addEventHandler(javafx.scene.input.KeyEvent.KEY_RELEASED, keyHandler);

            // also save immediately when editor loses focus
            this.noteEditor.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    try {
                        if (pendingSave != null && !pendingSave.isDone()) {
                            pendingSave.cancel(false);
                            // run immediate save
                            TreeItem<Note> sel = notesTree.getSelectionModel().getSelectedItem();
                            if (sel == null || sel.getValue() == null || sel.getValue().getId() == null) return;
                            final long noteId = sel.getValue().getId();
                            final Long parentId = sel.getValue().getParentId();
                            final String titleSnapshot = titleField.getText();
                            final String contentSnapshot = noteEditor.getHtmlText() == null ? "" : noteEditor.getHtmlText();
                            try {
                                if (saveStatusLabel != null) {
                                    Platform.runLater(() -> saveStatusLabel.setText("Saving..."));
                                }
                                Long userId = getCurrentUserId();
                                if (userId == null) return;
                                Note note = new Note();
                                note.setId(noteId);
                                note.setTitle(titleSnapshot == null ? "" : titleSnapshot);
                                note.setContent(contentSnapshot);
                                note.setParentId(parentId);
                                noteService.updateNote(userId, note);
                                lastSavedContent = contentSnapshot;
                                if (saveStatusLabel != null) {
                                    Platform.runLater(() -> saveStatusLabel.setText("Saved"));
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        notesTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.getValue() == null || newVal.getValue().getId() == null) {
                titleField.clear();
                if (noteEditor != null) noteEditor.setHtmlText("");
                if (createdAtLabel != null) createdAtLabel.setText("—");
                if (updatedAtLabel != null) updatedAtLabel.setText("—");
                return;
            }
            try {
                Long userId = getCurrentUserId();
                Note n = noteService.getNoteById(userId, newVal.getValue().getId());
                titleField.setText(n.getTitle() == null ? "" : n.getTitle());
                if (noteEditor != null) noteEditor.setHtmlText(n.getContent() == null ? "" : n.getContent());
                // visual fade-in for content and title
                try {
                    if (noteEditor != null) {
                        noteEditor.setOpacity(0);
                        FadeTransition fade = new FadeTransition(Duration.millis(300), noteEditor);
                        fade.setFromValue(0);
                        fade.setToValue(1);
                        fade.play();
                    }
                    if (titleField != null) {
                        titleField.setOpacity(0);
                        FadeTransition fadeTitle = new FadeTransition(Duration.millis(200), titleField);
                        fadeTitle.setFromValue(0);
                        fadeTitle.setToValue(1);
                        fadeTitle.play();
                    }
                } catch (Exception ignored) {}
                // track last saved content to avoid immediate autosave
                lastSavedContent = n.getContent() == null ? "" : n.getContent();
                if (saveStatusLabel != null) saveStatusLabel.setText("Saved");
                if (createdAtLabel != null) {
                    if (n.getCreatedAt() == null) createdAtLabel.setText("—");
                    else createdAtLabel.setText(n.getCreatedAt().format(dtf));
                }
                if (updatedAtLabel != null) {
                    if (n.getUpdatedAt() == null) updatedAtLabel.setText("—");
                    else updatedAtLabel.setText(n.getUpdatedAt().format(dtf));
                }
            } catch (Exception e) {
                titleField.clear();
                if (noteEditor != null) {
                    noteEditor.setHtmlText("");
                    lastSavedContent = "";
                }
                if (saveStatusLabel != null) saveStatusLabel.setText("Saved");
                if (createdAtLabel != null) createdAtLabel.setText("—");
                if (updatedAtLabel != null) updatedAtLabel.setText("—");
            }
        });

        // UI polish: attach hover scale animations to main buttons
        try {
            addHoverScale(createButton);
            addHoverScale(updateButton);
            addHoverScale(deleteButton);
            addHoverScale(settingsButton);
            addHoverScale(logoutButton);
        } catch (Exception ignored) {}

        // Fade-in for the whole scene once it's ready
        Platform.runLater(() -> {
            try {
                var root = welcomeLabel.getScene().getRoot();
                if (root != null) {
                    root.setOpacity(0);
                    FadeTransition ft = new FadeTransition(Duration.millis(400), root);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();
                }
            } catch (Exception ignored) {}
        });

        // Apply HTMLEditor inner stylesheet to the editor's WebView to enforce dark content styles
        Platform.runLater(() -> {
            try {
                javafx.scene.web.WebView web = (javafx.scene.web.WebView) noteEditor.lookup(".web-view");
                if (web != null) {
                    String css = getClass().getResource("/css/htmleditor.css").toExternalForm();
                    web.getEngine().setUserStyleSheetLocation(css);
                }
            } catch (Exception ignored) {}
        });
    }

    private void addHoverScale(Button btn) {
        if (btn == null) return;
        final double scaleTo = 1.03;
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), btn);
            st.setToX(scaleTo);
            st.setToY(scaleTo);
            st.playFromStart();
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.playFromStart();
        });
    }

    private Long getCurrentUserId() {
        User u = Session.getCurrentUser();
        return u == null ? null : u.getId().longValue();
    }

    private void loadNotesTree() {
        Long userId = getCurrentUserId();
        if (userId == null) return;
        try {
            List<NoteNode> roots = noteService.buildNoteTree(userId);
            Note rootNote = new Note();
            rootNote.setId(null);
            rootNote.setTitle("Notes");
            TreeItem<Note> rootItem = new TreeItem<>(rootNote);
            rootItem.setExpanded(true);

            for (NoteNode node : roots) {
                rootItem.getChildren().add(buildTreeItem(node));
            }
            notesTree.setRoot(rootItem);
        } catch (Exception e) {
            // on error, set empty root to avoid null pointers
            Note rootNote = new Note();
            rootNote.setId(null);
            rootNote.setTitle("Notes");
            TreeItem<Note> rootItem = new TreeItem<>(rootNote);
            notesTree.setRoot(rootItem);
        }
    }

    private TreeItem<Note> buildTreeItem(NoteNode node) {
        Objects.requireNonNull(node, "node required");
        Note n = node.getNote();
        TreeItem<Note> item = new TreeItem<>(n);
        for (NoteNode child : node.getChildren()) {
            item.getChildren().add(buildTreeItem(child));
        }
        return item;
    }

    public void refreshNotesTree() {
        loadNotesTree();
    }

    @FXML
    private void onCreateNote() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            showError("User required", "No user session");
            return;
        }
        String title = titleField.getText();
        String content = noteEditor == null ? "" : noteEditor.getHtmlText();
        TreeItem<Note> selected = notesTree.getSelectionModel().getSelectedItem();
        Long parentId = null;
        if (selected != null && selected.getValue() != null && selected.getValue().getId() != null) {
            parentId = selected.getValue().getId();
        }
        try {
            noteService.createNote(userId, title, content, parentId);
            refreshNotesTree();
        } catch (Exception e) {
            showError("Create failed", e.getMessage());
        }
    }

    @FXML
    private void onUpdateNote() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            showError("User required", "No user session");
            return;
        }
        TreeItem<Note> selected = notesTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().getId() == null) {
            showError("Update failed", "No note selected");
            return;
        }
        Note note = new Note();
        note.setId(selected.getValue().getId());
        note.setTitle(titleField.getText());
        note.setContent(noteEditor == null ? "" : noteEditor.getHtmlText());
        note.setParentId(selected.getValue().getParentId());
        try {
            noteService.updateNote(userId, note);
            refreshNotesTree();
        } catch (Exception e) {
            showError("Update failed", e.getMessage());
        }
    }

    @FXML
    private void onDeleteNote() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            showError("User required", "No user session");
            return;
        }
        TreeItem<Note> selected = notesTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().getId() == null) {
            showError("Delete failed", "No note selected");
            return;
        }
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm deletion");
        confirm.setHeaderText("Delete note?");
        confirm.setContentText("Are you sure you want to delete this note?");
        java.util.Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.OK) return;
        try {
            noteService.deleteNote(userId, selected.getValue().getId());
            refreshNotesTree();
        } catch (Exception e) {
            showError("Delete failed", e.getMessage());
        }
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "" : message);
        alert.showAndWait();
    }

    @FXML
    private void onLogout() {
        Session.setCurrentUser(null);
        // shutdown autosave scheduler
        try {
            scheduler.shutdownNow();
        } catch (Exception ignored) {}
        // Close window
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            SettingsController sc = loader.getController();
            Stage owner = (Stage) welcomeLabel.getScene().getWindow();
            // apply current theme to settings window
            Scene scene = new Scene(root, 360, 180);
            try {
                // Always apply dark theme stylesheet
                root.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            } catch (Exception ignored) {}

            Stage settingsStage = new Stage();
            settingsStage.initOwner(owner);
            settingsStage.initModality(Modality.WINDOW_MODAL);
            settingsStage.setTitle("Settings");
            settingsStage.setScene(scene);
            sc.setOwnerStage(owner);
            settingsStage.show();
        } catch (Exception e) {
            // ignore
        }
    }
}
