package control;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import model.GameConfig;
import model.SysData;
import util.DialogUtil;
import util.SoundManager;
import util.UIAnimations;

public class GameController {

    @FXML private GridPane player1Grid;
    @FXML private GridPane player2Grid;
    @FXML private Label player1BombsLeftLabel;
    @FXML private Label player2BombsLeftLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label timeLabel;
    @FXML private Label scoreLabel;
    @FXML private HBox heartsBox;
    @FXML private Button pauseBtn;
    @FXML private Button soundButton;
    @FXML private Button musicButton;

    @FXML private Parent root;
    @FXML private ImageView player1AvatarImage;
    @FXML private ImageView player2AvatarImage;

    private final GameStateController state = new GameStateController();

    private GameHistoryServiceController historyService;
    private GamePlayServiceController playService;
    private GameBonusServiceController bonusService;
    private GameUIServiceController uiService;

    /**
     * Initializes the game session using the given configuration.
     * Called from the previous screen.
     */
    public void init(GameConfig config) {
        // init state
        state.config = config;
        state.difficulty = config.getDifficulty();

        // create services in correct order (due to dependencies)
        historyService = new GameHistoryServiceController();
        uiService = new GameUIServiceController(state,
                player1Grid, player2Grid,
                player1BombsLeftLabel, player2BombsLeftLabel,
                difficultyLabel, timeLabel, scoreLabel,
                heartsBox, pauseBtn, soundButton, musicButton,
                root, player1AvatarImage, player2AvatarImage);

        playService = new GamePlayServiceController(state, uiService, historyService, this::showEndGameScreen);
        bonusService = new GameBonusServiceController(state, uiService, playService);

        // wire cross-deps
        uiService.setBonusService(bonusService);
        uiService.setPlayService(playService);

        // setup boards + counters (same as your init)
        playService.initializeNewMatch();

        // Apply generic animations to buttons/cards (same as your init)
        UIAnimations.applyHoverZoomToAllButtons(root);
        UIAnimations.applyFloatingToCards(root);

        // Build UI
        uiService.loadAvatars();
        uiService.buildHeartsBar();
        uiService.initLabels();
        uiService.buildGrids();

        uiService.initForbiddenCursor();
        uiService.applyTurnStateToBoards();

        // TIMER SETTINGS (same behavior)
        state.elapsedSeconds = 0;
        if (SysData.isTimerEnabled()) {
            uiService.updateTimeLabel();
            uiService.startTimer();
        } else {
            if (timeLabel != null) timeLabel.setText("Timer: OFF");
            state.timer = null;
        }

        // SYNC ICONS WITH SETTINGS
        uiService.refreshSoundIconFromSettings();
        uiService.refreshMusicIconFromSettings();

        // start/reset idle smart-hint timer
        bonusService.resetIdleHintTimer();
    }

    public void setOfficialPlayerNames(String player1OfficialName, String player2OfficialName) {
        state.player1OfficialName = player1OfficialName;
        state.player2OfficialName = player2OfficialName;
    }

    @FXML
    private void onExitBtnClicked() {
        bonusService.resetIdleHintTimer();
        historyService.saveGiveUpGame(state);
        uiService.stopTimer();
        System.exit(0);
    }

    @FXML
    private void onHelpBtnClicked() {
        bonusService.resetIdleHintTimer();
        System.out.println("Help clicked but screen not created yet!");
    }

    @FXML
    private void onBackBtnClicked() throws IOException {
        bonusService.resetIdleHintTimer();
        historyService.saveGiveUpGame(state);
        uiService.stopTimer();

        Stage stage = (Stage) player1Grid.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setStage(stage);

        stage.setScene(new Scene(root));
        stage.show();
        stage.centerOnScreen();
    }

    @FXML
    private void onPauseGame() {
        bonusService.resetIdleHintTimer();
        playService.togglePause();
    }

    @FXML
    private void onSoundOff() {
        bonusService.resetIdleHintTimer();
        boolean newState = !SysData.isSoundEnabled();
        SysData.setSoundEnabled(newState);
        uiService.refreshSoundIconFromSettings();
    }

    @FXML
    private void onMusicToggle() {
        bonusService.resetIdleHintTimer();
        SoundManager.toggleMusic();

        boolean musicOn = SoundManager.isMusicOn();
        SysData.setMusicEnabled(musicOn);

        uiService.refreshMusicIconFromSettings();
    }

    @FXML
    private void onMainMenu() {
        bonusService.resetIdleHintTimer();
    }

    @FXML
    private void showEndGameScreen() {
        // This method body stays identical to your original logic, just moved here.
        try {
            String fxmlPath = state.gameWon ? "/view/win_view.fxml" : "/view/lose_view.fxml";

            Stage stage = (Stage) player1Grid.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent endRoot = loader.load();

            EndGameController controller = loader.getController();
            controller.init(
                    stage,
                    state.config,
                    state.score,
                    state.elapsedSeconds,
                    state.sharedHearts,
                    state.gameWon
            );

            Scene endScene = new Scene(endRoot, 700, 450);
            stage.setScene(endScene);
            stage.centerOnScreen();
            stage.show();

            Platform.runLater(playService::showHeartsBonusPopupIfNeeded);

        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.show(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to load end-game screen.", e.getMessage());
        }
    }
}
