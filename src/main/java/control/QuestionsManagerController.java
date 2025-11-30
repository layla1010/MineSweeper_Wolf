package control;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Question;

import javafx.scene.control.Button;
import javafx.event.ActionEvent; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Optional;
import java.net.URLDecoder;

public class QuestionsManagerController {

    @FXML
    private VBox questionsContainerVBox;
    @FXML
    private Button newQuestionButton;
    @FXML
    private Button backButton;
    @FXML
    private javafx.scene.control.ComboBox<String> levelFilterCombo;
    @FXML
    private javafx.scene.control.ComboBox<String> idFilterCombo;
    @FXML
    private javafx.scene.control.TextField searchTextField;
    
    //all questions here unfiltered list - to use in filtering!
    private List<Question> allQuestions = new ArrayList<>();
    //Current working list of questions
    private List<Question> questions = new ArrayList<>();
    
    //Returns to the main menu screen (main_view.fxml).
    @FXML
    private void onBackButtonClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/main_view.fxml")
            );
            Parent root = loader.load();

            // Switch the stage scene
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Main Menu");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    //Once clicked: Navigates to the Add Question screen (Add_Question_view.fxml).
    @FXML
    private void onNewQuestionButtonClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/Add_Question_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) newQuestionButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Add New Question");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Called automatically when Questions_Management_view.fxml is loaded.
    @FXML
    public void initialize() {
        reloadQuestionsUI();
        
        //For filtering Task:
        //Loading questions from CSV into allQuestions
        allQuestions = loadQuestionsFromCsv();

        //Preparing filter combo boxes
        setupFilters();
        
        //Search listener: whenever user types → re-apply filters
        if (searchTextField != null) {
            searchTextField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        //Showing all questions initially
        applyFilters();
    }
    //Reloads the questions UI from CSV, clears the current list and VBox, then re-adds all cards.
    private void reloadQuestionsUI() {
        questions.clear();
        questions.addAll(loadQuestionsFromCsv());
        questionsContainerVBox.getChildren().clear();
        for (Question q : questions) {
            addQuestionCard(q);
        }
    }
    //Sets up the filter combo boxes (difficulty and ID), including default values and change listeners.
    private void setupFilters() {
        //Difficulty filter
        levelFilterCombo.getItems().clear();
        levelFilterCombo.getItems().add("All Levels");
        levelFilterCombo.getItems().add("Easy");
        levelFilterCombo.getItems().add("Medium");
        levelFilterCombo.getItems().add("Hard");
        levelFilterCombo.getItems().add("Expert");
        levelFilterCombo.setValue("All Levels"); // default

        //ID filter
        idFilterCombo.getItems().clear();
        idFilterCombo.getItems().add("All IDs");
        for (Question q : allQuestions) {
            idFilterCombo.getItems().add(String.valueOf(q.getId()));
        }
        idFilterCombo.setValue("All IDs"); // default

        //When user changes filter: re-apply filters:
        levelFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        idFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }
    //Applies all active filters (difficulty, ID, search text) to the allQuestions list and updates the UI with the matching questions.
    private void applyFilters() {
        String selectedDifficulty = levelFilterCombo.getValue();
        String selectedId = idFilterCombo.getValue();
        String searchQuery       = (searchTextField != null) ? searchTextField.getText() : null;

        //Normalize values for null safety
        if (selectedDifficulty == null) {
            selectedDifficulty = "All Levels";
        }
        if (selectedId == null) {
            selectedId = "All IDs";
        }
        if (searchQuery == null) {
            searchQuery = "";
        }
        
        String searchLower = searchQuery.trim().toLowerCase();

        //Clear current cards
        questionsContainerVBox.getChildren().clear();

        //Re-add only matching questions
        for (Question q : allQuestions) {

            //Filter by difficulty
            if (!"All Levels".equals(selectedDifficulty)) {
                if (!q.getDifficulty().equalsIgnoreCase(selectedDifficulty)) {
                    continue; //skip this question
                }
            }

            //Filter by ID
            if (!"All IDs".equals(selectedId)) {
                String qIdStr = String.valueOf(q.getId());
                if (!qIdStr.equals(selectedId)) {
                    continue; //skip this question
                }
            }
            
            //filter by search text (question content)
            if (!searchLower.isEmpty()) {
                String questionTextLower = q.getText() == null ? "" : q.getText().toLowerCase();
                if (!questionTextLower.contains(searchLower)) {
                    continue;
                }
            }

            //If it passed all filters then show it
            addQuestionCard(q);
        }
    }
    //Resolves the actual CSV file path for Questionsss.csv.
    private static String getQuestionsCsvPath() {
        try {
            String path = QuestionsManagerController.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            if (decoded.contains(".jar")) {
                decoded = decoded.substring(0, decoded.lastIndexOf('/'));   // folder of jar
                return decoded + "/Data/Questionsss.csv";
            } 
            else {
                if (decoded.contains("target/classes/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("target/classes/"));
                } else if (decoded.contains("bin/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("bin/"));
                } else {
                    return "Data/Questionsss.csv";
                }

                return decoded + "src/main/resources/Data/Questionsss.csv";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Data/Questionsss.csv"; 
        }
    }

    //Loads questions from the CSV file into a List<Question> and validates rows and logs any invalid entries to stderr.
    private List<Question> loadQuestionsFromCsv() {
        List<Question> result = new ArrayList<>();
        
        String csvPath = getQuestionsCsvPath();
        System.out.println("Loading questions from: " + csvPath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvPath), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }

            String delimiter = headerLine.contains(";") ? ";" : ",";

            String[] headers = headerLine.split(delimiter, -1);
            Map<String, Integer> col = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\uFEFF", "");
                col.put(h, i);
            }

            System.out.println("CSV headers (normalized): " + col.keySet());

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;

                String[] cells = line.split(delimiter, -1);

                Integer iA          = col.get("A");
                Integer iB          = col.get("B");
                Integer iC          = col.get("C");
                Integer iD          = col.get("D");
                Integer iDifficulty = col.get("Difficulty");
                Integer iId         = col.get("ID");
                Integer iQuestion   = col.get("Question");
                Integer iCorrect    = col.get("Correct Answer");
                //Basic safety check: enough columns
                if (cells.length <= Math.max(Math.max(iA, iB), Math.max(iC, Math.max(iD,
                        Math.max(iDifficulty, Math.max(iId, Math.max(iQuestion, iCorrect))))))) {
                    System.err.println("Skipping row " + rowNumber + " – not enough columns: " + line);
                    continue;
                }

                String optA          = cells[iA].trim();
                String optB          = cells[iB].trim();
                String optC          = cells[iC].trim();
                String optD          = cells[iD].trim();
                String difficultyNum = cells[iDifficulty].trim();
                String idStr         = cells[iId].trim();
                String questionText  = cells[iQuestion].trim();
                String correctLetter = cells[iCorrect].trim();
                //Validation checks
                if (!idStr.matches("\\d+")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid ID: '" + idStr + "'");
                    continue;
                }
                if (!difficultyNum.matches("[1-4]")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid difficulty: '" + difficultyNum + "'");
                    continue;
                }
                if (!correctLetter.matches("[A-Da-d]")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid correct answer: '" + correctLetter + "'");
                    continue;
                }
                if (questionText.isEmpty()) {
                    System.err.println("Skipping row " + rowNumber + " – empty question text");
                    continue;
                }

                int id = Integer.parseInt(idStr);
                String difficultyText = mapDifficulty(difficultyNum);
                int correctOption     = mapCorrectLetter(correctLetter);

                Question q = new Question(id, difficultyText, questionText,
                        optA, optB, optC, optD, correctOption);
                result.add(q);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }



    //Helper method that converts difficulty number to text
    private String mapDifficulty(String num) {
        return switch (num) {
            case "1" -> "Easy";
            case "2" -> "Medium";
            case "3" -> "Hard";
            case "4" -> "Expert";
            default -> "Unknown";
        };
    }

    //Helper method to convert correct answer option A/B/C/D to 1/2/3/4
    private int mapCorrectLetter(String letter) {
        return switch (letter.toUpperCase()) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 1; // fallback
        };
    }
    //Creates a visual Question card from FXML and adds it to the container VBox.
    private void addQuestionCard(Question q) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/QuestionCard_view.fxml")
            );
            VBox card = loader.load();

            QuestionCardController cardController = loader.getController();
            cardController.setData(q);
            card.setMaxWidth(Double.MAX_VALUE);
            cardController.setParentController(this);

            questionsContainerVBox.getChildren().add(card);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Opens the Edit Question screen for a specific Question.
    public void openEditScreen(Question q) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/Edit_Question_view.fxml"));
            Parent root = loader.load();

            // Get the edit screen controller and pass the question to it
            EditQuestionController editController = loader.getController();
            editController.setQuestion(q);  // we'll implement this next

            Stage stage = (Stage) questionsContainerVBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Question");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Deletes a question after user confirmation
    	public void deleteQuestion(Question q) {
    	    if (q == null) return;

    	    //Confirm with the user
    	    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    	    alert.setTitle("Delete Question");
    	    alert.setHeaderText("Are you sure you want to delete this question?");
    	    alert.setContentText("ID #" + q.getId() + " – " + q.getText());

    	    Optional<ButtonType> result = alert.showAndWait();
    	    if (result.isEmpty() || result.get() != ButtonType.OK) {
    	        // user cancelled -> do nothing
    	        return;
    	    }

    	    //Remove from our in-memory list (by id)
    	    questions.removeIf(qq -> qq.getId() == q.getId());

    	    //Save the updated list back to the CSV (with IDs renumbered)
    	    saveQuestionsToCsv(questions);

    	    //Refresh the UI from the CSV
    	    reloadQuestionsUI();

    	    System.out.println("Delete requested for question id=" + q.getId());
    }
    	
    	
    	// Writes the given list of questions back to the CSV file
    	private void saveQuestionsToCsv(List<Question> questions) {
    	    //adjust path if your file is elsewhere
    		String filePath = getQuestionsCsvPath();
    		System.out.println("Saving questions to: " + filePath);

    	    try (PrintWriter pw = new PrintWriter(
    	            new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

    	        // IMPORTANT

    	        pw.println("ID,Question,Difficulty,A,B,C,D,Correct Answer");

    	        int newId = 1;
    	        for (Question q : questions) {
    	            // Renumber IDs to keep them serial after delete
    	            q.setId(newId++);

    	            String difficultyNum = mapDifficultyToNumber(q.getDifficulty());       // Easy is "1"
    	            String correctLetter = mapCorrectNumberToLetter(q.getCorrectOption()); // 1 is "A"

    	            String line = String.join(",",
    	                    String.valueOf(q.getId()),        // ID
    	                    escapeCsv(q.getText()),           // Question
    	                    difficultyNum,                    // Difficulty (1–4)
    	                    escapeCsv(q.getOptA()),           // A
    	                    escapeCsv(q.getOptB()),           // B
    	                    escapeCsv(q.getOptC()),           // C
    	                    escapeCsv(q.getOptD()),           // D
    	                    correctLetter                     // Correct Answer
    	            );
    	            pw.println(line);
    	        }

    	    } catch (IOException e) {
    	        e.printStackTrace();
    	    }
    	}

    	// escape values that contain commas/quotes/newlines
    	private String escapeCsv(String s) {
    	    if (s == null) return "";
    	    if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
    	        s = s.replace("\"", "\"\"");  // " -> ""
    	        return "\"" + s + "\"";
    	    }
    	    return s;
    	}

    	// Difficulty text to number
    	private String mapDifficultyToNumber(String difficultyText) {
    	    if (difficultyText == null) return "1";
    	    switch (difficultyText.toLowerCase()) {
    	        case "easy":   return "1";
    	        case "medium": return "2";
    	        case "hard":   return "3";
    	        case "expert": return "4";
    	        default:       return "1";
    	    }
    	}

    	// correctOption 1..4 to "A..D"
    	private String mapCorrectNumberToLetter(int correctOption) {
    	    switch (correctOption) {
    	        case 1:  return "A";
    	        case 2:  return "B";
    	        case 3:  return "C";
    	        case 4:  return "D";
    	        default: return "A";
    	    }
    	}


}

