package control;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import model.Question;
import model.Theme;
import util.ThemeManager;
import javafx.scene.control.Tooltip;



public class QuestionCardController {

    @FXML private Label idLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label questionLabel;

    @FXML private Label option1Label; // A
    @FXML private Label option2Label; // B
    @FXML private Label option3Label; // C
    @FXML private Label option4Label; // D

    @FXML private ImageView editImg;
    @FXML private ImageView deleteImg;

    private Question question;
    private QuestionCardActions parentController;

    public void setParentController(QuestionCardActions parent) {
        this.parentController = parent;
    }

    @FXML
    private void onEditClicked(MouseEvent event) {
        if (parentController != null && question != null) {
            parentController.onEditQuestion(question);
        }
    }

    @FXML
    private void onDeleteClicked(MouseEvent event) {
        if (parentController != null && question != null) {
            parentController.onDeleteQuestion(question);
            System.out.println("Delete clicked for question id=" + question.getId());
        }
    }

    private void markAnswers(int correctIndex) {
        Label[] options = { option1Label, option2Label, option3Label, option4Label };

        for (int i = 0; i < options.length; i++) {
            Label lbl = options[i];

            // remove previous state
            lbl.getStyleClass().removeAll("option-correct", "option-wrong");

            // add new state
            if (i == correctIndex) {
                lbl.getStyleClass().add("option-correct");
            } else {
                lbl.getStyleClass().add("option-wrong");
            }
        }
    }

    //Normalizes Question.correctOption
    private int normalizeCorrectIndex(int correctOption) {

        if (correctOption >= 1 && correctOption <= 4) {
            return correctOption - 1;
        }

        if (correctOption >= 0 && correctOption <= 3) {
            return correctOption;
        }

        return 0;
    }
    
    private void applyWolfIconsIfNeeded() {
        // wolf theme only
        if (ThemeManager.getTheme() != Theme.WOLF) return;

        // swap to white icons (wolf)
        editImg.setImage(new Image(getClass().getResourceAsStream("/Images/editing-wolf.png")));
        deleteImg.setImage(new Image(getClass().getResourceAsStream("/Images/trash-wolf.png")));
    }


    
    public void setData(Question q) {
        this.question = q;

        idLabel.setText("#" + q.getId());
        difficultyLabel.setText(q.getDifficulty());
        questionLabel.setText(q.getText());

        option1Label.setText(q.getOptA());
        option2Label.setText(q.getOptB());
        option3Label.setText(q.getOptC());
        option4Label.setText(q.getOptD());

        setTooltip(option1Label, q.getOptA());
        setTooltip(option2Label, q.getOptB());
        setTooltip(option3Label, q.getOptC());
        setTooltip(option4Label, q.getOptD());

        markAnswers(normalizeCorrectIndex(q.getCorrectOption()));
        
     //  wolf-only icons
        applyWolfIconsIfNeeded();
    }
    
    
    private void setTooltip(Label label, String fullText) {
        if (fullText == null) fullText = "";
        Tooltip tp = label.getTooltip();
        if (tp == null) {
            tp = new Tooltip();
            label.setTooltip(tp);
        }
        tp.setText(fullText);

        tp.setWrapText(true);
        tp.setMaxWidth(380);
    }
}
