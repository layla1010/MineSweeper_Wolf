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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import util.SoundManager;

/**
 * Controller for the Settings screen.
 *
 * Responsibilities:
 *  - Navigate from the Settings screen to:
 *      * Main menu (back arrow)
 *      * Filters screen
 *      * Other future screens (Customize, Statistics, Daily Challenges, How To Play)
 *  - Play a click sound for all interactive buttons.
 *
 * This screen is purely a navigation hub. It does not store the values of
 * the filters; this is done in {@link FiltersController} and {@link model.SysData}.
 */
public class SettingsController {

    /** Root layout of the Settings screen (defined in settings_view.fxml). */
    @FXML
    private GridPane rootGrid;

    /** Button that opens the Filters screen. */
    @FXML
    private Button filtersBtn;

    /** Button that will open a future Customize screen. */
    @FXML
    private Button customizeBtn;

    /** Button that will open a future Statistics screen. */
    @FXML
    private Button statisticsBtn;

    /** Button that will open a future Daily Challenges screen. */
    @FXML
    private Button dailyChallengesBtn;

    /** Button that will open a future How To Play screen. */
    @FXML
    private Button howToPlayBtn;

    /**
     * Called automatically by JavaFX when the FXML is loaded.
     * Currently no additional initialization is required.
     */
    @FXML
    private void initialize() {
        // In the future, we can add animations or visual initialization here.
    }

    /**
     * Handles click on the back arrow.
     * Navigates back to the main menu screen (main_view.fxml).
     */
    @FXML
    private void onBackToMainClicked() {
        SoundManager.playClick();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
            Parent root = loader.load();

            // Optional: if MainController needs the Stage reference
            MainController mainController = loader.getController();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            mainController.setStage(stage);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Failed to open main menu screen.", e);
        }
    }

    /**
     * Handles click on the Filters button.
     * Navigates to the Filters screen (filters_view.fxml).
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
            showError("Failed to open Filters screen.", e);
        }
    }

    /**
     * Handles click on the Customize button.
     * For now, we only display a placeholder message.
     * In the future, this method can open a dedicated Customize screen.
     */
    @FXML
    private void onCustomizeClicked() {
        SoundManager.playClick();
        showInfo("Customize screen will be implemented later.");
    }

    /**
     * Handles click on the Statistics button.
     * For now, we only display a placeholder message.
     * In the future, this method can open a dedicated Statistics screen.
     */
    @FXML
    private void onStatisticsClicked() {
        SoundManager.playClick();
        showInfo("Statistics screen will be implemented later.");
    }

    /**
     * Handles click on the Daily Challenges button.
     * For now, we only display a placeholder message.
     * In the future, this method can open a dedicated Daily Challenges screen.
     */
    @FXML
    private void onDailyChallengesClicked() {
        SoundManager.playClick();
        showInfo("Daily Challenges screen will be implemented later.");
    }

    /**
     * Handles click on the How To Play button.
     * For now, we only display a placeholder message.
     * In the future, this method can open a dedicated How To Play screen.
     */
    @FXML
    private void onHowToPlayClicked() {
        SoundManager.playClick();
        showInfo("How To Play screen will be implemented later.");
    }

    // ---------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------

    /**
     * Shows a generic error alert with a message and exception details.
     *
     * @param message A short message to explain what failed.
     * @param e       The exception that was thrown.
     */
    private void showError(String message, Exception e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    /**
     * Shows an informational alert to the user.
     * Used for placeholder screens that are not yet implemented.
     *
     * @param message The message to display.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
