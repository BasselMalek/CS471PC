package com.hugsforbugs.cs471pc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.Callable;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
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
    private final Long startingOffset;
    private final Long boundingOffset;
    private final ProgressBar progressBar;
    private Long progress;

    public SegmentDownloader(int id, URI sourceURI, String destinationPath, String customFileName, Long startingOffset, Long boundingOffset, Node progressBar) {
        this.id = id;
        this.sourceURI = sourceURI;
        this.destinationPath = destinationPath;
        this.customFileName = customFileName;
        this.startingOffset = startingOffset;
        this.boundingOffset = boundingOffset;
        this.progressBar = (ProgressBar) progressBar;
    }

    @Override
    public Long call() throws IOException {
        try {

            if (this.sourceURI.getScheme() == "ftp") {
                connectOnFTP();
                this.fileReader = downloadOnFTP();
            } else {
                this.fileReader = downloadOnHTTP().body();
            }

            this.destinationFile = new File((this.destinationPath + this.customFileName + this.id + ".part"));
//
            this.destinationFile.createNewFile();
            this.fileWriter = new FileOutputStream(this.destinationFile);


//            if (bytesSkipped != this.startingOffset) {
//                throw new IOException("Failed to seek to the specified offset.");
//            }

            byte[] buffer = new byte[this.chunkSize];
            int bufferBytes;
            this.progress = 0L;
            while ((bufferBytes = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, bufferBytes);
                this.progress += bufferBytes;
                synchronized (this.progressBar) {
                    int finalBufferBytes = bufferBytes;
                    Platform.runLater(() -> {
                        this.progressBar.setProgress(this.progressBar.getProgress() + ((((double) finalBufferBytes / (this.boundingOffset - this.startingOffset))) / 3D));
                    });
                }
                System.out.println("Thread" + this.hashCode() + " downloaded " + this.progress + "/" + (this.boundingOffset - this.startingOffset));
            }
//
//            if (Thread.currentThread().isInterrupted()) {
//                throw new InterruptedException("Thread was interrupted");
//            }
//
            return -1L;

        } catch (InterruptedException e) {
            this.fileWriter.close();
            this.fileReader.close();
            this.destinationFile.delete();
            return this.startingOffset;
        }
//        catch (IOException e) {
//            System.err.println("Error during download: " + e.getMessage());
//            return this.startingOffset;
//        }
        finally {
            try {
                if (this.fileReader != null) this.fileReader.close();
                if (this.fileWriter != null) this.fileWriter.close();
            } catch (IOException e) {
                System.err.println("Failed to close streams: " + e.getMessage());
            }
        }
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

    HttpResponse<InputStream> downloadOnHTTP() throws IOException, InterruptedException {
        this.serverHTTPConn = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<InputStream> r = this.serverHTTPConn.send(HttpRequest.newBuilder().uri(this.sourceURI).headers("range", "bytes=" + this.startingOffset + "-" + (this.boundingOffset - 1), "accept", "application/octet-stream", "accept-encoding", "identity").GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        System.out.println(r.statusCode());
        return r;
    }
}
