package control;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Game;
import model.GameResult;
import model.SysData;
import util.DialogUtil;
import util.SoundManager;

/**
 * UI Controller: responsible only for UI wiring, rendering, navigation.
 * Filtering/sorting/validation are delegated to HistoryFilterService.
 */
public class HistoryController {

    @FXML private BorderPane root;
    @FXML private VBox historyList;

    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField filterValueField;
    @FXML private ComboBox<String> sortTypeCombo;
    @FXML private DatePicker dateFilterPicker;


    private final List<Game> allGames = new ArrayList<>();

    private static final DateTimeFormatter CSV_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HistoryFilterService service = new HistoryFilterService(CSV_DATE_FORMATTER);


    @FXML
    private void initialize() {
    	SysData.getInstance().ensureHistoryLoaded();
        allGames.clear();
        allGames.addAll(SysData.getInstance().getHistory().getGames());

        // defaults
        selectDefault(filterTypeCombo, HistoryFilterService.OPT_ALL);
        selectDefault(sortTypeCombo, HistoryFilterService.SORT_NONE);

        if (filterTypeCombo != null) {
            filterTypeCombo.setOnAction(event -> onFilterTypeChanged());
        }

        onFilterTypeChanged();

        if (allGames.isEmpty()) {
         	DialogUtil.show(AlertType.INFORMATION, "", "No history yet", "There are no games in the history yet.\nPlay some games first, then come back to this screen.");                  
        }

        refreshHistoryView(HistoryFilterService.OPT_ALL);
    }

    private static void selectDefault(ComboBox<String> combo, String defaultValue) {
        if (combo == null) return;
        if (combo.getSelectionModel().isEmpty()) {
            combo.getSelectionModel().select(defaultValue);
        }
    }

    @FXML
    private void onFilterTypeChanged() {
        String selected = (filterTypeCombo != null) ? filterTypeCombo.getValue() : HistoryFilterService.OPT_ALL;
        boolean isDate = HistoryFilterService.OPT_DATE.equals(selected);

        if (dateFilterPicker != null) {
            dateFilterPicker.setDisable(!isDate);
            if (!isDate) {
                dateFilterPicker.setValue(null);
            }
        }

        if (filterValueField != null) {
            filterValueField.setDisable(isDate);
            if (isDate) {
                filterValueField.clear();
            }
        }
    }

    @FXML
    private void onFilterApplyClicked() {
        SoundManager.playClick();
        //runFilterAndRender(/*smartSortAlso*/ false);
        runFilterAndRender();
    }

    @FXML
    private void onSortBtnClicked() {
        SoundManager.playClick();
        // Sort should sort the same filtered view -> smart inference consistent with Apply
        //runFilterAndRender(/*smartSortAlso*/ true);
        
        //Apply sort on the current filtered results
        runFilterAndRender();
    }

    //private void runFilterAndRender(boolean smartSortAlso) {
    private void runFilterAndRender() {
        String selectedType = (filterTypeCombo != null) ? filterTypeCombo.getValue() : HistoryFilterService.OPT_ALL;
        String typed = getFilterText();
        LocalDate selectedDate = (dateFilterPicker != null) ? dateFilterPicker.getValue() : null;

        String effectiveType = service.resolveEffectiveFilterType(selectedType, typed);

        HistoryFilterService.ValidationResult vr = service.validate(effectiveType, typed, selectedDate);
        if (!vr.ok) {
         	DialogUtil.show(AlertType.WARNING, "", vr.title,vr.message);                  
            return;
        }

        refreshHistoryView(effectiveType);
    }

    private void refreshHistoryView(String effectiveType) {
        String typed = getFilterText();
        LocalDate selectedDate = (dateFilterPicker != null) ? dateFilterPicker.getValue() : null;
        String sortLabel = (sortTypeCombo != null) ? sortTypeCombo.getValue() : HistoryFilterService.SORT_NONE;

        List<Game> filtered = service.filter(allGames, effectiveType, typed, selectedDate);
        List<Game> sorted = service.sort(filtered, sortLabel);

        populateHistory(sorted);

        if (HistoryFilterService.OPT_DATE.equals(effectiveType)
                && sorted.isEmpty()
                && selectedDate != null) {
         	DialogUtil.show(AlertType.WARNING, "", "No games on this date." ,"No games were found on " + selectedDate + ".");                  
        }
    }

    private String getFilterText() {
        if (filterValueField == null) return "";
        String txt = filterValueField.getText();
        return (txt == null) ? "" : txt.trim();
    }

