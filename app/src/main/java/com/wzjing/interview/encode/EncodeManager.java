package com.wzjing.interview.encode;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class EncodeManager {

    private static final String TAG = EncodeManager.class.getSimpleName();


    private ExecutorService executorService;
    private boolean encoding = false;
    private EncodeListener mEncodeListener;
    private int width;
    private int height;

    // Tasks
    private VideoEncodeTask videoEncodeTask;
    private VideoTransformTask videoTransformTask;

    // Event
    public static final int MSG_ERROR = 0;
    public static final int MSG_START = 1;
    public static final int MSG_STOP = 2;
    public static final int MSG_FINISH = 3;
    private EncodeManagerHandler mHandler;

    public EncodeManager(int width, int height) {
        this.width = width;
        this.height = height;
        executorService = Executors.newCachedThreadPool();
    }

    public void startEncode(@NonNull File destFile) {
        Log.d(TAG, "startEncode()");
        // check if ready to encode
        try {
            if (destFile.exists()) {
                if (!destFile.delete()) {
                    if (mEncodeListener != null) {
                        mEncodeListener.onError("unable to delete exist file:" + destFile.getAbsolutePath());
                        return;
                    }
                }
            }
            if (!destFile.createNewFile()) {
                mEncodeListener.onError("unable to create output file:" + destFile.getAbsolutePath());
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            mEncodeListener.onError("Exception while create output file:" + destFile.getAbsolutePath());
            return;
        }

        // start encode thread
        mHandler = new EncodeManagerHandler(this);
        videoTransformTask = new VideoTransformTask(this, width, height, destFile);
        execute(videoTransformTask);
    }

    public void execute(Runnable task) {
        executorService.submit(task);
    }

    public void addFrame(byte[] data, boolean eof) {
        if (videoTransformTask != null) {
            videoTransformTask.addFrame(data, eof);
        }
    }

    public void pauseEncode() {

    }

    public void resumeEncode() {

    }

    public void stopEncode() {
        Log.d(TAG, "stopEncode()");
        encoding = false;
        if (mEncodeListener != null) mEncodeListener.onStop();
    }

    public boolean isEncoding() {
        return encoding;
    }

    public void setEncodeListener(EncodeListener listener) {
        mEncodeListener = listener;
    }

    private EncodeListener getEncodeLister() {
        return mEncodeListener;
    }

    public void notifyEvent(int what, Object obj) {
        if (mHandler != null) {
            Message.obtain(mHandler, what, obj).sendToTarget();
        }
    }

    public interface EncodeListener {

        void onStart();

        void onError(String message);

        void onStop();

        void onFinish();
    }

    public static class EncodeManagerHandler extends Handler {

        private WeakReference<EncodeManager> encodeManagerRef;

        EncodeManagerHandler(@NonNull EncodeManager encodeManager) {
            encodeManagerRef = new WeakReference<>(encodeManager);
        }

        @Override
        public void handleMessage(Message msg) {
            EncodeManager manager = encodeManagerRef.get();
            if (manager == null) return;
            switch (msg.what) {
                case MSG_ERROR:
                    if (manager.getEncodeLister() != null)
                        manager.getEncodeLister().onError(msg.obj.toString());
                    break;
                case MSG_START:
                    if (manager.getEncodeLister() != null) {
                        manager.encoding = true;
                        manager.getEncodeLister().onStart();
                    }
                    break;
//                case MSG_STOP:
//                    if (manager.getEncodeLister() != null)
//                        manager.getEncodeLister().onStop();
//                    break;
                case MSG_FINISH:
                    if (manager.getEncodeLister() != null)
                        manager.getEncodeLister().onFinish();
                    break;
            }
        }
    }
}
