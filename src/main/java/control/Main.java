package control;

import javafx.application.Application;
import util.SoundManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
    	
    	SoundManager.init();
    	util.SoundManager.startMusic();
    	
    	/*if (musicIsOnButton != null && musicIsOnButton.getGraphic() instanceof ImageView iv) {
        iv.setImage(new Image(getClass().getResourceAsStream("/Images/volume.png")));
    }*/

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/main_view.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 700);
        stage.setTitle("Minesweeper");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
