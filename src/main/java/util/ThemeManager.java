package util;

import javafx.scene.Scene;

public final class ThemeManager {

    public enum Theme {
        COLORFUL, WOLF
    }

    private static Theme currentTheme = Theme.COLORFUL;

    private ThemeManager() {}

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void setTheme(Theme theme) {
        currentTheme = (theme == null) ? Theme.COLORFUL : theme;
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        // Remove old theme(s) if you want strict switching:
        scene.getStylesheets().removeIf(s ->
                s.endsWith("/css/wolf.css")
        );

        if (currentTheme == Theme.WOLF) {
            String wolfCss = ThemeManager.class.getResource("/css/wolf.css").toExternalForm();
            if (!scene.getStylesheets().contains(wolfCss)) {
                scene.getStylesheets().add(wolfCss);
            }
        }
    }
}
