package control;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Player;
import model.Role;
import model.SysData;
import util.DialogUtil;
import util.EmailService;
import util.OnboardingManager;
import util.OnboardingPolicy;
import util.OnboardingStep;
import util.SessionManager;
import util.SoundManager;
import util.UIAnimations;

public class PlayLoginController {

    private static final Logger LOG = Logger.getLogger(PlayLoginController.class.getName());

    private static final Pattern EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private static final SecureRandom RNG = new SecureRandom();

    private static final String ICON_VIEW = "/Images/view.png";
    private static final String ICON_HIDE = "/Images/hide.png";

    
    private static final int MAX_NAME_LEN = 7;
    
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

    private Stage getStage() {
        return (Stage) mainPane.getScene().getWindow();
    }

    
    private Image loadIcon(String path) {
        var in = getClass().getResourceAsStream(path);
        if (in == null) {
            LOG.severe("Missing image resource: " + path);
            return null;
        }
        return new Image(in);
    }

    @FXML
    private void initialize() {
        bindVisiblePasswords();
        setLoginMode(true, false);
        hideVisiblePasswordFields();

        if (p1EyeIcon != null) p1EyeIcon.setImage(loadIcon(ICON_VIEW));
        if (p2EyeIcon != null) p2EyeIcon.setImage(loadIcon(ICON_VIEW));
        if (adminEyeIcon != null) adminEyeIcon.setImage(loadIcon(ICON_VIEW));
        playIntroAnimation();
        runOnboarding();
        limitTextLength(p1NameField, MAX_NAME_LEN);
        limitTextLength(p2NameField, MAX_NAME_LEN);
        
    }
    
    void limitTextLength(TextField tf, int max) {
        if (tf == null) return;
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.length() > max) {
                tf.setText(newV.substring(0, max));
            }
        });
    }


    private void bindVisiblePasswords() {
        if (p1PasswordField != null && p1PasswordVisibleField != null) {
            p1PasswordVisibleField.textProperty().bindBidirectional(p1PasswordField.textProperty());
        }
        if (p2PasswordField != null && p2PasswordVisibleField != null) {
            p2PasswordVisibleField.textProperty().bindBidirectional(p2PasswordField.textProperty());
        }
        if (adminPasswordField != null && adminPasswordVisibleField != null) {
            adminPasswordVisibleField.textProperty().bindBidirectional(adminPasswordField.textProperty());
        }
    }

    private void hideVisiblePasswordFields() {
        hideField(p1PasswordVisibleField);
        hideField(p2PasswordVisibleField);
        hideField(adminPasswordVisibleField);
    }

    private void hideField(TextField tf) {
        if (tf == null) return;
        tf.setVisible(false);
        tf.setManaged(false);
    }

    private void playIntroAnimation() {
        if (mainPane == null) return;
        mainPane.setOpacity(0.0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), mainPane);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

   private void runOnboarding() {

    // UI init for login screen (icons/background/animation)
    if (p1EyeIcon != null) {
        p1EyeIcon.setImage(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/Images/view.png"),
                        "Missing resource: /Images/view.png")
        ));
    }
    if (p2EyeIcon != null) {
        p2EyeIcon.setImage(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/Images/view.png"),
                        "Missing resource: /Images/view.png")
        ));
    }
    if (adminEyeIcon != null) {
        adminEyeIcon.setImage(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/Images/view.png"),
                        "Missing resource: /Images/view.png")
        ));
    }

