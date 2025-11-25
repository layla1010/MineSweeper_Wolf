package control;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Difficulty;
import model.GameConfig;
import util.SoundManager;

public class NewGameController {

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

    private boolean player1AvatarChosen = false;
    private boolean player2AvatarChosen = false;
    private int selectedPlayer = 1;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        selectPlayer(1);
        setupAvatarThumbnails();
    }

    @FXML
    private void playClickSound() {
        SoundManager.playClick();
    }

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

    @FXML
    private void onPlayer1AreaClicked() {
        selectPlayer(1);
        playClickSound();
    }

    @FXML
    private void onPlayer2AreaClicked() {
        selectPlayer(2);
        playClickSound();
    }

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

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
