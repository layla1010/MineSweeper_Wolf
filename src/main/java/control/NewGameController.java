package control;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Difficulty;
import model.GameConfig;
import util.SoundManager;
import util.UIAnimations;

public class NewGameController {

    @FXML private Parent root;
    @FXML private TextField player1Nickname;
    @FXML private TextField player2Nickname;
    @FXML private ToggleGroup difficultyGroup;
    @FXML private ToggleButton easyToggle;
    @FXML private ToggleButton medToggle;
    @FXML private ToggleButton hardToggle;
    @FXML private Rectangle recP1;
    @FXML private Rectangle recP2;
    @FXML private ImageView player1avatar;
    @FXML private ImageView player2avatar;
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
    @FXML private Button setUpmusicIsOnButton;

    //Tracks whether each player has chosen an avatar
    private boolean player1AvatarChosen = false;
    private boolean player2AvatarChosen = false;
    //Currently selected avatar tile (for CSS highlighting)
    private StackPane selectedAvatarPane;
    //Which player is currently "active" for avatar picking (1 or 2)
    private int selectedPlayer = 1;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    //Sets player 1 as active by default, wires avatar thumbnails, and applies UI animations to the screen.
    @FXML
    private void initialize() {
        selectPlayer(1);
        setupAvatarThumbnails();
        UIAnimations.applyHoverZoomToAllButtons(root);
        UIAnimations.applyFloatingToCards(root);
        UIAnimations.applyHoverZoomToClass(root);
    }
    //it is a utility method to play a standard click sound.
    @FXML
    private void playClickSound() {
        SoundManager.playClick();
    }
    //Connects all predefined avatar thumbnails (img1..img13) to the click handler so that clicking one will set the avatar for the currently active player.
    private void setupAvatarThumbnails() {
        ImageView[] thumbs = {
            img1, img2, img3, img4, img5, img6,
            img7, img8, img9, img10, img11, img12, img13
        };

        for (ImageView iv : thumbs) {
            if (iv != null) {
                iv.setOnMouseClicked(e -> {
                    playClickSound();
                    setAvatarFromImageView(iv);
                });
            }
        }
    }
    //Assigns the image from a clicked thumbnail to the active player's avatar.
    private void setAvatarFromImageView(ImageView source) {
        Image image = source.getImage();
        if (image == null) return;

        if (selectedPlayer == 1) {
            player1avatar.setImage(image);
            player1AvatarChosen = true;
        } else {
            player2avatar.setImage(image);
            player2AvatarChosen = true;
        }
    }
    //Called when player 1's card/area is clicked. Marks player 1 as the active player for avatar picking and focus.
    @FXML
    private void onPlayer1AreaClicked() {
        selectPlayer(1);
        playClickSound();
        setActivePlayerCard(recP1, recP2, player1Nickname, player2Nickname);
    }
    //Called when player 2's card/area is clicked. Marks player 2 as the active player for avatar picking and focus.
    @FXML
    private void onPlayer2AreaClicked() {
        selectPlayer(2);
        playClickSound();
        setActivePlayerCard(recP2, recP1, player2Nickname, player1Nickname);
    }
    //Updates visual state of the selected player card and focuses the corresponding nickname TextField.
    private void setActivePlayerCard(Rectangle activeCard, Rectangle otherCard, TextField activeField, TextField otherField) {

		otherCard.getStyleClass().remove("player-card-active");
		
		if (!activeCard.getStyleClass().contains("player-card-active")) {
		activeCard.getStyleClass().add("player-card-active");
		}
		
		if (activeField != null) {
		activeField.requestFocus();
		}
		
		activeField.positionCaret(activeField.getText().length());
		activeField.selectAll();
	}
    //Internally sets which player is currently active (1 or 2), and updates the stroke color on the player rectangles as a hint.
    private void selectPlayer(int player) {
        this.selectedPlayer = player;
        if (recP1 != null && recP2 != null) {
            if (player == 1) {
                recP1.setStroke(Color.web("#35E0FF"));
                recP2.setStroke(Color.web("#274B8E"));
            } else {
                recP1.setStroke(Color.web("#274B8E"));
                recP2.setStroke(Color.web("#35E0FF"));
            }
        }
    }

