package control;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.Player;
import model.Role;
import model.SysData;
import util.AvatarManager;
import util.DialogUtil;
import util.SessionManager;
import util.SoundManager;

public class SignupController {

	 private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
	 private static final Logger LOG = Logger.getLogger(SignupController.class.getName());
	 private static final int MAX_NAME_LEN = 7;


	
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

 
    private Stage resolveStage() {
        if (stage != null) return stage;
        if (signupRoot != null && signupRoot.getScene() != null) {
            return (Stage) signupRoot.getScene().getWindow();
        }
        return null;
    }

    private Image loadIcon(String path) {
        var in = getClass().getResourceAsStream(path);
        if (in == null) {
            DialogUtil.show(AlertType.ERROR, null, "Missing Resource", "Missing image: " + path);
            return null;
        }
        return new Image(in);
    }

    
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
        eyeIconSignup.setImage(loadIcon("/Images/view.png"));
        eyeIconReSignup.setImage(loadIcon("/Images/view.png"));

        limitTextLength(NameSignup, MAX_NAME_LEN);

    }
    
    void limitTextLength(TextField tf, int max) {
        if (tf == null) return;
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.length() > max) {
                tf.setText(newV.substring(0, max));
            }
        });
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
        Stage s = resolveStage();
        if (s != null) avatarManager.handlePlus(s);
    }

    @FXML
    private void onSignUpBtnClicked() {
        playClickSound();

        String officialName = (NameSignup.getText() == null) ? "" : NameSignup.getText().trim();
        String email        = (EmailSignup.getText() == null) ? "" : EmailSignup.getText().trim();
        String password     = (PasswordSignup.getText() == null) ? "" : PasswordSignup.getText().trim();
        String rePassword   = (rePasswordSignup1.getText() == null) ? "" : rePasswordSignup1.getText().trim();

        if (officialName.isEmpty() || email.isEmpty() || password.isEmpty() || rePassword.isEmpty()) {
            DialogUtil.show(AlertType.ERROR, null, "Missing Information", "Please fill in all fields");
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            DialogUtil.show(AlertType.ERROR, null, "Invalid Email", "Please enter a valid email address.");
            return;
        }
        if (!password.equals(rePassword)) {
            DialogUtil.show(AlertType.ERROR, null, "Password mismatch", "Password and confirmation do not match.");
            return;
        }
        if (password.length() < 4) {
            DialogUtil.show(AlertType.ERROR, null, "Weak Password", "Password must be at least 4 characters long.");
            return;
        }

        SysData sysData = SysData.getInstance();

        if (sysData.findPlayerByOfficialName(officialName) != null) {
            DialogUtil.show(AlertType.ERROR, null, "Name Already Exists",
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
            DialogUtil.show(AlertType.ERROR, null, "Sign up Failed", ex.getMessage());
            return;
        }

        SessionManager.setLoggedInUser(newPlayer);
        DialogUtil.show(AlertType.INFORMATION, null, "Sign-Up successful",
                "Account created successfully.\nYou can now log in and play.");

        Stage s = resolveStage();
        if (s == null) {
            DialogUtil.show(AlertType.ERROR, null, "Navigation Error", "Could not determine window (Stage).");
            return;
        }

        try {
            util.ViewNavigator.switchTo(s, "/view/players_login_view.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to navigate to login screen after signup", e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation Error", "Could not open the login screen.");
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

        eyeIconSignup.setImage(loadIcon("/Images/hide.png"));
    }

    @FXML
    private void onPasswordSignupRelease() {
        // back to hidden
        PasswordSignup.setVisible(true);
        PasswordSignup.setManaged(true);

        passwordTextFieldSignUp.setVisible(false);
        passwordTextFieldSignUp.setManaged(false);

        eyeIconSignup.setImage(loadIcon("/Images/view.png"));
    }

    // Password #2: press to show, release to hide
    @FXML
    private void onRePasswordSignupPress() {
        SoundManager.playClick();

        repasswordTextFieldSignUp1.setVisible(true);
        repasswordTextFieldSignUp1.setManaged(true);

        rePasswordSignup1.setVisible(false);
        rePasswordSignup1.setManaged(false);

        eyeIconReSignup.setImage(loadIcon("/Images/hide.png"));
    }

    @FXML
    private void onRePasswordSignupRelease() {
        rePasswordSignup1.setVisible(true);
        rePasswordSignup1.setManaged(true);

        repasswordTextFieldSignUp1.setVisible(false);
        repasswordTextFieldSignUp1.setManaged(false);

        eyeIconReSignup.setImage(loadIcon("/Images/view.png"));
    }
    
    @FXML
    private void onBackToSignUPClicked() {
        SoundManager.playClick();

        Stage s = resolveStage();
        if (s == null) {
            DialogUtil.show(AlertType.ERROR, null, "Navigation Error", "Could not determine window (Stage).");
            return;
        }

        try {
            util.ViewNavigator.switchTo(s, "/view/players_login_view.fxml");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to navigate back to login screen", e);
            DialogUtil.show(AlertType.ERROR, null, "Navigation Error", "Could not open the login screen.");
        }
    }

}
