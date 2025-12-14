package control;


import java.io.IOException;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Game;
import model.GameResult;
import model.SysData;
import util.SoundManager;
import javafx.scene.image.Image;                
import javafx.scene.image.ImageView; 
import javafx.geometry.Pos; 
import javafx.scene.control.Alert;


public class HistoryController {

    @FXML private BorderPane root;
    @FXML private VBox historyList;

    
    //filter + sort controls
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField filterValueField;
    @FXML private ComboBox<String> sortTypeCombo;
    @FXML private javafx.scene.control.DatePicker dateFilterPicker;
    
    private Stage stage;

    //Holds all games loaded from SysData (unfiltered)
    private final List<Game> allGames = new ArrayList<>();

    //Date format used in the CSV history file
    private static final DateTimeFormatter CSV_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd"); // how it is stored in CSV
    
    //Sets the primary stage reference for this controller
    //Called from the main screen when navigating to history
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    //Called automatically when the History screen loads, Loads the game history from SysData, initializes default filter/sort values, hooks filter-type change handler, and populates the view.
    @FXML
    private void initialize() {
        // load history once when screen opens
        SysData.getInstance().loadHistoryFromCsv();
        allGames.clear();
        allGames.addAll(SysData.getInstance().getHistory().getGames());
        
        if (allGames.isEmpty()) {
            showInfo(
                    "No history yet",
                    "There are no games in the history yet.\n" +
                    "Play some games first, then come back to this screen."
            );
        }


        // default selections
        if (filterTypeCombo != null && filterTypeCombo.getSelectionModel().isEmpty()) {
            filterTypeCombo.getSelectionModel().select("All");
        }
        if (sortTypeCombo != null && sortTypeCombo.getSelectionModel().isEmpty()) {
            sortTypeCombo.getSelectionModel().select("None");
        }
        
       
            filterTypeCombo.setOnAction(event -> onFilterTypeChanged());
       

        refreshHistoryView();
    }
    
    //Rebuilds the history list based on current filter and sort settings
    private void refreshHistoryView() {
        List<Game> filtered = applyFilter(allGames);
        List<Game> sorted = applySort(filtered);
        populateHistory(sorted);
        
        // if filtering by date and no games found → show message
        String type = (filterTypeCombo != null) ? filterTypeCombo.getValue() : null;
        if ("Date".equals(type)
                && (sorted == null || sorted.isEmpty())
                && dateFilterPicker != null
                && dateFilterPicker.getValue() != null) {

            showFilterError(
                    "No games on this date",
                    "No games were found on " + dateFilterPicker.getValue() + "."
            );
        }
    }
    
    //Fills the history VBox with game cards. If the list is empty, shows a "No games found" label instead.
    private void populateHistory(List<Game> games) {
        historyList.getChildren().clear();

        if (games.isEmpty()) {
            Label empty = new Label("No games found.");
            empty.getStyleClass().add("history-empty-label");
            historyList.getChildren().add(empty);
            return;
        }

        for (Game game : games) {
            HBox card = createCardForGame(game);
            historyList.getChildren().add(card);
        }
    }
    
 // ===  helper to load avatar image from stored id/path ===
    private Image loadAvatarImage(String avatarId) {
        if (avatarId == null || avatarId.isBlank()) {
            return null;
        }

        // Custom avatar from file chooser
        if (avatarId.startsWith("file:")) {
            return new Image(avatarId, false);
        }

        // Built-in avatar: S1.png, S2.png, ...
        return new Image(getClass().getResourceAsStream("/Images/" + avatarId));
    }
    
 // === helper to create a small circular avatar view ===
    private ImageView createAvatarView(String avatarId) {
        Image img = loadAvatarImage(avatarId);
        ImageView iv = new ImageView();
        if (img != null) {
            iv.setImage(img);
        }
        iv.setFitWidth(38);
        iv.setFitHeight(38);
        iv.setPreserveRatio(true);
        iv.getStyleClass().add("history-avatar");
        return iv;
    }
    
    

