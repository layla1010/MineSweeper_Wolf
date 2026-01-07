package control;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import util.SoundManager;
import util.ViewNavigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntConsumer;

public class HowToPlayController {

    private static final int SIZE = 9;

    // Autoplay timings
    private static final double DEFAULT_STEP_DELAY = 2.5;
    private static final double QUESTION_READ_DELAY = 7.0;
    private static final double QUESTION_RESULT_DELAY = 6.0;
    private static final double SURPRISE_READ_DELAY = 6.0;

    // tiny delay so the board visually updates BEFORE a dialog blocks/pops
    private static final double FX_PULSE_DELAY = 0.12; // seconds
    private static final double CLICK_FEEDBACK_DELAY = 0.55; // seconds

    // Easy-mode constants (HowToPlay is essentially “easy demo”)
    private static final int ACTIVATION_COST_EASY = 5;
    private static final int TOTAL_HEART_SLOTS = 10;

    // Demo start values
    private static final int START_MINES = 10;
    private static final String START_FLAGS = "0/3";
    private static final int START_QUESTIONS = 6;
    private static final int START_SURPRISES = 2;
    private static final int START_HEARTS = 10;

    // Used overlay
    private static final String USED_OVERLAY_ID = "USED_OVERLAY";

    // Background resource (tries JPG first; falls back to PNG if needed)
    private static final String BG_PRIMARY = "/Images/OIP.jpg";
    private static final String BG_FALLBACK = "/Images/OIP.png";


    // NOTE: keeping your original object content (even though name says “EXPERT” and difficulty says “easy”)
    private static final model.Question DEMO_EXPERT_QUESTION =
            new model.Question(
                    999,
                    "easy",
                    "what is the colour of the sky?",
                    "yellow",
                    "purple",
                    "green",
                    "blue",
                    3
            );

    private static final boolean DEMO_SURPRISE_IS_GOOD = true;
    private static final int DEMO_SURPRISE_SCORE = 8;
    private static final int DEMO_SURPRISE_LIVES = 1;

    // ----------------- FXML -----------------

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

    @FXML private Button playBtn;
    @FXML private Button stopBtn;

    @FXML private HBox p1StatsBox;
    @FXML private HBox p2StatsBox;


    private StackPane[][] p1Tiles;
    private StackPane[][] p2Tiles;

    private boolean tutorialPopupsEnabled = true;
    private boolean isAutoPlaying = false;

    // demo counters
    private String p1Flags, p2Flags;
    private int p1Mines, p1Questions, p1Surprises;
    private int p2Mines, p2Questions, p2Surprises;

    private int sharedHearts;
    private int score;

    private boolean isP1Turn = true;

    private final HowToPlayBoardBuilder builder = new HowToPlayBoardBuilder();
    private final List<TourStep> steps = new ArrayList<>();
    private int stepIndex = 0;

    // pulse animation for highlight rectangles
    private Timeline pulse;

    // AUTO PLAY
    private PauseTransition autoStepPause;

    // RNG (centralized for QA/debug)
    private final Random rng = new Random();


    private enum DemoCellType { HIDDEN, EMPTY, NUMBER, FLAG, MINE, QUESTION, SURPRISE }

    private final DemoCellType[][] p1Type = new DemoCellType[SIZE][SIZE];
    private final DemoCellType[][] p2Type = new DemoCellType[SIZE][SIZE];

    private final boolean[][] p1Revealed = new boolean[SIZE][SIZE];
    private final boolean[][] p2Revealed = new boolean[SIZE][SIZE];

    private final boolean[][] p1Used = new boolean[SIZE][SIZE];
    private final boolean[][] p2Used = new boolean[SIZE][SIZE];

    // ----------------- INIT -----------------

    @FXML
    private void initialize() {
        p1Tiles = builder.build(p1Grid, SIZE, SIZE, true);
        p2Tiles = builder.build(p2Grid, SIZE, SIZE, false);

        stopBtn.setDisable(true);

        resetAll(true);
        buildSteps();

        Platform.runLater(() -> {
            startPulse();
            runStep(0);
        });
    }


