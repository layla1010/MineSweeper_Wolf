package util;

import java.io.File;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class AvatarManager {

    private ImageView player1Avatar;
    private ImageView player2Avatar;

    private ImageView singlePlayerAvatar;

    private boolean player1AvatarChosen = false;
    private boolean player2AvatarChosen = false;
    private boolean singleAvatarChosen  = false;

    private int selectedPlayer = 1;
    private StackPane selectedAvatarPane;


    public AvatarManager(ImageView player1Avatar, ImageView player2Avatar) {
        this.player1Avatar = player1Avatar;
        this.player2Avatar = player2Avatar;
    }

    public AvatarManager(ImageView singlePlayerAvatar) {
        this.singlePlayerAvatar = singlePlayerAvatar;
        this.selectedPlayer = 0; 
    }

    public void selectPlayer(int player) {
        if (player == 2) {
            this.selectedPlayer = 2;
        } else {
            this.selectedPlayer = 1;
        }
    }

    public void setupThumbnails(ImageView... thumbs) {
        for (ImageView iv : thumbs) {
            if (iv != null) {
                iv.setOnMouseClicked(e -> setAvatarFromImageView(iv));
            }
        }
    }

    private void setAvatarFromImageView(ImageView source) {
        Image image = source.getImage();
        if (image == null) return;

        if (selectedPlayer == 1 && player1Avatar != null) {
            player1Avatar.setImage(image);
            player1AvatarChosen = true;
        } else if (selectedPlayer == 2 && player2Avatar != null) {
            player2Avatar.setImage(image);
            player2AvatarChosen = true;
        } else if (singlePlayerAvatar != null) { 
            singlePlayerAvatar.setImage(image);
            singleAvatarChosen = true;
        }
    }

    public void handleAvatarPaneClick(MouseEvent event) {
        Object src = event.getSource();
        StackPane avatarPane = null;

        if (src instanceof StackPane sp) {
            avatarPane = sp;
        } else if (src instanceof Node node && node.getParent() instanceof StackPane sp) {
            avatarPane = sp;
        }

        if (avatarPane == null) return;

        if (selectedAvatarPane != null) {
            selectedAvatarPane.getStyleClass().remove("avatar-selected");
        }

        selectedAvatarPane = avatarPane;
        if (!avatarPane.getStyleClass().contains("avatar-selected")) {
            avatarPane.getStyleClass().add("avatar-selected");
        }
    }

    public void handlePlus(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Avatar Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = (stage == null)
                ? fileChooser.showOpenDialog(null)
                : fileChooser.showOpenDialog(stage);

        if (file != null) {
            Image img = new Image(file.toURI().toString());

            if (selectedPlayer == 1 && player1Avatar != null) {
                player1Avatar.setImage(img);
                player1AvatarChosen = true;
            } else if (selectedPlayer == 2 && player2Avatar != null) {
                player2Avatar.setImage(img);
                player2AvatarChosen = true;
            } else if (singlePlayerAvatar != null) {
                singlePlayerAvatar.setImage(img);
                singleAvatarChosen = true;
            }
        }
    }

    public boolean isPlayer1AvatarChosen() {
        return player1AvatarChosen;
    }

    public boolean isPlayer2AvatarChosen() {
        return player2AvatarChosen;
    }

    public boolean isSingleAvatarChosen() {
        return singleAvatarChosen;
    }

    public boolean areBothAvatarsChosen() {
        return player1AvatarChosen && player2AvatarChosen;
    }

    public void showAvatarErrorIfNeeded() {
        if (!areBothAvatarsChosen()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Avatar Error");
            alert.setHeaderText(null);
            alert.setContentText("Please select an avatar for both players.");
            alert.showAndWait();
        }
    }
}
