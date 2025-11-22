package controler;

import model.Board;
import model.Cell;
import model.CellType;
import model.Difficulty;
import model.GameConfig;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

public class GameController {

    //IDs for FXML elements
    @FXML private GridPane player1Grid;
    @FXML private GridPane player2Grid;
    @FXML private Label player1BombsLeftLabel;
    @FXML private Label player2BombsLeftLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label timeLabel;   //not yet functional, we’ll set initial text
    @FXML private Label scoreLabel;

    private Difficulty difficulty;
    private Board board1;
    private Board board2;

    private int sharedHearts;
    private int score;

    //Called automatically after FXML is loaded
    @FXML
    private void initialize() {
        GameConfig config = GameConfig.getCurrentOrDefault();
        this.difficulty = config.getDifficulty();

        this.board1 = new Board(difficulty);
        this.board2 = new Board(difficulty);

        this.sharedHearts = difficulty.getStartingHearts();
        this.score = 0;

        initLabels(config);
        buildGridForPlayer(player1Grid, board1, true);
        buildGridForPlayer(player2Grid, board2, false);
    }

    private void initLabels(GameConfig config) {
        difficultyLabel.setText("Difficulty: " + difficulty.name().charAt(0) +
                difficulty.name().substring(1).toLowerCase());
        timeLabel.setText("Time: 00:00"); //timer is not implemented yet

        player1BombsLeftLabel.setText(config.getPlayer1Name() +
                ", Bombs left: " + board1.getMineCount());
        player2BombsLeftLabel.setText(config.getPlayer2Name() +
                ", Bombs left: " + board2.getMineCount());

        scoreLabel.setText("Score: " + score);
    }

    private void buildGridForPlayer(GridPane grid, Board board, boolean isPlayer1) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        int rows = board.getRows();
        int cols = board.getCols();

        //make all cells same size (percentage)
        double colPercent = 100.0 / cols;
        double rowPercent = 100.0 / rows;

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(colPercent);
            cc.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(cc);
        }

        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(rowPercent);
            rc.setValignment(VPos.CENTER);
            grid.getRowConstraints().add(rc);
        }

        //Add buttons
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Button cellButton = createCellButton(board, r, c, isPlayer1);
                grid.add(cellButton, c, r);
            }
        }
    }

    private Button createCellButton(Board board, int row, int col, boolean isPlayer1) {
        Button button = new Button();
        button.setMinSize(25, 25);
        button.setPrefSize(25, 25);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        button.setStyle("-fx-background-color: #222; -fx-text-fill: #ffffff;");

        button.setOnAction(event -> handleCellClick(board, row, col, button, isPlayer1));
        return button;
    }

    private void handleCellClick(Board board, int row, int col, Button button, boolean isPlayer1) {
        Cell cell = board.getCell(row, col);

        switch (cell.getType()) {
            case MINE -> {
                button.setText("X");
                button.setStyle("-fx-background-color: red; -fx-text-fill: white;");
                sharedHearts = Math.max(0, sharedHearts - 1); //only to understand what happens to shared lives
            }
            case QUESTION -> {
                button.setText("?");
                button.setStyle("-fx-background-color: #444; -fx-text-fill: #00bfff;");
                score += 0;
            }
            case SURPRISE -> {
                button.setText("🎁");
                button.setStyle("-fx-background-color: #444; -fx-text-fill: yellow;");
                score += 2; // example rule
            }
            case NUMBER -> {
                int n = cell.getNeighborMines();
                button.setText(String.valueOf(n));
                button.setDisable(true);
                score += 1;
            }
            case EMPTY -> {
                button.setText("");
                button.setDisable(true);
                score += 1;
            }
        }

        scoreLabel.setText("Score: " + score);
    }

    //Default for now
    @FXML
    private void onExitBtnClicked() {
    	System.out.println("Exit clicked but not implemented yet:)");
    	System.exit(0);
        
    }

    @FXML
    private void onHelpBtnClicked() {
        //show a dialog or help window
        System.out.println("Help clicked but not implemented yet:)");
    }
}

