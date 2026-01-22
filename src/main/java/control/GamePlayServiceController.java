package control;

import java.util.ArrayDeque;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import model.Board;
import model.Cell;
import model.CellRevealResult;
import model.CellType;
import model.SysData;
import util.DialogUtil;

public class GamePlayServiceController {

    private final GameStateController s;
    private final GameUIServiceController ui;
    private final GameHistoryServiceController history;
    private final Runnable showEndGameScreenCallback;
    private GameBonusServiceController bonusService;


    
    public GamePlayServiceController(GameStateController s, GameUIServiceController ui, GameHistoryServiceController history, Runnable showEndGameScreenCallback) {
        this.s = s;
        this.ui = ui;
        this.history = history;
        this.showEndGameScreenCallback = showEndGameScreenCallback;
    }

    public void initializeNewMatch() {
        s.board1 = new Board(s.difficulty);
        s.board2 = new Board(s.difficulty);

        printBoardDebug("Player 1 Board", s.board1);
        printBoardDebug("Player 2 Board", s.board2);

        s.minesLeft1 = s.board1.getMineCount();
        s.minesLeft2 = s.board2.getMineCount();

        s.surprisesLeft1 = countType(s.board1, CellType.SURPRISE);
        s.surprisesLeft2 = countType(s.board2, CellType.SURPRISE);

        s.questionsLeft1 = countType(s.board1, CellType.QUESTION);
        s.questionsLeft2 = countType(s.board2, CellType.QUESTION);

        s.revealedCellsP1 = new boolean[s.board1.getRows()][s.board1.getCols()];
        s.revealedCellsP2 = new boolean[s.board2.getRows()][s.board2.getCols()];

        s.totalCellsP1 = s.board1.getRows() * s.board1.getCols();
        s.totalCellsP2 = s.board2.getRows() * s.board2.getCols();

        s.revealedCountP1 = 0;
        s.revealedCountP2 = 0;

        s.safeCellsRemaining1 = s.totalCellsP1 - s.board1.getMineCount();
        s.safeCellsRemaining2 = s.totalCellsP2 - s.board2.getMineCount();

        s.sharedHearts = s.difficulty.getInitialLives();
        s.score = 0;
        s.isPlayer1Turn = true;
        s.isPaused = false;
        s.gameOver = false;
        s.gameWon = false;
        s.mistakeMade = false;
    }
    
    public void setBonusService(GameBonusServiceController bonusService) {
        this.bonusService = bonusService;
    }


    public void togglePause() {
        s.isPaused = !s.isPaused;

        ui.updatePauseIcon();
        ui.setBoardsOpacity(s.isPaused ? 0.6 : 1.0);

        if (s.isPaused) {
            ui.pauseTimer();
        } else {
            if (SysData.isTimerEnabled()) {
                ui.resumeTimer();
            }
        }
    }

    public void switchTurn() {
        s.isPlayer1Turn = !s.isPlayer1Turn;

        if (s.isPlayer1Turn) {
            s.wrongFlagsThisTurnP1 = 0;
            s.flaggingLockedThisTurnP1 = false;
        } else {
            s.wrongFlagsThisTurnP2 = 0;
            s.flaggingLockedThisTurnP2 = false;
        }

        ui.applyTurnStateToBoards();
        ui.updateScoreAndMineLabels();
    }
    
