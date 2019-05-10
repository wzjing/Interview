package com.wzjing.interview.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.Semaphore;

public class AudioManager {

    private static final String TAG = AudioManager.class.getSimpleName();

    private final int MSG_ERROR = 0;

    private int sampleRate;
    private int channels;
    private File destFile;
    private Thread thread;
    private boolean recording = false;
    private Handler handler;

    private AudioRecordCallback mCallback;

    public AudioManager(int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public void startRecord(@NonNull File destFile) {
        this.destFile = destFile;
        handler = new Handler(this::handle);
        recording = true;
        thread = new Thread(this::recordInternal);
        thread.start();
    }

    private void recordInternal() {
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        Log.d(TAG, "recordInternal: buffersize -- " + bufferSize);

        OutputStream oStream;
        try {
            if (!destFile.exists() && !destFile.createNewFile()) {
                if (handler != null)
                    Message.obtain(handler, MSG_ERROR, "unable to create audio file");
            }
            oStream = new FileOutputStream(destFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
            if (handler != null) Message.obtain(handler, MSG_ERROR, e.getMessage());
            return;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            if (handler != null) Message.obtain(handler, MSG_ERROR, e.getMessage());
            return;
        }

        audioRecord.startRecording();
        byte[] buffer = new byte[bufferSize];
        while (recording || audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            int realBufferSize = audioRecord.read(buffer, 0, bufferSize);
            try {
                Log.d(TAG, "recordInternal: write: " + realBufferSize);
                oStream.write(buffer, 0, realBufferSize);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                if (handler != null) Message.obtain(handler, MSG_ERROR, e.getMessage());
                break;
            }
        }

        audioRecord.stop();
        try {
            oStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (handler != null) Message.obtain(handler, MSG_ERROR, e.getMessage());
        }
    }

    public void stopRecord() {
        recording = false;
    }

    public boolean handle(Message msg) {
        if (msg.what == MSG_ERROR) {
            if (mCallback != null) mCallback.onError(msg.obj.toString());
        }
        return true;
    }

    public void setRecordListener(AudioRecordCallback callback) {
        mCallback = callback;
    }

    public interface AudioRecordCallback {
        void onError(String msg);

        void onStart();
    }

}
