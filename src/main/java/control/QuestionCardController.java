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
    //The Question object whose data is displayed on this card
    private Question question;
    private QuestionsManagerController parentController;
    
    	//Receives a Question object and fills the UI fields in this card.
      public void setData(Question q) {
    	  this.question = q;

    	  idLabel.setText("#" + q.getId());
    	  difficultyLabel.setText(q.getDifficulty());
    	  questionLabel.setText(q.getText());

    	  option1Button.setText(q.getOptA());
    	  option2Button.setText(q.getOptB());
    	  option3Button.setText(q.getOptC());
    	  option4Button.setText(q.getOptD());
      }
      
      //Stores a reference to the parent controller, it is needed to trigger edit/delete actions
      public void setParentController(QuestionsManagerController parent) {
          this.parentController = parent;
      }
      //Once clicked, Notify the parent controller to open the edit screen for this question.
      @FXML
      private void onEditClicked(MouseEvent event) {
          if (parentController != null && question != null) {
              parentController.openEditScreen(question);
          }
      }
      //Once clicked, Ask the parent controller to delete the question.
      @FXML
      private void onDeleteClicked(MouseEvent event) {
    	  if (parentController != null && question != null) {
    	        parentController.deleteQuestion(question);
    	    }
    	  System.out.println("Delete clicked for question id=" + question.getId());
      }

      //Clears any CSS highlighting used to mark the correct option.
      private void clearCorrectHighlight() {
    	  option1Button.getStyleClass().remove("correct-option");
    	  option2Button.getStyleClass().remove("correct-option");
    	  option3Button.getStyleClass().remove("correct-option");
    	  option4Button.getStyleClass().remove("correct-option");
      }
	}

