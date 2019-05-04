package com.wzjing.interview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.wzjing.interview.encode.EncodeThread;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private final String TAG = MainActivity.class.getSimpleName();

    private Camera camera;
    private SurfaceView surfaceView;
    private FloatingActionButton fabButton;

    private final int CODE_PERMISSION_CAMERA = 0x101;

    private final int UI_HIDE = View.SYSTEM_UI_FLAG_IMMERSIVE |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN;

    private EncodeThread encodeThread;
    private File output;
    private boolean isEncoding = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(UI_HIDE);
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
                Log.d(TAG, "visibility: " + Integer.toBinaryString(visibility));
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != View.SYSTEM_UI_FLAG_FULLSCREEN) {
                    Log.d(TAG, "visibility: not fullscreen");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    getWindow().getDecorView().setSystemUiVisibility(UI_HIDE);
                } else {
                    Log.d(TAG, "visibility: fullscreen");
                }
            });
        }


        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        fabButton = findViewById(R.id.actionFab);


        surfaceView.getHolder().addCallback(this);
        fabButton.setOnClickListener(v -> {
            if (isEncoding) {
                if (encodeThread != null) {
                    fabButton.setImageResource(R.drawable.ic_play);
                    isEncoding = false;
                    encodeThread.close();
                } else {
                    Log.d(TAG, "encode thread is null");
                }
            } else {
                if (encodeThread != null) {
                    fabButton.setImageResource(R.drawable.ic_pause);
                    isEncoding = true;
                    encodeThread.start();
                } else {
                    Log.d(TAG, "encode thread is null");
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_PERMISSION_CAMERA);
        }

        Log.d(TAG, "StoragePath: " + Environment.getExternalStorageDirectory().getAbsolutePath());

//        File input = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "video.yuv");
        output = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "output.h264");

//        new Thread(() -> test(input.getAbsolutePath(), output.getAbsolutePath())).start();
    }

    private void openCamera(SurfaceHolder holder) {
        encodeThread = new EncodeThread(1920, 1080, output);
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        try {
            Camera.Parameters param = camera.getParameters();
            param.setPreviewSize(1920, 1080);
            param.setPreviewFormat(ImageFormat.YV12);

//            if (Build.VERSION.SDK_INT >= 24) {
//                param.getSupportedPreviewFpsRange().forEach(fps -> {
//
//                    Log.d(TAG, "Range: ");
//                    for (int f : fps) {
//                        Log.d(TAG, "" + f / 1000f);
//                    }
//                });
//                param.getSupportedPreviewSizes().forEach(size -> {
//                    Log.d(TAG, String.format("Size: %dx%d", size.width, size.height));
//                });
//                param.getSupportedPreviewFormats().forEach(format -> {
//                    Log.d(TAG, "Format: " + Integer.toHexString(format));
//                });
//            }
            param.setPreviewFpsRange(30000, 30000);
            camera.setParameters(param);

            camera.setPreviewCallback((data, camera) -> {
                if (isEncoding)
                    Message.obtain(encodeThread.getHandler(), 0, data).sendToTarget();
            });
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (NullPointerException e) {
            Log.d(TAG, "openCamera: Unable to open camera");
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "openCamera: exception happened");
            e.printStackTrace();
        }
    }


    private void closeCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // do nothing
        Log.d(TAG, "surfaceCreated: surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CODE_PERMISSION_CAMERA);
            }
        }
        openCamera(holder);
        Log.d(TAG, "surfaceChanged: surface changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
        Log.d(TAG, "surfaceDestroyed: surface destroyed");
    }

    native void test(String input, String output);

    static {
        System.loadLibrary("x264");
        System.loadLibrary("avcodec");
        System.loadLibrary("native-lib");
    }
}
