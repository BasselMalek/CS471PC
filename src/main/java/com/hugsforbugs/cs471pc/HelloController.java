package com.hugsforbugs.cs471pc;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
     @FXML
     private Label welcomeText;
    public void hello(ActionEvent e){
        System.out.println("HOWDY");
    }


//    protected void onHelloButtonClick() {
//        welcomeText.setText("Welcome to JavaFX Application!");
//    }
}