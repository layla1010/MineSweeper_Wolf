package model;

/*
 * Aya Ala Deen – Settings Screen Implementation Documentation
 */

/**
 * Central system data class.
 *
 * At this stage, it stores global configuration flags that represent
 * the current filter settings chosen by the users in the Filters screen.
 *
 * Future responsibilities:
 *  - Load and save these settings from/to a file (JSON/XML, etc.).
 *  - Store questions, history of games, and any other system-wide data.
 *
 * All fields are static because the game currently has a single shared
 * configuration for both players.
 */
public class SysData {

    // -----------------------------------------------------------------
    // Filter configuration (default values)
    // -----------------------------------------------------------------

    /** Controls whether background music is enabled. */
    private static boolean musicEnabled = true;

    /** Controls whether sound effects are enabled. */
    private static boolean soundEnabled = true;

    /** Controls whether a game timer should be shown and used. */
    private static boolean timerEnabled = true;

    /** Controls whether "smart hints" are enabled in the game. */
    private static boolean smartHintsEnabled = false;

    /** Controls whether flags are automatically removed in some situations. */
    private static boolean autoRemoveFlagEnabled = true;

    // -----------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------

    public static boolean isMusicEnabled() {
        return musicEnabled;
    }

    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isTimerEnabled() {
        return timerEnabled;
    }

    public static void setTimerEnabled(boolean enabled) {
        timerEnabled = enabled;
    }

    public static boolean isSmartHintsEnabled() {
        return smartHintsEnabled;
    }

    public static void setSmartHintsEnabled(boolean enabled) {
        smartHintsEnabled = enabled;
    }

    public static boolean isAutoRemoveFlagEnabled() {
        return autoRemoveFlagEnabled;
    }

    public static void setAutoRemoveFlagEnabled(boolean enabled) {
        autoRemoveFlagEnabled = enabled;
    }

    /**
     * Resets all filters to their default values.
     * Can be called from a "Reset to defaults" button in the future.
     */
    public static void resetToDefaults() {
        musicEnabled = true;
        soundEnabled = true;
        timerEnabled = true;
        smartHintsEnabled = false;
        autoRemoveFlagEnabled = true;
    }
}
