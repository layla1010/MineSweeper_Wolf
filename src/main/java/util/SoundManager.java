package util;

import model.SysData;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import model.SysData;

public class SoundManager {

    private static Clip clickClip;
    private static Clip musicClip;

    // state handled by SysData, NOT here
    private static boolean musicOn = false;

    // ============================================================
    //  INIT
    // ============================================================
    public static void init() {
        initClick();
        initMusic();

        // auto-start music only if enabled from SysData
        if (SysData.isMusicEnabled()) {
            startMusic();
        }
    }

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

            // start paused
            musicClip.stop();  
            musicOn = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    //  SOUND EFFECTS
    // ============================================================
    
    public static void playClick() {
        // אם המשתמש כיבה "Sound" במסך ה-Filters – לא לנגן קליק
        if (!SysData.isSoundEnabled()) {
            return;
        }

        if (clickClip == null) return;
        if (clickClip.isRunning()) clickClip.stop();
        clickClip.setFramePosition(0);
        clickClip.start();
    }


    // ============================================================
    //  MUSIC CONTROL
    // ============================================================
    public static void startMusic() {
        if (musicClip == null) return;

        if (!musicClip.isRunning()) {
            musicClip.setFramePosition(0);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        musicOn = true;
        SysData.setMusicEnabled(true);   // sync SysData
    }

    public static void stopMusic() {
        if (musicClip == null) return;

        musicClip.stop();
        musicOn = false;
        SysData.setMusicEnabled(false);  // sync SysData
    }

    public static void toggleMusic() {
        if (musicOn) {
            stopMusic();
        } else {
            startMusic();
        }
    }

    public static boolean isMusicOn() {
        return musicOn;
    }

    // ============================================================
    //  EXTRA — for future effects
    // ============================================================
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
