package control;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Theme;
import util.SoundManager;
import util.ThemeManager;
import util.ViewNavigator;

import static util.SoundManager.MusicTrack;

public class CustomizeController {

    @FXML private VBox root;

    // Theme toggles
    @FXML private ToggleButton defaultBtn;
    @FXML private ToggleButton wolfBtn;
    @FXML private ToggleButton greenWolfBtn;
    @FXML private ToggleButton cyberBlueBtn;

    private final ToggleGroup themeGroup = new ToggleGroup();

    // Music preview buttons
    @FXML private Button previewRetroBtn;
    @FXML private Button previewArcadeBtn;
    @FXML private Button previewEscalationBtn;
    @FXML private Button previewFriendlyTempoBtn;
    @FXML private Button previewJazzyBotsBtn;

    // Music select toggles
    @FXML private ToggleButton musicRetroBtn;
    @FXML private ToggleButton musicArcadeBtn;
    @FXML private ToggleButton musicEscalationBtn;
    @FXML private ToggleButton musicFriendlyTempoBtn;
    @FXML private ToggleButton musicJazzyBotsBtn;

    private final ToggleGroup musicGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        // THEMES
        defaultBtn.setToggleGroup(themeGroup);
        wolfBtn.setToggleGroup(themeGroup);
        greenWolfBtn.setToggleGroup(themeGroup);
        cyberBlueBtn.setToggleGroup(themeGroup);

        themeGroup.selectedToggleProperty().addListener(preventNoSelection(themeGroup));
        selectToggleForTheme(ThemeManager.getTheme());

        // MUSIC SELECT
        musicRetroBtn.setToggleGroup(musicGroup);
        musicArcadeBtn.setToggleGroup(musicGroup);
        musicEscalationBtn.setToggleGroup(musicGroup);
        musicFriendlyTempoBtn.setToggleGroup(musicGroup);
        musicJazzyBotsBtn.setToggleGroup(musicGroup);

        musicGroup.selectedToggleProperty().addListener(preventNoSelection(musicGroup));
        selectToggleForMusic(SoundManager.getSelectedMusicTrack());

        // Ensure preview buttons start in Play state
        resetAllPreviewButtons();

