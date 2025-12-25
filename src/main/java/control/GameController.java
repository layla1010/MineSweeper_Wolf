package control;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

import model.Board;
import model.Cell;
import model.CellType;
import model.Difficulty;
import model.Game;
import model.GameConfig;
import model.GameResult;
import model.Player;
import model.Question;
import model.SysData;
import util.DialogUtil;
import util.SessionManager;
import util.SoundManager;
import util.UIAnimations;

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
    @FXML private ImageView player1AvatarImage;
    @FXML private ImageView player2AvatarImage;

    private GameConfig config;
    private Difficulty difficulty;
    private Board board1;
    private Board board2;
    private StackPane[][] p1Buttons;
    private StackPane[][] p2Buttons;

    private int sharedHearts;
    private int score;
    private int minesLeft1;
    private int surprisesLeft1;
    private int surprisesLeft2;
    private int questionsLeft2;
    private int questionsLeft1;
    private int minesLeft2;

    private int flagsLeft1;
    private int flagsLeft2;
    
    private int flagsPlaced1;
    private int flagsPlaced2;

    private int revealedCountP1;
    private int revealedCountP2;

    private int totalCellsP1;
    private int totalCellsP2;
    private int endScoreBefore;

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

    // Win bonus popup data
    private int endHeartsRemaining = 0;
    private int endHeartsBonusPoints = 0;

    // Track cells that were already revealed (for scoring & surprise/question second-click)
    private boolean[][] revealedCellsP1;
    private boolean[][] revealedCellsP2;

    // True if any mistake happened (for win-without-mistakes flag)
    private boolean mistakeMade = false;

    private static final int TOTAL_HEART_SLOTS = 10;

    private PauseTransition idleHintTimer;
    private static final Duration IDLE_HINT_DELAY = Duration.minutes(2);
    private static final Duration IDLE_HINT_GLOW_DURATION = Duration.seconds(3);

    /**
     * Resets the idle timer. Any user action (click/flag/unflag/question/surprise)
     * should call this.
     */
    private void resetIdleHintTimer() {
        if (!SysData.isSmartHintsEnabled()) return;

        if (idleHintTimer != null) {
            idleHintTimer.stop();
        }

        idleHintTimer = new PauseTransition(IDLE_HINT_DELAY);
        idleHintTimer.setOnFinished(e -> {
            if (gameOver || isPaused) return;
            showIdleHint();
            // restart only after it triggers (still no action)
            resetIdleHintTimer();
        });
        idleHintTimer.playFromStart();
    }

    /**
     * Picks one unrevealed non mine cell (EMPTY/NUMBER/QUESTION/SURPRISE) on the CURRENT TURN board
     * and makes it glow temporarily.
     */
    private void showIdleHint() {
        if (!SysData.isSmartHintsEnabled()) return;
        if (gameOver || isPaused) return;

        boolean targetP1 = isPlayer1Turn;
        Board board = targetP1 ? board1 : board2;
        StackPane[][] tiles = targetP1 ? p1Buttons : p2Buttons;

        if (board == null || tiles == null) return;

        List<Button> candidates = new ArrayList<>();

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                if (cell == null) continue;

                // Only allow empty/number/question/surprise (explicitly exclude mines)
                if (cell.getType() == CellType.MINE) continue;

                StackPane tile = tiles[r][c];
                if (tile == null || tile.getChildren().isEmpty()) continue;

                Button btn = (Button) tile.getChildren().get(0);

                // must be unrevealed and clickable (not disabled)
                if (btn.isDisable()) continue;
                if (!btn.getStyleClass().contains("cell-hidden")) continue;

                // avoid hinting a flagged cell (they already acted on it)
                if (btn.getStyleClass().contains("cell-flagged")) continue;

                candidates.add(btn);
            }
        }

        if (candidates.isEmpty()) return;

        Button pick = candidates.get((int) (Math.random() * candidates.size()));

        if (!pick.getStyleClass().contains("idle-hint-glow")) {
            pick.getStyleClass().add("idle-hint-glow");
        }

        PauseTransition pt = new PauseTransition(IDLE_HINT_GLOW_DURATION);
        pt.setOnFinished(e -> pick.getStyleClass().remove("idle-hint-glow"));
        pt.play();
    }


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

        this.surprisesLeft1 = countType(board1, CellType.SURPRISE);
        this.surprisesLeft2 = countType(board2, CellType.SURPRISE);

        this.questionsLeft1 = countType(board1, CellType.QUESTION);
        this.questionsLeft2 = countType(board2, CellType.QUESTION);

        // ----------  load avatars above boards ----------
        setBoardAvatar(player1AvatarImage, config.getPlayer1AvatarPath());
        setBoardAvatar(player2AvatarImage, config.getPlayer2AvatarPath());
        // -----------------------------------------------------

        // Track which cells were already revealed (for scoring & surprise/question logic)
        this.revealedCellsP1 = new boolean[board1.getRows()][board1.getCols()];
        this.revealedCellsP2 = new boolean[board2.getRows()][board2.getCols()];

        this.totalCellsP1 = board1.getRows() * board1.getCols();
        this.totalCellsP2 = board2.getRows() * board2.getCols();

        this.revealedCountP1 = 0;
        this.revealedCountP2 = 0;

        // flagsUsed start at 0
        flagsPlaced1 = flagsPlaced2 = 0;
        recalcFlagsLeft(true);
        recalcFlagsLeft(false);

        this.safeCellsRemaining1 = totalCellsP1 - board1.getMineCount();
        this.safeCellsRemaining2 = totalCellsP2 - board2.getMineCount();

        this.sharedHearts = difficulty.getInitialLives();
        this.score = 0;
        this.isPlayer1Turn = true;
        this.isPaused = false;
        this.gameOver = false;
        this.gameWon = false;
        this.mistakeMade = false;

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

        // SYNC ICONS WITH SETTINGS 
        refreshSoundIconFromSettings();   // sound (clicks)
        refreshMusicIconFromSettings();   // music (background)

        // start/reset idle smart-hint timer
        resetIdleHintTimer();
    }

    private void setBoardAvatar(ImageView target, String avatarId) {
        if (target == null) return;
        if (avatarId == null || avatarId.isBlank()) return;

        try {
            Image img;
            if (avatarId.startsWith("file:")) {
                // custom file chosen with '+'
                img = new Image(avatarId);
            } else {
                // built-in S1.png ... in /Images
                var stream = getClass().getResourceAsStream("/Images/" + avatarId);
                if (stream == null) {
                    System.err.println("GameController: cannot find /Images/" + avatarId);
                    return;
                }
                img = new Image(stream);
            }
            target.setImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            var stream = getClass().getResourceAsStream(imgPath);
            if (stream == null) {
                System.err.println("Missing resource: " + imgPath);
                return;
            }
            Image img = new Image(stream);
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
                config.getPlayer1Nickname() + ", Mines left: " + minesLeft1
                        + " | Flags left: " + flagsLeft1
                        + " | Surprises left: " + surprisesLeft1
                        + " | Questions left: " + questionsLeft1
        );

        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + minesLeft2
                        + " | Flags left: " + flagsLeft2
                        + " | Surprises left: " + surprisesLeft2
                        + " | Questions left: " + questionsLeft2
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
            var stream = getClass().getResourceAsStream("/Images/cursor_forbidden.png");
            if (stream == null) {
                forbiddenCursor = null;
                return;
            }
            Image img = new Image(stream);
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

        // per-player style
        if (isPlayer1) {
            button.getStyleClass().add("p1-cell");
        } else {
            button.getStyleClass().add("p2-cell");
        }

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

            // Turn guard
            if ((tileIsPlayer1 && !isPlayer1Turn) || (!tileIsPlayer1 && isPlayer1Turn)) {
                return;
            }

            if (button.isDisable()) return;

            // Any interaction (including flag/unflag) counts as "activity" for smart hints
            resetIdleHintTimer();

            // RIGHT CLICK: flag/unflag only, NO turn switching
            if (e.getButton() == MouseButton.SECONDARY) {

                // only allow flagging on covered cells
                if (!button.getStyleClass().contains("cell-hidden")) {
                    return;
                }

                toggleFlag(board, r, c, button, tileIsPlayer1);
                return;
            }

            // LEFT CLICK: always behaves like reveal action, even if flagged
            if (e.getButton() == MouseButton.PRIMARY) {
                boolean consumedAction = handleCellClick(board, r, c, button, tile, tileIsPlayer1);
                if (consumedAction) {
                    switchTurn(); // reveal consumes turn
                }
                return;
            }

        });

        return tile;
    }

    /**
     * Toggles a flag icon on a covered cell.
     *  If it's a MINE: right-click reveals instantly and gives +1 score (Option A).
     *  If it's NOT a mine: placing a flag consumes a non-reusable flag (flagsPlaced = flagsUsed).
     *  Unflagging removes only the visual marker and does NOT refund a flag.
     *  Wrong flag is a mistake and costs -3 score.
     */
    private void toggleFlag(Board board, int row, int col, Button button, boolean isPlayer1) {
        if (gameOver || isPaused) return;

        Cell cell = board.getCell(row, col);
        if (cell == null) return;

        // don't allow flagging revealed/disabled cells (for safety)
        if (button.isDisable()) return;

        // 1) If already flagged, then remove only visual flag (NO REFUND of flagsUsed)
        if (button.getStyleClass().contains("cell-flagged")) {
            button.setGraphic(null);
            button.setText("");
            button.getStyleClass().remove("cell-flagged");

            // Do NOT decrement flagsPlacedX (non-reusable flags)
            recalcFlagsLeft(isPlayer1);
            updateScoreAndMineLabels();
            return;
        }

        // Check flags availability (allowedNow - flagsUsed)
        recalcFlagsLeft(isPlayer1);

        if (isPlayer1 && flagsLeft1 <= 0) {
            DialogUtil.show(AlertType.INFORMATION, null, "No Flags Left",
                    "You have no flags left.\nFlagging non-mines reduces your flag quota.");
            return;
        }
        if (!isPlayer1 && flagsLeft2 <= 0) {
            DialogUtil.show(AlertType.INFORMATION, null, "No Flags Left",
                    "You have no flags left.\nFlagging non-mines reduces your flag quota.");
            return;
        }

        // 3) If it's a mine -> reveal immediately (NOT a persistent flag)
        if (cell.isMine()) {
            button.setGraphic(null);
            button.setText("💣");

            button.getStyleClass().remove("cell-hidden");
            button.getStyleClass().remove("cell-flagged"); // just in case
            if (!button.getStyleClass().contains("cell-revealed")) button.getStyleClass().add("cell-revealed");
            if (!button.getStyleClass().contains("cell-mine"))     button.getStyleClass().add("cell-mine");

            button.setDisable(true);

            // scoring + mines left
            score += 1;
            if (isPlayer1) minesLeft1 = Math.max(0, minesLeft1 - 1);
            else          minesLeft2 = Math.max(0, minesLeft2 - 1);

            // mark revealed for consistency with unrevealed-count logic
            boolean[][] revealedArray = isPlayer1 ? revealedCellsP1 : revealedCellsP2;
            if (revealedArray != null && !revealedArray[row][col]) {
                revealedArray[row][col] = true;
                if (isPlayer1) revealedCountP1++;
                else          revealedCountP2++;
            }

            recalcFlagsLeft(isPlayer1);
            updateScoreAndMineLabels();

            // Either board finishing ends the match
            if (!gameOver && sharedHearts > 0 && (minesLeft1 == 0 || minesLeft2 == 0)) {
                gameWon = true;
                onGameOver();
            }
            return;
        }

        // 4) Otherwise (non-mine): place a real flag (penalty flag) and consume a non-reusable flag
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

        // Consume a flag permanently
        if (isPlayer1) {
            flagsPlaced1++;
            recalcFlagsLeft(true);
        } else {
            flagsPlaced2++;
            recalcFlagsLeft(false);
        }

        // Penalty + mistake
        mistakeMade = true;
        score -= 3;

        updateScoreAndMineLabels();
    }

    private int getUnrevealedCellsRemaining(boolean isPlayer1) {
        int total = isPlayer1 ? totalCellsP1 : totalCellsP2;
        int revealed = isPlayer1 ? revealedCountP1 : revealedCountP2;
        return Math.max(0, total - revealed);
    }

    // allowed flags = half of ALL unrevealed cells
    private int allowedFlagsNow(boolean isPlayer1) {
        return getUnrevealedCellsRemaining(isPlayer1) / 2; // floor
    }

    // flagsUsed is non-recoverable => flagsLeft = allowedNow - flagsUsed
    private void recalcFlagsLeft(boolean isPlayer1) {
        int allowed = allowedFlagsNow(isPlayer1);

        if (isPlayer1) {
            flagsLeft1 = Math.max(0, allowed - flagsPlaced1);
        } else {
            flagsLeft2 = Math.max(0, allowed - flagsPlaced2);
        }
    }

   

 
     // Auto-remove flag removes only the VISUAL marker. It does NOT refund a flag.
    private void autoRemoveFlagIfPresent(Board board, int row, int col, Button button, boolean isPlayer1) {
        if (!button.getStyleClass().contains("cell-flagged")) return;

        // Remove flag visuals only
        button.setGraphic(null);
        button.setText("");
        button.getStyleClass().remove("cell-flagged");

        // DO NOT decrement flagsPlacedX (non-reusable flags)
        recalcFlagsLeft(isPlayer1);
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
            if (revealedArray[row][col]) return;
            revealedArray[row][col] = true;
            isFirstReveal = true;
        }
        if (isFirstReveal) {
            if (isPlayer1) revealedCountP1++;
            else revealedCountP2++;

            recalcFlagsLeft(isPlayer1);
        }

        button.getStyleClass().removeAll(
                "cell-hidden", "cell-revealed",
                "cell-mine", "cell-question",
                "cell-surprise", "cell-number", "cell-empty"
        );

        switch (cell.getType()) {
            case MINE -> {
                button.setText("💣");
                button.setDisable(true);
                button.getStyleClass().addAll("cell-revealed", "cell-mine");

                int heartsBefore = sharedHearts;
                sharedHearts = Math.max(0, sharedHearts - 1);
                if (sharedHearts < heartsBefore) {
                    mistakeMade = true;
                }

                triggerExplosion(tile);

                if (isPlayer1) {
                    minesLeft1 = Math.max(0, minesLeft1 - 1);
                } else {
                    minesLeft2 = Math.max(0, minesLeft2 - 1);
                }
                buildHeartsBar();
                updateScoreAndMineLabels();

                // (#2) Either board finishing ends the match (keep ||)
                if (!gameOver && sharedHearts > 0 && (minesLeft1 == 0 || minesLeft2 == 0)) {
                    gameWon = true;
                    onGameOver();
                    return;
                }

                if (checkLoseAndHandle()) return;
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
                    if (isPlayer1) questionsLeft1 = Math.max(0, questionsLeft1 - 1);
                    else           questionsLeft2 = Math.max(0, questionsLeft2 - 1);
                } catch (Exception ex) {
                    button.setText("?");
                    button.getStyleClass().addAll("cell-revealed", "cell-question");
                }

                // first time a QUESTION is revealed: +1 point
                if (isFirstReveal) {
                    score += 1;
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
                    if (isPlayer1) surprisesLeft1 = Math.max(0, surprisesLeft1 - 1);
                    else           surprisesLeft2 = Math.max(0, surprisesLeft2 - 1);

                } catch (Exception ex) {
                    button.setText("★");
                    button.getStyleClass().addAll("cell-revealed", "cell-surprise");
                }

                // first time a SURPRISE is revealed: +1 point
                if (isFirstReveal) {
                    score += 1;
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

        // Update remaining safe cells and check for win
        if (cell.getType() != CellType.MINE) {
            if (isPlayer1) {
                safeCellsRemaining1 = Math.max(0, safeCellsRemaining1 - 1);
            } else {
                safeCellsRemaining2 = Math.max(0, safeCellsRemaining2 - 1);
            }

            // (#2) Either board finishing ends the match (keep ||)
            if (!gameOver && sharedHearts > 0 &&
                    (safeCellsRemaining1 == 0 || safeCellsRemaining2 == 0)) {
                gameWon = true;
                onGameOver();
            }
        }

        updateScoreAndMineLabels();
    }

    /**
     * Handles a cell click: reveal, cascade if empty/question/surprise, and trigger surprise/question activation
     * on second click. Always returns true → turn consumed.
     */
    private boolean handleCellClick(Board board,
            int row,
            int col,
            Button button,
            StackPane tile,
            boolean isPlayer1) {

		Cell cell = board.getCell(row, col);
		boolean[][] revealedArray = isPlayer1 ? revealedCellsP1 : revealedCellsP2;
		
		// Left click on flagged cell should still reveal -> remove flag visuals first
		if (button.getStyleClass().contains("cell-flagged")) {
		autoRemoveFlagIfPresent(board, row, col, button, isPlayer1);
		}
		
		// SECOND CLICK ON SURPRISE : activate surprise
		if (cell.getType() == CellType.SURPRISE &&
		revealedArray != null &&
		revealedArray[row][col]) {
		
		activateSurprise(board, row, col, button, tile, isPlayer1);
		return true;
		}
		
		// SECOND CLICK ON QUESTION : activate question
		if (cell.getType() == CellType.QUESTION &&
		revealedArray != null &&
		revealedArray[row][col]) {
		
		activateQuestion(board, row, col, button, tile, isPlayer1);
		return true;
		}
		
		revealSingleCell(board, row, col, button, tile, isPlayer1);
		
		// Cascade for EMPTY, QUESTION, SURPRISE (treated as empty by TA)
		if (cell.getType() == CellType.EMPTY ||
		cell.getType() == CellType.QUESTION ||
		cell.getType() == CellType.SURPRISE) {
		
		cascadeReveal(board, row, col, isPlayer1);
		}
		
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
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startRow, startCol});

        while (!stack.isEmpty()) {
            if (gameOver) return;
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

            // (#4/#5) remove only visual flag when cascade reveals; no refund
            autoRemoveFlagIfPresent(board, r, c, btn, isPlayer1);

            revealSingleCell(board, r, c, btn, tile, isPlayer1);

            if (cell.getType() == CellType.EMPTY ||
                    cell.getType() == CellType.QUESTION ||
                    cell.getType() == CellType.SURPRISE ||
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
                        + " | Flags left: " + flagsLeft1
                        + " | Surprises left: " + surprisesLeft1
                        + " | Questions left: " + questionsLeft1
        );

        player2BombsLeftLabel.setText(
                config.getPlayer2Nickname() + ", Mines left: " + minesLeft2
                        + " | Flags left: " + flagsLeft2
                        + " | Surprises left: " + surprisesLeft2
                        + " | Questions left: " + questionsLeft2
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
        resetIdleHintTimer();
        saveGiveUpGame();
        stopTimer();
        System.exit(0);
    }

    @FXML
    private void onHelpBtnClicked() {
        resetIdleHintTimer();
        System.out.println("Help clicked but screen not created yet!");
    }

    @FXML
    private void onBackBtnClicked() throws IOException {
        resetIdleHintTimer();
        saveGiveUpGame();
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
        resetIdleHintTimer();

        isPaused = !isPaused;

        if (pauseBtn != null && pauseBtn.getGraphic() instanceof ImageView iv) {
            String iconPath = isPaused ? "/Images/play-button.png" : "/Images/pause.png";

            var stream = getClass().getResourceAsStream(iconPath);
            if (stream == null) {
                System.err.println("Missing resource: " + iconPath);
                return;
            }
            Image img = new Image(stream);

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

    @FXML
    private void onSoundOff() {
        resetIdleHintTimer();
        boolean newState = !SysData.isSoundEnabled();
        SysData.setSoundEnabled(newState);
        refreshSoundIconFromSettings();
    }

    @FXML
    private void onMusicToggle() {
        resetIdleHintTimer();
        SoundManager.toggleMusic();

        boolean musicOn = SoundManager.isMusicOn();
        SysData.setMusicEnabled(musicOn);

        refreshMusicIconFromSettings();
    }

    @FXML
    private void onMainMenu() {
        resetIdleHintTimer();
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

        // stop idle hint timer
        if (idleHintTimer != null) idleHintTimer.stop();

        // convert remaining hearts to points when the game is won
        endHeartsRemaining = 0;
        endHeartsBonusPoints = 0;
        if (gameWon && sharedHearts > 0) {
            int perHeart = switch (difficulty) {
                case EASY   -> 5;
                case MEDIUM -> 8;
                case HARD   -> 12;
            };
            endScoreBefore = score;
            endHeartsRemaining = sharedHearts;
            endHeartsBonusPoints = sharedHearts * perHeart;
            score += endHeartsBonusPoints;
            updateScoreAndMineLabels();
        }

        stopTimer();
        saveCurrentGameToHistory();
        revealAllBoardsVisualOnly();

        PauseTransition pause = new PauseTransition(Duration.seconds(4.0));
        pause.setOnFinished(e -> showEndGameScreen());
        pause.play();

        System.out.println("Game over! Saved to history.");
    }

    private void showHeartsBonusPopupIfNeeded() {
        if (!gameWon) return;
        if (endHeartsRemaining <= 0 || endHeartsBonusPoints <= 0) return;

        DialogUtil.show(AlertType.INFORMATION,
                "Bonus added to your final score!",
                "Hearts Bonus",
                "Remaining hearts: " + endHeartsRemaining + "\n" +
                        "Score before Addition: " + endScoreBefore + "\n" +
                        "Added points: +" + endHeartsBonusPoints + "\n\n" +
                        "Final score: " + score);
    }

    private void saveCurrentGameToHistory() {
        if (config == null) {
            return;
        }

        GameResult result = gameWon ? GameResult.WIN : GameResult.LOSE;

        Player p1 = SessionManager.getPlayer1();
        Player p2 = SessionManager.getPlayer2();

        String player1Official = (p1 != null) ? p1.getOfficialName() : null;
        String player2Official = (p2 != null) ? p2.getOfficialName() : null;

        String player1Nick = config.getPlayer1Nickname();
        String player2Nick = config.getPlayer2Nickname();

        boolean winWithoutMistakes = (gameWon && !mistakeMade);

        Game gameRecord = new Game(
                player1Official,
                player2Official,
                player1Nick,
                player2Nick,
                difficulty,
                score,
                result,
                LocalDate.now(),
                elapsedSeconds,
                winWithoutMistakes,
                config.getPlayer1AvatarPath(),
                config.getPlayer2AvatarPath()
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

            Platform.runLater(this::showHeartsBonusPopupIfNeeded);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveGiveUpGame() {
        if (config == null) {
            return;
        }

        if (gameOver) {
            return;
        }

        GameResult result = GameResult.GIVE_UP;

        Player p1 = SessionManager.getPlayer1();
        Player p2 = SessionManager.getPlayer2();

        String player1Official = (p1 != null) ? p1.getOfficialName() : null;
        String player2Official = (p2 != null) ? p2.getOfficialName() : null;

        String player1Nick = config.getPlayer1Nickname();
        String player2Nick = config.getPlayer2Nickname();

        Game giveUpGame = new Game(
                player1Official,
                player2Official,
                player1Nick,
                player2Nick,
                difficulty,
                score,
                result,
                LocalDate.now(),
                elapsedSeconds,
                false,
                config.getPlayer1AvatarPath(),
                config.getPlayer2AvatarPath()
        );

        SysData sysData = SysData.getInstance();
        sysData.addGameToHistory(giveUpGame);
        sysData.saveHistoryToCsv();

        System.out.println("Saved GIVE_UP game: " + giveUpGame);
    }


    private void printBoardDebug(String title, Board board) {
        System.out.println("========== " + title + " ==========");
        int rows = board.getRows();
        int cols = board.getCols();

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
                        if (n >= 0 && n <= 9) ch = (char) ('0' + n);
                        else ch = 'N';
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

  

    private void activateSurprise(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {
        resetIdleHintTimer();

        int livesBefore = sharedHearts;
        int scoreBefore = score;

        int activationPoints = getActivationPoints();
        int goodBonus = getSurpriseGoodBonusPoints();
        int badPenalty = getSurpriseBadPenaltyPoints();

        int scoreChange = activationPoints;

        boolean good = Math.random() < 0.5;

        if (good) {
            scoreChange += goodBonus;

            if (sharedHearts < TOTAL_HEART_SLOTS) {
                sharedHearts += 1;
            } else {
                scoreChange += activationPoints;
            }
        } else {
            scoreChange -= badPenalty;
            sharedHearts = Math.max(0, sharedHearts - 1);
        }

        score += scoreChange;

        buildHeartsBar();
        button.setDisable(true);

        updateScoreAndMineLabels();

        if (checkLoseAndHandle()) return;

        int livesAfter = sharedHearts;
        int netScoreChange = score - scoreBefore;
        showSurprisePopup(good, netScoreChange, livesBefore, livesAfter);
    }

    private int getActivationPoints() {
        return switch (difficulty) {
            case EASY -> 5;
            case MEDIUM -> 8;
            case HARD -> 12;
        };
    }

    private int getSurpriseGoodBonusPoints() {
        return switch (difficulty) {
            case EASY -> 8;
            case MEDIUM -> 12;
            case HARD -> 16;
        };
    }

    private int getSurpriseBadPenaltyPoints() {
        return switch (difficulty) {
            case EASY -> 8;
            case MEDIUM -> 12;
            case HARD -> 16;
        };
    }

    private void showSurprisePopup(boolean good, int netScoreChange, int livesBefore, int livesAfter) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

 

    private Question getRandomQuestionFromPool() {
        List<Question> all = QuestionsManagerController.loadQuestionsForGame();

        if (all == null || all.isEmpty()) {
            System.out.println("No questions found.");
            return null;
        }

        int idx = (int) (Math.random() * all.size());
        return all.get(idx);
    }

    private int showQuestionDialog(Question q) {
        if (q == null) return 0;

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Trivia Question");
        alert.setHeaderText("Question (" + q.getDifficulty() + ")");
        StringBuilder content = new StringBuilder();
        content.append(q.getText()).append("\n\n");
        content.append("1) ").append(q.getOptA()).append("\n");
        content.append("2) ").append(q.getOptB()).append("\n");
        content.append("3) ").append(q.getOptC()).append("\n");
        content.append("4) ").append(q.getOptD()).append("\n");

        alert.setContentText(content.toString());

        ButtonType btn1 = new ButtonType("1");
        ButtonType btn2 = new ButtonType("2");
        ButtonType btn3 = new ButtonType("3");
        ButtonType btn4 = new ButtonType("4");

        alert.getButtonTypes().setAll(btn1, btn2, btn3, btn4);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == btn1) return 1;
            if (result.get() == btn2) return 2;
            if (result.get() == btn3) return 3;
            if (result.get() == btn4) return 4;
        }

        return -1;
    }

    private int addLivesWithCap(int livesToAdd, int pointsPerConvertedHeart) {
        int extraScoreFromConversion = 0;

        for (int i = 0; i < livesToAdd; i++) {
            if (sharedHearts < TOTAL_HEART_SLOTS) {
                sharedHearts++;
            } else {
                extraScoreFromConversion += pointsPerConvertedHeart;
            }
        }

        return extraScoreFromConversion;
    }

    private void revealRandomMineReward(Board board, boolean isPlayer1) {
        StackPane[][] buttons = isPlayer1 ? p1Buttons : p2Buttons;
        List<int[]> mines = new ArrayList<>();

        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (cell.getType() == CellType.MINE) {
                    StackPane tile = buttons[r][c];
                    if (tile == null || tile.getChildren().isEmpty()) continue;
                    Button btn = (Button) tile.getChildren().get(0);
                    if (!btn.isDisable()) {
                        mines.add(new int[]{r, c});
                    }
                }
            }
        }

        if (mines.isEmpty()) {
            return;
        }

        int idx = (int) (Math.random() * mines.size());
        int[] rc = mines.get(idx);
        int row = rc[0];
        int col = rc[1];

        boolean[][] revealedArray = isPlayer1 ? revealedCellsP1 : revealedCellsP2;
        if (revealedArray != null && !revealedArray[row][col]) {
            revealedArray[row][col] = true;
            if (isPlayer1) revealedCountP1++;
            else revealedCountP2++;
            recalcFlagsLeft(isPlayer1);
        }

        StackPane tile = buttons[row][col];
        Button button = (Button) tile.getChildren().get(0);

        // (#4/#5) remove only visual flag; no refund
        autoRemoveFlagIfPresent(board, row, col, button, isPlayer1);

        button.setText("💣");
        button.getStyleClass().removeAll("cell-hidden");
        if (!button.getStyleClass().contains("cell-revealed")) {
            button.getStyleClass().add("cell-revealed");
        }
        if (!button.getStyleClass().contains("cell-mine")) {
            button.getStyleClass().add("cell-mine");
        }
        button.setDisable(true);

        if (isPlayer1) {
            minesLeft1 = Math.max(0, minesLeft1 - 1);
        } else {
            minesLeft2 = Math.max(0, minesLeft2 - 1);
        }

        updateScoreAndMineLabels();

        // (#2) Either board finishing ends the match (keep ||)
        if (!gameOver && sharedHearts > 0 && (minesLeft1 == 0 || minesLeft2 == 0)) {
            gameWon = true;
            onGameOver();
        }
    }

    private void revealArea3x3Reward(Board board, boolean isPlayer1) {
        StackPane[][] buttons = isPlayer1 ? p1Buttons : p2Buttons;
        int rows = board.getRows();
        int cols = board.getCols();

        List<int[]> centers = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (cell.getType() == CellType.MINE) continue;

                StackPane tile = buttons[r][c];
                if (tile == null || tile.getChildren().isEmpty()) continue;
                Button btn = (Button) tile.getChildren().get(0);
                if (!btn.isDisable()) {
                    centers.add(new int[]{r, c});
                }
            }
        }

        if (centers.isEmpty()) {
            return;
        }

        int idx = (int) (Math.random() * centers.size());
        int[] rc = centers.get(idx);
        int centerRow = rc[0];
        int centerCol = rc[1];

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = centerRow + dr;
                int c = centerCol + dc;
                if (r < 0 || c < 0 || r >= rows || c >= cols) continue;

                Cell cell = board.getCell(r, c);
                if (cell.getType() == CellType.MINE) continue;

                StackPane tile = buttons[r][c];
                if (tile == null || tile.getChildren().isEmpty()) continue;
                Button btn = (Button) tile.getChildren().get(0);
                if (btn.isDisable()) continue;

                // (#4/#5) remove only visual flag; no refund
                autoRemoveFlagIfPresent(board, r, c, btn, isPlayer1);
                revealSingleCell(board, r, c, btn, tile, isPlayer1);
            }
        }
    }

    private void showQuestionResultPopup(Question q,
                                         boolean correct,
                                         int netScoreChange,
                                         int livesBefore,
                                         int livesAfter,
                                         String extraInfo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Question Result");

        String difficultyText = q != null ? q.getDifficulty() : "Unknown";

        StringBuilder msg = new StringBuilder();
        msg.append("You answered a ").append(difficultyText).append(" question.\n\n");
        msg.append(correct ? "Your answer is CORRECT!\n" : "Your answer is WRONG.\n");
        msg.append("Score change: ").append(netScoreChange >= 0 ? "+" : "").append(netScoreChange).append("\n");

        int livesDelta = livesAfter - livesBefore;
        msg.append("Lives change: ").append(livesDelta >= 0 ? "+" : "").append(livesDelta).append("\n");

        if (extraInfo != null && !extraInfo.isBlank()) {
            msg.append("\n").append(extraInfo).append("\n");
        }

        msg.append("\nNew score: ").append(score).append("\n");
        msg.append("New lives: ").append(sharedHearts).append("/").append(TOTAL_HEART_SLOTS);

        alert.setHeaderText(null);
        alert.setContentText(msg.toString());
        alert.showAndWait();
    }

    private void activateQuestion(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {
        resetIdleHintTimer();

        int activationPoints = getActivationPoints();
        int livesBefore = sharedHearts;
        int scoreBefore = score;

        score -= activationPoints;

        Question q = getRandomQuestionFromPool();
        if (q == null) {
            button.setDisable(true);
            updateScoreAndMineLabels();
            buildHeartsBar();
            return;
        }

        int chosenOption = showQuestionDialog(q);
        if (chosenOption == -1) {
            button.setDisable(true);
            updateScoreAndMineLabels();
            buildHeartsBar();
            return;
        }

        boolean correct = (chosenOption == q.getCorrectOption());
        String qDiff = q.getDifficulty() != null ? q.getDifficulty().toLowerCase() : "easy";
        String extraInfo = "";

        if (this.difficulty == Difficulty.EASY) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    score += 3;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    mistakeMade = true;
                    if (Math.random() < 0.5) {
                        score -= 3;
                        extraInfo = "Wrong answer: you lost 3 points.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    revealRandomMineReward(board, isPlayer1);
                    score += 6;
                    extraInfo = "Correct! One mine was revealed automatically for you.";
                } else {
                    mistakeMade = true;
                    if (Math.random() < 0.5) {
                        score -= 6;
                        extraInfo = "Wrong answer: you lost 6 points.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    revealArea3x3Reward(board, isPlayer1);
                    score += 10;
                    extraInfo = "Correct! A 3×3 area of cells was revealed for you.";
                } else {
                    score -= 10;
                    extraInfo = "Wrong answer: you lost 10 points.";
                    mistakeMade = true;
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    score += 15;
                    int converted = addLivesWithCap(2, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    }
                } else {
                    score -= 15;
                    sharedHearts = Math.max(0, sharedHearts - 1);
                    mistakeMade = true;
                    extraInfo = "Wrong answer: you lost 15 points and 1 life.";
                }
            }

        } else if (this.difficulty == Difficulty.MEDIUM) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    score += 8;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    score -= 8;
                    extraInfo = "Wrong answer: you lost 8 points.";
                    mistakeMade = true;
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    score += 10;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    mistakeMade = true;
                    if (Math.random() < 0.5) {
                        score -= 10;
                        sharedHearts = Math.max(0, sharedHearts - 1);
                        extraInfo = "Wrong answer: you lost 10 points and 1 life.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    score += 15;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    score -= 15;
                    sharedHearts = Math.max(0, sharedHearts - 1);
                    mistakeMade = true;
                    extraInfo = "Wrong answer: you lost 15 points and 1 life.";
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    score += 20;
                    int converted = addLivesWithCap(2, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    }
                } else {
                    mistakeMade = true;
                    score -= 20;
                    if (Math.random() < 0.5) {
                        sharedHearts = Math.max(0, sharedHearts - 1);
                        extraInfo = "Wrong answer: you lost 20 points and 1 life.";
                    } else {
                        sharedHearts = Math.max(0, sharedHearts - 2);
                        extraInfo = "Wrong answer: you lost 20 points and 2 lives.";
                    }
                }
            }

        } else if (this.difficulty == Difficulty.HARD) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    score += 10;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    } else {
                        extraInfo = "Correct! You gained 1 life.";
                    }
                } else {
                    score -= 10;
                    sharedHearts = Math.max(0, sharedHearts - 1);
                    mistakeMade = true;
                    extraInfo = "Wrong answer: you lost 10 points and 1 life.";
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    score += 15;
                    int livesToAdd = (Math.random() < 0.5) ? 1 : 2;
                    int converted = addLivesWithCap(livesToAdd, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "Correct! You gained " + livesToAdd +
                                " lives, but some were converted to +" + converted +
                                " points because you were at max lives.";
                    } else {
                        extraInfo = "Correct! You gained " + livesToAdd + " lives.";
                    }
                } else {
                    mistakeMade = true;
                    score -= 15;
                    int livesLost = (Math.random() < 0.5) ? 1 : 2;
                    sharedHearts = Math.max(0, sharedHearts - livesLost);
                    extraInfo = "Wrong answer: you lost 15 points and " +
                            livesLost + " life" + (livesLost > 1 ? "s." : ".");
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    score += 20;
                    int converted = addLivesWithCap(2, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "Correct! You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    } else {
                        extraInfo = "Correct! You gained 2 lives.";
                    }
                } else {
                    mistakeMade = true;
                    score -= 20;
                    sharedHearts = Math.max(0, sharedHearts - 2);
                    extraInfo = "Wrong answer: you lost 20 points and 2 lives.";
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    score += 40;
                    int converted = addLivesWithCap(3, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "Correct! You gained 3 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    } else {
                        extraInfo = "Correct! You gained 3 lives.";
                    }
                } else {
                    mistakeMade = true;
                    score -= 40;
                    sharedHearts = Math.max(0, sharedHearts - 3);
                    extraInfo = "Wrong answer: you lost 40 points and 3 lives.";
                }
            }
        }

        buildHeartsBar();
        button.setDisable(true);

        updateScoreAndMineLabels();

        if (checkLoseAndHandle()) return;

        int livesAfter = sharedHearts;
        int netScoreChange = score - scoreBefore;
        showQuestionResultPopup(q, correct, netScoreChange, livesBefore, livesAfter, extraInfo);
    }

 

    private void refreshMusicIconFromSettings() {
        if (musicButton == null) return;
        if (!(musicButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isMusicEnabled();

        if (enabled && !SoundManager.isMusicOn()) {
            SoundManager.startMusic();
        } else if (!enabled && SoundManager.isMusicOn()) {
            SoundManager.stopMusic();
        }

        String iconPath = enabled ? "/Images/music.png" : "/Images/music_mute.png";
        double size = 40;

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

    private boolean checkLoseAndHandle() {
        if (gameOver) return true;
        if (sharedHearts > 0) return false;

        gameWon = false;
        onGameOver();
        return true;
    }

    private int countType(Board board, CellType type) {
        int count = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getCell(r, c).getType() == type) count++;
            }
        }
        return count;
    }

    // Reveal both boards' cells at the end of the game
    private void revealAllCellsOnBoardVisualOnly(Board board, StackPane[][] buttons) {
        if (board == null || buttons == null) return;

        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StackPane tile = buttons[r][c];
                if (tile == null || tile.getChildren().isEmpty()) continue;

                Button btn = (Button) tile.getChildren().get(0);
                Cell cell = board.getCell(r, c);

                revealButtonVisualOnly(btn, cell);
            }
        }
    }

    private void revealButtonVisualOnly(Button button, Cell cell) {
        if (button == null || cell == null) return;

        button.setGraphic(null);
        button.setText("");

        button.getStyleClass().removeAll(
                "cell-hidden", "cell-flagged",
                "cell-mine", "cell-question", "cell-surprise",
                "cell-number", "cell-empty"
        );

        if (!button.getStyleClass().contains("cell-revealed")) {
            button.getStyleClass().add("cell-revealed");
        }
        button.setDisable(true);

        switch (cell.getType()) {
            case MINE -> {
                button.setText("💣");
                button.getStyleClass().add("cell-mine");
            }
            case QUESTION -> {
                try {
                    Image img = new Image(getClass().getResourceAsStream("/Images/question-mark.png"));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(20);
                    iv.setFitHeight(20);
                    iv.setPreserveRatio(true);
                    button.setGraphic(iv);
                } catch (Exception ex) {
                    button.setText("?");
                }
                button.getStyleClass().add("cell-question");
            }
            case SURPRISE -> {
                try {
                    Image img = new Image(getClass().getResourceAsStream("/Images/giftbox.png"));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(20);
                    iv.setFitHeight(20);
                    iv.setPreserveRatio(true);
                    button.setGraphic(iv);
                } catch (Exception ex) {
                    button.setText("★");
                }
                button.getStyleClass().add("cell-surprise");
            }
            case NUMBER -> {
                button.setText(String.valueOf(cell.getAdjacentMines()));
                button.getStyleClass().add("cell-number");
            }
            case EMPTY -> {
                button.setText("");
                button.getStyleClass().add("cell-empty");
            }
        }
    }

    private void revealAllBoardsVisualOnly() {
        revealAllCellsOnBoardVisualOnly(board1, p1Buttons);
        revealAllCellsOnBoardVisualOnly(board2, p2Buttons);
    }
}
