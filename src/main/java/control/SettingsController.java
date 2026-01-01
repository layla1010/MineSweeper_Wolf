package control;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import util.DialogUtil;
import util.SoundManager;

public class SettingsController {

    private static final Logger LOG = Logger.getLogger(SettingsController.class.getName());

    @FXML private GridPane rootGrid;

    @FXML private Button filtersBtn;
    @FXML private Button customizeBtn;
    @FXML private Button howToPlayBtn;
    @FXML private Button backBtn;

    @FXML
    private void initialize() {
    }

    private Stage resolveStage() {
        if (rootGrid != null && rootGrid.getScene() != null) {
            return (Stage) rootGrid.getScene().getWindow();
        }
        return null;
    }

    private void navigateTo(String fxmlPath, String failureMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = resolveStage();
            if (stage == null) {
                DialogUtil.show(AlertType.ERROR, null, "Navigation error",
                        "Could not determine the application window (Stage).");
                return;
            }

            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to navigate to: " + fxmlPath, e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation error", failureMessage);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected navigation failure to: " + fxmlPath, e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation error",
                    "An unexpected error occurred while opening the screen.");
        }
    }

    @FXML
    private void onBackToMainClicked() {
        SoundManager.playClick();
        navigateTo("/view/main_view.fxml", "Failed to load the main menu screen.");
    }

    @FXML
    private void onFiltersClicked() {
        SoundManager.playClick();
        navigateTo("/view/filters_view.fxml", "Failed to load the Filters screen.");
    }

    @FXML
    private void onCustomizeClicked() {
        SoundManager.playClick();
        navigateTo("/view/customize.fxml", "Failed to load the Filters screen.");
    }

    @FXML
    private void onHowToPlayClicked() {
        SoundManager.playClick();
        DialogUtil.show(AlertType.INFORMATION, null, "Not implemented yet",
                "How To Play screen is not implemented yet.\n" +
                "In the future, this screen will explain Minesweeper WOLF rules and strategies.");
    }
}
