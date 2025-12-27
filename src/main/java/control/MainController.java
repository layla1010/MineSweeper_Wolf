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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.DialogUtil;
import util.SessionManager;
import util.SoundManager;
import util.UIAnimations;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;


public class MainController {

    @FXML private GridPane mainGrid;
    @FXML private ImageView logoImage;
    @FXML private Button newGameBtn;
    @FXML private Rectangle newGameShimmer;
    @FXML private HBox loginBox;
    @FXML private Button statisticsBtn;
    @FXML private Hyperlink loginLink;
    @FXML private Button logoutBtn;



    private Stage stage;
    private boolean adminMode = false;


    //Sets the Stage reference for this controller.
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        playLogoAnimation();

        setupBackgroundOrbs();
        setupEnergyRings();
        setupSparkles();
        refreshLoginUI();

        UIAnimations.applyHoverZoomToAllButtons(mainGrid);
        UIAnimations.applyFloatingToCards(mainGrid);

        setupNewGameShimmer();
        loginBox.toFront();


    }

    
    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
    }
    
    private void refreshLoginUI() {
        boolean isGuest = SessionManager.isGuestMode();
        boolean hasLoggedInSession =
                SessionManager.getLoggedInUser() != null
                || SessionManager.getPlayer1() != null
                || SessionManager.getPlayer2() != null;

        // Login link only when guest
        if (loginBox != null) {
            loginBox.setVisible(isGuest);
            loginBox.setManaged(isGuest);
        }

        // Logout only when there is an actual logged-in session (players/admin)
        if (logoutBtn != null) {
            logoutBtn.setVisible(hasLoggedInSession && !isGuest);
            logoutBtn.setManaged(hasLoggedInSession && !isGuest);
        }
    }

    private boolean isGuestSession() {
        return SessionManager.getLoggedInUser() == null
                && SessionManager.getPlayer1() == null
                && SessionManager.getPlayer2() == null;
    }
    
    @FXML
    private void onLogoutClicked() {

        boolean confirm = DialogUtil.confirm(
                "Logout",
                "Logout",
                "Are you sure you want to log out?\n\nThis will clear the current session.\n\n"
        );

        if (!confirm) return;

        SessionManager.clear();
        SessionManager.setGuestMode(false); // important: user is not in guest mode anymore

        goToLoginView();
    }

    
    //Handles the "New Game" button click: Plays click sound, loads new_game_view.fxml, and navigates to the New Game setup screen.
    @FXML
    private void onNewGameClicked() {
        SoundManager.playClick();
        try {
            Stage stage = (Stage) mainGrid.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/new_game_view.fxml"));
            Pane newRoot = loader.load();

            NewGameController controller = loader.getController();
            controller.setStage(stage);

            stage.setScene(new Scene(newRoot));
            stage.show();
            stage.centerOnScreen();

        } catch (IOException e) {
         	DialogUtil.show(AlertType.ERROR, "Failed to load New Game screen", "Error", "An unexpected error occurred:\n" + e.getMessage());
        }
    }



      //Handles Question MANAGER BUTTON: Opens the Questions Management screen (Questions_Management_view.fxml).  
      @FXML
      private void onQuestionManagementClicked(ActionEvent event) {
        	SoundManager.playClick();
        	
        	 if (!SessionManager.isAdminLoggedIn()) {
             	DialogUtil.show(AlertType.INFORMATION, "Invalid User", "Access Denied","Only admins can access Question Management.");
             	return;
        	    }
        	 
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/Questions_Management_view.fxml")
                );
                Parent root = loader.load();

                // Get current window
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                                .getScene().getWindow();

                stage.setScene(new Scene(root));
                stage.centerOnScreen();
                stage.setTitle("Questions Management");
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
     }

      //Handles the History button click: Plays click sound and opens the History screen (history_view.fxml).
    @FXML
    private void onHistoryBtnClicked() {
        SoundManager.playClick();
        try {
            Stage stage = (Stage) mainGrid.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/history_view.fxml")
            );
            Parent historyRoot = loader.load();

            HistoryController historyController = loader.getController();
            historyController.setStage(stage);

            stage.setScene(new Scene(historyRoot, 1200, 750));
            stage.show();
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
           	DialogUtil.show(AlertType.INFORMATION, "Invalid User", "Access Denied","Only admins can access Question Management.");
         	return;
        }
    }

    //Plays the entrance animation for the logo and slides it in from the left and fades it in.
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

    //Sets up a shimmering highlight that moves across the "New Game" button in an infinite loop.
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

   //Creates a glowing orb circle with a radial gradient and blur effect.
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
        orb.setMouseTransparent(true);
        orb.setFill(grad);
        orb.setOpacity(0.6);
        orb.setEffect(new GaussianBlur(60));
        return orb;
    }
    //Animates a glowing orb with a slow, looping movement.
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

    //Creates and animates several large glowing orbs in the background to give the main menu a dynamic, colorful look.
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

        mainGrid.getChildren().addAll(orb1, orb2, orb3);

        orb1.toBack();
        orb2.toBack();
        orb3.toBack();

        animateOrb(orb1, 70, -70, 14, 0);
        animateOrb(orb2, -60, 60, 16, 3);
        animateOrb(orb3, 40, -30, 13, 6);
    }

    //Creates multiple "energy ring" animations that expand outwards from the center of the screen, giving a pulse effect behind the UI.
    private void setupEnergyRings() {
        createEnergyRing(0);
        createEnergyRing(1);
        createEnergyRing(2);
    }

    //Creates and animates a single expanding ring
    private void createEnergyRing(double delaySeconds) {
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
    private void setupSparkles() {
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


    //Once sittings button is clicked: Plays click sound and opens the Settings screen (settings_view.fxml).
    @FXML
    private void onSettingsClicked() {
        SoundManager.playClick();
        try {
            Stage stage = (Stage) mainGrid.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/settings_view.fxml"));
            Parent root = loader.load();

            SettingsController controller = loader.getController();

            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLoginClicked(ActionEvent event) {
    	System.out.println("LOGIN LINK CLICKED!");
    	 if (!SessionManager.isGuestMode()) {
    	        DialogUtil.show(AlertType.INFORMATION, "Already logged in", "You already have an active session. Log out first to switch users.", null);
    	        return;
    	    }

        SoundManager.playClick();
        try {
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource("/view/players_login_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
          	DialogUtil.show(AlertType.ERROR, "Error", "Login navigation failed",e.toString());
         	return;
        }
    }
    
    private void goToLoginView() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/players_login_view.fxml")); 
            Stage stage = (Stage) ((logoutBtn != null) ? logoutBtn.getScene().getWindow()
                    : (loginBox != null ? loginBox.getScene().getWindow()
                    : null));

            if (stage == null) {
                // As a fallback, you can throw; but better to fail loudly during dev.
                throw new IllegalStateException("Cannot resolve Stage in MainController.");
            }
            Scene scene = new Scene(root, 800, 400);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();



        } catch (IOException ex) {
            DialogUtil.show(AlertType.ERROR, "Navigation error", "Could not open Login screen.", null);
            ex.printStackTrace();
        }
    }
    
    public void onEnteredMainView() {
        refreshLoginUI();
    }
    
  //Opens the statistics view
    @FXML
    private void onStatisticsClicked() {
        SoundManager.playClick();

        // Block stats if players are not logged in (skip-login / guest mode)
        model.Player p1 = util.SessionManager.getPlayer1();
        model.Player p2 = util.SessionManager.getPlayer2();

        if (p1 == null || p2 == null) {
        	DialogUtil.show(AlertType.INFORMATION, "","", "Statistics are available only for logged-in players.\n\n" +
                    "Please login as two players before opening the statistics screen.");
            return;
        }

        try {
            Stage stage = (Stage) mainGrid.getScene().getWindow();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/view/stats_view.fxml"));
            Parent root = loader.load();

            StatsViewController controller = loader.getController();
            controller.setStage(stage);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        	DialogUtil.show(AlertType.ERROR, "Validation Error","Failed to load the Statistics screen.", e.toString());

        }
    }


}
