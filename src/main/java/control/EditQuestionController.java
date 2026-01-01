package control;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Question;
import model.SysData;
import util.DialogUtil;
import util.ValidationUtil;

import java.io.*;
import java.util.*;

public class EditQuestionController {

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

    @FXML
    public void initialize() {
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

        Optional<ButtonType> result = DialogUtil.showDialogWithResult(
                AlertType.CONFIRMATION,
                "Are you sure you want to cancel?",
                "Cancel",
                "Changes will not be saved."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/Questions_Management_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Questions Management");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        	DialogUtil.show(AlertType.ERROR, "", "Error","Failed to go back to Questions Management view.");

        }
    }
}

