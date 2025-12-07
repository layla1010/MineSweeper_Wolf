package control;

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
import util.SessionManager;
import util.SoundManager;

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

        passwordTextFieldSignUp.textProperty().bindBidirectional(PasswordSignup.textProperty());
        repasswordTextFieldSignUp1.textProperty().bindBidirectional(rePasswordSignup1.textProperty());

        
        passwordTextFieldSignUp.setVisible(false);
        passwordTextFieldSignUp.setManaged(false);
        repasswordTextFieldSignUp1.setVisible(false);
        repasswordTextFieldSignUp1.setManaged(false);

        eyeIconSignup.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));
        eyeIconReSignup.setImage(new Image(getClass().getResourceAsStream("/Images/view.png")));

        playIntroAnimation();
    }

    private void playIntroAnimation() {
        if (signupRoot == null) return;

        signupRoot.setOpacity(0.0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), signupRoot);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        if (anchor != null) {
            anchor.setTranslateY(30);
            TranslateTransition slide = new TranslateTransition(Duration.millis(600), anchor);
            slide.setFromY(30);
            slide.setToY(0);

            ParallelTransition pt = new ParallelTransition(fade, slide);
            pt.play();
        } else {
            fade.play();
        }
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
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
            showError("Missing Information", "Please fill in all fields.");
            return;
        }
        if (!isValidEmail(email)) {
            showError("Invalid Email", "Please enter a valid email address.");
            return;
        }
        if (!password.equals(rePassword)) {
            showError("Password Mismatch", "Password and confirmation do not match.");
            return;
        }
        if (password.length() < 4) {
            showError("Weak Password", "Password must be at least 4 characters long.");
            return;
        }

        SysData sysData = SysData.getInstance();

        if (sysData.findPlayerByOfficialName(officialName) != null) {
            showError("Name Already Exists",
                    "Official name is already in use. Please choose a different name.");
            return;
        }

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
            showError("Sign-Up Failed", ex.getMessage());
            return;
        }

        SessionManager.setLoggedInUser(newPlayer);

        showInfo("Sign-Up Successful",
                "Account created successfully.\nYou can now log in and play.");

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
            showError("Navigation Error", "Could not return to login screen.");
        }
    }


    @FXML
    private void onPasswordSignupPress() {
        SoundManager.playClick();

        passwordTextFieldSignUp.setVisible(true);
        passwordTextFieldSignUp.setManaged(true);

        PasswordSignup.setVisible(false);
        PasswordSignup.setManaged(false);

        eyeIconSignup.setImage(new Image(
                getClass().getResourceAsStream("/Images/hide.png")));
    }

    @FXML
    private void onPasswordSignupRelease() {
        PasswordSignup.setVisible(true);
        PasswordSignup.setManaged(true);

        passwordTextFieldSignUp.setVisible(false);
        passwordTextFieldSignUp.setManaged(false);

        eyeIconSignup.setImage(new Image(
                getClass().getResourceAsStream("/Images/view.png")));
    }

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
}
