package control;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import model.Player;
import model.PlayerStats;
import model.SysData;
import util.DialogUtil;
import util.SessionManager;
import util.SoundManager;


public class StatsViewController {

    private static final Logger LOG = Logger.getLogger(StatsViewController.class.getName());

    @FXML private ScrollPane mainPane;

    @FXML private ImageView p1avatar;
    @FXML private Text      p1OfficialNameText;
    @FXML private ImageView p2avatar;
    @FXML private Text      p2OfficialNameText;

    @FXML private Text numOfWins;
    @FXML private Text numOfLosses;
    @FXML private Text numOfGiveUps;

    @FXML private Text numOfWins2;
    @FXML private Text numOfLosses2;
    @FXML private Text numOfGiveUps2;

    @FXML private HBox headerHBox;

    @FXML private PieChart p1winsChart;
    @FXML private PieChart p1lossesChart;
    @FXML private PieChart p1giveUpsChart;
    @FXML private PieChart p1winsWithNoMistakesChart;

    @FXML private PieChart p2winsChart;
    @FXML private PieChart p2lossesChart;
    @FXML private PieChart p2giveUpsChart;
    @FXML private PieChart p2winsWithNoMistakesChart;

    @FXML private Label p1winsLabel;
    @FXML private Label p1lossesLabel;
    @FXML private Label p1giveUpsLabel;
    @FXML private Label p1winsWithNoMistakesLabel;

    @FXML private Label p2winsLabel;
    @FXML private Label p2lossesLabel;
    @FXML private Label p2giveUpsLabel;
    @FXML private Label p2winsWithNoMistakesLabel;

    @FXML private Label p1TotalGamesLabel;
    @FXML private Label p1BestScoreLabel;
    @FXML private Label p1BestScoreWithLabel;
    @FXML private Label p1BestTimeLabel;
    @FXML private Label p1BestTimeWithLabel;

    @FXML private Label p2TotalGamesLabel;
    @FXML private Label p2BestScoreLabel;
    @FXML private Label p2BestScoreWithLabel;
    @FXML private Label p2BestTimeLabel;
    @FXML private Label p2BestTimeWithLabel;

    @FXML private LineChart<Number, Number> p1ProgressChart;
    @FXML private LineChart<Number, Number> p2ProgressChart;
    
    private static final String DEFAULT_AVATAR_RES = "/Images/S5.png";
    private URL defaultAvatarUrl;

    private Stage resolveStage() {
        if (mainPane != null && mainPane.getScene() != null) {
            return (Stage) mainPane.getScene().getWindow();
        }
        return null;
    }

    public ScrollPane getMainPane() {
        return mainPane;
    }
    public static final class PlayerStatsData {

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

            this.easyScores   = (easyScores   != null) ? easyScores   : new int[0];
            this.mediumScores = (mediumScores != null) ? mediumScores : new int[0];
            this.hardScores   = (hardScores   != null) ? hardScores   : new int[0];
        }

