package util;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public final class ViewNavigator {

    private ViewNavigator() {}

    // =================================================================================
    // "Hot-Swap" Content + Forced Full Size Strategy To Solve Resolution Problem
    // =================================================================================
    private static void updateStage(Stage stage, Parent newRoot) {
        Scene currentScene = stage.getScene();

        if (currentScene != null) {
            // OPTION A: Window is already open.
            // Just replace the content. This keeps the window size exactly as it is.
            currentScene.setRoot(newRoot);
            
        } else {
            // OPTION B: First Launch (Application Start)
            Scene newScene = new Scene(newRoot);
            ThemeManager.applyTheme(newScene);
            stage.setScene(newScene);
            
            // FORCE FULL SCREEN SIZE & LOCK IT 
            
            // Get the visual bounds of the screen (total size minus taskbar)
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Force the stage to match these bounds
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());

            // Disable resizing (Removes the Maximize/Restore button functionality)
            // The user cannot make the window smaller now.
            stage.setResizable(false); 
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