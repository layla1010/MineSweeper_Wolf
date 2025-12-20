package control;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import model.Difficulty;
import model.GameConfig;
import model.SysData;
import util.AvatarManager;
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

    private Stage stage;
    private AvatarManager avatarManager;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Sets player 1 as active by default, wires avatar thumbnails,
     * applies UI animations, and syncs sound/music icons with global settings.
     */
    @FXML
    private void initialize() {
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

        String nickname1 = player1Nickname.getText();
        String nickname2 = player2Nickname.getText();

        if (nickname1 == null || nickname1.trim().isEmpty() ||
            nickname2 == null || nickname2.trim().isEmpty()) {
            showError("Please enter both players names.");
            return;
        }

        Toggle selectedToggle = difficultyGroup.getSelectedToggle();
        if (selectedToggle == null) {
            showError("Please select a difficulty level.");
            return;
        }
        
        String p1 = avatarManager.getSelectedAvatarIdForPlayer1();
        String p2 = avatarManager.getSelectedAvatarIdForPlayer2();

        if (p1 == null || p1.isBlank() || p2 == null || p2.isBlank()) {
            // Show an alert manually
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Avatar Required");
            a.setHeaderText(null);
            a.setContentText("Both players must choose an avatar before starting the game.");
            a.showAndWait();
            return;
        }

        Difficulty difficulty;
        if (selectedToggle == easyToggle) {
            difficulty = Difficulty.EASY;
        } else if (selectedToggle == medToggle) {
            difficulty = Difficulty.MEDIUM;
        } else {
            difficulty = Difficulty.HARD;
        }

        GameConfig config = new GameConfig(
                nickname1.trim(),
                nickname2.trim(),
                difficulty,
                p1,
                p2
        );

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/board_view.fxml"));
            Parent root = loader.load();

            GameController controller = loader.getController();
            controller.init(config);

            Stage stage = (Stage) player1Nickname.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to start the game due to an internal error.");
        }
    }
    
    
    @FXML
    private void onBackClicked() {
        playClickSound();

        try {
           
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
            Parent mainRoot = loader.load();

            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(mainRoot));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to main screen.");
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

    // Shows an error dialog with a given message (input validation).
    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
}
