package control;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        // load from CSV each time we open the history screen
        SysData.getInstance().loadHistoryFromCsv();
        populateHistory();
    }

    private void populateHistory() {
        historyList.getChildren().clear();

        List<Game> games = SysData.getInstance().getHistory().getGames();

        if (games.isEmpty()) {
            Label empty = new Label("No games played yet.");
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
