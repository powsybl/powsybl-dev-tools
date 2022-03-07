package com.powsybl.ad.viewer.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AppAreaDiagramViewer extends Application {
    // To Remove : https://docs.oracle.com/javase/8/javafx/api/javafx/application/Application.html
    // This function is the core of the IHM, it starts the application by calling Launch() in main
    public void start(Stage primaryStage) throws Exception {
        BorderPane mainPane = new BorderPane();

        Scene scene = new Scene(mainPane, 1000, 800);
        primaryStage.setTitle("Substation diagram viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
