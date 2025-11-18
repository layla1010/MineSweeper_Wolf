package com.minesweeper;

import javafx.application.Application;
import javafx.stage.Stage;
import view.MainView;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        new MainView().start(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
