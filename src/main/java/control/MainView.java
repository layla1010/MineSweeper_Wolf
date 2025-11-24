package control;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;

public class MainView {

    @FXML private ImageView logoImage;

    @FXML
    private void initialize() {
        playLogoAnimation();
    }

    private void playLogoAnimation() {

        // slide animation
        TranslateTransition slide = new TranslateTransition(Duration.millis(1200), logoImage);
        slide.setFromX(-600);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        // fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(800), logoImage);
        fade.setFromValue(0);
        fade.setToValue(1);

        slide.play();
        fade.play();
    }
}
