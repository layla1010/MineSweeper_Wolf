package util;

import java.util.Random;
import java.util.Set;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class UIAnimations {
	
	private UIAnimations() {}

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
    
    //Plays the entrance animation for the logo and slides it in from the left and fades it in.
    public static void playLogoAnimation(ImageView logoImage) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(1200), logoImage);
        slide.setFromX(-600);
        slide.setToX(50);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(800), logoImage);
        fade.setFromValue(0);
        fade.setToValue(1);

        slide.play();
        fade.play();
    }

    
    //Sets up a shimmering highlight that moves across the "New Game" button in an infinite loop.
    public static void setupNewGameShimmer(Button newGameBtn, Rectangle newGameShimmer) {

        LinearGradient lg = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(0.5, Color.rgb(255, 255, 255, 0.7)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        newGameShimmer.setFill(lg);

        newGameShimmer.widthProperty().bind(newGameBtn.widthProperty());
        newGameShimmer.heightProperty().bind(newGameBtn.heightProperty());

        Rectangle shimmerClip = new Rectangle();
        shimmerClip.widthProperty().bind(newGameBtn.widthProperty());
        shimmerClip.heightProperty().bind(newGameBtn.heightProperty());
        newGameShimmer.setClip(shimmerClip);

        newGameBtn.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            double width = newB.getWidth();
            if (width <= 0) return;

            Timeline shimmer = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(newGameShimmer.translateXProperty(), -width),
                            new KeyValue(newGameShimmer.opacityProperty(), 0.0)
                    ),
                    new KeyFrame(Duration.millis(700),
                            new KeyValue(newGameShimmer.opacityProperty(), 1.0)
                    ),
                    new KeyFrame(Duration.millis(1600),
                            new KeyValue(newGameShimmer.translateXProperty(), width),
                            new KeyValue(newGameShimmer.opacityProperty(), 0.0)
                    )
            );
            shimmer.setCycleCount(Animation.INDEFINITE);
            shimmer.setDelay(Duration.seconds(2));
            shimmer.play();
        });
    }


   //Creates a glowing orb circle with a radial gradient and blur effect.
    public static Circle createOrb(double radius, String centerColor, String edgeColor) {
        RadialGradient grad = new RadialGradient(
                0, 0,
                0.5, 0.5,
                0.5,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(centerColor)),
                new Stop(1.0, Color.web(edgeColor))
        );

        Circle orb = new Circle(radius);
        orb.setMouseTransparent(true);
        orb.setFill(grad);
        orb.setOpacity(0.6);
        orb.setEffect(new GaussianBlur(60));
        return orb;
    }
    
    //Animates a glowing orb with a slow, looping movement.
    public static void animateOrb(Circle orb, double dx, double dy, double seconds, double delay) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(seconds), orb);
        tt.setFromX(0);
        tt.setFromY(0);
        tt.setToX(dx);
        tt.setToY(dy);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setDelay(Duration.seconds(delay));
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    //Creates and animates several large glowing orbs in the background to give the main menu a dynamic, colorful look.
    public static void setupBackgroundOrbs(GridPane mainGrid) {
        Circle orb1 = createOrb(220, "#ff6ec7", "#ff1493");
        orb1.setTranslateX(-300);
        orb1.setTranslateY(-220);

        Circle orb2 = createOrb(260, "#7c3aed", "#5b21b6");
        orb2.setTranslateX(350);
        orb2.setTranslateY(260);

        Circle orb3 = createOrb(230, "#3b82f6", "#1e40af");
        orb3.setTranslateX(280);
        orb3.setTranslateY(0);

        mainGrid.getChildren().addAll(orb1, orb2, orb3);

        orb1.toBack();
        orb2.toBack();
        orb3.toBack();

        animateOrb(orb1, 70, -70, 14, 0);
        animateOrb(orb2, -60, 60, 16, 3);
        animateOrb(orb3, 40, -30, 13, 6);
    }

    //Creates multiple "energy ring" animations that expand outwards from the center of the screen, giving a pulse effect behind the UI.
    public static void setupEnergyRings(GridPane mainGrid) {
        createEnergyRing(mainGrid, 0);
        createEnergyRing(mainGrid, 1);
        createEnergyRing(mainGrid, 2);
    }

    //Creates and animates a single expanding ring
    public static void createEnergyRing(GridPane mainGrid, double delaySeconds) {
        Circle ring = new Circle(120);
        ring.setMouseTransparent(true);
        ring.setStroke(Color.rgb(255, 255, 255, 0.4));
        ring.setStrokeWidth(2);
        ring.setFill(Color.TRANSPARENT);
        ring.setOpacity(0);

        ring.centerXProperty().bind(mainGrid.widthProperty().divide(2));
        ring.centerYProperty().bind(mainGrid.heightProperty().divide(2));

        mainGrid.getChildren().add(0, ring);
        ring.toBack();

        ScaleTransition scale = new ScaleTransition(Duration.seconds(3), ring);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(3.0);
        scale.setToY(3.0);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setDelay(Duration.seconds(delaySeconds));
        scale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.seconds(3), ring);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setDelay(Duration.seconds(delaySeconds));

        scale.play();
        fade.play();
    }

   //Generates small twinkling "sparkle" circles across the background were each sparkle fades in and out on a loop, at random positions and timings.
    public static void setupSparkles(GridPane mainGrid) {
        Random rnd = new Random();
        int count = 45;

        for (int i = 0; i < count; i++) {
            double size = 2 + rnd.nextDouble() * 3;
            Circle sparkle = new Circle(size, Color.rgb(255, 255, 255, 0.9));
            sparkle.setMouseTransparent(true);
            sparkle.setOpacity(0.0);

            double startX = rnd.nextDouble() * 900 - 450;
            double startY = rnd.nextDouble() * 750 - 375;
            sparkle.setTranslateX(startX);
            sparkle.setTranslateY(startY);

            double appearTime = 400 + rnd.nextInt(400);
            double totalTime  = 1200 + rnd.nextInt(800);

            Timeline twinkle = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sparkle.opacityProperty(), 0.0)
                    ),
                    new KeyFrame(Duration.millis(appearTime),
                            new KeyValue(sparkle.opacityProperty(), 1.0)
                    ),
                    new KeyFrame(Duration.millis(totalTime),
                            new KeyValue(sparkle.opacityProperty(), 0.0)
                    )
            );
            twinkle.setCycleCount(Animation.INDEFINITE);
            twinkle.setDelay(Duration.seconds(rnd.nextDouble() * 4.0));
            twinkle.play();

            mainGrid.getChildren().add(0, sparkle);
            sparkle.toBack();
        }
    }
}
