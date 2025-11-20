package control;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import model.Difficulty;
import model.GameConfig;

public class NewGameController {

	@FXML private TextField player1Nickname;
	@FXML private TextField player2Nickname;
	@FXML private ToggleGroup difficultyGroup;
	@FXML private ToggleButton easyToggle;
	@FXML private ToggleButton medToggle;
	@FXML private ToggleButton hardToggle;
	
	@FXML private void onStartGameClicked() {
		
		String nickname1 = player1Nickname.getText();
		String nickname2 = player2Nickname.getText();
		
		if(nickname1 == null || nickname1.trim().isEmpty() || nickname2 == null || nickname2.trim().isEmpty()) {
			showError("Please enter both players names.");
			return;
		}
		
		if(difficultyGroup.getSelectedToggle() == null) {
			showError("Please select a difficulty level.");
			return;
		}
		
		Difficulty difficulty = (Difficulty) difficultyGroup.getSelectedToggle().getUserData();
		
        GameConfig config = new GameConfig(nickname1.trim(), nickname2.trim(), difficulty);
		
		System.out.println("player 1 nickname is: " + config.getPlayer1Nickname());
        System.out.println("Player 2 nickname: " + config.getPlayer2Nickname());
        System.out.println("Difficulty: " + config.getDifficulty());
        System.out.println("Rows: " + config.getRows());
        System.out.println("Cols: " + config.getCols());
        System.out.println("Mines: " + config.getMines());
        System.out.println("Initial lives: " + config.getInitialLives());
	}

	private void showError(String message) {
		
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Input Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

}
