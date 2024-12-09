package com.hugsforbugs.cs471pc;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.security.Security;
import java.util.Arrays;
import java.util.concurrent.Callable;


public class FileDownloader implements Callable<Boolean>{
    String uri;
    String dest;
    Integer size;
    FTPClient serverConn;
    InputStream fileReader;
    OutputStream fileWriter;
    Integer retryAttempts;

    public FileDownloader(String uri, String dest) throws IOException {
//        anonClient = new FTPClient();
        this.uri = uri;
        this.dest = dest;
    }

    @Override
    public Boolean call() throws IOException {

    }


    void pauseDownload(){};

    void launchSegment(){};

    void suspendSegment(){};

    void retrySegment(){};

}
