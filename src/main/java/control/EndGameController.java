package control;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import model.Difficulty;
import model.GameConfig;
import util.UIAnimations;

public class EndGameController {

    @FXML private Text playersName;
    @FXML private Text FinalScore;
    @FXML private Text timeSurvived;
    @FXML private Text difficultyLevel;
    @FXML private Text livesLeft;

    @FXML private Button playAgainBtn;
    @FXML private Button mainMenuBtn;

    private Stage stage;
    private GameConfig config;
    private int score;
    private int elapsedSeconds;
    private int remainingLives;
    private boolean gameWon;
    
    @FXML
	private GridPane root;

    //Called automatically when the End Game screen is loaded
	@FXML
	private void initialize() {
		UIAnimations.fadeIn(root);
		
	    util.UIAnimations.applyHoverZoomToAllButtons(root);
	    util.UIAnimations.applyFloatingToCards(root);
	}
	
	//Initialises the End Game screen with the results of the match. This method receives all game result data from the GameController
    public void init(Stage stage,
                     GameConfig config,
                     int score,
                     int elapsedSeconds,
                     int remainingLives,
                     boolean gameWon) {

        this.stage = stage;
        this.config = config;
        this.score = score;
        this.elapsedSeconds = elapsedSeconds;
        this.remainingLives = remainingLives;
        this.gameWon = gameWon;

        // Fill the text fields - Populate UI labels with actual game data
        if (config != null) {
            playersName.setText(config.getPlayer1Nickname() + " & " + config.getPlayer2Nickname());
            Difficulty diff = config.getDifficulty();
            if (diff != null) {
                difficultyLevel.setText(diff.name());
            }
        }

        FinalScore.setText(String.valueOf(score));
        //Format total seconds into mm:ss form
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timeSurvived.setText(String.format("%02d:%02d", minutes, seconds));

        livesLeft.setText(String.valueOf(remainingLives));
    }
    
    //Handles the "Main Menu" button: Loads the main menu screen (main_view.fxml) and replaces the current scene
    @FXML
    private void onMainMenu() {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/view/main_view.fxml"));
            javafx.scene.Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setStage(stage);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //Handles the "Play Again" button: Loads the new game setup screen (new_game_view.fxml), allowing the players to start a completely new match
    @FXML
    private void onPlayAgain() {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/view/new_game_view.fxml"));
            javafx.scene.Parent root = loader.load();

            NewGameController controller = loader.getController();
            controller.setStage(stage); // if you have such a method

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
