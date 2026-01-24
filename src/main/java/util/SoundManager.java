package util;

import model.SysData;

import javax.sound.sampled.*;
import java.util.prefs.Preferences;

/**
 * SoundManager handles all game audio:
 *  - Click sound effects for UI buttons.
 *  - Background music (looped).
 *
 * Supports:
 *  - Selectable background music tracks (persisted).
 *  - Preview playback with Play/Pause toggle.
 *  - Preview pauses background music and resumes after preview ends.
 *
 * NOTE: Click/effects behavior is unchanged.
 */
public class SoundManager {

    /** Short click sound effect. */
    private static Clip clickClip;

    /** Background music clip that loops continuously. */
    private static Clip musicClip;

    /** Preview clip (plays once, not looped). */
    private static Clip previewClip;

    /** Indicates whether the background music is currently playing. */
    private static boolean musicOn = false;

    // Background pause/resume during preview
    private static long pausedMusicPositionMicros = 0;
    private static boolean shouldResumeAfterPreview = false;

    // Guard against STOP firing multiple times
    private static boolean handlingPreviewStop = false;

    // Preview state
    private static MusicTrack previewTrack = null;
    private static boolean previewPaused = false;
    private static long previewPausedPositionMicros = 0;

    // -------------------- Music Track Selection --------------------

    public enum MusicTrack {
        RETRO("Retro (Default)", "/Sounds/retro.wav"),
        ARCADE("Arcade", "/Sounds/arcade.wav"),
        ESCALATION("Escalation", "/Sounds/Escalation.wav"),
        FRIENDLY_TEMPO("Friendly Tempo", "/Sounds/Friendly-Tempo.wav"),
        JAZZY_BEATS_BOTS("Jazzy Beats & Bots", "/Sounds/Jazzy-Beats-_-Bots.wav");

        private final String displayName;
        private final String resourcePath;

        MusicTrack(String displayName, String resourcePath) {
            this.displayName = displayName;
            this.resourcePath = resourcePath;
        }

        public String getDisplayName() { return displayName; }
        public String getResourcePath() { return resourcePath; }
    }

    public enum PreviewState {
        PLAYING, PAUSED, STOPPED
    }

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(SoundManager.class);

    private static final String PREF_SELECTED_MUSIC = "music.selectedTrack";

    private static MusicTrack selectedTrack = MusicTrack.RETRO;

    /**
     * Initializes all audio resources. Should be called once on app startup.
     * Loads click sound and prepares background music (selected track).
     */
    public static void init() {
        initClick();

        selectedTrack = loadSelectedMusicTrack();
        loadMusicClip(selectedTrack);

        if (SysData.isMusicEnabled()) {
            startMusic();
        }
    }

    // -------------------- Click Sound (unchanged) --------------------

