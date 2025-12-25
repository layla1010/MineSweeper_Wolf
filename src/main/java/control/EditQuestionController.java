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
import util.DialogUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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

    private static final String CSV_PATH = "src/main/resources/Data/Questionsss.csv";

    @FXML
    public void initialize() {
    	difficultyComboBox.setValue("EASY");
    	correctAnswerComboBox.setValue("A");        }

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

        int id = Integer.parseInt(idTextField.getText().trim());
        String difficultyText = difficultyComboBox.getValue();
        String difficultyNum  = QuestionsManagerController.mapDifficultyToNumber(difficultyText);
        String question = questionTextArea.getText().trim();
        String optA = optionATextField.getText().trim();
        String optB = optionBTextField.getText().trim();
        String optC = optionCTextField.getText().trim();
        String optD = optionDTextField.getText().trim();
        String correctLetter = correctAnswerComboBox.getValue();

        try {
            updateQuestionInCsv(id, difficultyNum, question,
                    optA, optB, optC, optD, correctLetter);
        } catch (IOException e) {
            e.printStackTrace();
        	DialogUtil.show(AlertType.INFORMATION,"", "Error","Failed to update question in CSV.");

            return;
        }

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
        if (isEmpty(questionTextArea.getText())) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","Question text cannot be empty.");

            return false;
        }
        if (isEmpty(optionATextField.getText())
                || isEmpty(optionBTextField.getText())
                || isEmpty(optionCTextField.getText())
                || isEmpty(optionDTextField.getText())) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","All four options (A, B, C, D) must be filled.");

            return false;
        }
        String a = optionATextField.getText().trim();
        String b = optionBTextField.getText().trim();
        String c = optionCTextField.getText().trim();
        String d = optionDTextField.getText().trim();
        
        if (!allDistinctIgnoreCase(a, b, c, d)) {
        	DialogUtil.show(AlertType.ERROR, "Invalid Input", "Validation Error","All four options (A, B, C, D) must be different from each other.");

            return false;
        }
        return true;
    }
    
    //Helper method that Checks that all given strings are distinct, ignoring case
    private boolean allDistinctIgnoreCase(String... values) {
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                if (values[i].equalsIgnoreCase(values[j])) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    //CSV update logic: We respect the existing header order, just like in AddQuestionController

    private void updateQuestionInCsv(int id,
                                     String difficultyNum,
                                     String question,
                                     String optA,
                                     String optB,
                                     String optC,
                                     String optD,
                                     String correctLetter) throws IOException {

        Path path = Paths.get(CSV_PATH);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("CSV file not found: " + CSV_PATH);
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return;

        String headerLine = lines.get(0);
        String delimiter = headerLine.contains(";") ? ";" : ",";

        String[] rawHeaders = headerLine.split(delimiter, -1);
        String[] headers = new String[rawHeaders.length];
        for (int i = 0; i < rawHeaders.length; i++) {
            headers[i] = rawHeaders[i].trim().replace("\uFEFF", "");
        }

        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            col.put(headers[i], i);
        }

        Integer idxId = col.get("ID");
        if (idxId == null) {
            throw new IOException("No ID column in CSV header.");
        }

        for (int row = 1; row < lines.size(); row++) {
            String line = lines.get(row);
            if (line.trim().isEmpty()) continue;

            String[] cells = line.split(delimiter, -1);
            if (cells.length <= idxId) continue;

            String idStr = cells[idxId].trim();
            if (!idStr.matches("\\d+")) continue;

            int rowId = Integer.parseInt(idStr);
            if (rowId != id) continue; // not the row we want

            //We found the row to update – build new cells array based on header names
            String[] newCells = Arrays.copyOf(cells, cells.length);

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i];
                switch (h) {
                    case "ID" -> newCells[i] = String.valueOf(id);
                    case "Question" -> newCells[i] = escape(question);
                    case "Difficulty" -> newCells[i] = difficultyNum;
                    case "A" -> newCells[i] = escape(optA);
                    case "B" -> newCells[i] = escape(optB);
                    case "C" -> newCells[i] = escape(optC);
                    case "D" -> newCells[i] = escape(optD);
                    case "Correct Answer" -> newCells[i] = correctLetter;
                    default -> { /* leave as is */ }
                }
            }

            lines.set(row, String.join(delimiter, newCells));
            break;
        }

        Files.write(path, lines, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains(";") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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

