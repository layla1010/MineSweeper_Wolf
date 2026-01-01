package util;

import javafx.scene.Scene;
import model.SysData;
import model.Theme;

public final class ThemeManager {


    private static Theme currentTheme = Theme.DEFAULT;

    private ThemeManager() {}

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void setTheme(Theme theme) {
        currentTheme = (theme == null) ? Theme.DEFAULT : theme;
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        String base = ThemeManager.class.getResource("/css/theme.css").toExternalForm();
        if (SysData.getCurrentTheme() == model.Theme.WOLF) {
            String wolf = ThemeManager.class.getResource("/css/wolf.css").toExternalForm();
            scene.getStylesheets().setAll(base, wolf);
        } else {
            scene.getStylesheets().setAll(base);
        }
    }

}
