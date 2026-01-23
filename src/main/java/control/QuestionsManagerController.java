package control;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Question;
import model.SysData;
import model.Theme;
import util.DialogUtil;
import util.ThemeManager;
import util.UIAnimations;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Questions Management screen.
 *
 * Responsibilities:
 *  - Display questions as UI cards.
 *  - Apply filters (difficulty, ID, search).
 *  - Handle navigation between screens.
 *  - Delegate all data operations to SysData.
 */

public class QuestionsManagerController implements QuestionCardActions {
	
	@FXML
	private AnchorPane QuestionManagerRoot;
	
    @FXML
    private VBox questionsContainerVBox;

    @FXML
    private Button newQuestionButton;
    
    @FXML
    private Button selectButton;
    
    @FXML 
    private Button editSelectedButton;
    
    @FXML 
    private Button deleteSelectedButton;


    @FXML
    private Button backButton;

    @FXML
    private ComboBox<String> levelFilterCombo;

    @FXML
    private ComboBox<String> idFilterCombo;

    @FXML
    private TextField searchTextField;
    
    private final List<QuestionCardController> cardControllers = new ArrayList<>();
    @FXML private ImageView titleBeforeImg;
    @FXML private ImageView titleAfterImg;
        
    private final SysData sysData = SysData.getInstance();
    private List<Question> allQuestions;
    
    private boolean selectionMode = false;
    

    // ================== Navigation ==================
    /**
     * Returns to the main menu screen (main_view.fxml).
     */
    @FXML
    private void onBackButtonClicked(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            util.ViewNavigator.switchTo(stage, "/view/main_view.fxml");
            stage.setTitle("Main Menu"); // optional
        } catch (Exception e) {
            e.printStackTrace();
            // optional: show a dialog (recommended)
            // DialogUtil.show(AlertType.ERROR, "Navigation Error", "Failed to open Main Menu", e.getMessage());
        }
    }


    /**
     * Navigates to the Add Question screen (Add_Question_view.fxml).
     */
    @FXML
    private void onNewQuestionButtonClicked(ActionEvent event) {
        try {
            Stage stage = (Stage) newQuestionButton.getScene().getWindow();
            util.ViewNavigator.switchTo(stage, "/view/Add_Question_view.fxml");
            stage.setTitle("Add New Question"); // optional
        } catch (Exception e) {
            e.printStackTrace();
            // optional: DialogUtil.show(AlertType.ERROR, "Navigation Error", "Failed to open Add Question", e.getMessage());
        }
    }


    // ================== Initialization ==================

    /**
     * Called automatically when Questions_Management_view.fxml is loaded.
     */
    @FXML
    public void initialize() {
    	UIAnimations.fadeIn(QuestionManagerRoot);
    	
        allQuestions = sysData.getAllQuestions();

        setupFilters();
        applyFilters();

        searchTextField.textProperty()
            .addListener((obs, o, n) -> applyFilters());
        
        applyWolfIconsIfNeeded();
    }
    
	
   private void applyWolfIconsIfNeeded() {
    if (ThemeManager.getTheme() == Theme.COLORFUL) return;

    Image wolf = new Image(getClass().getResourceAsStream("/Images/hurdle-wolf.png"));

    if (titleBeforeImg != null) titleBeforeImg.setImage(wolf);
    if (titleAfterImg  != null) titleAfterImg.setImage(wolf);
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
        cardControllers.clear();

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
            
            cardControllers.add(cardController);

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
            Stage stage = (Stage) questionsContainerVBox.getScene().getWindow();

            EditQuestionController editController =
                    util.ViewNavigator.switchToWithController(stage, "/view/Edit_Question_view.fxml");

            editController.setQuestion(q);

            stage.setTitle("Edit Question"); // optional
        } catch (Exception e) {
            e.printStackTrace();
            // optional: DialogUtil.show(AlertType.ERROR, "Navigation Error", "Failed to open Edit Question", e.getMessage());
        }
    }


    // ================== Delete ==================

    /**
     * Deletes a question after user confirmation and saves the updated list
     * back to the CSV file.
     */
    public void deleteQuestion(Question q) {
        if (q == null) return;

        Optional<ButtonType> result = DialogUtil.showDialogWithResult(
                Alert.AlertType.CONFIRMATION,
                "Delete Question",
                "Are you sure?",
                "ID #" + q.getId()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            sysData.deleteQuestionById(q.getId());
            allQuestions = sysData.getAllQuestions();
            applyFilters();
        }
    }
    
    @FXML
    private void onSelectButtonClicked(ActionEvent event) {

        if (!selectionMode) {
            //ENTER selection mode
            selectionMode = true;

            for (QuestionCardController card : cardControllers) {
                card.setSelectionMode(true);
            }

            editSelectedButton.setVisible(true);
            editSelectedButton.setManaged(true);

            deleteSelectedButton.setVisible(true);
            deleteSelectedButton.setManaged(true);

            selectButton.setText("Exit Select Mode");
        } else {
            //EXIT selection mode
            exitSelectionMode();
        }
    }
    
    private void exitSelectionMode() {
        selectionMode = false;

        for (QuestionCardController card : cardControllers) {
            card.setSelectionMode(false);
        }

        editSelectedButton.setVisible(false);
        editSelectedButton.setManaged(false);

        deleteSelectedButton.setVisible(false);
        deleteSelectedButton.setManaged(false);

        selectButton.setText("Select Questions");
        selectButton.getStyleClass().remove("button-bold");
    }



	@Override
	public void onEditQuestion(Question question) {
		openEditScreen(question);
		
	}

	@Override
	public void onDeleteQuestion(Question question) {
		deleteQuestion(question);
		
	}
	
	private List<Question> getSelectedQuestions() {
	    List<Question> selected = new ArrayList<>();
	    for (QuestionCardController card : cardControllers) {
	        if (card.isSelected()) {
	            selected.add(card.getQuestion());
	        }
	    }
	    return selected;
	}
	
	@FXML
	private void onDeleteSelectedClicked(ActionEvent event) {
	    List<Question> selected = getSelectedQuestions();

	    if (selected.isEmpty()) {
	        DialogUtil.show(
	            Alert.AlertType.INFORMATION,
	            "No selection",
	            "Delete questions",
	            "Please select at least one question."
	        );
	        return;
	    }

	    Optional<ButtonType> result = DialogUtil.showDialogWithResult(
	            Alert.AlertType.CONFIRMATION,
	            "Delete Questions",
	            "Confirm deletion",
	            selected.size() + " questions will be deleted."
	    );

	    if (result.isPresent() && result.get() == ButtonType.OK) {
	        for (Question q : selected) {
	            sysData.deleteQuestionById(q.getId());
	        }
	        allQuestions = sysData.getAllQuestions();
	        applyFilters();
	        exitSelectionMode();
	    }
	}
	
	@FXML
	private void onEditSelectedClicked(ActionEvent event) {
	    List<Question> selected = getSelectedQuestions();
	    if (selected.isEmpty()) {
	        DialogUtil.show(Alert.AlertType.INFORMATION, "No selection", "Edit questions",
	                "Please select at least one question.");
	        return;
	    }

	    try {
	        Stage stage = (Stage) QuestionManagerRoot.getScene().getWindow();

	        EditQuestionController editController =
	                util.ViewNavigator.switchToWithController(stage, "/view/Edit_Question_view.fxml");

	        editController.enableBulkEdit(selected);

	        stage.setTitle("Edit Selected Questions");
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}



}