    //Creates a single history card for one game session.
    private HBox createCardForGame(Game game) {
        HBox card = new HBox();
        card.getStyleClass().add("history-card");
        card.setSpacing(10);

        //top row: difficulty, players and result
        HBox header = new HBox();
        header.getStyleClass().add("history-card-header");
        header.setSpacing(10);

        String diffText =
                game.getDifficulty().name().substring(0, 1) +
                game.getDifficulty().name().substring(1).toLowerCase();

        Label difficultyLabel = new Label(diffText);
        difficultyLabel.getStyleClass().addAll("pill-label", "difficulty-pill");

     // ---  avatars and names in one HBox ---
        ImageView avatar1 = createAvatarView(game.getPlayer1AvatarPath());
        ImageView avatar2 = createAvatarView(game.getPlayer2AvatarPath());
        
        
        Label playersLabel =
                new Label(game.getPlayer1Nickname() + " & " + game.getPlayer2Nickname());
        playersLabel.getStyleClass().add("players-label");
        
        HBox playersBox = new HBox(8, avatar1, playersLabel, avatar2);
        playersBox.setAlignment(Pos.CENTER_LEFT);

        GameResult res = game.getResult();
        String resultText;
        String resultCssClass;

        switch (res) {
            case WIN -> {
                resultText = "Won";
                resultCssClass = "result-pill-won";
            }
            case LOSE -> {
                resultText = "Lost";
                resultCssClass = "result-pill-lost";
            }
            case GIVE_UP -> {
                resultText = "Give up";
                resultCssClass = "result-pill-giveup";
            }
            default -> {
                resultText = res.name();
                resultCssClass = "result-pill-lost"; // fallback style
            }
        }

        Label resultLabel = new Label(resultText);
        resultLabel.getStyleClass().add("pill-label");
        resultLabel.getStyleClass().add(resultCssClass);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(difficultyLabel, playersBox, spacer, resultLabel);

        //bottom row: date, score and time
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
    
    
    //Called when the filter-type ComboBox changes. Switches between text-based filters and date-based filter
    @FXML
    private void onFilterTypeChanged() {
        if (filterTypeCombo == null) return;

        String type = filterTypeCombo.getValue();
        boolean isDate = "Date".equals(type);

        // If DATE is selected then enable DatePicker, disable text field
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



    //Applies the current filter to the given list of games.
    private List<Game> applyFilter(List<Game> source) {
        String type = filterTypeCombo != null ? filterTypeCombo.getValue() : null;

        // no filter case
        if (type == null || "All".equals(type)) {
            return new ArrayList<>(source);
        }

        //DATE FILTER
        if ("Date".equals(type)) {
            if (dateFilterPicker == null || dateFilterPicker.getValue() == null) {
                // no date selected → no filtering
                return new ArrayList<>(source);
            }

            java.time.LocalDate selected = dateFilterPicker.getValue();
            List<Game> result = new ArrayList<>();

            for (Game g : source) {
                java.time.LocalDate gameDate =
                        java.time.LocalDate.parse(g.getDateAsString(), CSV_DATE_FORMATTER);
                if (gameDate.equals(selected)) {
                    result.add(g);
                }
            }
            return result;
        }

        //TEXT-BASED FILTERS
        String text = filterValueField != null ? filterValueField.getText() : null;
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>(source);
        }

        String query = text.trim().toLowerCase();
        List<Game> result = new ArrayList<>();

        for (Game g : source) {
            switch (type) {
                case "Player name" -> {
                    String p1 = g.getPlayer1Nickname().toLowerCase();
                    String p2 = g.getPlayer2Nickname().toLowerCase();
                    if (p1.contains(query) || p2.contains(query)) {
                        result.add(g);
                    }
                }
                case "Difficulty" -> {
                    String diff = g.getDifficulty().name().toLowerCase();
                    if (diff.contains(query)) {
                        result.add(g);
                    }
                }
                case "Result" -> {
                    String res = g.getResult().name().toLowerCase();   // e.g. "give_up"
                    String normQuery = normalizeToken(query);          // e.g. "giveup"
                    String normRes   = normalizeToken(res);            // e.g. "giveup"
                    // If the user typed a valid result word, compare canonically
                    if (isResultWord(normQuery)) {
                        String canonQuery = canonicalResultToken(normQuery);
                        String canonRes   = canonicalResultToken(normRes);

                        if (canonRes.equals(canonQuery)) {
                            result.add(g);
                        }
                    } else {
                    // match both raw and normalized strings
                    if (res.contains(query) || normRes.contains(normQuery) ) {
                        result.add(g);
                    }
                }}

                default -> result.add(g);
            }
        }

        return result;
    }
    
  
    
 // ----------------- FILTER VALIDATION HELPERS -----------------

    /** Normalize text: lowercase, remove spaces and underscores. */
    private String normalizeToken(String s) {
        if (s == null) return "";
        return s.toLowerCase().replace(" ", "").replace("_", "");
    }

    /** True if text clearly looks like a difficulty word. */
    private boolean isDifficultyWord(String text) {
        String t = normalizeToken(text);
        return t.equals("easy") || t.equals("medium") || t.equals("med") || t.equals("hard");
    }

    /** True if text clearly looks like a result word (win / lose / give up). */
    private boolean isResultWord(String text) {
        String t = normalizeToken(text);
        return t.equals("win") || t.equals("won")
                || t.equals("lose") || t.equals("loss") || t.equals("lost")
                || t.equals("giveup") || t.equals("giveupgame") || t.equals("give up")
                || t.equals("gave up");
    }
    
    /** Map user input / enum text to a canonical result token: win | lose | giveup */
    private String canonicalResultToken(String text) {
        String t = normalizeToken(text);

        if (t.equals("win") || t.equals("won")) return "win";
        if (t.equals("lose") || t.equals("lost") || t.equals("loss")) return "lose";
        if (t.equals("giveup") || t.equals("giveupgame") || t.equals("give")) return "giveup";

        return t; 
    }


    /** Counts how many different difficulty words appear in the text. */
    private int countDifficultyWords(String text) {
        String t = normalizeToken(text);
        int count = 0;
        if (t.contains("easy")) count++;
        if (t.contains("medium") || t.contains("med")) count++;
        if (t.contains("hard")) count++;
        return count;
    }

    
    /** Shows a small warning popup. */
    private void showFilterError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(350); 
        VBox box = new VBox(label);
        box.setSpacing(10);
        alert.getDialogPane().setContent(box);
        alert.showAndWait();
    }
    
