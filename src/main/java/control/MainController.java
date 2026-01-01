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
    	UIAnimations.playLogoAnimation(logoImage);
        UIAnimations.setupBackgroundOrbs(mainGrid);
        UIAnimations.setupEnergyRings(mainGrid);
        UIAnimations.setupSparkles(mainGrid);
        UIAnimations.setupNewGameShimmer(newGameBtn, newGameShimmer);
        refreshLoginUI();

        UIAnimations.applyHoverZoomToAllButtons(mainGrid);
        UIAnimations.applyFloatingToCards(mainGrid);

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