/*    if (mainPane != null) {
        mainPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2, #f093fb);"
        );*/
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

        playerLoginCard.setVisible(showPlayers);
        playerLoginCard.setManaged(showPlayers);

        adminLoginCard.setVisible(!showPlayers);
        adminLoginCard.setManaged(!showPlayers);

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

        String p1Name = safeTrim(p1NameField);
        String p2Name = safeTrim(p2NameField);
        String p1Pass = safeTrim(p1PasswordField);
        String p2Pass = safeTrim(p2PasswordField);
 
        if (p1Name.isEmpty() || p2Name.isEmpty() || p1Pass.isEmpty() || p2Pass.isEmpty()) {
            DialogUtil.show(AlertType.ERROR, null, "Login failed", "Both players must enter name and password.");
            return;
        }

        SysData sys = SysData.getInstance();
        Player p1 = sys.findPlayerByOfficialName(p1Name);
        Player p2 = sys.findPlayerByOfficialName(p2Name);

        if (p1 == null) {
            DialogUtil.show(AlertType.ERROR, null, "Login failed", "Player 1 is not a registered user.");
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

        String adminName = safeTrim(adminNameField);
        String adminPass = safeTrim(adminPasswordField);

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
            Stage s = getStage();
            util.ViewNavigator.switchTo(s, "/view/main_view.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to navigate to main_view", e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation Error", "Could not open the main screen.");
        }
    }


    @FXML
    private void onSignUpClicked() {
        playClickSound();
        try {
            Stage s = getStage();
            util.ViewNavigator.switchTo(s, "/view/signup_view.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to open signup_view", e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation Error", "Could not open sign-up screen.");
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

        // 1) Ask for email (should be styled like the rest)
        TextInputDialog emailDialog = new TextInputDialog();
        DialogUtil.applyDialogCss(emailDialog);
        emailDialog.getEditor().getStyleClass().add("glass-input");

        emailDialog.setTitle("Forgot Password");
        emailDialog.setHeaderText("Reset your password");
        emailDialog.setContentText("Please enter your email:");

        Optional<String> result = emailDialog.showAndWait();
        if (result.isEmpty()) return;

        String email = result.get().trim();
        if (email.isEmpty()) {
            DialogUtil.show(AlertType.ERROR, null, "Error", "Email cannot be empty.");
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            DialogUtil.show(AlertType.ERROR, null, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        SysData sysData = SysData.getInstance();
        Player player = sysData.findPlayerByEmail(email);

        if (player == null) {
            DialogUtil.show(AlertType.ERROR, null, "Unknown Email", "No player found for this email.");
            return;
        }

        if (adminLoginCard != null && adminLoginCard.isVisible()) {
            if (player.getRole() != Role.ADMIN) {
                DialogUtil.show(AlertType.ERROR, null, "Access Denied",
                        "This email does not belong to an admin account.");
                return;
            }
        }

        // 2) Create + send OTP
        String otp = generateOneTimePassword();
        try {
            sysData.createPasswordResetOtp(email, otp, 10);
            EmailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create/send reset OTP for: " + email, e);
            DialogUtil.show(AlertType.ERROR, null, "Email Error",
                    "Failed to send the reset code. Please try again later.");
            return;
        }

        // 3) Verify OTP
        Optional<String> enteredOtpOpt = DialogUtil.promptForOtp();
        if (enteredOtpOpt.isEmpty()) return;

        String enteredOtp = enteredOtpOpt.get().trim();
        if (!sysData.verifyPasswordResetOtp(email, enteredOtp)) {
            DialogUtil.show(AlertType.ERROR, null, "Invalid Code", "The code is incorrect or expired.");
            return;
        }

        // 4) Set new password
        Optional<String> newPassOpt = DialogUtil.promptForNewPasswordTwice();
        if (newPassOpt.isEmpty()) return;

        try {
            sysData.updatePlayerPassword(email, newPassOpt.get());
            sysData.clearPasswordResetOtp(email);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to update password / clear OTP for: " + email, e);
            DialogUtil.show(AlertType.ERROR, null, "Error", "Password update failed. Please try again.");
            return;
        }

        DialogUtil.show(AlertType.INFORMATION, null, "Password Updated",
                "You can now log in with your new password.");
    }


    private String generateOneTimePassword() {
        int code = RNG.nextInt(900_000) + 100_000; // 6 digits
        return Integer.toString(code);
    }

    @FXML
    private void playClickSound() {
        SoundManager.playClick();
    }

    private String safeTrim(TextField tf) {
        return tf == null || tf.getText() == null ? "" : tf.getText().trim();
    }

    private String safeTrim(PasswordField pf) {
        return pf == null || pf.getText() == null ? "" : pf.getText().trim();
    }


    private void setPasswordVisible(boolean visible, PasswordField hiddenField, TextField visibleField, ImageView eye) {
        if (hiddenField == null || visibleField == null) return;

        visibleField.setVisible(visible);
        visibleField.setManaged(visible);

        hiddenField.setVisible(!visible);
        hiddenField.setManaged(!visible);

        if (eye != null) eye.setImage(loadIcon(visible ? ICON_HIDE : ICON_VIEW));
    }

    @FXML private void onP1PasswordPress()    { SoundManager.playClick(); setPasswordVisible(true,  p1PasswordField, p1PasswordVisibleField, p1EyeIcon); }
    @FXML private void onP1PasswordRelease()  { setPasswordVisible(false, p1PasswordField, p1PasswordVisibleField, p1EyeIcon); }

    @FXML private void onP2PasswordPress()    { SoundManager.playClick(); setPasswordVisible(true,  p2PasswordField, p2PasswordVisibleField, p2EyeIcon); }
    @FXML private void onP2PasswordRelease()  { setPasswordVisible(false, p2PasswordField, p2PasswordVisibleField, p2EyeIcon); }

    @FXML private void onAdminPasswordPress()   { SoundManager.playClick(); setPasswordVisible(true,  adminPasswordField, adminPasswordVisibleField, adminEyeIcon); }
    @FXML private void onAdminPasswordRelease() { setPasswordVisible(false, adminPasswordField, adminPasswordVisibleField, adminEyeIcon); }


    public enum AdminLoginResult {
        INVALID_CREDENTIALS,
        NOT_ADMIN,
        SUCCESS
    }

    static AdminLoginResult evaluateAdminLogin(Player admin, String adminPass) {
        String pass = (adminPass == null) ? "" : adminPass;

        if (admin == null || !admin.checkPassword(pass)) {
            return AdminLoginResult.INVALID_CREDENTIALS;
        }
        if (!admin.isAdmin()) {
            return AdminLoginResult.NOT_ADMIN;
        }
        return AdminLoginResult.SUCCESS;
    }
}
