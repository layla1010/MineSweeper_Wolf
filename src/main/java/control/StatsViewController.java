package control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;


public class StatsViewController {
	
	private Stage stage;
	@FXML private ScrollPane mainPane;
	
	public ScrollPane getMainPane () {
		return mainPane;
	}

    public static class PlayerStatsData {
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


        public PlayerStatsData(int totalGames,
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

            this.totalGames = Math.max(0, totalGames);
            this.wins = Math.max(0, wins);
            this.losses = Math.max(0, losses);
            this.giveUps = Math.max(0, giveUps);
            this.winsWithNoMistakes = Math.max(0, winsWithNoMistakes);
            this.bestScore = bestScore;
            this.bestScoreOpponent = bestScoreOpponent;
            this.bestTimeSeconds = Math.max(0, bestTimeSeconds);
            this.bestTimeOpponent = bestTimeOpponent;
            this.easyScores   = easyScores != null ? easyScores : new int[0];
            this.mediumScores = mediumScores != null ? mediumScores : new int[0];
            this.hardScores   = hardScores != null ? hardScores : new int[0];
        }
    }

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

    // Dummy:
    private static final boolean USE_DUMMY_DATA = true;

    @FXML
    private void initialize() {
        if (USE_DUMMY_DATA) {
        	PlayerStatsData p1 = new PlayerStatsData(
        	        20, 10, 5, 3, 2,
        	        1200, "Alice",
        	        135, "Bob",
        	        new int[]{500, 650, 700, 800},
        	        new int[]{400, 550, 600},
        	        new int[]{300, 450, 500} 
        	        );
        	
        	PlayerStatsData p2 = new PlayerStatsData(
        	        15, 8, 4, 3, 1,
        	        900, "Charlie",
        	        200, "Dana",
        	        new int[]{200, 300, 350}, 
        	        new int[]{100, 250},
        	        new int[]{50, 150, 200, 250}
        	        );
        	
            setPlayer1Stats(p1);
            setPlayer2Stats(p2);
        }
    }


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
    }

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
    }


    private void applyStatsToUi(PlayerStatsData stats,
                                PieChart winsChart,   Label winsLabel,
                                PieChart lossesChart, Label lossesLabel,
                                PieChart giveUpsChart, Label giveUpsLabel,
                                PieChart winsNoMistakesChart, Label winsNoMistakesLabel,
                                Label totalGamesLabel,
                                Label bestScoreLabel, Label bestScoreWithLabel,
                                Label bestTimeLabel, Label bestTimeWithLabel) {

        if (stats == null) {
            return;
        }

        int totalOutcomes = stats.wins + stats.losses + stats.giveUps + stats.winsWithNoMistakes;
        double winPercent = 0, lossesPercent = 0, giveUpsPercent = 0, winsNoMistakesPercent = 0;

        if (totalOutcomes > 0) {
            winPercent            = stats.wins               * 100.0 / totalOutcomes;
            lossesPercent         = stats.losses             * 100.0 / totalOutcomes;
            giveUpsPercent        = stats.giveUps            * 100.0 / totalOutcomes;
            winsNoMistakesPercent = stats.winsWithNoMistakes * 100.0 / totalOutcomes;
        }

        setupDonut(winsChart,            winsLabel,            winPercent);
        setupDonut(lossesChart,          lossesLabel,          lossesPercent);
        setupDonut(giveUpsChart,         giveUpsLabel,         giveUpsPercent);
        setupDonut(winsNoMistakesChart,  winsNoMistakesLabel,  winsNoMistakesPercent);

        if (totalGamesLabel != null) {
            totalGamesLabel.setText(String.valueOf(stats.totalGames));
        }

        if (bestScoreLabel != null) {
            bestScoreLabel.setText("best score:    " + String.valueOf(stats.bestScore));
        }
        if (bestScoreWithLabel != null) {
            String opponent = (stats.bestScoreOpponent == null || stats.bestScoreOpponent.isBlank())
                    ? "-"
                    : stats.bestScoreOpponent;
            bestScoreWithLabel.setText("with:       " + opponent);
        }

        if (bestTimeLabel != null) {
            bestTimeLabel.setText("best time:  " + formatDuration(stats.bestTimeSeconds));
        }
        if (bestTimeWithLabel != null) {
            String opponent = (stats.bestTimeOpponent == null || stats.bestTimeOpponent.isBlank())
                    ? "-"
                    : stats.bestTimeOpponent;
            bestTimeWithLabel.setText("With:      " + opponent);
        }
        if (winsChart == p1winsChart && p1ProgressChart != null) {
            updateProgressChart(p1ProgressChart, stats);
        } else if (winsChart == p2winsChart && p2ProgressChart != null) {
            updateProgressChart(p2ProgressChart, stats);
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
        if (chart == null || label == null) {
            return;
        }

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


	public void setStage(Stage stage) {
        this.stage = stage;
		
	}


	
}
