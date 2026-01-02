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
import java.util.ArrayList;
import java.util.List;

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

    @FXML
    private void initialize() {
        p1Tiles = builder.build(p1Grid, SIZE, SIZE, true);
        p2Tiles = builder.build(p2Grid, SIZE, SIZE, false);

        stopBtn.setDisable(true);

        resetDemoState();
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
        resetScenario();
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
                        "Reveal / Activate ends the turn. Flags do NOT end the turn.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 40, 6, 2, 10, true, 0);
                    highlightCell(true, 4, 4);
                }
        ));

        // STEP 2: P1 reveal empty -> cascade (ONE action, ends turn)
        steps.add(new TourStep(
                "Player 1: Reveal Empty (Cascade = One Action)",
                "Player 1 reveals one empty safe cell. The board opens adjacent empty cells automatically (cascade). " +
                        "Even though multiple cells open, it still counts as ONE action and ends the turn.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 40, 6, 2, 10, true, 0);

                    setEmptyRevealed(p1Tiles[4][4]);
                    setEmptyRevealed(p1Tiles[4][5]);
                    setEmptyRevealed(p1Tiles[5][4]);
                    setEmptyRevealed(p1Tiles[5][5]);

                    setRevealedNumber(p1Tiles[3][4], 1);
                    setRevealedNumber(p1Tiles[3][5], 2);
                    setRevealedNumber(p1Tiles[6][4], 1);
                    setRevealedNumber(p1Tiles[6][5], 1);

                    highlightArea(true, 3, 4, 6, 5);
                }
        ));

        // STEP 3: switch turn to P2
        steps.add(new TourStep(
                "Turn Switch",
                "After the reveal action, the turn switches to Player 2.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 40, 6, 2, 10, false, 0);
                    highlightCell(false, 4, 4);
                }
        ));

        // STEP 4: P2 places a flag (does NOT end turn)
        steps.add(new TourStep(
                "Player 2: Flag (Does NOT End Turn)",
                "Player 2 places a flag. Flagging does NOT end the turn, so Player 2 can place more flags.",
                () -> {
                    hideAllHighlights();

                    // show example: flags decrease by 1, score +1 (if correct)
                    // IMPORTANT: turn stays Player 2
                    setSnapshot(10, 40, 6, 2, 10, 39, 6, 2, 10, false, 1);

                    setFlag(p2Tiles[4][4]);
                    highlightCell(false, 4, 4);
                }
        ));

        // STEP 5: P2 places another flag (still same turn)
        steps.add(new TourStep(
                "Player 2: Another Flag (Still Same Turn)",
                "Player 2 places another flag in the same turn. The turn still does NOT end.",
                () -> {
                    hideAllHighlights();

                    // another correct flag example: flags -1 again, score +1 again (illustration)
                    // turn remains Player 2
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 2);

                    setFlag(p2Tiles[4][5]);
                    highlightCell(false, 4, 5);
                }
        ));

        // STEP 6: P2 reveal number (ends turn)
        steps.add(new TourStep(
                "Player 2: Reveal (Ends Turn)",
                "Player 2 reveals a cell. Reveal ends the turn immediately.",
                () -> {
                    hideAllHighlights();

                    // still Player 2 during this action
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 2);

                    setRevealedNumber(p2Tiles[5][5], 2);
                    highlightCell(false, 5, 5);
                    highlightNeighborhood(false, 5, 5);
                }
        ));

        // STEP 7: switch to P1
        steps.add(new TourStep(
                "Turn Switch",
                "After the reveal action, the turn switches to Player 1.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, 2);
                    highlightCell(true, 2, 6);
                }
        ));

        // STEP 8: P1 REVEALS Question cell (NO activation, ends turn)
        steps.add(new TourStep(
                "Player 1: Reveal Question (Ends Turn)",
                "Player 1 reveals a Question cell. Reveal is ONE action and ends the turn. " +
                        "Activation happens on a later turn (a different action).",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, 2);

                    // reveal only
                    setQuestion(p1Tiles[2][6]);
                    highlightCell(true, 2, 6);
                }
        ));

        // STEP 9: switch to P2 (because reveal ended)
        steps.add(new TourStep(
                "Turn Switch",
                "After Player 1 revealed the Question cell, the turn switches to Player 2.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 2);
                    highlightCell(false, 2, 2);
                }
        ));

        // STEP 10: P2 REVEALS Surprise cell (NO activation/result, ends turn)
        steps.add(new TourStep(
                "Player 2: Reveal Surprise (Ends Turn)",
                "Player 2 reveals a Surprise cell. Reveal ends the turn. " +
                        "Activation + result happen on a later turn (different action).",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 2);

                    // reveal only
                    setSurprise(p2Tiles[2][2]);
                    highlightCell(false, 2, 2);
                }
        ));

        // STEP 11: switch to P1
        steps.add(new TourStep(
                "Turn Switch",
                "After Player 2 revealed the Surprise cell, the turn switches to Player 1.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, 2);
                    highlightCell(true, 2, 6);
                }
        ));

        // STEP 12: P1 ACTIVATES Question (separate action, ends turn)
        steps.add(new TourStep(
                "Player 1: Activate Question (Ends Turn)",
                "Player 1 activates the already revealed Question cell. Activation is a DIFFERENT action than reveal, " +
                        "costs points, shows the question dialog, and ends the turn.",
                () -> {
                    hideAllHighlights();

                    // activation cost example
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, true, -3);

                    // keep cell as question (already revealed)
                    setQuestion(p1Tiles[2][6]);
                    highlightCell(true, 2, 6);

                    Platform.runLater(() -> {
                        var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Question");
                        alert.setHeaderText("Question (Demo)");
                        alert.setContentText(
                                "This is the activation step.\n" +
                                        "Activation is separate from reveal.\n" +
                                        "Activation ends the turn."
                        );
                        alert.showAndWait();
                    });
                }
        ));

        // STEP 13: switch to P2
        steps.add(new TourStep(
                "Turn Switch",
                "After Player 1 activated the Question, the turn switches to Player 2.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, -3);
                    highlightCell(false, 2, 2);
                }
        ));

        // STEP 14: P2 ACTIVATES Surprise (separate action, ends turn)
        steps.add(new TourStep(
                "Player 2: Activate Surprise (Ends Turn)",
                "Player 2 activates the already revealed Surprise cell. Activation is separate from reveal, costs points, " +
                        "shows GOOD/BAD result, and ends the turn.",
                () -> {
                    hideAllHighlights();

                    // activation cost (-5) + example GOOD (+8) => net +3 (illustration)
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 10, false, 0);

                    setSurprise(p2Tiles[2][2]);
                    highlightCell(false, 2, 2);

                    Platform.runLater(() -> {
                        var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Surprise Result");
                        alert.setHeaderText("Surprise (Demo)");
                        alert.setContentText(
                                "This is the activation step.\n" +
                                        "Activation is separate from reveal.\n" +
                                        "Activation ends the turn."
                        );
                        alert.showAndWait();
                    });
                }
        ));

        // STEP 15: P1 hits a mine (reveal ends turn)
        steps.add(new TourStep(
                "Player 1: Mine Hit (Ends Turn)",
                "Player 1 reveals a mine. Shared hearts decrease immediately. Reveal ends the turn.",
                () -> {
                    hideAllHighlights();

                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 9, true, 0);

                    setMine(p1Tiles[3][3]);
                    highlightCell(true, 3, 3);
                }
        ));

        // STEP 16: wrap-up
        steps.add(new TourStep(
                "Key Rules Summary",
                "Reveal ends the turn. Activate (Question/Surprise) ends the turn. Flags do NOT end the turn.",
                () -> {
                    hideAllHighlights();
                    setSnapshot(10, 40, 6, 2, 10, 38, 6, 2, 9, false, 0);
                    highlightCell(false, 4, 4);
                }
        ));
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
    }

    private static final int TOTAL_HEART_SLOTS = 10;

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

    private javafx.scene.control.Button btn(StackPane tile) {
        return (javafx.scene.control.Button) tile.getChildren().get(0);
    }

    private void clearCellState(javafx.scene.control.Button b) {
        b.setText("");
        b.setGraphic(null);
        b.setDisable(false);

        b.getStyleClass().removeAll(
                "cell-hidden", "cell-revealed", "cell-flagged", "cell-mine",
                "cell-number", "cell-question", "cell-surprise"
        );
    }

    private void setIcon(javafx.scene.control.Button b, String resourcePath, double size) {
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
