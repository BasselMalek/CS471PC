package com.hugsforbugs.cs471pc;

import java.net.URI;
import java.util.concurrent.Callable;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;

public class SegmentDownloader implements Callable<Long> {
    private final Integer chunkSize = 8192;
    private final int id;
    private final URI sourceURI;
    private final String destinationPath;
    private final FTPClient serverConn;
    private BoundedInputStream fileReader;
    private FileOutputStream fileWriter;
    private File destinationFile;
    private final Long startingOffset;
    private final Long boundingOffset;

    public SegmentDownloader(int id, URI sourceURI, String destinationPath, Long startingOffset, Long boundingOffset) {
        this.id = id;
        this.sourceURI = sourceURI;
        this.destinationPath = destinationPath;
        this.startingOffset = startingOffset;
        this.boundingOffset = boundingOffset;
        this.serverConn = new FTPClient();
        this.serverConn.setDefaultPort(2121);
    }

    @Override
    public Long call() throws IOException {
        try {

            connect();
            this.serverConn.setRestartOffset(this.startingOffset);
            InputStream serverStream = this.serverConn.retrieveFileStream(this.sourceURI.getPath());

            System.out.println("Got stream and skipped");
            this.fileReader = new BoundedInputStream(serverStream, (this.boundingOffset - this.startingOffset));
            this.fileReader.setPropagateClose(true);
            this.destinationFile = new File((this.destinationPath + FileDownloader.getFileNameFromUri(this.sourceURI.getPath()).get(0) + this.id +".part"));

            this.destinationFile.createNewFile();
             this.fileWriter = new FileOutputStream(this.destinationFile);


//            if (bytesSkipped != this.startingOffset) {
//                throw new IOException("Failed to seek to the specified offset.");
//            }

            byte[] buffer = new byte[this.chunkSize];
            int bufferBytes;
            while ((bufferBytes = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, bufferBytes);
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread was interrupted");
            }

            return -1L;

        } catch (InterruptedException e) {
            this.fileWriter.close();
            this.fileReader.close();
            this.destinationFile.delete();
            return this.startingOffset;
        } catch (IOException e) {
            System.err.println("Error during download: " + e.getMessage());
            return this.startingOffset;
        } finally {
            try {
                if (this.fileReader != null) this.fileReader.close();
                if (this.fileWriter != null) this.fileWriter.close();
            } catch (IOException e) {
                System.err.println("Failed to close streams: " + e.getMessage());
            }
        }
    }

     void connect() throws IOException {
        this.serverConn.connect(this.sourceURI.getHost());
        this.serverConn.login("anonymous", "");
        this.serverConn.enterLocalPassiveMode();
        this.serverConn.setFileType(2);
        System.out.println(this.serverConn.getReplyString());
    }
}
