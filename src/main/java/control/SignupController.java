package control;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import util.AvatarManager;
import util.SoundManager;

public class SignupController {

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

    private Stage stage;
    private AvatarManager avatarManager;

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

}
