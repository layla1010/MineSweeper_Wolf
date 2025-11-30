package control;


//Aya Ala Deen – Settings Screen Implementation Documentation


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


//Controller for the Filters screen.
//Responsibilities: Display the current state of all gameplay filters, Update SysData whenever a toggle is changed, Navigate back to the Settings screen
//This controller does not execute the actual logic yet (music, sound, timers, etc.), it only updates configuration flags that other systems read.

public class FiltersController {

    //FXML Components
    @FXML
    private GridPane rootGrid;
    @FXML
    private ToggleButton musicToggle;//Toggle for enabling/disabling background music
    @FXML
    private ToggleButton soundToggle;//Toggle for enabling/disabling sound effects.
    @FXML
    private ToggleButton timerToggle;//Toggle for enabling/disabling the in-game timer
    @FXML
    private ToggleButton smartHintsToggle;//Toggle for enabling/disabling smart hints
    @FXML
    private ToggleButton autoRemoveFlagToggle;//Toggle for enabling/disabling auto remove flag


    //Called automatically after the FXML is loaded.
    @FXML
    private void initialize() {
        //Initialize UI from SysData configuration
        musicToggle.setSelected(SysData.isMusicEnabled());
        soundToggle.setSelected(SysData.isSoundEnabled());
        timerToggle.setSelected(SysData.isTimerEnabled());
        smartHintsToggle.setSelected(SysData.isSmartHintsEnabled());
        autoRemoveFlagToggle.setSelected(SysData.isAutoRemoveFlagEnabled());
    }


    //Navigates back to the Settings screen. Invoked when the back arrow button is clicked.
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

    // Toggle handlers – each one updates SysData
    @FXML
    private void onMusicToggled() {
        SoundManager.playClick();
        SysData.setMusicEnabled(musicToggle.isSelected());
    }

    @FXML
    private void onSoundToggled() {
        SoundManager.playClick();
        SysData.setSoundEnabled(soundToggle.isSelected());
    }

    @FXML
    private void onTimerToggled() {
        SoundManager.playClick();
        SysData.setTimerEnabled(timerToggle.isSelected());
    }

    @FXML
    private void onSmartHintsToggled() {
        SoundManager.playClick();
        SysData.setSmartHintsEnabled(smartHintsToggle.isSelected());
    }

    @FXML
    private void onAutoRemoveFlagToggled() {
        SoundManager.playClick();
        SysData.setAutoRemoveFlagEnabled(autoRemoveFlagToggle.isSelected());
    }
}
