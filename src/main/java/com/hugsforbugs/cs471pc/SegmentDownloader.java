package com.hugsforbugs.cs471pc;

import java.util.concurrent.Callable;

public class SegmentDownloader implements Callable<Boolean> {


    @Override
    public Boolean call() throws Exception {
        Thread.sleep(7000);
        return true;
    }
}
