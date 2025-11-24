package control;

import java.io.IOException;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.SoundManager;

public class MainController {

    @FXML private GridPane mainGrid;
    @FXML private ImageView logoImage;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        // If you DON'T want a click when main screen opens, delete this line:
        // SoundManager.playClick();
        playLogoAnimation();
    }

    private void playLogoAnimation() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(1200), logoImage);
        slide.setFromX(-600);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(800), logoImage);
        fade.setFromValue(0);
        fade.setToValue(1);

        slide.play();
        fade.play();
    }

    @FXML
    private void onNewGameClicked() {
        SoundManager.playClick();
        try {
            Stage stage = (Stage) mainGrid.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/new_game_view.fxml"));
            Pane root = loader.load();

            NewGameController newGameController = loader.getController();
            newGameController.setStage(stage);

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load New Game screen");
            alert.setContentText("An unexpected error occurred:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onQuestionManagementClicked() {
        SoundManager.playClick();
        // TODO: implement screen
    }

    @FXML
    private void onHistoryBtnClicked() {
        SoundManager.playClick();
        // TODO: implement screen
    }
}
