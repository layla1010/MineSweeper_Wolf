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

    // Base stylesheet (always kept)
    private static final String BASE_CSS = "/css/base.css";

    // Theme variants (swap between these)
    private static final String COLORFUL_CSS = "/css/theme.css";
    private static final String WOLF_CSS     = "/css/wolf.css";

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

    /**
     * Apply current theme CSS to the provided scene.
     * Rules:
     *  - base.css is always present
     *  - exactly one variant is present: theme.css (COLORFUL) OR wolf.css (WOLF)
     *  - never clears all stylesheets (so any screen-specific css can remain)
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        // 1) Ensure base is present
        addIfMissing(scene, BASE_CSS);

        // 2) Remove ONLY our known variants (do not touch other stylesheets)
        removeIfPresent(scene, COLORFUL_CSS);
        removeIfPresent(scene, WOLF_CSS);

        // 3) Add selected variant
        String variant = (currentTheme == Theme.WOLF) ? WOLF_CSS : COLORFUL_CSS;
        addIfMissing(scene, variant);

        // 4) Force re-apply (helps immediate visual update)
        if (scene.getRoot() != null) {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
        }

    }

    private static void addIfMissing(Scene scene, String resourcePath) {
        String external = toExternal(resourcePath);
        if (!scene.getStylesheets().contains(external)) {
            scene.getStylesheets().add(external);
        }
    }

    private static void removeIfPresent(Scene scene, String resourcePath) {
        URL url = ThemeManager.class.getResource(resourcePath);
        if (url == null) return;
        scene.getStylesheets().remove(url.toExternalForm());
    }

    private static String toExternal(String resourcePath) {
        URL url = ThemeManager.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Missing CSS resource: " + resourcePath);
        }
        return url.toExternalForm();
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
