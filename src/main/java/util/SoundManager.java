package util;

import model.SysData;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * SoundManager handles all game audio:
 *  - Click sound effects for UI buttons.
 *  - Background music (looped).
 *
 * It respects the global filters stored in {@link SysData}:
 *  - SysData.isSoundEnabled() → whether effects are played.
 *  - SysData.isMusicEnabled() → whether background music is on.
 */
public class SoundManager {

    /** Short click sound effect. */
    private static Clip clickClip;

    /** Background music clip that loops continuously. */
    private static Clip musicClip;

    /** Indicates whether the background music is currently playing. */
    private static boolean musicOn = false;

   

    /**
     * Initializes all audio resources. Should be called once on app startup.
     * Loads the click sound and the background music.
     * If music is enabled in SysData, it will auto-start music.
     */
    public static void init() {
        initClick();
        initMusic();

        // Auto-start music only if enabled from SysData
        if (SysData.isMusicEnabled()) {
            startMusic();
        }
    }

    /** Loads the short click sound. */
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

    /** Loads the background music clip and prepares it in loop mode (but stopped). */
    private static void initMusic() {
        try {
            var url = SoundManager.class.getResource("/Sounds/retro.wav");
            if (url == null) {
                System.err.println("retro.wav not found!");
                return;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            musicClip = AudioSystem.getClip();
            musicClip.open(audioIn);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);

            // Start in paused state
            musicClip.stop();
            musicOn = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   
    /**
     * Plays the click sound once from the beginning.
     * Respects SysData.isSoundEnabled().
     */
    public static void playClick() {
        // If the user disabled "Sound" in filters – don't play click
        if (!SysData.isSoundEnabled()) return;
        if (clickClip == null) return;

        if (clickClip.isRunning()) {
            clickClip.stop();
        }

        clickClip.setFramePosition(0);
        clickClip.start();
    }

    

    /**
     * Starts looping the background music. If already playing, this does nothing.
     * Also updates SysData.isMusicEnabled(true).
     */
    public static void startMusic() {
        if (musicClip == null) return;

        if (!musicClip.isRunning()) {
            musicClip.setFramePosition(0);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        musicOn = true;
        SysData.setMusicEnabled(true);   // sync SysData
    }

    /**
     * Stops the background music immediately.
     * Also updates SysData.isMusicEnabled(false).
     */
    public static void stopMusic() {
        if (musicClip == null) return;

        musicClip.stop();
        musicOn = false;
        SysData.setMusicEnabled(false);  // sync SysData
    }

    /**
     * Toggles background music on/off depending on current state.
     */
    public static void toggleMusic() {
        if (musicOn) {
            stopMusic();
        } else {
            startMusic();
        }
    }

    /**
     * @return true if background music is currently playing.
     */
    public static boolean isMusicOn() {
        return musicOn;
    }


    /**
     * Plays an arbitrary effect file from /Sounds/, e.g. "explosion.wav".
     * This does not reuse a Clip; it creates a new one each time.
     * Respects SysData.isSoundEnabled().
     */
    public static void playEffect(String soundName) {
        if (!SysData.isSoundEnabled()) return;  // respect sound filter

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
}
