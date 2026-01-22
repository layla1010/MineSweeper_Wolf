package control;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Theme;
import util.ThemeManager;
import util.ViewNavigator;

public class CustomizeController {

    @FXML private VBox root;

    @FXML private ToggleButton defaultBtn;
    @FXML private ToggleButton wolfBtn;
    @FXML private ToggleButton cyberBlueBtn; 

    private final ToggleGroup themeGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        // Put ALL toggles in the group
        defaultBtn.setToggleGroup(themeGroup);
        wolfBtn.setToggleGroup(themeGroup);
        cyberBlueBtn.setToggleGroup(themeGroup);

        // Optional but recommended: prevent “no selection”
        themeGroup.selectedToggleProperty().addListener(preventNoSelection());

        // Initialize selection from persisted theme
        Theme current = ThemeManager.getTheme();
        selectToggleForTheme(current);

        refreshButtonTexts();
    }

    @FXML
    private void onDefaultSelected() {
        applyAndPersistTheme(Theme.COLORFUL);
    }

    @FXML
    private void onWolfSelected() {
        applyAndPersistTheme(Theme.WOLF);
    }

    @FXML
    private void onCyberSelected() {
        applyAndPersistTheme(Theme.CYBER_BLUE);
    }

    private void applyAndPersistTheme(Theme theme) {
        ThemeManager.setTheme(theme);

        // Apply immediately to current scene
        Scene scene = root.getScene();
        ThemeManager.applyTheme(scene);

        // Keep selection consistent
        selectToggleForTheme(theme);

        refreshButtonTexts();
    }

    private void selectToggleForTheme(Theme theme) {
        if (theme == Theme.WOLF) {
            themeGroup.selectToggle(wolfBtn);
        } else if (theme == Theme.CYBER_BLUE) {
            themeGroup.selectToggle(cyberBlueBtn);
        } else {
            themeGroup.selectToggle(defaultBtn);
        }
    }

    private ChangeListener<Toggle> preventNoSelection() {
        return (obs, oldToggle, newToggle) -> {
            // If user tries to unselect the current toggle (newToggle becomes null),
            // revert back to the previous one so one is always selected.
            if (newToggle == null) {
                themeGroup.selectToggle(oldToggle);
            }
        };
    }

    @FXML
    private void onBackClicked() {
        Stage stage = (Stage) root.getScene().getWindow();
        ViewNavigator.switchTo(stage, "/view/settings_view.fxml");
    }

    private void refreshButtonTexts() {
        defaultBtn.setText(defaultBtn.isSelected() ? "SELECTED" : "SELECT");
        wolfBtn.setText(wolfBtn.isSelected() ? "SELECTED" : "SELECT");
        cyberBlueBtn.setText(cyberBlueBtn.isSelected() ? "SELECTED" : "SELECT");
    }
}
