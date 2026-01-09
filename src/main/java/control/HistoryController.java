package control;

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
import javafx.scene.control.Button;
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
import util.OnboardingManager;
import util.OnboardingPolicy;
import util.OnboardingStep;
import util.SessionManager;
import util.SoundManager;
import util.UIAnimations;

/**
 * UI Controller: responsible only for UI wiring, rendering, navigation.
 * Filtering/sorting/validation are delegated to HistoryFilterService.
 */
public class HistoryController {

    @FXML private BorderPane root;
    @FXML private VBox historyList;

    @FXML private Button soundButton;
    @FXML private Button musicButton;

    
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField filterValueField;
    @FXML private ComboBox<String> sortTypeCombo;
    @FXML private DatePicker dateFilterPicker;


    private final List<Game> allGames = new ArrayList<>();
    
    private List<Game> currentView = new ArrayList<>();


    private static final DateTimeFormatter CSV_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HistoryFilterService service = new HistoryFilterService(CSV_DATE_FORMATTER);


    @FXML
    private void initialize() {
    	SysData.getInstance().ensureHistoryLoaded();
    	UIAnimations.fadeIn(root);
    	
        allGames.clear();
        allGames.addAll(SysData.getInstance().getHistory().getGames());

        // defaults
        selectDefault(filterTypeCombo, HistoryFilterService.OPT_ALL);
        selectDefault(sortTypeCombo, HistoryFilterService.SORT_NONE);
        
        
        List<OnboardingStep> historySteps = List.of(
        		 new OnboardingStep("#backBtn", "Back",
                         "Return to main menu."),
        		 new OnboardingStep("#filterTypeCombo", "Filter By",
                         "Choose which game detail to filter the history by (player, difficulty, result, or date)."),
                 new OnboardingStep("#filterValueField", "Filter value",
                         "Enter the value to filter by, based on the selected filter type."),
                 new OnboardingStep("#dateFilterPicker", "Date Selector",
                         "Choose a date to filter history entries by date (used when “Date” is selected)."),
                 new OnboardingStep("#applyBtn", "Apply Filter",
                         "Apply the selected filter and value to update the history list."),
                 new OnboardingStep("#sortTypeCombo", "Sort by",
                         "Choose how the history entries are ordered (e.g., by date, score, or duration)."),
                 new OnboardingStep("#sortBtn", "Apply Sort",
                         "Apply the selected sorting option to reorder the history list."),
                 new OnboardingStep("#refreshIcon", "Clear filters",
                         "Reset all filters and sorting to show the full, unfiltered history list.")
         );
         
        OnboardingPolicy policy =
                SessionManager.isAdminMode() ? OnboardingPolicy.NEVER :
                SessionManager.isGuestMode() ? OnboardingPolicy.ALWAYS :
                OnboardingPolicy.ONCE_THEN_HOVER;

        String userKey = SessionManager.getOnboardingUserKey();

        OnboardingManager.runWithPolicy("onboarding.history", root, historySteps, policy, userKey);
        

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
        applyFilterAndRender();
    }

    private void applyFilterAndRender() {
        String selectedType = (filterTypeCombo != null)
                ? filterTypeCombo.getValue()
                : HistoryFilterService.OPT_ALL;

        String typed = getFilterText();
        LocalDate selectedDate = (dateFilterPicker != null) ? dateFilterPicker.getValue() : null;

        String effectiveType = service.resolveEffectiveFilterType(selectedType, typed);

        HistoryFilterService.ValidationResult vr = service.validate(effectiveType, typed, selectedDate);
        if (!vr.ok) {
            DialogUtil.show(AlertType.WARNING, "", vr.title, vr.message);
            return;
        }

        refreshHistoryView(effectiveType);
    }
	
	
	@FXML
	private void onSortBtnClicked() {
	    SoundManager.playClick();
	    applySortOnlyAndRender();
	}

