package util;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundManager {

    private static Clip clickClip;

    public static void init() {
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
        if (clickClip == null) return;
        if (clickClip.isRunning()) clickClip.stop();
        clickClip.setFramePosition(0);
        clickClip.start();
    }
}