        refreshButtonTexts();
    }

    // ---------------- THEMES ----------------
    @FXML private void onDefaultSelected() { applyAndPersistTheme(Theme.COLORFUL); }
    @FXML private void onWolfSelected() { applyAndPersistTheme(Theme.WOLF); }
    @FXML private void onGreenWolfSelected() { applyAndPersistTheme(Theme.GREENWOLF); }
    @FXML private void onCyberSelected() { applyAndPersistTheme(Theme.CYBER_BLUE); }

    private void applyAndPersistTheme(Theme theme) {
        ThemeManager.setTheme(theme);
        Scene scene = root.getScene();
        ThemeManager.applyTheme(scene);
        selectToggleForTheme(theme);
        refreshButtonTexts();
    }

    private void selectToggleForTheme(Theme theme) {
        if (theme == Theme.WOLF) themeGroup.selectToggle(wolfBtn);
        else if (theme == Theme.CYBER_BLUE) themeGroup.selectToggle(cyberBlueBtn);
        else if (theme == Theme.GREENWOLF) themeGroup.selectToggle(greenWolfBtn);
        else themeGroup.selectToggle(defaultBtn);
    }

    // ---------------- MUSIC PREVIEW (Play/Pause) ----------------

    @FXML private void onPreviewRetro() { togglePreview(MusicTrack.RETRO, previewRetroBtn); }
    @FXML private void onPreviewArcade() { togglePreview(MusicTrack.ARCADE, previewArcadeBtn); }
    @FXML private void onPreviewEscalation() { togglePreview(MusicTrack.ESCALATION, previewEscalationBtn); }
    @FXML private void onPreviewFriendlyTempo() { togglePreview(MusicTrack.FRIENDLY_TEMPO, previewFriendlyTempoBtn); }
    @FXML private void onPreviewJazzyBots() { togglePreview(MusicTrack.JAZZY_BEATS_BOTS, previewJazzyBotsBtn); }

    private void togglePreview(MusicTrack track, Button clickedButton) {
        SoundManager.PreviewState state = SoundManager.togglePreview(track);

        // If we started a new preview track, all other preview buttons should show Play
        if (state == SoundManager.PreviewState.PLAYING) {
            resetAllPreviewButtons();
            clickedButton.setText("⏸");
        } else if (state == SoundManager.PreviewState.PAUSED) {
            clickedButton.setText("▶");
        } else {
            // STOPPED (rare in UI, but safe)
            clickedButton.setText("▶");
        }
    }

    private void resetAllPreviewButtons() {
        if (previewRetroBtn != null) previewRetroBtn.setText("▶");
        if (previewArcadeBtn != null) previewArcadeBtn.setText("▶");
        if (previewEscalationBtn != null) previewEscalationBtn.setText("▶");
        if (previewFriendlyTempoBtn != null) previewFriendlyTempoBtn.setText("▶");
        if (previewJazzyBotsBtn != null) previewJazzyBotsBtn.setText("▶");
    }

    // ---------------- MUSIC SELECT ----------------

    @FXML private void onMusicRetroSelected() { applyAndPersistMusic(MusicTrack.RETRO); }
    @FXML private void onMusicArcadeSelected() { applyAndPersistMusic(MusicTrack.ARCADE); }
    @FXML private void onMusicEscalationSelected() { applyAndPersistMusic(MusicTrack.ESCALATION); }
    @FXML private void onMusicFriendlyTempoSelected() { applyAndPersistMusic(MusicTrack.FRIENDLY_TEMPO); }
    @FXML private void onMusicJazzyBotsSelected() { applyAndPersistMusic(MusicTrack.JAZZY_BEATS_BOTS); }

    private void applyAndPersistMusic(MusicTrack track) {
        SoundManager.setSelectedMusicTrack(track);

        // Selecting track should also reset preview icons (since preview was stopped)
        resetAllPreviewButtons();

        selectToggleForMusic(track);
        refreshButtonTexts();
    }

    private void selectToggleForMusic(MusicTrack track) {
        switch (track) {
            case ARCADE -> musicGroup.selectToggle(musicArcadeBtn);
            case ESCALATION -> musicGroup.selectToggle(musicEscalationBtn);
            case FRIENDLY_TEMPO -> musicGroup.selectToggle(musicFriendlyTempoBtn);
            case JAZZY_BEATS_BOTS -> musicGroup.selectToggle(musicJazzyBotsBtn);
            case RETRO -> musicGroup.selectToggle(musicRetroBtn);
            default -> musicGroup.selectToggle(musicRetroBtn);
        }
    }

    // ---------------- COMMON ----------------

    private ChangeListener<Toggle> preventNoSelection(ToggleGroup group) {
        return (obs, oldToggle, newToggle) -> {
            if (newToggle == null) group.selectToggle(oldToggle);
        };
    }

    @FXML
    private void onBackClicked() {
        Stage stage = (Stage) root.getScene().getWindow();
        ViewNavigator.switchTo(stage, "/view/settings_view.fxml");
    }

    private void refreshButtonTexts() {
        // Theme text
        defaultBtn.setText(defaultBtn.isSelected() ? "SELECTED" : "SELECT");
        wolfBtn.setText(wolfBtn.isSelected() ? "SELECTED" : "SELECT");
        cyberBlueBtn.setText(cyberBlueBtn.isSelected() ? "SELECTED" : "SELECT");
        greenWolfBtn.setText(greenWolfBtn.isSelected() ? "SELECTED" : "SELECT");

        // Music text
        musicRetroBtn.setText(musicRetroBtn.isSelected() ? "SELECTED" : "SELECT");
        musicArcadeBtn.setText(musicArcadeBtn.isSelected() ? "SELECTED" : "SELECT");
        musicEscalationBtn.setText(musicEscalationBtn.isSelected() ? "SELECTED" : "SELECT");
        musicFriendlyTempoBtn.setText(musicFriendlyTempoBtn.isSelected() ? "SELECTED" : "SELECT");
        musicJazzyBotsBtn.setText(musicJazzyBotsBtn.isSelected() ? "SELECTED" : "SELECT");
    }
}
