package control;

import java.io.IOException;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Player;
import model.Role;
import model.SysData;
import util.AvatarManager;
import util.DialogUtil;
import util.SessionManager;
import util.SoundManager;
import util.UIAnimations;

public class SignupController {

    @FXML private GridPane signupRoot;
    @FXML private AnchorPane anchor;   

    @FXML private ImageView playerAvatar;

    @FXML private ImageView img1;
    @FXML private ImageView img2;
    @FXML private ImageView img3;
    @FXML private ImageView img4;
    @FXML private ImageView img5;
    @FXML private ImageView img6;
    @FXML private ImageView img7;
    @FXML private ImageView img8;
    @FXML private ImageView img9;
    @FXML private ImageView img10;
    @FXML private ImageView img11;
    @FXML private ImageView img12;
    @FXML private ImageView img13;

    @FXML private TextField NameSignup;
    @FXML private TextField EmailSignup;

    @FXML private PasswordField PasswordSignup;
    @FXML private TextField    passwordTextFieldSignUp;
    @FXML private ImageView    eyeIconSignup;

    @FXML private PasswordField rePasswordSignup1;
    @FXML private TextField     repasswordTextFieldSignUp1;
    @FXML private ImageView     eyeIconReSignup;

    private AvatarManager avatarManager;
    private Stage stage;

    public SignupController() {
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        avatarManager = new AvatarManager(playerAvatar, null);
        avatarManager.setupThumbnails(
                img1, img2, img3, img4, img5, img6,
                img7, img8, img9, img10, img11, img12, img13
        );
        avatarManager.selectPlayer(1);

        // Bind password & visible text fields
        passwordTextFieldSignUp.textProperty().bindBidirectional(PasswordSignup.textProperty());
        repasswordTextFieldSignUp1.textProperty().bindBidirectional(rePasswordSignup1.textProperty());

        // Start with visible-text fields hidden (handled already in FXML via visible=false + managed=false
        // but we force it in case SceneBuilder toggled anything)
        passwordTextFieldSignUp.setVisible(false);
        passwordTextFieldSignUp.setManaged(false);
        repasswordTextFieldSignUp1.setVisible(false);
        repasswordTextFieldSignUp1.setManaged(false);

        // Set initial icons
        eyeIconSignup.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        eyeIconReSignup.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));

        // Play intro animation
        UIAnimations.fadeInWithSlide(signupRoot, anchor);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    @FXML
    private void playClickSound() {
        SoundManager.playClick();
    }

    @FXML
    private void onPlayerAreaClicked() {
        avatarManager.selectPlayer(1);
        playClickSound();
    }

    @FXML
    private void onAvatarClicked(MouseEvent event) {
        playClickSound();
        avatarManager.handleAvatarPaneClick(event);
    }

    @FXML
    private void onPlus() {
        playClickSound();
        Stage stage = (Stage) playerAvatar.getScene().getWindow();
        avatarManager.handlePlus(stage);
    }

    @FXML
    private void onSignUpBtnClicked() {
        playClickSound();

        String officialName = (NameSignup.getText() == null) ? "" : NameSignup.getText().trim();
        String email        = (EmailSignup.getText() == null) ? "" : EmailSignup.getText().trim();
        String password     = (PasswordSignup.getText() == null) ? "" : PasswordSignup.getText().trim();
        String rePassword   = (rePasswordSignup1.getText() == null) ? "" : rePasswordSignup1.getText().trim();

        if (officialName.isEmpty() || email.isEmpty() || password.isEmpty() || rePassword.isEmpty()) {
         	DialogUtil.show(AlertType.ERROR, null, "Missing Information","Please fill in all fields");
            return;
        }
        if (!isValidEmail(email)) {
         	DialogUtil.show(AlertType.ERROR, null, "Invalid Email","Please enter a valid email address.");
         	return;
        }
        if (!password.equals(rePassword)) {
         	DialogUtil.show(AlertType.ERROR, null, "Password mismatch","Password and confirmation do not match.");
            return;
        }
        if (password.length() < 4) {
         	DialogUtil.show(AlertType.ERROR, null, "Weak Password","Password must be at least 4 characters long.");
            return;
        }

        SysData sysData = SysData.getInstance();

        if (sysData.findPlayerByOfficialName(officialName) != null) {
         	DialogUtil.show(AlertType.ERROR, null, "Name Already Exists","Official name is already in use. Please choose a different name.");
            return;
        }

        // avatar id from AvatarManager
        String avatarId = avatarManager.getSelectedAvatarIdForPlayer1();
        if (avatarId == null || avatarId.isBlank()) {
            avatarId = "S4.png";
        }

        Player newPlayer;
        try {
            newPlayer = sysData.createPlayer(
                    officialName,
                    email,
                    password,
                    Role.PLAYER,
                    avatarId
            );
        } catch (IllegalArgumentException ex) {
         	DialogUtil.show(AlertType.ERROR, null, "Sign up Failed",ex.getMessage());
            return;
        }

        SessionManager.setLoggedInUser(newPlayer);
     	DialogUtil.show(AlertType.INFORMATION, null, "Sign-Up successful","Account created successfully. \n You can now log in and play.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/players_login_view.fxml"));
            Parent root = loader.load();

            PlayLoginController loginController = loader.getController();
            loginController.setStage(stage);

            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
         	DialogUtil.show(AlertType.ERROR, null, "Navigation Error","Could not return to login screen.");
        }
    }


    // Password #1: press to show, release to hide
    @FXML
    private void onPasswordSignupPress() {
        SoundManager.playClick();

        // show text field, hide password field
        passwordTextFieldSignUp.setVisible(true);
        passwordTextFieldSignUp.setManaged(true);

        PasswordSignup.setVisible(false);
        PasswordSignup.setManaged(false);

        eyeIconSignup.setImage(new Image(
                getClass().getResourceAsStream("/Images/hide.png")));
    }

    @FXML
    private void onPasswordSignupRelease() {
        // back to hidden
        PasswordSignup.setVisible(true);
        PasswordSignup.setManaged(true);

        passwordTextFieldSignUp.setVisible(false);
        passwordTextFieldSignUp.setManaged(false);

        eyeIconSignup.setImage(new Image(
                getClass().getResourceAsStream("/Images/view.png")));
    }

    // Password #2: press to show, release to hide
    @FXML
    private void onRePasswordSignupPress() {
        SoundManager.playClick();

        repasswordTextFieldSignUp1.setVisible(true);
        repasswordTextFieldSignUp1.setManaged(true);

        rePasswordSignup1.setVisible(false);
        rePasswordSignup1.setManaged(false);

        eyeIconReSignup.setImage(new Image(
                getClass().getResourceAsStream("/Images/hide.png")));
    }

    @FXML
    private void onRePasswordSignupRelease() {
        rePasswordSignup1.setVisible(true);
        rePasswordSignup1.setManaged(true);

        repasswordTextFieldSignUp1.setVisible(false);
        repasswordTextFieldSignUp1.setManaged(false);

        eyeIconReSignup.setImage(new Image(
                getClass().getResourceAsStream("/Images/view.png")));
    }
    
    @FXML
    private void onBackToSignUPClicked() {
        SoundManager.playClick();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/players_login_view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signupRoot.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
