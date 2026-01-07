package util;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Screen;



public final class DialogUtil {


    public static void show(AlertType type,String header, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        //alert.setContentText(message);
        makeDialogAutoSize(alert, message);

        alert.getDialogPane().getStylesheets().add(
                DialogUtil.class.getResource("/css/theme.css").toExternalForm()
            );

        	alert.showAndWait();

    }
    
    public static Optional<ButtonType> showDialogWithResult(
            AlertType type,
            String header,
            String title,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        //alert.setContentText(message);
        
        makeDialogAutoSize(alert, message);

        alert.getDialogPane().getStylesheets().add(
                DialogUtil.class.getResource("/css/theme.css").toExternalForm()
        );

        return alert.showAndWait();   // <-- THIS is the key difference
    }
    
    public static boolean confirm(String header, String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        //alert.setContentText(message);
        
        makeDialogAutoSize(alert, message);

        alert.getDialogPane().getStylesheets().add(
                DialogUtil.class.getResource("/css/theme.css").toExternalForm()
        );

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
            String title,
            String header,
            String message,
            ButtonType... buttons
    ) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);

        makeDialogAutoSize(alert, message);

        alert.getButtonTypes().setAll(buttons);

        alert.getDialogPane().getStylesheets().add(
                DialogUtil.class.getResource("/css/theme.css").toExternalForm()
        );

        return alert.showAndWait();
    }



}
