package control;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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

    private GameConfig config;
    private int score;
    private int elapsedSeconds;
    private int remainingLives;
    private boolean gameWon;
    
    //@FXML private GridPane root;
    @FXML private StackPane root;

    //Called automatically when the End Game screen is loaded
	@FXML
	private void initialize() {
		UIAnimations.fadeIn(root);
		
	    util.UIAnimations.applyHoverZoomToAllButtons(root);
	    util.UIAnimations.applyFloatingToCards(root);
	}
	
	//Initializes the End Game screen with the results of the match. This method receives all game result data from the GameController
    public void init(
                     GameConfig config,
                     int score,
                     int elapsedSeconds,
                     int remainingLives,
                     boolean gameWon) {

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
            Stage stage = (Stage) root.getScene().getWindow();
            util.ViewNavigator.switchTo(stage, "/view/main_view.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    
    //Handles the "Play Again" button: Loads the new game setup screen (new_game_view.fxml), allowing the players to start a completely new match
    @FXML
    private void onPlayAgain() {
        try {
            Stage stage = (Stage) root.getScene().getWindow(); // 
            util.ViewNavigator.switchTo(stage, "/view/new_game_view.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
