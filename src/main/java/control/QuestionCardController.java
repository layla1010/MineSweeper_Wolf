package control;



import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.Question;

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
    
    private Question question;
    private QuestionsManagerController parentController;
    


    //This will be called by the manager screen to fill the card once added
//    public void setData(int id, String difficulty, String questionText,
//            String optA, String optB, String optC, String optD,
//            int correctOption) {
//
//    		idLabel.setText("#" + id);
//    		difficultyLabel.setText(difficulty);
//    		questionLabel.setText(questionText);
//
//    		option1Button.setText(optA);
//    		option2Button.setText(optB);
//    		option3Button.setText(optC);
//    		option4Button.setText(optD);
//
//    		clearCorrectHighlight();
//    		switch (correctOption) {
//    		case 1 -> option1Button.getStyleClass().add("correct-option");
//    		case 2 -> option2Button.getStyleClass().add("correct-option");
//    		case 3 -> option3Button.getStyleClass().add("correct-option");
//    		case 4 -> option4Button.getStyleClass().add("correct-option");
//    		}
//    }
    
      public void setData(Question q) {
    	  this.question = q;

    	  idLabel.setText("#" + q.getId());
    	  difficultyLabel.setText(q.getDifficulty());
    	  questionLabel.setText(q.getText());

    	  option1Button.setText(q.getOptA());
    	  option2Button.setText(q.getOptB());
    	  option3Button.setText(q.getOptC());
    	  option4Button.setText(q.getOptD());
    	  // highlight correct option etc. (your existing logic)
      }
      

      public void setParentController(QuestionsManagerController parent) {
          this.parentController = parent;
      }

      @FXML
      private void onEditClicked(MouseEvent event) {
          if (parentController != null && question != null) {
              parentController.openEditScreen(question);
          }
      }
      
      @FXML
      private void onDeleteClicked(MouseEvent event) {
//          if (parentController != null && question != null) {
//              parentController.openEditScreen(question);
//          }
    	  System.out.println("Delete clicked for question id=" + question.getId());
      }


      private void clearCorrectHighlight() {
    	  option1Button.getStyleClass().remove("correct-option");
    	  option2Button.getStyleClass().remove("correct-option");
    	  option3Button.getStyleClass().remove("correct-option");
    	  option4Button.getStyleClass().remove("correct-option");
      }
	}