        public static PlayerStatsData from(PlayerStats s) {
        	  if (s == null) {
        	        return new PlayerStatsData("-", null, 0,0,0,0,0, 0,"-", 0,"-", null,null,null);
        	    }
            return new PlayerStatsData(
                    s.playerName,
                    s.avatarId,
                    s.totalGames,
                    s.wins,
                    s.losses,
                    s.giveUps,
                    s.winsWithNoMistakes,
                    s.bestScore,
                    s.bestScoreOpponent,
                    s.bestTimeSeconds,
                    s.bestTimeOpponent,
                    s.easyScores,
                    s.mediumScores,
                    s.hardScores
            );
        }
    }

    @FXML
    private void initialize() {
        try {
            SysData.getInstance().ensureHistoryLoaded();

            configureProgressChart(p1ProgressChart, "Player 1 Progress");
            configureProgressChart(p2ProgressChart, "Player 2 Progress");
            configureDonutChartsDefaults();

            // Render both players with minimal duplication
            renderPlayerStats(
                    SessionManager.getPlayer1(),  p1OfficialNameText, p1avatar, numOfWins, /*losses*/ numOfLosses, /*giveups*/ numOfGiveUps, p1winsChart, p1winsLabel,
                    p1lossesChart, p1lossesLabel, p1giveUpsChart, p1giveUpsLabel,  p1winsWithNoMistakesChart, p1winsWithNoMistakesLabel, p1TotalGamesLabel,
                    p1BestScoreLabel, p1BestScoreWithLabel, p1BestTimeLabel, p1BestTimeWithLabel, p1ProgressChart
            );

            renderPlayerStats(
                    SessionManager.getPlayer2(), p2OfficialNameText, p2avatar, numOfWins2, /*losses*/ numOfLosses2, /*giveups*/ numOfGiveUps2, p2winsChart, p2winsLabel,
                    p2lossesChart, p2lossesLabel, p2giveUpsChart, p2giveUpsLabel, p2winsWithNoMistakesChart, p2winsWithNoMistakesLabel, p2TotalGamesLabel,
                    p2BestScoreLabel, p2BestScoreWithLabel, p2BestTimeLabel, p2BestTimeWithLabel, p2ProgressChart
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "StatsView initialize failed", e);
            DialogUtil.show(AlertType.ERROR, "", "Statistics screen failed to load",
                    "An unexpected error occurred while loading the statistics screen.");
        }
    }

    // Renders one player's stats into a set of UI controls
    private void renderPlayerStats(Player model,
                                   Text officialNameText,
                                   ImageView avatarView,
                                   Text winsText,
                                   Text lossesText,
                                   Text giveUpsText,
                                   PieChart winsChart, Label winsLabel,
                                   PieChart lossesChart, Label lossesLabel,
                                   PieChart giveUpsChart, Label giveUpsLabel,
                                   PieChart winsNoMistakesChart, Label winsNoMistakesLabel,
                                   Label totalGamesLabel,
                                   Label bestScoreLabel, Label bestScoreWithLabel,
                                   Label bestTimeLabel, Label bestTimeWithLabel,
                                   LineChart<Number, Number> progressChart) {

        // If player is missing (for example, single player mode or not selected), show safe defaults.
        if (model == null) {
            applyEmptyState(
                    officialNameText, avatarView,
                    winsText, lossesText, giveUpsText,
                    winsChart, winsLabel,
                    lossesChart, lossesLabel,
                    giveUpsChart, giveUpsLabel,
                    winsNoMistakesChart, winsNoMistakesLabel,
                    totalGamesLabel,
                    bestScoreLabel, bestScoreWithLabel,
                    bestTimeLabel, bestTimeWithLabel,
                    progressChart
            );
            return;
        }

        SysData sys = SysData.getInstance();
        PlayerStats raw = sys.computeStatsForPlayer(model);
        PlayerStatsData stats = PlayerStatsData.from(raw);

        // Apply the data
        if (officialNameText != null) officialNameText.setText(stats.playerName);

        if (avatarView != null) {
            Image img = loadAvatarImage(stats.avatarImagePath);
            if (img != null) avatarView.setImage(img);
        }

        if (winsText != null) winsText.setText(String.valueOf(stats.wins));
        if (lossesText != null) lossesText.setText(String.valueOf(stats.losses));
        if (giveUpsText != null) giveUpsText.setText(String.valueOf(stats.giveUps));

        applyStatsToUi(
                stats,
                winsChart, winsLabel,
                lossesChart, lossesLabel,
                giveUpsChart, giveUpsLabel,
                winsNoMistakesChart, winsNoMistakesLabel,
                totalGamesLabel,
                bestScoreLabel, bestScoreWithLabel,
                bestTimeLabel, bestTimeWithLabel
        );

        if (progressChart != null) {
            updateProgressChart(progressChart, stats);
        }
    }

    private void applyEmptyState(Text officialNameText,
                                 ImageView avatarView,
                                 Text winsText,
                                 Text lossesText,
                                 Text giveUpsText,
                                 PieChart winsChart, Label winsLabel,
                                 PieChart lossesChart, Label lossesLabel,
                                 PieChart giveUpsChart, Label giveUpsLabel,
                                 PieChart winsNoMistakesChart, Label winsNoMistakesLabel,
                                 Label totalGamesLabel,
                                 Label bestScoreLabel, Label bestScoreWithLabel,
                                 Label bestTimeLabel, Label bestTimeWithLabel,
                                 LineChart<Number, Number> progressChart) {

        if (officialNameText != null) officialNameText.setText("-");

        if (avatarView != null) {
            Image img = loadAvatarImage(null);
            if (img != null) avatarView.setImage(img);
        }

        if (winsText != null) winsText.setText("0");
        if (lossesText != null) lossesText.setText("0");
        if (giveUpsText != null) giveUpsText.setText("0");

        // 0% donuts
        setupDonut(winsChart, winsLabel, 0);
        setupDonut(lossesChart, lossesLabel, 0);
        setupDonut(giveUpsChart, giveUpsLabel, 0);
        setupDonut(winsNoMistakesChart, winsNoMistakesLabel, 0);

        if (totalGamesLabel != null) totalGamesLabel.setText("0");

        if (bestScoreLabel != null) bestScoreLabel.setText("best score:    0");
        if (bestScoreWithLabel != null) bestScoreWithLabel.setText("with:       -");

        if (bestTimeLabel != null) bestTimeLabel.setText("best time:  0:00");
        if (bestTimeWithLabel != null) bestTimeWithLabel.setText("With:      -");

        if (progressChart != null) progressChart.getData().clear();
    }

    private void applyStatsToUi(PlayerStatsData stats,
                                PieChart winsChart, Label winsLabel,
                                PieChart lossesChart, Label lossesLabel,
                                PieChart giveUpsChart, Label giveUpsLabel,
                                PieChart winsNoMistakesChart, Label winsNoMistakesLabel,
                                Label totalGamesLabel,
                                Label bestScoreLabel, Label bestScoreWithLabel,
                                Label bestTimeLabel, Label bestTimeWithLabel) {

        if (stats == null) return;

        int totalGames = stats.totalGames;
        double winPercent = 0;
        double lossesPercent = 0;
        double giveUpsPercent = 0;
        double winsNoMistakesPercent = 0;

        if (totalGames > 0) {
            winPercent            = stats.wins               * 100.0 / totalGames;
            lossesPercent         = stats.losses             * 100.0 / totalGames;
            giveUpsPercent        = stats.giveUps            * 100.0 / totalGames;
            winsNoMistakesPercent = stats.winsWithNoMistakes * 100.0 / totalGames;
        }

        setupDonut(winsChart, winsLabel, winPercent);
        setupDonut(lossesChart, lossesLabel, lossesPercent);
        setupDonut(giveUpsChart, giveUpsLabel, giveUpsPercent);
        setupDonut(winsNoMistakesChart, winsNoMistakesLabel, winsNoMistakesPercent);

        if (totalGamesLabel != null) totalGamesLabel.setText(String.valueOf(stats.totalGames));

        if (bestScoreLabel != null) bestScoreLabel.setText("best score:    " + stats.bestScore);
        if (bestScoreWithLabel != null) bestScoreWithLabel.setText("with:       " + safe(stats.bestScoreOpponent));

        if (bestTimeLabel != null) bestTimeLabel.setText("best time:  " + formatDuration(stats.bestTimeSeconds));
        if (bestTimeWithLabel != null) bestTimeWithLabel.setText("With:      " + safe(stats.bestTimeOpponent));
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
        
        chart.getData().add(easySeries);
        chart.getData().add(medSeries);
        chart.getData().add(hardSeries);

    }

    private void addSeriesData(XYChart.Series<Number, Number> series, int[] scores) {
        if (scores == null) return;
        for (int i = 0; i < scores.length; i++) {
            series.getData().add(new XYChart.Data<>(i + 1, scores[i]));
        }
    }

    // Creates a donut-style chart by putting the % as "Filled" and the rest as "Remaining".
    private void setupDonut(PieChart chart, Label label, double percent) {
        if (chart == null || label == null) return;

        double p = clampPercent(percent);

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Filled", p),
                new PieChart.Data("Remaining", 100.0 - p)
        );

        chart.setData(data);
        label.setText(String.format("%.0f%%", p));
    }

    private double clampPercent(double p) {
        if (Double.isNaN(p) || Double.isInfinite(p)) return 0;
        if (p < 0) return 0;
        if (p > 100) return 100;
        return p;
    }

    private String formatDuration(int durationSeconds) {
        int minutes = Math.max(0, durationSeconds) / 60;
        int seconds = Math.max(0, durationSeconds) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Avatar loading rules:
     *  null/blank -> default resource
     *  file:...   -> load from URI
     *  startsWith("/") -> load as resource path
     *  else -> treat as filename under /Images/
     */
    	private Image loadAvatarImage(String avatarId) {
    	    try {
    	        if (avatarId == null || avatarId.isBlank()) {
    	            return loadDefaultAvatar();
    	        }

    	        if (avatarId.startsWith("file:")) {
    	            return new Image(avatarId, true);
    	        }

    	        if (avatarId.startsWith("/")) {
    	            URL url = getClass().getResource(avatarId);
    	            if (url != null) return new Image(url.toExternalForm(), true);
    	        }

    	        URL url = getClass().getResource("/Images/" + avatarId);
    	        if (url != null) return new Image(url.toExternalForm(), true);

    	        return loadDefaultAvatar();

    	    } catch (Exception e) {
    	        LOG.log(Level.WARNING, "Failed to load avatar: " + avatarId, e);
    	        return loadDefaultAvatar();
    	    }
    	}

    	private Image loadDefaultAvatar() {
    	    try {
    	        if (defaultAvatarUrl == null) {
    	            defaultAvatarUrl = getClass().getResource(DEFAULT_AVATAR_RES);
    	            if (defaultAvatarUrl == null) {
    	                LOG.severe("Missing default avatar resource: " + DEFAULT_AVATAR_RES);
    	                return null;
    	            }
    	        }
    	        return new Image(defaultAvatarUrl.toExternalForm(), true);

    	    } catch (Exception e) {
    	        LOG.log(Level.SEVERE, "Failed to load default avatar: " + DEFAULT_AVATAR_RES, e);
    	        return null;
    	    }
    	}
    


  
     // Progress chart configuration.
    private void configureProgressChart(LineChart<Number, Number> chart, String title) {
        if (chart == null) return;

        chart.setTitle(title);

        if (chart.getXAxis() instanceof NumberAxis x) {
            x.setLabel("Game # (for this difficulty)");
            x.setForceZeroInRange(false);
            x.setAutoRanging(true);
        }

        if (chart.getYAxis() instanceof NumberAxis y) {
            y.setLabel("Score");
            y.setForceZeroInRange(false);
            y.setAutoRanging(true);
        }

        chart.setCreateSymbols(true);
        chart.setAlternativeRowFillVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setVerticalZeroLineVisible(false);
        chart.setHorizontalZeroLineVisible(false);

        chart.setLegendVisible(true);
        chart.setAnimated(false);
    }

    private void configureDonutChartsDefaults() {
        configureDonutChart(p1winsChart);
        configureDonutChart(p1lossesChart);
        configureDonutChart(p1giveUpsChart);
        configureDonutChart(p1winsWithNoMistakesChart);

        configureDonutChart(p2winsChart);
        configureDonutChart(p2lossesChart);
        configureDonutChart(p2giveUpsChart);
        configureDonutChart(p2winsWithNoMistakesChart);
    }

    private void configureDonutChart(PieChart chart) {
        if (chart == null) return;
        chart.setAnimated(false);
        chart.setLabelsVisible(false);
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    @FXML
    private void onBackToMainClicked() {
        SoundManager.playClick();

        Stage s = resolveStage();
        if (s == null) {
            DialogUtil.show(AlertType.ERROR, "", "Navigation failed",
                    "Could not determine the application window (Stage).");
            return;
        }

        try {
            util.ViewNavigator.switchTo(s, "/view/main_view.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to navigate back to main_view.fxml", e);
            DialogUtil.show(AlertType.ERROR, "", "Navigation failed",
                    "Could not return to the main screen. Please try again.");
        }
    }

}
