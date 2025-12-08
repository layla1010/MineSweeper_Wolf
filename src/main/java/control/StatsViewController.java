package control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import model.Player;
import model.PlayerStats;
import model.SysData;
import util.SessionManager;

/**
 * Controller for the statistics screen.
 * Displays statistics for Player 1 and Player 2:
 * - Wins / Losses / Give ups / Wins with no mistakes
 * - Total games, best score, best time and opponents
 * - Score progression over time by difficulty
 */
public class StatsViewController {

    private Stage stage;

    @FXML private ScrollPane mainPane;

    @FXML private ImageView player1avatar;
    @FXML private Text      p1OfficialNameText;
    @FXML private ImageView player2avatar;
    @FXML private Text      p2OfficialNameText;

    public ScrollPane getMainPane() {
        return mainPane;
    }

    
    public static class PlayerStatsData {

        public final String playerName;
        public final String avatarImagePath;

        public final int totalGames;
        public final int wins;
        public final int losses;
        public final int giveUps;
        public final int winsWithNoMistakes;

        public final int bestScore;
        public final String bestScoreOpponent;

        public final int bestTimeSeconds;
        public final String bestTimeOpponent;

        // Arrays of scores to show progression by difficulty
        public final int[] easyScores;
        public final int[] mediumScores;
        public final int[] hardScores;

        public PlayerStatsData(String playerName,
                               String avatarImagePath,
                               int totalGames,
                               int wins,
                               int losses,
                               int giveUps,
                               int winsWithNoMistakes,
                               int bestScore,
                               String bestScoreOpponent,
                               int bestTimeSeconds,
                               String bestTimeOpponent,
                               int[] easyScores,
                               int[] mediumScores,
                               int[] hardScores) {

            this.playerName = (playerName == null || playerName.isBlank()) ? "-" : playerName;
            this.avatarImagePath = avatarImagePath;

            this.totalGames = Math.max(0, totalGames);
            this.wins = Math.max(0, wins);
            this.losses = Math.max(0, losses);
            this.giveUps = Math.max(0, giveUps);
            this.winsWithNoMistakes = Math.max(0, winsWithNoMistakes);

            this.bestScore = Math.max(0, bestScore);
            this.bestScoreOpponent = (bestScoreOpponent == null || bestScoreOpponent.isBlank())
                    ? "-" : bestScoreOpponent;

            this.bestTimeSeconds = Math.max(0, bestTimeSeconds);
            this.bestTimeOpponent = (bestTimeOpponent == null || bestTimeOpponent.isBlank())
                    ? "-" : bestTimeOpponent;

            this.easyScores   = easyScores   != null ? easyScores   : new int[0];
            this.mediumScores = mediumScores != null ? mediumScores : new int[0];
            this.hardScores   = hardScores   != null ? hardScores   : new int[0];
        }
    }

    // Pie charts for Player 1
    @FXML private PieChart p1winsChart;
    @FXML private PieChart p1lossesChart;
    @FXML private PieChart p1giveUpsChart;
    @FXML private PieChart p1winsWithNoMistakesChart;

    // Pie charts for Player 2
    @FXML private PieChart p2winsChart;
    @FXML private PieChart p2lossesChart;
    @FXML private PieChart p2giveUpsChart;
    @FXML private PieChart p2winsWithNoMistakesChart;

    // Text labels for Player 1 pie charts
    @FXML private Label p1winsLabel;
    @FXML private Label p1lossesLabel;
    @FXML private Label p1giveUpsLabel;
    @FXML private Label p1winsWithNoMistakesLabel;

    // Text labels for Player 2 pie charts
    @FXML private Label p2winsLabel;
    @FXML private Label p2lossesLabel;
    @FXML private Label p2giveUpsLabel;
    @FXML private Label p2winsWithNoMistakesLabel;

    // Summary labels for Player 1
    @FXML private Label p1TotalGamesLabel;
    @FXML private Label p1BestScoreLabel;
    @FXML private Label p1BestScoreWithLabel;
    @FXML private Label p1BestTimeLabel;
    @FXML private Label p1BestTimeWithLabel;

