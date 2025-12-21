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
import model.Question;
import util.SessionManager;
import util.UIAnimations;
import util.SoundManager;

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
    
    private int flagsUsed1;
    private int flagsUsed2;
    
    private int revealedCountP1;
    private int revealedCountP2;

    private int totalCellsP1;
    private int totalCellsP2;
    
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

    // Track cells that were already revealed (for scoring & surprise/question second-click)
    private boolean[][] revealedCellsP1;
    private boolean[][] revealedCellsP2;

    // True if any mistake happened (for win-without-mistakes flag)
    private boolean mistakeMade = false;

    private static final int TOTAL_HEART_SLOTS = 10;

    // ============================================================
    // INIT
    // ============================================================

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
        
     //  starting flags per difficulty
        this.flagsLeft1 = getInitialFlagsForDifficulty(difficulty);
        this.flagsLeft2 = getInitialFlagsForDifficulty(difficulty);
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

        this.flagsUsed1 = 0;
        this.flagsUsed2 = 0;

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

        // --- SYNC ICONS WITH SETTINGS ---
        refreshSoundIconFromSettings();   // sound (clicks)
        refreshMusicIconFromSettings();   // music (background)
        
        
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
            Image img = new Image(getClass().getResourceAsStream("/Images/cursor_forbidden.png"));
            forbiddenCursor = new ImageCursor(img, img.getWidth() / 2, img.getHeight() / 2);
        } catch (Exception e) {
            forbiddenCursor = null;
        }
    }
    
    


    // ============================================================
    // BOARD BUILDING
    // ============================================================

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

            if (gameOver) {
                return;
            }
            if (isPaused) {
                return;
            }

            // Right click = FLAG
            if (e.getButton() == MouseButton.SECONDARY) {
                if ((tileIsPlayer1 && !isPlayer1Turn) ||
                    (!tileIsPlayer1 && isPlayer1Turn)) {
                    return;
                }

                if (button.isDisable()) {
                    return;
                }

                toggleFlag(board, r, c, button, tileIsPlayer1);
                return;
            }

            // Only left click past this point
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

    // ============================================================
    // FLAGGING
    // ============================================================

   /**
 * Toggles a flag icon on a covered cell.
 * If a flag is present, removes it; otherwise adds a flag graphic.
 * If a QUESTION or SURPRISE cell is flagged, score is reduced by 3 points.
 * If a non-mine is flagged, marks mistakeMade = true.
 */
