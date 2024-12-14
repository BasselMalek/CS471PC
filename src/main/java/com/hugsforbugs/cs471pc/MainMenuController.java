package com.hugsforbugs.cs471pc;


import com.mysql.cj.jdbc.MysqlXAConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class MainMenuController {
    @FXML
    private Label entrySize, entryEST;
    @FXML
    private TextInputDialog dialog;
    @FXML
    private TextField entrySourceURI, maxfield;
    @FXML
    private ProgressBar entryProgressBar;

    @FXML
    private GridPane progressGrid;
    @FXML


    ExecutorService downloadPool;
    private ArrayList<Future<DownloadEntry>> runningDownloads = new ArrayList<>();
    private final ArrayList<ArrayList<Node>> entryRows = new ArrayList<>();

    public void initialize() {
        this.progressGrid.setOnMouseClicked(event -> {
            Node targetNode = (Node) event.getTarget();
            Integer row = GridPane.getRowIndex(targetNode);
            if (row != null) {
                System.out.println("Clicked on row: " + (row));
                highlight(row);
//                this.selectedRowIndex = row;
            }
        });
    }



    private boolean maxNumberSet = false;

    @FXML
    public void maxNumberOf() {
        if (maxfield == null) {
            maxfield = new TextField(); // Ensure maxfield is initialized
        }

            if (maxfield.getText().trim().isEmpty()) {
                showAlert("Missing Information", "You must add a number.");
                maxNumberSet = false;
            } else {
                try {
                    int maxNumber = Integer.parseInt(maxfield.getText().trim());
                    if (maxNumber <= 0) {
                        showAlert("Invalid Input", "Number must be greater than 0.");
                        maxNumberSet = false;
                    } else {
                        maxNumberSet = true; // Valid input
                        try {
                            this.downloadPool = Executors.newFixedThreadPool(Integer.parseInt(maxfield.getText().trim()));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Max Number set to: " + maxNumber);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "You must enter a valid number.");
                    maxNumberSet = false;
                }
            }
        }


    @FXML
    public void addprompt() {
        if (!maxNumberSet){
            showAlert("Invalid Input","Please Try To Enter A Vaild Number To Continue");
            return;
        }
        this.dialog = new TextInputDialog();
        this.dialog.setGraphic(null);
        this.dialog.setTitle("Enter Download Details");
        this.dialog.setHeaderText("New Download Entry");

        // Create a custom layout with two TextFields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter file name");
        TextField urlField = new TextField();
        urlField.setPromptText("Enter URL");
        TextField destinationField = new TextField();
        destinationField.setPromptText("Enter Destination");

        // Add fields to the grid
        grid.add(new Label("Filename:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("Destination:"), 0, 2);
        grid.add(destinationField, 1, 2);


        this.dialog.getDialogPane().setContent(grid);
        this.dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        // Show dialog and handle result
        Optional<String> result = this.dialog.showAndWait();
        if (result.isPresent()) {
            String name = nameField.getText().trim();
            String url = urlField.getText().trim();
            String destination = destinationField.getText().trim();

            if (url.isEmpty() || destination.isEmpty()) {
                showAlert("Missing Information", "ALL Fields Are Required.");
            } else {
                ;
                openThreadWindowWithGrid(addNewEntry(name, url, destination), this.downloadPool,new DownloadEntry(0, name, url, destination));
            }
        }
    }



    public GridPane addNewEntry(String name, String url, String destination) {


        this.entrySourceURI = new TextField(url);
        this.entrySourceURI.setEditable(false);
        this.entrySourceURI.setPrefWidth(300.0);

        this.entrySize = new Label("....");
        this.entrySize.setPrefWidth(300.0);


        this.entryProgressBar = new ProgressBar(0.0);
        this.entryProgressBar.setPrefWidth(380.0);



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

//        // lmaoooo all the above couldve been avoided
        GridPane result = new GridPane();
        result.add(this.entrySourceURI, 0, 0);
        result.add(this.entrySize, 1, 0);
        result.add(this.entryProgressBar, 2, 0);
        return result;
//        addRowClickListener(this.entrySourceURI, this.entrySize, this.entryProgressBar);

//        this.progressGrid.add(openThreadWindow(),3,0);

//        this.progressGrid.getChildren().addAll(this.entrySourceURI, this.entrySize, this.entryProgressBar, this.entryEST);


    }

    private void highlight(int rowIndex) {
        this.progressGrid.getChildren().forEach(child -> child.setStyle(""));
        for (Node node : this.progressGrid.getChildren()) {
            Integer row = GridPane.getRowIndex(node);
            if (row != null && row == rowIndex) {
                node.setStyle("-fx-background-color: #B2C9AD;");
            }
        }
    }

//    private void addRowClickListener(Node... nodes) {
//        for (Node node : nodes) {
//            node.setOnMouseClicked(event -> {
//                this.progressGrid.getChildren().forEach(child -> child.setStyle(""));
//                this.selectedRowIndex = GridPane.getRowIndex(node);
//            });
//        }
//    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        alert.setGraphic(null);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

//    private Button openThreadWindow() {
//        openthread.setOnAction(event -> {
//            try {
//                // Example: Load a new thread window
//                FXMLLoader loader = new FXMLLoader(getClass().getResource("thread.fxml"));
//                AnchorPane threadPane = loader.load();
//
//                // Set up a new stage
//                Stage threadStage = new Stage();
//                threadStage.setTitle("Thread Window");
//                threadStage.setScene(new Scene(threadPane));
//                threadStage.show();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//        openthread.fire();
//        return openthread;

    private void openThreadWindowWithGrid(GridPane gridPane, ExecutorService runningPool, DownloadEntry downloadEntry) {
        try {

            // Load the thread window FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("thread.fxml"));
            AnchorPane threadPane = loader.load();

            // Access the ThreadController
            ThreadController threadController = loader.getController();
            threadController.setGridPane(gridPane, runningPool, downloadEntry); // Pass the GridPane

            // Set up the thread window
            Stage threadStage = new Stage();
            threadStage.setTitle("Thread Window");
            threadStage.setScene(new Scene(threadPane));
            threadStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}




