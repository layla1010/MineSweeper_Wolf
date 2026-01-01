package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

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
    
    public static <T> T switchToWithController(Stage stage, String fxmlPath, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, width, height);
            ThemeManager.applyTheme(scene);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

            return loader.getController();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to navigate to: " + fxmlPath, e);
        }
    }
    
    public static void switchTo(Stage stage, String fxmlPath, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, width, height);
            ThemeManager.applyTheme(scene);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to navigate to: " + fxmlPath, e);
        }
    }

    
    public static <T> T switchToWithController(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

            return loader.getController();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to navigate to: " + fxmlPath, e);
        }
    }

    
    public static void switchTo(Stage stage, String fxmlPath, Consumer<Object> controllerInit) throws IOException {
        if (stage == null) throw new IllegalArgumentException("stage is null");
        if (fxmlPath == null || fxmlPath.isBlank()) throw new IllegalArgumentException("fxmlPath is blank");

        FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controllerInit != null) {
            controllerInit.accept(controller);
        }

        Scene scene = new Scene(root);
        ThemeManager.applyTheme(scene);   // CENTRAL: always apply theme here

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

}