    /**
     * Toggles a flag icon on a covered cell.
     *  If it's a MINE: right-click reveals instantly and gives +1 score (Option A).
     *  If it's NOT a mine: placing a flag consumes a non-reusable flag (flagsPlaced = flagsUsed).
     *  Unflagging removes only the visual marker and does NOT refund a flag.
     *  Wrong flag is a mistake and costs -3 score.
     */
    public void toggleFlag(Board board, int row, int col, Button button, boolean isPlayer1) {
        if (s.gameOver || s.isPaused) return;

        Cell cell = board.getCell(row, col);
        if (cell == null) return;

        if (button.isDisable()) return;

        // Unflag: always allowed, does NOT reduce wrong-flag counter (Option 1).
        if (button.getStyleClass().contains("cell-flagged")) {
            button.setGraphic(null);
            button.setText("");
            button.getStyleClass().remove("cell-flagged");

            ui.updateScoreAndMineLabels();
            return;
        }

        // If flagging is locked for this player during this turn, block NEW flags (but unflagging above still works).
        boolean locked = isPlayer1 ? s.flaggingLockedThisTurnP1 : s.flaggingLockedThisTurnP2;
        if (locked) {
            DialogUtil.show(Alert.AlertType.INFORMATION, null, "Flagging Disabled",
                    "You reached " + GameStateController.WRONG_FLAGS_LIMIT_PER_TURN +
                            " wrong flags this turn.\nFlagging is disabled until your turn ends.");
            return;
        }

     

        // Correct flag on a mine: ALWAYS allowed (even if flagsLeft is 0).
        if (cell.isMine()) {
            button.setGraphic(null);
            button.setText("💣");

            button.getStyleClass().removeAll(
                "cell-hidden", "cell-flagged",
                "cell-number", "cell-empty", "cell-question", "cell-surprise"
            );

            if (!button.getStyleClass().contains("cell-revealed")) button.getStyleClass().add("cell-revealed");
            if (!button.getStyleClass().contains("cell-activated")) button.getStyleClass().add("cell-activated");

            // Mine base + "correctly flagged" variant
            if (!button.getStyleClass().contains("cell-mine")) button.getStyleClass().add("cell-mine");
            if (!button.getStyleClass().contains("cell-mine-flagged")) button.getStyleClass().add("cell-mine-flagged");

            button.setDisable(true);

            s.score += 1;
            if (isPlayer1) s.minesLeft1 = Math.max(0, s.minesLeft1 - 1);
            else s.minesLeft2 = Math.max(0, s.minesLeft2 - 1);

            boolean[][] revealedArray = isPlayer1 ? s.revealedCellsP1 : s.revealedCellsP2;
            if (revealedArray != null && !revealedArray[row][col]) {
                revealedArray[row][col] = true;
                if (isPlayer1) s.revealedCountP1++;
                else s.revealedCountP2++;
            }

            ui.updateScoreAndMineLabels();

            if (!s.gameOver && s.sharedHearts > 0 && (s.minesLeft1 == 0 || s.minesLeft2 == 0)) {
                s.gameWon = true;
                onGameOver();
            }
            return;
        }

        // Wrong flag (non-mine): place visual flag, apply penalty, increment wrong flags this turn.
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


        s.mistakeMade = true;
        s.score -= 3;

        if (isPlayer1) {
            s.wrongFlagsThisTurnP1++;
            if (s.wrongFlagsThisTurnP1 >= GameStateController.WRONG_FLAGS_LIMIT_PER_TURN) {
                s.flaggingLockedThisTurnP1 = true;
                DialogUtil.show(Alert.AlertType.WARNING, null, "Flagging Disabled",
                        "Too many wrong flags this turn.\nFlagging is disabled until your turn ends.");
            }
        } else {
            s.wrongFlagsThisTurnP2++;
            if (s.wrongFlagsThisTurnP2 >= GameStateController.WRONG_FLAGS_LIMIT_PER_TURN) {
                s.flaggingLockedThisTurnP2 = true;
                DialogUtil.show(Alert.AlertType.WARNING, null, "Flagging Disabled",
                        "Too many wrong flags this turn.\nFlagging is disabled until your turn ends.");
            }
        }

        ui.updateScoreAndMineLabels();
        checkSellHeartCondition();

    }




    private void autoRemoveFlagIfPresent(Board board, int row, int col, Button button, boolean isPlayer1, boolean batchMode) {
        if (!button.getStyleClass().contains("cell-flagged")) return;

        button.setGraphic(null);
        button.setText("");
        button.getStyleClass().remove("cell-flagged");

        if (!batchMode) {
            ui.updateScoreAndMineLabels();
        }    }

  
   
