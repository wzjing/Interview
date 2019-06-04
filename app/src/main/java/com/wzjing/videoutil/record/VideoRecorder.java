package com.wzjing.videoutil.record;

import android.media.MediaCodecInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;

import static android.os.Build.VERSION_CODES.N;

@SuppressWarnings("unused")
public class VideoRecorder {

    private final String TAG = VideoRecorder.class.getSimpleName();

    private int width;
    private int height;
    private SurfaceView surfaceView;
    private CameraManager cameraManager;
    private RecordListener mListener;
    private MediaRecorder mediaRecorder;

    private boolean isCameraReady = false;
    private boolean isRecording = false;

    public VideoRecorder(int width, int height, @NonNull SurfaceView surfaceView) {
        this.width = width;
        this.height = height;
        this.surfaceView = surfaceView;
        cameraManager = new CameraManager(surfaceView.getHolder(), width, height);
    }

    /**
     * need: {@link android.Manifest.permission#CAMERA}
     */
    public void startPreview() {
        cameraManager.setListener(new CameraManager.CameraListener() {
            @Override
            public void onStart() {
                isCameraReady = true;
            }

            @Override
            public void onClose() {
                isCameraReady = false;
            }

            @Override
            public void onError(String msg) {
                if (mListener != null) mListener.onError("Camera Error: " + msg);
            }
        });
        cameraManager.open();
    }

    public void stopPreview() {
        cameraManager.close();
    }

    /**
     * need: {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE}
     *
     * @param storePath storage path
     */
    public void startRecord(String storePath) {

        Log.d(TAG, "videoRecorder->startRecord()");

        if (!isCameraReady) {
            Log.e(TAG, "camera not ready for record");
            return;
        }

        if (isRecording) {
            Log.w(TAG, "already in recording state");
            return;
        }
        File storeFile = new File(storePath);
        if (storeFile.exists()) {
            if (!storeFile.delete()) {
                if (mListener != null) mListener.onError("unable to override exist video file");
            }
            try {
                if (!storeFile.createNewFile())
                    if (mListener != null) mListener.onError("unable to create video file");

            } catch (IOException e) {
                e.printStackTrace();
                if (mListener != null) mListener.onError("unable to create video file");
                return;
            }
        }

        cameraManager.getCamera().unlock();
        mediaRecorder = new MediaRecorder();
        setRecordConfigure(storeFile);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            if (mListener != null) mListener.onError("recorder start error, storage file error");
            cameraManager.getCamera().lock();
            return;
        } catch (IllegalStateException e) {
            e.printStackTrace(System.err);
            if (mListener != null) mListener.onError("recorder is in wrong state");
            cameraManager.getCamera().lock();
            return;
        }
        isRecording = true;
        if (mListener != null) mListener.onStart();
    }

    public void stopRecord() {
        isRecording = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        if (mListener != null) mListener.onStop();
    }

    public void setRecordListener(RecordListener listener) {
        mListener = listener;
    }

    public boolean isRecording() {
        return isRecording;
    }

    private void resumeRecord() {
        if (Build.VERSION.SDK_INT > N) {
            mediaRecorder.resume();
        } else {
            //TODO: 设置新文件的路径，低版本增加一个录制文件即可
            File file = new File("new path");
            startRecord("/mnt/0/sdcard/new_path.mp4");
        }
    }

    private void pauseRecord() {
        if (Build.VERSION.SDK_INT > N) {
            mediaRecorder.pause();
        } else {
            stopRecord();
        }
    }

    private void setRecordConfigure(File file) {
        mediaRecorder.setOnErrorListener((MediaRecorder mr, int what, int extra) -> {
            if (mListener != null) mListener.onError("record error: " + what);
        });
        mediaRecorder.setCamera(cameraManager.getCamera());
        mediaRecorder.setOrientationHint(90);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
        mediaRecorder.setOutputFile(file.getAbsolutePath());
        // video configuration
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(2);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(96000);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncodingBitRate(4000000);
        mediaRecorder.setVideoSize(width, height);
    }

    public interface RecordListener {

        void onStart();

        void onStop();

        void onError(String message);
    }
}
