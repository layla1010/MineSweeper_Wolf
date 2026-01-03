package control;

import javafx.geometry.Pos;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.SoundManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HowToPlayController {

    private static final int SIZE = 9;

    @FXML private Parent root;

    @FXML private HBox heartsBox;
    @FXML private Label scoreLabel;

    @FXML private Rectangle p1NbrHL;
    @FXML private Rectangle p2NbrHL;

    @FXML private GridPane p1Grid;
    @FXML private GridPane p2Grid;

    @FXML private StackPane p1BoardLayer;
    @FXML private StackPane p2BoardLayer;

    @FXML private Rectangle p1RowHL;
    @FXML private Rectangle p1ColHL;
    @FXML private Rectangle p1CellHL;

    @FXML private Rectangle p2RowHL;
    @FXML private Rectangle p2ColHL;
    @FXML private Rectangle p2CellHL;

    @FXML private Label stepTitle;
    @FXML private Label stepBody;
    @FXML private Label stepCounter;

    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    @FXML private Button backBtn;
    @FXML private Button restartBtn;

    @FXML private Label turnLabel;

    private StackPane[][] p1Tiles;
    private StackPane[][] p2Tiles;

    @FXML private Button playBtn;
    @FXML private Button stopBtn;

    @FXML private HBox p1StatsBox;
    @FXML private HBox p2StatsBox;

    private Timeline autoPlay;
    private boolean isAutoPlaying = false;

    private static final int START_MINES = 10;
    private static final int START_FLAGS = 40;
    private static final int START_QUESTIONS = 6;
    private static final int START_SURPRISES = 2;
    private static final int START_HEARTS = 10;

    private int p1Mines, p1Flags, p1Questions, p1Surprises;
    private int p2Mines, p2Flags, p2Questions, p2Surprises;

    private int sharedHearts;
    private int score;

    private boolean isP1Turn = true;

    private final HowToPlayBoardBuilder builder = new HowToPlayBoardBuilder();

    private final List<TourStep> steps = new ArrayList<>();
    private int stepIndex = 0;

    // pulse animation for highlight rectangles
    private Timeline pulse;

    // ----------------- "REAL GAME" DEMO STATE (for How To Play) -----------------

    private enum DemoCellType { HIDDEN, EMPTY, NUMBER, FLAG, MINE, QUESTION, SURPRISE }

    private DemoCellType[][] p1Type = new DemoCellType[SIZE][SIZE];
    private DemoCellType[][] p2Type = new DemoCellType[SIZE][SIZE];

    private boolean[][] p1Revealed = new boolean[SIZE][SIZE];
    private boolean[][] p2Revealed = new boolean[SIZE][SIZE];

    private boolean[][] p1Used = new boolean[SIZE][SIZE];   // activated (question/surprise)
    private boolean[][] p2Used = new boolean[SIZE][SIZE];

    // Easy-mode constants (match the “real game style” rules we used before)
    private static final int ACTIVATION_COST_EASY = 5;
    private static final int SURPRISE_POINTS_EASY = 8;

    private static final int TOTAL_HEART_SLOTS = 10;

    @FXML
    private void initialize() {
        p1Tiles = builder.build(p1Grid, SIZE, SIZE, true);
        p2Tiles = builder.build(p2Grid, SIZE, SIZE, false);

        stopBtn.setDisable(true);

        resetDemoState();
        resetScenario();         // only once at start
        hideAllHighlights();
        buildSteps();

        Platform.runLater(() -> {
            startPulse();
            runStep(0);
        });
    }

    // ----------------- Navigation Buttons -----------------

    @FXML
    private void onBack() {
        SoundManager.playClick();
        stopAutoplayIfRunning();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/settings_view.fxml"));
            Parent newRoot = loader.load();

            Stage stage = (Stage) this.backBtn.getScene().getWindow();
            stage.setScene(new Scene(newRoot));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRestart() {
        stopAutoplayIfRunning();
        resetDemoState();
        resetScenario();     // restart = new clean demo board
        runStep(0);
    }

    @FXML
    private void onPlay() {
        if (isAutoPlaying) return;

        isAutoPlaying = true;
        playBtn.setDisable(true);
        stopBtn.setDisable(false);

        autoPlay = new Timeline(new KeyFrame(Duration.seconds(2.5), e -> {
            if (stepIndex < steps.size() - 1) {
                runStep(stepIndex + 1);
            } else {
                onStop();
            }
        }));
        autoPlay.setCycleCount(Timeline.INDEFINITE);
        autoPlay.play();
    }

    private void stopAutoplayIfRunning() {
        if (isAutoPlaying) onStop();
    }

    @FXML
    private void onStop() {
        isAutoPlaying = false;
        if (autoPlay != null) autoPlay.stop();
        autoPlay = null;

        playBtn.setDisable(false);
        stopBtn.setDisable(true);
    }

    @FXML
    private void onPrev() {
        stopAutoplayIfRunning();
        if (stepIndex > 0) runStep(stepIndex - 1);
    }

    @FXML
    private void onNext() {
        stopAutoplayIfRunning();
        if (stepIndex < steps.size() - 1) runStep(stepIndex + 1);
    }

    // ----------------- Steps -----------------

    private void buildSteps() {
        steps.clear();

        // STEP 1
        steps.add(new TourStep(
                "Start (Easy Mode)",
                "Both boards start fully hidden. Each player can act ONLY on their own board. " +
                        "Turns alternate ONLY when a turn-ending action happens (Reveal / Activate Question / Activate Surprise / Mine). " +
                        "Placing flags does NOT end the turn.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 40, 6, 2, 10, true, 0);
                    highlightCell(true, 4, 4);
                }
        ));

        // STEP 2: P1 reveal empty -> cascade (ONE action ends turn)
        steps.add(new TourStep(
                "Player 1: Reveal Empty (Cascade = One Action)",
                "Player 1 reveals one empty safe cell. The board opens adjacent empty cells automatically (cascade). " +
                        "Even though multiple cells open, it still counts as ONE action and ends the turn.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 40, 6, 2, 10, true, 0);

                    setEmptyRevealed(p1Tiles[4][4]); markRevealed(true, 4, 4, DemoCellType.EMPTY);
                    setEmptyRevealed(p1Tiles[4][5]); markRevealed(true, 4, 5, DemoCellType.EMPTY);
                    setEmptyRevealed(p1Tiles[5][4]); markRevealed(true, 5, 4, DemoCellType.EMPTY);
                    setEmptyRevealed(p1Tiles[5][5]); markRevealed(true, 5, 5, DemoCellType.EMPTY);

                    setRevealedNumber(p1Tiles[3][4], 1); markRevealed(true, 3, 4, DemoCellType.NUMBER);
                    setRevealedNumber(p1Tiles[3][5], 2); markRevealed(true, 3, 5, DemoCellType.NUMBER);
                    setRevealedNumber(p1Tiles[6][4], 1); markRevealed(true, 6, 4, DemoCellType.NUMBER);
                    setRevealedNumber(p1Tiles[6][5], 1); markRevealed(true, 6, 5, DemoCellType.NUMBER);

                    highlightArea(true, 3, 4, 6, 5);
                }
        ));

        // STEP 3: switch turn to P2 (because reveal ended P1 turn)
        steps.add(new TourStep(
                "Turn Switch",
                "Because Player 1 performed a turn-ending action (Reveal), the turn switches to Player 2.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 40, 6, 2, 10, false, 0);
                    highlightCell(false, 4, 4);
                }
        ));

        // STEP 4: P2 places a flag (does NOT end turn)
        steps.add(new TourStep(
                "Player 2: Flag (Does NOT End Turn)",
                "Player 2 places a flag. Flags do NOT end the turn, so Player 2 can keep placing more flags or choose another action.",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 39, 6, 2, 10, false, 0);

                    setFlag(p2Tiles[4][4]); markFlag(false, 4, 4);
                    highlightCell(false, 4, 4);
                }
        ));

        // STEP 5: P2 places ANOTHER flag (still same turn)
        steps.add(new TourStep(
                "Player 2: Another Flag (Still Same Turn)",
                "Player 2 places another flag. Still the same turn (flags do NOT switch turns).",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 0);

                    setFlag(p2Tiles[4][4]); markFlag(false, 4, 4);
                    setFlag(p2Tiles[4][5]); markFlag(false, 4, 5);
                    highlightArea(false, 4, 4, 4, 5);
                }
        ));

        // STEP 6: P2 reveals a numbered cell (THIS ends turn)
        steps.add(new TourStep(
                "Player 2: Reveal Number (Ends Turn)",
                "Now Player 2 reveals a cell (turn-ending action). The number tells how many mines exist in the 8 neighboring cells. " +
                        "After a reveal, the turn ends and switches.",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 0);

                    setFlag(p2Tiles[4][4]); markFlag(false, 4, 4);
                    setFlag(p2Tiles[4][5]); markFlag(false, 4, 5);

                    setRevealedNumber(p2Tiles[5][5], 2); markRevealed(false, 5, 5, DemoCellType.NUMBER);

                    highlightCell(false, 5, 5);
                    highlightNeighborhood(false, 5, 5);
                }
        ));

        // STEP 7: turn switches to P1 (because reveal ended P2 turn)
        steps.add(new TourStep(
                "Turn Switch",
                "After Player 2 revealed a cell, the turn switches to Player 1.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, 0);
                    highlightCell(true, 2, 6);
                }
        ));

        // STEP 8: P1 REVEALS a Question cell (ONLY reveal, no activation yet)
        steps.add(new TourStep(
                "Player 1: Reveal Question Cell (Reveal ≠ Activate)",
                "Player 1 reveals a Question cell. This reveal is ONE action and ENDS the turn. " +
                        "Important: revealing the Question cell does NOT activate it automatically.",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, 0);

                    setQuestion(p1Tiles[2][6]);
                    markRevealed(true, 2, 6, DemoCellType.QUESTION);

                    highlightCell(true, 2, 6);
                }
        ));

        // STEP 9: switch to P2 (because reveal ended P1 turn)
        steps.add(new TourStep(
                "Turn Switch",
                "Because Player 1 performed a turn-ending action (Reveal), the turn switches to Player 2.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 0);
                    highlightCell(false, 2, 2);
                }
        ));

        // STEP 10: P2 REVEALS a Surprise cell (ONLY reveal, no activation yet)
        steps.add(new TourStep(
                "Player 2: Reveal Surprise Cell (Reveal ≠ Activate)",
                "Player 2 reveals a Surprise cell. This reveal is ONE action and ENDS the turn. " +
                        "Important: revealing the Surprise cell does NOT activate it automatically.",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 0);

                    setSurprise(p2Tiles[2][2]);
                    markRevealed(false, 2, 2, DemoCellType.SURPRISE);

                    highlightCell(false, 2, 2);
                }
        ));

        // STEP 11: Back to P1 - ACTIVATE Question cell (separate action) - REAL RULES (Easy-mode style)
        steps.add(new TourStep(
                "Player 1: Activate Question (Separate Action)",
                "Now Player 1 activates the previously revealed Question cell. Activation is a separate action (different turn). " +
                        "A multiple-choice question appears. Tutorial: correct answer is highlighted in green, wrong answers in red, and the result screen shows what would happen for each option.",
                () -> {
                    hideAllHighlights();

                    // keep visible
                    setQuestion(p1Tiles[2][6]);
                    setSurprise(p2Tiles[2][2]);
                    markRevealed(true, 2, 6, DemoCellType.QUESTION);
                    markRevealed(false, 2, 2, DemoCellType.SURPRISE);

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, score);

                    highlightCell(true, 2, 6);

                    Platform.runLater(() -> activateQuestionHowTo(true, 2, 6));
                }
        ));

        // STEP 12: Back to P2 - ACTIVATE Surprise (separate action) - REAL RULES (Easy-mode style)
        steps.add(new TourStep(
                "Player 2: Activate Surprise (Separate Action)",
                "Now Player 2 activates the previously revealed Surprise cell. Activation is a separate action (different turn). " +
                        "A GOOD/BAD effect happens, score/lives update, and the cell becomes used (cannot be activated again).",
                () -> {
                    hideAllHighlights();

                    // keep visible
                    setQuestion(p1Tiles[2][6]);
                    setSurprise(p2Tiles[2][2]);
                    markRevealed(true, 2, 6, DemoCellType.QUESTION);
                    markRevealed(false, 2, 2, DemoCellType.SURPRISE);

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, score);

                    highlightCell(false, 2, 2);

                    Platform.runLater(() -> activateSurpriseHowTo(false, 2, 2));
                }
        ));
    }

    // ----------------- REAL-GAME-LIKE ACTIVATIONS (HowToPlay) -----------------

    private void activateQuestionHowTo(boolean isP1, int row, int col) {
        // must be revealed, must be Question, must NOT be used
        if (!isQuestionCell(isP1, row, col)) return;
        if (!isRevealed(isP1, row, col)) return;
        if (isUsed(isP1, row, col)) return;

        int scoreBefore = score;
        int livesBefore = sharedHearts;

        // activation cost
        score -= ACTIVATION_COST_EASY;

        // pull a real question if possible (reflection-safe), otherwise show fallback demo question
        model.Question q = tryLoadRandomQuestion();
        if (q == null) {
            showFallbackQuestionDemo(scoreBefore, livesBefore, isP1, row, col);
            return;
        }

        int picked = showQuestionPickDialogTutorial(q, true); // tutorial mode -> colors
        if (picked < 0) {
            // closed -> still consume activation and mark used (like using it)
            markUsed(isP1, row, col);
            buildHeartsBar();
            updateInfoBar();
            return;
        }

        boolean correct = (picked == q.getCorrectOption());
        String qDiff = (q.getDifficulty() == null) ? "easy" : q.getDifficulty().toLowerCase();

        int scoreChange = 0;
        int livesChange = 0;
        String extraLine;

        // EASY MODE scoring style
        switch (qDiff) {
            case "easy" -> {
                scoreChange = correct ? +3 : -3;
                extraLine = correct ? "Correct! You gained +3 points (-5 activation already applied)."
                        : "Wrong! You lost 3 points (-5 activation already applied).";
            }
            case "medium" -> {
                scoreChange = correct ? +6 : -6;
                extraLine = correct ? "Correct! You gained +6 points (-5 activation already applied)."
                        : "Wrong! You lost 6 points (-5 activation already applied).";
            }
            case "hard" -> {
                scoreChange = correct ? +10 : -10;
                extraLine = correct ? "Correct! You gained +10 points (-5 activation already applied)."
                        : "Wrong! You lost 10 points (-5 activation already applied).";
            }
            default -> { // expert (or unknown -> treat as expert for demo)
                if (correct) {
                    scoreChange = +15;
                    livesChange = +2;
                    extraLine = "Correct! +15 points and +2 lives (-5 activation already applied).";
                } else {
                    scoreChange = -15;
                    livesChange = -1;
                    extraLine = "Wrong! -15 points and -1 life (-5 activation already applied).";
                }
            }
        }

        // apply
        score += scoreChange;
        sharedHearts = clamp(sharedHearts + livesChange, 0, TOTAL_HEART_SLOTS);

        // mark used so it won't activate again
        markUsed(isP1, row, col);

        // UI updates
        buildHeartsBar();
        updateInfoBar();

        // NEW: show a richer results screen (shows what happens for each option)
        showQuestionResultDialogWithAllOptions(
                q,
                picked,
                scoreBefore,
                livesBefore,
                score,
                sharedHearts,
                ACTIVATION_COST_EASY
        );

        // (If you still want the old result dialog too, uncomment)
        // int totalScoreChangeFromBefore = score - scoreBefore;
        // int totalLivesChangeFromBefore = sharedHearts - livesBefore;
        // String diffText = (q.getDifficulty() == null) ? "Question" : q.getDifficulty();
        // showQuestionResultDialog(diffText, correct, scoreBefore, livesBefore, totalScoreChangeFromBefore, totalLivesChangeFromBefore, extraLine);
    }

    private void showFallbackQuestionDemo(int scoreBefore, int livesBefore, boolean isP1, int row, int col) {
        // In fallback we still show the tutorial colored dialog + full outcomes screen
        model.Question q = new model.Question(
                0, "easy",
                "Demo Question (Fallback): Which option is correct?",
                "Wrong option", "Correct option", "Wrong option", "Wrong option",
                1 // correct = B (0=A,1=B,2=C,3=D)
        );

        int picked = showQuestionPickDialogTutorial(q, true);
        if (picked < 0) {
            markUsed(isP1, row, col);
            buildHeartsBar();
            updateInfoBar();
            return;
        }

        boolean correct = (picked == q.getCorrectOption());
        int scoreChange = correct ? +3 : -3;   // easy fallback
        score += scoreChange;

        markUsed(isP1, row, col);
        buildHeartsBar();
        updateInfoBar();

        showQuestionResultDialogWithAllOptions(
                q,
                picked,
                scoreBefore,
                livesBefore,
                score,
                sharedHearts,
                ACTIVATION_COST_EASY
        );
    }

    private void activateSurpriseHowTo(boolean isP1, int row, int col) {
        // must be revealed, must be Surprise, must NOT be used
        if (!isSurpriseCell(isP1, row, col)) return;
        if (!isRevealed(isP1, row, col)) return;
        if (isUsed(isP1, row, col)) return;

        int scoreBefore = score;
        int livesBefore = sharedHearts;

        // activation cost
        score -= ACTIVATION_COST_EASY;

        boolean good = Math.random() < 0.5;

        int scoreDeltaFromEffect = good ? +SURPRISE_POINTS_EASY : -SURPRISE_POINTS_EASY;
        int livesDeltaFromEffect = good ? +1 : -1;

        score += scoreDeltaFromEffect;
        sharedHearts = clamp(sharedHearts + livesDeltaFromEffect, 0, TOTAL_HEART_SLOTS);

        markUsed(isP1, row, col);
        buildHeartsBar();
        updateInfoBar();

        int netScoreChange = score - scoreBefore;
        int netLivesChange = sharedHearts - livesBefore;

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Surprise Result");
        a.setHeaderText(good ? "GOOD SURPRISE!" : "BAD SURPRISE!");
        a.setContentText(
                "Score Before: " + scoreBefore + "\n" +
                        "Lives Before: " + livesBefore + "/10\n\n" +
                        "Score Change: " + (netScoreChange >= 0 ? "+" : "") + netScoreChange +
                        "  ( " + (good ? "+" : "-") + SURPRISE_POINTS_EASY + " surprise"
                        + "  -" + ACTIVATION_COST_EASY + " activation )\n" +
                        "Lives Change: " + (netLivesChange >= 0 ? "+" : "") + netLivesChange + "\n\n" +
                        "New Score: " + score + "\n" +
                        "New Lives: " + sharedHearts + "/10"
        );
        a.showAndWait();
    }

    // Try to load a real question pool without hard dependencies (won't break compile)
    @SuppressWarnings("unchecked")
    private model.Question tryLoadRandomQuestion() {
        try {
            // 1) control.QuestionsManagerController.loadQuestionsForGame()
            Class<?> cls = Class.forName("control.QuestionsManagerController");
            Method m = cls.getDeclaredMethod("loadQuestionsForGame");
            Object res = m.invoke(null);
            if (res instanceof List<?> list) {
                List<model.Question> q = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof model.Question qq) q.add(qq);
                }
                if (!q.isEmpty()) return q.get((int) (Math.random() * q.size()));
            }
        } catch (Exception ignored) { }

        try {
            // 2) model.SysData.getInstance().getQuestions()
            Class<?> sys = Class.forName("model.SysData");
            Method getInstance = sys.getDeclaredMethod("getInstance");
            Object inst = getInstance.invoke(null);
            Method getQuestions = sys.getDeclaredMethod("getQuestions");
            Object res = getQuestions.invoke(inst);
            if (res instanceof List<?> list) {
                List<model.Question> q = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof model.Question qq) q.add(qq);
                }
                if (!q.isEmpty()) return q.get((int) (Math.random() * q.size()));
            }
        } catch (Exception ignored) { }

        return null;
    }

    private int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    // ----------------- NEW: Tutorial Question Dialog (colors) -----------------

    private int showQuestionPickDialogTutorial(model.Question q, boolean tutorialMode) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Question");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(12);
        root.setPadding(new Insets(18));
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #5b5bb6, #8a57b8, #c26ad6);
        """);

        Label title = new Label(
                "You got a " + (q.getDifficulty() == null ? "Question" : q.getDifficulty() + " Question") + "!"
        );
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-family: 'Copperplate Gothic Bold';");

        Label questionText = new Label(q.getText());
        questionText.setWrapText(true);
        questionText.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-family: 'Copperplate Gothic Light';");

        Label hint = new Label("Tutorial mode: correct answer is highlighted.");
        hint.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 13px;");
        hint.setVisible(tutorialMode);
        hint.setManaged(tutorialMode);

        int correctIdx = q.getCorrectOption(); // assume 0..3

        Button btnA = createAnswerBtn("A", q.getOptA());
        Button btnB = createAnswerBtn("B", q.getOptB());
        Button btnC = createAnswerBtn("C", q.getOptC());
        Button btnD = createAnswerBtn("D", q.getOptD());

        if (tutorialMode) {
            styleAnswerForTutorial(btnA, 0 == correctIdx);
            styleAnswerForTutorial(btnB, 1 == correctIdx);
            styleAnswerForTutorial(btnC, 2 == correctIdx);
            styleAnswerForTutorial(btnD, 3 == correctIdx);
        }

        final int[] chosen = { -1 };

        btnA.setOnAction(e -> { chosen[0] = 0; stage.close(); });
        btnB.setOnAction(e -> { chosen[0] = 1; stage.close(); });
        btnC.setOnAction(e -> { chosen[0] = 2; stage.close(); });
        btnD.setOnAction(e -> { chosen[0] = 3; stage.close(); });

        javafx.scene.layout.VBox answersBox = new javafx.scene.layout.VBox(10, btnA, btnB, btnC, btnD);

        root.getChildren().addAll(title, questionText, hint, answersBox);

        stage.setScene(new Scene(root, 620, 460));
        stage.centerOnScreen();
        stage.showAndWait();

        return chosen[0];
    }

    private void styleAnswerForTutorial(Button b, boolean correct) {
        String border = correct
                ? "rgba(70, 255, 140, 0.85)"   // green
                : "rgba(255, 90, 90, 0.80)";   // red
        String bg = correct
                ? "rgba(70, 255, 140, 0.12)"
                : "rgba(255, 90, 90, 0.10)";

        b.setStyle(b.getStyle() + """
            ; -fx-border-color: %s;
              -fx-border-width: 2;
              -fx-background-color: %s;
        """.formatted(border, bg));
    }

    // ----------------- NEW: Result Dialog with ALL options outcomes -----------------

    private static final class Outcome {
        final int scoreDelta;
        final int livesDelta;
        final String text;

        Outcome(int scoreDelta, int livesDelta, String text) {
            this.scoreDelta = scoreDelta;
            this.livesDelta = livesDelta;
            this.text = text;
        }
    }

    private Outcome outcomeForDifficulty(String diff, boolean isCorrect) {
        diff = (diff == null) ? "easy" : diff.toLowerCase();
        switch (diff) {
            case "easy":
                return isCorrect ? new Outcome(+3, 0, "+3 score") : new Outcome(-3, 0, "-3 score");
            case "medium":
                return isCorrect ? new Outcome(+6, 0, "+6 score") : new Outcome(-6, 0, "-6 score");
            case "hard":
                return isCorrect ? new Outcome(+10, 0, "+10 score") : new Outcome(-10, 0, "-10 score");
            default:
                return isCorrect ? new Outcome(+15, +2, "+15 score, +2 lives") : new Outcome(-15, -1, "-15 score, -1 life");
        }
    }

    private void showQuestionResultDialogWithAllOptions(
            model.Question q,
            int chosenIdx,
            int scoreBefore,
            int livesBefore,
            int scoreAfter,
            int livesAfter,
            int activationCost
    ) {
        int correctIdx = q.getCorrectOption();
        String diff = (q.getDifficulty() == null) ? "easy" : q.getDifficulty();

        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Question Result");

        javafx.scene.layout.AnchorPane root = new javafx.scene.layout.AnchorPane();
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #5b5bb6, #8a57b8, #c26ad6);
        """);

        boolean isCorrectChosen = (chosenIdx == correctIdx);

        Label header = new Label(
                "YOU ANSWERED A " + (diff == null ? "QUESTION" : diff.toUpperCase() + " QUESTION") + ".\n\n" +
                        (isCorrectChosen ? "YOUR ANSWER IS CORRECT!" : "YOUR ANSWER IS WRONG!")
        );
        header.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 18px;
            -fx-font-family: 'Copperplate Gothic Light';
        """);

        Label beforeAfter = new Label(
                "Score: " + scoreBefore + " → " + scoreAfter + "\n" +
                        "Lives: " + livesBefore + "/10 → " + livesAfter + "/10\n" +
                        "Activation cost: -" + activationCost + " score"
        );
        beforeAfter.setStyle("""
            -fx-text-fill: rgba(255,255,255,0.90);
            -fx-font-size: 15px;
        """);

        javafx.scene.layout.VBox outcomes = new javafx.scene.layout.VBox(8,
                makeOutcomeLine("A", 0, correctIdx, chosenIdx, diff, activationCost),
                makeOutcomeLine("B", 1, correctIdx, chosenIdx, diff, activationCost),
                makeOutcomeLine("C", 2, correctIdx, chosenIdx, diff, activationCost),
                makeOutcomeLine("D", 3, correctIdx, chosenIdx, diff, activationCost)
        );

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12, header, beforeAfter, outcomes);
        box.setPadding(new Insets(26));

        Button ok = new Button("OK");
        ok.setStyle("""
            -fx-background-color: rgba(255,255,255,0.20);
            -fx-background-radius: 18;
            -fx-border-radius: 18;
            -fx-border-color: rgba(255,255,255,0.55);
            -fx-border-width: 1;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-padding: 8 26 8 26;
            -fx-cursor: hand;
        """);
        ok.setOnAction(e -> stage.close());

        javafx.scene.layout.AnchorPane.setTopAnchor(box, 0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(box, 0.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(box, 0.0);

        javafx.scene.layout.AnchorPane.setBottomAnchor(ok, 22.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(ok, 26.0);

        root.getChildren().addAll(box, ok);

        stage.setScene(new Scene(root, 880, 560));
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private javafx.scene.Node makeOutcomeLine(String letter, int idx, int correctIdx, int chosenIdx, String diff, int activationCost) {
        boolean wouldBeCorrect = (idx == correctIdx);
        Outcome o = outcomeForDifficulty(diff, wouldBeCorrect);

        int netScore = o.scoreDelta - activationCost;
        int netLives = o.livesDelta;

        String text = letter + ") " +
                (wouldBeCorrect ? "CORRECT" : "WRONG") +
                "  →  Score " + (netScore >= 0 ? "+" : "") + netScore +
                " , Lives " + (netLives >= 0 ? "+" : "") + netLives +
                "   (" + o.text + " , -" + activationCost + " activation)";

        Label lbl = new Label(text);
        lbl.setWrapText(true);

        String base = """
            -fx-padding: 10 12 10 12;
            -fx-background-color: rgba(255,255,255,0.12);
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-border-width: 1;
            -fx-text-fill: white;
            -fx-font-size: 14px;
        """;

        String border = (idx == correctIdx)
                ? "rgba(70, 255, 140, 0.85)"
                : "rgba(255, 90, 90, 0.70)";

        String chosenExtra = (idx == chosenIdx)
                ? " -fx-border-width: 2.6; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.18);"
                : "";

        lbl.setStyle(base + " -fx-border-color: " + border + ";" + chosenExtra);

        return lbl;
    }

    // ----------------- Your existing dialogs (kept as-is, but still used by createAnswerBtn) -----------------

    private int showQuestionPickDialog(model.Question q) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Question");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(12);
        root.setPadding(new Insets(18));
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #5b5bb6, #8a57b8, #c26ad6);
        """);

        Label title = new Label(
                "You got a " + (q.getDifficulty() == null ? "Question" : q.getDifficulty() + " Question") + "!"
        );
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-family: 'Copperplate Gothic Bold';");

        Label questionText = new Label(q.getText());
        questionText.setWrapText(true);
        questionText.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-family: 'Copperplate Gothic Light';");

        Button btnA = createAnswerBtn("A", q.getOptA());
        Button btnB = createAnswerBtn("B", q.getOptB());
        Button btnC = createAnswerBtn("C", q.getOptC());
        Button btnD = createAnswerBtn("D", q.getOptD());

        final int[] chosen = { -1 };

        btnA.setOnAction(e -> { chosen[0] = 0; stage.close(); });
        btnB.setOnAction(e -> { chosen[0] = 1; stage.close(); });
        btnC.setOnAction(e -> { chosen[0] = 2; stage.close(); });
        btnD.setOnAction(e -> { chosen[0] = 3; stage.close(); });

        javafx.scene.layout.VBox answersBox = new javafx.scene.layout.VBox(10, btnA, btnB, btnC, btnD);

        root.getChildren().addAll(title, questionText, answersBox);

        stage.setScene(new Scene(root, 620, 420));
        stage.centerOnScreen();
        stage.showAndWait();

        return chosen[0];
    }

    private Button createAnswerBtn(String letter, String text) {
        if (text == null) text = "";
        Button b = new Button(letter + ") " + text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setWrapText(true);

        b.setStyle("""
            -fx-background-color: rgba(255,255,255,0.18);
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-border-color: rgba(255,255,255,0.45);
            -fx-border-width: 1;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-padding: 10 14 10 14;
            -fx-cursor: hand;
        """);

        javafx.scene.control.Tooltip tp = new javafx.scene.control.Tooltip(text);
        tp.setWrapText(true);
        tp.setMaxWidth(420);
        b.setTooltip(tp);

        return b;
    }

    private void showQuestionResultDialog(
            String difficultyText,
            boolean isCorrect,
            int scoreBefore,
            int livesBefore,
            int scoreChange,
            int livesChange,
            String extraLine
    ) {
        int newScore = scoreBefore + scoreChange;
        int newLives = Math.max(0, livesBefore + livesChange);

        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Question Result");

        javafx.scene.layout.AnchorPane root = new javafx.scene.layout.AnchorPane();
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #5b5bb6, #8a57b8, #c26ad6);
        """);

        String headerLine = "YOU ANSWERED A " + difficultyText.toUpperCase() + " QUESTION.";
        String correctnessLine = isCorrect ? "YOUR ANSWER IS CORRECT!" : "YOUR ANSWER IS WRONG!";

        String body = ""
                + headerLine + "\n\n"
                + correctnessLine + "\n\n"
                + "Score Before: " + scoreBefore + "\n"
                + "Lives Before: " + livesBefore + "/10\n\n"
                + "Score Change: " + (scoreChange >= 0 ? "+" : "") + scoreChange + "\n"
                + "Lives Change: " + (livesChange >= 0 ? "+" : "") + livesChange + "\n\n"
                + (extraLine == null ? "" : (extraLine + "\n\n"))
                + "New Score: " + newScore + "\n"
                + "New Lives: " + newLives + "/10\n";

        Label text = new Label(body);
        text.setWrapText(true);
        text.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 18px;
            -fx-font-family: 'Copperplate Gothic Light';
        """);

        javafx.scene.layout.AnchorPane.setTopAnchor(text, 35.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(text, 30.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(text, 30.0);

        Button ok = new Button("OK");
        ok.setStyle("""
            -fx-background-color: rgba(255,255,255,0.20);
            -fx-background-radius: 18;
            -fx-border-radius: 18;
            -fx-border-color: rgba(255,255,255,0.55);
            -fx-border-width: 1;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-padding: 8 26 8 26;
            -fx-cursor: hand;
        """);
        ok.setOnAction(e -> stage.close());

        javafx.scene.layout.AnchorPane.setBottomAnchor(ok, 25.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(ok, 30.0);

        Label info = new Label("i");
        info.setAlignment(Pos.CENTER);
        info.setStyle("""
            -fx-background-color: rgba(255,255,255,0.12);
            -fx-text-fill: white;
            -fx-font-size: 22px;
            -fx-font-family: 'Copperplate Gothic Bold';
            -fx-background-radius: 999;
            -fx-border-radius: 999;
            -fx-border-color: rgba(255,255,255,0.55);
            -fx-border-width: 2;
        """);
        info.setPrefSize(42, 42);

        javafx.scene.control.Tooltip.install(info,
                new javafx.scene.control.Tooltip("This screen summarizes the question outcome."));

        javafx.scene.layout.AnchorPane.setTopAnchor(info, 18.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(info, 22.0);

        root.getChildren().addAll(text, ok, info);

        stage.setScene(new Scene(root, 820, 520));
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private void runStep(int idx) {
        if (idx < 0 || idx >= steps.size()) return;

        stepIndex = idx;

        hideAllHighlights();

        TourStep s = steps.get(idx);
        stepTitle.setText(s.title);
        stepBody.setText(s.body);
        stepCounter.setText((idx + 1) + " / " + steps.size());

        prevBtn.setDisable(idx == 0);
        nextBtn.setDisable(idx == steps.size() - 1);

        s.action.run();
    }

    // ----------------- Scenario Reset -----------------

    private void resetScenario() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                setHidden(p1Tiles[r][c]);
                setHidden(p2Tiles[r][c]);

                p1Type[r][c] = DemoCellType.HIDDEN;
                p2Type[r][c] = DemoCellType.HIDDEN;

                p1Revealed[r][c] = false;
                p2Revealed[r][c] = false;

                p1Used[r][c] = false;
                p2Used[r][c] = false;
            }
        }
        hideAllHighlights();
    }

    private void resetDemoState() {
        p1Mines = START_MINES;
        p1Flags = START_FLAGS;
        p1Questions = START_QUESTIONS;
        p1Surprises = START_SURPRISES;

        p2Mines = START_MINES;
        p2Flags = START_FLAGS;
        p2Questions = START_QUESTIONS;
        p2Surprises = START_SURPRISES;

        sharedHearts = START_HEARTS;
        score = 0;

        updateInfoBar();
        buildHeartsBar();

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                p1Type[r][c] = DemoCellType.HIDDEN;
                p2Type[r][c] = DemoCellType.HIDDEN;
                p1Revealed[r][c] = false;
                p2Revealed[r][c] = false;
                p1Used[r][c] = false;
                p2Used[r][c] = false;
            }
        }
    }

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

    // ----------------- Turn Visual State -----------------

    private void setTurn(boolean p1Turn) {
        p1Grid.getStyleClass().removeAll("active-board", "inactive-board");
        p2Grid.getStyleClass().removeAll("active-board", "inactive-board");

        if (p1Turn) {
            p1Grid.getStyleClass().add("active-board");
            p2Grid.getStyleClass().add("inactive-board");
        } else {
            p2Grid.getStyleClass().add("active-board");
            p1Grid.getStyleClass().add("inactive-board");
        }

        isP1Turn = p1Turn;
        updateInfoBar();
    }

    

    // ----------------- Highlight logic -----------------

    private void hideAllHighlights() {
        setRectVisible(p1RowHL, false);
        setRectVisible(p1ColHL, false);
        setRectVisible(p1CellHL, false);
        setRectVisible(p1NbrHL, false);

        setRectVisible(p2RowHL, false);
        setRectVisible(p2ColHL, false);
        setRectVisible(p2CellHL, false);
        setRectVisible(p2NbrHL, false);
    }

    private void setRectVisible(Rectangle r, boolean on) {
        r.setVisible(on);
        r.setManaged(false);
        r.setMouseTransparent(true);
        r.setOpacity(on ? 1.0 : 0.0);
    }

    private void highlightCell(boolean isP1, int row, int col) {
        StackPane[][] tiles = isP1 ? p1Tiles : p2Tiles;
        StackPane layer = isP1 ? p1BoardLayer : p2BoardLayer;
        Rectangle cellHL = isP1 ? p1CellHL : p2CellHL;

        Bounds b = nodeBoundsInLayer(tiles[row][col], layer);
        positionRect(cellHL, b);
        setRectVisible(cellHL, true);
    }

    private void highlightNeighborhood(boolean isP1, int row, int col) {
        StackPane[][] tiles = isP1 ? p1Tiles : p2Tiles;
        StackPane layer = isP1 ? p1BoardLayer : p2BoardLayer;
        Rectangle nbrHL = isP1 ? p1NbrHL : p2NbrHL;

        int r0 = Math.max(0, row - 1);
        int c0 = Math.max(0, col - 1);
        int r1 = Math.min(SIZE - 1, row + 1);
        int c1 = Math.min(SIZE - 1, col + 1);

        Bounds a = nodeBoundsInLayer(tiles[r0][c0], layer);
        Bounds b = nodeBoundsInLayer(tiles[r1][c1], layer);

        Bounds merged = mergeBounds(a, b);
        positionRect(nbrHL, merged);
        setRectVisible(nbrHL, true);
    }

    private void highlightArea(boolean isP1, int r0, int c0, int r1, int c1) {
        StackPane[][] tiles = isP1 ? p1Tiles : p2Tiles;
        StackPane layer = isP1 ? p1BoardLayer : p2BoardLayer;

        Rectangle areaHL = isP1 ? p1RowHL : p2RowHL;

        int rr0 = Math.max(0, Math.min(r0, r1));
        int cc0 = Math.max(0, Math.min(c0, c1));
        int rr1 = Math.min(SIZE - 1, Math.max(r0, r1));
        int cc1 = Math.min(SIZE - 1, Math.max(c0, c1));

        Bounds a = nodeBoundsInLayer(tiles[rr0][cc0], layer);
        Bounds b = nodeBoundsInLayer(tiles[rr1][cc1], layer);

        Bounds merged = mergeBounds(a, b);
        positionRect(areaHL, merged);
        setRectVisible(areaHL, true);
    }

    private Bounds nodeBoundsInLayer(StackPane node, StackPane layer) {
        Bounds scene = node.localToScene(node.getBoundsInLocal());
        return layer.sceneToLocal(scene);
    }

    private Bounds mergeBounds(Bounds a, Bounds b) {
        double minX = Math.min(a.getMinX(), b.getMinX());
        double minY = Math.min(a.getMinY(), b.getMinY());
        double maxX = Math.max(a.getMaxX(), b.getMaxX());
        double maxY = Math.max(a.getMaxY(), b.getMaxY());
        return new BoundingBox(minX, minY, (maxX - minX), (maxY - minY));
    }

    private void positionRect(Rectangle rect, Bounds b) {
        double pad = 4;
        rect.setX(b.getMinX() - pad);
        rect.setY(b.getMinY() - pad);
        rect.setWidth(b.getWidth() + 2 * pad);
        rect.setHeight(b.getHeight() + 2 * pad);
        rect.setOpacity(1.0);
    }

    // ----------------- Pulse animation -----------------

    private void startPulse() {
        if (pulse != null) pulse.stop();

        pulse = new Timeline(
                new KeyFrame(Duration.ZERO, e -> setPulseOpacity(1.0)),
                new KeyFrame(Duration.seconds(0.55), e -> setPulseOpacity(0.55)),
                new KeyFrame(Duration.seconds(1.10), e -> setPulseOpacity(1.0))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    private void setPulseOpacity(double v) {
        pulseRect(p1RowHL, v);
        pulseRect(p1ColHL, v);
        pulseRect(p1CellHL, v);
        pulseRect(p1NbrHL, v);

        pulseRect(p2RowHL, v);
        pulseRect(p2ColHL, v);
        pulseRect(p2CellHL, v);
        pulseRect(p2NbrHL, v);
    }

    private void pulseRect(Rectangle r, double v) {
        if (r.isVisible()) r.setOpacity(v);
    }

    // ----------------- Cell Painting (uses your CSS) -----------------

    private Button btn(StackPane tile) {
        return (Button) tile.getChildren().get(0);
    }

    private void clearCellState(Button b) {
        b.setText("");
        b.setGraphic(null);
        b.setDisable(false);

        b.getStyleClass().removeAll(
                "cell-hidden", "cell-revealed", "cell-flagged", "cell-mine",
                "cell-number", "cell-question", "cell-surprise"
        );
    }

    private void setIcon(Button b, String resourcePath, double size) {
        try {
            Image img = new Image(getClass().getResourceAsStream(resourcePath));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            b.setGraphic(iv);
        } catch (Exception ignored) { }
    }

    private void setHidden(StackPane tile) {
        var b = btn(tile);
        clearCellState(b);
        b.getStyleClass().add("cell-hidden");
    }

    private void setRevealedNumber(StackPane tile, int n) {
        var b = btn(tile);
        clearCellState(b);
        b.setText(String.valueOf(n));
        b.getStyleClass().addAll("cell-revealed", "cell-number");
    }

    private void setEmptyRevealed(StackPane tile) {
        var b = btn(tile);
        clearCellState(b);
        b.setText("");
        b.getStyleClass().add("cell-revealed");
    }

    private void setFlag(StackPane tile) {
        var b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/red-flag.png", 22);
        b.getStyleClass().add("cell-flagged");
    }

    private void setMine(StackPane tile) {
        var b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/bomb.png", 22);
        b.setDisable(true);
        b.getStyleClass().addAll("cell-revealed", "cell-mine");
    }

    private void setQuestion(StackPane tile) {
        var b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/question-mark.png", 20);
        b.getStyleClass().addAll("cell-revealed", "cell-question");
    }

    private void setSurprise(StackPane tile) {
        var b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/giftbox.png", 20);
        b.getStyleClass().addAll("cell-revealed", "cell-surprise");
    }

    // ----------------- Demo state marking helpers -----------------

    private void markRevealed(boolean isP1, int r, int c, DemoCellType t) {
        if (isP1) {
            p1Type[r][c] = t;
            p1Revealed[r][c] = true;
        } else {
            p2Type[r][c] = t;
            p2Revealed[r][c] = true;
        }
    }

    private void markFlag(boolean isP1, int r, int c) {
        if (isP1) p1Type[r][c] = DemoCellType.FLAG;
        else p2Type[r][c] = DemoCellType.FLAG;
    }

    private boolean isRevealed(boolean isP1, int r, int c) {
        return isP1 ? p1Revealed[r][c] : p2Revealed[r][c];
    }

    private boolean isUsed(boolean isP1, int r, int c) {
        return isP1 ? p1Used[r][c] : p2Used[r][c];
    }

    private void markUsed(boolean isP1, int r, int c) {
        if (isP1) p1Used[r][c] = true;
        else p2Used[r][c] = true;
    }

    private boolean isQuestionCell(boolean isP1, int r, int c) {
        return (isP1 ? p1Type[r][c] : p2Type[r][c]) == DemoCellType.QUESTION;
    }

    private boolean isSurpriseCell(boolean isP1, int r, int c) {
        return (isP1 ? p1Type[r][c] : p2Type[r][c]) == DemoCellType.SURPRISE;
    }

    // ----------------- Small helper record -----------------

    private static final class TourStep {
        final String title;
        final String body;
        final Runnable action;

        TourStep(String title, String body, Runnable action) {
            this.title = title;
            this.body = body;
            this.action = action;
        }
    }

    private ImageView icon(String path, double size) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            System.err.println("Missing resource: " + path);
            return new ImageView();
        }
        ImageView iv = new ImageView(new Image(stream));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }

    private HBox statItem(String iconPath, String value) {
        ImageView iv = icon(iconPath, 18);

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("stats-value");

        HBox item = new HBox(6, iv, valueLbl);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPickOnBounds(false);
        return item;
    }

    private Label sep() {
        Label l = new Label("|");
        l.getStyleClass().add("stats-sep");
        return l;
    }

    private void buildPlayerStats(HBox box, String nickname,
                                  int mines, int flags, int surprises, int questions) {
        if (box == null) return;

        box.getChildren().clear();

        Label name = new Label(nickname + ",");
        name.getStyleClass().add("player-name");

        box.getChildren().add(name);
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/bomb.png", String.valueOf(mines)));
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/red-flag.png", String.valueOf(flags)));
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/giftbox.png", String.valueOf(surprises)));
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/question-mark.png", String.valueOf(questions)));
    }

    private void updateInfoBar() {
        buildPlayerStats(p1StatsBox, "Player 1", p1Mines, p1Flags, p1Surprises, p1Questions);
        buildPlayerStats(p2StatsBox, "Player 2", p2Mines, p2Flags, p2Surprises, p2Questions);

        turnLabel.setText("Turn: " + (isP1Turn ? "Player 1" : "Player 2"));

        if (scoreLabel != null) {
            scoreLabel.setText("Score: " + score);
        }
    }

    private static final class BoundingBox extends javafx.geometry.BoundingBox {
        BoundingBox(double minX, double minY, double width, double height) {
            super(minX, minY, width, height);
        }
    }

    private void setSnapshot(
            int p1Mines, int p1Flags, int p1Questions, int p1Surprises,
            int p2Mines, int p2Flags, int p2Questions, int p2Surprises,
            int hearts,
            boolean p1Turn,
            int score
    ) {
        this.p1Mines = p1Mines;
        this.p1Flags = p1Flags;
        this.p1Questions = p1Questions;
        this.p1Surprises = p1Surprises;

        this.p2Mines = p2Mines;
        this.p2Flags = p2Flags;
        this.p2Questions = p2Questions;
        this.p2Surprises = p2Surprises;

        this.sharedHearts = hearts;
        this.score = score;

        setTurn(p1Turn);
        buildHeartsBar();
        updateInfoBar();
    }
}
