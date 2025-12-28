package control;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import util.DialogUtil;
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
            Scene scene = new Scene(root);
            util.ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
         	DialogUtil.show(AlertType.ERROR, null, "Navigation error", "Failed to load the main menu screen.");                  
        }
    }

    @FXML
    private void onFiltersClicked() {
        SoundManager.playClick();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/filters_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            Scene scene = new Scene(root);
            util.ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
         	DialogUtil.show(AlertType.ERROR, null, "Navigation error", "Failed to load the Filters screen.");                  
        }
    }

    @FXML
    private void onCustomizeClicked() {
        SoundManager.playClick();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customize_theme_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            Scene scene = new Scene(root);
            util.ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
         	DialogUtil.show(AlertType.ERROR, null, "Navigation error", "Failed to load the Customize screen.");                  
        }
     	// DialogUtil.show(AlertType.INFORMATION, null, "Not implemented yet","Customize screen is not implemented yet.\n" + "In the future, this screen will allow changing themes, colors and avatars.");      
    }

    @FXML
    private void onHowToPlayClicked() {
        SoundManager.playClick();
     	DialogUtil.show(AlertType.INFORMATION, null, "Not implemented yet","How To Play screen is not implemented yet.\n" + "In the future, this screen will explain Minesweeper WOLF rules and strategies.");  
    }

}
