package control;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.SysData;
import util.SoundManager;

public class Main extends Application {
	
	//Entry point of the application.
    @Override
    public void start(Stage primaryStage) throws Exception {
        SysData.getInstance().loadHistoryFromCsv();
        SysData.getInstance().loadPlayersFromCsv();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/players_login_view.fxml"));
        Parent root = loader.load(); 

        Scene scene = new Scene(root);//, 1200, 740);
        primaryStage.setScene(scene);
        primaryStage.setTitle("MineSweeper_Wolf");
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
