package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public final class ViewNavigator {

    private ViewNavigator() {}
    private static final double APP_WIDTH = 1300;
    private static final double APP_HEIGHT = 770;


    // =================================================================================
    // "Hot-Swap" Content + Forced Full Size Strategy To Solve Resolution Problem
    // =================================================================================
    private static void updateStage(Stage stage, Parent newRoot) {
        Scene currentScene = stage.getScene();

        if (currentScene != null) {
            // Switching views → keep window size
            currentScene.setRoot(newRoot);
        } else {
            // First launch
            Scene newScene = new Scene(newRoot);
            ThemeManager.applyTheme(newScene);
            stage.setScene(newScene);

            stage.setResizable(true); // allow applying size first
            stage.show();

            // Enforce fixed design resolution AFTER show
            javafx.application.Platform.runLater(() -> {
                stage.setWidth(APP_WIDTH);
                stage.setHeight(APP_HEIGHT);

                stage.setMinWidth(APP_WIDTH);
                stage.setMinHeight(APP_HEIGHT);
                stage.setMaxWidth(APP_WIDTH);
                stage.setMaxHeight(APP_HEIGHT);

                stage.centerOnScreen();
                stage.setResizable(false); // lock it
            });

            return; // prevent double show
        }

        stage.show();
    }


    // =================================================================================
    // PUBLIC METHODS
    // =================================================================================

    public static void switchTo(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            updateStage(stage, root); // Use the fixed updateStage method
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }
    
    public static <T> T switchToWithController(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            updateStage(stage, root);
            return loader.getController();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to navigate to: " + fxmlPath, e);
        }
    }

    // Retained for compatibility
    public static <T> T switchToWithController(Stage stage, String fxmlPath, double width, double height) {
        return switchToWithController(stage, fxmlPath);
    }
    
    public static void switchTo(Stage stage, String fxmlPath, double width, double height) {
        switchTo(stage, fxmlPath);
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

        updateStage(stage, root);
    }
    
    // =================================================================================
    // BACK STACK
    // =================================================================================
    private static final java.util.Deque<String> backStack = new java.util.ArrayDeque<>();

    public static void pushReturnTarget(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) return;
        backStack.push(fxmlPath);
    }

    public static String popReturnTargetOrDefault(String fallback) {
        String v = backStack.pollFirst();
        return (v == null || v.isBlank()) ? fallback : v;
    }

    public static boolean hasReturnTarget() {
        return !backStack.isEmpty();
    }
    
    public static void goBack(Stage stage, String fallbackFxml) {
        String target = popReturnTargetOrDefault(fallbackFxml);
        switchTo(stage, target);
    }
}