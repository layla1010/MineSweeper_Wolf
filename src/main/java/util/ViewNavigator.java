package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class ViewNavigator {

    private ViewNavigator() {}

    public static void switchTo(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }
}
