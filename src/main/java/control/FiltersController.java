package control;

/*
 * Aya Ala Deen – Settings Screen Implementation Documentation
 */

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
 *  - Display the current state of all filter options:
 *      * Music
 *      * Sound
 *      * Timer
 *      * Smart Hints
 *      * Auto Remove Flag
 *  - Update {@link SysData} whenever the user toggles a filter.
 *  - Allow the user to return to the Settings screen via a back arrow.
 *
 * This controller does not perform any actual audio / timer logic,
 * it only changes the configuration flags in {@link SysData}.
 * Other parts of the application (e.g., SoundManager, GameController)
 * can read these flags and behave accordingly.
 */
public class FiltersController {

    /** Root layout for the Filters screen (defined in filters_view.fxml). */
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

    /** Toggle for enabling/disabling auto remove flag. */
    @FXML
    private ToggleButton autoRemoveFlagToggle;

    /**
     * Called automatically after the FXML is loaded.
     * Initializes all toggle buttons according to the current values
     * stored in {@link SysData}.
     */
    @FXML
    private void initialize() {
        // Initialize UI from SysData configuration
        musicToggle.setSelected(SysData.isMusicEnabled());
        soundToggle.setSelected(SysData.isSoundEnabled());
        timerToggle.setSelected(SysData.isTimerEnabled());
        smartHintsToggle.setSelected(SysData.isSmartHintsEnabled());
        autoRemoveFlagToggle.setSelected(SysData.isAutoRemoveFlagEnabled());
    }

    /**
     * Navigates back to the Settings screen.
     * Invoked when the back arrow button is clicked.
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
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    // Toggle handlers – each one updates SysData
    // ---------------------------------------------------------------------

    /**
     * Called when the Music toggle is pressed.
     * Updates {@link SysData} and plays a click sound.
     */
    @FXML
    private void onMusicToggled() {
        SoundManager.playClick();
        SysData.setMusicEnabled(musicToggle.isSelected());
    }

    /**
     * Called when the Sound toggle is pressed.
     * Updates {@link SysData} and plays a click sound.
     */
    @FXML
    private void onSoundToggled() {
        SoundManager.playClick();
        SysData.setSoundEnabled(soundToggle.isSelected());
    }

    /**
     * Called when the Timer toggle is pressed.
     * Updates {@link SysData}.
     */
    @FXML
    private void onTimerToggled() {
        SoundManager.playClick();
        SysData.setTimerEnabled(timerToggle.isSelected());
    }

    /**
     * Called when the Smart Hints toggle is pressed.
     * Updates {@link SysData}.
     */
    @FXML
    private void onSmartHintsToggled() {
        SoundManager.playClick();
        SysData.setSmartHintsEnabled(smartHintsToggle.isSelected());
    }

    /**
     * Called when the Auto Remove Flag toggle is pressed.
     * Updates {@link SysData}.
     */
    @FXML
    private void onAutoRemoveFlagToggled() {
        SoundManager.playClick();
        SysData.setAutoRemoveFlagEnabled(autoRemoveFlagToggle.isSelected());
    }
}
