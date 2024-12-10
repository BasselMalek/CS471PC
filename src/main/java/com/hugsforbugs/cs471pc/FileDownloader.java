package com.hugsforbugs.cs471pc;

import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;


public class FileDownloader implements Callable<int[]> {
    final Integer ftpPort = 2121;
    final Integer chunkSize = 8192;
    URI sourceURI;
    String destinationPath;
    ArrayList<String> destinationFile;
    Long size = 12L;
    ExecutorService segmentRunner;
    ArrayList<Long> segmentOffsets;
    ArrayList<Boolean> segmentStates;
    Integer retryAttempts = 5;


    public FileDownloader(String uri, String dest) throws IOException, URISyntaxException {
//        anonClient = new FTPClient();
        this.sourceURI = new URI(uri);
        this.destinationPath = dest;
        this.destinationFile = getFileNameFromUri(this.sourceURI.getPath());
        this.segmentRunner = Executors.newFixedThreadPool(3);
    }

    @Override
    public int[] call() throws IOException, ExecutionException, InterruptedException {

        //Connect to FTP server.
//        FTPClient anonClient = new FTPClient();
//        anonClient.connect(this.sourceURI.getHost(), this.ftpPort);
//        if (!anonClient.login("anonymous", "")) {
//            return new int[]{0, 1};
//        }
//
//        //Validate the actual URI and disk space.
//        try {
//            this.size = anonClient.mlistFile(this.sourceURI.getPath()).getSize();
//        } catch (NullPointerException e) {
//            throw new RuntimeException("Invalid URI");
//        }
//        if (new File("C://").getUsableSpace() < this.size) {
//            return new int[]{0, 1};
//        }
//
//        //Divide file into parts and start the segments.
        this.segmentOffsets = calculateOffsets();
//        anonClient.logout();
//        anonClient.disconnect();
////        this.segmentStates.set(0, this.segmentRunner.submit(new SegmentDownloader()));
////        this.segmentStates.set(1, this.segmentRunner.submit(new SegmentDownloader()));
////        this.segmentStates.set(2, this.segmentRunner.submit(new SegmentDownloader()));
//        //TODO: spawn new segments while you have attempts.
//
//        //Shutdown and kill.
//        this.segmentRunner.shutdown();
//        this.segmentRunner.awaitTermination(10000, TimeUnit.MILLISECONDS);

        //Recombine
        recombineParts();
        return new int[]{0, 0};
    }


    void pauseDownload() {
    }

    ;

    void launchSegment() {
    }

    ;

    void suspendSegment() {
    }

    ;

    void retrySegment() {
    }

    void recombineParts() {
        Path finalFile = Paths.get(this.destinationPath + this.destinationFile.getFirst() + "." + this.destinationFile.getLast());
        ArrayList<Path> partFiles = new ArrayList<>(List.of(
                Paths.get(this.destinationPath + this.destinationFile.getFirst() + "1.part"),
                Paths.get(this.destinationPath + this.destinationFile.getFirst() + "2.part"),
                Paths.get(this.destinationPath + this.destinationFile.getFirst() + "3.part")
        ));
        try {
            FileChannel tunnelOut = FileChannel.open(finalFile, CREATE, WRITE);
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
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<Long> calculateOffsets() {
        ArrayList<Long> calculatedResult = new ArrayList<>();
        calculatedResult.add(0, 0L);
        calculatedResult.add(1, (long) (Math.ceil(this.size / 3D)));
        calculatedResult.add(2, (long) (Math.ceil(this.size / 3D) * 2));
        return calculatedResult;
    }

    private ArrayList<String> getFileNameFromUri(String path) {
        Pattern filePattern = Pattern.compile("/([^/]+)\\.([^/.]+)$");
        Matcher fileFinder = filePattern.matcher(path);
        if (fileFinder.find()) {
            return new ArrayList<String>(List.of(fileFinder.group(1), fileFinder.group(2)));
        }
        return null;
    }
}
