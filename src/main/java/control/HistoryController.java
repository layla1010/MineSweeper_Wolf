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
import javafx.scene.control.DatePicker;
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

public class HistoryController {

    @FXML private BorderPane root;
    @FXML private VBox historyList;

    
   // filter + sort controls
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField filterValueField;
    @FXML private ComboBox<String> sortTypeCombo;
    @FXML private javafx.scene.control.DatePicker dateFilterPicker;
    
    private Stage stage;

    
    private final List<Game> allGames = new ArrayList<>();

    private static final DateTimeFormatter CSV_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd"); // how it is stored in CSV
    
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        // load history once when screen opens
        SysData.getInstance().loadHistoryFromCsv();
        allGames.clear();
        allGames.addAll(SysData.getInstance().getHistory().getGames());

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
    
    private void refreshHistoryView() {
        List<Game> filtered = applyFilter(allGames);
        List<Game> sorted = applySort(filtered);
        populateHistory(sorted);
    }
    
    
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

    private HBox createCardForGame(Game game) {
        HBox card = new HBox();
        card.getStyleClass().add("history-card");
        card.setSpacing(10);

        // ===== top row: difficulty | players | result =====
        HBox header = new HBox();
        header.getStyleClass().add("history-card-header");
        header.setSpacing(10);

        String diffText =
                game.getDifficulty().name().substring(0, 1) +
                game.getDifficulty().name().substring(1).toLowerCase();

        Label difficultyLabel = new Label(diffText);
        difficultyLabel.getStyleClass().addAll("pill-label", "difficulty-pill");

        Label playersLabel =
                new Label(game.getPlayer1Nickname() + " & " + game.getPlayer2Nickname());
        playersLabel.getStyleClass().add("players-label");

        Label resultLabel = new Label(
                game.getResult() == GameResult.WIN ? "Won" : "Lost"
        );
        resultLabel.getStyleClass().add("pill-label");
        if (game.getResult() == GameResult.WIN) {
            resultLabel.getStyleClass().add("result-pill-won");
        } else {
            resultLabel.getStyleClass().add("result-pill-lost");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(difficultyLabel, playersLabel, spacer, resultLabel);

        // ===== bottom row: date | score | time =====
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
    
    /* =====================  FILTER & SORT  ===================== */
    
    @FXML
    private void onFilterTypeChanged() {
        if (filterTypeCombo == null) return;

        String type = filterTypeCombo.getValue();
        boolean isDate = "Date".equals(type);

        // If DATE is selected → enable DatePicker, disable text field
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



    
    private List<Game> applyFilter(List<Game> source) {
        String type = filterTypeCombo != null ? filterTypeCombo.getValue() : null;

        // no filter case
        if (type == null || "All".equals(type)) {
            return new ArrayList<>(source);
        }

        // ----- DATE FILTER -----
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

        // ----- TEXT-BASED FILTERS -----
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
                    String res = g.getResult().name().toLowerCase(); // WIN / LOSS
                    if (res.contains(query)) {
                        result.add(g);
                    }
                }
                default -> result.add(g);
            }
        }

        return result;
    }

    
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

    
    /* =====================  BUTTON HANDLERS  ===================== */
    
    @FXML
    private void onFilterApplyClicked() {
        SoundManager.playClick();
        refreshHistoryView();
    }
    
    @FXML
    private void onClearFilterClicked() {
        SoundManager.playClick();
        if (filterTypeCombo != null) {
            filterTypeCombo.getSelectionModel().select("All");
        }
        if (filterValueField != null) {
            ((List<Game>) filterValueField).clear();
        }
        if (dateFilterPicker != null) {
            dateFilterPicker.setValue(null);
        }
        onFilterTypeChanged();
        refreshHistoryView();
    }

    
    @FXML
    private void onSortBtnClicked() {
        SoundManager.playClick();
        refreshHistoryView();
    }

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
