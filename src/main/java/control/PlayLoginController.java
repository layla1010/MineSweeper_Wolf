package control;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.DialogUtil;
import util.EmailService;
import util.OnboardingManager;
import util.OnboardingPolicy;
import util.OnboardingStep;
import util.SoundManager;
import util.UIAnimations;

import java.util.List;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import model.Player;
import model.Role;
import model.SysData;
import util.SessionManager;

public class PlayLoginController {

    @FXML private AnchorPane mainPane;

    @FXML private GridPane   playerLoginCard;
    @FXML private AnchorPane adminLoginCard;

    @FXML private Text playersTab;
    @FXML private Text adminTab;

    @FXML private TextField p1NameField;
    @FXML private TextField p2NameField;

    @FXML private PasswordField p1PasswordField;
    @FXML private TextField     p1PasswordVisibleField;
    @FXML private ImageView     p1EyeIcon;

    @FXML private PasswordField p2PasswordField;
    @FXML private TextField     p2PasswordVisibleField;
    @FXML private ImageView     p2EyeIcon;

    @FXML private TextField     adminNameField;
    @FXML private PasswordField adminPasswordField;
    @FXML private TextField     adminPasswordVisibleField;
    @FXML private ImageView     adminEyeIcon;

    @FXML private Text forgotPasswordText;
    @FXML private Text forgotPasswordTextAdmin;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
    	
        if (p1PasswordField != null && p1PasswordVisibleField != null) {
            p1PasswordVisibleField.textProperty()
                    .bindBidirectional(p1PasswordField.textProperty());
        }

        if (p2PasswordField != null && p2PasswordVisibleField != null) {
            p2PasswordVisibleField.textProperty()
                    .bindBidirectional(p2PasswordField.textProperty());
        }

        if (adminPasswordField != null && adminPasswordVisibleField != null) {
            adminPasswordVisibleField.textProperty()
                    .bindBidirectional(adminPasswordField.textProperty());
        }

        setLoginMode(true, false);

        if (p1PasswordVisibleField != null) {
            p1PasswordVisibleField.setVisible(false);
            p1PasswordVisibleField.setManaged(false);
        }
        if (p2PasswordVisibleField != null) {
            p2PasswordVisibleField.setVisible(false);
            p2PasswordVisibleField.setManaged(false);
        }
        if (adminPasswordVisibleField != null) {
            adminPasswordVisibleField.setVisible(false);
            adminPasswordVisibleField.setManaged(false);
        }

        if (p1EyeIcon != null) {
            p1EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }
        if (p2EyeIcon != null) {
            p2EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }
        if (adminEyeIcon != null) {
            adminEyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }

