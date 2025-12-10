package control;

import model.Board;
import model.Cell;
import model.CellType;
import model.Difficulty;
import model.GameConfig;
import model.Game;
import model.SysData;
import model.GameResult;
import model.Player;
import util.SessionManager;
import util.UIAnimations;
import util.SoundManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

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
    @FXML private Button soundButton;
    @FXML private Button musicButton;

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

    private String player1OfficialName;
    private String player2OfficialName;

    private boolean isPlayer1Turn = true;
    private boolean isPaused = false;

    private ImageCursor forbiddenCursor;

    private boolean gameOver = false;
    private boolean gameWon = false;
    private Timeline timer;
    private int elapsedSeconds = 0;

    private int safeCellsRemaining1;
    private int safeCellsRemaining2;

    // Added by Jihad – track cells that were already revealed (for scoring & surprise)
    private boolean[][] revealedCellsP1;
    private boolean[][] revealedCellsP2;

    private static final int TOTAL_HEART_SLOTS = 10;

    /**
     * Initializes the game session using the given configuration.
     * Called from the previous screen.
     */
    public void init(GameConfig config) {
        this.config = config;
        this.difficulty = config.getDifficulty();

        this.board1 = new Board(difficulty);
        this.board2 = new Board(difficulty);

        // Debug print of both boards
        printBoardDebug("Player 1 Board", board1);
        printBoardDebug("Player 2 Board", board2);

        this.minesLeft1 = board1.getMineCount();
        this.minesLeft2 = board2.getMineCount();

        // Track which cells were already revealed (for scoring & surprise logic)
        this.revealedCellsP1 = new boolean[board1.getRows()][board1.getCols()];
        this.revealedCellsP2 = new boolean[board2.getRows()][board2.getCols()];

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

        // Apply generic animations to buttons/cards
        UIAnimations.applyHoverZoomToAllButtons(root);
        UIAnimations.applyFloatingToCards(root);

        buildHeartsBar();
        initLabels();
        buildGridForPlayer(player1Grid, board1, true);
        buildGridForPlayer(player2Grid, board2, false);

        initForbiddenCursor();
        applyTurnStateToBoards();

        // --- TIMER SETTINGS ---
        elapsedSeconds = 0;
        if (SysData.isTimerEnabled()) {
            updateTimeLabel();
            startTimer();
        } else {
            if (timeLabel != null) {
                timeLabel.setText("Timer: OFF");
            }
            timer = null;
        }

        // --- SYNC ICONS WITH SETTINGS ---
        refreshSoundIconFromSettings();   // sound (clicks)
        refreshMusicIconFromSettings();   // music (background)
    }

    /**
     * Builds the hearts bar UI based on the current number of shared lives.
     * Fills up to TOTAL_HEART_SLOTS with full or empty heart icons.
     */
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

    /**
     * Initializes labels with difficulty name, starting time,
     * mines left for both players, and initial score.
     */
    private void initLabels() {
        difficultyLabel.setText(
                "Difficulty: " +
                        difficulty.name().charAt(0) +
                        difficulty.name().substring(1).toLowerCase()
        );

        if (SysData.isTimerEnabled()) {
            timeLabel.setText("Time: 00:00");
        } else {
            timeLabel.setText("Timer: OFF");
        }

        player1BombsLeftLabel.setText(
                config.getPlayer1Nickname() + ", Mines left: " + board1.getMineCount()
        );
        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + board2.getMineCount()
        );

        scoreLabel.setText("Score: " + score);
    }

    public void setOfficialPlayerNames(String player1OfficialName, String player2OfficialName) {
        this.player1OfficialName = player1OfficialName;
        this.player2OfficialName = player2OfficialName;
    }

    /**
     * Loads the "forbidden" cursor image used when a board is inactive.
     * If it fails, falls back to default cursor.
     */
    private void initForbiddenCursor() {
        try {
            Image img = new Image(getClass().getResourceAsStream("/Images/cursor_forbidden.png"));
            forbiddenCursor = new ImageCursor(img, img.getWidth() / 2, img.getHeight() / 2);
        } catch (Exception e) {
            forbiddenCursor = null;
        }
    }

    /**
     * Builds the minefield grid for a player.
     * Configures rows/columns in the GridPane and fills it with clickable tiles.
     */
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

    /**
     * Creates a single clickable tile for a given cell, sets up mouse handlers
     * for left/right clicks, turn checking, and calls the logic to reveal cells
     * or toggle flags.
     */
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
            if (gameOver) return;
            if (isPaused) return;

            // Right-click → flag
            if (e.getButton() == MouseButton.SECONDARY) {
                if ((tileIsPlayer1 && !isPlayer1Turn) ||
                    (!tileIsPlayer1 && isPlayer1Turn)) {
                    return;
                }

                if (button.isDisable()) {
                    return;
                }

                // NEW: pass board + coords so we can check cell type
                toggleFlag(board, r, c, button, tileIsPlayer1);
                return;
            }

            // Only left click
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

    /**
     * Toggles a flag icon on a covered cell.
     * If a flag is present, removes it; otherwise adds a flag graphic or emoji.
     * If a QUESTION or SURPRISE cell is flagged, score is reduced by 3 points.
     * "Auto Remove Flag" only affects what happens when revealing a cell,
     * not manual flagging.
     */
    private void toggleFlag(Board board, int row, int col, Button button, boolean isPlayer1) {
        Cell cell = board.getCell(row, col);

        // If there is already a flag → remove it (no score change)
        if (button.getGraphic() instanceof ImageView) {
            button.setGraphic(null);
            button.getStyleClass().remove("cell-flagged");
            updateScoreAndMineLabels();
            return;
        }

        // We are placing a new flag
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

        // NEW RULE: flagging a QUESTION or SURPRISE cell → -3 points
        if (cell.getType() == CellType.QUESTION || cell.getType() == CellType.SURPRISE) {
            System.out.println("The score before flagging: " + score);
            score -= 3;
            System.out.println("Flagged " + cell.getType() + " at (" + row + "," + col + "), score -3, now: " + score);
        }

        updateScoreAndMineLabels();
    }

    /**
     * Reveals a single cell and updates hearts, score, and game state
     * according to the cell type (MINE, QUESTION, SURPRISE, NUMBER, EMPTY).
     * Also checks for win/lose conditions.
     */
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

        // Check if this cell is being revealed for the first time
        boolean[][] revealedArray = isPlayer1 ? revealedCellsP1 : revealedCellsP2;
        boolean isFirstReveal = false;
        if (revealedArray != null) {
            isFirstReveal = !revealedArray[row][col];
            revealedArray[row][col] = true;
        }

        // Auto Remove Flag – if enabled, remove the flag when revealing a cell
        if (SysData.isAutoRemoveFlagEnabled()) {
            if (button.getGraphic() instanceof ImageView) {
                button.setGraphic(null);
            }
            button.getStyleClass().remove("cell-flagged");
        }

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
                } catch (Exception ex) {
                    button.setText("?");
                    button.getStyleClass().addAll("cell-revealed", "cell-question");
                }

                // first time a QUESTION is revealed: +1 point
                if (isFirstReveal) {
                    System.out.println("Before first reveal QUESTION at (" + row + "," + col + "), score: " + score);
                    score += 1;
                    System.out.println("First reveal QUESTION at (" + row + "," + col + "), score +1, now: " + score);
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
                } catch (Exception ex) {
                    button.setText("★");
                    button.getStyleClass().addAll("cell-revealed", "cell-surprise");
                }

                // first time a SURPRISE is revealed: +1 point
                if (isFirstReveal) {
                    System.out.println("Before first reveal SURPRISE at (" + row + "," + col + "), score: " + score);
                    score += 1;
                    System.out.println("First reveal SURPRISE at (" + row + "," + col + "), score +1, now: " + score);
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

        // Smart Hints – if enabled, visually mark QUESTION cells in a special CSS class
        if (SysData.isSmartHintsEnabled() && cell.getType() == CellType.QUESTION) {
            if (!button.getStyleClass().contains("smart-hint-cell")) {
                button.getStyleClass().add("smart-hint-cell");
            }
        }

        // Update remaining safe cells and check for win
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

    /**
     * Handles a cell click: reveal, cascade if empty, and trigger surprise activation
     * on second click. Always returns true → turn consumed.
     */
    private boolean handleCellClick(Board board,
                                    int row,
                                    int col,
                                    Button button,
                                    StackPane tile,
                                    boolean isPlayer1) {

        Cell cell = board.getCell(row, col);

        // Determine which revealed-array to use
        boolean[][] revealedArray = isPlayer1 ? revealedCellsP1 : revealedCellsP2;

        // If this is a SURPRISE cell and it was already revealed before,
        // this click is an ACTIVATION (second click).
        if (cell.getType() == CellType.SURPRISE &&
            revealedArray != null &&
            revealedArray[row][col]) {

            activateSurprise(board, row, col, button, tile, isPlayer1);
            // applySmartHint will exit early anyway for non-NUMBER cells
            applySmartHint(board, row, col, isPlayer1);
            return true;
        }

        // Normal reveal (first time or other types)
        revealSingleCell(board, row, col, button, tile, isPlayer1);

        // Cascade for EMPTY cells
        if (cell.getType() == CellType.EMPTY) {
            cascadeReveal(board, row, col, isPlayer1);
        }

        // Smart hint behaviour based on NUMBER neighbors
        applySmartHint(board, row, col, isPlayer1);

        return true;
    }

    /**
     * Performs a flood-fill style reveal of neighboring cells starting from
     * the given position, for empty areas.
     */
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

    /**
     * Updates the score label and "mines left" labels for both players.
     */
    private void updateScoreAndMineLabels() {
        scoreLabel.setText("Score: " + score);

        player1BombsLeftLabel.setText(
                config.getPlayer1Nickname() + ", Mines left: " + minesLeft1
        );
        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + minesLeft2
        );
    }

    /**
     * Switches the active player turn and refreshes the board states.
     */
    private void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
        applyTurnStateToBoards();
    }

    /**
     * Applies active/inactive state styles and interactivity to both boards
     * based on whose turn it is.
     */
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

    /**
     * Marks a board as active: enables it, sets styles, and updates the player label style.
     */
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

    /**
     * Marks a board as inactive: disables it, applies "forbidden" cursor or default,
     * and updates styles.
     */
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

    /**
     * Starts the game timer that updates every second and
     * stops any existing timer before creating a new one.
     */
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

    /**
     * Pauses the timer without resetting elapsed time.
     */
    private void pauseTimer() {
        if (timer != null) {
            timer.pause();
        }
    }

    /**
     * Resumes the timer after it has been paused.
     */
    private void resumeTimer() {
        if (timer != null) {
            timer.play();
        }
    }

    /**
     * Completely stops the timer.
     */
    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    /**
     * Updates the time label using the current number of elapsed seconds
     * in "Time: mm:ss" format.
     */
    private void updateTimeLabel() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timeLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    /**
     * Once clicking on exit button: stops the timer and closes the application.
     */
    @FXML
    private void onExitBtnClicked() {
        stopTimer();
        System.exit(0);
    }

    /**
     * Clicking on help button: currently just logs a message to the console
     * (placeholder for future help screen).
     */
    @FXML
    private void onHelpBtnClicked() {
        System.out.println("Help clicked but screen not created yet!");
    }

    /**
     * Clicking on back button: stops the timer and returns to the main menu screen.
     */
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

    /**
     * Handles the Pause/Play button: toggles paused state,
     * updates button icon and board opacity, and pauses/resumes the timer accordingly.
     */
    @FXML
    private void onPauseGame() {
        isPaused = !isPaused();

        if (pauseBtn != null && pauseBtn.getGraphic() instanceof ImageView iv) {
            String iconPath = isPaused ? "/Images/play-button.png" : "/Images/pause.png";
            Image img = new Image(getClass().getResourceAsStream(iconPath));
            iv.setImage(img);
        }

        double opacity = isPaused ? 0.6 : 1.0;
        if (player1Grid != null) player1Grid.setOpacity(opacity);
        if (player2Grid != null) player2Grid.setOpacity(opacity);

        if (isPaused) {
            pauseTimer();
        } else {
            if (SysData.isTimerEnabled()) {
                resumeTimer();
            }
        }
    }

    private boolean isPaused() {
        return isPaused;
    }

    /**
     * Handling the sound button: toggles sound effects setting (click sounds) and updates icon.
     */
    @FXML
    private void onSoundOff() {
        boolean newState = !SysData.isSoundEnabled();
        SysData.setSoundEnabled(newState);
        refreshSoundIconFromSettings();
    }

    /**
     * Handling the music button: toggles background music via SoundManager
     * and syncs SysData + icon.
     */
    @FXML
    private void onMusicToggle() {
        // Toggle music playback
        SoundManager.toggleMusic();

        // Save current state to SysData
        boolean musicOn = SoundManager.isMusicOn();
        SysData.setMusicEnabled(musicOn);

        // Update icon and ensure state is synced
        refreshMusicIconFromSettings();
    }

    @FXML
    private void onMainMenu() {
        // TODO: implement if needed (currently unused)
    }

    /**
     * Triggers a small explosion animation on a tile when a mine is hit.
     * Creates flash, shockwave, and debris animations and removes them after completion.
     */
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

    /**
     * Main game-over handler: prevents duplicate handling, stops the timer,
     * saves the game result, and opens the end-game screen.
     */
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

    /**
     * Saves the current game session (result, score, time, etc.)
     * into SysData's history and writes it to CSV.
     */
    private void saveCurrentGameToHistory() {
        if (config == null) {
            return;
        }

        GameResult result = gameWon ? GameResult.WIN : GameResult.LOSE;

        // Get the logged-in players from the session
        Player p1 = SessionManager.getPlayer1();
        Player p2 = SessionManager.getPlayer2();

        String player1Official = (p1 != null) ? p1.getOfficialName() : null;
        String player2Official = (p2 != null) ? p2.getOfficialName() : null;

        // Nicknames still come from GameConfig
        String player1Nick = config.getPlayer1Nickname();
        String player2Nick = config.getPlayer2Nickname();

        Game gameRecord = new Game(
                player1Official,
                player2Official,
                player1Nick,
                player2Nick,
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

    /**
     * Shows the appropriate end game screen (win or lose view),
     * passes the game data into EndGameController, and switches the scene.
     */
    @FXML
    private void showEndGameScreen() {
        try {
            String fxmlPath = gameWon
                    ? "/view/win_view.fxml"
                    : "/view/lose_view.fxml";

            Stage stage = (Stage) player1Grid.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent endRoot = loader.load();

            EndGameController controller = loader.getController();
            controller.init(
                    stage,
                    config,
                    score,
                    elapsedSeconds,
                    sharedHearts,
                    gameWon
            );

            Scene endScene = new Scene(endRoot, 700, 450);
            stage.setScene(endScene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =================== DEBUG / SURPRISE LOGIC (JIHAD) ===================

    /**
     * Helper method: prints the logical contents of a board to the console.
     */
    private void printBoardDebug(String title, Board board) {
        System.out.println("========== " + title + " ==========");
        int rows = board.getRows();
        int cols = board.getCols();

        // Print column indices header
        System.out.print("    ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("%3d", c);
        }
        System.out.println();
        System.out.print("    ");
        for (int c = 0; c < cols; c++) {
            System.out.print("---");
        }
        System.out.println();

        for (int r = 0; r < rows; r++) {
            // Row index on the left
            System.out.printf("%3d|", r);

            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                char ch;

                switch (cell.getType()) {
                    case MINE -> ch = 'M';
                    case QUESTION -> ch = 'Q';
                    case SURPRISE -> ch = 'S';
                    case NUMBER -> {
                        int n = cell.getAdjacentMines();
                        // Show digits 0–9, or 'N' if >9 just in case
                        if (n >= 0 && n <= 9) {
                            ch = (char) ('0' + n);
                        } else {
                            ch = 'N';
                        }
                    }
                    case EMPTY -> ch = '.';
                    default -> ch = '?';
                }

                System.out.printf("%3c", ch);
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Activates a surprise cell on second click:
     * base activation score + random good/bad effect.
     *
     * Good:
     *   - extra positive points
     *   - +1 life (or convert life to points if at max)
     *
     * Bad:
     *   - extra negative points
     *   - -1 life
     *
     * After activation, the cell is disabled (cannot be clicked again).
     */
    private void activateSurprise(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {

        int livesBefore = sharedHearts;
        int scoreBefore = score;

        int activationPoints = getSurpriseActivationPoints();
        int goodBonus = getSurpriseGoodBonusPoints();
        int badPenalty = getSurpriseBadPenaltyPoints();

        // Always get activation points
        int scoreChange = activationPoints;

        // 50% chance good / bad
        boolean good = Math.random() < 0.5;

        if (good) {
            // Good surprise: extra positive points + possibly +1 life
            scoreChange += goodBonus;

            if (sharedHearts < TOTAL_HEART_SLOTS) {
                // Gain 1 life
                sharedHearts += 1;
            } else {
                // Hearts already max → convert the would-be extra life into points
                scoreChange += activationPoints;
            }
        } else {
            // Bad surprise: extra negative points and -1 life
            scoreChange -= badPenalty;
            sharedHearts = Math.max(0, sharedHearts - 1);
        }

        // Apply the total score change
        score += scoreChange;

        // Update hearts bar
        buildHeartsBar();

        // If lives hit zero → game over, same as stepping on mines
        if (sharedHearts == 0 && !gameOver) {
            gameWon = false;
            onGameOver();
        }

        // Disable this surprise cell so it cannot be activated again
        button.setDisable(true);

        // Refresh score / mines labels
        updateScoreAndMineLabels();

        // Show popup with result
        int livesAfter = sharedHearts;
        int netScoreChange = score - scoreBefore;
        showSurprisePopup(good, netScoreChange, livesBefore, livesAfter);
    }

    // Base score for activating a surprise (second click) per difficulty
    private int getSurpriseActivationPoints() {
        return switch (difficulty) {
            case EASY -> 5;
            case MEDIUM -> 8;
            case HARD -> 12;
        };
    }

    // Extra score for GOOD surprise (on top of activation)
    private int getSurpriseGoodBonusPoints() {
        return switch (difficulty) {
            case EASY -> 8;
            case MEDIUM -> 12;
            case HARD -> 16;
        };
    }

    // Extra score (negative) for BAD surprise (on top of activation)
    private int getSurpriseBadPenaltyPoints() {
        return switch (difficulty) {
            case EASY -> 8;
            case MEDIUM -> 12;
            case HARD -> 16;
        };
    }

    /**
     * Shows a popup describing the surprise result and the changes
     * in score and lives.
     */
    private void showSurprisePopup(boolean good, int netScoreChange, int livesBefore, int livesAfter) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Surprise Result");

        String typeText = good ? "GOOD surprise!" : "BAD surprise!";
        int livesDelta = livesAfter - livesBefore;
        String scoreChangeText = (netScoreChange >= 0 ? "+" : "") + netScoreChange;
        String livesChangeText = (livesDelta > 0 ? "+" : "") + livesDelta;

        StringBuilder msg = new StringBuilder();
        msg.append(typeText).append("\n\n");
        msg.append("Score change: ").append(scoreChangeText).append("\n");
        msg.append("Lives change: ").append(livesChangeText).append("\n");
        msg.append("\nNew score: ").append(score).append("\n");
        msg.append("New lives: ").append(sharedHearts).append("/").append(TOTAL_HEART_SLOTS);

        alert.setHeaderText(null);
        alert.setContentText(msg.toString());
        alert.showAndWait();
    }

    /**
     * Smart Hints feature:
     * Applies only to NUMBER cells.
     * If the number of flagged neighbors equals the number on the cell,
     * highlight the remaining safe neighbors for 2 seconds.
     */
    private void applySmartHint(Board board, int row, int col, boolean isPlayer1) {
        // feature disabled → do nothing
        if (!SysData.isSmartHintsEnabled()) return;

        Cell cell = board.getCell(row, col);

        // smart hint applies only to NUMBER cells
        if (cell.getType() != CellType.NUMBER) return;

        int needed = cell.getAdjacentMines();
        int flagged = 0;

        StackPane[][] buttons = isPlayer1 ? p1Buttons : p2Buttons;

        // 1) count flagged neighbors
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                int nr = row + dr;
                int nc = col + dc;

                if (nr < 0 || nc < 0 || nr >= board.getRows() || nc >= board.getCols()) continue;

                StackPane tile = buttons[nr][nc];
                if (tile == null || tile.getChildren().isEmpty()) continue;

                Button b = (Button) tile.getChildren().get(0);

                if (b.getStyleClass().contains("cell-flagged")) {
                    flagged++;
                }
            }
        }

        // 2) if all needed mines are flagged → highlight remaining safe cells
        if (flagged == needed) {

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;

                    int nr = row + dr;
                    int nc = col + dc;

                    if (nr < 0 || nc < 0 || nr >= board.getRows() || nc >= board.getCols()) continue;

                    StackPane tile = buttons[nr][nc];
                    if (tile == null || tile.getChildren().isEmpty()) continue;

                    Button b = (Button) tile.getChildren().get(0);

                    // highlight only clickable, un-flagged cells
                    if (!b.isDisable() && !b.getStyleClass().contains("cell-flagged")) {

                        b.getStyleClass().add("smart-hint");

                        // 3) highlight disappears after 2 seconds
                        PauseTransition pause = new PauseTransition(Duration.seconds(2));
                        pause.setOnFinished(e -> b.getStyleClass().remove("smart-hint"));
                        pause.play();
                    }
                }
            }
        }
    }

    // =================== SOUND / MUSIC ICON HELPERS ===================

    private void refreshMusicIconFromSettings() {
        if (musicButton == null) return;
        if (!(musicButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isMusicEnabled();

        // Sync SoundManager with SysData
        if (enabled && !SoundManager.isMusicOn()) {
            SoundManager.startMusic();
        } else if (!enabled && SoundManager.isMusicOn()) {
            SoundManager.stopMusic();
        }

        String iconPath;
        double size;

        if (enabled) {
            iconPath = "/Images/music.png";
            size = 40;
        } else {
            iconPath = "/Images/music_mute.png";
            size = 60;
        }

        Image img = new Image(getClass().getResourceAsStream(iconPath));
        iv.setImage(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
    }

    private void refreshSoundIconFromSettings() {
        if (soundButton == null) return;
        if (!(soundButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isSoundEnabled();
        String iconPath = enabled ? "/Images/volume.png" : "/Images/mute.png";

        Image img = new Image(getClass().getResourceAsStream(iconPath));
        iv.setImage(img);
    }
}
