package com.hugsforbugs.cs471pc;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.ByteArrayInputStream;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("Main-page.fxml"));
//        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Main-page.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
        Scene scene = new Scene(root);
        String css = this.getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(css);
        stage.setScene(scene);
        stage.setTitle("Okaayyyyy less go");
//        stage.setResizable(false);
//        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}