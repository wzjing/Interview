package com.wzjing.interview.encode;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class EncodeHandler extends Handler {
    private final String TAG = getClass().getSimpleName();

    private static boolean encodeRunning;
    private Encoder encoder;
    private File destFile;
    private FileOutputStream outputStream;
    private static ArrayBlockingQueue<byte[]> rawFrameArray;
    private byte[] tempBuffer;
    private int width;
    private int height;

    EncodeHandler(int width, int height, File destFile, Looper looper) {
        super(looper);
        this.destFile = destFile;
        this.width = width;
        this.height = height;
        rawFrameArray = new ArrayBlockingQueue<>(10);
        encoder = new MediaCodecEncoder(width, height);
        encoder.addCallback(data -> {
            try {
                outputStream.write(data);
                Log.d(TAG, "write to file");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "error write frame to file");
            }
            Log.d(TAG, "write finished");
        });
    }

    void startEncoder() {

        tempBuffer = new byte[width * height * 3 / 2];

        encodeRunning = true;

        Thread thread = new Thread(() -> {
            Log.d(TAG, "encode-thread: start");
            try {
                if (destFile.exists()) {
                    if (!destFile.delete()) throw new IOException("unable to delete file");
                }
                if (!destFile.createNewFile()) throw new IOException("unable to create file");
                outputStream = new FileOutputStream(destFile);
            } catch (IOException e) {
                Log.e(TAG, "unable to open file: " + destFile.getAbsolutePath());
                e.printStackTrace();
                return;
            }
            encoder.start();
            while (encodeRunning || !rawFrameArray.isEmpty()) {
                byte[] data = rawFrameArray.poll();
                if (data != null) {
                    Log.d(TAG, "encode-thread: send to encoder");
                    encoder.encodeFrame(data);
                } else {
                    Log.d(TAG, "encode-thread: data empty");
                }
            }
            Log.d(TAG, "encode-thread: stop");
            encoder.stop();
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setName("Encode Thread");
        thread.start();
    }

    void stopEncoder() {
        encodeRunning = false;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.d(TAG, "received preview");
        byte[] data = (byte[]) msg.obj;
        // encode yuv
        if (rawFrameArray.size() >= 10) {
            Log.d(TAG, "Queue full");
            rawFrameArray.poll();
        }
        rawFrameArray.add(NV21ToNV12(data, width, height));
    }

    private byte[] NV21ToNV12(byte[] nv21, int width, int height) {

        if (nv21 == null || tempBuffer == null) return null;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, tempBuffer, 0, framesize);
        for (i = 0; i < framesize; i++) {
            tempBuffer[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            tempBuffer[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            tempBuffer[framesize + j] = nv21[j + framesize - 1];
        }

        return tempBuffer;
    }
}
