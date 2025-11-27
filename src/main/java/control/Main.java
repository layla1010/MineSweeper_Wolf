package control;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.SysData;
import util.SoundManager;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SysData.getInstance().loadHistoryFromCsv();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 740);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mine Sweeper Smart");
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();

        SoundManager.init();
        SoundManager.startMusic();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
