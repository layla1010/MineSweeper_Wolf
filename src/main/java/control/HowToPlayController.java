package control;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HowToPlayController {

    @FXML private GridPane demoGridP1;
    @FXML private GridPane demoGridP2;

    @FXML private StackPane demoLayer;
    @FXML private StackPane highlight;

    @FXML private Label captionLabel;

    @FXML private Label turnLabel;
    @FXML private Label p1ScoreLabel;
    @FXML private Label p2ScoreLabel;
    @FXML private Label heartsLabel;
    @FXML private Label giftsLabel;
    @FXML private Label minesLabel;
    @FXML private Label questionsLabel;

    @FXML private Button backBtn;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Button playPauseBtn;

    private StackPane[][] tilesP1;
    private StackPane[][] tilesP2;
    private TutorialEngine engine;

    @FXML
    private void initialize() {
        int rows = 6, cols = 6;

        HowToPlayBoardBuilder builder = new HowToPlayBoardBuilder();
        tilesP1 = builder.build(demoGridP1, rows, cols, true);
        tilesP2 = builder.build(demoGridP2, rows, cols, false);

        DemoBoard boardP1 = new DemoBoard(rows, cols);
        DemoBoard boardP2 = new DemoBoard(rows, cols);

        engine = new TutorialEngine(
                boardP1, boardP2,
                tilesP1, tilesP2,
                captionLabel, playPauseBtn,
                demoLayer, highlight,
                turnLabel, p1ScoreLabel, p2ScoreLabel,
                heartsLabel, giftsLabel, minesLabel, questionsLabel
        );

        // run step 0 only after window is shown (bounds are correct)
        demoGridP1.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o, ow, nw) -> {
                    if (nw != null) Platform.runLater(() -> engine.goToStep(0));
                });
            }
        });
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) demoGridP1.getScene().getWindow();
        stage.close();
    }

    @FXML private void onPlayPause() { if (engine != null) engine.togglePlay(); }
    @FXML private void onNext() { if (engine != null) engine.next(); }
    @FXML private void onPrev() { if (engine != null) engine.prev(); }
}