    @FXML
    private void onBack() {
        SoundManager.playClick();
        stopAutoplayIfRunning();

        try {
            Stage stage = (Stage) backBtn.getScene().getWindow();
            ViewNavigator.switchTo(stage, "/view/settings_view.fxml");
        } catch (Exception e) {
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
        if (stepIndex > 0) replayToStep(stepIndex - 1);
    }

    @FXML
    private void onNext() {
        stopAutoplayIfRunning();
        if (stepIndex < steps.size() - 1) {
            stepIndex++;
            runStepInternal(stepIndex, true);
        }
    }

    private void stopAutoplayIfRunning() {
        if (isAutoPlaying) onStop();
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
        String title = steps.get(idx).title;
        if (title.contains("Activate Question")) return QUESTION_READ_DELAY + QUESTION_RESULT_DELAY;
        if (title.contains("Activate Surprise")) return SURPRISE_READ_DELAY;
        return DEFAULT_STEP_DELAY;
    }


    private void buildSteps() {
        steps.clear();

        steps.add(new TourStep(
                "Start (Easy Mode)",
                "Both boards start fully hidden. Each player can act ONLY on their own board. " +
                        "Turns alternate ONLY when a turn-ending action happens (Reveal / Activate Question / Activate Surprise / Mine). " +
                        "Placing flags does NOT end the turn.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, "0/3", 6, 2, 10, "0/3", 6, 2, 10, true, 0);
                    highlightCell(true, 4, 4);
                }
        ));

        steps.add(new TourStep(
                "Player 1: Reveal Empty (Cascade = One Action)",
                "Player 1 reveals one empty safe cell. The board opens adjacent empty cells automatically (cascade). " +
                        "Even though multiple cells open, it still counts as ONE action and ends the turn.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, "0/3", 6, 2, 10, "0/3", 6, 2, 10, true, 0);

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
                    setSnapshot(10, "0/3", 6, 2, 10, "0/3", 6, 2, 10, false, 0);
                    highlightCell(false, 4, 4);
                }
        ));

