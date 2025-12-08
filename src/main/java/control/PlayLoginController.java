	package control;
	
	
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
	
	   
	    
	    @FXML private TextField p1NameField;
	    @FXML private TextField p2NameField;
	
	    
	    
	    @FXML private GridPane playerLoginCard;
	    @FXML private AnchorPane adminLoginCard;
	
	    @FXML private Text playersTab;
	    @FXML private Text adminTab;
	    @FXML private TextField adminNameField;
	
	
	    @FXML private PasswordField p1PasswordField;
	    @FXML private TextField     p1PasswordVisibleField;
	    @FXML private Button        p1TogglePasswordButton;
	    @FXML private ImageView     p1EyeIcon;
	    private boolean p1PasswordVisible = false;
	
	    @FXML private PasswordField p2PasswordField;
	    @FXML private TextField     p2PasswordVisibleField;
	    @FXML private Button        p2TogglePasswordButton;
	    @FXML private ImageView     p2EyeIcon;
	    private boolean p2PasswordVisible = false;
	    
	    @FXML private PasswordField adminPasswordField;
	    @FXML private TextField     adminPasswordVisibleField;
	    @FXML private Button        adminTogglePasswordButton;
	    @FXML private ImageView     adminEyeIcon;
	    private boolean adminPasswordVisible = false;
	    
	    @FXML private Text forgotPasswordText;
	    
	    @FXML private Text forgotPasswordTextAdmin;

	    
	    private Stage stage;
	
	    public void setStage(Stage stage) {
	        this.stage = stage;
	    }
	
	    @FXML
	    private void initialize() {
	    	showPlayersLogin();
	
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
	            Alert a = new Alert(AlertType.ERROR);
	            a.setTitle("Login failed");
	            a.setHeaderText(null);
	            a.setContentText("Player 1 is not a registered user.");
	            a.showAndWait();
	            return;
	        }

	        if (p2 == null) {
	            Alert a = new Alert(AlertType.ERROR);
	            a.setTitle("Login failed");
	            a.setHeaderText(null);
	            a.setContentText("Player 2 is not a registered user.");
	            a.showAndWait();
	            return;
	        }

	        if (!p1.checkPassword(p1Pass)) {
	            Alert a = new Alert(AlertType.ERROR);
	            a.setTitle("Login failed");
	            a.setHeaderText(null);
	            a.setContentText("Incorrect password for Player 1.");
	            a.showAndWait();
	            return;
	        }

	        if (!p2.checkPassword(p2Pass)) {
	            Alert a = new Alert(AlertType.ERROR);
	            a.setTitle("Login failed");
	            a.setHeaderText(null);
	            a.setContentText("Incorrect password for Player 2.");
	            a.showAndWait();
	            return;
	        }

	        if (p1 == p2) {
	            Alert a = new Alert(AlertType.ERROR);
	            a.setTitle("Login failed");
	            a.setHeaderText(null);
	            a.setContentText("Player 1 and Player 2 must be different users.");
	            a.showAndWait();
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

	        if (admin == null || !admin.checkPassword(adminPass)) {
	            Alert a = new Alert(AlertType.ERROR);
	            a.setTitle("Login failed");
	            a.setHeaderText(null);
	            a.setContentText("Invalid Name or Password.");
	            a.showAndWait();
	            return;
	            } 
	        if (!admin.isAdmin()) {
		            Alert a = new Alert(AlertType.ERROR);
		            a.setTitle("Login failed");
		            a.setHeaderText(null);
		            a.setContentText("Not a saved Admin.");
		            a.showAndWait();
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
	
	            Stage stage = (Stage) playerLoginCard.getScene().getWindow();
	            stage.setScene(new Scene(root));
	            stage.centerOnScreen();
	            stage.show();
	
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    
	    @FXML
	    private void playClickSound() {
	        SoundManager.playClick();
	    }
	    
	    @FXML
	    private void showPlayersLogin() {
	        playerLoginCard.setVisible(true);
	        playerLoginCard.setManaged(true);
	        adminLoginCard.setVisible(false);
	        adminLoginCard.setManaged(false);
	
	        playersTab.setStyle("-fx-fill: white; -fx-underline: true; -fx-cursor: hand;");
	        adminTab.setStyle("-fx-fill: #7dd3fc; -fx-underline: false; -fx-cursor: hand;");
	    }
	    
	    @FXML
	    private void showAdminLogin() {
	        playerLoginCard.setVisible(false);
	        playerLoginCard.setManaged(false);
	        adminLoginCard.setVisible(true);
	        adminLoginCard.setManaged(true);
	
	        playersTab.setStyle("-fx-fill: #7dd3fc; -fx-underline: false; -fx-cursor: hand;");
	        adminTab.setStyle("-fx-fill: white; -fx-underline: true; -fx-cursor: hand;");
	    }
	
	    @FXML
	    private void onToggleP1Password() {
	        playClickSound();
	        p1PasswordVisible = !p1PasswordVisible;
	
	        if (p1PasswordVisible) {
	            p1PasswordVisibleField.setVisible(true);
	            p1PasswordVisibleField.setManaged(true);
	
	            p1PasswordField.setVisible(false);
	            p1PasswordField.setManaged(false);
	
	            if (p1EyeIcon != null) {
	                Image img = new Image(
	                        getClass().getResourceAsStream("/Images/hide.png")
	                );
	                p1EyeIcon.setImage(img);
	            }
	        } else {
	            p1PasswordField.setVisible(true);
	            p1PasswordField.setManaged(true);
	
	            p1PasswordVisibleField.setVisible(false);
	            p1PasswordVisibleField.setManaged(false);
	
	            if (p1EyeIcon != null) {
	                Image img = new Image(
	                        getClass().getResourceAsStream("/Images/view.png")
	                );
	                p1EyeIcon.setImage(img);
	            }
	        }
	    }
	
	    @FXML
	    private void onToggleP2Password() {
	        playClickSound();
	        p2PasswordVisible = !p2PasswordVisible;
	
	        if (p2PasswordVisible) {
	            p2PasswordVisibleField.setVisible(true);
	            p2PasswordVisibleField.setManaged(true);
	
	            p2PasswordField.setVisible(false);
	            p2PasswordField.setManaged(false);
	
	            if (p2EyeIcon != null) {
	                Image img = new Image(
	                        getClass().getResourceAsStream("/Images/hide.png")
	                );
	                p2EyeIcon.setImage(img);
	            }
	        } else {
	            p2PasswordField.setVisible(true);
	            p2PasswordField.setManaged(true);
	
	            p2PasswordVisibleField.setVisible(false);
	            p2PasswordVisibleField.setManaged(false);
	
	            if (p2EyeIcon != null) {
	                Image img = new Image(
	                        getClass().getResourceAsStream("/Images/view.png")
	                );
	                p2EyeIcon.setImage(img);
	            }
	        }
	    }
	    
	    @FXML
	    private void onToggleAdminPassword() {
	        playClickSound();
	        adminPasswordVisible = !adminPasswordVisible;
	
	        if (adminPasswordVisible) {
	            adminPasswordVisibleField.setVisible(true);
	            adminPasswordVisibleField.setManaged(true);
	
	            adminPasswordField.setVisible(false);
	            adminPasswordField.setManaged(false);
	
	            if (adminEyeIcon != null) {
	                Image img = new Image(
	                        getClass().getResourceAsStream("/Images/hide.png")
	                );
	                adminEyeIcon.setImage(img);
	            }
	        } else {
	            adminPasswordField.setVisible(true);
	            adminPasswordField.setManaged(true);
	
	            adminPasswordVisibleField.setVisible(false);
	            adminPasswordVisibleField.setManaged(false);
	
	            if (adminEyeIcon != null) {
	                Image img = new Image(
	                        getClass().getResourceAsStream("/Images/view.png")
	                );
	                adminEyeIcon.setImage(img);
	            }
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
	            stage.show();
	            stage.centerOnScreen();
	
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    public void onSkipLoginClicked () {
	    	
	    }
	    
	    
	    @FXML
	    private void onForgotPasswordClicked() {
	        playClickSound();  // optional sound

	        // 1. Ask for email
	        TextInputDialog dialog = new TextInputDialog();
	        dialog.setTitle("Forgot Password");
	        dialog.setHeaderText("Reset your password");
	        dialog.setContentText("Please enter your email:");

	        Optional<String> result = dialog.showAndWait();
	        if (!result.isPresent()) {
	            // user cancelled
	            return;
	        }

	        String email = result.get().trim();
	        if (email.isEmpty()) {
	            Alert alert = new Alert(AlertType.ERROR);
	            alert.setTitle("Error");
	            alert.setHeaderText(null);
	            alert.setContentText("Email cannot be empty.");
	            alert.showAndWait();
	            return;
	        }

	        // 2. Look up player by email (SysData must have this method)
	        SysData sysData = SysData.getInstance();
	        Player player = sysData.findPlayerByEmail(email);

	        if (player == null) {
	            // email is NOT for a player
	            Alert alert = new Alert(AlertType.ERROR);
	            alert.setTitle("Unknown Email");
	            alert.setHeaderText(null);
	            alert.setContentText("No player found for this email.");
	            alert.showAndWait();
	            return;
	        }
	        
	        if (adminLoginCard.isVisible()) {
	            if (player.getRole() != Role.ADMIN) {
	                Alert alert = new Alert(AlertType.ERROR);
	                alert.setTitle("Access Denied");
	                alert.setHeaderText(null);
	                alert.setContentText("This email does not belong to an Admin account.");
	                alert.showAndWait();
	                return; // do NOT send OTP
	            }
	        }

	        // 3. Generate one-time password (6-digit code)
	        String otp = generateOneTimePassword();

	        // 4. "Send" the OTP to the email (real email sending not implemented here)
	        //    For now, we just print it in console so you can see it while testing.
	        try {
	            EmailService.sendOtpEmail(email, otp);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        // 5. Inform the user
	        Alert info = new Alert(AlertType.INFORMATION);
	        info.setTitle("Password Reset");
	        info.setHeaderText("One-time password sent");
	        info.setContentText("If an account exists for " + email + ", a one-time password has been sent.");
	        info.showAndWait();

	        // TODO LATER: you can open another dialog to enter the OTP and new password,
	        // and then call player.setPassword(newPassword) and save to CSV.
	    }

	    private String generateOneTimePassword() {
	        int code = (int) (Math.random() * 900_000) + 100_000; // 100000–999999
	        return Integer.toString(code);
	    }

	}
