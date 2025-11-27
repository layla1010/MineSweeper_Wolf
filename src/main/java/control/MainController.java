package control;

import java.io.IOException;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.SoundManager;
import util.UIAnimations;

public class MainController {

    @FXML private GridPane root;
    @FXML private ImageView logoImage;
    @FXML private Button newGameBtn;
    @FXML private Rectangle newGameShimmer;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        playLogoAnimation();

        setupBackgroundOrbs();       
        setupEnergyRings();         
        setupSparkles();             

        UIAnimations.applyHoverZoomToAllButtons(root);
        UIAnimations.applyFloatingToCards(root);

        setupNewGameShimmer();
    }

 
    private void playLogoAnimation() {
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

   
    @FXML
    private void onNewGameClicked() {
        SoundManager.playClick();
        try {
            Stage stage = (Stage) root.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/new_game_view.fxml"));
            Pane newRoot = loader.load();

            NewGameController newGameController = loader.getController();
            newGameController.setStage(stage);

            stage.setScene(new Scene(newRoot));
            stage.show();
            stage.centerOnScreen();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load New Game screen");
            alert.setContentText("An unexpected error occurred:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onQuestionManagementClicked() {
        SoundManager.playClick();
        // need to implement question management (layla)
    }

    @FXML
    private void onHistoryBtnClicked() {
        SoundManager.playClick();
        // need to implement history (layla)
    }

    @FXML
    private void onSettingsClicked() {
        SoundManager.playClick();
        // need to implement settings (layla)
    }

  
    private void setupNewGameShimmer() {
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
            shimmer.setAutoReverse(false);
            shimmer.setDelay(Duration.seconds(2));
            shimmer.play();
        });
    }

   
    private Circle createOrb(double radius, String centerColor, String edgeColor) {
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
        orb.setFill(grad);
        orb.setOpacity(0.6);
        orb.setEffect(new GaussianBlur(60));
        return orb;
    }

    private void animateOrb(Circle orb, double dx, double dy, double seconds, double delay) {
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

    private void setupBackgroundOrbs() {
        Circle orb1 = createOrb(220, "#ff6ec7", "#ff1493");
        orb1.setTranslateX(-300);
        orb1.setTranslateY(-220);

        Circle orb2 = createOrb(260, "#7c3aed", "#5b21b6");
        orb2.setTranslateX(350);
        orb2.setTranslateY(260);

        Circle orb3 = createOrb(230, "#3b82f6", "#1e40af");
        orb3.setTranslateX(280);
        orb3.setTranslateY(0);

        root.getChildren().addAll(orb1, orb2, orb3);

        orb1.toBack();
        orb2.toBack();
        orb3.toBack();

        animateOrb(orb1, 70, -70, 14, 0);
        animateOrb(orb2, -60, 60, 16, 3);
        animateOrb(orb3, 40, -30, 13, 6);
    }

    
    private void setupEnergyRings() {
        createEnergyRing(0);
        createEnergyRing(1);
        createEnergyRing(2);
    }

    private void createEnergyRing(double delaySeconds) {
        Circle ring = new Circle(120);
        ring.setStroke(Color.rgb(255, 255, 255, 0.4));
        ring.setStrokeWidth(2);
        ring.setFill(Color.TRANSPARENT);
        ring.setOpacity(0);

        ring.centerXProperty().bind(root.widthProperty().divide(2));
        ring.centerYProperty().bind(root.heightProperty().divide(2));

        root.getChildren().add(0, ring);
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

   
    private void setupSparkles() {
        Random rnd = new Random();
        int count = 45;

        for (int i = 0; i < count; i++) {
            double size = 2 + rnd.nextDouble() * 3;
            Circle sparkle = new Circle(size, Color.rgb(255, 255, 255, 0.9));
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

            root.getChildren().add(0, sparkle);
            sparkle.toBack();
        }
    }
}
