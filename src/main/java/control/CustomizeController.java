package control;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.SysData;
import model.Theme; // optional (see below)
import util.SoundManager;
import util.ThemeManager;

public class CustomizeController {

    @FXML private VBox root;

    @FXML private ToggleButton wolfThemeBtn;
    @FXML private ToggleButton colorfulThemeBtn;

    private final ToggleGroup themeGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        // Group so only one theme can be selected
        wolfThemeBtn.setToggleGroup(themeGroup);
        colorfulThemeBtn.setToggleGroup(themeGroup);

        // Load existing selection (if you implement Theme in SysData)
        try {
            Theme current = SysData.getCurrentTheme();
            if (current == Theme.DEFAULT) {
                colorfulThemeBtn.setSelected(true);
                wolfThemeBtn.setSelected(false);
            } else {
                // default WOLF
                wolfThemeBtn.setSelected(false);
                colorfulThemeBtn.setSelected(true);
            }
        } catch (Exception ignore) {
            // If you didn't add Theme yet -> default to colorful selected
        	colorfulThemeBtn.setSelected(true);
        }

        refreshButtonTexts();
    }

    @FXML
    private void onWolfThemeSelected() {
        SoundManager.playClick();

        // ensure single selection
        wolfThemeBtn.setSelected(true);
        colorfulThemeBtn.setSelected(false);

        util.ThemeManager.setTheme(util.ThemeManager.Theme.WOLF);

     // Apply immediately to current window:
     Scene scene = root.getScene();
     util.ThemeManager.applyTheme(scene);

        refreshButtonTexts();

        // TODO: Apply theme live (CSS swap) if you want later
    }

    @FXML
    private void onColorfulThemeSelected() {
        SoundManager.playClick();

        colorfulThemeBtn.setSelected(true);
        wolfThemeBtn.setSelected(false);

        util.ThemeManager.setTheme(util.ThemeManager.Theme.COLORFUL);
        Scene scene = root.getScene();
        util.ThemeManager.applyTheme(scene);

        refreshButtonTexts();
    }

    @FXML
    private void onCustomizedThemeClicked() {
        SoundManager.playClick();

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Not Implemented Yet");
        a.setHeaderText(null);
        a.setContentText("Customized theme is not implemented yet.");
        a.showAndWait();
    }

    @FXML
    private void onBackClicked() {
        SoundManager.playClick();

        // Change this path to wherever your Filters/Settings screen is.
        // If you want it to go back to Settings:
        String backView = "/view/settings_view.fxml";

        try {
            Stage stage = (Stage) root.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(backView));
            Parent p = loader.load();

            Scene scene = new Scene(p);
            util.ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshButtonTexts() {
        wolfThemeBtn.setText(wolfThemeBtn.isSelected() ? "SELECTED" : "SELECT");
        colorfulThemeBtn.setText(colorfulThemeBtn.isSelected() ? "SELECTED" : "SELECT");
    }
}
