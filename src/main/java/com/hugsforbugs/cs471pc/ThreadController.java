package com.hugsforbugs.cs471pc;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadController {

    @FXML
    private GridPane threadGrid;
    private ExecutorService downloadPoolRef;
    private ObservableList<Node> entryRows;
    private DownloadEntry downloadEntry;
    private Future<DownloadEntry> runningDownloads;


    // Method to accept GridPane from the main menu
    public void setGridPane(GridPane gridPane, ExecutorService runningPool, DownloadEntry downloadEntry) {
        this.threadGrid.getChildren().clear();
        this.threadGrid.getColumnConstraints().setAll(gridPane.getColumnConstraints());
        this.threadGrid.getRowConstraints().setAll(gridPane.getRowConstraints());
        this.threadGrid.getChildren().addAll(gridPane.getChildren());
        this.entryRows = this.threadGrid.getChildren();
        this.downloadPoolRef = runningPool;
        this.downloadEntry = downloadEntry;
    }
    public void initialize() {
        this.threadGrid.setOnMouseClicked(event -> {
            Node targetNode = (Node) event.getTarget();
            Integer row = GridPane.getRowIndex(targetNode);
            if (row != null) {
                System.out.println("Clicked on row: " + (row));
                highlight(row);
            }
        });
    }


//    public void deleteSelectedRow() {
//        if (this.selectedRowIndex == -1) {
//            System.out.println("No row selected!");
//            return;
//        }
//        this.threadGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == this.selectedRowIndex);
//        this.gridMappedEntries.remove(this.currentRow);
//        this.entryRows.remove(this.currentRow);
//        // Shift rows after deletion
//        for (Node node : this.threadGrid.getChildren()) {
//            Integer row = GridPane.getRowIndex(node);
//            if (row != null && row > this.selectedRowIndex) {
//                GridPane.setRowIndex(node, row - 1);
//            }
//        }
//        this.selectedRowIndex = -1;
//        this.currentRow--;
//    }

    @FXML
    public void startDownload() throws URISyntaxException {
         this.runningDownloads = this.downloadPoolRef.submit(new FileDownloader(this.downloadEntry,
                 this.entryRows));
    }

    @FXML
    public void pauseDownload() throws ExecutionException, InterruptedException {
//        if (this.selectedRowIndex == -1) {
//            System.out.println("No row selected!");
//            return;
//        }
        this.runningDownloads.cancel(true);
        this.runningDownloads = null;

    }
    private void highlight(int rowIndex) {
        this.threadGrid.getChildren().forEach(child -> child.setStyle(""));
        for (Node node : this.threadGrid.getChildren()) {
            Integer row = GridPane.getRowIndex(node);
            if (row != null && row == rowIndex) {
                node.setStyle("-fx-background-color: #B2C9AD;");
            }
        }
    }

}
