package control;

import model.Board;
import model.Cell;
import model.Difficulty;
import model.GameConfig;
import view.MainView;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

public class GameController {

    @FXML private GridPane player1Grid;
    @FXML private GridPane player2Grid;
    @FXML private Label player1BombsLeftLabel;
    @FXML private Label player2BombsLeftLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label timeLabel;
    @FXML private Label scoreLabel;

    private Difficulty difficulty;
    private Board board1;
    private Board board2;
    private Stage stage;

    
    private Button[][] p1Buttons;
    private Button[][] p2Buttons;


    private GameConfig config;

    private int sharedHearts;
    private int score;
    private int minesLeft1;
    private int minesLeft2;
   

    /**
     * Called by NewGameController after FXML is loaded.
     */
    public void init(GameConfig config) {
    	
    	System.out.println("GameController.init called");
        System.out.println("player1Grid = " + player1Grid);
        System.out.println("player2Grid = " + player2Grid);
        
        this.config = config;
        this.difficulty = config.getDifficulty();

        this.board1 = new Board(difficulty);
        this.board2 = new Board(difficulty);
        this.minesLeft1= board1.getMineCount();
        this.minesLeft2= board2.getMineCount();


        this.sharedHearts = difficulty.getInitialLives();
        this.score = 0;

        initLabels();
        buildGridForPlayer(player1Grid, board1, true);
        buildGridForPlayer(player2Grid, board2, false);
    }

    private void initLabels() {
        difficultyLabel.setText("Difficulty: " +
                difficulty.name().charAt(0) +
                difficulty.name().substring(1).toLowerCase());

        timeLabel.setText("Time: 00:00");

        player1BombsLeftLabel.setText(
                config.getPlayer1Nickname() + ", Mines left: " + board1.getMineCount()
        );
        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + board2.getMineCount()
        );
        
        
        scoreLabel.setText("Score: " + score);
    }

    private void buildGridForPlayer(GridPane grid, Board board, boolean isPlayer1) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        int rows = board.getRows();
        int cols = board.getCols();

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

        Button[][] buttons = new Button[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Button cellButton = createCellButton(board, r, c, isPlayer1);
                buttons[r][c] = cellButton;
                grid.add(cellButton, c, r);
            }
        }

        if (isPlayer1) {
            p1Buttons = buttons;
        } else {
            p2Buttons = buttons;
        }
    }

    private Button createCellButton(Board board, int row, int col, boolean isPlayer1) {
        Button button = new Button();
        button.setMinSize(0, 0);
        button.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        GridPane.setHgrow(button, Priority.ALWAYS);
        GridPane.setVgrow(button, Priority.ALWAYS);

        button.getStyleClass().addAll("cell-tile", "cell-hidden");

        final int r = row;
        final int c = col;

        button.setOnMouseClicked(event -> {
            handleCellClick(board, r, c, button, isPlayer1);
            
            

          
        });

        return button;
    }


    private void handleCellClick(Board board, int row, int col, Button button, boolean isPlayer1) {
        Cell cell = board.getCell(row, col);

        // remove all possible state classes
        button.getStyleClass().removeAll(
            "cell-hidden", "cell-revealed",
            "cell-mine", "cell-question",
            "cell-surprise", "cell-number", "cell-empty"
        );

        switch (cell.getType()) {
            case MINE -> {
                button.setText("💣");
                button.getStyleClass().addAll("cell-revealed", "cell-mine");
                sharedHearts = Math.max(0, sharedHearts - 1);
                if (isPlayer1) {
                	 minesLeft1 -= 1;
                }
                else {
               	 	 minesLeft2 -= 1;

                }
            }
            case QUESTION -> {
                button.setText("?");
                button.getStyleClass().addAll("cell-revealed", "cell-question");
                score += 0;
            }
            case SURPRISE -> {
                button.setText("★");
                button.getStyleClass().addAll("cell-revealed", "cell-surprise");
                score += 2;
            }
            case NUMBER -> {
                int n = cell.getAdjacentMines();
                button.setText(String.valueOf(n));
                button.setDisable(true);
                button.getStyleClass().addAll("cell-revealed", "cell-number");
                score += 1;
            }
            case EMPTY -> {
                button.setText("");
                button.setDisable(true);
                button.getStyleClass().addAll("cell-revealed", "cell-empty");
                score += 1;
            }
        }

        
        scoreLabel.setText("Score: " + score);
        
        player1BombsLeftLabel.setText(
                config.getPlayer1Nickname() + ", Mines left: " + minesLeft1
        );
        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + minesLeft2
        );
        
    }


    @FXML
    private void onExitBtnClicked() {
        System.out.println("Exit clicked -> Exit whole system (need to edit!!):)");
        System.exit(0);
    }

    @FXML
    private void onHelpBtnClicked() {
        System.out.println("Help clicked but screen not created yet!");
    }
    
    @FXML
    private void onBackBtnClicked() throws IOException {
        Stage stage = (Stage) player1Grid.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/new_game_view.fxml"));
        Parent root = loader.load();

        NewGameController newGameController = loader.getController();
        newGameController.setStage(stage);  // if you need stage inside NewGameController

        stage.setScene(new Scene(root));
        stage.show();
    }

}
