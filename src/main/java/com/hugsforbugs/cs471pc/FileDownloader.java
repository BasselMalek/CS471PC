package com.hugsforbugs.cs471pc;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;


public class FileDownloader implements Callable<DownloadEntry> {
    FTPClient checkerOnFTPClient;
    HttpClient checkerOnHTTPClient;
    final Integer ftpPort = 2121;
    DownloadEntry receivedEntry;
    private final ObservableList<Node> rowGUI;
    Boolean isParallelizable = false;
    URI sourceURI;
    String destinationPath;
    ArrayList<String> destinationFile;
    Long fileSize;
    ExecutorService segmentRunner;
    ArrayList<Long> segmentBounds;
    ConcurrentHashMap<Integer, Long> segmentOffsets;
    ArrayList<Future<Long>> segmentStates;
    private String customFileName;

    public FileDownloader(DownloadEntry entry, ObservableList<Node> rowGUI) throws URISyntaxException {
        this.receivedEntry = entry;
        this.rowGUI = rowGUI;
        this.checkerOnFTPClient = new FTPClient();
        this.sourceURI = new URI(entry.sourcePath);
//        this.updateDownloadStatement = this.databaseConnection.prepareStatement("UPDATE downloads SET file_name = ?, file_type = ?, file_size = ?, " +
//                "file_url = ?, file_destination = ?, file_status = ?, download_offsets = ? WHERE id=?");
        this.destinationPath = entry.destinationPath;
//        this.destinationFile = getFileNameFromUri(this.sourceURI.getPath());
        this.segmentRunner = Executors.newFixedThreadPool(3);
        this.customFileName = entry.fileName;
        this.segmentOffsets = this.receivedEntry.segmentOffsets;
        this.segmentStates = new ArrayList<>();
//        this.retryAttempts = new AtomicInteger(5);
    }

    @Override
    public DownloadEntry call() throws IOException, InterruptedException {
        try {
            if (this.sourceURI.getScheme() == "ftp") {
                validateOnFTP();

            } else {
                validateOnHTTP();
            }
            if (new File(this.destinationPath).getFreeSpace() < this.fileSize) {
                return this.receivedEntry;
            }

            //Start download.
            startDownload();
            this.segmentRunner.shutdown();
            while (!this.segmentRunner.awaitTermination(5, TimeUnit.MINUTES)) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("pausing filed");
                    pauseDownload();
                    return this.receivedEntry;
                }
            }


            //Recombine.
            recombineParts();

