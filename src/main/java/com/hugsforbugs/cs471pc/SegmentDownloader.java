package com.hugsforbugs.cs471pc;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.concurrent.Callable;
import org.apache.commons.net.ftp.FTPClient;
import java.io.*;
public class SegmentDownloader implements Callable<ArrayList<Integer>> {
    private final Integer chunkSize = 8192;
    private FTPClient serverConn;
    private long fileSize;
    private InputStream fileReader;
    private OutputStream fileWriter;
    private String uri;
    private String dest;
    private long offset;

    public SegmentDownloader(String uri, String dest, long fileSize, long offset) {
        this.uri = uri;
        this.dest = dest;
        this.fileSize = fileSize;
        this.offset = offset;
    }

    @Override
    public ArrayList<Integer> call() {
        try {

            serverConn.connect("localhost", 2121);
            serverConn.login("admin", "admin");
            serverConn.enterLocalPassiveMode();

            fileReader = serverConn.retrieveFileStream(uri);

            fileWriter = new FileOutputStream((new File(dest + ".part")), true);

            long bytesSkipped = fileReader.skip(offset);
            if (bytesSkipped != offset) {
                throw new IOException("Failed to seek to the specified offset.");
            }

            byte[] buffer = new byte[chunkSize];
            int bufferBytes;

            while ((bufferBytes = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, bufferBytes);
                offset += bufferBytes;
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread was interrupted");
            }

            System.out.println("Download complete for: " + uri);
            return new ArrayList<>(List.of(0));

        } catch (InterruptedException e) {
            System.err.println("Download interrupted at offset: " + offset);
            return new ArrayList<>(List.of(0, (int) offset));
        } catch (IOException e) {
            System.err.println("Error during download: " + e.getMessage());
            return new ArrayList<>(List.of(0, (int) offset));
        } finally {
            try {
                if (fileReader != null) fileReader.close();
                if (fileWriter != null) fileWriter.close();
            } catch (IOException e) {
                System.err.println("Failed to close streams: " + e.getMessage());
            }
        }
    }

    public void suspendSegment() {
        try {
            if (fileReader != null) fileReader.close();
            if (fileWriter != null) fileWriter.close();

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread was interrupted");
            }

            System.out.println("Segment suspended at offset: " + offset);
        } catch (IOException e) {
            System.err.println("Error suspending segment: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Download interrupted at offset: " + offset);
            try (RandomAccessFile localFile = new RandomAccessFile(dest + ".part", "rw")) {
                long fileLength = localFile.length();

                if (fileLength > offset) {
                    long lastChunkSize = fileLength - offset;
                    offset -= lastChunkSize;
                    localFile.setLength(offset);
                    System.out.println("Last chunk removed. Updated offset: " + offset);
                }
            } catch (IOException ioExc) {
                System.err.println("Error truncating file: " + ioExc.getMessage());
            }
        }
    }
}