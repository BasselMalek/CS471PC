package com.hugsforbugs.cs471pc;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadController {

    @FXML
    private GridPane threadGrid;
    private int currentRow = 0;
    private int selectedRowIndex = -1;
    private final ArrayList<ArrayList<Node>> entryRows = new ArrayList<>();
    private final HashMap<Integer, DownloadEntry> gridMappedEntries = new HashMap<>();
    private ArrayList<Future<DownloadEntry>> runningDownloads = new ArrayList<>();


    // Method to accept GridPane from the main menu
    public void setGridPane(GridPane gridPane) {
        this.threadGrid.getChildren().clear();
        this.threadGrid.getColumnConstraints().setAll(gridPane.getColumnConstraints());
        this.threadGrid.getRowConstraints().setAll(gridPane.getRowConstraints());
        this.threadGrid.getChildren().addAll(gridPane.getChildren());
    }
    public void initialize() {
        this.threadGrid.setOnMouseClicked(event -> {
            Node targetNode = (Node) event.getTarget();
            Integer row = GridPane.getRowIndex(targetNode);
            if (row != null) {
                System.out.println("Clicked on row: " + (row));
                highlight(row);
                this.selectedRowIndex = row;
            }
        });
    }

    @FXML
    public void deleteSelectedRow() {
        if (this.selectedRowIndex == -1) {
            System.out.println("No row selected!");
            return;
        }
        this.threadGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == this.selectedRowIndex);
        this.gridMappedEntries.remove(this.currentRow);
        this.entryRows.remove(this.currentRow);
        // Shift rows after deletion
        for (Node node : this.threadGrid.getChildren()) {
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
         this.runningDownloads.add(this.selectedRowIndex, this.downloadPool.submit(new FileDownloader(this.gridMappedEntries.get(this.selectedRowIndex), this.entryRows.get(this.selectedRowIndex))));
    }

    @FXML
    public void pauseDownload() throws ExecutionException, InterruptedException {
        if (this.selectedRowIndex == -1) {
            System.out.println("No row selected!");
            return;
        }
        this.runningDownloads.get(this.selectedRowIndex).cancel(true);
        this.runningDownloads.remove(this.selectedRowIndex);

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
