package control;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import model.Difficulty;
import model.GameConfig;
import model.SysData;
import util.AvatarManager;
import util.DialogUtil;
import util.OnboardingManager;
import util.OnboardingPolicy;
import util.OnboardingStep;
import util.SessionManager;
import util.SoundManager;
import util.UIAnimations;

public class NewGameController {

    @FXML private Parent root;
    @FXML private TextField player1Nickname;
    @FXML private TextField player2Nickname;
    @FXML private ToggleGroup difficultyGroup;
    @FXML private ToggleButton easyToggle;
    @FXML private ToggleButton medToggle;
    @FXML private ToggleButton hardToggle;
    @FXML private Rectangle recP1;
    @FXML private Rectangle recP2;
    @FXML private ImageView player1avatar;
    @FXML private ImageView player2avatar;
    @FXML private ImageView img1;
    @FXML private ImageView img2;
    @FXML private ImageView img3;
    @FXML private ImageView img4;
    @FXML private ImageView img5;
    @FXML private ImageView img6;
    @FXML private ImageView img7;
    @FXML private ImageView img8;
    @FXML private ImageView img9;
    @FXML private ImageView img10;
    @FXML private ImageView img11;
    @FXML private ImageView img12;
    @FXML private ImageView img13;
    @FXML private Button setUpSoundButton;
    @FXML private Button setUpMusicButton;

    private AvatarManager avatarManager;
    private static final int MAX_NAME_LEN = 7;

    /**
     * Sets player 1 as active by default, wires avatar thumbnails,
     * applies UI animations, and syncs sound/music icons with global settings.
     */
    @FXML
    private void initialize() {
    	UIAnimations.fadeIn(root);
        selectPlayer(1);

        avatarManager = new AvatarManager(player1avatar, player2avatar);
        avatarManager.setupThumbnails(
                img1, img2, img3, img4, img5, img6,
                img7, img8, img9, img10, img11, img12, img13
        );

        UIAnimations.applyHoverZoomToAllButtons(root);
        UIAnimations.applyFloatingToCards(root);
        UIAnimations.applyHoverZoomToClass(root);

        // Sync icons with global settings (SysData + SoundManager)
        refreshSoundIconFromSettings();
        refreshMusicIconFromSettings();
        
        
        List<OnboardingStep> newGameSteps = List.of(
        		 new OnboardingStep("#backBtn", "Back",
                         "Return to the main menu."),
                 new OnboardingStep("#easyToggle", "Difficulty",
                         "Pick a difficulty. Each option changes grid size, mines, questions, and surprises."),
                 new OnboardingStep("#player1Nickname", "Player names",
                         "Enter both nicknames. These appear in-game."),
                 new OnboardingStep("#anchor", "Avatars",
                         "Choose an avatar for each player. Click the player card first to select who you’re editing."),
                 new OnboardingStep("#setUpSoundButton", "Sound effects",
                         "Toggle click and UI sound effects."),
                 new OnboardingStep("#setUpMusicButton", "Music",
                         "Toggle background music."),
                 new OnboardingStep("#startGameBtn", "Start game",
                         "Starts the match using your current setup.")
         );
         
        OnboardingPolicy policy =
                SessionManager.isAdminMode() ? OnboardingPolicy.NEVER :
                SessionManager.isGuestMode() ? OnboardingPolicy.ALWAYS :
                OnboardingPolicy.ONCE_THEN_HOVER;

        String userKey = SessionManager.getOnboardingUserKey();

        OnboardingManager.runWithPolicy("onboarding.newgame", root, newGameSteps, policy, userKey);
        
        limitTextLength(player1Nickname,MAX_NAME_LEN);
        limitTextLength(player2Nickname,MAX_NAME_LEN);

    }
    
