package com.example.knowledge.controller;

import com.example.knowledge.model.User;
import com.example.knowledge.service.AuthService;
import com.example.knowledge.service.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin() {
        messageLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();
        try {
            User u = authService.login(username, password);
            Session.setCurrentUser(u);
            // load main.fxml and replace current window's scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof com.example.knowledge.controller.MainController) {
                com.example.knowledge.controller.MainController mc = (com.example.knowledge.controller.MainController) controller;
                mc.setNoteService(new com.example.knowledge.service.NoteService());
                // ensure controller refreshes now that service is injected
                mc.refreshNotesTree();
            } else {
                // try to call setCurrentUser(User) reflectively if controller supports it
                try {
                    java.lang.reflect.Method m = controller.getClass().getMethod("setCurrentUser", com.example.knowledge.model.User.class);
                    m.invoke(controller, u);
                } catch (NoSuchMethodException ignored) {
                }
            }
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("KnowledgeApp");
            Scene scene = new Scene(root, 800, 600);
            try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Login failed");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onRegister() {
        // Open register screen
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Register");
            Scene scene = new Scene(root, 400, 300);
            try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to open register screen: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void openMainWindow() throws IOException {
        // kept for compatibility but not used in new flow
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        Object controller = loader.getController();
        if (controller instanceof com.example.knowledge.controller.MainController) {
            com.example.knowledge.controller.MainController mc = (com.example.knowledge.controller.MainController) controller;
            mc.setNoteService(new com.example.knowledge.service.NoteService());
        }
        Stage stage = new Stage();
        stage.setTitle("KnowledgeApp");
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    private void closeCurrentWindow() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.close();
    }
}
