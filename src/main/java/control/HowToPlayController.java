package control;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.SoundManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

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

    private boolean tutorialPopupsEnabled = true;

    // ----- FIXED DEMO QUESTION FOR HOW TO PLAY -----
    private static final model.Question DEMO_EXPERT_QUESTION =
            new model.Question(
                    999,
                    "expert",
                    "In the SQA defect removal cost model, why is solving a defect during operation extremely expensive?",
                    "It requires rewriting documentation",
                    "It delays integration",
                    "It must be solved by senior managers",
                    "The defect impacts real users and may require large-scale fixes",
                    3
            );

    // ----- FIXED DEMO SURPRISE FOR HOW TO PLAY -----
    private static final boolean DEMO_SURPRISE_IS_GOOD = true;
    private static final int DEMO_SURPRISE_SCORE = 8;
    private static final int DEMO_SURPRISE_LIVES = 1;

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

    private final DemoCellType[][] p1Type = new DemoCellType[SIZE][SIZE];
    private final DemoCellType[][] p2Type = new DemoCellType[SIZE][SIZE];

    private final boolean[][] p1Revealed = new boolean[SIZE][SIZE];
    private final boolean[][] p2Revealed = new boolean[SIZE][SIZE];

    private final boolean[][] p1Used = new boolean[SIZE][SIZE];
    private final boolean[][] p2Used = new boolean[SIZE][SIZE];

    // Easy-mode constants
    private static final int ACTIVATION_COST_EASY = 5;
    @SuppressWarnings("unused")
    private static final int SURPRISE_POINTS_EASY = 8;

    private static final int TOTAL_HEART_SLOTS = 10;

    // --------- AUTO PLAY SCHEDULING ----------
    private PauseTransition autoStepPause;
    private static final double DEFAULT_STEP_DELAY = 2.5;

    // question: show QUESTION, then show RESULTS, then continue
    private static final double QUESTION_READ_DELAY = 7.0;
    private static final double QUESTION_RESULT_DELAY = 6.0;

    // surprise popup reading time (auto-close)
    private static final double SURPRISE_READ_DELAY = 6.0;

    // ----------------- USED OVERLAY (dark like real game) -----------------
    private static final String USED_OVERLAY_ID = "USED_OVERLAY";

    @FXML
    private void initialize() {
        p1Tiles = builder.build(p1Grid, SIZE, SIZE, true);
        p2Tiles = builder.build(p2Grid, SIZE, SIZE, false);

        stopBtn.setDisable(true);

        resetDemoState();
        resetScenario();
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

            Stage stage = (Stage) backBtn.getScene().getWindow();
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
        runStep(0);
    }

    @FXML
    private void onPlay() {
        if (isAutoPlaying) return;

        isAutoPlaying = true;
        playBtn.setDisable(true);
        stopBtn.setDisable(false);

        scheduleNextAutoStep(0.0);
    }

    private void scheduleNextAutoStep(double delaySeconds) {
        if (autoStepPause != null) autoStepPause.stop();

        autoStepPause = new PauseTransition(Duration.seconds(delaySeconds));
        autoStepPause.setOnFinished(e -> {
            if (!isAutoPlaying) return;

            if (stepIndex < steps.size() - 1) {
                stepIndex++;
                runStepInternal(stepIndex, true);

                scheduleNextAutoStep(getAutoDelayForStep(stepIndex));
            } else {
                onStop();
            }
        });
        autoStepPause.play();
    }

    private double getAutoDelayForStep(int idx) {
        if (idx == 10) return QUESTION_READ_DELAY + QUESTION_RESULT_DELAY;
        if (idx == 11) return SURPRISE_READ_DELAY;
        return DEFAULT_STEP_DELAY;
    }

    private void stopAutoplayIfRunning() {
        if (isAutoPlaying) onStop();
    }

    @FXML
    private void onStop() {
        isAutoPlaying = false;

        if (autoStepPause != null) autoStepPause.stop();
        autoStepPause = null;

        playBtn.setDisable(false);
        stopBtn.setDisable(true);
    }

    @FXML
    private void onPrev() {
        stopAutoplayIfRunning();
        if (stepIndex > 0) {
            replayToStep(stepIndex - 1);
        }
    }

    @FXML
    private void onNext() {
        stopAutoplayIfRunning();
        if (stepIndex < steps.size() - 1) {
            stepIndex++;
            runStepInternal(stepIndex, true);
        }
    }

    // ----------------- Steps -----------------

    private void buildSteps() {
        steps.clear();

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

        steps.add(new TourStep(
                "Turn Switch",
                "Because Player 1 performed a turn-ending action (Reveal), the turn switches to Player 2.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 40, 6, 2, 10, false, 0);
                    highlightCell(false, 4, 4);
                }
        ));

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

        steps.add(new TourStep(
                "Turn Switch",
                "After Player 2 revealed a cell, the turn switches to Player 1.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, 0);
                    highlightCell(true, 2, 6);
                }
        ));

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

        steps.add(new TourStep(
                "Turn Switch",
                "Because Player 1 performed a turn-ending action (Reveal), the turn switches to Player 2.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 0);
                    highlightCell(false, 2, 2);
                }
        ));

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

        steps.add(new TourStep(
                "Player 1: Activate Question (Separate Action)",
                "Now Player 1 activates the previously revealed Question cell. " +
                        "Tutorial: correct answer is highlighted in green, wrong answers in red, and the result screen shows what would happen for each option.",
                () -> {
                    hideAllHighlights();

                    setQuestion(p1Tiles[2][6]);
                    setSurprise(p2Tiles[2][2]);
                    markRevealed(true, 2, 6, DemoCellType.QUESTION);
                    markRevealed(false, 2, 2, DemoCellType.SURPRISE);

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, score);
                    highlightCell(true, 2, 6);

                    if (tutorialPopupsEnabled) {
                        Platform.runLater(() -> activateQuestionHowTo(true, 2, 6));
                    }
                }
        ));

        steps.add(new TourStep(
                "Player 2: Activate Surprise (Separate Action)",
                "Now Player 2 activates the previously revealed Surprise cell. " +
                        "Tutorial: we show BOTH GOOD and BAD outcomes in a styled popup, but the demo applies a fixed outcome.",
                () -> {
                    hideAllHighlights();

                    setQuestion(p1Tiles[2][6]);
                    setSurprise(p2Tiles[2][2]);
                    markRevealed(true, 2, 6, DemoCellType.QUESTION);
                    markRevealed(false, 2, 2, DemoCellType.SURPRISE);

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, score);
                    highlightCell(false, 2, 2);

                    if (tutorialPopupsEnabled) {
                        Platform.runLater(() -> activateSurpriseHowTo(false, 2, 2));
                    }
                }
        ));
    }

    // ----------------- REAL-GAME-LIKE ACTIVATIONS (HowToPlay) -----------------

    private void activateQuestionHowTo(boolean isP1, int row, int col) {
        if (!isQuestionCell(isP1, row, col)) return;
        if (!isRevealed(isP1, row, col)) return;
        if (isUsed(isP1, row, col)) return;

        model.Question q = DEMO_EXPERT_QUESTION;

        // -------- AUTO PLAY MODE ----------
        if (isAutoPlaying) {
            int scoreBefore = score;
            int livesBefore = sharedHearts;

            score -= ACTIVATION_COST_EASY;
            buildHeartsBar();
            updateInfoBar();

            showQuestionPickDialogTutorialAutoClose(q, true, QUESTION_READ_DELAY, picked -> {
                if (!isAutoPlaying) return;

                boolean correct = (picked == q.getCorrectOption());
                String qDiff = (q.getDifficulty() == null) ? "easy" : q.getDifficulty().toLowerCase();

                int scoreChange = 0;
                int livesChange = 0;

                switch (qDiff) {
                    case "easy" -> scoreChange = correct ? +3 : -3;
                    case "medium" -> scoreChange = correct ? +6 : -6;
                    case "hard" -> scoreChange = correct ? +10 : -10;
                    default -> {
                        if (correct) { scoreChange = +15; livesChange = +2; }
                        else { scoreChange = -15; livesChange = -1; }
                    }
                }

                score += scoreChange;
                sharedHearts = clamp(sharedHearts + livesChange, 0, TOTAL_HEART_SLOTS);

                markUsed(isP1, row, col);
                setUsed(isP1 ? p1Tiles[row][col] : p2Tiles[row][col]);

                buildHeartsBar();
                updateInfoBar();

                showQuestionResultDialogWithAllOptionsAutoClose(
                        q,
                        picked,
                        scoreBefore,
                        livesBefore,
                        score,
                        sharedHearts,
                        ACTIVATION_COST_EASY,
                        QUESTION_RESULT_DELAY
                );
            });

            return;
        }

        // -------- MANUAL MODE ----------
        int scoreBefore = score;
        int livesBefore = sharedHearts;

        score -= ACTIVATION_COST_EASY;

        int picked = showQuestionPickDialogTutorial(q, true);
        if (picked < 0) {
            markUsed(isP1, row, col);
            setUsed(isP1 ? p1Tiles[row][col] : p2Tiles[row][col]);
            buildHeartsBar();
            updateInfoBar();
            return;
        }

        boolean correct = (picked == q.getCorrectOption());
        String qDiff = (q.getDifficulty() == null) ? "easy" : q.getDifficulty().toLowerCase();

        int scoreChange = 0;
        int livesChange = 0;

        switch (qDiff) {
            case "easy" -> scoreChange = correct ? +3 : -3;
            case "medium" -> scoreChange = correct ? +6 : -6;
            case "hard" -> scoreChange = correct ? +10 : -10;
            default -> {
                if (correct) { scoreChange = +15; livesChange = +2; }
                else { scoreChange = -15; livesChange = -1; }
            }
        }

        score += scoreChange;
        sharedHearts = clamp(sharedHearts + livesChange, 0, TOTAL_HEART_SLOTS);

        markUsed(isP1, row, col);
        setUsed(isP1 ? p1Tiles[row][col] : p2Tiles[row][col]);

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
        if (!isSurpriseCell(isP1, row, col)) return;
        if (!isRevealed(isP1, row, col)) return;
        if (isUsed(isP1, row, col)) return;

        int scoreBefore = score;
        int livesBefore = sharedHearts;

        score -= ACTIVATION_COST_EASY;

        boolean good = DEMO_SURPRISE_IS_GOOD;

        int scoreDeltaFromEffect = good ? +DEMO_SURPRISE_SCORE : -DEMO_SURPRISE_SCORE;
        int livesDeltaFromEffect = good ? +DEMO_SURPRISE_LIVES : -DEMO_SURPRISE_LIVES;

        score += scoreDeltaFromEffect;
        sharedHearts = clamp(sharedHearts + livesDeltaFromEffect, 0, TOTAL_HEART_SLOTS);

        markUsed(isP1, row, col);
        setUsed(isP1 ? p1Tiles[row][col] : p2Tiles[row][col]);

        buildHeartsBar();
        updateInfoBar();

        if (isAutoPlaying) {
            showSurpriseTutorialDialogAutoClose(
                    scoreBefore, livesBefore,
                    DEMO_SURPRISE_SCORE, DEMO_SURPRISE_LIVES,
                    ACTIVATION_COST_EASY,
                    SURPRISE_READ_DELAY
            );
            return;
        }

        showSurpriseTutorialDialog(
                scoreBefore, livesBefore,
                DEMO_SURPRISE_SCORE, DEMO_SURPRISE_LIVES,
                ACTIVATION_COST_EASY
        );
    }

    private int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    // ----------------- AUTO: SAME Question Dialog UI, but auto-close -----------------

    private void showQuestionPickDialogTutorialAutoClose(model.Question q, boolean tutorialMode, double seconds, IntConsumer onAutoPick) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Question");

        VBox root = new VBox(12);
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

        int correctIdx = q.getCorrectOption();

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

        btnA.setDisable(true);
        btnB.setDisable(true);
        btnC.setDisable(true);
        btnD.setDisable(true);

        VBox answersBox = new VBox(10, btnA, btnB, btnC, btnD);

        Label closing = new Label("Closing automatically...");
        closing.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13px;");

        root.getChildren().addAll(title, questionText, hint, answersBox, closing);

        stage.setScene(new Scene(root, 620, 500));
        stage.centerOnScreen();
        stage.show();

        PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
        pt.setOnFinished(e -> {
            stage.close();
            onAutoPick.accept(correctIdx);
        });
        pt.play();
    }

    // ----------------- Tutorial Question Dialog (manual) -----------------

    private int showQuestionPickDialogTutorial(model.Question q, boolean tutorialMode) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Question");

        VBox root = new VBox(12);
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

        int correctIdx = q.getCorrectOption();

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

        VBox answersBox = new VBox(10, btnA, btnB, btnC, btnD);
        root.getChildren().addAll(title, questionText, hint, answersBox);

        stage.setScene(new Scene(root, 620, 460));
        stage.centerOnScreen();
        stage.showAndWait();

        return chosen[0];
    }

    private void styleAnswerForTutorial(Button b, boolean correct) {
        String border = correct ? "rgba(70, 255, 140, 0.85)" : "rgba(255, 90, 90, 0.80)";
        String bg = correct ? "rgba(70, 255, 140, 0.12)" : "rgba(255, 90, 90, 0.10)";

        b.setStyle(b.getStyle() + """
            ; -fx-border-color: %s;
              -fx-border-width: 2;
              -fx-background-color: %s;
        """.formatted(border, bg));
    }

    // ----------------- Result Dialog with ALL options outcomes -----------------

    private static final class Outcome {
        final int scoreDelta;
        final int livesDelta;

        Outcome(int scoreDelta, int livesDelta) {
            this.scoreDelta = scoreDelta;
            this.livesDelta = livesDelta;
        }
    }

    private Outcome outcomeForDifficulty(String diff, boolean isCorrect) {
        diff = (diff == null) ? "easy" : diff.toLowerCase();
        return switch (diff) {
            case "easy" -> isCorrect ? new Outcome(+3, 0) : new Outcome(-3, 0);
            case "medium" -> isCorrect ? new Outcome(+6, 0) : new Outcome(-6, 0);
            case "hard" -> isCorrect ? new Outcome(+10, 0) : new Outcome(-10, 0);
            default -> isCorrect ? new Outcome(+15, +2) : new Outcome(-15, -1);
        };
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
        Stage stage = buildQuestionResultStage(q, chosenIdx, scoreBefore, livesBefore, scoreAfter, livesAfter, activationCost, true);
        stage.showAndWait();
    }

    private void showQuestionResultDialogWithAllOptionsAutoClose(
            model.Question q,
            int chosenIdx,
            int scoreBefore,
            int livesBefore,
            int scoreAfter,
            int livesAfter,
            int activationCost,
            double seconds
    ) {
        Stage stage = buildQuestionResultStage(q, chosenIdx, scoreBefore, livesBefore, scoreAfter, livesAfter, activationCost, false);
        stage.show();

        PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
        pt.setOnFinished(e -> stage.close());
        pt.play();
    }

    private Stage buildQuestionResultStage(
            model.Question q,
            int chosenIdx,
            int scoreBefore,
            int livesBefore,
            int scoreAfter,
            int livesAfter,
            int activationCost,
            boolean showOkButton
    ) {
        int correctIdx = q.getCorrectOption();
        String diff = (q.getDifficulty() == null) ? "easy" : q.getDifficulty();

        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Question Result");

        AnchorPane root = new AnchorPane();
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #5b5bb6, #8a57b8, #c26ad6);
        """);

        boolean isCorrectChosen = (chosenIdx == correctIdx);

        Label header = new Label(
                "YOU ANSWERED A " + diff.toUpperCase() + " QUESTION.\n\n" +
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

        VBox outcomes = new VBox(8,
                makeOutcomeLine("A", 0, correctIdx, chosenIdx, diff, activationCost),
                makeOutcomeLine("B", 1, correctIdx, chosenIdx, diff, activationCost),
                makeOutcomeLine("C", 2, correctIdx, chosenIdx, diff, activationCost),
                makeOutcomeLine("D", 3, correctIdx, chosenIdx, diff, activationCost)
        );

        VBox box = new VBox(12, header, beforeAfter, outcomes);
        box.setPadding(new Insets(26));

        AnchorPane.setTopAnchor(box, 0.0);
        AnchorPane.setLeftAnchor(box, 0.0);
        AnchorPane.setRightAnchor(box, 0.0);

        root.getChildren().add(box);

        if (showOkButton) {
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

            AnchorPane.setBottomAnchor(ok, 22.0);
            AnchorPane.setRightAnchor(ok, 26.0);

            root.getChildren().add(ok);
        } else {
            Label closing = new Label("Closing automatically...");
            closing.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 13px;");
            AnchorPane.setBottomAnchor(closing, 24.0);
            AnchorPane.setRightAnchor(closing, 26.0);
            root.getChildren().add(closing);
        }

        stage.setScene(new Scene(root, 880, 560));
        stage.centerOnScreen();
        return stage;
    }

    private Node makeOutcomeLine(String letter, int idx, int correctIdx, int chosenIdx, String diff, int activationCost) {
        boolean wouldBeCorrect = (idx == correctIdx);
        Outcome o = outcomeForDifficulty(diff, wouldBeCorrect);

        int netScore = o.scoreDelta - activationCost;
        int netLives = o.livesDelta;

        String text = letter + ") " +
                (wouldBeCorrect ? "CORRECT" : "WRONG") +
                "  →  Score " + (netScore >= 0 ? "+" : "") + netScore +
                " , Lives " + (netLives >= 0 ? "+" : "") + netLives +
                "   (" + (wouldBeCorrect ? "+" : "") + o.scoreDelta +
                " score, " + (o.livesDelta >= 0 ? "+" : "") + o.livesDelta +
                " lives, -" + activationCost + " activation)";

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

    // ----------------- Surprise Tutorial Popup (GOOD + BAD) -----------------

    private void showSurpriseTutorialDialog(
            int scoreBefore,
            int livesBefore,
            int surprisePoints,
            int surpriseLives,
            int activationCost
    ) {
        Stage stage = buildSurpriseStage(scoreBefore, livesBefore, surprisePoints, surpriseLives, activationCost, true);
        stage.showAndWait();
    }

    private void showSurpriseTutorialDialogAutoClose(
            int scoreBefore,
            int livesBefore,
            int surprisePoints,
            int surpriseLives,
            int activationCost,
            double seconds
    ) {
        Stage stage = buildSurpriseStage(scoreBefore, livesBefore, surprisePoints, surpriseLives, activationCost, false);
        stage.show();

        PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
        pt.setOnFinished(e -> stage.close());
        pt.play();
    }

    private Stage buildSurpriseStage(
            int scoreBefore,
            int livesBefore,
            int surprisePoints,
            int surpriseLives,
            int activationCost,
            boolean showOkButton
    ) {
        int goodNetScore = -activationCost + surprisePoints;
        int goodNetLives = +surpriseLives;

        int badNetScore = -activationCost - surprisePoints;
        int badNetLives = -surpriseLives;

        int goodScoreAfter = scoreBefore + goodNetScore;
        int badScoreAfter = scoreBefore + badNetScore;

        int goodLivesAfter = clamp(livesBefore + goodNetLives, 0, TOTAL_HEART_SLOTS);
        int badLivesAfter = clamp(livesBefore + badNetLives, 0, TOTAL_HEART_SLOTS);

        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("Surprise Result");

        AnchorPane root = new AnchorPane();
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #5b5bb6, #8a57b8, #c26ad6);
        """);

        Label header = new Label("SURPRISE TUTORIAL RESULT");
        header.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 26px;
            -fx-font-family: 'Copperplate Gothic Bold';
        """);
        AnchorPane.setTopAnchor(header, 26.0);
        AnchorPane.setLeftAnchor(header, 28.0);

        Label info = new Label(
                "Score: " + scoreBefore + "\n" +
                        "Lives: " + livesBefore + "/" + TOTAL_HEART_SLOTS + "\n" +
                        "Activation cost: -" + activationCost + " score\n\n" +
                        "Tutorial mode: we show BOTH outcomes (GOOD and BAD)."
        );
        info.setStyle("""
            -fx-text-fill: rgba(255,255,255,0.90);
            -fx-font-size: 15px;
            -fx-font-family: 'Copperplate Gothic Light';
        """);
        AnchorPane.setTopAnchor(info, 78.0);
        AnchorPane.setLeftAnchor(info, 30.0);

        VBox cards = new VBox(14);
        cards.setPadding(new Insets(10, 30, 10, 30));

        HBox goodCard = buildSurpriseOutcomeCard(
                "GOOD SURPRISE",
                goodNetScore, goodNetLives,
                surprisePoints, activationCost,
                goodScoreAfter, goodLivesAfter,
                true
        );

        HBox badCard = buildSurpriseOutcomeCard(
                "BAD SURPRISE",
                badNetScore, badNetLives,
                surprisePoints, activationCost,
                badScoreAfter, badLivesAfter,
                false
        );

        cards.getChildren().addAll(goodCard, badCard);

        AnchorPane.setTopAnchor(cards, 200.0);
        AnchorPane.setLeftAnchor(cards, 0.0);
        AnchorPane.setRightAnchor(cards, 0.0);

        Label icon = new Label("i");
        icon.setAlignment(Pos.CENTER);
        icon.setStyle("""
            -fx-background-color: rgba(255,255,255,0.12);
            -fx-text-fill: white;
            -fx-font-size: 22px;
            -fx-font-family: 'Copperplate Gothic Bold';
            -fx-background-radius: 999;
            -fx-border-radius: 999;
            -fx-border-color: rgba(255,255,255,0.55);
            -fx-border-width: 2;
        """);
        icon.setPrefSize(42, 42);
        Tooltip.install(icon, new Tooltip("This popup explains Surprise outcomes in tutorial mode."));

        AnchorPane.setTopAnchor(icon, 18.0);
        AnchorPane.setRightAnchor(icon, 22.0);

        root.getChildren().addAll(header, info, cards, icon);

        if (showOkButton) {
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

            AnchorPane.setBottomAnchor(ok, 22.0);
            AnchorPane.setRightAnchor(ok, 28.0);
            root.getChildren().add(ok);
        } else {
            Label closing = new Label("Closing automatically...");
            closing.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 13px;");
            AnchorPane.setBottomAnchor(closing, 24.0);
            AnchorPane.setRightAnchor(closing, 28.0);
            root.getChildren().add(closing);
        }

        stage.setScene(new Scene(root, 820, 520));
        stage.centerOnScreen();
        return stage;
    }

    private HBox buildSurpriseOutcomeCard(
            String title,
            int netScore,
            int netLives,
            int surprisePoints,
            int activationCost,
            int scoreAfter,
            int livesAfter,
            boolean good
    ) {
        String border = good ? "rgba(100,255,140,0.95)" : "rgba(255,80,80,0.95)";
        String bg = good ? "rgba(80,220,120,0.12)" : "rgba(255,80,80,0.10)";

        Label txt = new Label(
                title + "  →  " +
                        "Score " + (netScore >= 0 ? "+" : "") + netScore +
                        ", Lives " + (netLives >= 0 ? "+" : "") + netLives +
                        "   (" + (good ? "+" : "-") + surprisePoints + " surprise, -" + activationCost + " activation)\n" +
                        "After: Score " + scoreAfter + ", Lives " + livesAfter + "/" + TOTAL_HEART_SLOTS
        );
        txt.setWrapText(true);
        txt.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-family: 'Copperplate Gothic Light';
        """);

        HBox box = new HBox(txt);
        box.setPadding(new Insets(14));
        box.setStyle("""
            -fx-background-color: %s;
            -fx-background-radius: 16;
            -fx-border-radius: 16;
            -fx-border-color: %s;
            -fx-border-width: 2;
        """.formatted(bg, border));

        HBox.setHgrow(txt, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    // ----------------- UI helpers -----------------

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

        Tooltip tp = new Tooltip(text);
        tp.setWrapText(true);
        tp.setMaxWidth(420);
        b.setTooltip(tp);

        return b;
    }

    private void runStep(int idx) {
        if (idx < 0 || idx >= steps.size()) return;

        stopAutoplayIfRunning();

        stepIndex = idx;

        resetDemoState();
        resetScenario();
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
        cc1 = Math.min(SIZE - 1, Math.max(c0, c1));

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
                "cell-number", "cell-question", "cell-surprise", "cell-used"
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
        removeUsedOverlay(tile);
        Button b = btn(tile);
        clearCellState(b);
        b.getStyleClass().add("cell-hidden");
    }

    private void setRevealedNumber(StackPane tile, int n) {
        removeUsedOverlay(tile);
        Button b = btn(tile);
        clearCellState(b);
        b.setText(String.valueOf(n));
        b.getStyleClass().addAll("cell-revealed", "cell-number");
    }

    private void setEmptyRevealed(StackPane tile) {
        removeUsedOverlay(tile);
        Button b = btn(tile);
        clearCellState(b);
        b.setText("");
        b.getStyleClass().add("cell-revealed");
    }

    private void setFlag(StackPane tile) {
        removeUsedOverlay(tile);
        Button b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/red-flag.png", 22);
        b.getStyleClass().add("cell-flagged");
    }

    private void setQuestion(StackPane tile) {
        removeUsedOverlay(tile);
        Button b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/question-mark.png", 20);
        b.getStyleClass().addAll("cell-revealed", "cell-question");
    }

    private void setSurprise(StackPane tile) {
        removeUsedOverlay(tile);
        Button b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/giftbox.png", 20);
        b.getStyleClass().addAll("cell-revealed", "cell-surprise");
    }

    // ----------------- Used (dark overlay but keep icon) -----------------

    private void applyUsedOverlay(StackPane tile) {
        for (Node n : tile.getChildren()) {
            if (USED_OVERLAY_ID.equals(n.getId())) return;
        }

        Rectangle overlay = new Rectangle();
        overlay.setId(USED_OVERLAY_ID);
        overlay.setFill(Color.rgb(0, 0, 0, 0.55));
        overlay.widthProperty().bind(tile.widthProperty());
        overlay.heightProperty().bind(tile.heightProperty());
        overlay.setMouseTransparent(true);
        overlay.setArcWidth(10);
        overlay.setArcHeight(10);

        tile.getChildren().add(overlay);
    }

    private void removeUsedOverlay(StackPane tile) {
        tile.getChildren().removeIf(n -> USED_OVERLAY_ID.equals(n.getId()));
    }

    private void setUsed(StackPane tile) {
        Button b = btn(tile);

        // keep icon, but block interaction
        b.setDisable(true);

        // keep revealed look, add used tag if you want CSS hooks
        if (!b.getStyleClass().contains("cell-used")) {
            b.getStyleClass().add("cell-used");
        }

        // dark overlay on top of the button
        applyUsedOverlay(tile);

        // optional: dim the icon a bit
        if (b.getGraphic() != null) {
            b.getGraphic().setOpacity(0.75);
        }
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

    private void runStepInternal(int idx, boolean allowPopups) {
        TourStep s = steps.get(idx);

        stepTitle.setText(s.title);
        stepBody.setText(s.body);
        stepCounter.setText((idx + 1) + " / " + steps.size());

        prevBtn.setDisable(idx == 0);
        nextBtn.setDisable(idx == steps.size() - 1);

        this.tutorialPopupsEnabled = allowPopups;

        s.action.run();
    }

    // ✅ FIXED: לא להריץ את ה-step האחרון פעמיים
    private void replayToStep(int targetIdx) {
        resetDemoState();
        resetScenario();
        hideAllHighlights();

        for (int i = 0; i < targetIdx; i++) {
            runStepInternal(i, false);
        }

        stepIndex = targetIdx;
        runStepInternal(targetIdx, true);
    }
}