    /** Simple information popup for non-error messages. */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(350);
        VBox box = new VBox(label);
        box.setSpacing(10);
        alert.getDialogPane().setContent(box);
        alert.showAndWait();
    }

    /** Safely get trimmed text from the filter field. */
    private String getFilterText() {
        if (filterValueField == null) return "";
        String txt = filterValueField.getText();
        return txt == null ? "" : txt.trim();
    }

    /**
     * Validates the current filter input according to filter type.
     * Returns true if it's OK to run the filter, false if we showed an error.
     */
    private boolean validateFilterInput() {
        if (filterTypeCombo == null) return true;

        String type = filterTypeCombo.getValue();
        if (type == null || "All".equals(type)) {
            // no filter → always ok
            return true;
        }

     // DATE filter: require a *valid* selected date
        if ("Date".equals(type)) {
            if (dateFilterPicker == null) {
                showFilterError(
                        "Missing date",
                        "You chose to filter by date, but no date field is available."
                );
                return false;
            }

            String editorText = dateFilterPicker.getEditor().getText().trim();

            // user typed something but DatePicker couldn't parse it
            if (!editorText.isEmpty() && dateFilterPicker.getValue() == null) {
                showFilterError(
                        "Invalid date",
                        "The date you entered (\"" + editorText + "\") is not a valid calendar date.\n" +
                        "Please choose a valid date from the calendar."
                );
                return false;
            }

            // nothing typed and nothing selected
            if (editorText.isEmpty() && dateFilterPicker.getValue() == null) {
                showFilterError(
                        "Missing date",
                        "You chose to filter by date but didn’t select any date.\n" +
                        "Please pick a date from the calendar."
                );
                return false;
            }

            // future date → not allowed
            if (dateFilterPicker.getValue() != null &&
                dateFilterPicker.getValue().isAfter(LocalDate.now())) {
                showFilterError(
                        "Invalid date",
                        "You selected a day that hasn’t come yet.\n" +
                        "Please choose a date that already happened."
                );
                return false;
            }

            return true;
        }


        // TEXT–based filters:
        String text = getFilterText();
        if (text.isEmpty()) {
            showFilterError(
                    "Missing value",
                    "Please enter a value in the text field for this filter."
            );
            return false;
        }

        String lower = text.toLowerCase();

     // --- Player name filter ---
        if ("Player name".equals(type)) {

            // 1) too short name
            if (text.length() < 2) {
                showFilterError(
                        "Name too short",
                        "Please enter at least 2 characters when filtering by player name."
                );
                return false;
            }

            // 2) user typed difficulty instead of name
            if (isDifficultyWord(lower)) {
                showFilterError(
                        "Filter mismatch",
                        "You selected \"Player name\" but typed a difficulty (\"" + text + "\").\n" +
                        "Please enter a player nickname/name, or change the filter to \"Difficulty\"."
                );
                return false;
            }

            // 3) user typed result instead of name
            if (isResultWord(lower)) {
                showFilterError(
                        "Filter mismatch",
                        "You selected \"Player name\" but typed a result (\"" + text + "\").\n" +
                        "Please enter a player nickname/name, or change the filter to \"Result\"."
                );
                return false;
            }

            // otherwise OK
            return true;
        }


     // --- Difficulty filter ---
        if ("Difficulty".equals(type)) {

            if (isResultWord(lower)) {
                showFilterError(
                        "Filter mismatch",
                        "You selected \"Difficulty\" but typed a result (\"" + text + "\").\n" +
                        "Use \"Result\" filter for WIN / LOSE / GIVE UP."
                );
                return false;
            }

            // more than one difficulty word (e.g. "easy hard")
            int diffWords = countDifficultyWords(text);
            if (diffWords > 1) {
                showFilterError(
                        "Too many difficulties",
                        "Please filter by one difficulty at a time.\n" +
                        "Valid values are: Easy, Medium, Hard."
                );
                return false;
            }

            // not a known difficulty at all
            if (!isDifficultyWord(lower)) {
                showFilterError(
                        "Invalid difficulty",
                        "Unknown difficulty \"" + text + "\".\n" +
                        "Valid values are: Easy, Medium, Hard."
                );
                return false;
            }

            return true;
        }


        // --- Result filter ---
        if ("Result".equals(type)) {
            if (isDifficultyWord(lower)) {
                showFilterError(
                        "Filter mismatch",
                        "You selected \"Result\" but typed a difficulty (\"" + text + "\").\n" +
                        "Use \"Difficulty\" filter for Easy / Medium / Hard."
                );
                return false;
            }
            if (!isResultWord(lower)) {
                showFilterError(
                        "Invalid result",
                        "Unknown result \"" + text + "\".\n" +
                        "Valid values are: Win, Lose, Give up."
                );
                return false;
            }
            return true;
        }

        // Any other filter type → accept
        return true;
    }


    //Applies sorting to the given list of games based on the selected sort option.
    private List<Game> applySort(List<Game> source) {
        String sort = sortTypeCombo != null ? sortTypeCombo.getValue() : null;
        List<Game> list = new ArrayList<>(source);

        if (sort == null || "None".equals(sort)) {
            return list;
        }

        switch (sort) {
            case "Score (high → low)" ->
                list.sort(Comparator.comparingInt(Game::getFinalScore).reversed());

            case "Score (low → high)" ->
                list.sort(Comparator.comparingInt(Game::getFinalScore));

            case "Date (newest)" ->
                list.sort(Comparator.<Game, LocalDate>comparing(
                        g -> LocalDate.parse(g.getDateAsString(), CSV_DATE_FORMATTER)
                ).reversed());

            case "Date (oldest)" ->
                list.sort(Comparator.<Game, LocalDate>comparing(
                        g -> LocalDate.parse(g.getDateAsString(), CSV_DATE_FORMATTER)
                ));

            case "Duration (short → long)" ->
                list.sort(Comparator.comparingInt(Game::getDurationSeconds));

            case "Duration (long → short)" ->
                list.sort(Comparator.comparingInt(Game::getDurationSeconds).reversed());
        }

        return list;
    }

    
   
    //Plays click sound and refreshes the history view with current filters.
    @FXML
    private void onFilterApplyClicked() {
        SoundManager.playClick();
        
       // validate before actually filtering
        if (!validateFilterInput()) {
            // invalid input – do NOT refresh the view
            return;
        }
        refreshHistoryView();
    }
    
    //Resets all filter controls to default (All / empty / no date) and reloads the full, unsorted history.
    @FXML
    private void onClearFilterClicked() {
        SoundManager.playClick();
        if (filterTypeCombo != null) {
            filterTypeCombo.getSelectionModel().select("All");
        }
        if (filterValueField != null) {
        	filterValueField.clear();
        }
        if (dateFilterPicker != null) {
            dateFilterPicker.setValue(null);
        }
        onFilterTypeChanged();
        refreshHistoryView();
    }

    //Plays click sound and refreshes the history view using the current sort selection.
    @FXML
    private void onSortBtnClicked() {
        SoundManager.playClick();
        refreshHistoryView();
    }

    //Navigates back to the main menu screen and passes the stage to MainController.
    @FXML
    private void onBackBtnClicked() throws IOException {
        SoundManager.playClick();

        Stage s = (stage != null)
                ? stage
                : (Stage) root.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/main_view.fxml")
        );
        Parent mainRoot = loader.load();

        MainController mainController = loader.getController();
        mainController.setStage(s);

        s.setScene(new Scene(mainRoot, 1200, 750));
        s.show();
    }
}