	private void applySortOnlyAndRender() {
	    String selectedType = (filterTypeCombo != null)
	            ? filterTypeCombo.getValue()
	            : HistoryFilterService.OPT_ALL;

	    String typed = getFilterText();

	    // For sort-only, we still want the same effective type inference logic,
	    // but we do NOT validate filter inputs.
	    String effectiveType = service.resolveEffectiveFilterType(selectedType, typed);

	    refreshHistoryView(effectiveType);
	}
	
  

	private void refreshHistoryView(String effectiveType) {
	    String typed = getFilterText();
	    LocalDate selectedDate = (dateFilterPicker != null) ? dateFilterPicker.getValue() : null;
	    String sortLabel = (sortTypeCombo != null) ? sortTypeCombo.getValue() : HistoryFilterService.SORT_NONE;

	    List<Game> filtered = service.filter(allGames, effectiveType, typed, selectedDate);
	    List<Game> sorted = service.sort(filtered, sortLabel);

	    currentView = sorted;           // <-- add this line
	    populateHistory(sorted);

	    if (HistoryFilterService.OPT_DATE.equals(effectiveType)
	            && sorted.isEmpty()
	            && selectedDate != null) {
	        DialogUtil.show(AlertType.WARNING, "", "No games on this date.",
	                "No games were found on " + selectedDate + ".");
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
        
        switch (game.getDifficulty()) {
        case EASY -> difficultyLabel.getStyleClass().add("difficulty-easy");
        case MEDIUM -> difficultyLabel.getStyleClass().add("difficulty-medium");
        case HARD -> difficultyLabel.getStyleClass().add("difficulty-hard");
        default -> {}
    }


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
            case WIN -> { resultText = "🏆 WON"; css = "result-pill-won"; }
            case LOSE -> { resultText = "💀 LOST"; css = "result-pill-lost"; }
            case GIVE_UP -> { resultText = "🏳 GIVE UP"; css = "result-pill-giveup"; }
            default -> { resultText = (res == null) ? "Unknown" : prettifyEnumName(res.name()); css = "result-pill-lost"; }
        }

        Label lbl = new Label(resultText);
        lbl.getStyleClass().add("pill-label");
        lbl.getStyleClass().add(css);
        return lbl;
    }

    private ImageView createAvatarView(String avatarId) {
    	Image img = util.AvatarManager.resolveAvatar(avatarId);

        ImageView iv = new ImageView();
        if (img != null) iv.setImage(img);

        iv.setFitWidth(38);
        iv.setFitHeight(38);
        iv.setPreserveRatio(true);
        iv.getStyleClass().add("history-avatar");
        return iv;
    }
    

    @FXML
    private void onSoundOff() {
        SoundManager.playClick();

        boolean newState = !SysData.isSoundEnabled();
        SysData.setSoundEnabled(newState);

        refreshSoundIconFromSettings();
    }

    @FXML
    private void onMusicToggle() {
        SoundManager.playClick();

        SoundManager.toggleMusic();

        boolean musicOn = SoundManager.isMusicOn();
        SysData.setMusicEnabled(musicOn);

        refreshMusicIconFromSettings();
    }

    private void refreshSoundIconFromSettings() {
        if (soundButton == null) return;
        if (!(soundButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isSoundEnabled();
        String iconPath = enabled ? "/Images/volume.png" : "/Images/mute.png";

        var stream = getClass().getResourceAsStream(iconPath);
        if (stream == null) return;

        iv.setImage(new Image(stream));
    }

    private void refreshMusicIconFromSettings() {
        if (musicButton == null) return;
        if (!(musicButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isMusicEnabled();

        if (enabled && !SoundManager.isMusicOn()) SoundManager.startMusic();
        if (!enabled && SoundManager.isMusicOn()) SoundManager.stopMusic();

        String iconPath = enabled ? "/Images/music.png" : "/Images/music_mute.png";

        var stream = getClass().getResourceAsStream(iconPath);
        if (stream == null) return;

        iv.setImage(new Image(stream));
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
