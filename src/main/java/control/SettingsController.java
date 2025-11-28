package control;

/*
 * Aya Ala Deen – Settings Screen Implementation Documentation
 */

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import util.SoundManager;

/**
 * Controller for the WOLF Settings dashboard screen.
 *
 * Responsibilities:
 *  - Display the main settings menu as a 2x2 dashboard:
 *      * Filters
 *      * Customize
 *      * Statistics
 *      * How To Play
 *  - Navigate to the Filters screen.
 *  - Provide placeholders (stubs) for future screens:
 *      * Customize
 *      * Statistics
 *      * How To Play
 *  - Navigate back to the main menu when the back arrow is clicked.
 */
public class SettingsController {

    /** Root grid of the Settings screen (defined in settings_view.fxml). */
    @FXML
    private GridPane rootGrid;

    /** Filters menu button (top-left card). */
    @FXML
    private Button filtersBtn;

    /** Customize menu button (top-right card). */
    @FXML
    private Button customizeBtn;

    /** Statistics menu button (bottom-left card). */
    @FXML
    private Button statisticsBtn;

    /** How To Play menu button (bottom-right card). */
    @FXML
    private Button howToPlayBtn;
    
    private Stage stage;
    


    /**
     * Called automatically after FXML is loaded.
     * Additional initialization (if needed) can be placed here.
     */
    @FXML
    private void initialize(Stage stage) {
        this.stage = stage;
    }

    // ---------------------------------------------------------------------
    // Navigation handlers
    // ---------------------------------------------------------------------

    /**
     * Navigates back to the main menu screen.
     * Triggered when the back arrow button is clicked.
     */
    @FXML
    private void onBackToMainClicked() {
        SoundManager.playClick();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load the main menu screen.");
        }
    }

    /**
     * Opens the Filters screen (filters_view.fxml).
     * This screen is already implemented and connected to SysData.
     */
    @FXML
    private void onFiltersClicked() {
        SoundManager.playClick();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/filters_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load the Filters screen.");
        }
    }

    /**
     * Placeholder for the Customize screen.
     * Currently not implemented. Shows an informational dialog.
     */
    @FXML
    private void onCustomizeClicked() {
        SoundManager.playClick();
        showInfo("Customize screen is not implemented yet.\n" +
                 "In the future, this screen will allow changing themes, colors and avatars.");
    }

    /**
     * Placeholder for the Statistics screen.
     * Currently not implemented. Shows an informational dialog.
     */
    @FXML
    private void onStatisticsClicked() {
        SoundManager.playClick();
        try {
        	Stage stage = (Stage) rootGrid.getScene().getWindow();
        	
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/view/stats_view.fxml"));
            javafx.scene.Parent root = loader.load();

            StatsViewController controller = loader.getController();
            controller.setStage(stage);

            Scene scene = new Scene(root, 1200,730);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Placeholder for the How To Play screen.
     * Currently not implemented. Shows an informational dialog.
     */
    @FXML
    private void onHowToPlayClicked() {
        SoundManager.playClick();
        showInfo("How To Play screen is not implemented yet.\n" +
                 "In the future, this screen will explain Minesweeper WOLF rules and strategies.");
    }

    // ---------------------------------------------------------------------
    // Helper methods for dialogs
    // ---------------------------------------------------------------------

    /**
     * Shows an error dialog with the given message.
     *
     * @param message The message to display.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an informational dialog with the given message.
     *
     * @param message The message to display.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Not Implemented Yet");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
