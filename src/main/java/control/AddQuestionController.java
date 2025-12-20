package control;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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

    //Path to your CSV file (relative to project root)
    private static final String CSV_PATH = "src/main/resources/Data/Questionsss.csv";
    
    //Called automatically by JavaFX after the FXML is loaded, Sets the next available question ID and makes the ID field read-only
    @FXML
    public void initialize() {
        // ID is serial and should be auto-filled, not editable
        int nextId = computeNextId();
        idTextField.setText(String.valueOf(nextId));
        difficultyComboBox.getSelectionModel().selectFirst();
        correctAnswerComboBox.getSelectionModel().selectFirst();
        
    }

  
    //Computes the next question ID based on the existing IDs in the CSV file.
    //Reads all rows, finds the maximum ID, and returns max + 1.
    //If the file does not exist or is empty, returns 1.
    private int computeNextId() {
        Path path = Paths.get(CSV_PATH);
        if (!Files.exists(path)) {
            //If file doesn't exist yet, start from 1
            return 1;
        }

        int maxId = 0;

        try (BufferedReader reader =
                     Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

            String header = reader.readLine();
            if (header == null) {
                return 1;
            }

            //detect delimiter: ; or ,
            String delimiter = header.contains(";") ? ";" : ",";

            //find "ID" column index
            String[] headers = header.split(delimiter, -1);
            int idIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\uFEFF", "");
                if ("ID".equals(h)) {
                    idIndex = i;
                    break;
                }
            }
            if (idIndex == -1) {
                return 1;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cells = line.split(delimiter, -1);
                if (cells.length <= idIndex) continue;
                String idStr = cells[idIndex].trim();
                if (idStr.matches("\\d+")) {
                    int id = Integer.parseInt(idStr);
                    if (id > maxId) {
                        maxId = id;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return maxId + 1;
    }

    //Here we can see Handlers for buttons:
    //Save button: Validates the form, Collects all the question data from the UI, Appends a new row to the CSV file
    //and Navigates back to the Questions Management screen if successful.
    @FXML
    private void onSaveButtonClicked(ActionEvent event) {
        //Validate
        if (!validateForm()) {
            return; //stop if invalid
        }

        //Collect data
        int id = Integer.parseInt(idTextField.getText().trim());
        String difficultyText = difficultyComboBox.getValue();
        String difficultyNum  = QuestionsManagerController.mapDifficultyToNumber(difficultyText); 
        String question = questionTextArea.getText().trim();
        String optA = optionATextField.getText().trim();
        String optB = optionBTextField.getText().trim();
        String optC = optionCTextField.getText().trim();
        String optD = optionDTextField.getText().trim();
        String correctLetter = correctAnswerComboBox.getValue(); 

        //Append to CSV
        try {
            appendQuestionToCsv(id, difficultyNum, question,
                    optA, optB, optC, optD, correctLetter);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to save question to CSV.");
            return;
        }

        //Go back to Questions Management screen
        goBackToManager();
    }
    
    
    //Here we handle the cancel button: Shows a confirmation dialog, and if the user confirms,
    //discards changes and returns to the Questions Management screen
    @FXML
    private void onCancelButtonClicked(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel");
        alert.setHeaderText("Are you sure you want to cancel?");
        alert.setContentText("Your changes will not be saved.");

        Optional<ButtonType> result = alert.showAndWait();
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
            showError("Please select a difficulty.");
            return false;
        }
        if (correctAnswerComboBox.getValue() == null) {
            showError("Please select the correct answer (A/B/C/D).");
            return false;
        }
        if (isEmpty(questionTextArea.getText())) {
            showError("Question text cannot be empty.");
            return false;
        }
        if (isEmpty(optionATextField.getText())
                || isEmpty(optionBTextField.getText())
                || isEmpty(optionCTextField.getText())
                || isEmpty(optionDTextField.getText())) {
            showError("All four options (A, B, C, D) must be filled.");
            return false;
        }
        //all options must be different
        String a = optionATextField.getText().trim();
        String b = optionBTextField.getText().trim();
        String c = optionCTextField.getText().trim();
        String d = optionDTextField.getText().trim();

        if (!allDistinctIgnoreCase(a, b, c, d)) {
            showError("All four options (A, B, C, D) must be different from each other.");
            return false;
        }
        return true;
    }
    
    //Helper method: Checks that all given strings are distinct, ignoring case
    boolean allDistinctIgnoreCase(String... values) {
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
    
    //Shows a simple error alert with a given message
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Invalid input");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    //CSV append logic : Appends a new question row to the CSV file
    //Behaviour:
    //If the file exists: reads its header and uses the existing column order.
    //If the file does not exist: creates it with a default header.
    //Builds a row that matches the header order and writes it to the end of the file.

    private void appendQuestionToCsv(int id,
            String difficultyNum,
            String question,
            String optA,
            String optB,
            String optC,
            String optD,
            String correctLetter) throws IOException {

    	Path path = Paths.get(CSV_PATH);
    	String delimiter = ",";
    	String[] headers;

    	if (Files.exists(path)) {
    		//File exists then read the existing header and use its order
    		try (BufferedReader reader =
    				Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

    			String headerLine = reader.readLine();
    			if (headerLine == null) {
    				throw new IOException("CSV file has no header line");
    			}

    			delimiter = headerLine.contains(";") ? ";" : ",";
    			String[] rawHeaders = headerLine.split(delimiter, -1);

    			headers = new String[rawHeaders.length];
    			for (int i = 0; i < rawHeaders.length; i++) {
    				headers[i] = rawHeaders[i].trim().replace("\uFEFF", "");
    			}
    		}
    	} else {
    		//File does NOT exist then create it with a sensible header order
    		delimiter = ",";
    		headers = new String[] {
    				"ID", "Question", "Difficulty",
    				"A", "B", "C", "D", "Correct Answer"
    		};

    		Files.createDirectories(path.getParent());
    		try (BufferedWriter writer = Files.newBufferedWriter(
    				path, StandardCharsets.UTF_8,
    				StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

    			String headerLine = String.join(delimiter, headers);
    			writer.write(headerLine);
    			writer.newLine();
    		}
    	}

    	//Build a row that matches the header order
    	String[] cells = new String[headers.length];

    	for (int i = 0; i < headers.length; i++) {
    		String h = headers[i];
    		switch (h) {
    		case "ID" -> cells[i] = String.valueOf(id);
    		case "Question" -> cells[i] = escape(question);
    		case "Difficulty" -> cells[i] = difficultyNum;
    		case "A" -> cells[i] = escape(optA);
    		case "B" -> cells[i] = escape(optB);
    		case "C" -> cells[i] = escape(optC);
    		case "D" -> cells[i] = escape(optD);
    		case "Correct Answer" -> cells[i] = correctLetter;
    		default -> cells[i] = ""; //unknown column: leave empty
    		}
    	}

    	//Append the row
    	try (BufferedWriter writer = Files.newBufferedWriter(
    			path, StandardCharsets.UTF_8,
    			StandardOpenOption.APPEND)) {

    		String line = String.join(delimiter, cells);
    		writer.write(line);
    		writer.newLine();
    	}
    }

    //Escapes a value for safe CSV storage.
    //If the value contains a comma, semicolon, or quote,
    //wraps it in quotes and doubles any existing quotes.
    private String escape(String value) {
        if (value == null) return "";
        //Wrap in quotes only if needed
        if (value.contains(",") || value.contains(";") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
            showError("Failed to go back to Questions Management view.");
        }
    }
}
