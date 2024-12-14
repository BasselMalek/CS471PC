package com.hugsforbugs.cs471pc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SegmentDownloader implements Callable<Long> {
    private final Integer chunkSize = 8192;
    private final int id;
    private final URI sourceURI;
    private final String destinationPath;
    private FTPClient serverFTPConn;
    private HttpClient serverHTTPConn;
    private InputStream fileReader;
    private FileOutputStream fileWriter;
    private File destinationFile;
    private final String customFileName;
    private final ConcurrentHashMap<Integer, Long> downloadOffsets;
    private final Long startingOffset;
    private final Long boundingOffset;
    private final ProgressBar progressBar;
    private Long progress;

    public SegmentDownloader(int id, URI sourceURI, String destinationPath, String customFileName, ConcurrentHashMap<Integer,Long> downloadOffsets, Long hardBound,Node progressBar) {
        this.id = id;
        this.sourceURI = sourceURI;
        this.destinationPath = destinationPath;
        this.customFileName = customFileName;
        this.downloadOffsets = downloadOffsets;
        this.startingOffset = this.downloadOffsets.get(this.id - 1);
        this.boundingOffset = hardBound;
        this.progressBar = (ProgressBar) progressBar;
    }

    @Override
    public Long call() throws IOException {
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread was interrupted before starting download");
            }

            if (this.sourceURI.getScheme().matches("ftp")) {
                connectOnFTP();
                this.fileReader = downloadOnFTP();
            } else {
                this.fileReader = downloadOnHTTP().body();
            }

            this.destinationFile = new File((this.destinationPath + this.customFileName + this.id + ".part"));
            this.fileWriter = new FileOutputStream(this.destinationFile, !this.destinationFile.createNewFile());



            byte[] buffer = new byte[this.chunkSize];
            int bufferBytes;
            this.progress = 0L;
            while ((bufferBytes = fileReader.read(buffer)) != -1) {
                // Explicitly check for interruption and throw if needed
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Download segment interrupted");
                }
                fileWriter.write(buffer, 0, bufferBytes);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Download segment interrupted");
                }
//                this.downloadOffsets.put(this.id - 1, this.startingOffset + this.progress);

                this.progress += bufferBytes;
                updateProgressBar(bufferBytes);

            }

        } catch (InterruptedException e) {
            System.out.println("Segment download paused: " + e.getMessage());
            return pauseSegment();
        } finally {
            try {
                if (this.serverHTTPConn != null) this.serverHTTPConn.close();
                if (this.serverFTPConn != null){
                    this.serverFTPConn.logout();
                    this.serverFTPConn.disconnect();
                }
                if (this.fileReader != null) this.fileReader.close();
                if (this.fileWriter != null) this.fileWriter.close();
            } catch (IOException e) {
                System.err.println("Failed to close streams: " + e.getMessage());
            }
        }
        return -1L;
    }

    void connectOnFTP() throws IOException {
        this.serverFTPConn = new FTPClient();
        this.serverFTPConn.setDefaultPort(2121);
        this.serverFTPConn.connect(this.sourceURI.getHost());
        this.serverFTPConn.login("anonymous", "");
        this.serverFTPConn.enterLocalPassiveMode();
        this.serverFTPConn.setFileType(2);
        System.out.println(this.serverFTPConn.getReplyString());
    }

    BoundedInputStream downloadOnFTP() throws IOException {
        this.serverFTPConn.setRestartOffset(this.startingOffset);
        InputStream serverStream = this.serverFTPConn.retrieveFileStream(this.sourceURI.getPath());
        BoundedInputStream resultantStream = new BoundedInputStream(serverStream, (this.boundingOffset - this.startingOffset));
        resultantStream.setPropagateClose(true);
        return resultantStream;
    }

    private void updateProgressBar(int bufferBytes) {
        synchronized (this.progressBar) {
            Platform.runLater(() -> {
                this.progressBar.setProgress(this.progressBar.getProgress() + ((((double) bufferBytes / (this.boundingOffset - this.startingOffset))) / 3D));
            });
        }
    }

    HttpResponse<InputStream> downloadOnHTTP() throws IOException, InterruptedException {
        this.serverHTTPConn = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<InputStream> r = this.serverHTTPConn.send(HttpRequest.newBuilder().uri(this.sourceURI).
                headers("range", "bytes=" + this.startingOffset + "-" + (this.boundingOffset - 1),
                        "accept", "application/octet-stream",
                        "accept-encoding", "identity").GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        System.out.println(r.statusCode());
        return r;
    }

    private Long pauseSegment() {
        try {
            // Close connections
            if (this.serverHTTPConn != null) this.serverHTTPConn.close();

            // Close file streams if open
            if (this.fileWriter != null) this.fileWriter.close();
            if (this.fileReader != null) this.fileReader.close();
            System.out.println("at least killed connections");

            // Update download offsets
//            this.downloadOffsets.put(this.id - 1, this.startingOffset + this.progress);

            // Return current progress
            System.out.println( this.startingOffset + this.progress);
            return this.startingOffset + this.progress;
        } catch (IOException e) {
            System.err.println("Error during segment pause: " + e.getMessage());
            return this.startingOffset + this.progress;
        }
    }
}
