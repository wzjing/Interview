package com.wzjing.videoutil.record;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

public class CameraManager implements SurfaceHolder.Callback {

    private final String TAG = getClass().getSimpleName();

    private final int STATE_NONE = 0;
    private final int STATE_OPEN = 1;
    private final int STATE_INIT = 2;
    private final int STATE_PREVIEW = 3;

    private SurfaceHolder mHOlder;
    private int width;
    private int height;
    private Camera camera;
    private CameraListener mListener;

    private boolean isSurfaceReady = false;
    private int mState = 0;

    /**
     * CameraManager构造方法
     *
     * @param holder 用于显示相机预览
     * @param width  预览宽度，指相机的原始宽度，即横屏下的宽度
     * @param height 预览高度，只相机的原始高度，即横屏下的高度
     */
    public CameraManager(@NonNull SurfaceHolder holder, int width, int height) {
        this.width = width;
        this.height = height;
        mHOlder = holder;
        holder.addCallback(this);
    }

    public void open() {
        Log.d(TAG, "open");
        mState = STATE_OPEN;
        start();
        if (isSurfaceReady) startPreview(mHOlder);
    }

    public void close() {
        Log.d(TAG, "close");
        stop();
        mState = STATE_NONE;
    }

    public void setListener(CameraListener listener) {
        mListener = listener;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Camera getCamera() {
        return camera;
    }

    private void start() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        if (camera == null) {
            if (mListener != null) mListener.onError("open camera error");
            return;
        }

        Camera.Parameters params = camera.getParameters();
        boolean fpsSupported = false;
        for (int[] range : params.getSupportedPreviewFpsRange()) {
            if (range[0] == 30000 && range[1] == 30000) {
                fpsSupported = true;
                break;
            }
        }
        for (int[] fpsRange : params.getSupportedPreviewFpsRange()) {
            Log.d(TAG, "Range: " + fpsRange[0] + ":" + fpsRange[1]);
        }
        if (fpsSupported) {
            params.setPreviewFpsRange(30000, 30000);
        } else {
            Log.w(TAG, "device not support 30fps video recording");
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
            if (mListener != null)
                mListener.onError("device not support preview size: " + width + "x" + height);
            return;
        }
        params.setPreviewFormat(ImageFormat.NV21);
        try {
            camera.setParameters(params);
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            if (mListener != null)
                mListener.onError("set parameter failed, maybe some parameter is invalid");
            return;
        }
        camera.setDisplayOrientation(90);
        mState = STATE_INIT;
    }

    private void stop() {
        mState = STATE_OPEN;
        if (camera != null) {
            if (mListener != null) mListener.onClose();
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    private void startPreview(@NonNull SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            if (mListener != null) mListener.onError("unable to set preview display");
            return;
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            if (mListener != null)
                mListener.onError("unable to start preview, camera maybe already released or other error");
            return;

        }
        mState = STATE_PREVIEW;
        if (mListener != null) mListener.onStart();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        isSurfaceReady = true;
        if (mState == STATE_OPEN) {
            start();
        }
        if (mState == STATE_INIT) {
            startPreview(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceReady = false;
        if (mState != STATE_NONE) stop();
    }

    public interface CameraListener {
        void onStart();

        void onClose();

        void onError(String msg);
    }
}