    // Summary labels for Player 2
    @FXML private Label p2TotalGamesLabel;
    @FXML private Label p2BestScoreLabel;
    @FXML private Label p2BestScoreWithLabel;
    @FXML private Label p2BestTimeLabel;
    @FXML private Label p2BestTimeWithLabel;

    // Line charts for progression of scores
    @FXML private LineChart<Number, Number> p1ProgressChart;
    @FXML private LineChart<Number, Number> p2ProgressChart;

    
    
    @FXML
    private void initialize() {
        SysData.getInstance().loadHistoryFromCsv();

        // Real data: take logged-in players from SessionManager
        SysData sys = SysData.getInstance();

        Player p1Model = SessionManager.getPlayer1();
        Player p2Model = SessionManager.getPlayer2();

        if (p1Model != null) {
            PlayerStats stats1 = sys.computeStatsForPlayer(p1Model);

            PlayerStatsData p1 = new PlayerStatsData(
                    stats1.playerName,
                    stats1.avatarId,
                    stats1.totalGames,
                    stats1.wins,
                    stats1.losses,
                    stats1.giveUps,
                    stats1.winsWithNoMistakes,
                    stats1.bestScore,
                    stats1.bestScoreOpponent,
                    stats1.bestTimeSeconds,
                    stats1.bestTimeOpponent,
                    stats1.easyScores,
                    stats1.mediumScores,
                    stats1.hardScores
            );
            setPlayer1Stats(p1);
        }

        if (p2Model != null) {
            PlayerStats stats2 = sys.computeStatsForPlayer(p2Model);

            PlayerStatsData p2 = new PlayerStatsData(
                    stats2.playerName,
                    stats2.avatarId,
                    stats2.totalGames,
                    stats2.wins,
                    stats2.losses,
                    stats2.giveUps,
                    stats2.winsWithNoMistakes,
                    stats2.bestScore,
                    stats2.bestScoreOpponent,
                    stats2.bestTimeSeconds,
                    stats2.bestTimeOpponent,
                    stats2.easyScores,
                    stats2.mediumScores,
                    stats2.hardScores
            );
            setPlayer2Stats(p2);
        }
    }

    
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // Applies Player 1 statistics to all Player 1 UI controls
    public void setPlayer1Stats(PlayerStatsData stats) {
        applyStatsToUi(
                stats,
                p1winsChart, p1winsLabel,
                p1lossesChart, p1lossesLabel,
                p1giveUpsChart, p1giveUpsLabel,
                p1winsWithNoMistakesChart, p1winsWithNoMistakesLabel,
                p1TotalGamesLabel,
                p1BestScoreLabel, p1BestScoreWithLabel,
                p1BestTimeLabel, p1BestTimeWithLabel
        );

        if (p1OfficialNameText != null) {
            p1OfficialNameText.setText(stats.playerName);
        }

        if (player1avatar != null && stats.avatarImagePath != null && !stats.avatarImagePath.isBlank()) {
            player1avatar.setImage(new Image(stats.avatarImagePath));
        }

        if (p1ProgressChart != null) {
            updateProgressChart(p1ProgressChart, stats);
        }
    }

    // Applies Player 2 statistics to all Player 2 UI controls
    public void setPlayer2Stats(PlayerStatsData stats) {
        applyStatsToUi(
                stats,
                p2winsChart, p2winsLabel,
                p2lossesChart, p2lossesLabel,
                p2giveUpsChart, p2giveUpsLabel,
                p2winsWithNoMistakesChart, p2winsWithNoMistakesLabel,
                p2TotalGamesLabel,
                p2BestScoreLabel, p2BestScoreWithLabel,
                p2BestTimeLabel, p2BestTimeWithLabel
        );

        if (p2OfficialNameText != null) {
            p2OfficialNameText.setText(stats.playerName);
        }

        if (player2avatar != null && stats.avatarImagePath != null && !stats.avatarImagePath.isBlank()) {
            player2avatar.setImage(new Image(stats.avatarImagePath));
        }

        if (p2ProgressChart != null) {
            updateProgressChart(p2ProgressChart, stats);
        }
    }

