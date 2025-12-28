package control;

import javafx.animation.*;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class TutorialEngine {

    private final DemoBoard b1, b2;
    private final StackPane[][] tilesP1, tilesP2;

    private final Label caption;
    private final Button playPauseBtn;

    private final StackPane demoLayer;
    private final StackPane highlight;

    private final Label turnLabel, p1ScoreLabel, p2ScoreLabel, heartsLabel, giftsLabel, minesLabel, questionsLabel;

    private final List<Runnable> steps = new ArrayList<>();
    private int stepIndex = 0;

    private boolean playing = false;
    private SequentialTransition autoplay;

    // demo state
    private boolean p1Turn = true;
    private int p1Score = 0;
    private int p2Score = 0;
    private int hearts = 3;

    private int giftsLeft = 1;
    private int minesLeft = 5;
    private int questionsLeft = 2;

    public TutorialEngine(
            DemoBoard boardP1,
            DemoBoard boardP2,
            StackPane[][] tilesP1,
            StackPane[][] tilesP2,
            Label captionLabel,
            Button playPauseBtn,
            StackPane demoLayer,
            StackPane highlight,
            Label turnLabel,
            Label p1ScoreLabel,
            Label p2ScoreLabel,
            Label heartsLabel,
            Label giftsLabel,
            Label minesLabel,
            Label questionsLabel
    ) {
        this.b1 = boardP1;
        this.b2 = boardP2;
        this.tilesP1 = tilesP1;
        this.tilesP2 = tilesP2;
        this.caption = captionLabel;
        this.playPauseBtn = playPauseBtn;

        this.demoLayer = demoLayer;
        this.highlight = highlight;

        this.turnLabel = turnLabel;
        this.p1ScoreLabel = p1ScoreLabel;
        this.p2ScoreLabel = p2ScoreLabel;
        this.heartsLabel = heartsLabel;
        this.giftsLabel = giftsLabel;
        this.minesLabel = minesLabel;
        this.questionsLabel = questionsLabel;

        initUIState();
        buildSteps();
    }

    private void initUIState() {
        setHighlightVisible(false);
        setCaption("Welcome! This demo shows how to play — step by step.");
        updateHud();
        setPlayButtonText();
        renderAllHidden();
    }

    private void updateHud() {
        turnLabel.setText("Turn: " + (p1Turn ? "Player 1" : "Player 2"));
        p1ScoreLabel.setText("P1 Score: " + p1Score);
        p2ScoreLabel.setText("P2 Score: " + p2Score);

        heartsLabel.setText("Hearts: " + hearts);
        giftsLabel.setText("Gifts: " + giftsLeft);
        minesLabel.setText("Mines left: " + minesLeft);
        questionsLabel.setText("Questions left: " + questionsLeft);
    }

    private void setCaption(String text) {
        caption.setText(text);
    }

    // =======================
    // Public controls
    // =======================
    public void goToStep(int index) {
        stopAutoplay();
        runStepSilently(index);
    }

    public void next() {
        stopAutoplay();
        goToStep(stepIndex + 1);
    }

    public void prev() {
        stopAutoplay();
        goToStep(stepIndex - 1);
    }

    public void togglePlay() {
        if (playing) stopAutoplay();
        else startAutoplay();
        setPlayButtonText();
    }

    // =======================
    // Autoplay engine
    // =======================
    private void startAutoplay() {
        playing = true;

        if (autoplay != null) autoplay.stop();
        autoplay = new SequentialTransition();

        // run current step now
        runStepSilently(stepIndex);

        for (int i = stepIndex + 1; i < steps.size(); i++) {
            int target = i;
            PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
            pause.setOnFinished(e -> runStepSilently(target));
            autoplay.getChildren().add(pause);
        }

        autoplay.setOnFinished(e -> {
            playing = false;
            setPlayButtonText();
        });

        autoplay.playFromStart();
    }

    private void stopAutoplay() {
        playing = false;
        if (autoplay != null) autoplay.stop();
    }

    private void setPlayButtonText() {
        playPauseBtn.setText(playing ? "Pause" : "Play");
    }

    // =======================
    // Steps definition
    // =======================
    private void buildSteps() {

        steps.add(() -> {
            resetDemo();
            setCaption("Step 1: This is a 2-player match. Each player plays on their own board.");
            focusBoardsOverview();
        });

        steps.add(() -> {
            resetDemo();
            p1Turn = true;
            updateHud();

            setCaption("Step 2: Player 1 clicks a tile. A number appears (adjacent mines). +1 score.");
            animateClick(tilesP1, 1, 1, () -> {
                reveal(tilesP1, b1, 1, 1);
                p1Score += 1;
                updateHud();
                switchTurn();
            });
        });

        steps.add(() -> {
            resetDemo();
            p1Turn = true;
            reveal(tilesP1, b1, 1, 1);
            p1Score = 1;
            switchTurn();

            setCaption("Step 3: Player 2 clicks a mine. One shared heart is lost.");
            animateClick(tilesP2, 1, 2, () -> {
                reveal(tilesP2, b2, 1, 2);
                hearts = Math.max(0, hearts - 1);
                updateHud();
                switchTurn();
            });
        });

        steps.add(() -> {
            resetDemo();
            reveal(tilesP1, b1, 1, 1); p1Score = 1;
            p1Turn = false;
            reveal(tilesP2, b2, 1, 2); hearts = 2;
            switchTurn();

            setCaption("Step 4: Question tiles trigger trivia (in the real game). Here we show the icon. +1 score.");
            animateClick(tilesP1, 2, 2, () -> {
                reveal(tilesP1, b1, 2, 2);
                p1Score += 1;
                questionsLeft = Math.max(0, questionsLeft - 1);
                updateHud();
                switchTurn();
            });
        });

        steps.add(() -> {
            resetDemo();
            reveal(tilesP1, b1, 1, 1); p1Score = 1;
            p1Turn = false;
            reveal(tilesP2, b2, 1, 2); hearts = 2;
            switchTurn();
            reveal(tilesP1, b1, 2, 2); p1Score = 2; questionsLeft = 1;
            switchTurn();

            setCaption("Step 5: Surprise tiles give a bonus effect. Here we show a gift icon. +1 score.");
            animateClick(tilesP2, 3, 3, () -> {
                reveal(tilesP2, b2, 3, 3);
                p2Score += 1;
                giftsLeft = Math.max(0, giftsLeft - 1);
                updateHud();
                switchTurn();
            });
        });

        steps.add(() -> {
            resetDemo();
            setCaption("Step 6: Right-click to place a flag (mark suspicion). In the real game: flags are limited.");
            p1Turn = true;
            updateHud();
            animateRightClickFlag(tilesP1, 0, 0, () -> {
                flag(tilesP1, 0, 0);
                updateHud();
                switchTurn();
            });
        });

        steps.add(() -> {
            resetDemo();
            setHighlightVisible(false);
            setCaption("Done! Use Prev/Next to replay steps, or Play to auto-run the demo.");
            updateHud();
        });
    }

    private void resetDemo() {
        stopAutoplay();
        p1Turn = true;
        p1Score = 0;
        p2Score = 0;
        hearts = 3;
        giftsLeft = 1;
        minesLeft = 5;
        questionsLeft = 2;
        updateHud();
        renderAllHidden();
        setHighlightVisible(false);
    }

    // =======================
    // Rendering / reveal
    // =======================
    private void renderAllHidden() {
        renderHiddenBoard(tilesP1);
        renderHiddenBoard(tilesP2);
    }

    private void renderHiddenBoard(StackPane[][] tiles) {
        for (int r = 0; r < tiles.length; r++) {
            for (int c = 0; c < tiles[0].length; c++) {
                var btn = (javafx.scene.control.Button) tiles[r][c].getChildren().get(0);
                btn.setText("");
                btn.setGraphic(null);

                btn.getStyleClass().removeAll(
                        "cell-revealed","cell-mine","cell-number","cell-empty","cell-question","cell-surprise","cell-flagged"
                );
                if (!btn.getStyleClass().contains("cell-hidden")) btn.getStyleClass().add("cell-hidden");
                btn.setDisable(true);
            }
        }
    }

    private void reveal(StackPane[][] tiles, DemoBoard board, int r, int c) {
        var btn = (javafx.scene.control.Button) tiles[r][c].getChildren().get(0);

        btn.getStyleClass().remove("cell-hidden");
        if (!btn.getStyleClass().contains("cell-revealed")) btn.getStyleClass().add("cell-revealed");

        DemoCellType type = board.cells[r][c];
        btn.getStyleClass().removeAll("cell-mine","cell-number","cell-empty","cell-question","cell-surprise","cell-flagged");
        btn.setGraphic(null);
        btn.setText("");

        switch (type) {
            case NUMBER_1 -> {
                btn.getStyleClass().add("cell-number");
                btn.setText("1");
            }
            case MINE -> {
                btn.getStyleClass().add("cell-mine");
                btn.setText("💣");
                minesLeft = Math.max(0, minesLeft - 1);
            }
            case QUESTION -> {
                btn.getStyleClass().add("cell-question");
                btn.setText("?");
            }
            case SURPRISE -> {
                btn.getStyleClass().add("cell-surprise");
                btn.setText("★");
            }
            case EMPTY -> {
                btn.getStyleClass().add("cell-empty");
                btn.setText("");
            }
            default -> { }
        }
    }

    private void flag(StackPane[][] tiles, int r, int c) {
        var btn = (javafx.scene.control.Button) tiles[r][c].getChildren().get(0);
        btn.getStyleClass().remove("cell-hidden");
        if (!btn.getStyleClass().contains("cell-flagged")) btn.getStyleClass().add("cell-flagged");
        btn.setText("🚩");
    }

    // =======================
    // Turn / focus / overlays
    // =======================
    private void switchTurn() {
        p1Turn = !p1Turn;
        updateHud();
    }

    private void focusBoardsOverview() {
        setHighlightVisible(false);
    }

    private void setHighlightVisible(boolean visible) {
        highlight.setVisible(visible);
    }

    private void moveOverlayToCell(StackPane[][] tiles, int r, int c) {
        StackPane tile = tiles[r][c];

        Bounds tileBoundsScene = tile.localToScene(tile.getBoundsInLocal());
        Bounds layerBoundsScene = demoLayer.localToScene(demoLayer.getBoundsInLocal());

        double x = tileBoundsScene.getMinX() - layerBoundsScene.getMinX();
        double y = tileBoundsScene.getMinY() - layerBoundsScene.getMinY();
        double w = tileBoundsScene.getWidth();
        double h = tileBoundsScene.getHeight();

        highlight.setVisible(true);
        highlight.setManaged(false);
        highlight.setPrefSize(w, h);
        highlight.relocate(x, y);
    }

    // =======================
    // Animations (click feel)
    // =======================
    private void animateClick(StackPane[][] tiles, int r, int c, Runnable onReveal) {
        moveOverlayToCell(tiles, r, c);

        ScaleTransition st = new ScaleTransition(Duration.millis(140), highlight);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.96);
        st.setToY(0.96);
        st.setAutoReverse(true);
        st.setCycleCount(2);

        PauseTransition pause = new PauseTransition(Duration.millis(50));
        pause.setOnFinished(e -> { if (onReveal != null) onReveal.run(); });

        new SequentialTransition(st, pause).play();
    }

    private void animateRightClickFlag(StackPane[][] tiles, int r, int c, Runnable onFlag) {
        moveOverlayToCell(tiles, r, c);

        FadeTransition ft = new FadeTransition(Duration.millis(180), highlight);
        ft.setFromValue(1);
        ft.setToValue(0.35);
        ft.setAutoReverse(true);
        ft.setCycleCount(4);

        PauseTransition pause = new PauseTransition(Duration.millis(40));
        pause.setOnFinished(e -> { if (onFlag != null) onFlag.run(); });

        new SequentialTransition(ft, pause).play();
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void runStepSilently(int index) {
        this.stepIndex = clamp(index, 0, steps.size() - 1);
        steps.get(this.stepIndex).run();
        setPlayButtonText();
    }
}
