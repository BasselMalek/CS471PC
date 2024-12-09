package com.hugsforbugs.cs471pc;

import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class FileDownloader implements Callable<int[]> {
    final Integer ftpPort = 2121;
    final Integer chunkSize = 8192;
    URI sourceURI;
    String destinationPath;
    Long size;
    ExecutorService segmentRunner;
    ArrayList<Long> segmentOffsets;
    ArrayList<Boolean> segmentStates;
    InputStream fileReader;
    OutputStream fileWriter;
    Integer retryAttempts = 5;


    public FileDownloader(String uri, String dest) throws IOException, URISyntaxException {
//        anonClient = new FTPClient();
        this.sourceURI = new URI(uri);
        this.destinationPath = dest;
        this.segmentRunner = Executors.newFixedThreadPool(3);
    }

    @Override
    public int[] call() throws IOException, ExecutionException, InterruptedException {

        FTPClient anonClient = new FTPClient();
        anonClient.connect(this.sourceURI.getHost(), this.ftpPort);
        if (!anonClient.login("anonymous", "")) {
            return new int[]{0, 1};
        }
        try {
            this.size = anonClient.mlistFile(this.sourceURI.getPath()).getSize();
        } catch (NullPointerException e) {
            throw new RuntimeException("Invalid URI");
        }
        if (new File("C://").getUsableSpace() < this.size) {
            return new int[]{0, 1};
        }
        this.segmentOffsets = calculateOffsets();
        Future<Boolean> segOne = this.segmentRunner.submit(new SegmentDownloader());
        Future<Boolean> segTwo = this.segmentRunner.submit(new SegmentDownloader());
        Future<Boolean> segThr = this.segmentRunner.submit(new SegmentDownloader());

        this.segmentStates = new ArrayList<>(List.of(segOne.get(), segTwo.get(), segThr.get()));
        while ((this.segmentStates.contains(false))&this.retryAttempts>0){
            if
        }




        return new int[]{0, 0};
//        this.size = anonClient.mlistFile(this.sourceURI).getSize();
//        if (new File(this.destinationURI).getUsableSpace() < this.size +)
//
//        if (fileStream == null) {
//            throw new IOException("Failed to retrieve file: " + this.sourceURI);
//        }
//
//        File destFile = new File(this.destinationURI);
//        if (!destFile.exists() && !destFile.createNewFile()) {
//            throw new IOException("Failed to create destination file: " + destinationURI);
//        }
//
//        try (FileOutputStream result = new FileOutputStream(destFile);
//             fileStream) {
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = fileStream.read(buffer)) != -1) {
//                result.write(buffer, 0, bytesRead);
//            }
//            result.flush();
//            fileStream.close();
//        }
//         if (!anonClient.completePendingCommand()) {
//            throw new IOException("Failed to complete FTP command");
//        }
//
//        System.out.println("Download complete. Server Reply: " + anonClient.getReplyString());
//         return true;
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

    ;

    private ArrayList<Long> calculateOffsets() {
        ArrayList<Long> calculatedResult = new ArrayList<>();
        calculatedResult.set(0, 0L);
        calculatedResult.set(1, Math.ceilDiv(this.size, 3) - 1);
        calculatedResult.set(1, Math.ceilDiv(this.size, 3) * 2 - 1);
        return calculatedResult;
    }

}
