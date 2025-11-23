package view;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainView {

    public void start(Stage stage) {
        try {
            // Explicitly load from the classpath path inside the JAR
            FXMLLoader loader =
                    new FXMLLoader(MainView.class.getResource("/view/new_game_view.fxml"));

            Pane root = loader.load();
            Font.loadFont(getClass().getResource("/fonts/ethnocentric rg.ttf").toExternalForm(), 14);


            Scene scene = new Scene(root);

            stage.setTitle("Minesweeper - Main View");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
