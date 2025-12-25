package control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import model.Board;
import model.Cell;
import model.CellType;
import model.Difficulty;
import model.Question;
import model.SysData;

public class GameBonusServiceController {

    private final GameStateController s;
    private final GameUIServiceController ui;
    private final GamePlayServiceController play;

    private PauseTransition idleHintTimer;
    private static final Duration IDLE_HINT_DELAY = Duration.seconds(45);
    private static final Duration IDLE_HINT_GLOW_DURATION = Duration.seconds(5);

    public GameBonusServiceController(GameStateController s, GameUIServiceController ui, GamePlayServiceController play) {
        this.s = s;
        this.ui = ui;
        this.play = play;
    }

    
     // Resets the idle timer. Any user action (click/flag/unflag/question/surprise and pause), should call this.
    public void resetIdleHintTimer() {
        if (!SysData.isSmartHintsEnabled()) return;

        if (idleHintTimer != null) {
            idleHintTimer.stop();
        }

        idleHintTimer = new PauseTransition(IDLE_HINT_DELAY);
        idleHintTimer.setOnFinished(e -> {
            if (s.gameOver || s.isPaused) return;
            showIdleHint();
            resetIdleHintTimer();
        });
        idleHintTimer.playFromStart();
    }

    private void showIdleHint() {
        if (!SysData.isSmartHintsEnabled()) return;
        if (s.gameOver || s.isPaused) return;

        boolean targetP1 = s.isPlayer1Turn;
        Board board = targetP1 ? s.board1 : s.board2;
        StackPane[][] tiles = targetP1 ? s.p1Buttons : s.p2Buttons;

        if (board == null || tiles == null) return;

        List<Button> candidates = new ArrayList<>();

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                if (cell == null) continue;

                if (cell.getType() == CellType.MINE) continue;

                StackPane tile = tiles[r][c];
                if (tile == null || tile.getChildren().isEmpty()) continue;

                Button btn = (Button) tile.getChildren().get(0);

                if (btn.isDisable()) continue;
                if (!btn.getStyleClass().contains("cell-hidden")) continue;
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

    
     // this is called before normal reveal on left click.
     // if user clicked an already-revealed QUESTION/SURPRISE cell, activate it and consume turn.
     // it returns true if activation happened (meaning caller should switch turn and skip normal reveal).
    public boolean tryHandleSecondClickActivation(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {
        Cell cell = board.getCell(row, col);
        boolean[][] revealedArray = isPlayer1 ? s.revealedCellsP1 : s.revealedCellsP2;

        if (cell.getType() == CellType.SURPRISE &&
                revealedArray != null &&
                revealedArray[row][col]) {

            activateSurprise(board, row, col, button, tile, isPlayer1);
            return true;
        }

        if (cell.getType() == CellType.QUESTION &&
                revealedArray != null &&
                revealedArray[row][col]) {

            activateQuestion(board, row, col, button, tile, isPlayer1);
            return true;
        }

        return false;
    }

    private void activateSurprise(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {
        resetIdleHintTimer();

        int livesBefore = s.sharedHearts;
        int scoreBefore = s.score;

        int activationPoints = getActivationPoints();
        int goodBonus = getSurpriseGoodBonusPoints();
        int badPenalty = getSurpriseBadPenaltyPoints();

        int scoreChange = activationPoints;

        boolean good = Math.random() < 0.5;

        if (good) {
            scoreChange += goodBonus;

            if (s.sharedHearts < GameStateController.TOTAL_HEART_SLOTS) {
                s.sharedHearts += 1;
            } else {
                scoreChange += activationPoints;
            }
        } else {
            scoreChange -= badPenalty;
            s.sharedHearts = Math.max(0, s.sharedHearts - 1);
        }

        s.score += scoreChange;

        ui.buildHeartsBar();
        button.setDisable(true);

        ui.updateScoreAndMineLabels();

        if (play.checkLoseAndHandle()) return;

        int livesAfter = s.sharedHearts;
        int netScoreChange = s.score - scoreBefore;
        showSurprisePopup(good, netScoreChange, livesBefore, livesAfter);
    }

    private int getActivationPoints() {
        return switch (s.difficulty) {
            case EASY -> 5;
            case MEDIUM -> 8;
            case HARD -> 12;
        };
    }

    private int getSurpriseGoodBonusPoints() {
        return switch (s.difficulty) {
            case EASY -> 8;
            case MEDIUM -> 12;
            case HARD -> 16;
        };
    }

    private int getSurpriseBadPenaltyPoints() {
        return switch (s.difficulty) {
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
        msg.append("\nNew score: ").append(s.score).append("\n");
        msg.append("New lives: ").append(s.sharedHearts).append("/").append(GameStateController.TOTAL_HEART_SLOTS);

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
            if (s.sharedHearts < GameStateController.TOTAL_HEART_SLOTS) {
                s.sharedHearts++;
            } else {
                extraScoreFromConversion += pointsPerConvertedHeart;
            }
        }

        return extraScoreFromConversion;
    }

    private void revealRandomMineReward(Board board, boolean isPlayer1) {
        StackPane[][] buttons = isPlayer1 ? s.p1Buttons : s.p2Buttons;
        if (buttons == null) return;

        int rows = board.getRows();
        int cols = board.getCols();

        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (cell == null || cell.getType() != CellType.MINE) continue;

                StackPane tile = buttons[r][c];
                if (tile == null || tile.getChildren().isEmpty()) continue;

                Button btn = (Button) tile.getChildren().get(0);
                if (!btn.isDisable()) {
                    candidates.add(new int[]{r, c});
                }
            }
        }

        while (!candidates.isEmpty()) {
            int idx = (int) (Math.random() * candidates.size());
            int[] rc = candidates.remove(idx);
            int r = rc[0], c = rc[1];

            StackPane tile = buttons[r][c];
            Button btn = (Button) tile.getChildren().get(0);

            if (btn.getStyleClass().contains("cell-flagged")) {
                if (SysData.isAutoRemoveFlagEnabled()) {
                    // remove only visuals, not refund flags
                    // the actual logic is in play service, but here we do the visual change:
                    btn.setGraphic(null);
                    btn.setText("");
                    btn.getStyleClass().remove("cell-flagged");
                }
                continue;
            }

            boolean[][] revealedArray = isPlayer1 ? s.revealedCellsP1 : s.revealedCellsP2;
            if (revealedArray != null && !revealedArray[r][c]) {
                revealedArray[r][c] = true;
                if (isPlayer1) s.revealedCountP1++;
                else s.revealedCountP2++;
            }

            btn.setText("💣");
            btn.getStyleClass().removeAll("cell-hidden");
            if (!btn.getStyleClass().contains("cell-revealed")) btn.getStyleClass().add("cell-revealed");
            if (!btn.getStyleClass().contains("cell-mine")) btn.getStyleClass().add("cell-mine");
            btn.setDisable(true);

            if (isPlayer1) s.minesLeft1 = Math.max(0, s.minesLeft1 - 1);
            else s.minesLeft2 = Math.max(0, s.minesLeft2 - 1);

            ui.updateScoreAndMineLabels();

            if (!s.gameOver && s.sharedHearts > 0 && (s.minesLeft1 == 0 || s.minesLeft2 == 0)) {
                s.gameWon = true;
                play.onGameOver();
            }
            return;
        }
    }

    private void revealArea3x3Reward(Board board, boolean isPlayer1) {
        StackPane[][] buttons = isPlayer1 ? s.p1Buttons : s.p2Buttons;
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

        if (centers.isEmpty()) return;

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

                if (btn.getStyleClass().contains("cell-flagged")) {
                    if (SysData.isAutoRemoveFlagEnabled()) {
                        btn.setGraphic(null);
                        btn.setText("");
                        btn.getStyleClass().remove("cell-flagged");
                    }
                    continue;
                }

                play.revealSingleCell(board, r, c, btn, tile, isPlayer1);
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

        msg.append("\nNew score: ").append(s.score).append("\n");
        msg.append("New lives: ").append(s.sharedHearts).append("/").append(GameStateController.TOTAL_HEART_SLOTS);

        alert.setHeaderText(null);
        alert.setContentText(msg.toString());
        alert.showAndWait();
    }

    private void activateQuestion(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {
        resetIdleHintTimer();

        int activationPoints = getActivationPoints();
        int livesBefore = s.sharedHearts;
        int scoreBefore = s.score;

        s.score -= activationPoints;

        Question q = getRandomQuestionFromPool();
        if (q == null) {
            button.setDisable(true);
            ui.updateScoreAndMineLabels();
            ui.buildHeartsBar();
            return;
        }

        int chosenOption = showQuestionDialog(q);
        if (chosenOption == -1) {
            button.setDisable(true);
            ui.updateScoreAndMineLabels();
            ui.buildHeartsBar();
            return;
        }

        boolean correct = (chosenOption == q.getCorrectOption());
        String qDiff = q.getDifficulty() != null ? q.getDifficulty().toLowerCase() : "easy";
        String extraInfo = "";

        if (s.difficulty == Difficulty.EASY) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    s.score += 3;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    s.mistakeMade = true;
                    if (Math.random() < 0.5) {
                        s.score -= 3;
                        extraInfo = "Wrong answer: you lost 3 points.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    revealRandomMineReward(board, isPlayer1);
                    s.score += 6;
                    extraInfo = "Correct! One mine was revealed automatically for you.";
                } else {
                    s.mistakeMade = true;
                    if (Math.random() < 0.5) {
                        s.score -= 6;
                        extraInfo = "Wrong answer: you lost 6 points.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    revealArea3x3Reward(board, isPlayer1);
                    s.score += 10;
                    extraInfo = "Correct! A 3×3 area of cells was revealed for you.";
                } else {
                    s.score -= 10;
                    extraInfo = "Wrong answer: you lost 10 points.";
                    s.mistakeMade = true;
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    s.score += 15;
                    int converted = addLivesWithCap(2, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    }
                } else {
                    s.score -= 15;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                    s.mistakeMade = true;
                    extraInfo = "Wrong answer: you lost 15 points and 1 life.";
                }
            }

        } else if (s.difficulty == Difficulty.MEDIUM) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    s.score += 8;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    s.score -= 8;
                    extraInfo = "Wrong answer: you lost 8 points.";
                    s.mistakeMade = true;
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    s.score += 10;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    s.mistakeMade = true;
                    if (Math.random() < 0.5) {
                        s.score -= 10;
                        s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                        extraInfo = "Wrong answer: you lost 10 points and 1 life.";
                    } else {
                        extraInfo = "Wrong answer: no additional penalty this time.";
                    }
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    s.score += 15;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    }
                } else {
                    s.score -= 15;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                    s.mistakeMade = true;
                    extraInfo = "Wrong answer: you lost 15 points and 1 life.";
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    s.score += 20;
                    int converted = addLivesWithCap(2, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 20;
                    if (Math.random() < 0.5) {
                        s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                        extraInfo = "Wrong answer: you lost 20 points and 1 life.";
                    } else {
                        s.sharedHearts = Math.max(0, s.sharedHearts - 2);
                        extraInfo = "Wrong answer: you lost 20 points and 2 lives.";
                    }
                }
            }

        } else if (s.difficulty == Difficulty.HARD) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    s.score += 10;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                converted + " points.";
                    } else {
                        extraInfo = "Correct! You gained 1 life.";
                    }
                } else {
                    s.score -= 10;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                    s.mistakeMade = true;
                    extraInfo = "Wrong answer: you lost 10 points and 1 life.";
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    s.score += 15;
                    int livesToAdd = (Math.random() < 0.5) ? 1 : 2;
                    int converted = addLivesWithCap(livesToAdd, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "Correct! You gained " + livesToAdd +
                                " lives, but some were converted to +" + converted +
                                " points because you were at max lives.";
                    } else {
                        extraInfo = "Correct! You gained " + livesToAdd + " lives.";
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 15;
                    int livesLost = (Math.random() < 0.5) ? 1 : 2;
                    s.sharedHearts = Math.max(0, s.sharedHearts - livesLost);
                    extraInfo = "Wrong answer: you lost 15 points and " +
                            livesLost + " life" + (livesLost > 1 ? "s." : ".");
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    s.score += 20;
                    int converted = addLivesWithCap(2, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "Correct! You gained 2 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    } else {
                        extraInfo = "Correct! You gained 2 lives.";
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 20;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 2);
                    extraInfo = "Wrong answer: you lost 20 points and 2 lives.";
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    s.score += 40;
                    int converted = addLivesWithCap(3, activationPoints);
                    s.score += converted;
                    if (converted > 0) {
                        extraInfo = "Correct! You gained 3 lives, but some were converted to +" +
                                converted + " points because you were at max lives.";
                    } else {
                        extraInfo = "Correct! You gained 3 lives.";
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 40;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 3);
                    extraInfo = "Wrong answer: you lost 40 points and 3 lives.";
                }
            }
        }

        ui.buildHeartsBar();
        button.setDisable(true);

        ui.updateScoreAndMineLabels();

        if (play.checkLoseAndHandle()) return;

        int livesAfter = s.sharedHearts;
        int netScoreChange = s.score - scoreBefore;
        showQuestionResultPopup(q, correct, netScoreChange, livesBefore, livesAfter, extraInfo);
    }
}
