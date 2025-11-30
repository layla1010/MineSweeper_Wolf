package util;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

//SoundManager handles all game audio
public class SoundManager {

    private static Clip clickClip;
    //Background music clip that loops continuously.
    private static Clip musicClip;
    //Indicates whether the background music is currently playing.
    private static boolean musicOn = false;

    public static void init() {
        initClick();
        initMusic();
    }
    //Loads the short click sound
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
    //Loads the background music
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
    //Plays the click sound once from the beginning.
    public static void playClick() {
        if (clickClip == null) return;
        if (clickClip.isRunning()) clickClip.stop();
        clickClip.setFramePosition(0);
        clickClip.start();
    }
    //Starts looping the background music. If already playing, this does nothing.
    public static void startMusic() {
        if (musicClip == null) return;
        if (!musicClip.isRunning()) {
            musicClip.setFramePosition(0);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
        musicOn = true;
    }
    //Stops the background music immediately.
    public static void stopMusic() {
        if (musicClip == null) return;
        musicClip.stop();
        musicOn = false;
    }
    //Toggles background music on/off depending on current state.
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
