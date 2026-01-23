package control;

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import util.DialogUtil;
import util.OnboardingManager;
import util.OnboardingPolicy;
import util.OnboardingStep;
import util.SessionManager;
import util.SoundManager;
import util.UIAnimations;
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

    private boolean adminMode = false;


    @FXML
    private void initialize() {
    	UIAnimations.fadeIn(mainGrid);
    	UIAnimations.playLogoAnimation(logoImage);
        UIAnimations.setupBackgroundOrbs(mainGrid);
        UIAnimations.setupEnergyRings(mainGrid);
        UIAnimations.setupSparkles(mainGrid);
        UIAnimations.setupNewGameShimmer(newGameBtn, newGameShimmer);
        refreshLoginUI();

        UIAnimations.applyHoverZoomToAllButtons(mainGrid);
        UIAnimations.applyFloatingToCards(mainGrid);

        loginBox.toFront();
        
        List<OnboardingStep> mainSteps = new ArrayList<>();

        mainSteps.add(new OnboardingStep("#settingsBtn", "Settings",
                "Open the Settings page to manage sound, music, themes, and learn how the game works."));

        boolean isGuest = SessionManager.isGuestMode();
        boolean hasLoggedInSession =
                SessionManager.getLoggedInUser() != null
                || SessionManager.getPlayer1() != null
                || SessionManager.getPlayer2() != null;

        // Show exactly one of them (never both)
        if (hasLoggedInSession && !isGuest) {
            mainSteps.add(new OnboardingStep("#logoutBtn", "Log out",
                    "End the current session and return to guest mode."));
        } else if (isGuest) {
            mainSteps.add(new OnboardingStep("#loginLink", "Log in",
                    "Go to the login screen to sign in or create a new account."));
        }
        mainSteps.add(new OnboardingStep("#newGameBtn", "New Game",
        		"Start a new game and choose your match settings."));
        mainSteps.add(new OnboardingStep("#historyBtn", "History",
        		"View previous games, results, and match details."));
        mainSteps.add(new OnboardingStep("#questionManagementBtn", "Question Management",
        		"Manage game questions, answers, and difficulty content (Only for Admin)."));
        mainSteps.add(new OnboardingStep("#statisticsBtn", "Statistics",
        		"Analyze player performance, scores, and game statistics (Only for registered players)."));

        
        OnboardingPolicy policy =
                SessionManager.isAdminMode() ? OnboardingPolicy.NEVER :
                SessionManager.isGuestMode() ? OnboardingPolicy.ALWAYS :
                OnboardingPolicy.ONCE_THEN_HOVER;

        String userKey = SessionManager.getOnboardingUserKey();

        OnboardingManager.runWithPolicy("onboarding.main", mainGrid, mainSteps, policy, userKey);
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
            util.ViewNavigator.switchTo(stage, "/view/new_game_view.fxml");
        } catch (Exception e) {
            e.printStackTrace();
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
        	        Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
        	                .getScene().getWindow();

        	        util.ViewNavigator.switchTo(stage, "/view/Questions_Management_view.fxml");
        	        stage.setTitle("Questions Management");

            } catch (Exception e) {
                e.printStackTrace();
            }
     }

      //Handles the History button click: Plays click sound and opens the History screen (history_view.fxml).
      @FXML
      private void onHistoryBtnClicked() {
          SoundManager.playClick();
          try {
              Stage stage = (Stage) mainGrid.getScene().getWindow();
              util.ViewNavigator.switchTo(stage, "/view/history_view.fxml", 1200, 750);
          } catch (Exception e) {
              e.printStackTrace();
              DialogUtil.show(AlertType.ERROR,
                      "Navigation Error",
                      "Failed to open History screen.",
                      e.getMessage());
          }
      }


    //Once sittings button is clicked: Plays click sound and opens the Settings screen (settings_view.fxml).
      @FXML
      private void onSettingsClicked() {
          SoundManager.playClick();
          try {
              Stage stage = (Stage) mainGrid.getScene().getWindow();
              util.ViewNavigator.switchTo(stage, "/view/settings_view.fxml");
          } catch (Exception e) {
              e.printStackTrace();
              DialogUtil.show(AlertType.ERROR,
                      "Navigation Error",
                      "Failed to open Settings screen.",
                      e.getMessage());
          }
      }


      @FXML
      private void onLoginClicked(ActionEvent event) {
          System.out.println("LOGIN LINK CLICKED!");

          if (!SessionManager.isGuestMode()) {
              DialogUtil.show(AlertType.INFORMATION,
                      "Already logged in",
                      "You already have an active session. Log out first to switch users.",
                      null);
              return;
          }

          SoundManager.playClick();

          goToLoginView();
      }

    
      private void goToLoginView() {
    	    try {
    	        Stage stage = (Stage) ((logoutBtn != null) ? logoutBtn.getScene().getWindow()
    	                : (loginBox != null ? loginBox.getScene().getWindow()
    	                : null));

    	        if (stage == null) {
    	            throw new IllegalStateException("Cannot resolve Stage in MainController.");
    	        }

    	        util.ViewNavigator.switchTo(stage, "/view/players_login_view.fxml", 800, 400);
    	        stage.setTitle("Login");
    	        stage.setResizable(false);

    	    } catch (Exception ex) {
    	        DialogUtil.show(AlertType.ERROR,
    	                "Navigation error",
    	                "Could not open Login screen.",
    	                ex.getMessage());
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

        model.Player p1 = util.SessionManager.getPlayer1();
        model.Player p2 = util.SessionManager.getPlayer2();

        if (p1 == null || p2 == null) {
            DialogUtil.show(AlertType.INFORMATION,
                    "",
                    "Access Denied",
                    "Statistics are available only for logged-in players.\n\n" +
                            "Please login as two players before opening the statistics screen.");
            return;
        }

        try {
            Stage stage = (Stage) mainGrid.getScene().getWindow();
            util.ViewNavigator.switchTo(stage, "/view/stats_view.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.show(AlertType.ERROR,
                    "Validation Error",
                    "Failed to load the Statistics screen.",
                    e.toString());
        }
    }

}