private void toggleFlag(Board board, int row, int col, Button button, boolean isPlayer1) {
    Cell cell = board.getCell(row, col);

    // If there is already a flag → remove it (no score / mines change)
    if (button.getGraphic() instanceof ImageView) {
        button.setGraphic(null);
        button.getStyleClass().remove("cell-flagged");
        // flags are NOT restored (you designed them as non-recoverable)
        updateScoreAndMineLabels();
        return;
    }

    // If trying to place a NEW flag but no flags left → show popup
    if (isPlayer1) {
    	recalcFlagsLeft(true); // make sure flagsLeft1 is up to date
        if (flagsLeft1 <= 0) {
            showNoFlagsLeftAlert();
            return;
        }
        flagsUsed1++;          // consume permanently (non-recoverable)
        recalcFlagsLeft(true); // update flagsLeft1 after consuming
    } else {
    	recalcFlagsLeft(false);
        if (flagsLeft2 <= 0) {
            showNoFlagsLeftAlert();
            return;
        }
        flagsUsed2++;
        recalcFlagsLeft(false);
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

    // ----- scoring + mine counters -----
    if (cell.isMine()) {
        // show mine instead of just a flag
        button.setGraphic(null);
        button.setText("💣");
        button.getStyleClass().add("cell-mine");
        button.getStyleClass().add("cell-revealed");

        // this mine is now safely found → it shouldn't be clickable anymore
        button.setDisable(true);

        // +1 point for correctly flagged mine
        score += 1;

        if (isPlayer1) {
            minesLeft1 = Math.max(0, minesLeft1 - 1);
        } else {
            minesLeft2 = Math.max(0, minesLeft2 - 1);
        }
        // mark as revealed
        boolean[][] revealedArray = isPlayer1 ? revealedCellsP1 : revealedCellsP2;
        if (revealedArray != null && !revealedArray[row][col]) {
            revealedArray[row][col] = true;

            if (isPlayer1) revealedCountP1++;
            else revealedCountP2++;

            recalcFlagsLeft(isPlayer1);
        }



    } else {
        // Wrong flag on non-mine → mistake and -3 points
        mistakeMade = true;
        score -= 3;
    }

    updateScoreAndMineLabels();

    // Win condition: all mines on at least one board are flagged and hearts > 0
    if (!gameOver && sharedHearts > 0 && (minesLeft1 == 0 || minesLeft2 == 0)) {
        gameWon = true;
        onGameOver();
    }
}

private void showNoFlagsLeftAlert() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("No Flags Left");
    alert.setHeaderText(null);
    alert.setContentText("You have no flags left.\nUsed flags cannot be recovered.");
    alert.showAndWait();
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
        flagsLeft1 = Math.max(0, allowed - flagsUsed1);
    } else {
        flagsLeft2 = Math.max(0, allowed - flagsUsed2);
    }
}


    // ============================================================
    // REVEAL LOGIC (FIRST CLICK)
    // ============================================================

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
                button.setDisable(true);
                button.getStyleClass().addAll("cell-revealed", "cell-mine");

                int heartsBefore = sharedHearts;
                sharedHearts = Math.max(0, sharedHearts - 1);
                if (sharedHearts < heartsBefore) {
                    // revealing a mine and actually losing a life = mistake
                    mistakeMade = true;
                }

                triggerExplosion(tile);

                if (isPlayer1) {
                    minesLeft1 -= 1;
                } else {
                    minesLeft2 -= 1;
                }
                buildHeartsBar();
                updateScoreAndMineLabels();
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
                    if (isPlayer1) surprisesLeft1 = Math.max(0, surprisesLeft1 - 1);
                    else           surprisesLeft2 = Math.max(0, surprisesLeft2 - 1);

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
     * Handles a cell click: reveal, cascade if empty, and trigger surprise/question activation
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

        // Normal reveal (first time or other types)
        revealSingleCell(board, row, col, button, tile, isPlayer1);

        // Cascade for EMPTY cells
        if (cell.getType() == CellType.EMPTY || cell.getType() == CellType.QUESTION || cell.getType() == CellType.SURPRISE) {
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

    // ============================================================
    // TURN HANDLING
    // ============================================================

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

    // ============================================================
    // TIMER
    // ============================================================

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

    // ============================================================
    // TOP BAR BUTTONS (EXIT / HELP / BACK / PAUSE / SOUND / MUSIC)
    // ============================================================

    /**
     * Once clicking on exit button: saves GIVE_UP and closes the application.
     */
    @FXML
    private void onExitBtnClicked() {
        saveGiveUpGame();
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
     * Clicking on back button: saves GIVE_UP, stops the timer and returns to the main menu screen.
     */
    @FXML
    private void onBackBtnClicked() throws IOException {
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
        // Currently unused; can be wired from FXML if needed
    }

    // ============================================================
    // EXPLOSION ANIMATION
    // ============================================================

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

    // ============================================================
    // GAME OVER + SAVE
    // ============================================================

    /**
     * Main game-over handler: prevents duplicate handling, stops the timer,
     * saves the game result, and opens the end-game screen.
     */
    private void onGameOver() {
        if (gameOver) {
            return;
        }
        gameOver = true;

        // 4. convert remaining hearts to points when the game is won
        if (gameWon && sharedHearts > 0) {
            int perHeart = switch (difficulty) {
                case EASY   -> 5;
                case MEDIUM -> 8;
                case HARD   -> 12;
            };
            int bonus = sharedHearts * perHeart;
            score += bonus;
            updateScoreAndMineLabels();  // refresh score label before leaving screen
        }

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

    /**
     * Saves a GIVE_UP record when the players leave the game
     * via Back/Exit before it naturally ends.
     */
    private void saveGiveUpGame() {
        // If there is no config, we can't build a proper Game record
        if (config == null) {
            return;
        }

        // If the game already ended (WIN/LOSE) and was saved,
        // don't also save a GIVE_UP on top of it.
        if (gameOver) {
            return;
        }

        GameResult result = GameResult.GIVE_UP;

        // Logged-in players (registered users)
        Player p1 = SessionManager.getPlayer1();
        Player p2 = SessionManager.getPlayer2();

        String player1Official = (p1 != null) ? p1.getOfficialName() : null;
        String player2Official = (p2 != null) ? p2.getOfficialName() : null;

        // Nicknames come from the GameConfig
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

    // ============================================================
    // DEBUG
    // ============================================================

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

    // ============================================================
    // SURPRISE ACTIVATION (SECOND CLICK)
    // ============================================================

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

        int activationPoints = getActivationPoints();
        int goodBonus = getSurpriseGoodBonusPoints();
        int badPenalty = getSurpriseBadPenaltyPoints();

        // Base activation points (positive)
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

        // Disable this surprise cell so it cannot be activated again
        button.setDisable(true);

        // Refresh score / mines labels
        updateScoreAndMineLabels();

        if (checkLoseAndHandle()) return;
        
        // Show popup with result
        int livesAfter = sharedHearts;
        int netScoreChange = score - scoreBefore;
        showSurprisePopup(good, netScoreChange, livesBefore, livesAfter);
    }


    // Base score for activating a surprise/question (second click) per difficulty
    private int getActivationPoints() {
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

    // ============================================================
    // QUESTION SYSTEM (SECOND CLICK ON QUESTION)
    // ============================================================

    /**
     * Returns a random question from the CSV using QuestionsManagerController logic.
     */
    private Question getRandomQuestionFromPool() {
        // QuestionsManagerController is in the same package (control),
        // so no import is required.
        List<Question> all = QuestionsManagerController.loadQuestionsForGame();

        if (all == null || all.isEmpty()) {
            System.out.println("No questions found.");
            return null;
        }

        int idx = (int) (Math.random() * all.size());
        return all.get(idx);
    }

    /**
     * Shows a multiple-choice question dialog and returns the chosen option (1-4).
     */
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

    /**
     * Adds lives but caps at TOTAL_HEART_SLOTS.
     * Extra lives are converted to points at "pointsPerConvertedHeart".
     * Returns the extra score gained from conversion.
     */
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

    /**
     * Reveals one random mine visually (if any) on the given board for the given player,
     * without affecting hearts. It does decrease the mines-left counter and updates labels.
     */
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

        StackPane tile = buttons[row][col];
        Button button = (Button) tile.getChildren().get(0);

        // Show the mine but do NOT change hearts or trigger explosion
        button.setText("💣");
        button.getStyleClass().removeAll("cell-hidden", "cell-flagged");
        if (!button.getStyleClass().contains("cell-revealed")) {
            button.getStyleClass().add("cell-revealed");
        }
        if (!button.getStyleClass().contains("cell-mine")) {
            button.getStyleClass().add("cell-mine");
        }
        button.setDisable(true);

        // Decrease mines-left count
        if (isPlayer1) {
            minesLeft1 = Math.max(0, minesLeft1 - 1);
        } else {
            minesLeft2 = Math.max(0, minesLeft2 - 1);
        }
        updateScoreAndMineLabels();
    }

    /**
     * Reveals up to a 3x3 area of NON-MINE cells automatically for the given player.
     * Uses normal revealSingleCell for non-mine cells (so score & safeCellsRemaining update),
     * skips mines entirely to avoid punishing the player on a reward.
     */
    private void revealArea3x3Reward(Board board, boolean isPlayer1) {
        StackPane[][] buttons = isPlayer1 ? p1Buttons : p2Buttons;
        int rows = board.getRows();
        int cols = board.getCols();

        // Collect candidate centers (cells that are not mines and not disabled)
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
                if (cell.getType() == CellType.MINE) {
                    // Skip mines in this reward area
                    continue;
                }

                StackPane tile = buttons[r][c];
                if (tile == null || tile.getChildren().isEmpty()) continue;
                Button btn = (Button) tile.getChildren().get(0);
                if (btn.isDisable()) continue;

                // Normal reveal for non-mine cells, so score & safeCellsRemaining work as usual
                revealSingleCell(board, r, c, btn, tile, isPlayer1);
            }
        }
    }

    /**
     * Shows a popup describing what happened after answering a question.
     */
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

    /**
     * Activates a QUESTION cell on second click.
     * - Always costs activation points (5/8/12 by game difficulty).
     * - Loads random question from CSV.
     * - Rewards/penalties depend on GAME difficulty and QUESTION difficulty.
     * - Lives are capped at TOTAL_HEART_SLOTS; extra are converted to points.
     * - Disables the cell and shows a result popup.
     */
    private void activateQuestion(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {

        int activationPoints = getActivationPoints(); // 5 EASY, 8 MEDIUM, 12 HARD
        int livesBefore = sharedHearts;
        int scoreBefore = score;

        // Always costs the player activation points
        score -= activationPoints;

        // Get a random question
        Question q = getRandomQuestionFromPool();
        if (q == null) {
            // No question available: just block the cell and update UI
            button.setDisable(true);
            updateScoreAndMineLabels();
            buildHeartsBar();
            return;
        }

        int chosenOption = showQuestionDialog(q);
        if (chosenOption == -1) {
            // No answer chosen, just block cell
            button.setDisable(true);
            updateScoreAndMineLabels();
            buildHeartsBar();
            return;
        }

        boolean correct = (chosenOption == q.getCorrectOption());
        String qDiff = q.getDifficulty() != null ? q.getDifficulty().toLowerCase() : "easy";
        String extraInfo = "";

        // GAME MODE: EASY
        if (this.difficulty == Difficulty.EASY) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    // +1 life and +3 points (with cap & conversion)
                    score += 3;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    // Wrong: -3 points or 0, 50% each
                    if (Math.random() < 0.5) {
                        score -= 3;
                        extraInfo = "Wrong answer: you lost 3 points.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    // Reveal one mine automatically (no heart penalty) +6 points
                    revealRandomMineReward(board, isPlayer1);
                    score += 6;
                    extraInfo = "Correct! One mine was revealed automatically for you.";
                } else {
                    // Wrong: -6 points or 0, 50% each
                    if (Math.random() < 0.5) {
                        score -= 6;
                        extraInfo = "Wrong answer: you lost 6 points.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    // Reveal 3x3 cells automatically +10 points
                    revealArea3x3Reward(board, isPlayer1);
                    score += 10;
                    extraInfo = "Correct! A 3×3 area of cells was revealed for you.";
                } else {
                    // Wrong: -10 points
                    score -= 10;
                    extraInfo = "Wrong answer: you lost 10 points.";
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    // +2 lives and +15 points, with cap & conversion
                    score += 15;
                    int converted = addLivesWithCap(2, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    }
                } else {
                    // Wrong: -15 points and -1 life
                    score -= 15;
                    sharedHearts = Math.max(0, sharedHearts - 1);
                    extraInfo = "Wrong answer: you lost 15 points and 1 life.";
                }
            }

        // GAME MODE: MEDIUM
        } else if (this.difficulty == Difficulty.MEDIUM) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    // Correct: +1 life and +8 points
                    score += 8;
                    int converted = addLivesWithCap(1, activationPoints); // 8 per extra life in MEDIUM
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    // Wrong: -8 points
                    score -= 8;
                    extraInfo = "Wrong answer: you lost 8 points.";
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    // Correct: +1 life and +10 points
                    score += 10;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    // Wrong: either (-10 points and -1 life) OR no penalty (50% each)
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
                    // Correct: +1 life and +15 points
                    score += 15;
                    int converted = addLivesWithCap(1, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    // Wrong: -15 points and -1 life
                    score -= 15;
                    sharedHearts = Math.max(0, sharedHearts - 1);
                    extraInfo = "Wrong answer: you lost 15 points and 1 life.";
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    // Correct: +2 lives and +20 points
                    score += 20;
                    int converted = addLivesWithCap(2, activationPoints);
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    }
                } else {
                    // Wrong: either (-20 points and -1 life) OR (-20 points and -2 lives) (50% each)
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

        // GAME MODE: HARD
        } else if (this.difficulty == Difficulty.HARD) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    // Correct: +1 life and +10 points
                    score += 10;
                    int converted = addLivesWithCap(1, activationPoints); // 12 per extra life in HARD
                    score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    } else {
                        extraInfo = "Correct! You gained 1 life.";
                    }
                } else {
                    // Wrong: -10 points and -1 life
                    score -= 10;
                    sharedHearts = Math.max(0, sharedHearts - 1);
                    extraInfo = "Wrong answer: you lost 10 points and 1 life.";
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    // Correct: either +1 life and +15 points OR +2 lives and +15 points (50% each)
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
                    // Wrong: either (-15 points and -1 life) OR (-15 points and -2 lives) (50% each)
                    score -= 15;
                    int livesLost = (Math.random() < 0.5) ? 1 : 2;
                    sharedHearts = Math.max(0, sharedHearts - livesLost);
                    extraInfo = "Wrong answer: you lost 15 points and " +
                            livesLost + " life" + (livesLost > 1 ? "s." : ".");
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    // Correct: +2 lives and +20 points
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
                    // Wrong: -20 points and -2 lives
                    score -= 20;
                    sharedHearts = Math.max(0, sharedHearts - 2);
                    extraInfo = "Wrong answer: you lost 20 points and 2 lives.";
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    // Correct: +3 lives and +40 points
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
                    // Wrong: -40 points and -3 lives
                    score -= 40;
                    sharedHearts = Math.max(0, sharedHearts - 3);
                    extraInfo = "Wrong answer: you lost 40 points and 3 lives.";
                }
            }
        }

        // Update hearts bar after any life changes
        buildHeartsBar();

        // Block this question cell so it can't be activated again
        button.setDisable(true);

        // Update labels & show result popup
        updateScoreAndMineLabels();
        
        if (checkLoseAndHandle()) return;

        int livesAfter = sharedHearts;
        int netScoreChange = score - scoreBefore;
        showQuestionResultPopup(q, correct, netScoreChange, livesBefore, livesAfter, extraInfo);
    }

    // ============================================================
    // SMART HINTS
    // ============================================================

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
/*
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
        }*/
    }

    // ============================================================
    // SOUND / MUSIC ICON HELPERS
    // ============================================================

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
            size = 40;
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


}