    public boolean revealAndMaybeActivate(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {
        Cell cell = board.getCell(row, col);

        if (button.getStyleClass().contains("cell-flagged")) {
            autoRemoveFlagIfPresent(board, row, col, button, isPlayer1,true);
        }

        // SECOND CLICK cases are handled in GameBonusService (called by UI click handler before this method).
        // Here we do normal reveal.
        revealSingleCell(board, row, col, button, tile, isPlayer1, false);

        if (cell.getType() == CellType.EMPTY || cell.getAdjacentMines() == 0) {
            cascadeReveal(board, row, col, isPlayer1);
        }

        return true;
    }

    public void revealSingleCell(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1, boolean batchMode) {
        Cell cell = board.getCell(row, col);

        if (button.isDisable()) return;

        boolean[][] revealedArray = isPlayer1 ? s.revealedCellsP1 : s.revealedCellsP2;
        boolean isFirstReveal = false;

        if (revealedArray != null) {
            if (revealedArray[row][col]) return;
            revealedArray[row][col] = true;
            isFirstReveal = true;
        }

        if (isFirstReveal) {
            if (isPlayer1) s.revealedCountP1++;
            else s.revealedCountP2++;

        }

        button.getStyleClass().removeAll(
                "cell-hidden", "cell-revealed",
                "cell-mine", "cell-question",
                "cell-surprise", "cell-number", "cell-empty"
        );
        //Update this switch command to a better one using Template Pattern:
//        switch (cell.getType()) {
//            case MINE -> {
//                button.setText("💣");
//                button.setDisable(true);
//                button.getStyleClass().addAll("cell-revealed", "cell-mine");
//
//                int heartsBefore = s.sharedHearts;
//                s.sharedHearts = Math.max(0, s.sharedHearts - 1);
//                if (s.sharedHearts < heartsBefore) {
//                    s.mistakeMade = true;
//                }
//
//                triggerExplosion(tile);
//
//                if (isPlayer1) s.minesLeft1 = Math.max(0, s.minesLeft1 - 1);
//                else s.minesLeft2 = Math.max(0, s.minesLeft2 - 1);
//
//                ui.buildHeartsBar();
//                ui.updateScoreAndMineLabels();
//
//                if (!s.gameOver && s.sharedHearts > 0 && (s.minesLeft1 == 0 || s.minesLeft2 == 0)) {
//                    s.gameWon = true;
//                    onGameOver();
//                    return;
//                }
//
//                if (checkLoseAndHandle()) return;
//            }
//
//            case QUESTION -> {
//                try {
//                    Image img = new Image(getClass().getResourceAsStream("/Images/question-mark.png"));
//                    ImageView iv = new ImageView(img);
//                    iv.setFitWidth(20);
//                    iv.setFitHeight(20);
//                    iv.setPreserveRatio(true);
//                    button.setGraphic(iv);
//                    button.getStyleClass().addAll("cell-revealed", "cell-question");
//                    if (isPlayer1) s.questionsLeft1 = Math.max(0, s.questionsLeft1 - 1);
//                    else s.questionsLeft2 = Math.max(0, s.questionsLeft2 - 1);
//                } catch (Exception ex) {
//                    button.setText("?");
//                    button.getStyleClass().addAll("cell-revealed", "cell-question");
//                }
//
//                if (isFirstReveal) {
//                    s.score += 1;
//                }
//            }
//
//            case SURPRISE -> {
//                try {
//                    Image img = new Image(getClass().getResourceAsStream("/Images/giftbox.png"));
//                    ImageView iv = new ImageView(img);
//                    iv.setFitWidth(20);
//                    iv.setFitHeight(20);
//                    iv.setPreserveRatio(true);
//                    button.setGraphic(iv);
//                    button.getStyleClass().addAll("cell-revealed", "cell-surprise");
//                    if (isPlayer1) s.surprisesLeft1 = Math.max(0, s.surprisesLeft1 - 1);
//                    else s.surprisesLeft2 = Math.max(0, s.surprisesLeft2 - 1);
//                } catch (Exception ex) {
//                    button.setText("★");
//                    button.getStyleClass().addAll("cell-revealed", "cell-surprise");
//                }
//
//                if (isFirstReveal) {
//                    s.score += 1;
//                }
//            }
//
//            case NUMBER -> {
//                int n = cell.getAdjacentMines();
//                button.setText(String.valueOf(n));
//                button.setDisable(true);
//                button.getStyleClass().addAll("cell-revealed", "cell-number");
//                s.score += 1;
//            }
//
//            case EMPTY -> {
//                button.setText("");
//                button.setDisable(true);
//                button.getStyleClass().addAll("cell-revealed", "cell-empty");
//                s.score += 1;
//            }
//        }
        
        CellRevealResult result = cell.reveal(isFirstReveal);

        // clear visuals (same as before)
        button.setGraphic(null);
        button.setText("");
        button.getStyleClass().removeAll(
             "cell-hidden", "cell-revealed",
             "cell-mine", "cell-question",
             "cell-surprise", "cell-number", "cell-empty"
        );
        
        //COMMON revealed state
        button.getStyleClass().add("cell-revealed");

        
        if (result.type == CellType.MINE) {

            button.setText("💣");
            button.setDisable(true);
            button.getStyleClass().add("cell-mine");
            button.getStyleClass().add("cell-activated");

            int heartsBefore = s.sharedHearts;

            if (result.loseHeart) {
                s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                if (s.sharedHearts < heartsBefore) {
                    s.mistakeMade = true;
                }
             // Check if hearts == 0 → try buy → else game over
                if (checkLoseAndHandle()) {
                    return;
                }
            }

            if (result.triggerExplosion) {
                triggerExplosion(tile);
            }

            if (isPlayer1)
                s.minesLeft1 = Math.max(0, s.minesLeft1 - 1);
            else
                s.minesLeft2 = Math.max(0, s.minesLeft2 - 1);

            
            if (!batchMode) {
                ui.buildHeartsBar();
                ui.updateScoreAndMineLabels();
            }
            if (!s.gameOver && s.sharedHearts > 0 &&
                    (s.minesLeft1 == 0 || s.minesLeft2 == 0)) {
                s.gameWon = true;
                onGameOver();
                return;
            }

            if (checkLoseAndHandle()) return;
        }

        /* ===================== QUESTION ===================== */
        else if (result.type == CellType.QUESTION) {

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

            if (isPlayer1)
                s.questionsLeft1 = Math.max(0, s.questionsLeft1 - 1);
            else
                s.questionsLeft2 = Math.max(0, s.questionsLeft2 - 1);
        }

        /* ===================== SURPRISE ===================== */
        else if (result.type == CellType.SURPRISE) {

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

            if (isPlayer1)
                s.surprisesLeft1 = Math.max(0, s.surprisesLeft1 - 1);
            else
                s.surprisesLeft2 = Math.max(0, s.surprisesLeft2 - 1);
        }

        /* ===================== NUMBER ===================== */
        else if (result.type == CellType.NUMBER) {

            button.setText(String.valueOf(cell.getAdjacentMines()));
            button.setDisable(true);
            button.getStyleClass().add("cell-number");
            button.getStyleClass().add("cell-activated");
        }

        /* ===================== EMPTY ===================== */
        else if (result.type == CellType.EMPTY) {

            button.setText("");
            button.setDisable(true);
            button.getStyleClass().add("cell-empty");
            button.getStyleClass().add("cell-activated");
        }

        /* ===================== SCORE ===================== */
        if (result.addScore) {
            s.score += 1;
        }

        if (cell.getType() != CellType.MINE) {
            if (isPlayer1) s.safeCellsRemaining1 = Math.max(0, s.safeCellsRemaining1 - 1);
            else s.safeCellsRemaining2 = Math.max(0, s.safeCellsRemaining2 - 1);

            if (!s.gameOver && s.sharedHearts > 0 &&
                    (s.safeCellsRemaining1 == 0 || s.safeCellsRemaining2 == 0)) {
                s.gameWon = true;
                onGameOver();
            }
        }

        if (!batchMode) {
            ui.updateScoreAndMineLabels();
            checkSellHeartCondition();
        }
    }

    private void cascadeReveal(Board board, int startRow, int startCol, boolean isPlayer1) {
        StackPane[][] buttons = isPlayer1 ? s.p1Buttons : s.p2Buttons;
        int rows = board.getRows();
        int cols = board.getCols();

        boolean[][] visited = new boolean[rows][cols];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startRow, startCol});

