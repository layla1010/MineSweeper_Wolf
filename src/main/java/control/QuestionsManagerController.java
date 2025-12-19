package control;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Question;

import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the Questions Management screen.
 *
 * Responsibilities:
 *  - Load all questions from the CSV file.
 *  - Display each question as a "card" in a VBox.
 *  - Support filters by difficulty, question ID, and text search.
 *  - Navigate to "Add Question" and "Edit Question" screens.
 *  - Delete questions and save the updated list back to CSV.
 */
public class QuestionsManagerController {

    @FXML
    private VBox questionsContainerVBox;

    @FXML
    private Button newQuestionButton;

    @FXML
    private Button backButton;

    @FXML
    private ComboBox<String> levelFilterCombo;

    @FXML
    private ComboBox<String> idFilterCombo;

    @FXML
    private TextField searchTextField;

    /** Unfiltered list of all questions (used as the base for filtering). */
    private List<Question> allQuestions = new ArrayList<>();

    /** Current working list of questions (e.g. after delete). */
    private List<Question> questions = new ArrayList<>();

    // ================== Navigation ==================

    /**
     * Returns to the main menu screen (main_view.fxml).
     */
    @FXML
    private void onBackButtonClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/main_view.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Main Menu");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigates to the Add Question screen (Add_Question_view.fxml).
     */
    @FXML
    private void onNewQuestionButtonClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/Add_Question_view.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) newQuestionButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Add New Question");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================== Initialization ==================

    /**
     * Called automatically when Questions_Management_view.fxml is loaded.
     */
    @FXML
    public void initialize() {
        // First load questions from CSV into both allQuestions and questions
        questions.clear();
        questions.addAll(loadQuestionsFromCsv());
        allQuestions.clear();
        allQuestions.addAll(questions);

        // Build initial UI from the list
        reloadQuestionsUI();

        // Prepare filter combo boxes (difficulty + ID)
        setupFilters();

        // Search listener: whenever user types → re-apply filters
        if (searchTextField != null) {
            searchTextField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        // Show all questions initially with filters applied
        applyFilters();
    }

    /**
     * Reloads the questions UI from the current allQuestions list.
     * Clears the VBox and re-adds all question cards.
     */
    private void reloadQuestionsUI() {
        questionsContainerVBox.getChildren().clear();
        for (Question q : allQuestions) {
            addQuestionCard(q);
        }
    }

    // ================== Filters & Search ==================

    /**
     * Sets up the filter combo boxes (difficulty and ID), including default values
     * and change listeners.
     */
    private void setupFilters() {
        // Difficulty filter
        levelFilterCombo.getItems().clear();
        levelFilterCombo.getItems().add("All Levels");
        levelFilterCombo.getItems().add("Easy");
        levelFilterCombo.getItems().add("Medium");
        levelFilterCombo.getItems().add("Hard");
        levelFilterCombo.getItems().add("Expert");
        levelFilterCombo.setValue("All Levels"); // default

        // ID filter
        idFilterCombo.getItems().clear();
        idFilterCombo.getItems().add("All IDs");
        for (Question q : allQuestions) {
            idFilterCombo.getItems().add(String.valueOf(q.getId()));
        }
        idFilterCombo.setValue("All IDs"); // default

        // When user changes filter: re-apply filters
        levelFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        idFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Applies all active filters (difficulty, ID, search text) to the allQuestions list
     * and updates the UI with the matching questions.
     */
    private void applyFilters() {
        String selectedDifficulty = levelFilterCombo.getValue();
        String selectedId = idFilterCombo.getValue();
        String searchQuery = (searchTextField != null) ? searchTextField.getText() : null;

        // Normalize values for null safety
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

        // Clear current cards
        questionsContainerVBox.getChildren().clear();

        // Re-add only matching questions
        for (Question q : allQuestions) {

            // Filter by difficulty
            if (!"All Levels".equals(selectedDifficulty)) {
                if (!q.getDifficulty().equalsIgnoreCase(selectedDifficulty)) {
                    continue; // skip this question
                }
            }

            // Filter by ID
            if (!"All IDs".equals(selectedId)) {
                String qIdStr = String.valueOf(q.getId());
                if (!qIdStr.equals(selectedId)) {
                    continue; // skip this question
                }
            }

            // Filter by search text (question content)
            if (!searchLower.isEmpty()) {
                String questionTextLower = (q.getText() == null) ? "" : q.getText().toLowerCase();
                if (!questionTextLower.contains(searchLower)) {
                    continue;
                }
            }

            // If it passed all filters, show it
            addQuestionCard(q);
        }
    }

    // ================== CSV Path & Loading ==================

    /**
     * Resolves the actual CSV file path for Data/Questionsss.csv.
     * Works both from IDE (classes folder) and from the JAR.
     */
    private static String getQuestionsCsvPath() {
        try {
            String path = QuestionsManagerController.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            // Running from a JAR
            if (decoded.contains(".jar")) {
                decoded = decoded.substring(0, decoded.lastIndexOf('/'));   // folder of jar
                return decoded + "/Data/Questionsss.csv";
            } else {
                // Running from IDE - strip build folder and point to src/main/resources
                if (decoded.contains("target/classes/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("target/classes/"));
                } else if (decoded.contains("bin/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("bin/"));
                } else {
                    // fallback, relative path
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
    public List<Question> loadQuestionsFromCsv() {
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

                // Basic safety check: enough columns
                int maxIndex = Math.max(Math.max(iA, iB),
                        Math.max(iC, Math.max(iD,
                        Math.max(iDifficulty, Math.max(iId,
                        Math.max(iQuestion, iCorrect))))));

                if (cells.length <= maxIndex) {
                    System.err.println("Skipping row " + rowNumber + " – not enough columns: " + line);
                    continue;
                }

                String optA          = cells[iA].trim();
                String optB          = cells[iB].trim();
                String optC          = cells[iC].trim();
                String optD          = cells[iD].trim();
                String difficultyRaw = cells[iDifficulty].trim();
                String idStr         = cells[iId].trim();
                String questionText  = cells[iQuestion].trim();
                String correctLetter = cells[iCorrect].trim();

                // Validation checks
                if (!idStr.matches("\\d+")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid ID: '" + idStr + "'");
                    continue;
                }
                String difficultyNum;
                
                if (difficultyRaw.matches("[1-4]")) {
                    difficultyNum = difficultyRaw;
                } else {
                    difficultyNum = mapDifficultyToNumber(difficultyRaw); // you already have this method
                    if (difficultyNum == null) {
                        System.err.println("Skipping row " + rowNumber + " – invalid difficulty: '" + difficultyRaw + "'");
                        continue;
                    }
                }
                String difficultyText = mapDifficulty(difficultyNum);

                if (!correctLetter.matches("[A-Da-d]")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid correct answer: '" + correctLetter + "'");
                    continue;
                }
                if (questionText.isEmpty()) {
                    System.err.println("Skipping row " + rowNumber + " – empty question text");
                    continue;
                }

                int id = Integer.parseInt(idStr);
                int correctOption     = mapCorrectLetter(correctLetter);

                Question q = new Question(
                        id,
                        difficultyText,
                        questionText,
                        optA,
                        optB,
                        optC,
                        optD,
                        correctOption
                );
                result.add(q);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
    
    public static List<Question> loadQuestionsForGame() {
        QuestionsManagerController temp = new QuestionsManagerController();
        return temp.loadQuestionsFromCsv();
    }


    /**
     * Converts difficulty number to text.
     */
    private String mapDifficulty(String num) {
        return switch (num) {
            case "1" -> "Easy";
            case "2" -> "Medium";
            case "3" -> "Hard";
            case "4" -> "Expert";
            default -> "Unknown";
        };
    }

    /**
     * Converts correct answer letter A/B/C/D to index 1..4.
     */
    private int mapCorrectLetter(String letter) {
        return switch (letter.toUpperCase()) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 1; // fallback
        };
    }

    // ================== UI Cards ==================

    /**
     * Creates a visual Question card from FXML and adds it to the container VBox.
     */
    private void addQuestionCard(Question q) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/QuestionCard_view.fxml")
            );
            VBox card = loader.load();

            QuestionCardController cardController = loader.getController();
            cardController.setData(q);
            cardController.setParentController(this);

            card.setMaxWidth(Double.MAX_VALUE);
            questionsContainerVBox.getChildren().add(card);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the Edit Question screen for a specific Question.
     */
    public void openEditScreen(Question q) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/Edit_Question_view.fxml")
            );
            Parent root = loader.load();

            EditQuestionController editController = loader.getController();
            editController.setQuestion(q);

            Stage stage = (Stage) questionsContainerVBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Question");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================== Delete & Save ==================

    /**
     * Deletes a question after user confirmation and saves the updated list
     * back to the CSV file.
     */
    public void deleteQuestion(Question q) {
        if (q == null) return;

        // Confirm with the user
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Question");
        alert.setHeaderText("Are you sure you want to delete this question?");
        alert.setContentText("ID #" + q.getId() + " – " + q.getText());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            // user cancelled -> do nothing
            return;
        }

        // Remove from in-memory lists
        questions.removeIf(qq -> qq.getId() == q.getId());
        allQuestions.removeIf(qq -> qq.getId() == q.getId());

        // Save the updated list back to the CSV (with IDs renumbered)
        saveQuestionsToCsv(allQuestions);

        // Reload UI from updated list
        reloadQuestionsUI();
        // Rebuild filters because IDs changed
        setupFilters();
        applyFilters();

        System.out.println("Deleted question id=" + q.getId());
    }

    /**
     * Writes the given list of questions back to the CSV file.
     * IDs are renumbered sequentially (1..N) after delete.
     */
    private void saveQuestionsToCsv(List<Question> questions) {
        String filePath = getQuestionsCsvPath();
        System.out.println("Saving questions to: " + filePath);

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            // CSV header – column names
            pw.println("ID,Question,Difficulty,A,B,C,D,Correct Answer");

            int newId = 1;
            for (Question q : questions) {
                // Renumber IDs to keep them serial after delete
                q.setId(newId++);

                String difficultyNum = mapDifficultyToNumber(q.getDifficulty());
                String correctLetter = mapCorrectNumberToLetter(q.getCorrectOption());

                String line = String.join(",",
                        String.valueOf(q.getId()),      // ID
                        escapeCsv(q.getText()),         // Question
                        difficultyNum,                  // Difficulty (1–4)
                        escapeCsv(q.getOptA()),         // A
                        escapeCsv(q.getOptB()),         // B
                        escapeCsv(q.getOptC()),         // C
                        escapeCsv(q.getOptD()),         // D
                        correctLetter                   // Correct Answer
                );
                pw.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Escapes values that contain commas/quotes/newlines for CSV.
     */
    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");  // " -> ""
            return "\"" + s + "\"";
        }
        return s;
    }

    /**
     * Difficulty text (Easy/Medium/Hard/Expert) to number (1..4).
     */
    static String mapDifficultyToNumber(String difficultyText) {
        if (difficultyText == null) return "1";
        switch (difficultyText.toLowerCase()) {
            case "easy":   return "1";
            case "medium": return "2";
            case "hard":   return "3";
            case "expert": return "4";
            default:       return "1";
        }
    }

    /**
     * correctOption 1..4 → "A..D".
     */
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