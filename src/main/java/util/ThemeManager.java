package util;

import javafx.scene.Scene;
import model.Theme;

import java.net.URL;
import java.util.Objects;
import java.util.prefs.Preferences;

public final class ThemeManager {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(ThemeManager.class);

    private static final String PREF_KEY = "ui.theme";

    private static Theme currentTheme = loadTheme();

    private ThemeManager() {}

    public static Theme getTheme() {
        return currentTheme;
    }

    public static void setTheme(Theme theme) {
        if (theme == null) theme = Theme.COLORFUL;
        currentTheme = theme;
        PREFS.put(PREF_KEY, theme.name());
    }

    /** Apply current theme CSS to the provided scene, safely and idempotently. */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        // Remove any theme CSS previously applied
        scene.getStylesheets().removeIf(ThemeManager::isThemeStylesheet);

        // Ensure base.css exists (optional but recommended)
        addIfMissing(scene, "/css/base.css");

        // Add selected theme CSS
        addIfMissing(scene, currentTheme.getCssPath());
    }

    private static boolean isThemeStylesheet(String stylesheetUrl) {
        // robust detection by known filenames
        return stylesheetUrl != null && (
                stylesheetUrl.endsWith("theme.css") ||
                stylesheetUrl.endsWith("wolf.css")
        );
    }

    private static void addIfMissing(Scene scene, String resourcePath) {
        URL url = ThemeManager.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Missing CSS resource: " + resourcePath);
        }
        String external = url.toExternalForm();
        if (!scene.getStylesheets().contains(external)) {
            scene.getStylesheets().add(external);
        }
    }

    private static Theme loadTheme() {
        String saved = PREFS.get(PREF_KEY, Theme.COLORFUL.name());
        try {
            return Theme.valueOf(Objects.requireNonNull(saved));
        } catch (Exception e) {
            return Theme.COLORFUL;
        }
    }
}
