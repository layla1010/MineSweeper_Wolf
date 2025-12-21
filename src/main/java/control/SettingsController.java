package control;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import util.SoundManager;

public class SettingsController {

    @FXML private GridPane rootGrid;

    @FXML private Button filtersBtn;
    @FXML private Button customizeBtn;
    @FXML private Button howToPlayBtn;

    @FXML private Button backBtn;

    @FXML
    private void initialize() {
        // FXML calls this automatically (no args).
        // Keep empty or add init logic.
    }

    @FXML
    private void onBackToMainClicked() {
        SoundManager.playClick();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load the main menu screen.");
        }
    }

    @FXML
    private void onFiltersClicked() {
        SoundManager.playClick();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/filters_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load the Filters screen.");
        }
    }

    @FXML
    private void onCustomizeClicked() {
        SoundManager.playClick();
        showInfo("Customize screen is not implemented yet.\n" +
                 "In the future, this screen will allow changing themes, colors and avatars.");
    }

    @FXML
    private void onHowToPlayClicked() {
        SoundManager.playClick();
        showInfo("How To Play screen is not implemented yet.\n" +
                 "In the future, this screen will explain Minesweeper WOLF rules and strategies.");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Not Implemented Yet");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
