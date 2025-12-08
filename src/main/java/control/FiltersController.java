package control;

// Aya Ala Deen – Filters Screen Implementation

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.SysData;
import util.SoundManager;

/**
 * Controller for the Filters screen.
 *
 * Responsibilities:
 *  - Show the current state of all gameplay filters (music, sound, timer, smart hints, auto-remove-flag)
 *  - Update SysData whenever the user changes a toggle
 *  - Navigate back to the Settings screen
 *
 * This screen ONLY changes configuration flags in SysData.
 * The actual logic that uses these flags (e.g. turning timer on/off, applying smart hints, etc.)
 * is implemented in other parts of the system.
 */
public class FiltersController {

    // ====== FXML Components ======
    @FXML
    private GridPane rootGrid;

    /** Toggle for enabling/disabling background music. */
    @FXML
    private ToggleButton musicToggle;

    /** Toggle for enabling/disabling sound effects. */
    @FXML
    private ToggleButton soundToggle;

    /** Toggle for enabling/disabling the in-game timer. */
    @FXML
    private ToggleButton timerToggle;

    /** Toggle for enabling/disabling smart hints. */
    @FXML
    private ToggleButton smartHintsToggle;

    /** Toggle for enabling/disabling auto removal of flags when not needed. */
    @FXML
    private ToggleButton autoRemoveFlagToggle;


    /**
     * Called automatically after the FXML is loaded.
     * Here we sync the toggles with the current configuration stored in SysData,
     * so the UI always shows the real state of the settings.
     */
    @FXML
    private void initialize() {
        musicToggle.setSelected(SysData.isMusicEnabled());
        soundToggle.setSelected(SysData.isSoundEnabled());
        timerToggle.setSelected(SysData.isTimerEnabled());
        smartHintsToggle.setSelected(SysData.isSmartHintsEnabled());
        autoRemoveFlagToggle.setSelected(SysData.isAutoRemoveFlagEnabled());
    }

    /**
     * Go back to the Settings screen.
     * Called when the user clicks the "back" arrow button.
     */
    @FXML
    private void onBackToSettingsClicked() {
        SoundManager.playClick();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/settings_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            // In a real app we might show an alert here, but for now we just print the error.
            e.printStackTrace();
        }
    }

    // ====== Toggle Handlers – each one updates SysData ======

    /**
     * Called when the Music toggle is pressed.
     * Updates SysData and also makes sure the actual background music state
     * matches the selected option.
     */
    @FXML
    private void onMusicToggled() {
        SoundManager.playClick();

        boolean enabled = musicToggle.isSelected();
        SysData.setMusicEnabled(enabled);

        // Keep real background music in sync with the toggle
        boolean currentlyOn = SoundManager.isMusicOn();
        if (enabled && !currentlyOn) {
            SoundManager.toggleMusic();
        } else if (!enabled && currentlyOn) {
            SoundManager.toggleMusic();
        }
    }

    /**
     * Called when the Sound toggle is pressed.
     * Only updates the "sound effects" flag in SysData.
     * Other screens will read this flag before playing sounds.
     */
    @FXML
    private void onSoundToggled() {
        SoundManager.playClick();
        SysData.setSoundEnabled(soundToggle.isSelected());
    }

    /**
     * Called when the Timer toggle is pressed.
     * Turns the timer configuration on/off in SysData.
     * The actual timer will check this flag when running.
     */
    @FXML
    private void onTimerToggled() {
        SoundManager.playClick();
        SysData.setTimerEnabled(timerToggle.isSelected());
    }

    /**
     * Called when the Smart Hints toggle is pressed.
     * Updates SysData so the game logic knows whether to use smart hints or not.
     */
    @FXML
    private void onSmartHintsToggled() {
        SoundManager.playClick();
        SysData.setSmartHintsEnabled(smartHintsToggle.isSelected());
    }

    /**
     * Called when the Auto-Remove-Flag toggle is pressed.
     * Updates SysData so the game knows if it should automatically
     * remove flags when they are no longer relevant.
     */
    @FXML
    private void onAutoRemoveFlagToggled() {
        SoundManager.playClick();
        SysData.setAutoRemoveFlagEnabled(autoRemoveFlagToggle.isSelected());
    }
}