package com.hugsforbugs.cs471pc;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;
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
        private TextInputDialog dialog;
        @FXML
        private TextField urlField;
        @FXML
        private TextField fieldUrl;
        @FXML
        private ProgressBar progressBar;

        @FXML
        private GridPane progressGrid;

        private int currentRow = 0;
        private int selectedRowIndex = -1;

    public void initialize() {

        progressGrid.setOnMouseClicked(event -> {
            Node targetNode = (Node) event.getTarget();
            Integer row = GridPane.getRowIndex(targetNode);
            if (row != null) {
                System.out.println("Clicked on row: " + row);
                highlight(row);
                selectedRowIndex = row;
            }
        });
    }

        @FXML
        public void addprompt(){
                dialog = new TextInputDialog();
                dialog.setTitle("Enter Download URL");
                dialog.setHeaderText("New Download Entry");
                dialog.setContentText("Please enter the URL:");
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(url -> {
                    addNewEntry(url);
                });
            }


        @FXML
        public void addNewEntry(String url) {
            currentRow++;


            fieldUrl = new TextField(url);
            fieldUrl.setEditable(false);
            fieldUrl.setPrefWidth(300.0);

            size = new Label("50000 Mb");
            size.setPrefWidth(300.0);


            progressBar = new ProgressBar(0.0);
            progressBar.setPrefWidth(380.0);

            EST = new Label("10 mins");
            EST.setPrefWidth(300.0);


//            int textFieldColumn = 0;
//            int sizeColumn = 1;
//            int progressBarColumn = 2;
//            int ESTColumn = 3;

//            GridPane.setColumnIndex(fieldUrl, textFieldColumn);
//            GridPane.setRowIndex(fieldUrl, currentRow);
//
//            GridPane.setColumnIndex(size, sizeColumn);
//            GridPane.setRowIndex(size, currentRow);
//
//            GridPane.setColumnIndex(progressBar, progressBarColumn);
//            GridPane.setRowIndex(progressBar, currentRow);
//
//            GridPane.setColumnIndex(EST, ESTColumn);
//            GridPane.setRowIndex(EST, currentRow);

            // lmaoooo all the above couldve been avoided
            progressGrid.add(fieldUrl, 0, currentRow);
            progressGrid.add(size, 1, currentRow);
            progressGrid.add(progressBar, 2, currentRow);
            progressGrid.add(EST, 3, currentRow);

            progressGrid.getChildren().addAll(fieldUrl,size, progressBar,EST);
            addRowClickListener(fieldUrl,size, progressBar,EST);
          // dummy method
            simulateDownload();
        }

    private void highlight(int rowIndex) {
        progressGrid.getChildren().forEach(child -> child.setStyle(""));
        for (Node node : progressGrid.getChildren()) {
            Integer row = GridPane.getRowIndex(node);
            if (row != null && row == rowIndex) {
                node.setStyle("-fx-background-color: #740938;");
            }
        }
    }

    @FXML
    public void deleteSelectedRow() {
        if (selectedRowIndex == -1) {
            System.out.println("No row selected!");
            return;
        }
        progressGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == selectedRowIndex);
        // Shift rows after deletion
        for (Node node : progressGrid.getChildren()) {
            Integer row = GridPane.getRowIndex(node);
            if (row != null && row > selectedRowIndex) {
                GridPane.setRowIndex(node, row - 1);
            }
        }
        selectedRowIndex = -1;
        currentRow--;
    }


//    private void high() {
//        for (Node node : progressGrid.getChildren()) {
//            node.setOnMouseEntered((MouseEvent t) -> {
//                node.setStyle("-fx-background-color:#FFFF00;"); // Highlight yellow on hover
//            });
//
//            node.setOnMouseExited((MouseEvent t) -> {
//                node.setStyle("-fx-background-color:#dae7f3;"); // Revert to original color when hover ends
//            });
//        }
//    }

    private void addRowClickListener(Node... nodes) {
        for (Node node : nodes) {
            node.setOnMouseClicked(event -> {
                progressGrid.getChildren().forEach(child -> child.setStyle(""));

//                for (Node n : nodes) {
//                    n.setStyle("-fx-background-color: lightblue;");
//                }

                selectedRowIndex = GridPane.getRowIndex(node);
            });
        }

//        high();
    }

        private void simulateDownload() {

        }


}

