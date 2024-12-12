package com.hugsforbugs.cs471pc;


import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.Node;


public class MainMenuController {

    @FXML
    private Label entrySize, entryEST;
    @FXML
    private TextInputDialog dialog;
    @FXML
    private TextField entrySourceURI;
    @FXML
    private ProgressBar entryProgressBar;

    @FXML
    private GridPane progressGrid;


    ExecutorService downloadPool;
    private int currentRow = 0;
    private final ArrayList<ArrayList<Node>> entryRows = new ArrayList<>();
    private final  HashMap<Integer, DownloadEntry> gridMappedEntries = new HashMap<>();
    private int selectedRowIndex = -1;

    public void initialize() {
        try {
            this.downloadPool = Executors.newFixedThreadPool(5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.progressGrid.setOnMouseClicked(event -> {
            Node targetNode = (Node) event.getTarget();
            Integer row = GridPane.getRowIndex(targetNode);
            if (row != null) {
                System.out.println("Clicked on row: " + row);
                highlight(row);
                this.selectedRowIndex = row;
            }
        });
    }

    @FXML
    public void addbatch() {
        this.dialog = new TextInputDialog();
        this.dialog.setGraphic(null);
        this.dialog.setTitle("Enter Download Details");
        this.dialog.setHeaderText("New Download Entry");

        // Create a custom layout with two TextFields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField urlField = new TextField();
        urlField.setPromptText("Enter URL");
        TextField destinationField = new TextField();
        destinationField.setPromptText("Enter Destination");

        // Add fields to the grid
        grid.add(new Label("URL:"), 0, 0);
        grid.add(urlField, 1, 0);
        grid.add(new Label("Destination:"), 0, 1);
        grid.add(destinationField, 1, 1);

        this.dialog.getDialogPane().setContent(grid);
        this.dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        // Show dialog and handle result
        Optional<String> result = this.dialog.showAndWait();
        if (result.isPresent()) {
            String url = urlField.getText().trim();
            String destination = destinationField.getText().trim();

            if (url.isEmpty() || destination.isEmpty()) {
                showAlert("Missing Information", "Both URL and Destination are required.");
            } else {
//                addNewEntry(url);
            }
        }
    }

    @FXML
    public void addprompt() {
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
                showAlert("Missing Information", "Both URL and Destination are required.");
            } else {
                addNewEntry(name, url, destination);
            }
        }
    }


    @FXML
    public void addNewEntry(String name, String url, String destination) {
        this.currentRow++;
        this.gridMappedEntries.put(this.currentRow, new DownloadEntry(0,name, url, destination));


        this.entrySourceURI = new TextField(url);
        this.entrySourceURI.setEditable(false);
        this.entrySourceURI.setPrefWidth(300.0);

        this.entrySize = new Label("50000 Mb");
        this.entrySize.setPrefWidth(300.0);


        this.entryProgressBar = new ProgressBar(0.0);
        this.entryProgressBar.setPrefWidth(380.0);

        this.entryEST = new Label("Where ETA");
        this.entryEST.setPrefWidth(300.0);


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
        this.progressGrid.add(this.entrySourceURI, 0, this.currentRow);
        this.progressGrid.add(this.entrySize, 1, this.currentRow);
        this.progressGrid.add(this.entryProgressBar, 2, this.currentRow);
        this.progressGrid.add(this.entryEST, 3, this.currentRow);
        this.entryRows.add(new ArrayList<>(List.of(this.entrySize, this.entryProgressBar, this.entryEST)));

//        this.progressGrid.getChildren().addAll(this.entrySourceURI, this.entrySize, this.entryProgressBar, this.entryEST);
        addRowClickListener(this.entrySourceURI, this.entrySize, this.entryProgressBar, this.entryEST);
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

    @FXML
    public void deleteSelectedRow() {
        if (this.selectedRowIndex == -1) {
            System.out.println("No row selected!");
            return;
        }
        this.progressGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == this.selectedRowIndex);
        this.gridMappedEntries.remove(this.currentRow);
        this.entryRows.remove(this.currentRow);
        // Shift rows after deletion
        for (Node node : this.progressGrid.getChildren()) {
            Integer row = GridPane.getRowIndex(node);
            if (row != null && row > this.selectedRowIndex) {
                GridPane.setRowIndex(node, row - 1);
            }
        }
        this.selectedRowIndex = -1;
        this.currentRow--;
    }

    @FXML
    public void startDownload() throws URISyntaxException {
        this.downloadPool.submit(new FileDownloader(this.gridMappedEntries.get(this.currentRow), this.entryRows.get(this.currentRow)));
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
                this.progressGrid.getChildren().forEach(child -> child.setStyle(""));
                this.selectedRowIndex = GridPane.getRowIndex(node);
            });
        }
    }

    private void simulateDownload() {

    }

    @FXML
    public void pauseDownload() {
        if (this.selectedRowIndex == -1) {
            System.out.println("No row selected!");
            return;
        }
        //code
        if (true) {
            System.out.println("Download paused for row: " + selectedRowIndex);
        } else {
            System.out.println("No download found for row: " + selectedRowIndex);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        alert.setGraphic(null);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

