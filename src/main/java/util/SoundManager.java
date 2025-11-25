package util;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundManager {

    private static Clip clickClip;
    private static Clip musicClip;
    private static boolean musicOn = false;

    public static void init() {
        initClick();
        initMusic();
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
            musicClip.stop();  
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playClick() {
        if (clickClip == null) return;
        if (clickClip.isRunning()) clickClip.stop();
        clickClip.setFramePosition(0);
        clickClip.start();
    }

    public static void startMusic() {
        if (musicClip == null) return;
        if (!musicClip.isRunning()) {
            musicClip.setFramePosition(0);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
        musicOn = true;
    }

    public static void stopMusic() {
        if (musicClip == null) return;
        musicClip.stop();
        musicOn = false;
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
}
