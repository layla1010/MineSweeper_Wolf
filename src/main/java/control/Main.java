package control;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.SysData;
import util.SoundManager;
import util.ThemeManager;

public class Main extends Application {
	
	//Entry point of the application.
    @Override
    public void start(Stage primaryStage) throws Exception {
    	SysData.getInstance().ensureHistoryLoaded();
    	SysData.getInstance().ensurePlayersLoaded();
    	SysData.getInstance().ensureQuestionsLoaded();


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/players_login_view.fxml"));
       // FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/board_view.fxml"));
        Parent root = loader.load(); 

        Scene scene = new Scene(root);//, 1200, 740);
        ThemeManager.applyTheme(scene); 
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
