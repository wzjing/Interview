package com.wzjing.interview.camera;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

public class CameraManager {

    private final String TAG = getClass().getSimpleName();

    private SurfaceHolder mHodler;
    private int width;
    private int height;
    private Camera camera;

    public CameraManager(@NonNull SurfaceHolder holder, int width, int height) {
        this.width = width;
        this.height = height;
        mHodler = holder;
    }

    public void addCallback(Camera.PreviewCallback callback) {
        Log.d(TAG, "addCallback");
        camera.setPreviewCallback(callback);
    }

    public void open() throws DeviceNotSupportException, RuntimeException, IOException {
        Log.d(TAG, "open");
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        if (camera == null) {
            throw new DeviceNotSupportException("can't startRecord camera");
        }

        Camera.Parameters params = camera.getParameters();
        boolean fpsSupported = false;
        for (int[] range : params.getSupportedPreviewFpsRange()) {
            if (range[0] == 30000 && range[1] == 30000) {
                fpsSupported = true;
                break;
            }
        }
        if (fpsSupported) {
            for (int[] fpsRange : params.getSupportedPreviewFpsRange()) {
                Log.d(TAG, "Range: " + fpsRange[0] + ":" + fpsRange[1]);
            }
            params.setPreviewFpsRange(30000, 30000);
        } else {
            throw new DeviceNotSupportException("device not support 30fps video recording");
        }
        boolean sizeSupported = false;
        for (Camera.Size size : params.getSupportedPreviewSizes()) {
            if (width == size.width && height == size.height) {
                sizeSupported = true;
                break;
            }
        }
        if (sizeSupported) {
            params.setPreviewSize(width, height);
        } else {
            throw new DeviceNotSupportException("device not support preview size: " + width + "x" + height);
        }
        params.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(params);
        camera.setDisplayOrientation(90);
        camera.setPreviewDisplay(mHodler);
        camera.startPreview();
    }

    public void close() {
        Log.d(TAG, "close");
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static class DeviceNotSupportException extends Exception {
        DeviceNotSupportException(String errorMessage) {
            super(errorMessage);
        }
    }
}