        while (!stack.isEmpty()) {
            if (s.gameOver) return;
            int[] rc = stack.pop();
            int r = rc[0];
            int c = rc[1];

            if (r < 0 || c < 0 || r >= rows || c >= cols) continue;
            if (visited[r][c]) continue;
            visited[r][c] = true;

            Cell cell = board.getCell(r, c);

            if (cell.getType() == CellType.MINE) continue;

            StackPane tile = buttons[r][c];
            if (tile == null || tile.getChildren().isEmpty()) continue;
            Button btn = (Button) tile.getChildren().get(0);

            if (btn.getStyleClass().contains("cell-flagged")) {
                if (SysData.isAutoRemoveFlagEnabled()) {
                    autoRemoveFlagIfPresent(board, r, c, btn, isPlayer1,true);
                }
                continue;
            }

            revealSingleCell(board, r, c, btn, tile, isPlayer1, true);

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
        ui.updateScoreAndMineLabels();
    }

    public boolean checkLoseAndHandle() {
        if (s.gameOver) return true;
        if (s.sharedHearts > 0) return false;

     // Try buying a heart
        if (bonusService != null && bonusService.tryBuyHeartBeforeGameOver()) {
            return false;
        }
        
        s.gameWon = false;
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

    public void onGameOver() {
        if (s.gameOver) return;
        s.gameOver = true;

        s.endHeartsRemaining = 0;
        s.endHeartsBonusPoints = 0;

        if (s.gameWon && s.sharedHearts > 0) {
            int perHeart = switch (s.difficulty) {
                case EASY -> 5;
                case MEDIUM -> 8;
                case HARD -> 12;
            };
            s.endScoreBefore = s.score;
            s.endHeartsRemaining = s.sharedHearts;
            s.endHeartsBonusPoints = s.sharedHearts * perHeart;
            s.score += s.endHeartsBonusPoints;
            ui.updateScoreAndMineLabels();
        }

        ui.stopTimer();
        history.saveCurrentGameToHistory(s);
        revealAllBoardsVisualOnly();

        PauseTransition pause = new PauseTransition(Duration.seconds(4.0));
        pause.setOnFinished(e -> showEndGameScreenCallback.run());
        pause.play();

        System.out.println("Game over! Saved to history.");
    }

    public void showHeartsBonusPopupIfNeeded() {
        if (!s.gameWon) return;
        if (s.endHeartsRemaining <= 0 || s.endHeartsBonusPoints <= 0) return;

        DialogUtil.show(Alert.AlertType.INFORMATION,
                "Bonus added to your final score!",
                "Hearts Bonus",
                "Remaining hearts: " + s.endHeartsRemaining + "\n" +
                        "Score before Addition: " + s.endScoreBefore + "\n" +
                        "Added points: +" + s.endHeartsBonusPoints + "\n\n" +
                        "Final score: " + s.score);
    }

    // Visual-only reveal at end
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
        if (!button.getStyleClass().contains("cell-activated")) {
            button.getStyleClass().add("cell-activated");
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
        revealAllCellsOnBoardVisualOnly(s.board1, s.p1Buttons);
        revealAllCellsOnBoardVisualOnly(s.board2, s.p2Buttons);
    }

    // Explosion animation unchanged
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

            javafx.animation.TranslateTransition debrisFly = new javafx.animation.TranslateTransition(Duration.millis(600), debris);
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
    
    
    /************Buying/Selling hearts logic***********/
    private void checkSellHeartCondition() {
        if (bonusService == null) return;

        if (bonusService.trySellHeartToContinue()) {
            return;
        }

        // If player refused, game ends
        if (s.score <= bonusService.getMinScore()) {
            s.gameWon = false;
            onGameOver();
        }
    }

}
