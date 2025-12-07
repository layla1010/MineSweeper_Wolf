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
	import javafx.scene.input.MouseEvent;
	import javafx.scene.layout.AnchorPane;
	import javafx.scene.layout.GridPane;
	import javafx.scene.layout.Pane;
	import javafx.scene.text.Text;
	import javafx.stage.Stage;
	import util.AvatarManager;
	import util.SoundManager;
	
	public class PlayLoginController {
		
		@FXML private AnchorPane mainPane;
	
	   
	    
	    @FXML private TextField p1NameField;
	    @FXML private TextField p2NameField;
	
	    
	    
	    @FXML private GridPane playerLoginCard;
	    @FXML private AnchorPane adminLoginCard;
	
	    @FXML private Text playersTab;
	    @FXML private Text adminTab;
	    @FXML private TextField adminNameField;
	
	    private AvatarManager avatarManager;
	
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
	
	        // TO DO: validate via CSV
	        if (p1Name.isEmpty() || p2Name.isEmpty()) {
	            // show error alert, etc.
	            // (I already have showError in other controllers; I can reuse a util)
	            return;
	        }
	
	        // Save to some shared session (pseudo-code)
	        // PlayerSession.setPlayers(p1Name, p1Pass, p2Name, p2Pass);
	
	        goToMainPage(false); 
	    }
	    
	    @FXML
	    private void onAdminLogin() {
	        SoundManager.playClick(); 
	
	        String adminName = adminNameField.getText() == null ? "" : adminNameField.getText().trim();
	        String adminPass = adminPasswordField.getText() == null ? "" : adminPasswordField.getText().trim();
	
	        // TO DO: validate admin credentials (strict)
	        // if (!"admin".equals(adminName) || !"1234".equals(adminPass)) { show error; return; }
	
	        // Save admin in session if needed
	        // PlayerSession.setAdmin(adminName);
	
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
	}
