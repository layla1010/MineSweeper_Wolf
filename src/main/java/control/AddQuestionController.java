package control;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import util.DialogUtil;
import util.UIAnimations;
import util.ValidationUtil;
import javafx.stage.Stage;
import model.SysData;

import java.io.*;
import java.util.Optional;

public class AddQuestionController {


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
    
    //Called automatically by JavaFX after the FXML is loaded, Sets the next available question ID and makes the ID field read-only
    @FXML
    public void initialize() {
    	
        int nextId = SysData.getInstance().getNextQuestionId();
        idTextField.setText(String.valueOf(nextId));
        
        difficultyComboBox.getSelectionModel().selectFirst();
        correctAnswerComboBox.getSelectionModel().selectFirst();
    }

    //Here we can see Handlers for buttons:
    //Save button: Validates input, delegates question creation to SysData,
    //and navigates back to the Questions Management screen.

    @FXML
    private void onSaveButtonClicked(ActionEvent event) {
        if (!validateForm()) {
            return;
        }

        SysData.getInstance().addQuestion(
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

    
    
    //Here we handle the cancel button: Shows a confirmation dialog, and if the user confirms,
    //discards changes and returns to the Questions Management screen
    @FXML
    private void onCancelButtonClicked(ActionEvent event) {
    	  Optional<ButtonType> result = DialogUtil.showDialogWithResult(
                  AlertType.CONFIRMATION,
                  "Are you sure you want to cancel?",
                  "Cancel",
                  "Your Changes will not be saved."
          );

          if (result.isPresent() && result.get() == ButtonType.OK) {
              goBackToManager();
          }
    }

    //Validation: Validates all form fields before saving
    //Checks: Difficulty selected, Correct answer selected, Question text not empty,
    //All four options (A, B, C, D) filled, All four options are different (case-insensitive), and
    //Shows an error alert and returns false if any check fails
    private boolean validateForm() {
        if (difficultyComboBox.getValue() == null) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error", "Please select a difficulty.");
            return false;
        }
        if (correctAnswerComboBox.getValue() == null) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error", "Please select the correct answer (A/B/C/D).");
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
        //all options must be different
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

    //Navigation back to Questions_Management_view
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
        	DialogUtil.show(AlertType.INFORMATION, "Invalid Input", "Validation Error","Failed to go back to Questions Management view.");

        }
    }
}
