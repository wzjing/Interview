package com.wzjing.interview.record;

import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;

public class VideoRecorder {

    private int width = 0;
    private int height = 0;
    private SurfaceView surfaceView;
    private CameraManager cameraManager;
    private RecordListener mListener;
    private MediaRecorder mediaRecorder;

    public VideoRecorder(int width, int height, @NonNull SurfaceView surfaceView) {
        this.width = width;
        this.height = height;
        this.surfaceView = surfaceView;
        cameraManager = new CameraManager(surfaceView.getHolder(), width, height);
    }

    void startPreview() {
        try {
            cameraManager.open();
        } catch (CameraManager.DeviceNotSupportException e) {
            e.printStackTrace(System.err);
            if (mListener != null) mListener.onError(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace(System.err);
            if (mListener != null) mListener.onError("unable to show preview");
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            if (mListener != null) mListener.onError("unable to open camera");
        }
    }

    void stopPreview() {
        cameraManager.close();
    }

    void startRecord(File storeFile) {
        cameraManager.unlock();
        setRecordConfigure(file);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            //TODO: handle error
            return;
        } catch (IllegalStateException e) {
            // do nothing
            e.printStackTrace(System.err);
            return;
        }
    }

    void stopRecord() {

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
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncodingBitRate(4000000);
        mediaRecorder.setVideoSize(width, height);
    }

    public interface RecordListener {
        void onstart();

        void onstop();

        void onError(String message);
    }
}
