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

    private Stage getStage() {
        return (Stage) rootGrid.getScene().getWindow();
    }


    @FXML
    private void onBackToMainClicked() {
        SoundManager.playClick();
        try {
            util.ViewNavigator.switchTo(getStage(), "/view/main_view.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to navigate to main_view", e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation error",
                    "Failed to load the main menu screen.");
        }
    }

    @FXML
    private void onFiltersClicked() {
        SoundManager.playClick();
        try {
            util.ViewNavigator.switchTo(getStage(), "/view/filters_view.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to navigate to filters_view", e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation error",
                    "Failed to load the Filters screen.");
        }
    }

    @FXML
    private void onCustomizeClicked() {
        SoundManager.playClick();
        try {
            util.ViewNavigator.switchTo(getStage(), "/view/customize.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to navigate to customize", e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation error",
                    "Failed to load the Customize screen.");
        }
    }


    @FXML
    private void onHowToPlayClicked() {
        SoundManager.playClick();
        DialogUtil.show(AlertType.INFORMATION, null, "Not implemented yet",
                "How To Play screen is not implemented yet.\n" +
                "In the future, this screen will explain Minesweeper WOLF rules and strategies.");
    }
}
