package util;

import java.util.Set;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;

public class UIAnimations {

    public static void applyHoverZoom(Node node) {
        final double hoverScale = 1.05;
        final Duration duration = Duration.millis(150);

        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(duration, node);
            st.setToX(hoverScale);
            st.setToY(hoverScale);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(duration, node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_IN);
            st.play();
        });

        node.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), node);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });

        node.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), node);
            st.setToX(hoverScale);
            st.setToY(hoverScale);
            st.play();
        });
    }

    public static void applyHoverZoomToAllButtons(Parent root) {
        Set<Node> buttons = root.lookupAll(".button");
        Set<Node> toggles = root.lookupAll(".toggle-button");

        buttons.forEach(UIAnimations::applyHoverZoom);
        toggles.forEach(UIAnimations::applyHoverZoom);
    }

    public static void applyHoverZoomToClass(Parent root) {
        Set<Node> nodes = root.lookupAll(".zoom-on-hover");
        nodes.forEach(UIAnimations::applyHoverZoom);
    }

    public static void applyFloating(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(3), node);
        tt.setFromY(0);
        tt.setToY(-10);           
        tt.setAutoReverse(true);  
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    public static void applyFloatingToCards(Parent root) {
        Set<Node> cards = root.lookupAll(".floating-card");
        cards.forEach(UIAnimations::applyFloating);
    }
}
