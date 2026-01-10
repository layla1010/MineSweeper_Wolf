package control;

import java.io.IOException;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import model.GameConfig;
import model.SysData;
import util.DialogUtil;
import util.OnboardingManager;
import util.OnboardingPolicy;
import util.OnboardingStep;
import util.SoundManager;
import util.SessionManager;
import util.UIAnimations;
import util.ViewNavigator;

public class GameController {

    @FXML private GridPane player1Grid;
    @FXML private GridPane player2Grid;
    @FXML private Label difficultyLabel;
    @FXML private Label timeLabel;
    @FXML private Label scoreLabel;
    @FXML private HBox heartsBox;
    @FXML private Button pauseBtn;
    @FXML private Button soundButton;
    @FXML private Button musicButton;
    @FXML private HBox player1StatsBox;
    @FXML private HBox player2StatsBox;
    @FXML private Parent root;
    @FXML private ImageView player1AvatarImage;
    @FXML private ImageView player2AvatarImage;
    @FXML private StackPane rootStack;
    private Parent howToPlayOverlay;  

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
        
        UIAnimations.fadeIn(root);

        // create services in correct order (due to dependencies)
        historyService = new GameHistoryServiceController();
        uiService = new GameUIServiceController(state,
                player1Grid, player2Grid,
                player1StatsBox, player2StatsBox,
                difficultyLabel, timeLabel, scoreLabel,
                heartsBox, pauseBtn, soundButton, musicButton,
                root, player1AvatarImage, player2AvatarImage);
        uiService.registerAsObserver();

        playService = new GamePlayServiceController(state, uiService, historyService, this::showEndGameScreen);
        bonusService = new GameBonusServiceController(state, uiService, playService);

        // wire cross-deps
        uiService.setBonusService(bonusService);
        uiService.setPlayService(playService);

        // setup boards + counters 
        playService.initializeNewMatch();

        // Apply generic animations to buttons/cards
        UIAnimations.applyHoverZoomToAllButtons(root);
        UIAnimations.applyFloatingToCards(root);

        // Build UI
        uiService.loadAvatars();
        uiService.buildHeartsBar();
        uiService.initLabels();
        uiService.buildGrids();

        uiService.initForbiddenCursor();
        uiService.applyTurnStateToBoards();

        // TIMER SETTINGS 
        state.elapsedSeconds = 0;
        if (SysData.isTimerEnabled()) {
            uiService.updateTimeLabel();
            uiService.stopTimer();
        } else {
            if (timeLabel != null) timeLabel.setText("Timer: OFF");
            state.timer = null;
        }
        
        Runnable afterOnboardingClose = () -> {
            // Start timer only if enabled
            if (SysData.isTimerEnabled()) {
                uiService.startTimer();
            }
            // start/reset idle smart-hint timer
            bonusService.resetIdleHintTimer();
        };

        // SYNC ICONS WITH SETTINGS
        uiService.refreshSoundIconFromSettings();
        uiService.refreshMusicIconFromSettings();
        
        
        List<OnboardingStep> gameSteps = List.of(
        			new OnboardingStep("#exitBtn", "Exit",
                        "Exit closes the application immediately."),
        			new OnboardingStep("#helpBtn", "Help",
                        "Opens the How to Play screen, shows the rules of the game."),
        			new OnboardingStep("#backBtn", "Back",
        					"Return to the New Game screen with your previously selected settings."),
                   new OnboardingStep("#pauseBtn", "Pause",
                           "Pause temporarily blocks gameplay and freezes interaction."),
                   new OnboardingStep("#soundButton", "Sound effects",
                           "Toggle click and UI sounds."),
                   new OnboardingStep("#musicButton", "Music",
                           "Toggle background music."),
                   new OnboardingStep("#label", "Difficulty + timer",
                           "Track current difficulty and elapsed time."),
                   new OnboardingStep("#player1BombsLeftLabel", "Mines left",
                           "Shows remaining mines,flags,questions and surprises for each board."),
                   new OnboardingStep("#heartsBox", "Shared hearts",
                           "These are shared between both players. Losing all hearts ends the match."),
                   new OnboardingStep("#scoreLabel", "Score",
                           "Score increases based on progress and correct actions.")
           
   );
        OnboardingPolicy policy =
                SessionManager.isAdminMode() ? OnboardingPolicy.NEVER :
                SessionManager.isGuestMode() ? OnboardingPolicy.ALWAYS :
                OnboardingPolicy.ONCE_THEN_HOVER;

        String userKey = SessionManager.getOnboardingUserKey();
        
        OnboardingManager.runWithPolicy("onboarding.game", root, gameSteps, policy, userKey, afterOnboardingClose);
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

        // Pause game if you want (recommended)
        if (!state.isPaused) {
            onPauseGame();
        }

        try {
            // Load once (cache)
            if (howToPlayOverlay == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/how_to_play_view.fxml"));
                howToPlayOverlay = loader.load();

                // IMPORTANT: let HowToPlay close itself using a callback (instead of navigation)
                HowToPlayController ctrl = loader.getController();
                ctrl.setCloseAction(this::closeHowToPlayOverlayFromGame);

                // Make overlay consume clicks so game UI is blocked
                howToPlayOverlay.setPickOnBounds(true);
                StackPane.setAlignment(howToPlayOverlay, javafx.geometry.Pos.CENTER);
                root.setEffect(new javafx.scene.effect.GaussianBlur(10));


            }

            if (!rootStack.getChildren().contains(howToPlayOverlay)) {
                rootStack.getChildren().add(howToPlayOverlay);
            }

        } catch (IOException e) {
            e.printStackTrace();
            // fail-safe resume
            if (state.isPaused) onPauseGame();
        }
    }
    
    private void closeHowToPlayOverlayFromGame() {
        if (howToPlayOverlay != null) {
            rootStack.getChildren().remove(howToPlayOverlay);
        }

        root.setEffect(null);

        if (state.isPaused) {
            onPauseGame();
        }

        bonusService.resetIdleHintTimer();
    }




    @FXML
    private void onBackBtnClicked() throws IOException {
        bonusService.resetIdleHintTimer();

        // Close overlays + clear blur/dim
        if (howToPlayOverlay != null) {
            rootStack.getChildren().remove(howToPlayOverlay);
        }
        root.setEffect(null);

        // Stop match runtime
        if (uiService != null) {
            uiService.stopTimer();
            uiService.unregisterAsObserver();
        }
        Stage stage = (Stage) player1Grid.getScene().getWindow();
        NewGameController ng = ViewNavigator.switchToWithController(stage, "/view/new_game_view.fxml");
        ng.prefillFromConfig(state.config);
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
    try {
        // Stop observing / listening before we replace the scene
        if (uiService != null) {
            uiService.unregisterAsObserver();
        }

        String fxmlPath = state.gameWon ? "/view/win_view.fxml" : "/view/lose_view.fxml";
        Stage stage = (Stage) player1Grid.getScene().getWindow();

        EndGameController controller =
                util.ViewNavigator.switchToWithController(stage, fxmlPath, 700, 450);

        controller.init(
                state.config,
                state.score,
                state.elapsedSeconds,
                state.sharedHearts,
                state.gameWon
        );

        Platform.runLater(playService::showHeartsBonusPopupIfNeeded);

    } catch (Exception e) {
        e.printStackTrace();
        DialogUtil.show(javafx.scene.control.Alert.AlertType.ERROR,
                "Navigation Error",
                "Failed to load end-game screen.",
                e.getMessage());
    }
  }
  }
