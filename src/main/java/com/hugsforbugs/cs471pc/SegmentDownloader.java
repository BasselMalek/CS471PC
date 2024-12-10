//package com.hugsforbugs.cs471pc;
//
//import java.util.ArrayList;
//import java.util.concurrent.Callable;
//import org.apache.commons.net.ftp.FTPClient;
//import java.io.*;
//public class SegmentDownloader implements Callable<ArrayList<Integer>> {
//    private final Integer chunkSize = 8192;
//    private FTPClient serverConn;
//    private InputStream fileReader;
//    private OutputStream fileWriter;
//    private String uri;
//    private String dest;
//    private long offset;
//
//    public SegmentDownloader(FTPClient serverConn, String uri, String dest, long offset) {
//        this.serverConn = serverConn;
//        this.uri = uri;
//        this.dest = dest;
//        this.offset = offset;
//    }
//
//    @Override
//    public Boolean call() {
//        try {
//            fileReader = serverConn.retrieveFileStream(uri);
//
//            fileWriter = new FileOutputStream((new File(dest + ".part")), true);
//
//            long bytesSkipped = fileReader.skip(offset);
//            if (bytesSkipped != offset) {
//                throw new IOException("Failed to seek to the specified offset.");
//            }
//
//            byte[] buffer = new byte[4096];
//            int bufferBytes;
//
//            while ((bufferBytes = fileReader.read(buffer)) != -1) {
//                fileWriter.write(buffer, 0, bufferBytes);
//                offset += bufferBytes;
//            }
//
//            if (Thread.currentThread().isInterrupted()) {
//                throw new InterruptedException("Thread was interrupted");
//            }
//
//            System.out.println("Download complete for: " + uri);
//            return true;
//
//        } catch (InterruptedException e) {
//            System.err.println("Download interrupted at offset: " + offset);
//            return false;
//        } catch (IOException e) {
//            System.err.println("Error during download: " + e.getMessage());
//            return false;
//        } finally {
//            try {
//                if (fileReader != null) fileReader.close();
//                if (fileWriter != null) fileWriter.close();
//            } catch (IOException e) {
//                System.err.println("Failed to close streams: " + e.getMessage());
//            }
//        }
//    }
//
//    public void suspendSegment() {
//        try {
//            if (fileReader != null) fileReader.close();
//            if (fileWriter != null) fileWriter.close();
//            System.out.println("Segment suspended at offset: " + offset);
//        } catch (IOException e) {
//            System.err.println("Error suspending segment: " + e.getMessage());
//        }
//    }
//}