        if (mainPane != null) {
            mainPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2, #f093fb);"
            );
        }
        
        UIAnimations.fadeIn(mainPane);
        
     // Guided onboarding (login must ALWAYS show because user not known yet)
        List<OnboardingStep> loginSteps = List.of(
                new OnboardingStep("#playersTab", "Players login",
                        "Use this tab for a 2-player match. Each player logs in with their own account."),
                new OnboardingStep("#adminTab", "Admin login",
                        "Use Admin only for admin features. Regular matches do not require admin login."),
                new OnboardingStep("#playersLoginButton", "Login",
                        "After entering both players’ names and passwords, press Login to continue."),
                new OnboardingStep("#skipLoginButton", "Play as guest",
                        "Skip enters Guest Mode. Your match won’t be linked to registered player accounts."),
                new OnboardingStep("#forgotPasswordText", "Forgot password",
                        "Enter your email to receive a one-time password (OTP). Use it as your password on this login screen.")
        );

        OnboardingManager.runWithPolicy(
                "onboarding.login",
                mainPane,
                loginSteps,
                OnboardingPolicy.ALWAYS,
                null
        );
    
    }

    private void setLoginMode(boolean showPlayers, boolean animate) {
        if (playerLoginCard == null || adminLoginCard == null) return;

        if (!animate) {
            playerLoginCard.setVisible(showPlayers);
            playerLoginCard.setManaged(showPlayers);
            adminLoginCard.setVisible(!showPlayers);
            adminLoginCard.setManaged(!showPlayers);
        } else {
            if (showPlayers) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), adminLoginCard);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), playerLoginCard);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                fadeOut.setOnFinished(e -> {
                    adminLoginCard.setVisible(false);
                    adminLoginCard.setManaged(false);
                    playerLoginCard.setVisible(true);
                    playerLoginCard.setManaged(true);
                    fadeIn.play();
                });

                fadeOut.play();
            } else {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), playerLoginCard);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), adminLoginCard);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                fadeOut.setOnFinished(e -> {
                    playerLoginCard.setVisible(false);
                    playerLoginCard.setManaged(false);
                    adminLoginCard.setVisible(true);
                    adminLoginCard.setManaged(true);
                    fadeIn.play();
                });

                fadeOut.play();
            }
        }

        if (playersTab != null && adminTab != null) {
            if (showPlayers) {
                playersTab.setStyle("-fx-fill: white; -fx-underline: true; -fx-cursor: hand;");
                adminTab.setStyle("-fx-fill: #7dd3fc; -fx-underline: false; -fx-cursor: hand;");
            } else {
                playersTab.setStyle("-fx-fill: #7dd3fc; -fx-underline: false; -fx-cursor: hand;");
                adminTab.setStyle("-fx-fill: white; -fx-underline: true; -fx-cursor: hand;");
            }
        }
    }

    @FXML
    private void showPlayersLogin() {
        playClickSound();
        setLoginMode(true, true);
    }

    @FXML
    private void showAdminLogin() {
        playClickSound();
        setLoginMode(false, true);
    }

    @FXML
    private void onPlayersLoginClicked() {
        SoundManager.playClick();

        String p1Name = (p1NameField.getText() == null) ? "" : p1NameField.getText().trim();
        String p2Name = (p2NameField.getText() == null) ? "" : p2NameField.getText().trim();
        String p1Pass = (p1PasswordField.getText() == null) ? "" : p1PasswordField.getText().trim();
        String p2Pass = (p2PasswordField.getText() == null) ? "" : p2PasswordField.getText().trim();

        if (p1Name.isEmpty() || p2Name.isEmpty() ||
                p1Pass.isEmpty() || p2Pass.isEmpty()) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Both players must enter name and password.");                
            return;
        }

        SysData sys = SysData.getInstance();

        Player p1 = sys.findPlayerByOfficialName(p1Name);
        Player p2 = sys.findPlayerByOfficialName(p2Name);

        if (p1 == null) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Player 1 is not a registered user");                 
            return;
        }

        if (p2 == null) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Player 2 is not a registered user.");                 
            return;
        }

        if (!p1.checkPassword(p1Pass)) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Incorrect password for player 1.");                 
            return;
        }

        if (!p2.checkPassword(p2Pass)) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Incorrect password for player 2.");                 
            return;
        }

        if (p1 == p2) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Player 1 and 2 must be different users.");                 
            return;
        }

        SessionManager.setPlayers(p1, p1Name, p2, p2Name);
        goToMainPage(false);
    }

    @FXML
    private void onAdminLogin() {
        SoundManager.playClick();

        String adminName = adminNameField.getText() == null ? "" : adminNameField.getText().trim();
        String adminPass = adminPasswordField.getText() == null ? "" : adminPasswordField.getText().trim();

        Player admin = SysData.getInstance().findPlayerByOfficialName(adminName);
        
        AdminLoginResult result = evaluateAdminLogin(admin, adminPass);
        
        if (result == AdminLoginResult.INVALID_CREDENTIALS) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Invalid Name or Password.");                 
         	return;
        }
        
        if (result == AdminLoginResult.NOT_ADMIN) {
         	DialogUtil.show(AlertType.ERROR, null, "Login failed", "Not a saved Admin.");                  
            return;
        }

        SessionManager.setLoggedInUser(admin);
        goToMainPage(true);
    }

    private void goToMainPage(boolean asAdmin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.setStage(stage);

            Stage currentStage = (Stage) mainPane.getScene().getWindow();
            currentStage.setScene(new Scene(root));
            currentStage.centerOnScreen();
            currentStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void onP1PasswordPress() {
        SoundManager.playClick();
        if (p1PasswordVisibleField == null || p1PasswordField == null) return;

        p1PasswordVisibleField.setVisible(true);
        p1PasswordVisibleField.setManaged(true);

        p1PasswordField.setVisible(false);
        p1PasswordField.setManaged(false);

        if (p1EyeIcon != null) {
            p1EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/hide.png")));
        }
    }

    @FXML
    private void onP1PasswordRelease() {
        if (p1PasswordVisibleField == null || p1PasswordField == null) return;

        p1PasswordField.setVisible(true);
        p1PasswordField.setManaged(true);

        p1PasswordVisibleField.setVisible(false);
        p1PasswordVisibleField.setManaged(false);

        if (p1EyeIcon != null) {
            p1EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }
    }

    @FXML
    private void onP2PasswordPress() {
        SoundManager.playClick();
        if (p2PasswordVisibleField == null || p2PasswordField == null) return;

        p2PasswordVisibleField.setVisible(true);
        p2PasswordVisibleField.setManaged(true);

        p2PasswordField.setVisible(false);
        p2PasswordField.setManaged(false);

        if (p2EyeIcon != null) {
            p2EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/hide.png")));
        }
    }

    @FXML
    private void onP2PasswordRelease() {
        if (p2PasswordVisibleField == null || p2PasswordField == null) return;

        p2PasswordField.setVisible(true);
        p2PasswordField.setManaged(true);

        p2PasswordVisibleField.setVisible(false);
        p2PasswordVisibleField.setManaged(false);

        if (p2EyeIcon != null) {
            p2EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }
    }

    @FXML
    private void onAdminPasswordPress() {
        SoundManager.playClick();
        if (adminPasswordVisibleField == null || adminPasswordField == null) return;

        adminPasswordVisibleField.setVisible(true);
        adminPasswordVisibleField.setManaged(true);

        adminPasswordField.setVisible(false);
        adminPasswordField.setManaged(false);

        if (adminEyeIcon != null) {
            adminEyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/hide.png")));
        }
    }

    @FXML
    private void onAdminPasswordRelease() {
        if (adminPasswordVisibleField == null || adminPasswordField == null) return;

        adminPasswordField.setVisible(true);
        adminPasswordField.setManaged(true);

        adminPasswordVisibleField.setVisible(false);
        adminPasswordVisibleField.setManaged(false);

        if (adminEyeIcon != null) {
            adminEyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }
    }


    @FXML
    private void onSignUpClicked() {
        playClickSound();

        try {
            Stage stage = (Stage) mainPane.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/signup_view.fxml"));
            Pane newRoot = loader.load();

            SignupController controller = loader.getController();
            controller.setStage(stage);

            stage.setScene(new Scene(newRoot));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onSkipLoginClicked() {
        playClickSound();
        SessionManager.clear();
        SessionManager.setGuestMode(true);
        goToMainPage(false);
    }

    @FXML
    private void onForgotPasswordClicked() {
        playClickSound();  

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Reset your password");
        dialog.setContentText("Please enter your email:");
        dialog.getDialogPane().getStylesheets().add(PlayLoginController.class.getResource("/css/theme.css").toExternalForm());
      
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            // user cancelled
            return;
        }

        String email = result.get().trim();
        if (email.isEmpty()) {
         	DialogUtil.show(AlertType.ERROR, null, "Error", "Email cannot be empty.");                
            return;
        }

        SysData sysData = SysData.getInstance();
        Player player = sysData.findPlayerByEmail(email);

        if (player == null) {
         	DialogUtil.show(AlertType.ERROR, null, "Unknown Email", "No player found for this email.");                
            return;
        }

        if (adminLoginCard.isVisible()) {
            if (player.getRole() != Role.ADMIN) {
             	DialogUtil.show(AlertType.ERROR, null, "Access Denied", "This email does not belong to an admin account.");                 
                return;
            }
        }

        String otp = generateOneTimePassword();

        try {
            sysData.updatePlayerPassword(email, otp);
        } catch (Exception e) {
            e.printStackTrace();
         	DialogUtil.show(AlertType.ERROR, null, "Error", "Could not update password. Please try again.");                 
            return;
        }

        try {
            EmailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            e.printStackTrace();
        }
     	DialogUtil.show(AlertType.INFORMATION, "One-time password has been set", "Password Reset",   "A one-time password has been sent to " + email +  ".\nUse it as your password on the login screen.\n" +  "You can change it later if you want.");                 
    }


    private String generateOneTimePassword() {
        int code = (int) (Math.random() * 900_000) + 100_000; 
        return Integer.toString(code);
    }

    @FXML
    private void playClickSound() {
        SoundManager.playClick();
    }

    
    public enum AdminLoginResult {
        INVALID_CREDENTIALS,   // admin == null OR wrong password
        NOT_ADMIN,             // credentials are ok, but role is not admin
        SUCCESS                // credentials are ok + is admin
    }

    static AdminLoginResult evaluateAdminLogin(Player admin, String adminPass) {

        // Normalize pass to avoid null issues in tests
        String pass = (adminPass == null) ? "" : adminPass;

        if (admin == null  || !admin.checkPassword(pass)) {
            return AdminLoginResult.INVALID_CREDENTIALS;
        }
        
        if (!admin.isAdmin()) {
            return AdminLoginResult.NOT_ADMIN;
        }

        return AdminLoginResult.SUCCESS;
    }

}
