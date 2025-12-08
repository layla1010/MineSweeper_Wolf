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
import util.EmailService;
import util.SoundManager;

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

    // Players login fields
    @FXML private TextField p1NameField;
    @FXML private TextField p2NameField;

    @FXML private PasswordField p1PasswordField;
    @FXML private TextField     p1PasswordVisibleField;
    @FXML private ImageView     p1EyeIcon;

    @FXML private PasswordField p2PasswordField;
    @FXML private TextField     p2PasswordVisibleField;
    @FXML private ImageView     p2EyeIcon;

    // Admin login fields
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

    // ================== INIT ==================
    @FXML
    private void initialize() {
        // Bind visible text fields to password fields
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

        // Start with players tab
        setLoginMode(true, false);

        // Hide "visible" text fields initially
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

        // Set initial eye icons
        if (p1EyeIcon != null) {
            p1EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }
        if (p2EyeIcon != null) {
            p2EyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }
        if (adminEyeIcon != null) {
            adminEyeIcon.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        }

        // Optional: make background match main page gradient (instead of plain purple)
        if (mainPane != null) {
            mainPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2, #f093fb);"
            );
        }

        playIntroAnimation();
    }

    // ================== ANIMATIONS ==================

    private void playIntroAnimation() {
        if (mainPane == null) return;

        mainPane.setOpacity(0.0);

        FadeTransition fade = new FadeTransition(Duration.millis(600), mainPane);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
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
                // admin -> players
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
                // players -> admin
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

        // Tab styles
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

    // ================== TAB HANDLERS ==================
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

    // ================== PLAYERS LOGIN ==================
    @FXML
    private void onPlayersLoginClicked() {
        SoundManager.playClick();

        String p1Name = (p1NameField.getText() == null) ? "" : p1NameField.getText().trim();
        String p2Name = (p2NameField.getText() == null) ? "" : p2NameField.getText().trim();
        String p1Pass = (p1PasswordField.getText() == null) ? "" : p1PasswordField.getText().trim();
        String p2Pass = (p2PasswordField.getText() == null) ? "" : p2PasswordField.getText().trim();

        if (p1Name.isEmpty() || p2Name.isEmpty() ||
                p1Pass.isEmpty() || p2Pass.isEmpty()) {
            Alert a = new Alert(AlertType.ERROR);
            a.setTitle("Login failed");
            a.setHeaderText(null);
            a.setContentText("Both players must enter name and password.");
            a.showAndWait();
            return;
        }

        SysData sys = SysData.getInstance();

        Player p1 = sys.findPlayerByOfficialName(p1Name);
        Player p2 = sys.findPlayerByOfficialName(p2Name);

        if (p1 == null) {
            showError("Login failed", "Player 1 is not a registered user.");
            return;
        }

        if (p2 == null) {
            showError("Login failed", "Player 2 is not a registered user.");
            return;
        }

        if (!p1.checkPassword(p1Pass)) {
            showError("Login failed", "Incorrect password for Player 1.");
            return;
        }

        if (!p2.checkPassword(p2Pass)) {
            showError("Login failed", "Incorrect password for Player 2.");
            return;
        }

        if (p1 == p2) {
            showError("Login failed", "Player 1 and Player 2 must be different users.");
            return;
        }

        SessionManager.setPlayers(p1, p1Name, p2, p2Name);
        goToMainPage(false);
    }

    // ================== ADMIN LOGIN ==================
    @FXML
    private void onAdminLogin() {
        SoundManager.playClick();

        String adminName = adminNameField.getText() == null ? "" : adminNameField.getText().trim();
        String adminPass = adminPasswordField.getText() == null ? "" : adminPasswordField.getText().trim();

        Player admin = SysData.getInstance().findPlayerByOfficialName(adminName);

        if (admin == null || !admin.checkPassword(adminPass)) {
            showError("Login failed", "Invalid Name or Password.");
            return;
        }
        if (!admin.isAdmin()) {
            showError("Login failed", "Not a saved Admin.");
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

    // ================== PASSWORD VISIBILITY (PRESS & HOLD) ==================

    // ---- Player 1 ----
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

    // ---- Player 2 ----
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

    // ---- Admin ----
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

    // ================== SIGN UP + SKIP + FORGOT ==================

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

    // currently empty – you can decide later what "Skip" should do
    @FXML
    public void onSkipLoginClicked() {
        playClickSound();
        goToMainPage(false);
    }

    @FXML
    private void onForgotPasswordClicked() {
        playClickSound();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Reset your password");
        dialog.setContentText("Please enter your email:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            return;
        }

        String email = result.get().trim();
        if (email.isEmpty()) {
            showError("Error", "Email cannot be empty.");
            return;
        }

        SysData sysData = SysData.getInstance();
        Player player = sysData.findPlayerByEmail(email);

        if (player == null) {
            showError("Unknown Email", "No player found for this email.");
            return;
        }

        // if admin card visible, enforce admin role
        if (adminLoginCard != null && adminLoginCard.isVisible()) {
            if (player.getRole() != Role.ADMIN) {
                showError("Access Denied", "This email does not belong to an Admin account.");
                return;
            }
        }

        String otp = generateOneTimePassword();

        try {
            EmailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Alert info = new Alert(AlertType.INFORMATION);
        info.setTitle("Password Reset");
        info.setHeaderText("One-time password sent");
        info.setContentText("If an account exists for " + email + ", a one-time password has been sent.");
        info.showAndWait();

        // later you can add OTP verification + password change dialog
    }

    private String generateOneTimePassword() {
        int code = (int) (Math.random() * 900_000) + 100_000; // 100000–999999
        return Integer.toString(code);
    }

    // ================== UTIL ==================
    @FXML
    private void playClickSound() {
        SoundManager.playClick();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
