package com.example.knowledge.controller;

import com.example.knowledge.service.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class SettingsController {

    @FXML
    private ComboBox<String> themeCombo;

    private Stage ownerStage;

    @FXML
    private void initialize() {
        themeCombo.getItems().addAll("Dark", "Light");
        // default selection; real selection set when ownerStage is injected
        if (themeCombo.getItems().size() > 0) themeCombo.setValue("Dark");
    }

    public void setOwnerStage(Stage owner) {
        this.ownerStage = owner;
        if (ownerStage != null && ownerStage.getScene() != null && ownerStage.getScene().getRoot() != null) {
            // Always use Dark as the app no longer supports the light stylesheet
            themeCombo.setValue("Dark");
        }
    }

    @FXML
    private void onThemeChange() {
        if (ownerStage == null) return;
        String theme = themeCombo.getValue();
        if (theme == null) return;
        try {
            // Ignore Light selection; always apply dark theme
            String url = getClass().getResource("/css/style.css").toExternalForm();
            var root = ownerStage.getScene().getRoot();
            root.getStylesheets().clear();
            root.getStylesheets().add(url);
            var myRoot = themeCombo.getScene().getRoot();
            myRoot.getStylesheets().clear();
            myRoot.getStylesheets().add(url);
        } catch (Exception e) {
            // ignore theme change errors
        }
    }

    @FXML
    private void onLogout() {
        try {
            // clear session
            Session.setCurrentUser(null);
            // load login.fxml into ownerStage
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 400, 300);
            // Always apply dark stylesheet on logout
            try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
            ownerStage.setTitle("KnowledgeApp");
            ownerStage.setScene(scene);
            ownerStage.show();
            // close settings window
            Stage s = (Stage) themeCombo.getScene().getWindow();
            s.close();
        } catch (IOException e) {
            // ignore logout errors
        }
    }
}
