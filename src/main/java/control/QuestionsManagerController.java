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

public class QuestionsManagerController {

    @FXML
    private VBox questionsContainerVBox;
    @FXML
    private Button newQuestionButton;
    
    
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

    @FXML
    public void initialize() {
        //First we load questions from CSV
        List<Question> questions = loadQuestionsFromCsv("/Data/Questionsss.csv");

        //Then for each question we add a card
        for (Question q : questions) {
            addQuestionCard(q);
        }
    }
    //Method to call csv file and turn each valid row into a question object
    private List<Question> loadQuestionsFromCsv(String resourcePath) {
        //A list to store questions objects
    	List<Question> result = new ArrayList<>();
    	//Looking for the file inside resources and if not found return an empty list
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in == null) {
            System.err.println("CSV not found on classpath: " + resourcePath);
            return result;
        }
        //Used BufferedReader for reading text efficiently from any source
        //It adds a buffer (a temporary memory area) so reading text becomes faster and easier
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
        	//Reads the first line of the file and if the file is empty, it just returns an empty list.
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }

            //Detecting delimiter such as ; or ,
            String delimiter = headerLine.contains(";") ? ";" : ",";

            //We start by building header
            String[] headers = headerLine.split(delimiter, -1);//splits the header into an array of column names.
            Map<String, Integer> col = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                //trim + remove BOM if present
                String h = headers[i].trim().replace("\uFEFF", "");//trim(): removes spaces around it.
                col.put(h, i);
            }
            //Now we don’t care about the order of the columns, only about their names.
            System.out.println("CSV headers (normalized): " + col.keySet()); //Just prints which headers were found, for debugging
           
            //At this point, headers are: A, B, C, D, Difficulty, ID, Question, Correct Answer
            //Now we loop over all data starting from row 1(row 0 is a header)
            String line;
            int rowNumber = 1; //we already read header as row 0
            while ((line = reader.readLine()) != null) {
            	rowNumber++;
                if (line.trim().isEmpty()) continue;//skip empty rows

                String[] cells = line.split(delimiter, -1);//Splits the row into individual column values based on the same delimiter as before

                //Get indices by HEADER NAME (order doesn’t matter so things will not be messed up due to columns order)
                //So we use the header map to find where each column is!
                Integer iA          = col.get("A");
                Integer iB          = col.get("B");
                Integer iC          = col.get("C");
                Integer iD          = col.get("D");
                Integer iDifficulty = col.get("Difficulty");
                Integer iId         = col.get("ID");
                Integer iQuestion   = col.get("Question");
                Integer iCorrect    = col.get("Correct Answer");


                //Basic length check - Calculates the maximum index we need,
                //If number of columns in this row is less than or equal that maximum index: it means this row doesn’t have all the required columns.
                //prints a warning and skips this row with continue.
                //This protects us from lines that are too short or broken.
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
                
                //Validation – skip obviously bad rows (like repeated header)
                //ID must be digits only - no letters, no empty
                if (!idStr.matches("\\d+")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid ID: '" + idStr + "'");
                    continue;
                }
                //Difficulty must be one of 1, 2, 3, 4
                if (!difficultyNum.matches("[1-4]")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid difficulty: '" + difficultyNum + "'");
                    continue;
                }
                //Correct answer must be A, B, C, or D
                if (!correctLetter.matches("[A-Da-d]")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid correct answer: '" + correctLetter + "'");
                    continue;
                }
                //Question text can’t be empty
                if (questionText.isEmpty()) {
                    System.err.println("Skipping row " + rowNumber + " – empty question text");
                    continue;
                }

                int id = Integer.parseInt(idStr);
                String difficultyText = mapDifficulty(difficultyNum);
                int correctOption     = mapCorrectLetter(correctLetter);

                Question q = new Question(id, difficultyText, questionText, optA, optB, optC, optD, correctOption);
                result.add(q);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }


    //Helper method
    private String mapDifficulty(String num) {
        return switch (num) {
            case "1" -> "Easy";
            case "2" -> "Medium";
            case "3" -> "Hard";
            case "4" -> "Expert";
            default -> "Unknown";
        };
    }

    //Helper method to convert A/B/C/D to 1/2/3/4
    private int mapCorrectLetter(String letter) {
        return switch (letter.toUpperCase()) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 1; // fallback
        };
    }

    private void addQuestionCard(Question q) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/QuestionCard_view.fxml")
            );
            VBox card = loader.load();

            QuestionCardController cardController = loader.getController();
            cardController.setData(
                    q.getId(),
                    q.getDifficulty(),
                    q.getText(),
                    q.getOptA(), q.getOptB(), q.getOptC(), q.getOptD(),
                    q.getCorrectOption()
            );
            card.setMaxWidth(Double.MAX_VALUE);

            questionsContainerVBox.getChildren().add(card);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

