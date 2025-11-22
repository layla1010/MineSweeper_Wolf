package com.minesweeper;


import model.Difficulty;
import model.GameConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
       
    	//Here we can change manually the Difficulty :EASY / MEDIUM / HARD to test sizes.
    	GameConfig.setCurrent(new GameConfig("Tom", "Jerry", Difficulty.HARD));
        URL fxml = getClass().getResource("/view/board_view.fxml");
        if (fxml == null) {
            throw new IllegalStateException("Cannot find FXML: /view/board_view.fxml");
        }
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Minesweeper Board View");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
