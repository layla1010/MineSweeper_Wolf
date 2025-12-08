package control;

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

public class SettingsController {

   
    @FXML
    private GridPane rootGrid;
    @FXML
    private Button filtersBtn;
    @FXML
    private Button customizeBtn;
    @FXML
    private Button statisticsBtn;
    @FXML
    private Button howToPlayBtn;
    
    private Stage stage;
    



    @FXML
    private void initialize(Stage stage) {
        this.stage = stage;
    }
    //Navigates back to the main menu
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

    
     //Opens the Filters screen (filters_view.fxml).
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

  
    //Handles the "Customize" card/button. Currently not implemented. Shows an informational dialog.
    //In the future this will allow changing themes, colors, avatars, etc.
    @FXML
    private void onCustomizeClicked() {
        SoundManager.playClick();
        showInfo("Customize screen is not implemented yet.\n" +
                 "In the future, this screen will allow changing themes, colors and avatars.");
    }

    //Opens the statistics view
    @FXML
    private void onStatisticsClicked() {
        SoundManager.playClick();

        // Block stats if players are not logged in (skip-login / guest mode)
        model.Player p1 = util.SessionManager.getPlayer1();
        model.Player p2 = util.SessionManager.getPlayer2();

        if (p1 == null || p2 == null) {
            showInfo("Statistics are available only for logged-in players.\n" +
                     "Please login as two players before opening the statistics screen.");
            return;
        }

        try {
            Stage stage = (Stage) rootGrid.getScene().getWindow();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/view/stats_view.fxml"));
            Parent root = loader.load();

            StatsViewController controller = loader.getController();
            controller.setStage(stage);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load the Statistics screen.");
        }
    }



   
     //Placeholder for the How To Play screen.
    	//Currently not implemented. Shows an informational dialog.
    @FXML
    private void onHowToPlayClicked() {
        SoundManager.playClick();
        showInfo("How To Play screen is not implemented yet.\n" +
                 "In the future, this screen will explain Minesweeper WOLF rules and strategies.");
    }
    
    //Shows an error dialog with the given message.
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Shows an informational dialog with the given message.
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Not Implemented Yet");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
