package com.hugsforbugs.cs471pc;

import org.apache.commons.net.ftp.FTPClient;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadEntry {
    Integer downloadStatus;
    String fileName;
    String sourcePath;
    String destinationPath;
    ConcurrentHashMap<Integer, Long> segmentOffsets;

    public DownloadEntry(Integer downloadStatus,String filename,  String sourcePath, String destinationPath) {
        this.fileName = filename;
        this.downloadStatus = downloadStatus;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.segmentOffsets = new ConcurrentHashMap<>();
    }
}


