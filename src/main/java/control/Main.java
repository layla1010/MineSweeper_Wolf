package control;

import javafx.application.Application;
import javafx.stage.Stage;
import model.SysData;
import util.SoundManager;
import util.ViewNavigator; // Import the helper we created

public class Main extends Application {
    
    //Entry point of the application.
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load The Data
        SysData.getInstance().ensureHistoryLoaded();
        SysData.getInstance().ensurePlayersLoaded();
        SysData.getInstance().ensureQuestionsLoaded();

        // Launch the First Screen using ViewNavigator
        // This triggers the logic to GET screen size, MAXIMIZE window, and LOCK it.
        ViewNavigator.switchTo(primaryStage, "/view/players_login_view.fxml");

        // Initialize Audio
        SoundManager.init();
        SoundManager.startMusic();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