    private void populateHistory(List<Game> games) {
        if (historyList == null) return;
        historyList.getChildren().clear();

        if (games == null || games.isEmpty()) {
            Label empty = new Label("No games found.");
            empty.getStyleClass().add("history-empty-label");
            historyList.getChildren().add(empty);
            return;
        }

        for (Game game : games) {
            historyList.getChildren().add(createCardForGame(game));
        }
    }

    private HBox createCardForGame(Game game) {
        HBox card = new HBox();
        card.getStyleClass().add("history-card");
        card.setSpacing(10);

        HBox header = new HBox();
        header.getStyleClass().add("history-card-header");
        header.setSpacing(10);

        String diffText = prettifyEnumName(game.getDifficulty().name());
        Label difficultyLabel = new Label(diffText);
        difficultyLabel.getStyleClass().addAll("pill-label", "difficulty-pill");

        ImageView avatar1 = createAvatarView(game.getPlayer1AvatarPath());
        ImageView avatar2 = createAvatarView(game.getPlayer2AvatarPath());

        Label playersLabel = new Label(game.getPlayer1Nickname() + " & " + game.getPlayer2Nickname());
        playersLabel.getStyleClass().add("players-label");

        HBox playersBox = new HBox(8, avatar1, playersLabel, avatar2);
        playersBox.setAlignment(Pos.CENTER_LEFT);

        Label resultLabel = buildResultPill(game.getResult());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(difficultyLabel, playersBox, spacer, resultLabel);

        HBox footer = new HBox();
        footer.getStyleClass().add("history-card-footer");
        footer.setSpacing(15);

        Label dateBadge = new Label(game.getDateAsString());
        dateBadge.getStyleClass().add("info-badge");

        Label scoreBadge = new Label("Score: " + game.getFinalScore());
        scoreBadge.getStyleClass().add("info-badge");

        Label timeBadge = new Label("Time: " + game.getDurationFormatted());
        timeBadge.getStyleClass().add("info-badge");

        footer.getChildren().addAll(dateBadge, scoreBadge, timeBadge);

        VBox content = new VBox(header, footer);
        content.setSpacing(6);

        card.getChildren().add(content);
        return card;
    }

    private static String prettifyEnumName(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.substring(0, 1) + raw.substring(1).toLowerCase().replace("_", " ");
    }

    private static Label buildResultPill(GameResult res) {
        String resultText;
        String css;

        switch (res) {
            case WIN -> { resultText = "Won"; css = "result-pill-won"; }
            case LOSE -> { resultText = "Lost"; css = "result-pill-lost"; }
            case GIVE_UP -> { resultText = "Give up"; css = "result-pill-giveup"; }
            default -> { resultText = (res == null) ? "Unknown" : prettifyEnumName(res.name()); css = "result-pill-lost"; }
        }

        Label lbl = new Label(resultText);
        lbl.getStyleClass().add("pill-label");
        lbl.getStyleClass().add(css);
        return lbl;
    }

    private ImageView createAvatarView(String avatarId) {
        Image img = loadAvatarImage(avatarId);
        ImageView iv = new ImageView();
        if (img != null) iv.setImage(img);

        iv.setFitWidth(38);
        iv.setFitHeight(38);
        iv.setPreserveRatio(true);
        iv.getStyleClass().add("history-avatar");
        return iv;
    }

    private Image loadAvatarImage(String avatarId) {
        if (avatarId == null || avatarId.isBlank()) return null;

        if (avatarId.startsWith("file:")) {
            try {
                return new Image(avatarId, false);
            } catch (Exception e) {
                return null;
            }
        }

        try (InputStream stream = getClass().getResourceAsStream("/Images/" + avatarId)) {
            if (stream == null) return null;
            return new Image(stream);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void onClearFilterClicked() {
        SoundManager.playClick();

        if (filterTypeCombo != null) {
            filterTypeCombo.getSelectionModel().select(HistoryFilterService.OPT_ALL);
        }
        if (filterValueField != null) {
            filterValueField.clear();
        }
        if (dateFilterPicker != null) {
            dateFilterPicker.setValue(null);
        }

        onFilterTypeChanged();
        refreshHistoryView(HistoryFilterService.OPT_ALL);
    }

    @FXML
    private void onBackButtonClicked() {
        SoundManager.playClick();

        Stage s = (Stage) root.getScene().getWindow();
        util.ViewNavigator.switchTo(s, "/view/main_view.fxml", 1200, 750);
    }

}