    @FXML
    private void onEasyCardClicked() {
        playClickSound();
        easyToggle.fire();
    }

    @FXML
    private void onMediumCardClicked() {
        playClickSound();
        medToggle.fire();
    }

    @FXML
    private void onHardCardClicked() {
        playClickSound();
        hardToggle.fire();
    }
    //Handles the "Start Game" button and validates: Both player names are filled, a difficulty is selected and both players have chosen avatars
    @FXML
    private void onStartGameClicked() {
        playClickSound();

        String nickname1 = player1Nickname.getText();
        String nickname2 = player2Nickname.getText();

        if (nickname1 == null || nickname1.trim().isEmpty() ||
            nickname2 == null || nickname2.trim().isEmpty()) {
            showError("Please enter both players names.");
            return;
        }

        Toggle selectedToggle = difficultyGroup.getSelectedToggle();
        if (selectedToggle == null) {
            showError("Please select a difficulty level.");
            return;
        }

        if (!player1AvatarChosen || !player2AvatarChosen) {
            showError("Please select an avatar for both players.");
            return;
        }

        Difficulty difficulty;
        if (selectedToggle == easyToggle) {
            difficulty = Difficulty.EASY;
        } else if (selectedToggle == medToggle) {
            difficulty = Difficulty.MEDIUM;
        } else {
            difficulty = Difficulty.HARD;
        }

        GameConfig config = new GameConfig(
                nickname1.trim(),
                nickname2.trim(),
                difficulty
        );

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/board_view.fxml"));
            Parent root = loader.load();

            GameController controller = loader.getController();
            controller.init(config);

            Stage stage = (Stage) player1Nickname.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to start the game due to an internal error.");
        }
    }
    //Handles the "+" button for custom avatar selection Opens a FileChooser so the current active player can pick a local image as his/her avatar
    @FXML
    private void onPlus() {
        playClickSound();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Avatar Image");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = null;
        if (player1avatar != null && player1avatar.getScene() != null) {
            stage = (Stage) player1avatar.getScene().getWindow();
        }

        File file = (stage == null)
            ? fileChooser.showOpenDialog(null)
            : fileChooser.showOpenDialog(stage);

        if (file != null) {
            Image img = new Image(file.toURI().toString());
            if (selectedPlayer == 1) {
                player1avatar.setImage(img);
                player1AvatarChosen = true;
            } else {
                player2avatar.setImage(img);
                player2AvatarChosen = true;
            }
        }
    }
    
     //Handles the sound toggle button - the speaker icon: Toggles background music on/off using SoundManager and updates the icon accordingly.
    @FXML void onSoundOff() {
        util.SoundManager.toggleMusic();

        if (setUpmusicIsOnButton != null && setUpmusicIsOnButton.getGraphic() instanceof ImageView iv) {
            String iconPath = util.SoundManager.isMusicOn()
                    ? "/Images/volume.png"
                    : "/Images/mute.png";

            Image img = new Image(getClass().getResourceAsStream(iconPath));
            iv.setImage(img);
        }
    }
    //Handles clicks on any avatar StackPane, adds/removes the "avatar-selected" CSS class from the clicked avatar.
    @FXML
    private void onAvatarClicked(MouseEvent event) {
        Object src = event.getSource();
        StackPane avatarPane = null;

        if (src instanceof StackPane) {
            avatarPane = (StackPane) src;
        } else if (src instanceof Node) {
            Node node = (Node) src;
            if (node.getParent() instanceof StackPane) {
                avatarPane = (StackPane) node.getParent();
            }
        }

        if (avatarPane == null) {
            return;
        }

        if (selectedAvatarPane != null) {
            selectedAvatarPane.getStyleClass().remove("avatar-selected");
        }

        selectedAvatarPane = avatarPane;
        if (!avatarPane.getStyleClass().contains("avatar-selected")) {
            avatarPane.getStyleClass().add("avatar-selected");
        }
        playClickSound();
    }
  
    //Shows an error dialog with a given message and it is used for input validation failures.
    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
