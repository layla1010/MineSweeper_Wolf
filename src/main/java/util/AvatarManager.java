package util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages avatar selection for one or two players.
 * - Built-in avatars are PNG files in /Images: S1.png, S2.png, ..., S13.png
 * - Players can also choose a custom image from their computer with the "+" button.
 *
 * The selected avatar is represented by a String "avatarId":
 *   • For built-in avatars:   "S1.png", "S2.png", ...
 *   • For custom avatars:     a file URL, e.g. "file:/C:/Users/Adan/Pictures/myAvatar.png"
 */
public class AvatarManager {

    private final ImageView player1AvatarView;
    private final ImageView player2AvatarView;

    // All small avatar thumbnails (S1..S13) – optional for highlighting if you want.
    private final List<ImageView> thumbnails = new ArrayList<>();

    // Which player we are currently choosing for (1 or 2)
    private int currentPlayerIndex = 1;

    // Chosen avatar IDs
    private String selectedAvatarIdP1;
    private String selectedAvatarIdP2;

    // CSS class for “selected” thumbnail (defined in style.css)
    private static final String SELECTED_CLASS = "avatar-selected";

    public AvatarManager(ImageView player1AvatarView, ImageView player2AvatarView) {
        this.player1AvatarView = player1AvatarView;
        this.player2AvatarView = player2AvatarView;
    }

    /**
     * Register the small thumbnails .
     * We set their userData to the file name so we can read it on click.
     */
    public void setupThumbnails(ImageView... thumbs) {
        thumbnails.clear();

        int index = 0;
        for (ImageView iv : thumbs) {
            if (iv == null) {
                index++;
                continue;
            }

            thumbnails.add(iv);

            String avatarId = null;

            // 1) Preferred: derive from the actual image URL (so order doesn't matter)
            Image img = iv.getImage();
            if (img != null && img.getUrl() != null) {
                String url = img.getUrl();
                int slash = url.lastIndexOf('/');
                avatarId = (slash >= 0) ? url.substring(slash + 1) : url;
                // avatarId will be something like "S4.png", "S10.png", etc.
            }

            // 2) Fallback: if for some reason URL is null, keep the old index-based logic
            if (avatarId == null || avatarId.isBlank()) {
                avatarId = "S" + (index + 1) + ".png";
            }

            iv.setUserData(avatarId);
            index++;
        }
    }


    /**
     * Tell the manager which player we’re currently selecting for.
     * 1 = Player 1, 2 = Player 2
     */
    public void selectPlayer(int playerIndex) {
        if (playerIndex != 1 && playerIndex != 2) {
            throw new IllegalArgumentException("playerIndex must be 1 or 2");
        }
        this.currentPlayerIndex = playerIndex;
    }

    /**
     * Handle click on one of the avatar thumbnails.
     * Called from controller's onAvatarClicked(MouseEvent).
     */
    public void handleAvatarPaneClick(MouseEvent event) {
        if (!(event.getSource() instanceof ImageView clicked)) {
            return;
        }

        Object data = clicked.getUserData();
        String avatarId;

        if (data instanceof String s && !s.isBlank()) {
            avatarId = s;
        } else {
            // Fallback: derive from image URL
            Image img = clicked.getImage();
            if (img != null && img.getUrl() != null) {
                String url = img.getUrl();
                int idx = url.lastIndexOf('/');
                avatarId = (idx >= 0) ? url.substring(idx + 1) : url;
            } else {
                return;
            }
        }

        // Apply to the correct player
        if (currentPlayerIndex == 1) {
            selectedAvatarIdP1 = avatarId;
            if (player1AvatarView != null) {
                player1AvatarView.setImage(loadAvatarImage(avatarId));
            }
        } else {
            selectedAvatarIdP2 = avatarId;
            if (player2AvatarView != null) {
                player2AvatarView.setImage(loadAvatarImage(avatarId));
            }
        }

        // Optional: highlight selected thumbnail
        updateThumbnailSelection(clicked);
    }

    /**
     * "+" button – allow player to choose a custom image from disk.
     */
    public void handlePlus(Stage stage) {
        if (stage == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Avatar Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        String fileUrl = file.toURI().toString();  // e.g. file:/C:/...

        Image img = new Image(fileUrl);
        if (currentPlayerIndex == 1) {
            selectedAvatarIdP1 = fileUrl;
            if (player1AvatarView != null) {
                player1AvatarView.setImage(img);
            }
        } else {
            selectedAvatarIdP2 = fileUrl;
            if (player2AvatarView != null) {
                player2AvatarView.setImage(img);
            }
        }

        // Custom image is not one of the small thumbnails, so no thumbnail highlight here.
    }

    /* =====================================================
       GETTERS USED BY YOUR CONTROLLERS
       ===================================================== */

    /** Used by SignupController – avatar for the new player. */
    public String getSelectedAvatarIdForPlayer1() {
        return selectedAvatarIdP1;
    }

    /** Used by GameSetupController if needed. */
    public String getSelectedAvatarIdForPlayer2() {
        return selectedAvatarIdP2;
    }

    /** Generic helper in case you need it somewhere else. */
    public String getSelectedAvatarId(int playerIndex) {
        return (playerIndex == 1) ? selectedAvatarIdP1 : selectedAvatarIdP2;
    }

    public boolean isPlayer1AvatarChosen() {
        return selectedAvatarIdP1 != null && !selectedAvatarIdP1.isBlank();
    }

    public boolean isPlayer2AvatarChosen() {
        return selectedAvatarIdP2 != null && !selectedAvatarIdP2.isBlank();
    }

    public boolean areBothAvatarsChosen() {
        return isPlayer1AvatarChosen() && isPlayer2AvatarChosen();
    }

    /* =====================================================
       INTERNAL HELPERS
       ===================================================== */

    private Image loadAvatarImage(String avatarId) {
    	var stream = AvatarManager.class.getResourceAsStream("/Images/" + avatarId);
    	if (stream == null) {
    	    System.err.println("AvatarManager: cannot find /Images/" + avatarId);
    	    return null;
    	}
    	return new Image(stream);
    }
    
    

    private void updateThumbnailSelection(ImageView selectedThumb) {
        for (ImageView iv : thumbnails) {
            if (iv == null) continue;
            iv.getStyleClass().remove(SELECTED_CLASS);
        }
        if (selectedThumb != null && !selectedThumb.getStyleClass().contains(SELECTED_CLASS)) {
            selectedThumb.getStyleClass().add(SELECTED_CLASS);
        }
    }
}
