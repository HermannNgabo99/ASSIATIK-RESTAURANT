package com.borne_audi.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class FrontendApplication extends Application {

    private static Stage stage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;

        URL homeUrl = resolveFxml("home-view.fxml");
        if (homeUrl == null) {
            showError("FXML introuvable", "home-view.fxml introuvable (chemin resources).", null);
            throw new RuntimeException("FXML introuvable: home-view.fxml");
        }

        // ✅ MODE BORNE: taille = écran
        var bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(
                FXMLLoader.load(homeUrl),
                bounds.getWidth(),
                bounds.getHeight()
        );

        stage.setTitle("Asiatik Express - Borne");
        stage.setScene(scene);

        // ✅ plein écran borne
        stage.setFullScreen(true);
        stage.setFullScreenExitHint(""); // enlève le message ESC
        stage.setResizable(false);

        stage.show();
    }

    public static void setRoot(String fxml) {
        try {
            URL url = resolveFxml(fxml);
            if (url == null) {
                showError("FXML introuvable", "Impossible de trouver: " + fxml, null);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);

            // ✅ garde la taille actuelle (plein écran)
            double w = stage.getScene() == null ? 1200 : stage.getScene().getWidth();
            double h = stage.getScene() == null ? 800 : stage.getScene().getHeight();

            Scene scene = new Scene(loader.load(), w, h);
            stage.setScene(scene);

            // ✅ conserve le mode borne après changement d'écran
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setResizable(false);

        } catch (Exception e) {
            showError("Erreur lors du changement d'écran", "Impossible d'ouvrir: " + fxml, e);
            e.printStackTrace();
        }
    }

    private static URL resolveFxml(String fxml) {
        // 1) chemin recommandé
        URL url = FrontendApplication.class.getResource("/com/borne_audi/frontend/" + fxml);
        if (url != null) return url;

        // 2) ton cas actuel (dossier avec des points)
        url = FrontendApplication.class.getResource("/com.borne_audi.frontend/" + fxml);
        if (url != null) return url;

        // 3) fallback relatif
        return FrontendApplication.class.getResource(fxml);
    }

    private static void showError(String title, String header, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);

        if (e != null) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();
            if (stack.length() > 2000) stack = stack.substring(0, 2000) + "\n... (suite dans la console)";
            alert.setContentText(stack);
        } else {
            alert.setContentText("");
        }

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}