    void limitTextLength(TextField tf, int max) {
        if (tf == null) return;
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.length() > max) {
                tf.setText(newV.substring(0, max));
            }
        });
    }

    // Utility method to play a standard click sound.
    @FXML
    private void playClickSound() {
        SoundManager.playClick();
    }

    // Called when player 1's card/area is clicked.
    @FXML
    private void onPlayer1AreaClicked() {
        selectPlayer(1);
        avatarManager.selectPlayer(1);
        playClickSound();
        setActivePlayerCard(recP1, recP2, player1Nickname, player2Nickname);
    }

    // Called when player 2's card/area is clicked.
    @FXML
    private void onPlayer2AreaClicked() {
        selectPlayer(2);
        avatarManager.selectPlayer(2);
        playClickSound();
        setActivePlayerCard(recP2, recP1, player2Nickname, player1Nickname);
    }

    /**
     * Updates visual state of the selected player card and focuses
     * the corresponding nickname TextField.
     */
    private void setActivePlayerCard(Rectangle activeCard,
                                     Rectangle otherCard,
                                     TextField activeField,
                                     TextField otherField) {

        otherCard.getStyleClass().remove("player-card-active");

        if (!activeCard.getStyleClass().contains("player-card-active")) {
            activeCard.getStyleClass().add("player-card-active");
        }

        if (activeField != null) {
            activeField.requestFocus();
        }

        activeField.positionCaret(activeField.getText().length());
        activeField.selectAll();
    }

    /**
     * Internally sets which player is currently active (1 or 2),
     * and updates the stroke color on the player rectangles as a hint.
     */
    private void selectPlayer(int player) {
        if (recP1 != null && recP2 != null) {
            if (player == 1) {
                recP1.setStroke(Color.web("#35E0FF"));
                recP2.setStroke(Color.web("#274B8E"));
            } else {
                recP1.setStroke(Color.web("#274B8E"));
                recP2.setStroke(Color.web("#35E0FF"));
            }
        }
    }

    @FXML
    private void onEasyCardClicked() {
        playClickSound();
        easyToggle.fire();
    }

    @FXML
    private void onMediumCardClicked() {
        playClickSound();
        medToggle.fire();
    }

    @FXML
    private void onHardCardClicked() {
        playClickSound();
        hardToggle.fire();
    }

    /**
     * Handles the "Start Game" button and validates:
     *  - Both player names are filled
     *  - A difficulty is selected
     *  - Both players have chosen avatars
     */
    @FXML
    private void onStartGameClicked() {
        playClickSound();

        if (!validateInputs()) return;

        GameConfig config = new GameConfig(
                player1Nickname.getText().trim(),
                player2Nickname.getText().trim(),
                resolveDifficulty(),
                avatarManager.getSelectedAvatarIdForPlayer1(),
                avatarManager.getSelectedAvatarIdForPlayer2()
        );

        try {
            Stage stage = (Stage) player1Nickname.getScene().getWindow();

            GameController controller =
                    util.ViewNavigator.switchToWithController(stage, "/view/board_view.fxml");

            controller.init(config);

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.show(AlertType.ERROR, null, "Input error",
                    "Failed to start the game due to an internal error.");
        }
    }

    
    @FXML
    private void onBackClicked() {
        playClickSound();

        try {
            Stage stage = (Stage) root.getScene().getWindow();
            util.ViewNavigator.switchTo(stage, "/view/main_view.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.show(AlertType.ERROR, null, "Input error",
                    "Failed to return to main screen.");
        }
    }


    /**
     * Handles the "+" button for custom avatar selection.
     * Opens a FileChooser so the current active player can pick
     * a local image as his/her avatar.
     */
    @FXML
    private void onPlus() {
        playClickSound();
        Stage stage = (Stage) player1avatar.getScene().getWindow();
        avatarManager.handlePlus(stage);
    }

    /**
     * Handles the sound button: toggles global sound-effects flag in SysData
     * and updates the speaker icon.
     */
    @FXML
    void onSoundOff() {
        // Flip global sound flag
        boolean newState = !SysData.isSoundEnabled();
        SysData.setSoundEnabled(newState);

        // Update icon
        refreshSoundIconFromSettings();
    }

    /**
     * Handles the music button: toggles background music via SoundManager,
     * updates SysData.musicEnabled, and refreshes the icon.
     */
    @FXML
    void onMusicToggle() {
        // Toggle actual music playback
        SoundManager.toggleMusic();

        // Sync SysData with real music state
        boolean musicOn = SoundManager.isMusicOn();
        SysData.setMusicEnabled(musicOn);

        // Update icon + keep SoundManager in sync
        refreshMusicIconFromSettings();
    }

    /**
     * Handles clicks on any avatar ImageView.
     * Delegates to AvatarManager to mark avatar selection.
     */
    @FXML
    private void onAvatarClicked(MouseEvent event) {
        playClickSound();
        avatarManager.handleAvatarPaneClick(event);
    }

    /** Update speaker icon according to SysData.isSoundEnabled(). */
    private void refreshSoundIconFromSettings() {
        if (setUpSoundButton == null) return;
        if (!(setUpSoundButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isSoundEnabled();
        String iconPath = enabled ? "/Images/volume.png" : "/Images/mute.png";

        Image img = new Image(getClass().getResourceAsStream(iconPath));
        iv.setImage(img);
    }

    /** Update music icon according to SysData.isMusicEnabled(). */
    private void refreshMusicIconFromSettings() {
        if (setUpMusicButton == null) return;
        if (!(setUpMusicButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isMusicEnabled();

        // Sync SoundManager with SysData
        if (enabled && !SoundManager.isMusicOn()) {
            SoundManager.startMusic();
        } else if (!enabled && SoundManager.isMusicOn()) {
            SoundManager.stopMusic();
        }

        String iconPath;
        double size;

        if (enabled) {
            // Music ON → small music icon
            iconPath = "/Images/music.png";
            size = 40;
        } else {
            // Music OFF → larger muted icon
            iconPath = "/Images/music_mute.png";
            size = 40;
        }

        Image img = new Image(getClass().getResourceAsStream(iconPath));
        iv.setImage(img);

        iv.setFitWidth(size);
        iv.setFitHeight(size);
    }
    
    
    private boolean validateInputs() {
        if (player1Nickname.getText().trim().isEmpty()
            || player2Nickname.getText().trim().isEmpty()) {

            DialogUtil.show(AlertType.ERROR, null,
                    "Input error", "Please enter both players names.");
            return false;
        }

        if (difficultyGroup.getSelectedToggle() == null) {
            DialogUtil.show(AlertType.ERROR, null,
                    "Input error", "Please select a difficulty level.");
            return false;
        }

        if (!avatarManager.hasBothAvatarsSelected()) {
            DialogUtil.show(AlertType.ERROR, null,
                    "Avatar Required",
                    "Both players must choose an avatar before starting the game.");
            return false;
        }

        return true;
    }
    
    private Difficulty resolveDifficulty() {
        Toggle t = difficultyGroup.getSelectedToggle();
        if (t == easyToggle) return Difficulty.EASY;
        if (t == medToggle) return Difficulty.MEDIUM;
        return Difficulty.HARD;
    }


    
}
