package com.hugsforbugs.cs471pc;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;


public class MainMenuController {

        @FXML
        private Label size, EST;
        @FXML
        private TextField urlField;
        @FXML
        private TextField fieldUrl;
        @FXML
        private ProgressBar progressBar;

        @FXML
        private GridPane progressGrid;

        private int currentRow = 0;

        @FXML
        public void addNewEntry() {
            currentRow++;


            fieldUrl = new TextField();
            fieldUrl.setPromptText("Enter URL");

            size = new Label("50000 Mb");
            size.setPrefWidth(300.0);


            progressBar = new ProgressBar(0.0);
            progressBar.setPrefWidth(380.0);

            EST = new Label("10 mins");
            EST.setPrefWidth(100.0);


            int textFieldColumn = 0;
            int sizeColumn = 1;
            int progressBarColumn = 2;
            int ESTColumn = 3;

            GridPane.setColumnIndex(fieldUrl, textFieldColumn);
            GridPane.setRowIndex(fieldUrl, currentRow);

            GridPane.setColumnIndex(size, sizeColumn);
            GridPane.setRowIndex(size, currentRow);

            GridPane.setColumnIndex(progressBar, progressBarColumn);
            GridPane.setRowIndex(progressBar, currentRow);

            GridPane.setColumnIndex(EST, ESTColumn);
            GridPane.setRowIndex(EST, currentRow);

            progressGrid.getChildren().addAll(fieldUrl,size, progressBar,EST);

          // dummy method
            simulateDownload();
        }

        // Simulate a download process
        private void simulateDownload() {

        }

        @FXML
        public void DeleteEntry() {
            progressGrid.getChildren().removeAll(fieldUrl,size, progressBar, EST);

        }
    }

