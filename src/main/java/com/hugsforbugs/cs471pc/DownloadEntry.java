package com.hugsforbugs.cs471pc;

import org.apache.commons.net.ftp.FTPClient;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadEntry {
    Integer downloadStatus;
    String sourcePath;
    String destinationPath;
    Long fileSize;
    ArrayList<Long> segmentOffsets;
    ArrayList<Future<Boolean>> segmentStates;

    public DownloadEntry(Integer downloadStatus, String sourcePath, String destinationPath) {
        this.downloadStatus = downloadStatus;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }
}


