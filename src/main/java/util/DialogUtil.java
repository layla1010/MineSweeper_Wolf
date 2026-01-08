package util;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import model.Theme;



public final class DialogUtil {


	public static void show(AlertType type, String header, String title, String message) {
	    Alert alert = new Alert(type);
	    alert.setTitle(title);
	    alert.setHeaderText(header);
	    makeDialogAutoSize(alert, message);

	    applyDialogCss(alert);
	    alert.showAndWait();
	}
    
    public static void applyDialogCss(Dialog<?> dialog) {
        if (dialog == null) return;

        dialog.getDialogPane().getStylesheets().clear(); // IMPORTANT: avoid duplicates

        var base = DialogUtil.class.getResource("/css/base.css");
        if (base != null) dialog.getDialogPane().getStylesheets().add(base.toExternalForm());

        var theme = DialogUtil.class.getResource(
            ThemeManager.getTheme() == Theme.WOLF ? "/css/wolf.css" : "/css/theme.css"
        );
        if (theme != null) dialog.getDialogPane().getStylesheets().add(theme.toExternalForm());
    }

    
    
    public static Optional<String> promptForOtp() {
        TextInputDialog otpDialog = new TextInputDialog();
        applyDialogCss(otpDialog);
        otpDialog.getEditor().getStyleClass().add("glass-input");
        otpDialog.setTitle("Email Verification");
        otpDialog.setHeaderText("Enter the code you received");
        otpDialog.setContentText("6-digit code:");

        return otpDialog.showAndWait().map(String::trim);
    }
    
    public static Optional<String> promptForNewPasswordTwice() {
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogUtil.applyDialogCss(dialog);
        dialog.setTitle("Set New Password");
        dialog.setHeaderText("Choose a new password");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField pass1 = new PasswordField();
        pass1.setPromptText("New password");
        pass1.getStyleClass().add("glass-input");

        PasswordField pass2 = new PasswordField();
        pass2.setPromptText("Confirm password");
        pass2.getStyleClass().add("glass-input");

        Label error = new Label();
        error.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        grid.add(new Label("New password:"), 0, 0);
        grid.add(pass1, 1, 0);
        grid.add(new Label("Confirm:"), 0, 1);
        grid.add(pass2, 1, 1);
        grid.add(error, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        applyDialogCss(dialog);

        // Disable OK until basic validity is met; also show reason
        var okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable validate = () -> {
            String p1 = pass1.getText() == null ? "" : pass1.getText().trim();
            String p2 = pass2.getText() == null ? "" : pass2.getText().trim();

            String msg = null;
            if (p1.isEmpty()) msg = "Password cannot be empty.";
            else if (p1.length() < 6) msg = "Password must be at least 6 characters.";
            else if (!p1.equals(p2)) msg = "Passwords do not match.";

            error.setText(msg == null ? "" : msg);
            okButton.setDisable(msg != null);
        };

        pass1.textProperty().addListener((o, a, b) -> validate.run());
        pass2.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return Optional.empty();

        return Optional.of(pass1.getText().trim());
    }


    
    public static Optional<ButtonType> showDialogWithResult(
            AlertType type, String header, String title, String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        makeDialogAutoSize(alert, message);

        applyDialogCss(alert);  
        return alert.showAndWait();
    }
    
    public static boolean confirm(String header, String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        makeDialogAutoSize(alert, message);

        applyDialogCss(alert);   // MISSING before
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    
    private static void makeDialogAutoSize(Alert alert, String message) {

        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);

        alert.getDialogPane().setContent(contentLabel);

        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setPrefWidth(500);   // good default
        alert.getDialogPane().setMaxWidth(Screen.getPrimary().getVisualBounds().getWidth() * 0.9);

        alert.setResizable(true);
    }
    
    public static Optional<ButtonType> confirmWithCustomButtons(
            String title, String header, String message, ButtonType... buttons
    ) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        makeDialogAutoSize(alert, message);
        alert.getButtonTypes().setAll(buttons);

        applyDialogCss(alert);   // instead of hard-coded theme.css
        return alert.showAndWait();
    }
}
