package control;

// Aya Ala Deen – Filters Screen Implementation

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.SysData;
import util.SoundManager;

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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/settings_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootGrid.getScene().getWindow();
            Scene scene = new Scene(root);
            util.ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ====== Toggle handlers ======

    @FXML
    private void onMusicToggled() {
        SoundManager.playClick();

        boolean newState = !SysData.isMusicEnabled();
        SysData.setMusicEnabled(newState);
        syncToggle(musicToggle, newState);

        if (newState != SoundManager.isMusicOn()) {
            SoundManager.toggleMusic();
        }
    }

    @FXML
    private void onSoundToggled() {
        SoundManager.playClick();

        boolean newState = !SysData.isSoundEnabled();
        SysData.setSoundEnabled(newState);
        syncToggle(soundToggle, newState);
    }

    @FXML
    private void onTimerToggled() {
        SoundManager.playClick();

        boolean newState = !SysData.isTimerEnabled();
        SysData.setTimerEnabled(newState);
        syncToggle(timerToggle, newState);
    }

    @FXML
    private void onSmartHintsToggled() {
        SoundManager.playClick();

        boolean newState = !SysData.isSmartHintsEnabled();
        SysData.setSmartHintsEnabled(newState);
        syncToggle(smartHintsToggle, newState);
    }

    @FXML
    private void onAutoRemoveFlagToggled() {
        SoundManager.playClick();

        boolean newState = !SysData.isAutoRemoveFlagEnabled();
        SysData.setAutoRemoveFlagEnabled(newState);
        syncToggle(autoRemoveFlagToggle, newState);
    }

    // ====== Helper ======

    /**
     * Updates the toggle image according to the boolean state.
     */
    private void syncToggle(ImageView toggle, boolean enabled) {
        toggle.setImage(enabled ? SWITCH_ON : SWITCH_OFF);
    }
}
