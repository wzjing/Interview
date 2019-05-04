package com.wzjing.interview.encode;

import android.os.Handler;
import android.os.Looper;

import java.io.File;

public class EncodeThread extends Thread {

    private Looper looper;
    private EncodeHandler handler;
    private int width;
    private int height;
    private File destFile;

    public EncodeThread(int width, int height, File destFile) {
        this.width = width;
        this.height = height;
        this.destFile = destFile;
        setName("Encode-handle");
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        looper = Looper.myLooper();
        handler = new EncodeHandler(width, height, destFile, looper);
        handler.startEncoder();
        Looper.loop();
    }

    public void close() {
        handler.stopEncoder();
        if (looper != null) {
            looper.quit();
        }
    }
}
