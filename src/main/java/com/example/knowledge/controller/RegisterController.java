package com.example.knowledge.controller;

import com.example.knowledge.model.User;
import com.example.knowledge.service.AuthService;
import com.example.knowledge.service.NoteService;
import com.example.knowledge.service.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button registerButton;
    @FXML
    private Button backButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void onRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        try {
            User u = authService.register(username, password);
            // set session user
            Session.setCurrentUser(u);

            // load main.fxml and replace current window's scene (same pattern as LoginController)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof com.example.knowledge.controller.MainController) {
                com.example.knowledge.controller.MainController mc = (com.example.knowledge.controller.MainController) controller;
                mc.setNoteService(new NoteService());
                mc.refreshNotesTree();
            }

            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setTitle("KnowledgeApp");
            Scene scene = new Scene(root, 800, 600);
            try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Registration failed");
            err.setHeaderText(null);
            err.setContentText(e.getMessage());
            err.showAndWait();
        }
    }

    @FXML
    private void onBack() throws IOException {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
}