            //Update entry to be stored after termination.
            this.receivedEntry.downloadStatus = 2;
            this.segmentOffsets = null;
        } catch (InterruptedException e) {
            System.out.println("pausing file");
            pauseDownload();
            System.out.println("file paused");
            return this.receivedEntry;
        }
        return this.receivedEntry;
    }


    void startDownload() throws IOException {
        Platform.runLater(() -> {
            ((Label) this.rowGUI.get(1)).setText(this.fileSize / 1000000 + "MB");
        });
        this.segmentOffsets = calculateOffsets();
        if (this.segmentOffsets.size() == 2) {
            this.segmentStates.add(this.segmentRunner.submit(new SegmentDownloader(1, this.sourceURI, this.destinationPath, this.destinationFile.getFirst(),
                    this.segmentOffsets,this.segmentBounds.getLast(), this.rowGUI.get(2))));
        } else {
            this.segmentStates.add(this.segmentRunner.submit(new SegmentDownloader(1, this.sourceURI, this.destinationPath, this.destinationFile.getFirst(),
                    this.segmentOffsets,this.segmentBounds.get(0), this.rowGUI.get(2))));
            this.segmentStates.add(this.segmentRunner.submit(new SegmentDownloader(2, this.sourceURI, this.destinationPath, this.destinationFile.getFirst(),
                    this.segmentOffsets, this.segmentBounds.get(1),this.rowGUI.get(2))));
            this.segmentStates.add(this.segmentRunner.submit(new SegmentDownloader(3, this.sourceURI, this.destinationPath, this.destinationFile.getFirst(),
                    this.segmentOffsets,this.segmentBounds.get(2), this.rowGUI.get(2))));
        }
    }

    void pauseDownload() throws InterruptedException {
        try {
            this.segmentStates.forEach((promise) -> {
                promise.cancel(true);  // This sends an interrupt to the thread
            });
            this.segmentRunner.shutdown();
            this.segmentRunner.awaitTermination(10, TimeUnit.SECONDS);
            this.segmentRunner.shutdownNow();

            this.receivedEntry.downloadStatus = 1;
            this.receivedEntry.segmentOffsets = this.segmentOffsets;
            System.out.println("s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Restore interrupt status
            throw e;
        }
    }


    void recombineParts() {
        Path finalFile = Paths.get(this.destinationPath + this.destinationFile.getFirst() + "." + this.destinationFile.getLast());
        ArrayList<Path> partFiles = new ArrayList<>(List.of(Paths.get(this.destinationPath + this.destinationFile.getFirst() + "1.part")));
        if (this.isParallelizable) {
            partFiles.add(Paths.get(this.destinationPath + this.destinationFile.getFirst() + "2.part"));
            partFiles.add(Paths.get(this.destinationPath + this.destinationFile.getFirst() + "3.part"));
        }
        try {
            FileChannel tunnelOut = FileChannel.open(finalFile, CREATE, WRITE, APPEND);
            for (Path partFile : partFiles) {
                FileChannel tunnelIn = FileChannel.open(partFile, READ);
                long size = tunnelIn.size();
                //Apparently a magic number because win cannot transfer more.
                int maxCount = (64 * 1024 * 1024) - (32 * 1024);
                long position = 0;
                while (position < size) {
                    position += tunnelIn.transferTo(position, maxCount, tunnelOut);
                }
                tunnelIn.close();
                partFile.toFile().delete();
            }
            tunnelOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void validateOnFTP() throws IOException, InterruptedException {
        //Connect to FTP server.
        this.checkerOnFTPClient.connect(this.sourceURI.getHost(), this.ftpPort);
        if (!checkerOnFTPClient.login("anonymous", "")) {
            throw new InterruptedException("Access to selected resource denied.");
        }
        //Validate the actual URI.
        try {
            this.fileSize = checkerOnFTPClient.mlistFile(this.sourceURI.getPath()).getSize();

            this.checkerOnFTPClient.logout();
            this.checkerOnFTPClient.disconnect();
        } catch (NullPointerException e) {
            throw new RuntimeException("Invalid URI.");
        }
    }

    void validateOnHTTP() throws IOException, InterruptedException {
        this.checkerOnHTTPClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<String> test = this.checkerOnHTTPClient.send(HttpRequest.newBuilder().uri(this.sourceURI).HEAD().build(), HttpResponse.BodyHandlers.ofString());
        this.fileSize = Long.parseLong(test.headers().allValues("content-length").getFirst());
        this.isParallelizable = !test.headers().allValues("accept-ranges").isEmpty();
        String[] splitType = this.customFileName.split("\\.");
        this.destinationFile = new ArrayList<>(List.of(splitType[0], splitType[1]));
        this.checkerOnHTTPClient.close();
    }

    private ConcurrentHashMap<Integer, Long> calculateOffsets() throws IOException {
        this.segmentBounds = new ArrayList<>(List.of(((long) (Math.ceil(this.fileSize / 3D))),  ((long) (Math.ceil(this.fileSize / 3D)*2)), this.fileSize));
        if (this.receivedEntry.downloadStatus == 1) {
            System.out.println("resumed");
            for (int i = 0; i < this.segmentOffsets.size(); i++) {
                RandomAccessFile checker = new RandomAccessFile(this.destinationPath + this.destinationFile.getFirst() + (i+1)+".part", "r");
                this.segmentOffsets.put(i, (i*(fileSize/3L))+checker.length());
                checker.close();
            }
            return this.segmentOffsets;
        }
        System.out.println("fresh");
        ConcurrentHashMap<Integer, Long> calculatedResult = new ConcurrentHashMap<>();
            calculatedResult.put(0, 0L);
        if (this.isParallelizable) {
            calculatedResult.put(1, (long) (Math.ceil(this.fileSize / 3D)));
            calculatedResult.put(2, (long) (Math.ceil(this.fileSize / 3D) * 2));
        }
        return calculatedResult;
    }

    private ArrayList<String> getFileNameFromFTPUri(String path) {
        Pattern filePattern = Pattern.compile("/([^/]+)\\.([^/.]+)$");
        Matcher fileFinder = filePattern.matcher(path);
        if (fileFinder.find()) {
            return new ArrayList<String>(List.of(fileFinder.group(1), fileFinder.group(2)));
        }
        return null;
    }
}
