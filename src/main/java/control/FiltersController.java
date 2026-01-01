package control;

// Aya Ala Deen – Filters Screen Implementation

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.SysData;
import util.SoundManager;
import util.ViewNavigator;

/**
 * Controller for the Filters screen.
 *
 * Uses ImageView-based toggles instead of ToggleButtons
 * for clearer visual feedback.
 */
public class FiltersController {

    // ====== FXML ======
    @FXML private GridPane rootGrid;

    @FXML private ImageView musicToggle;
    @FXML private ImageView soundToggle;
    @FXML private ImageView timerToggle;
    @FXML private ImageView smartHintsToggle;
    @FXML private ImageView autoRemoveFlagToggle;

    // ====== Images ======
    private static final Image SWITCH_ON  =
            new Image(FiltersController.class.getResourceAsStream("/Images/switch-on.png"));
    private static final Image SWITCH_OFF =
            new Image(FiltersController.class.getResourceAsStream("/Images/switch-off.png"));

    // ====== Lifecycle ======
    @FXML
    private void initialize() {
        syncToggle(musicToggle, SysData.isMusicEnabled());
        syncToggle(soundToggle, SysData.isSoundEnabled());
        syncToggle(timerToggle, SysData.isTimerEnabled());
        syncToggle(smartHintsToggle, SysData.isSmartHintsEnabled());
        syncToggle(autoRemoveFlagToggle, SysData.isAutoRemoveFlagEnabled());
    }

    // ====== Navigation ======
    @FXML
    private void onBackToSettingsClicked() {
        SoundManager.playClick();
        Stage stage = (Stage) rootGrid.getScene().getWindow();
        // Change path if your settings view path differs
        ViewNavigator.switchTo(stage, "/view/settings_view.fxml");
    }

    // ====== Toggle handlers ======

    @FXML
    private void onMusicToggled() {
        SoundManager.playClick();

        boolean enabled = toggleAndSync(
                musicToggle,
                SysData.isMusicEnabled(),
                SysData::setMusicEnabled
        );

        if (enabled != SoundManager.isMusicOn()) {
            SoundManager.toggleMusic();
        }
    }

    @FXML
    private void onSoundToggled() {
        SoundManager.playClick();

        toggleAndSync(
                soundToggle,
                SysData.isSoundEnabled(),
                SysData::setSoundEnabled
        );
    }

    @FXML
    private void onTimerToggled() {
    	toggleAndSync(
                timerToggle,
                SysData.isTimerEnabled(),
                SysData::setTimerEnabled
        );
    }

    @FXML
    private void onSmartHintsToggled() {
        SoundManager.playClick();

        toggleAndSync(
                smartHintsToggle,
                SysData.isSmartHintsEnabled(),
                SysData::setSmartHintsEnabled
        );
    }

    @FXML
    private void onAutoRemoveFlagToggled() {
        SoundManager.playClick();

        toggleAndSync(
                autoRemoveFlagToggle,
                SysData.isAutoRemoveFlagEnabled(),
                SysData::setAutoRemoveFlagEnabled
        );
    }

    // ====== Helper ======

    /**
     * Updates the toggle image according to the boolean state.
     */
    private void syncToggle(ImageView toggle, boolean enabled) {
        toggle.setImage(enabled ? SWITCH_ON : SWITCH_OFF);
    }
    
    private boolean toggleAndSync(ImageView toggle, boolean currentState, java.util.function.Consumer<Boolean> setter) {
        boolean newState = !currentState;
        setter.accept(newState);
        syncToggle(toggle, newState);
        return newState;
    }

}
