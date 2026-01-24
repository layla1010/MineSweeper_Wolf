package control;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import model.Question;
import model.SysData;
import util.DialogUtil;
import util.UIAnimations;
import util.ValidationUtil;
import java.util.*;

public class EditQuestionController {
	//@FXML private AnchorPane EditQuestionRoot;
	@FXML private StackPane EditQuestionRoot;
    @FXML private Label idTextField;
    @FXML private ComboBox<String> difficultyComboBox;
    @FXML private TextArea questionTextArea;
    @FXML private TextField optionATextField;
    @FXML private TextField optionBTextField;
    @FXML private TextField optionCTextField;
    @FXML private TextField optionDTextField;
    @FXML private ComboBox<String> correctAnswerComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Question originalQuestion;
    
    private List<Question> bulkQuestions = null;
    private boolean bulkMode = false;

    @FXML
    public void initialize() {
    	UIAnimations.fadeIn(EditQuestionRoot);
    	
    	difficultyComboBox.setValue("EASY");
    	correctAnswerComboBox.setValue("A");        
    }

    //Called from QuestionsManagerController.openEditScreen
    //Receives the Question object selected in the manager screen and fills all the UI fields with its data so it can be edited
    public void setQuestion(Question q) {
        this.originalQuestion = q;

        idTextField.setText(String.valueOf(q.getId()));
        questionTextArea.setText(q.getText());
        optionATextField.setText(q.getOptA());
        optionBTextField.setText(q.getOptB());
        optionCTextField.setText(q.getOptC());
        optionDTextField.setText(q.getOptD());

        difficultyComboBox.setValue(q.getDifficulty().toUpperCase());
        //convert correctOption int (1-4) to letter ("A"-"D")
        correctAnswerComboBox.setValue(correctOptionToLetter(q.getCorrectOption()));
    }

    //converts the correct option index from the Question model, to the answer letter for the ComboBox
    private String correctOptionToLetter(int c) {
        return switch (c) {
            case 1 -> "A";
            case 2 -> "B";
            case 3 -> "C";
            case 4 -> "D";
            default -> "A";
        };
    }


    //Handling save button in edit view: Validates the form, Reads updated values from the UI, Updates the matching row in the CSV file, Returns to the Questions Management screen if successful
    @FXML
    private void onSaveButtonClicked(ActionEvent event) {
    	//HERE WE CHECK BULK EDIT MODE
        if (bulkMode) {

            if (difficultyComboBox.getValue() == null
                    || correctAnswerComboBox.getValue() == null) {

                DialogUtil.show(
                    AlertType.ERROR,
                    "Invalid Input",
                    "Validation Error",
                    "Please select both difficulty and correct answer."
                );
                return;
            }

            String newDifficulty = difficultyComboBox.getValue();
            String newCorrectLetter = correctAnswerComboBox.getValue();
            int newCorrectIndex = "ABCD".indexOf(newCorrectLetter) + 1;

            for (Question q : bulkQuestions) {
                q.setDifficulty(newDifficulty);
                q.setCorrectOption(newCorrectIndex);
            }

            SysData.getInstance().saveQuestionsToCsv();
            goBackToManager();
            return;
        }
    	
        if (!validateForm()) {
            return;
        }

        SysData.getInstance().updateQuestion(
                originalQuestion.getId(),
                difficultyComboBox.getValue(),
                questionTextArea.getText().trim(),
                optionATextField.getText().trim(),
                optionBTextField.getText().trim(),
                optionCTextField.getText().trim(),
                optionDTextField.getText().trim(),
                correctAnswerComboBox.getValue()
        );

        goBackToManager();
    }


    //Handling click button: Shows a confirmation dialog, and if the user agrees, discards changes and navigates back to the manager screen.  
    @FXML
    private void onCancelButtonClicked(ActionEvent event) {

        ButtonType yesCancel = new ButtonType("Yes, Cancel", ButtonBar.ButtonData.YES);
        ButtonType noContiue = new ButtonType("No, continue editing", ButtonBar.ButtonData.NO);

        Optional<ButtonType> result = DialogUtil.confirmWithCustomButtons(
                "Cancel",
                "Are you sure you want to cancel?",
                "Changes will not be saved.",
                yesCancel,
                noContiue
        );

        if (result.isPresent() && result.get() == yesCancel) {
            goBackToManager();
        }
    }



    //Validation, Checks: Difficulty is selected, Correct answer is selected, Question text is not empty, All four options are filled and All four options are different
    //Shows an error alert if any rule is broken

    private boolean validateForm() {
        if (difficultyComboBox.getValue() == null) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","Please select a difficulty.");

            return false;
        }
        if (correctAnswerComboBox.getValue() == null) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","Please select the correct answer (A/B/C/D).");

            return false;
        }
        if (ValidationUtil.isBlank(questionTextArea.getText())) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","Question text cannot be empty.");

            return false;
        }
        if (ValidationUtil.isBlank(optionATextField.getText())
                || ValidationUtil.isBlank(optionBTextField.getText())
                || ValidationUtil.isBlank(optionCTextField.getText())
                || ValidationUtil.isBlank(optionDTextField.getText())) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","All four options (A, B, C, D) must be filled.");

            return false;
        }
        String a = optionATextField.getText().trim();
        String b = optionBTextField.getText().trim();
        String c = optionCTextField.getText().trim();
        String d = optionDTextField.getText().trim();
        
        if (!ValidationUtil.allDistinctIgnoreCase(a, b, c, d)) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","All four options (A, B, C, D) must be different from each other.");

            return false;
        }
        return true;
    }
 
    //Navigates back to the Questions Management view.
    private void goBackToManager() {
        try {
            Stage stage = (Stage) saveButton.getScene().getWindow();
            util.ViewNavigator.switchTo(stage, "/view/Questions_Management_view.fxml");
            stage.setTitle("Questions Management"); // optional: keep your title behavior
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.show(AlertType.ERROR, "", "Error",
                    "Failed to go back to Questions Management view.");
        }
    }
    
    public void enableBulkEdit(List<Question> questions) {
        this.bulkQuestions = questions;
        this.bulkMode = true;

        // Disable fields NOT allowed in bulk edit
        questionTextArea.setDisable(true);
        optionATextField.setDisable(true);
        optionBTextField.setDisable(true);
        optionCTextField.setDisable(true);
        optionDTextField.setDisable(true);

        // Enable ONLY allowed fields
        difficultyComboBox.setDisable(false);
        correctAnswerComboBox.setDisable(false);

        // Optional UX clarity
        idTextField.setText("Multiple questions selected (" + questions.size() + ")");
    }
}

