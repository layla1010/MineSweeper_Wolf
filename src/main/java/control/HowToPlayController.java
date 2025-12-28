package control;

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
import javafx.scene.control.Tooltip;
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

	@FXML
	private Parent root;

	@FXML
	private HBox heartsBox;
	@FXML
	private Label scoreLabel;

	@FXML
	private Rectangle p1NbrHL;
	@FXML
	private Rectangle p2NbrHL;

	@FXML
	private GridPane p1Grid;
	@FXML
	private GridPane p2Grid;

	@FXML
	private StackPane p1BoardLayer;
	@FXML
	private StackPane p2BoardLayer;

	@FXML
	private Rectangle p1RowHL;
	@FXML
	private Rectangle p1ColHL;
	@FXML
	private Rectangle p1CellHL;

	@FXML
	private Rectangle p2RowHL;
	@FXML
	private Rectangle p2ColHL;
	@FXML
	private Rectangle p2CellHL;

	@FXML
	private Label stepTitle;
	@FXML
	private Label stepBody;
	@FXML
	private Label stepCounter;

	@FXML
	private Button prevBtn;
	@FXML
	private Button nextBtn;

	@FXML
	private Button backBtn;
	@FXML
	private Button restartBtn;

	@FXML
	private Label p1InfoLabel;
	@FXML
	private Label p2InfoLabel;
	@FXML
	private Label turnLabel;

	private StackPane[][] p1Tiles;
	private StackPane[][] p2Tiles;

	@FXML
	private Button playBtn;
	@FXML
	private Button stopBtn;

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

		resetDemoState(); // instead of updateInfoBar()

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
		runStep(0);
	}

	@FXML
	private void onPlay() {
		if (isAutoPlaying)
			return;

		isAutoPlaying = true;
		playBtn.setDisable(true);
		stopBtn.setDisable(false);

		// advance every 2.5 seconds (tune)
		autoPlay = new Timeline(new KeyFrame(Duration.seconds(2.5), e -> {
			if (stepIndex < steps.size() - 1) {
				runStep(stepIndex + 1);
			} else {
				onStop(); // stop at end
			}
		}));
		autoPlay.setCycleCount(Timeline.INDEFINITE);
		autoPlay.play();
	}

	private void stopAutoplayIfRunning() {
		if (isAutoPlaying)
			onStop();
	}

	@FXML
	private void onStop() {
		isAutoPlaying = false;
		if (autoPlay != null)
			autoPlay.stop();
		autoPlay = null;

		playBtn.setDisable(false);
		stopBtn.setDisable(true);
	}

	@FXML
	private void onPrev() {
		stopAutoplayIfRunning();
		if (stepIndex > 0)
			runStep(stepIndex - 1);
	}

	@FXML
	private void onNext() {
		stopAutoplayIfRunning();
		if (stepIndex < steps.size() - 1)
			runStep(stepIndex + 1);
	}

	// ----------------- Steps -----------------

	private void buildSteps() {
		steps.clear();

		steps.add(new TourStep(
			    "\nWelcome",
			    "This page demonstrates game rules using a scripted demo board. Use Next/Prev to move between steps.",
			    () -> {
			        resetScenario();
			        hideAllHighlights();
			        setSnapshot(10,40,6,2, 10,40,6,2, 10, true);
			    }
			));


		steps.add(new TourStep("\nTurns and active board",
				"Only the glowing board is active. Here, Player 1 starts. The other board is dimmed.", () -> {
					resetScenario();
					setTurn(true);
					hideAllHighlights();
					// highlight whole board (cell highlight on a central cell to draw attention)
					highlightCell(true, 4, 4);
				}));

		steps.add(new TourStep("\nRevealed numbers",
				"A number tells how many mines are around that cell (in its 8 neighbors). Example: this is a '2'.",
				() -> {
					resetScenario();
					setTurn(true);

					setRevealedNumber(p1Tiles[4][4], 2);
					highlightCell(true, 4, 4);
				}));

		steps.add(new TourStep("Neighbors (8 surrounding cells)",
				"A revealed number counts mines in the 8 surrounding cells (including diagonals). The highlighted area shows the neighborhood to reason about.",
				() -> {
					resetScenario();
					setTurn(true);
					setRevealedNumber(p1Tiles[4][4], 2);
					highlightNeighborhood(true, 4, 4);
					highlightCell(true, 4, 4);
				}));

		steps.add(new TourStep("Flagging a suspected mine",
			    "You can mark a suspected mine with a flag. Flags help you avoid clicking mines.",
			    () -> {
			        resetScenario();
			        hideAllHighlights();
			        setSnapshot(10, 39, 6, 2,   10, 40, 6, 2,   10, true); 

			        setRevealedNumber(p1Tiles[4][4], 2);
			        setFlag(p1Tiles[3][3]);
			        highlightCell(true, 3, 3);
			    }
			));


		steps.add(new TourStep("Mines are dangerous",
				"A mine ends progress and costs hearts. Do not reveal mines—flag them instead.", () -> {
					resetScenario();
					setTurn(true);

					setMine(p1Tiles[3][3]); // show mine
					highlightCell(true, 3, 3);
					setSnapshot(9,39,6,2, 10,40,6,2, 10, true);
				}));

		steps.add(new TourStep("Question cells",
				"Question cells are special: they require an action/answer depending on your implementation rules.",
				() -> {
					resetScenario();
					setTurn(true);

					setQuestion(p1Tiles[2][6]);
					highlightCell(true, 2, 6);
					setSnapshot(9,39,5,2, 10,40,6,2, 10, true);
				}));

		steps.add(new TourStep("Surprise cells",
				"Surprise cells can trigger random bonuses/penalties depending on your implementation.", () -> {
					resetScenario();
					setTurn(true);

					setSurprise(p1Tiles[6][2]);
					highlightCell(true, 6, 2);
					setSnapshot(9,39,5,1, 10,40,6,2, 9, true);
				}));

		steps.add(new TourStep("Switching turns",
				"After an action, the turn switches to the other player. Now Player 2 becomes active.", () -> {
					resetScenario();
					setTurn(false);
					hideAllHighlights();
					highlightCell(false, 4, 4);
				}));

		steps.add(new TourStep("Player 2 also follows the same rules",
				"Numbers, flags, mines, and special cells behave the same. This is a demo example on Player 2 board.",
				() -> {
					resetScenario();
					setTurn(false);

					setRevealedNumber(p2Tiles[4][4], 1);
					setFlag(p2Tiles[4][5]);
					highlightNeighborhood(false, 4, 4);
					highlightCell(false, 4, 4);
				}));
	}

	private void runStep(int idx) {
		if (idx < 0 || idx >= steps.size())
			return;
		stepIndex = idx;

		TourStep s = steps.get(idx);
		stepTitle.setText(s.title);
		stepBody.setText(s.body);

		stepCounter.setText((idx + 1) + " / " + steps.size());

		prevBtn.setDisable(idx == 0);
		nextBtn.setDisable(idx == steps.size() - 1);

		// execute step content
		s.action.run();
	}

	// ----------------- Scenario Reset -----------------

	private void resetScenario() {
		// clear both boards
		for (int r = 0; r < SIZE; r++) {
			for (int c = 0; c < SIZE; c++) {
				setHidden(p1Tiles[r][c]);
				setHidden(p2Tiles[r][c]);
			}
		}

		// hide highlight overlays
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

		// Reset other demo numbers if you add them later:
		// score = 0; etc.

		updateInfoBar();
		buildHeartsBar();
	}

	private static final int TOTAL_HEART_SLOTS = 10;

	private void buildHeartsBar() {
		if (heartsBox == null)
			return;

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
		if (!on) {
			r.setOpacity(0.0);
		} else {
			r.setOpacity(1.0);
		}
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
		// padding so it looks like a focus ring
		double pad = 4;

		rect.setX(b.getMinX() - pad);
		rect.setY(b.getMinY() - pad);
		rect.setWidth(b.getWidth() + 2 * pad);
		rect.setHeight(b.getHeight() + 2 * pad);
		rect.setOpacity(1.0);
	}

	// ----------------- Pulse animation -----------------

	private void startPulse() {
		if (pulse != null)
			pulse.stop();

		pulse = new Timeline(new KeyFrame(Duration.ZERO, e -> setPulseOpacity(1.0)),
				new KeyFrame(Duration.seconds(0.55), e -> setPulseOpacity(0.55)),
				new KeyFrame(Duration.seconds(1.10), e -> setPulseOpacity(1.0)));
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
		if (r.isVisible())
			r.setOpacity(v);
	}

	// ----------------- Cell Painting (uses your CSS) -----------------

	private javafx.scene.control.Button btn(StackPane tile) {
		return (javafx.scene.control.Button) tile.getChildren().get(0);
	}

	private void clearCellState(javafx.scene.control.Button b) {
		b.setText("");
		b.setGraphic(null);
		b.setDisable(false);

		b.getStyleClass().removeAll("cell-hidden", "cell-revealed", "cell-flagged", "cell-mine", "cell-number",
				"cell-question", "cell-surprise");
	}

	private void setIcon(javafx.scene.control.Button b, String resourcePath, double size) {
		try {
			Image img = new Image(getClass().getResourceAsStream(resourcePath));
			ImageView iv = new ImageView(img);
			iv.setFitWidth(size);
			iv.setFitHeight(size);
			iv.setPreserveRatio(true);
			b.setGraphic(iv);
		} catch (Exception ex) {
		}
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

	private void setFlag(StackPane tile) {
	    var b = btn(tile);
	    clearCellState(b);

	    b.setText("⚑");
	    b.getStyleClass().add("cell-flagged");
	}


	private void setMine(StackPane tile) {
	    var b = btn(tile);
	    clearCellState(b);

	    b.setText("💣");
	    b.setDisable(true);
	    b.getStyleClass().addAll("cell-revealed", "cell-mine");
	}


	private void setQuestion(StackPane tile) {
	    var b = btn(tile);
	    clearCellState(b);

	    setIcon(b, "/Images/question-mark.png", 20);
	    if (b.getGraphic() == null) b.setText("?");

	    b.getStyleClass().addAll("cell-revealed", "cell-question");
	}


	private void setSurprise(StackPane tile) {
	    var b = btn(tile);
	    clearCellState(b);

	    setIcon(b, "/Images/giftbox.png", 20);
	    if (b.getGraphic() == null) b.setText("★");

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

	private void updateInfoBar() {
		p1InfoLabel.setText("Player 1 | Mines left: " + p1Mines + " | Flags left: " + p1Flags + " | Surprises left: "
				+ p1Surprises + " | Questions left: " + p1Questions);

		p2InfoLabel.setText("Player 2 | Mines left: " + p2Mines + " | Flags left: " + p2Flags + " | Surprises left: "
				+ p2Surprises + " | Questions left: " + p2Questions);

		turnLabel.setText("Turn: " + (isP1Turn ? "Player 1" : "Player 2"));

		if (scoreLabel != null)
			scoreLabel.setText("Score: 0"); // or demoScore if you add it
	}

	// JavaFX BoundingBox helper (since we used it above)
	private static final class BoundingBox extends javafx.geometry.BoundingBox {
		BoundingBox(double minX, double minY, double width, double height) {
			super(minX, minY, width, height);
		}
	}
	
	private void setSnapshot(
	        int p1Mines, int p1Flags, int p1Questions, int p1Surprises,
	        int p2Mines, int p2Flags, int p2Questions, int p2Surprises,
	        int hearts,
	        boolean p1Turn
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

	    setTurn(p1Turn);      // sets isP1Turn + board glow + updateInfoBar()
	    buildHeartsBar();     // reflect hearts snapshot
	}

}