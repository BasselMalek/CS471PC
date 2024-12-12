package com.hugsforbugs.cs471pc;

import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;


public class FileDownloader implements Callable<DownloadEntry> {
    FTPClient checkerClient;
    final Integer ftpPort = 2121;
    DownloadEntry receivedEntry;
    Integer isFresh;
    URI sourceURI;
    String destinationPath;
    ArrayList<String> destinationFile;
    Long fileSize;
    ExecutorService segmentRunner;
    ArrayList<Long> segmentOffsets;
    ArrayList<Future<Long>> segmentStates;
    AtomicInteger retryAttempts;
    Connection databaseConnection;
    PreparedStatement updateDownloadStatement;

    public FileDownloader(Integer isFresh, DownloadEntry entry) throws IOException, URISyntaxException, SQLException {
        this.receivedEntry = entry;
        this.checkerClient = new FTPClient();
        this.isFresh = isFresh;
        this.sourceURI = new URI(entry.sourcePath);
//        this.updateDownloadStatement = this.databaseConnection.prepareStatement("UPDATE downloads SET file_name = ?, file_type = ?, file_size = ?, " +
//                "file_url = ?, file_destination = ?, file_status = ?, download_offsets = ? WHERE id=?");
        this.destinationPath = entry.destinationPath;
        this.destinationFile = getFileNameFromUri(this.sourceURI.getPath());
        this.segmentRunner = Executors.newFixedThreadPool(3);
//        this.retryAttempts = new AtomicInteger(5);
    }

    @Override
    public DownloadEntry call() throws IOException {
        try {
            //Connect to FTP server.
            checkerClient.connect(this.sourceURI.getHost(), this.ftpPort);
            if (!checkerClient.login("anonymous", "")) {
                return this.receivedEntry;
            }

            //Validate the actual URI and disk space.
            try {
                this.fileSize = checkerClient.mlistFile(this.sourceURI.getPath()).getSize();
            } catch (NullPointerException e) {
                throw new RuntimeException("Invalid URI");
            }
            if (new File(this.destinationPath).getFreeSpace() < this.fileSize) {
                return this.receivedEntry;
            }

//        //Divide file into parts and start the segments.
            checkerClient.logout();
            checkerClient.disconnect();

            //Start download.
            startDownload();

            this.segmentRunner.shutdown();
            this.segmentRunner.awaitTermination(5, TimeUnit.MINUTES);
//            this.segmentStates.forEach((Future<Long> statePromise) -> {
//                try {
//                    Long res = statePromise.get();
//                } catch (InterruptedException | ExecutionException e) {
//                    throw new RuntimeException(e);
//                }
//            });

            //Shutdown and kill.

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
        if (this.segmentOffsets == null) {
            this.segmentOffsets = calculateOffsets();
            this.segmentStates = new ArrayList<>();
        }
        this.segmentRunner.submit(new SegmentDownloader(1,this.sourceURI, this.destinationPath, this.segmentOffsets.get(0), this.segmentOffsets.get(1)));
        this.segmentRunner.submit(new SegmentDownloader(2,this.sourceURI, this.destinationPath, this.segmentOffsets.get(1), this.segmentOffsets.get(2)));
        this.segmentRunner.submit(new SegmentDownloader(3,this.sourceURI, this.destinationPath, this.segmentOffsets.get(2), this.segmentOffsets.get(3)));
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
        ArrayList<Path> partFiles = new ArrayList<>(List.of(Paths.get(this.destinationPath + this.destinationFile.getFirst() + "1.part"), Paths.get(this.destinationPath + this.destinationFile.getFirst() + "2.part"), Paths.get(this.destinationPath + this.destinationFile.getFirst() + "3.part")));
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

    private ArrayList<Long> calculateOffsets() {
        ArrayList<Long> calculatedResult = new ArrayList<>();
        calculatedResult.add(0, 0L);
        calculatedResult.add(1, (long) (Math.ceil(this.fileSize / 3D)));
        calculatedResult.add(2, (long) (Math.ceil(this.fileSize / 3D) * 2));
        calculatedResult.add(3, this.fileSize);
        return calculatedResult;
    }

    static protected ArrayList<String> getFileNameFromUri(String path) {
        Pattern filePattern = Pattern.compile("/([^/]+)\\.([^/.]+)$");
        Matcher fileFinder = filePattern.matcher(path);
        if (fileFinder.find()) {
            return new ArrayList<String>(List.of(fileFinder.group(1), fileFinder.group(2)));
        }
        return null;
    }
}