        steps.add(new TourStep(
                "Player 2: Flag (Does NOT End Turn)",
                "Player 2 places a flag. Flags do NOT end the turn, so Player 2 can keep placing more flags or choose another action.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, "0/3", 6, 2, 10, "1/3", 6, 2, 10, false, 0);

                    setFlag(p2Tiles[4][4]); markFlag(false, 4, 4);
                    highlightCell(false, 4, 4);
                }
        ));

        steps.add(new TourStep(
                "Player 2: Another Flag (Still Same Turn)",
                "Player 2 places another flag. Still the same turn (flags do NOT switch turns).",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, "0/3", 6, 2, 10, "2/3", 6, 2, 10, false, 0);

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
                    setSnapshot(10, "0/3", 6, 2, 10, "2/3", 6, 2, 10, false, 0);

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
                    setSnapshot(10, "0/3", 6, 2, 10, "0/3", 6, 2, 10, true, 0);
                }
        ));

        steps.add(new TourStep(
                "Player 1: Reveal Mine (Ends Turn + Lose a Life)",
                "Player 1 reveals a Mine. This ends the turn immediately and costs 1 shared life. " +
                        "The mine counter is also updated.",
                () -> {
                    hideAllHighlights();

                    p1Mines = 10; p1Flags = "0/3"; p1Questions = 6; p1Surprises = 2;
                    p2Mines = 10; p2Flags = "0/3"; p2Questions = 6; p2Surprises = 2;
                    sharedHearts = 10;

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, true, score);

                    int r = 1, c = 1;

                    setMine(p1Tiles[r][c]);
                    markRevealed(true, r, c, DemoCellType.MINE);

                    consumeMine(true);

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, true, score);

                    buildHeartsBar();
                    updateInfoBar();

                    highlightCell(true, r, c);
                }
        ));

        steps.add(new TourStep(
                "Player 2: Reveal Surprise Cell (Reveal ≠ Activate)",
                "Player 2 reveals a Surprise cell. This reveal is ONE action and ENDS the turn. " +
                        "Important: revealing the Surprise cell does NOT activate it automatically.",
                () -> {
                    hideAllHighlights();

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, false, score);

                    setSurprise(p2Tiles[2][2]);
                    markRevealed(false, 2, 2, DemoCellType.SURPRISE);

                    p2Surprises = Math.max(0, p2Surprises - 1);

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, false, score);

                    highlightCell(false, 2, 2);
                }
        ));

        steps.add(new TourStep(
                "Player 1: Reveal Question Cell (Reveal ≠ Activate)",
                "Player 1 reveals a Question cell. This reveal is ONE action and ENDS the turn. " +
                        "Important: revealing the Question cell does NOT activate it automatically.",
                () -> {
                    hideAllHighlights();

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, true, score);

                    setQuestion(p1Tiles[2][6]);
                    markRevealed(true, 2, 6, DemoCellType.QUESTION);

                    p1Questions = Math.max(0, p1Questions - 1);

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, true, score);

                    highlightCell(true, 2, 6);
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

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, false, score);

                    highlightCell(false, 2, 2);

                    if (tutorialPopupsEnabled) {
                        Platform.runLater(() -> playClickFeedback(false, 2, 2, () -> {
                            PauseTransition extra = new PauseTransition(Duration.seconds(0.6));
                            extra.setOnFinished(ev -> activateSurpriseHowTo(false, 2, 2));
                            extra.play();
                        }));
                    }
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

                    setSnapshot(p1Mines, p1Flags, p1Questions, p1Surprises,
                            p2Mines, p2Flags, p2Questions, p2Surprises,
                            sharedHearts, true, score);

                    highlightCell(true, 2, 6);

                    if (tutorialPopupsEnabled) {
                        Platform.runLater(() -> activateQuestionHowTo(true, 2, 6));
                    }
                }
        ));
    }

    private void runStep(int idx) {
        if (idx < 0 || idx >= steps.size()) return;

        stopAutoplayIfRunning();

        stepIndex = idx;

        resetAll(true);

        TourStep s = steps.get(idx);

        stepTitle.setText(s.title);
        stepBody.setText(s.body);
        stepCounter.setText((idx + 1) + " / " + steps.size());

        prevBtn.setDisable(idx == 0);
        nextBtn.setDisable(idx == steps.size() - 1);

        s.action.run();

        applyUsedCellsVisuals();
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
        applyUsedCellsVisuals();
    }

    // FIX: do not run last step twice
    private void replayToStep(int targetIdx) {
        resetAll(true);

        for (int i = 0; i < targetIdx; i++) {
            runStepInternal(i, false);
        }

        stepIndex = targetIdx;
        runStepInternal(targetIdx, true);
    }

    private void resetAll(boolean resetCounters) {
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

        if (resetCounters) {
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
        }
    }


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

    private void setSnapshot(
            int p1Mines, String p1Flags, int p1Questions, int p1Surprises,
            int p2Mines, String p2Flags, int p2Questions, int p2Surprises,
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
        if (r == null) return;
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
        if (rect == null || b == null) return;
        double pad = 4;
        rect.setX(b.getMinX() - pad);
        rect.setY(b.getMinY() - pad);
        rect.setWidth(b.getWidth() + 2 * pad);
        rect.setHeight(b.getHeight() + 2 * pad);
        rect.setOpacity(1.0);
    }

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
        if (r != null && r.isVisible()) r.setOpacity(v);
    }


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
            var stream = getClass().getResourceAsStream(resourcePath);
            if (stream == null) return;
            Image img = new Image(stream);
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

    private void setMine(StackPane tile) {
        removeUsedOverlay(tile);
        Button b = btn(tile);
        clearCellState(b);
        setIcon(b, "/Images/bomb.png", 22);
        b.getStyleClass().addAll("cell-revealed", "cell-mine");
    }


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

        b.setDisable(true);

        if (!b.getStyleClass().contains("cell-used")) {
            b.getStyleClass().add("cell-used");
        }

        applyUsedOverlay(tile);

        if (b.getGraphic() != null) {
            b.getGraphic().setOpacity(0.75);
        }
    }

    private void applyUsedCellsVisuals() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (p1Used[r][c]) setUsed(p1Tiles[r][c]);
                if (p2Used[r][c]) setUsed(p2Tiles[r][c]);
            }
        }
    }

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

    private int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private void consumeMine(boolean isP1) {
        if (isP1) p1Mines = Math.max(0, p1Mines - 1);
        else p2Mines = Math.max(0, p2Mines - 1);

        sharedHearts = clamp(sharedHearts - 1, 0, TOTAL_HEART_SLOTS);
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

    private void buildPlayerStats(HBox box, String nickname, int mines, String flags, int surprises, int questions) {
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


    private void playClickFeedback(boolean isP1, int row, int col, Runnable after) {
        StackPane tile = (isP1 ? p1Tiles : p2Tiles)[row][col];
        Button b = btn(tile);

        double oldSX = b.getScaleX();
        double oldSY = b.getScaleY();

        b.setScaleX(0.90);
        b.setScaleY(0.90);

        PauseTransition pt = new PauseTransition(Duration.seconds(CLICK_FEEDBACK_DELAY));
        pt.setOnFinished(e -> {
            b.setScaleX(oldSX);
            b.setScaleY(oldSY);
            if (after != null) after.run();
        });
        pt.play();
    }

    private void afterFxPulse(Runnable r) {
        PauseTransition pt = new PauseTransition(Duration.seconds(FX_PULSE_DELAY));
        pt.setOnFinished(e -> r.run());
        pt.play();
    }


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

    private static final class Outcome {
        final int scoreDelta;
        final int livesDelta;

        Outcome(int scoreDelta, int livesDelta) {
            this.scoreDelta = scoreDelta;
            this.livesDelta = livesDelta;
        }
    }

    // central rules (keeps your exact logic; easy/medium wrong is OR 50%)
    private Outcome computeOutcome(String diff, boolean correct) {
        String d = (diff == null) ? "easy" : diff.toLowerCase();

        if (correct) {
            return switch (d) {
                case "easy" -> new Outcome(+3, +1);
                case "medium" -> new Outcome(+6, 0);
                case "hard" -> new Outcome(+10, 0);
                default -> new Outcome(+15, +2);
            };
        }

        // wrong
        return switch (d) {
            case "easy" -> rng.nextBoolean() ? new Outcome(-3, 0) : new Outcome(0, 0);
            case "medium" -> rng.nextBoolean() ? new Outcome(-6, 0) : new Outcome(0, 0);
            case "hard" -> new Outcome(-10, 0);
            default -> new Outcome(-15, -1);
        };
    }


    private void activateQuestionHowTo(boolean isP1, int row, int col) {
        if (!isQuestionCell(isP1, row, col)) return;
        if (!isRevealed(isP1, row, col)) return;
        if (isUsed(isP1, row, col)) return;

        model.Question q = DEMO_EXPERT_QUESTION;

        int scoreBefore = score;
        int livesBefore = sharedHearts;

        // activation cost always applied first
        score -= ACTIVATION_COST_EASY;
        buildHeartsBar();
        updateInfoBar();

        boolean autoMode = isAutoPlaying;

        showQuestionPickDialog(
                q,
                true,                 // tutorial mode: highlight correct answer
                !autoMode,            // interactive only if NOT autoplay
                autoMode ? QUESTION_READ_DELAY : null,
                picked -> {
                    // On auto-pick, we always pick correctIdx; on manual pick, picked may be -1? (we enforce >=0 there)
                    if (picked < 0) {
                        markUsed(isP1, row, col);
                        setUsed(isP1 ? p1Tiles[row][col] : p2Tiles[row][col]);
                        buildHeartsBar();
                        updateInfoBar();
                        return;
                    }

                    boolean correct = (picked == q.getCorrectOption());
                    String qDiff = (q.getDifficulty() == null) ? "easy" : q.getDifficulty().toLowerCase();

                    Outcome o = computeOutcome(qDiff, correct);

                    score += o.scoreDelta;
                    sharedHearts = clamp(sharedHearts + o.livesDelta, 0, TOTAL_HEART_SLOTS);

                    markUsed(isP1, row, col);
                    setUsed(isP1 ? p1Tiles[row][col] : p2Tiles[row][col]);

                    buildHeartsBar();
                    updateInfoBar();

                    Stage result = buildQuestionResultStage(
                            q,
                            picked,
                            scoreBefore,
                            livesBefore,
                            score,
                            sharedHearts,
                            ACTIVATION_COST_EASY,
                            !autoMode
                    );

                    if (autoMode) {
                        showStage(result, QUESTION_RESULT_DELAY);
                    } else {
                        showStage(result, null);
                    }
                }
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

        Runnable showDialog = () -> {
            Stage st = buildSurpriseStage(
                    ownerWindow(),
                    scoreBefore, livesBefore,
                    DEMO_SURPRISE_SCORE, DEMO_SURPRISE_LIVES,
                    ACTIVATION_COST_EASY,
                    !isAutoPlaying
            );
            if (isAutoPlaying) showStage(st, SURPRISE_READ_DELAY);
            else showStage(st, null);
        };

        afterFxPulse(showDialog);
    }

    // interactive = true  -> buttons enabled; user picks (A-D) or closes -> -1
    // interactive = false -> buttons disabled; auto-close and auto-pick correct
 // interactive = true  -> buttons enabled; user picks (A-D) or closes -> -1 (callback)
 // interactive = false -> buttons disabled; auto-close and auto-pick correct (callback)
 private void showQuestionPickDialog(
         model.Question q,
         boolean tutorialMode,
         boolean interactive,
         Double autoCloseSeconds,
         IntConsumer onPick
 ) {
     Objects.requireNonNull(q, "question must not be null");
     Objects.requireNonNull(onPick, "onPick must not be null");

     // Always create/modify/show stage on FX thread
     Platform.runLater(() -> {
         Stage stage = new Stage();
         stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

         Window owner = ownerWindow();
         if (owner != null) stage.initOwner(owner);

         stage.setTitle("Question");
         stage.setResizable(false);

         VBox root = new VBox(12);
         root.setPadding(new Insets(18));

         String bgUrl = resolveBgUrl();
         root.setStyle(bgStyle(bgUrl));

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

         VBox answersBox = new VBox(10, btnA, btnB, btnC, btnD);
         root.getChildren().addAll(title, questionText, hint, answersBox);

         // Single-shot result delivery (prevents double-callback bugs)
         final boolean[] delivered = { false };
         Runnable deliverMinusOne = () -> {
             if (delivered[0]) return;
             delivered[0] = true;
             onPick.accept(-1);
         };

         // Close handler -> treat as "no pick"
         stage.setOnCloseRequest(e -> deliverMinusOne.run());
         stage.setOnHidden(e -> deliverMinusOne.run()); // covers programmatic close too

         if (interactive) {
             btnA.setOnAction(e -> { if (!delivered[0]) { delivered[0] = true; stage.close(); onPick.accept(0); } });
             btnB.setOnAction(e -> { if (!delivered[0]) { delivered[0] = true; stage.close(); onPick.accept(1); } });
             btnC.setOnAction(e -> { if (!delivered[0]) { delivered[0] = true; stage.close(); onPick.accept(2); } });
             btnD.setOnAction(e -> { if (!delivered[0]) { delivered[0] = true; stage.close(); onPick.accept(3); } });

             Scene scene = new Scene(root, 380, 350);
             stage.setScene(scene);
             stage.centerOnScreen();
             stage.show(); // NOT showAndWait
             return;
         }

         // auto mode: disable and close automatically
         btnA.setDisable(true);
         btnB.setDisable(true);
         btnC.setDisable(true);
         btnD.setDisable(true);

         Label closing = new Label("Closing automatically...");
         closing.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13px;");
         root.getChildren().add(closing);

         Scene scene = new Scene(root, 380, 365);
         stage.setScene(scene);
         stage.centerOnScreen();
         stage.show();

         double seconds = (autoCloseSeconds == null) ? QUESTION_READ_DELAY : autoCloseSeconds;
         PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
         pt.setOnFinished(e -> {
             if (delivered[0]) return;
             delivered[0] = true;
             stage.close();
             onPick.accept(correctIdx);
         });
         pt.play();
     });
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

        Tooltip tp = new Tooltip(text);
        tp.setWrapText(true);
        tp.setMaxWidth(420);
        b.setTooltip(tp);

        return b;
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
        stage.setResizable(false);

        AnchorPane root = new AnchorPane();
        root.setStyle(bgStyle(resolveBgUrl()));

        boolean isCorrectChosen = (chosenIdx == correctIdx);

        Label header = new Label(
                "YOU ANSWERED AN " + diff.toUpperCase() + " QUESTION.\n\n" +
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
                        "Activation cost: -" + activationCost + " points (since we're in easy mode)"
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

        Scene scene = new Scene(root, 580, 450);
        scene.getStylesheets().add(
                HowToPlayController.class.getResource("/css/theme.css").toExternalForm()
        );

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
            if (e.getCode() == KeyCode.ENTER && showOkButton) stage.close();
        });

        stage.setScene(scene);
        stage.centerOnScreen();
        return stage;
    }

    private Node makeOutcomeLine(String letter, int idx, int correctIdx, int chosenIdx, String diff, int activationCost) {
        boolean wouldBeCorrect = (idx == correctIdx);
        String d = (diff == null) ? "easy" : diff.toLowerCase();

        // Special case: EASY/MEDIUM wrong has OR (50%)
        if (!wouldBeCorrect && (d.equals("easy") || d.equals("medium"))) {
            int p = d.equals("easy") ? 3 : 6;

            int opt1 = -activationCost - p;
            int opt2 = -activationCost;

            String text = letter + ") WRONG  →  Score (" + opt1 + " OR " + opt2 + "), Lives +0"
                    + "   (50% chance: -" + p + " OR 0, -" + activationCost + " activation)";

            Label lbl = baseOutcomeLabel(text);

            String border = "rgba(255, 90, 90, 0.70)";
            String chosenExtra = (idx == chosenIdx)
                    ? " -fx-border-width: 2.6; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.18);"
                    : "";

            lbl.setStyle(lbl.getStyle() + " -fx-border-color: " + border + ";" + chosenExtra);
            return lbl;
        }

        Outcome o = wouldBeCorrect ? computeOutcome(d, true) : computeOutcome(d, false);

        int netScore = o.scoreDelta - activationCost;
        int netLives = o.livesDelta;

        String text = letter + ") " +
                (wouldBeCorrect ? "CORRECT" : "WRONG") +
                "  →  Score " + (netScore >= 0 ? "+" : "") + netScore +
                " , Lives " + (netLives >= 0 ? "+" : "") + netLives +
                "   (" + (wouldBeCorrect ? "+" : "") + o.scoreDelta +
                " score, " + (o.livesDelta >= 0 ? "+" : "") + o.livesDelta +
                " lives, -" + activationCost + " activation)";

        Label lbl = baseOutcomeLabel(text);

        String border = (idx == correctIdx)
                ? "rgba(70, 255, 140, 0.85)"
                : "rgba(255, 90, 90, 0.70)";

        String chosenExtra = (idx == chosenIdx)
                ? " -fx-border-width: 2.6; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.18);"
                : "";

        lbl.setStyle(lbl.getStyle() + " -fx-border-color: " + border + ";" + chosenExtra);
        return lbl;
    }

    private Label baseOutcomeLabel(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("""
            -fx-padding: 10 12 10 12;
            -fx-background-color: rgba(255,255,255,0.12);
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-border-width: 1;
            -fx-text-fill: white;
            -fx-font-size: 14px;
        """);
        return lbl;
    }


    private Stage buildSurpriseStage(
            Window owner,
            int scoreBefore,
            int livesBefore,
            int surprisePoints,
            int surpriseLives,
            int activationCost,
            boolean showOkButton
    ) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Surprise Result");
        stage.setResizable(false);

        AnchorPane pane = new AnchorPane();
        pane.setStyle(bgStyle(resolveBgUrl()));

        Label header = new Label("SURPRISE TUTORIAL RESULT");
        header.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 22px;
            -fx-font-family: 'Copperplate Gothic Bold';
        """);

        int scoreAfterGood = scoreBefore - activationCost + surprisePoints;
        int livesAfterGood = clamp(livesBefore + surpriseLives, 0, TOTAL_HEART_SLOTS);

        int scoreAfterBad = scoreBefore - activationCost - surprisePoints;
        int livesAfterBad = clamp(livesBefore - surpriseLives, 0, TOTAL_HEART_SLOTS);

        Label subtitle = new Label("Activation cost is always applied first.");
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 14px;");

        Label goodLine = new Label(
                "GOOD outcome:  Score " + scoreBefore + " → " + scoreAfterGood +
                        " , Lives " + livesBefore + "/10 → " + livesAfterGood + "/10"
        );
        goodLine.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 15px;
            -fx-background-color: rgba(70, 255, 140, 0.12);
            -fx-border-color: rgba(70, 255, 140, 0.85);
            -fx-border-width: 2;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-padding: 10 12 10 12;
        """);

        Label badLine = new Label(
                "BAD outcome:   Score " + scoreBefore + " → " + scoreAfterBad +
                        " , Lives " + livesBefore + "/10 → " + livesAfterBad + "/10"
        );
        badLine.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 15px;
            -fx-background-color: rgba(255, 90, 90, 0.10);
            -fx-border-color: rgba(255, 90, 90, 0.80);
            -fx-border-width: 2;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-padding: 10 12 10 12;
        """);

        Label costLine = new Label("Activation cost: -" + activationCost + " score");
        costLine.setStyle("-fx-text-fill: rgba(255,255,255,0.90); -fx-font-size: 14px;");

        VBox content = new VBox(14, header, subtitle, costLine, goodLine, badLine);
        content.setPadding(new Insets(26));

        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        pane.getChildren().add(content);

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
            pane.getChildren().add(ok);
        } else {
            Label closing = new Label("Closing automatically...");
            closing.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 13px;");
            AnchorPane.setBottomAnchor(closing, 24.0);
            AnchorPane.setRightAnchor(closing, 26.0);
            pane.getChildren().add(closing);
        }

        Scene scene = new Scene(pane, 420, 320);
        scene.getStylesheets().add(
                HowToPlayController.class.getResource("/css/theme.css").toExternalForm()
        );

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
            if (e.getCode() == KeyCode.ENTER && showOkButton) stage.close();
        });

        stage.setScene(scene);
        stage.centerOnScreen();
        return stage;
    }

    private Window ownerWindow() {
        if (backBtn != null && backBtn.getScene() != null) return backBtn.getScene().getWindow();
        if (nextBtn != null && nextBtn.getScene() != null) return nextBtn.getScene().getWindow();
        return null;
    }

    private void showStage(Stage stage, Double autoCloseSeconds) {
        if (stage == null) return;

        Runnable doShow = () -> {
            // Manual mode (blocking)
            if (autoCloseSeconds == null) {
                stage.showAndWait();
                return;
            }

            // Auto mode (non-blocking)
            stage.show();

            PauseTransition pt = new PauseTransition(Duration.seconds(autoCloseSeconds));
            pt.setOnFinished(e -> { if (stage.isShowing()) stage.close(); });
            pt.play();
        };

        // Always defer: avoids "not allowed during animation/layout processing"
        Platform.runLater(doShow);
    }

    private String resolveBgUrl() {
        var url = HowToPlayController.class.getResource(BG_PRIMARY);
        if (url == null) url = HowToPlayController.class.getResource(BG_FALLBACK);
        if (url == null) {
            // last resort: no background
            return "";
        }
        return url.toExternalForm();
    }

    private String bgStyle(String bgUrl) {
        if (bgUrl == null || bgUrl.isBlank()) return "";
        return """
            -fx-background-image: url('%s');
            -fx-background-repeat: no-repeat;
            -fx-background-position: center center;
            -fx-background-size: cover;
        """.formatted(bgUrl);
    }
}
