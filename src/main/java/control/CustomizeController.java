package control;

import javafx.fxml.FXML;
import javafx.scene.Scene;
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

    private final ToggleGroup themeGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        // ToggleGroup ensures only one is selected.
        defaultBtn.setToggleGroup(themeGroup);
        wolfBtn.setToggleGroup(themeGroup);

        // Initialize selection from persisted theme
        Theme current = ThemeManager.getTheme();
        if (current == Theme.WOLF) {
            themeGroup.selectToggle(wolfBtn);
        } else {
            themeGroup.selectToggle(defaultBtn);
        }

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

    private void applyAndPersistTheme(Theme theme) {
        ThemeManager.setTheme(theme);

        // Apply immediately to current scene
        Scene scene = root.getScene();
        ThemeManager.applyTheme(scene);

        // Keep button text consistent
        // Ensure the group selection stays correct even if user re-clicks selected toggle
        if (theme == Theme.WOLF) {
            themeGroup.selectToggle(wolfBtn);
        } else {
            themeGroup.selectToggle(defaultBtn);
        }

        refreshButtonTexts();
    }

    @FXML
    private void onBackClicked() {
        Stage stage = (Stage) root.getScene().getWindow();
        // Change path if your settings view path differs
        ViewNavigator.switchTo(stage, "/view/settings_view.fxml");
    }

    private void refreshButtonTexts() {
        defaultBtn.setText(defaultBtn.isSelected() ? "SELECTED" : "SELECT");
        wolfBtn.setText(wolfBtn.isSelected() ? "SELECTED" : "SELECT");
    }
}
