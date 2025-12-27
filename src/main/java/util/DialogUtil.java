package util;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;



public final class DialogUtil {


    public static void show(AlertType type,String header, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

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
        alert.setContentText(message);

        alert.getDialogPane().getStylesheets().add(
                DialogUtil.class.getResource("/css/theme.css").toExternalForm()
        );

        return alert.showAndWait();   // <-- THIS is the key difference
    }
    
    public static boolean confirm(String header, String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        alert.getDialogPane().getStylesheets().add(
                DialogUtil.class.getResource("/css/theme.css").toExternalForm()
        );

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }


}
