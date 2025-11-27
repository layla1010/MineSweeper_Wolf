package control;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class QuestionCardController {

    @FXML private Label idLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label questionLabel;

    @FXML private Button option1Button;
    @FXML private Button option2Button;
    @FXML private Button option3Button;
    @FXML private Button option4Button;

    @FXML private ImageView editImg;
    @FXML private ImageView deleteImg;

    //This will be called by the manager screen to fill the card once added
    public void setData(int id, String difficulty, String questionText,
            String optA, String optB, String optC, String optD,
            int correctOption) {

    		idLabel.setText("#" + id);
    		difficultyLabel.setText(difficulty);
    		questionLabel.setText(questionText);

    		option1Button.setText(optA);
    		option2Button.setText(optB);
    		option3Button.setText(optC);
    		option4Button.setText(optD);

    		clearCorrectHighlight();
    		switch (correctOption) {
    		case 1 -> option1Button.getStyleClass().add("correct-option");
    		case 2 -> option2Button.getStyleClass().add("correct-option");
    		case 3 -> option3Button.getStyleClass().add("correct-option");
    		case 4 -> option4Button.getStyleClass().add("correct-option");
    		}
    }


    private void clearCorrectHighlight() {
        option1Button.getStyleClass().remove("correct-option");
        option2Button.getStyleClass().remove("correct-option");
        option3Button.getStyleClass().remove("correct-option");
        option4Button.getStyleClass().remove("correct-option");
    }
}