    // =========================
    // Internal helpers
    // =========================

    private void applyStatsToUi(PlayerStatsData stats,
                                PieChart winsChart, Label winsLabel,
                                PieChart lossesChart, Label lossesLabel,
                                PieChart giveUpsChart, Label giveUpsLabel,
                                PieChart winsNoMistakesChart, Label winsNoMistakesLabel,
                                Label totalGamesLabel,
                                Label bestScoreLabel, Label bestScoreWithLabel,
                                Label bestTimeLabel, Label bestTimeWithLabel) {

        if (stats == null) return;

        int totalOutcomes = stats.wins + stats.losses + stats.giveUps + stats.winsWithNoMistakes;
        double winPercent = 0, lossesPercent = 0, giveUpsPercent = 0, winsNoMistakesPercent = 0;

        if (totalOutcomes > 0) {
            winPercent            = stats.wins               * 100.0 / totalOutcomes;
            lossesPercent         = stats.losses             * 100.0 / totalOutcomes;
            giveUpsPercent        = stats.giveUps            * 100.0 / totalOutcomes;
            winsNoMistakesPercent = stats.winsWithNoMistakes * 100.0 / totalOutcomes;
        }

        setupDonut(winsChart,           winsLabel,           winPercent);
        setupDonut(lossesChart,         lossesLabel,         lossesPercent);
        setupDonut(giveUpsChart,        giveUpsLabel,        giveUpsPercent);
        setupDonut(winsNoMistakesChart, winsNoMistakesLabel, winsNoMistakesPercent);

        if (totalGamesLabel != null) {
            totalGamesLabel.setText(String.valueOf(stats.totalGames));
        }

        if (bestScoreLabel != null) {
            bestScoreLabel.setText("best score:    " + stats.bestScore);
        }
        if (bestScoreWithLabel != null) {
            String opponent = (stats.bestScoreOpponent == null || stats.bestScoreOpponent.isBlank())
                    ? "-" : stats.bestScoreOpponent;
            bestScoreWithLabel.setText("with:       " + opponent);
        }

        if (bestTimeLabel != null) {
            bestTimeLabel.setText("best time:  " + formatDuration(stats.bestTimeSeconds));
        }
        if (bestTimeWithLabel != null) {
            String opponent = (stats.bestTimeOpponent == null || stats.bestTimeOpponent.isBlank())
                    ? "-" : stats.bestTimeOpponent;
            bestTimeWithLabel.setText("With:      " + opponent);
        }
    }

    private void updateProgressChart(LineChart<Number, Number> chart, PlayerStatsData stats) {
        chart.getData().clear();

        XYChart.Series<Number, Number> easySeries = new XYChart.Series<>();
        easySeries.setName("Easy");
        addSeriesData(easySeries, stats.easyScores);

        XYChart.Series<Number, Number> medSeries = new XYChart.Series<>();
        medSeries.setName("Medium");
        addSeriesData(medSeries, stats.mediumScores);

        XYChart.Series<Number, Number> hardSeries = new XYChart.Series<>();
        hardSeries.setName("Hard");
        addSeriesData(hardSeries, stats.hardScores);

        chart.getData().addAll(easySeries, medSeries, hardSeries);
    }

    private void addSeriesData(XYChart.Series<Number, Number> series, int[] scores) {
        if (scores == null) return;
        for (int i = 0; i < scores.length; i++) {
            series.getData().add(new XYChart.Data<>(i + 1, scores[i]));
        }
    }

    private void setupDonut(PieChart chart, Label label, double percent) {
        if (chart == null || label == null) return;

        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Filled",    percent),
                new PieChart.Data("Remaining", 100.0 - percent)
        );
        chart.setData(data);
        label.setText(String.format("%.0f%%", percent));
    }

    private String formatDuration(int durationSeconds) {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
