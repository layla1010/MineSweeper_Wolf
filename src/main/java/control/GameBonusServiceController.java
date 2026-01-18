package control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import model.Board;
import model.Cell;
import model.CellType;
import model.Difficulty;
import model.Question;
import model.SysData;
import model.Theme;
import util.DialogUtil;
import util.ThemeManager;

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
        int surprisePoints = getSurprisePoints();

        int scoreChange;

        boolean good = Math.random() < 0.5;

        if (good) {
        	  scoreChange = +surprisePoints - activationPoints;
              if (s.sharedHearts < GameStateController.TOTAL_HEART_SLOTS) {
                  s.sharedHearts += 1;
              }
        }else {
        	  scoreChange = -surprisePoints - activationPoints;
        	  s.sharedHearts = Math.max(0, s.sharedHearts - 1);
        }
        
        s.score += scoreChange;

        ui.buildHeartsBar();
        button.setDisable(true);

        ui.updateScoreAndMineLabels();

        if (play.checkLoseAndHandle()) return;

        int livesAfter = s.sharedHearts;
        int netScoreChange = s.score - scoreBefore;
        showSurprisePopup(good, scoreBefore, netScoreChange, livesBefore, livesAfter, activationPoints, surprisePoints);
    }

    private int getActivationPoints() {
        return switch (s.difficulty) {
            case EASY -> 5;
            case MEDIUM -> 8;
            case HARD -> 12;
        };
    }

    private int getSurprisePoints() {
        return switch (s.difficulty) {
            case EASY -> 8;
            case MEDIUM -> 12;
            case HARD -> 16;
        };
    }

    private void showSurprisePopup(boolean good,  int scoreBefore, int netScoreChange, int livesBefore, int livesAfter, int activationPoints, int surprisePoints) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Surprise Result");

        String typeText = good ? "GOOD SURPRISE!" : "BAD SURPRISE!";
        int livesDelta = livesAfter - livesBefore;
        String scoreChangeText = (netScoreChange >= 0 ? "+" : "") + netScoreChange;
        String livesChangeText = (livesDelta > 0 ? "+" : "") + livesDelta;
        String rewardText = (good ? "+" : "-") + surprisePoints + " POINTS FOR SURPRISE";
        String activationText = "-" + activationPoints + " POINTS FOR ACTIVATION";

        StringBuilder msg = new StringBuilder();
        msg.append("Score Before: ").append(scoreBefore).append("\n");
        msg.append("Lives Before: ").append(livesBefore).append("/")
           .append(GameStateController.TOTAL_HEART_SLOTS).append("\n\n");

        msg.append("Score change: ").append(scoreChangeText)
           .append(" (").append(rewardText).append(" ").append(activationText).append(")\n\n");

        msg.append("Lives change: ").append(livesChangeText).append("\n\n");
        msg.append("New score: ").append(s.score).append("\n");
        msg.append("New lives: ").append(s.sharedHearts).append("/")
           .append(GameStateController.TOTAL_HEART_SLOTS);

        alert.setHeaderText(typeText);

        alert.setContentText(msg.toString());
        javafx.scene.control.Label content = new javafx.scene.control.Label(msg.toString());
        content.setWrapText(true);
        content.setMaxWidth(520); // controls wrapping width (tune this)
        alert.getDialogPane().setContent(content);

        // Force dialog to size to content
        alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        alert.getDialogPane().setMinWidth(560);


        alert.getDialogPane().getStylesheets().add(
        	    GameBonusServiceController.class.getResource(
        	        ThemeManager.getTheme() == Theme.WOLF ? "/css/wolf.css" : "/css/theme.css"
        	    ).toExternalForm()
        	);


        alert.showAndWait();
    }

    private Question getRandomQuestionFromPool() {
        //List<Question> all = QuestionsManagerController.loadQuestionsForGame();
    	List<Question> all = SysData.getInstance().getAllQuestions();

        if (all == null || all.isEmpty()) {
            System.out.println("No questions found.");
            return null;
        }

        int idx = (int) (Math.random() * all.size());
        return all.get(idx);
    }

    private int showQuestionDialog(Question q) {
        if (q == null) return -1;

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Trivia Question");
        dialog.setHeaderText("You Got a " + q.getDifficulty() + " Question");

        Label questionLabel = new Label(q.getText());
        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("question-text");

        VBox optionsBox = new VBox(10);
        optionsBox.setAlignment(Pos.CENTER_LEFT);

        Button btn1 = new Button("1. " + q.getOptA());
        Button btn2 = new Button("2. " + q.getOptB());
        Button btn3 = new Button("3. " + q.getOptC());
        Button btn4 = new Button("4. " + q.getOptD());

        btn1.setMaxWidth(Double.MAX_VALUE);
        btn2.setMaxWidth(Double.MAX_VALUE);
        btn3.setMaxWidth(Double.MAX_VALUE);
        btn4.setMaxWidth(Double.MAX_VALUE);

        btn1.setOnAction(e -> { dialog.setResult(1); dialog.close(); });
        btn2.setOnAction(e -> { dialog.setResult(2); dialog.close(); });
        btn3.setOnAction(e -> { dialog.setResult(3); dialog.close(); });
        btn4.setOnAction(e -> { dialog.setResult(4); dialog.close(); });

        optionsBox.getChildren().addAll(btn1, btn2, btn3, btn4);

        VBox root = new VBox(15, questionLabel, optionsBox);
        root.setAlignment(Pos.CENTER_LEFT);

        dialog.getDialogPane().setContent(root);

        dialog.getDialogPane().getStylesheets().add(
        	    GameBonusServiceController.class.getResource(
        	            ThemeManager.getTheme() == Theme.WOLF ? "/css/wolf.css" : "/css/theme.css"
        	        ).toExternalForm()
        	    );


        Optional<Integer> result = dialog.showAndWait();
        return result.orElse(-1);
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

                play.revealSingleCell(board, r, c, btn, tile, isPlayer1, true);
            }
        }
    }

    private void showQuestionResultPopup(Question q,
            int scoreBefore,
            boolean correct,
            int netScoreChange,
            int livesBefore,
            int livesAfter,
            String extraInfo) {

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Question Result");
		
		int activationPoints = getActivationPoints();
		
		String difficultyText = (q != null && q.getDifficulty() != null) ? q.getDifficulty() : "Unknown";
		boolean useAn = difficultyText.equalsIgnoreCase("easy") || difficultyText.equalsIgnoreCase("expert");
		
		String scoreChangeText = (netScoreChange >= 0 ? "+" : "") + netScoreChange;
		int livesDelta = livesAfter - livesBefore;
		String livesChangeText = (livesDelta >= 0 ? "+" : "") + livesDelta;
		
		StringBuilder msg = new StringBuilder();
		
		msg.append(correct ? "Your answer is CORRECT!\n" : "Your answer is WRONG!\n\n");
		
		msg.append("Score Before: ").append(scoreBefore).append("\n");
		msg.append("Lives Before: ").append(livesBefore).append("/")
		.append(GameStateController.TOTAL_HEART_SLOTS).append("\n\n");
		
		// Add activation cost mention 
		msg.append("Score change: ").append(scoreChangeText)
		.append(" ( -").append(activationPoints).append(" POINTS FOR ACTIVATION )\n");
		
		msg.append("Lives change: ").append(livesChangeText).append("\n");
		
		if (extraInfo != null && !extraInfo.isBlank()) {
		msg.append("\n").append(extraInfo).append("\n");
		}
		
		msg.append("\nNew score: ").append(s.score).append("\n");
		msg.append("New lives: ").append(s.sharedHearts).append("/")
		.append(GameStateController.TOTAL_HEART_SLOTS);
		
		alert.setHeaderText("You answered " + (useAn ? "an " : "a ") + difficultyText + " question.");		

		
		// Use wrapped Label so nothing gets clipped
		Label content = new Label(msg.toString());
		content.setWrapText(true);
		content.setMaxWidth(520);
		alert.getDialogPane().setContent(content);
		alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
		alert.getDialogPane().setMinWidth(560);
		
		alert.getDialogPane().getStylesheets().add(
			    GameBonusServiceController.class.getResource(
			        ThemeManager.getTheme() == Theme.WOLF ? "/css/wolf.css" : "/css/theme.css"
			    ).toExternalForm()
			);

		
		alert.showAndWait();
		}


    private void activateQuestion(Board board, int row, int col, Button button, StackPane tile, boolean isPlayer1) {
        resetIdleHintTimer();
        
        

        int activationPoints = getActivationPoints(); // game difficulty based
        int livesBefore = s.sharedHearts;
        int scoreBefore = s.score;

        // activation cost happens for every question activation
        s.score -= activationPoints;
        
        int beforeReward = s.score;

        

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
        String qDiff = (q.getDifficulty() != null) ? q.getDifficulty().toLowerCase() : "easy";

        // Always attach activation cost mention to the popup text
        String activationSuffix = " (-" + activationPoints + " points for activation).";
        String extraInfo = "";

        if (s.difficulty == Difficulty.EASY) {

            if (qDiff.equals("easy")) {
                if (correct) {
                    s.score += 3;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;

                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                    converted + " points" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +3 points" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    if (Math.random() < 0.5) {
                        s.score -= 3;
                        extraInfo = "you lost 3 points" + activationSuffix;
                    } else {
                        extraInfo = "no additional penalty this time" + activationSuffix;
                    }
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    revealRandomMineReward(board, isPlayer1);
                    s.score += 6;
                    extraInfo = "Correct! One mine was revealed automatically and you gained +6 points" + activationSuffix;
                } else {
                    s.mistakeMade = true;
                    if (Math.random() < 0.5) {
                        s.score -= 6;
                        extraInfo = "you lost 6 points" + activationSuffix;
                    } else {
                        extraInfo = "no additional penalty this time" + activationSuffix;
                    }
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    revealArea3x3Reward(board, isPlayer1);
                    int afterReveal = s.score;
                    s.score += 10;
                    int afterReward = s.score;

                    int revealDelta = afterReveal - beforeReward;      
                    int totalDelta = afterReward - scoreBefore;
                    extraInfo =
                            "Correct! Reward: +10" +
                            ", 3×3 reveal side-score: " + (revealDelta >= 0 ? "+" : "") + revealDelta +
                            ".\n Net: " + (totalDelta >= 0 ? "+" : "") + totalDelta + activationSuffix;
                } else {
                    s.mistakeMade = true;
                    s.score -= 10;
                    extraInfo = "you lost 10 points" + activationSuffix;
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    s.score += 15;
                    int converted = addLivesWithCap(2, activationPoints);
                    s.score += converted;

                    if (converted > 0) {
                        extraInfo = "Correct! You gained 2 lives, but some were converted to +" +
                                    converted + " points because you were at max lives" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +15 points and 2 lives" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 15;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                    extraInfo = "you lost 15 points and 1 life" + activationSuffix;
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
                                    converted + " points" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +8 points and 1 life" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 8;
                    extraInfo = "you lost 8 points" + activationSuffix;
                }

            } else if (qDiff.equals("medium")) {
                if (correct) {
                    s.score += 10;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;

                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                    converted + " points" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +10 points and 1 life" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    if (Math.random() < 0.5) {
                        s.score -= 10;
                        s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                        extraInfo = "you lost 10 points and 1 life" + activationSuffix;
                    } else {
                        extraInfo = "no additional penalty this time" + activationSuffix;
                    }
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    s.score += 15;
                    int converted = addLivesWithCap(1, activationPoints);
                    s.score += converted;

                    if (converted > 0) {
                        extraInfo = "You were already at max lives, so the extra life was converted to +" +
                                    converted + " points" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +15 points and 1 life" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 15;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                    extraInfo = "you lost 15 points and 1 life" + activationSuffix;
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    s.score += 20;
                    int converted = addLivesWithCap(2, activationPoints);
                    s.score += converted;

                    if (converted > 0) {
                        extraInfo = "Correct! You gained 2 lives, but some were converted to +" +
                                    converted + " points because you were at max lives" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +20 points and 2 lives" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 20;
                    if (Math.random() < 0.5) {
                        s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                        extraInfo = "you lost 20 points and 1 life" + activationSuffix;
                    } else {
                        s.sharedHearts = Math.max(0, s.sharedHearts - 2);
                        extraInfo = "you lost 20 points and 2 lives" + activationSuffix;
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
                                    converted + " points" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +10 points and 1 life" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 10;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 1);
                    extraInfo = "you lost 10 points and 1 life" + activationSuffix;
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
                                    " points because you were at max lives" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +15 points and " + livesToAdd + " lives" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 15;
                    int livesLost = (Math.random() < 0.5) ? 1 : 2;
                    s.sharedHearts = Math.max(0, s.sharedHearts - livesLost);
                    extraInfo = "you lost 15 points and " + livesLost +
                                " life" + (livesLost > 1 ? "s" : "") + activationSuffix;
                }

            } else if (qDiff.equals("hard")) {
                if (correct) {
                    s.score += 20;
                    int converted = addLivesWithCap(2, activationPoints);
                    s.score += converted;

                    if (converted > 0) {
                        extraInfo = "Correct! You gained 2 lives, but some were converted to +" +
                                    converted + " points because you were at max lives" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +20 points and 2 lives" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 20;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 2);
                    extraInfo = "you lost 20 points and 2 lives" + activationSuffix;
                }

            } else if (qDiff.equals("expert")) {
                if (correct) {
                    s.score += 40;
                    int converted = addLivesWithCap(3, activationPoints);
                    s.score += converted;

                    if (converted > 0) {
                        extraInfo = "Correct! You gained 3 lives, but some were converted to +" +
                                    converted + " points because you were at max lives" + activationSuffix;
                    } else {
                        extraInfo = "Correct! You gained +40 points and 3 lives" + activationSuffix;
                    }
                } else {
                    s.mistakeMade = true;
                    s.score -= 40;
                    s.sharedHearts = Math.max(0, s.sharedHearts - 3);
                    extraInfo = "you lost 40 points and 3 lives" + activationSuffix;
                }
            }
        }

        ui.buildHeartsBar();
        button.setDisable(true);
        ui.updateScoreAndMineLabels();

        if (play.checkLoseAndHandle()) return;

        int livesAfter = s.sharedHearts;
        int netScoreChange = s.score - scoreBefore;

        showQuestionResultPopup(q, scoreBefore, correct, netScoreChange, livesBefore, livesAfter, extraInfo);
    }
    
    //*********************Buying/Selling hearts logic************************//
    
    private int getHeartPrice() {
        return switch (s.difficulty) {
            case EASY -> 5;
            case MEDIUM -> 8;
            case HARD -> 12;
        };
    }

    public int getMinScore() {
        return switch (s.difficulty) {
            case EASY -> -10;
            case MEDIUM -> -16;
            case HARD -> -24;
        };
    }
    
    public boolean tryBuyHeartBeforeGameOver() {
        int price = getHeartPrice();
        int minScore = getMinScore();

        if (s.score < price || s.score - price <= minScore) {
        	DialogUtil.show(
        	        Alert.AlertType.INFORMATION,
        	        "Cannot Buy Heart",
        	        "Not Enough Score",
        	        "You don't have enough score to safely buy a heart."
        	    );
        return false;
        }

        boolean buy = DialogUtil.confirm(
                "Out of Hearts!",
                "Buy Heart",
                "You can buy 1 heart for " + price + " points.\n\nDo you want to continue?"
        );

        if (buy) {
            s.score -= price;
            s.sharedHearts = 1;
            ui.buildHeartsBar();
            ui.updateScoreAndMineLabels();
            return true;
        }

        return false;
    }
    
    public boolean trySellHeartToContinue() {
        if (s.sharedHearts <= 1) return false;

        int minScore = getMinScore();
        int gain = getHeartPrice();

        if (s.score > minScore) return false;

        boolean sell = DialogUtil.confirm(
                "Low Score!",
                "Sell Heart",
                "You can sell 1 heart for +" + gain + " points.\n\nDo you want to continue?"
        );

        if (sell) {
            s.sharedHearts--;
            s.score += gain;
            ui.buildHeartsBar();
            ui.updateScoreAndMineLabels();
            return true;
        }

        return false;
    }



}
