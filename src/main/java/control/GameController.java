package control;

import model.Board;
import model.Cell;
import model.CellType;
import model.Difficulty;
import model.GameConfig;
import model.Game;
import model.SysData;
import model.GameResult;
import util.UIAnimations;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GameController {

    @FXML private GridPane player1Grid;
    @FXML private GridPane player2Grid;
    @FXML private Label player1BombsLeftLabel;
    @FXML private Label player2BombsLeftLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label timeLabel;
    @FXML private Label scoreLabel;
    @FXML private HBox heartsBox;
    @FXML private Button pauseBtn;
    @FXML private Button musicIsOnButton;
    @FXML private Parent root;

    private GameConfig config;
    private Difficulty difficulty;
    private Board board1;
    private Board board2;
    private StackPane[][] p1Buttons;
    private StackPane[][] p2Buttons;

    private int sharedHearts;
    private int score;
    private int minesLeft1;
    private int minesLeft2;

    private boolean isPlayer1Turn = true;
    private boolean isPaused = false;

    private ImageCursor forbiddenCursor;

    private boolean gameOver = false;
    private boolean gameWon = false;
    private Timeline timer;
    private int elapsedSeconds = 0;

    private int safeCellsRemaining1;
    private int safeCellsRemaining2;

    private static final int TOTAL_HEART_SLOTS = 10;


    public void init(GameConfig config) {
        this.config = config;
        this.difficulty = config.getDifficulty();

        this.board1 = new Board(difficulty);
        this.board2 = new Board(difficulty);

        this.minesLeft1 = board1.getMineCount();
        this.minesLeft2 = board2.getMineCount();

        int totalCells1 = board1.getRows() * board1.getCols();
        int totalCells2 = board2.getRows() * board2.getCols();
        this.safeCellsRemaining1 = totalCells1 - board1.getMineCount();
        this.safeCellsRemaining2 = totalCells2 - board2.getMineCount();

        this.sharedHearts = difficulty.getInitialLives();
        this.score = 0;
        this.isPlayer1Turn = true;
        this.isPaused = false;
        this.gameOver = false;
        this.gameWon = false;

        UIAnimations.applyHoverZoomToAllButtons(root);
        UIAnimations.applyFloatingToCards(root);

        buildHeartsBar();
        initLabels();
        buildGridForPlayer(player1Grid, board1, true);
        buildGridForPlayer(player2Grid, board2, false);

        initForbiddenCursor();
        applyTurnStateToBoards();

        elapsedSeconds = 0;
        updateTimeLabel();
        startTimer();
    }

  
    private void buildHeartsBar() {
        if (heartsBox == null) return;

        heartsBox.getChildren().clear();

        for (int i = 0; i < TOTAL_HEART_SLOTS; i++) {
            boolean isFull = i < sharedHearts;

            String imgPath = isFull ? "/Images/heart.png" : "/Images/favorite.png";
            Image img = new Image(getClass().getResourceAsStream(imgPath));

            ImageView iv = new ImageView(img);
            iv.setFitHeight(50);
            iv.setFitWidth(50);
            iv.setPreserveRatio(true);

            StackPane slot = new StackPane(iv);
            slot.setPrefSize(50, 50);
            slot.setMinSize(50, 50);
            slot.setMaxSize(50, 50);
            StackPane.setMargin(iv, new Insets(0));

            heartsBox.getChildren().add(slot);
        }
    }

    private void initLabels() {
        difficultyLabel.setText(
                "Difficulty: " +
                        difficulty.name().charAt(0) +
                        difficulty.name().substring(1).toLowerCase()
        );

        timeLabel.setText("Time: 00:00");

        player1BombsLeftLabel.setText(
                config.getPlayer1Nickname() + ", Mines left: " + board1.getMineCount()
        );
        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + board2.getMineCount()
        );

        scoreLabel.setText("Score: " + score);
    }

    private void initForbiddenCursor() {
        try {
            Image img = new Image(getClass().getResourceAsStream("/Images/cursor_forbidden.png"));
            forbiddenCursor = new ImageCursor(img, img.getWidth() / 2, img.getHeight() / 2);
        } catch (Exception e) {
            forbiddenCursor = null;
        }
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

        StackPane[][] buttons = new StackPane[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StackPane tile = createCellTile(board, r, c, isPlayer1);
                buttons[r][c] = tile;
                grid.add(tile, c, r);
            }
        }

        if (isPlayer1) {
            p1Buttons = buttons;
        } else {
            p2Buttons = buttons;
        }
    }

    private StackPane createCellTile(Board board, int row, int col, boolean isPlayer1) {
        Button button = new Button();
        button.setMinSize(0, 0);
        button.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        button.getStyleClass().addAll("cell-tile", "cell-hidden");

        StackPane tile = new StackPane(button);
        tile.setMinSize(0, 0);
        tile.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        GridPane.setHgrow(tile, Priority.ALWAYS);
        GridPane.setVgrow(tile, Priority.ALWAYS);

        final int r = row;
        final int c = col;
        final boolean tileIsPlayer1 = isPlayer1;

        button.setOnMouseClicked(e -> {
            if (gameOver) {
                return;
            }
            if (isPaused) {
                return;
            }

            if (e.getButton() == MouseButton.SECONDARY) {
                if ((tileIsPlayer1 && !isPlayer1Turn) ||
                        (!tileIsPlayer1 && isPlayer1Turn)) {
                    return;
                }

                if (button.isDisable()) {
                    return;
                }

                toggleFlag(button);
                return;
            }

            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }

            if ((tileIsPlayer1 && !isPlayer1Turn) ||
                    (!tileIsPlayer1 && isPlayer1Turn)) {
                return;
            }

            if (button.isDisable()) {
                return;
            }

            boolean consumedAction = handleCellClick(board, r, c, button, tile, tileIsPlayer1);
            if (consumedAction) {
                switchTurn();
            }
        });

        return tile;
    }

    private void toggleFlag(Button button) {
        if (button.getGraphic() instanceof ImageView) {
            button.setGraphic(null);
            button.getStyleClass().remove("cell-flagged");
            return;
        }

        try {
            Image img = new Image(getClass().getResourceAsStream("/Images/red-flag.png"));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(20);
            iv.setFitHeight(20);
            iv.setPreserveRatio(true);
            button.setGraphic(iv);
        } catch (Exception ex) {
            button.setText("🚩");
        }
        if (!button.getStyleClass().contains("cell-flagged")) {
            button.getStyleClass().add("cell-flagged");
        }
    }

    private void revealSingleCell(Board board,
                                  int row,
                                  int col,
                                  Button button,
                                  StackPane tile,
                                  boolean isPlayer1) {

        Cell cell = board.getCell(row, col);

        if (button.isDisable()) {
            return;
        }

        if (button.getGraphic() instanceof ImageView) {
            button.setGraphic(null);
        }
        button.getStyleClass().remove("cell-flagged");

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
                triggerExplosion(tile);

                if (isPlayer1) {
                    minesLeft1 -= 1;
                } else {
                    minesLeft2 -= 1;
                }
                buildHeartsBar();

                if (sharedHearts == 0 && !gameOver) {
                    gameWon = false;
                    onGameOver();
                }
            }
            case QUESTION -> {
                try {
                    Image img = new Image(getClass().getResourceAsStream("/Images/question-mark.png"));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(20);
                    iv.setFitHeight(20);
                    iv.setPreserveRatio(true);
                    button.setGraphic(iv);
                    button.getStyleClass().addAll("cell-revealed", "cell-question");
                    score += 0;
                } catch (Exception ex) {
                    button.setText("?");
                    button.getStyleClass().addAll("cell-revealed", "cell-question");
                    score += 0;
                }
            }
            case SURPRISE -> {
                try {
                    Image img = new Image(getClass().getResourceAsStream("/Images/giftbox.png"));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(20);
                    iv.setFitHeight(20);
                    iv.setPreserveRatio(true);
                    button.setGraphic(iv);
                    button.getStyleClass().addAll("cell-revealed", "cell-surprise");
                    score += 2;
                } catch (Exception ex) {
                    button.setText("★");
                    button.getStyleClass().addAll("cell-revealed", "cell-surprise");
                    score += 2;
                }
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

        if (cell.getType() != CellType.MINE) {
            if (isPlayer1) {
                safeCellsRemaining1 = Math.max(0, safeCellsRemaining1 - 1);
            } else {
                safeCellsRemaining2 = Math.max(0, safeCellsRemaining2 - 1);
            }

            if (!gameOver && sharedHearts > 0 &&
                    (safeCellsRemaining1 == 0 || safeCellsRemaining2 == 0)) {
                gameWon = true;
                onGameOver();
            }
        }

        updateScoreAndMineLabels();
    }

    private boolean handleCellClick(Board board,
                                    int row,
                                    int col,
                                    Button button,
                                    StackPane tile,
                                    boolean isPlayer1) {

        Cell cell = board.getCell(row, col);

        revealSingleCell(board, row, col, button, tile, isPlayer1);

        if (cell.getType() == CellType.EMPTY) {
            cascadeReveal(board, row, col, isPlayer1);
        }

        return true;
    }

    private void cascadeReveal(Board board, int startRow, int startCol, boolean isPlayer1) {

        StackPane[][] buttons = isPlayer1 ? p1Buttons : p2Buttons;
        int rows = board.getRows();
        int cols = board.getCols();

        boolean[][] visited = new boolean[rows][cols];
        java.util.Deque<int[]> stack = new java.util.ArrayDeque<>();
        stack.push(new int[]{startRow, startCol});

        while (!stack.isEmpty()) {
            int[] rc = stack.pop();
            int r = rc[0];
            int c = rc[1];

            if (r < 0 || c < 0 || r >= rows || c >= cols) continue;
            if (visited[r][c]) continue;
            visited[r][c] = true;

            Cell cell = board.getCell(r, c);

            if (cell.getType() == CellType.MINE) {
                continue;
            }

            StackPane tile = buttons[r][c];
            if (tile == null || tile.getChildren().isEmpty()) continue;
            Button btn = (Button) tile.getChildren().get(0);

            revealSingleCell(board, r, c, btn, tile, isPlayer1);

            if (cell.getType() == CellType.EMPTY ||
                    (cell.getType() == CellType.NUMBER && cell.getAdjacentMines() == 0)) {

                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr < 0 || nc < 0 || nr >= rows || nc >= cols) continue;
                        if (!visited[nr][nc]) {
                            stack.push(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
    }

    private void updateScoreAndMineLabels() {
        scoreLabel.setText("Score: " + score);

        player1BombsLeftLabel.setText(
                config.getPlayer1Nickname() + ", Mines left: " + minesLeft1
        );
        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + minesLeft2
        );
    }

   
    private void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
        applyTurnStateToBoards();
    }

    private void applyTurnStateToBoards() {
        if (player1Grid == null || player2Grid == null) return;

        if (isPlayer1Turn) {
            setBoardActive(player1Grid, player1BombsLeftLabel);
            setBoardInactive(player2Grid, player2BombsLeftLabel);
        } else {
            setBoardInactive(player1Grid, player1BombsLeftLabel);
            setBoardActive(player2Grid, player2BombsLeftLabel);
        }
    }

    private void setBoardActive(GridPane grid, Label label) {
        grid.setDisable(false);

        grid.getStyleClass().remove("inactive-board");
        if (!grid.getStyleClass().contains("active-board")) {
            grid.getStyleClass().add("active-board");
        }

        grid.setCursor(Cursor.HAND);

        label.getStyleClass().remove("inactive-player-label");
        if (!label.getStyleClass().contains("active-player-label")) {
            label.getStyleClass().add("active-player-label");
        }
    }

    private void setBoardInactive(GridPane grid, Label label) {
        grid.setDisable(true);

        grid.getStyleClass().remove("active-board");
        if (!grid.getStyleClass().contains("inactive-board")) {
            grid.getStyleClass().add("inactive-board");
        }

        if (forbiddenCursor != null) {
            grid.setCursor(forbiddenCursor);
        } else {
            grid.setCursor(Cursor.DEFAULT);
        }

        label.getStyleClass().remove("active-player-label");
        if (!label.getStyleClass().contains("inactive-player-label")) {
            label.getStyleClass().add("inactive-player-label");
        }
    }

   
    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }

        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (!isPaused && !gameOver) {
                        elapsedSeconds++;
                        updateTimeLabel();
                    }
                })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void pauseTimer() {
        if (timer != null) {
            timer.pause();
        }
    }

    private void resumeTimer() {
        if (timer != null) {
            timer.play();
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void updateTimeLabel() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timeLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

 
    @FXML
    private void onExitBtnClicked() {
        stopTimer();
        System.exit(0);
    }

    @FXML
    private void onHelpBtnClicked() {
        System.out.println("Help clicked but screen not created yet!");
    }

    @FXML
    private void onBackBtnClicked() throws IOException {
        stopTimer();

        Stage stage = (Stage) player1Grid.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setStage(stage);

        stage.setScene(new Scene(root));
        stage.show();
        stage.centerOnScreen();
    }

    @FXML
    private void onPauseGame() {
        isPaused = !isPaused;

        if (pauseBtn != null && pauseBtn.getGraphic() instanceof ImageView iv) {
            String iconPath = isPaused ? "/Images/play-button.png"
                    : "/Images/pause.png";
            Image img = new Image(getClass().getResourceAsStream(iconPath));
            iv.setImage(img);
        }

        double opacity = isPaused ? 0.6 : 1.0;
        if (player1Grid != null) player1Grid.setOpacity(opacity);
        if (player2Grid != null) player2Grid.setOpacity(opacity);

        if (isPaused) {
            pauseTimer();
        } else {
            resumeTimer();
        }
    }

    @FXML
    private void onSoundOff() {
        util.SoundManager.toggleMusic();

        if (musicIsOnButton != null && musicIsOnButton.getGraphic() instanceof ImageView iv) {
            String iconPath = util.SoundManager.isMusicOn()
                    ? "/Images/volume.png"
                    : "/Images/mute.png";

            Image img = new Image(getClass().getResourceAsStream(iconPath));
            iv.setImage(img);
        }
    }
    
    @FXML
    private void onMainMenu() {
    
    }

   
    private void triggerExplosion(StackPane tilePane) {
        double centerX = tilePane.getWidth() / 2.0;
        double centerY = tilePane.getHeight() / 2.0;

        Circle flash = new Circle(20, Color.ORANGERED);
        flash.setOpacity(0.8);
        flash.setCenterX(centerX);
        flash.setCenterY(centerY);
        tilePane.getChildren().add(flash);

        FadeTransition flashFade = new FadeTransition(Duration.millis(200), flash);
        flashFade.setFromValue(0.8);
        flashFade.setToValue(0);

        Circle shockwave = new Circle(0, Color.TRANSPARENT);
        shockwave.setStroke(Color.YELLOW);
        shockwave.setStrokeWidth(3);
        shockwave.setCenterX(centerX);
        shockwave.setCenterY(centerY);
        tilePane.getChildren().add(shockwave);

        Timeline shockExpand = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shockwave.radiusProperty(), 0),
                        new KeyValue(shockwave.opacityProperty(), 1)
                ),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(shockwave.radiusProperty(), 40),
                        new KeyValue(shockwave.opacityProperty(), 0)
                )
        );

        List<Rectangle> debrisList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Rectangle debris = new Rectangle(4, 4, Color.DARKGRAY);
            debris.setTranslateX(centerX);
            debris.setTranslateY(centerY);
            tilePane.getChildren().add(debris);
            debrisList.add(debris);

            TranslateTransition debrisFly = new TranslateTransition(Duration.millis(600), debris);
            debrisFly.setByX((Math.random() - 0.5) * 100);
            debrisFly.setByY((Math.random() - 0.5) * 100);
            debrisFly.setInterpolator(Interpolator.EASE_OUT);
            debrisFly.play();
        }

        flashFade.play();
        shockExpand.play();

        shockExpand.setOnFinished(e -> {
            tilePane.getChildren().removeAll(flash, shockwave);
            tilePane.getChildren().removeAll(debrisList);
        });
    }

  
    private void onGameOver() {
        if (gameOver) {
            return;
        }
        gameOver = true;

       
        stopTimer();

        saveCurrentGameToHistory();
        
        showEndGameScreen();


        System.out.println("Game over! Saved to history.");
    }

   
    private void saveCurrentGameToHistory() {
        if (config == null) {
            return;
        }

        GameResult result = gameWon ? GameResult.WIN : GameResult.LOSE;

        Game gameRecord = new Game(
                config.getPlayer1Nickname(),
                config.getPlayer2Nickname(),
                difficulty,
                score,
                result,
                LocalDate.now(),
                elapsedSeconds
        );

        SysData sysData = SysData.getInstance();
        sysData.addGameToHistory(gameRecord);
        sysData.saveHistoryToCsv();

        System.out.println("Saved game: " + gameRecord);
    }
    
    @FXML
    private void showEndGameScreen() {
        try {
            String fxmlPath = gameWon
                    ? "/view/win_view.fxml"
                    : "/view/lose_view.fxml";

            Stage stage = (Stage) player1Grid.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent endRoot = loader.load();

            

            Scene endScene = new Scene(endRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setScene(endScene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
