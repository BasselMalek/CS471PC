package com.hugsforbugs.cs471pc;

import javafx.application.Platform;
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
    private final ArrayList<Node> rowGUI;
    Integer isFresh;
    Boolean isParallelizable = false;
    URI sourceURI;
    String destinationPath;
    ArrayList<String> destinationFile;
    Long fileSize;
    ExecutorService segmentRunner;
    ArrayList<Long> segmentOffsets;
    ArrayList<Future<Long>> segmentStates;
    AtomicInteger retryAttempts;
    private String customFileName;

    public FileDownloader(DownloadEntry entry, ArrayList<Node> rowGUI) throws  URISyntaxException {
        this.receivedEntry = entry;
        this.rowGUI = rowGUI;
        this.checkerOnFTPClient = new FTPClient();
        this.isFresh = isFresh;
        this.sourceURI = new URI(entry.sourcePath);
//        this.updateDownloadStatement = this.databaseConnection.prepareStatement("UPDATE downloads SET file_name = ?, file_type = ?, file_size = ?, " +
//                "file_url = ?, file_destination = ?, file_status = ?, download_offsets = ? WHERE id=?");
        this.destinationPath = entry.destinationPath;
//        this.destinationFile = getFileNameFromUri(this.sourceURI.getPath());
        this.segmentRunner = Executors.newFixedThreadPool(3);
        this.customFileName = entry.fileName;
//        this.retryAttempts = new AtomicInteger(5);
    }

    @Override
    public DownloadEntry call() throws IOException {
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
            this.segmentRunner.awaitTermination(5, TimeUnit.MINUTES);


            //Recombine.
            recombineParts();

            //Update entry to be stored after termination.
            this.receivedEntry.downloadStatus = 2;
            this.segmentOffsets = null;
        } catch (InterruptedException e) {
            pauseDownload();
        }
        return this.receivedEntry;
    }


    void startDownload() {
        Platform.runLater(()->{
            ((Label)this.rowGUI.getFirst()).setText(this.fileSize/1000000 + "MB");
        });
        if (this.segmentOffsets == null) {
            this.segmentOffsets = calculateOffsets();
            this.segmentStates = new ArrayList<>();
        }
        if (this.segmentOffsets.size() == 2) {
            this.segmentRunner.submit(new SegmentDownloader(1, this.sourceURI, this.destinationPath, this.customFileName,
                    this.segmentOffsets.get(0), this.segmentOffsets.get(1), this.rowGUI.get(1)));
        } else {
            this.segmentRunner.submit(new SegmentDownloader(1, this.sourceURI, this.destinationPath, this.customFileName,
                    this.segmentOffsets.get(0), this.segmentOffsets.get(1), this.rowGUI.get(1)));
            this.segmentRunner.submit(new SegmentDownloader(2, this.sourceURI, this.destinationPath, this.customFileName,
                    this.segmentOffsets.get(1), this.segmentOffsets.get(2), this.rowGUI.get(1)));
            this.segmentRunner.submit(new SegmentDownloader(3, this.sourceURI, this.destinationPath, this.customFileName,
                    this.segmentOffsets.get(2), this.segmentOffsets.get(3), this.rowGUI.get(1)));
        }
    }

    synchronized void pauseDownload() {
        try {
            this.segmentRunner.shutdownNow();
            this.segmentRunner.awaitTermination(3000, TimeUnit.MILLISECONDS);
            this.receivedEntry.downloadStatus = 1;
            this.receivedEntry.segmentOffsets = this.segmentOffsets;
        } catch (InterruptedException e) {
            throw new RuntimeException("Segment workers couldn't close gracefully.");
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
        String[] splitType = test.headers().allValues("content-type").getFirst().split("/");
        if (splitType[0].matches("application")) {
            this.destinationFile = new ArrayList<>(List.of(
                    this.customFileName, "exe"
            ));
        } else {
            this.destinationFile = new ArrayList<>(List.of(
                    this.customFileName, splitType[1]
            ));
        }
        this.checkerOnHTTPClient.close();
    }

    private ArrayList<Long> calculateOffsets() {
        ArrayList<Long> calculatedResult = new ArrayList<>();
        calculatedResult.add(0L);
        if (this.isParallelizable) {
            calculatedResult.add((long) (Math.ceil(this.fileSize / 3D)));
            calculatedResult.add((long) (Math.ceil(this.fileSize / 3D) * 2));
        }
        calculatedResult.add(this.fileSize);
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
