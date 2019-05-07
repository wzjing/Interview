package com.wzjing.interview.encode;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

public class VideoTransformTask implements Runnable {

    private static final String TAG = VideoTransformTask.class.getSimpleName();

    private Handler mHandler;
    private Looper looper;
    private EncodeManager manager;
    private int width;
    private int height;
    private File destFile;
    private VideoEncodeTask videoEncodeTask;

    public VideoTransformTask(EncodeManager manager, int width, int height, File destFile) {
        this.manager = manager;
        this.width = width;
        this.height = height;
        this.destFile = destFile;
    }

    @Override
    public void run() {
        Log.d(TAG, "run()");
        Looper.prepare();
        looper = Looper.myLooper();
        mHandler = new VideoTransformHandler(looper, this);
        videoEncodeTask = new VideoEncodeTask(manager, width, height, destFile);
        manager.execute(videoEncodeTask);
        Looper.loop();
    }

    private void cancel() {
        if (looper != null) {
            looper.quit();
        }
    }

    public void addFrame(byte[] data, boolean eof) {
        Message.obtain(mHandler, VideoTransformHandler.MSG_FRAME, eof ? 1 : 0, 0, data).sendToTarget();
    }


    private static class VideoTransformHandler extends Handler {
        private static final int MSG_FRAME = 0;

        private WeakReference<VideoTransformTask> threadRef;
        private byte[] tempBuffer;
        private int count = 0;

        VideoTransformHandler(Looper looper, VideoTransformTask ioThread) {
            super(looper);
            threadRef = new WeakReference<>(ioThread);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_FRAME) {
                VideoTransformTask task = threadRef.get();
                if (task == null) return;
                long start = System.currentTimeMillis();
                boolean eof = msg.arg1 == 1;
                task.videoEncodeTask.addFrame(nv21ToNV12((byte[]) msg.obj, task.width, task.height), eof);
                if (eof) {
                    task.cancel();
                }
                count++;
                Log.d(TAG, "addFrame: " + (System.currentTimeMillis() - start) + "ms");
                Log.d(TAG, "sending frame to encoder: " + count + "(" + (eof ? "EOF" : "---") + ")");
            }
        }

        private byte[] nv21ToYUV420p(byte[] buffer, int width, int height) {
            long start = System.currentTimeMillis();
            if (buffer == null) return null;
            if (tempBuffer == null) tempBuffer = new byte[width * height * 3 / 2];
            int size = width * height;
            System.arraycopy(buffer, 0, tempBuffer, 0, size);
            for (int i = 0; i < size / 4; i++) {
                tempBuffer[size + i] = buffer[size + i * 2 + 1];
                tempBuffer[size + size / 4 + i] = buffer[size + i * 2];
            }
            Log.d(TAG, "nv21ToYUV420p: " + (System.currentTimeMillis() - start) + "ms");
            return tempBuffer;
        }

        private byte[] nv21ToNV12(byte[] buffer, int width, int height) {
            long start = System.currentTimeMillis();
            if (buffer == null) return null;
            if (tempBuffer == null) tempBuffer = new byte[width * height * 3 / 2];
            int size = width * height;
            System.arraycopy(buffer, 0, tempBuffer, 0, size);
            for (int i = 0; i < size / 2-1; i += 2) {
                tempBuffer[size + i] = buffer[size + i + 1];
                tempBuffer[size + i + 1] = buffer[size + i];
            }
            Log.d(TAG, "nv21ToYUV420p: " + (System.currentTimeMillis() - start) + "ms");
            return tempBuffer;
        }
    }

}