    private static void initClick() {
        try {
            var url = SoundManager.class.getResource("/Sounds/pop-tap.wav");
            if (url == null) {
                System.err.println("pop-tap.wav not found!");
                return;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            clickClip = AudioSystem.getClip();
            clickClip.open(audioIn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playClick() {
        if (!SysData.isSoundEnabled()) return;
        if (clickClip == null) return;

        if (clickClip.isRunning()) clickClip.stop();
        clickClip.setFramePosition(0);
        clickClip.start();
    }

    public static void playEffect(String soundName) {
        if (!SysData.isSoundEnabled()) return;

        try {
            var url = SoundManager.class.getResource("/Sounds/" + soundName);
            if (url == null) return;

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------- Background Music (selectable) --------------------

    public static MusicTrack getSelectedMusicTrack() {
        return selectedTrack;
    }

    /**
     * Persists the selected music track and (if music is enabled) switches immediately.
     */
    public static void setSelectedMusicTrack(MusicTrack track) {
        if (track == null) return;

        selectedTrack = track;
        saveSelectedMusicTrack(track);

        // Stop preview completely so there is no overlap and no auto-resume confusion
        stopPreviewFully();

        stopMusicInternal();
        loadMusicClip(track);

        if (SysData.isMusicEnabled()) {
            startMusic();
        } else {
            musicOn = false;
        }
    }

    // -------------------- Preview (Play / Pause toggle) --------------------

    /**
     * Toggle preview for a given track:
     * - If the same track is currently previewing and playing -> PAUSE.
     * - If the same track is paused -> RESUME.
     * - If another track is requested -> stop current preview and play the new one.
     */
    public static PreviewState togglePreview(MusicTrack track) {
        if (track == null) return PreviewState.STOPPED;

        // If same track preview exists: toggle play/pause
        if (previewClip != null && previewTrack == track) {
            if (previewClip.isRunning()) {
                // Pause
                previewPausedPositionMicros = previewClip.getMicrosecondPosition();
                previewClip.stop();
                previewPaused = true;
                return PreviewState.PAUSED;
            } else if (previewPaused) {
                // Resume
                try {
                    previewClip.setMicrosecondPosition(previewPausedPositionMicros);
                    previewClip.start();
                    previewPaused = false;
                    return PreviewState.PLAYING;
                } catch (Exception e) {
                    e.printStackTrace();
                    stopPreviewFully();
                    return PreviewState.STOPPED;
                }
            } else {
                // Preview ended and clip is stopped: restart from beginning
                stopPreviewFully();
                startPreview(track);
                return PreviewState.PLAYING;
            }
        }

        // Different track requested: stop old preview and start new
        stopPreviewFully();
        startPreview(track);
        return PreviewState.PLAYING;
    }

    public static MusicTrack getPreviewTrack() {
        return previewTrack;
    }

    public static boolean isPreviewPlaying() {
        return previewClip != null && previewClip.isRunning();
    }

    public static boolean isPreviewPaused() {
        return previewClip != null && !previewClip.isRunning() && previewPaused;
    }

    private static void startPreview(MusicTrack track) {
        try {
            handlingPreviewStop = false;
            previewTrack = track;
            previewPaused = false;
            previewPausedPositionMicros = 0;

            // Pause background music if it is currently playing
            shouldResumeAfterPreview =
                    (SysData.isMusicEnabled() && musicClip != null && musicClip.isRunning());

            if (shouldResumeAfterPreview) {
                pausedMusicPositionMicros = musicClip.getMicrosecondPosition();
                musicClip.stop(); // pause WITHOUT changing SysData
                musicOn = false;
            }

            var url = SoundManager.class.getResource(track.getResourcePath());
            if (url == null) {
                System.err.println("Preview track not found: " + track.getResourcePath());
                resumeMusicIfNeeded();
                previewTrack = null;
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            previewClip = AudioSystem.getClip();
            previewClip.open(audioIn);

            // When preview ends naturally, resume background music if it was paused
            previewClip.addLineListener(event -> {
                if (event.getType() != LineEvent.Type.STOP) return;
                if (handlingPreviewStop) return;

                // If STOP was caused by pausing, do NOT treat it as finished
                if (previewPaused) return;

                handlingPreviewStop = true;

                stopPreviewFully();      // fully close + reset preview state
                resumeMusicIfNeeded();   // resumes only if we paused it

                handlingPreviewStop = false;
            });

            previewClip.setFramePosition(0);
            previewClip.start();

        } catch (Exception e) {
            e.printStackTrace();
            stopPreviewFully();
            resumeMusicIfNeeded();
        }
    }

    /**
     * Stops preview and clears all preview state. Does NOT resume music automatically.
     * (Resume is handled explicitly where appropriate.)
     */
    private static void stopPreviewFully() {
        try {
            if (previewClip != null) {
                previewClip.stop();
                previewClip.close();
                previewClip = null;
            }
        } catch (Exception ignored) {
        } finally {
            previewTrack = null;
            previewPaused = false;
            previewPausedPositionMicros = 0;
            shouldResumeAfterPreview = false;
            handlingPreviewStop = false;
        }
    }

    private static void resumeMusicIfNeeded() {
        if (!shouldResumeAfterPreview) return;

        // Only resume if music is still enabled globally
        if (!SysData.isMusicEnabled()) {
            shouldResumeAfterPreview = false;
            return;
        }

        if (musicClip != null) {
            musicClip.setMicrosecondPosition(pausedMusicPositionMicros);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            musicOn = true;
        }

        shouldResumeAfterPreview = false;
    }

    // -------------------- Music clip loading --------------------

    private static void loadMusicClip(MusicTrack track) {
        try {
            closeMusicClip();

            var url = SoundManager.class.getResource(track.getResourcePath());
            if (url == null) {
                System.err.println("Music file not found: " + track.getResourcePath());
                musicClip = null;
                musicOn = false;
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            musicClip = AudioSystem.getClip();
            musicClip.open(audioIn);

            // Prepare in loop mode, but initially stopped
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            musicClip.stop();
            musicOn = false;

        } catch (Exception e) {
            e.printStackTrace();
            musicClip = null;
            musicOn = false;
        }
    }

    private static void closeMusicClip() {
        try {
            if (musicClip != null) {
                musicClip.stop();
                musicClip.close();
                musicClip = null;
            }
        } catch (Exception ignored) {
        }
    }

    public static void startMusic() {
        if (musicClip == null) return;

        if (!musicClip.isRunning()) {
            musicClip.setFramePosition(0);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        musicOn = true;
        SysData.setMusicEnabled(true);
    }

    public static void stopMusic() {
        // If user turns music off, ensure preview is not forcing a resume later
        stopMusicInternal();
        SysData.setMusicEnabled(false);
    }

    private static void stopMusicInternal() {
        if (musicClip == null) {
            musicOn = false;
            return;
        }
        musicClip.stop();
        musicOn = false;
    }

    public static void toggleMusic() {
        if (musicOn) stopMusic();
        else startMusic();
    }

    public static boolean isMusicOn() {
        return musicOn;
    }

    // -------------------- Persistence --------------------

    private static void saveSelectedMusicTrack(MusicTrack track) {
        PREFS.put(PREF_SELECTED_MUSIC, track.name());
    }

    private static MusicTrack loadSelectedMusicTrack() {
        String stored = PREFS.get(PREF_SELECTED_MUSIC, MusicTrack.RETRO.name());
        try {
            return MusicTrack.valueOf(stored);
        } catch (Exception e) {
            return MusicTrack.RETRO;
        }
    }
}
