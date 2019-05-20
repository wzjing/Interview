package com.wzjing.interview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;

import com.wzjing.interview.record.CameraManager;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private final String TAG = MainActivity.class.getSimpleName();

    private FloatingActionButton fabButton;
    private Chronometer timeCounter;
    private SurfaceView surfaceView;

    private MediaRecorder mediaRecorder;
    private CameraManager cameraManager;
    private boolean surfaceReady = false;
    private boolean cameraOpen = false;
    private final int CODE_PERMISSION_CAMERA = 0x101;
    private final int CODE_PERMISSION_STORAGE = 0x102;
    private boolean isPermissionRequesting = false;
    private boolean recording = false;

    private int UI_HIDE;
    private AlertDialog errorDialog;

    private final int WIDTH = 1920;
    private final int HEIGHT = 1080;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UI_HIDE = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= 19) {
            UI_HIDE |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        }

        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(UI_HIDE);
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != View.SYSTEM_UI_FLAG_FULLSCREEN) {

                    new CountDownTimer(1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            getWindow().getDecorView().setSystemUiVisibility(UI_HIDE);
                        }
                    }.start();

                }
            });
        }

        Log.d(TAG, "setContentView");
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.previewSurface);
        fabButton = findViewById(R.id.actionFab);
        timeCounter = findViewById(R.id.timeCounter);

        surfaceView.getHolder().addCallback(this);
        fabButton.setOnClickListener(v -> {
            if (!recording) {
                File destFile = getFile("interview.mp4");
                if (destFile != null) {
                    startRecord(destFile);
                    recording = true;
                }
            } else {
                stopRecord();
                recording = false;
            }
        });

        cameraManager = new CameraManager(surfaceView.getHolder(), WIDTH, HEIGHT);
        mediaRecorder = new MediaRecorder();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE_PERMISSION_CAMERA) {
            isPermissionRequesting = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    showErrorDialog("Permission Error", "Camera permission was not granted!");
                }
            }
            if (surfaceReady) {
                openCamera();
            }
        } else if (requestCode == CODE_PERMISSION_STORAGE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    showErrorDialog("Permission Error", "Storage permission was not granted!");
                }
            }
        }
    }

    private void setRecordConfigure(File file) {
        mediaRecorder.setOnErrorListener((MediaRecorder mr, int what, int extra) -> {
            showErrorDialog("Recording error", "" + what);
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
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncodingBitRate(4000000);
        mediaRecorder.setVideoSize(WIDTH, HEIGHT);
    }

    private void startRecord(File file) {
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

        timeCounter.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        timeCounter.setBase(SystemClock.elapsedRealtime());
        timeCounter.start();
        fabButton.setImageResource(R.drawable.ic_pause);
        fabButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
    }

    private void stopRecord() {
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        timeCounter.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        timeCounter.stop();
        fabButton.setImageResource(R.drawable.ic_play);
        fabButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
    }

    private File getFile(String name) {
        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_PERMISSION_STORAGE);
            return null;
        }
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), name);
        if (file.exists()) {
            if (!file.delete()) {
                return null;
            }
        }
        try {
            if (file.createNewFile()) {
                return file;
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openCamera() {
        Log.d(TAG, "openCamera()");
        if (isPermissionRequesting) return;
        Log.d(TAG, "permission check");

        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "openCamera: checking Permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODE_PERMISSION_CAMERA);
            isPermissionRequesting = true;
            return;
        }

        Log.d(TAG, String.format(Locale.getDefault(), "openCamera: surface: %b camera: %b", surfaceReady, cameraOpen));
        if (!surfaceReady || cameraOpen) return;
        try {
            cameraManager.open();
        } catch (CameraManager.DeviceNotSupportException e) {
            e.printStackTrace();
            showErrorDialog("Device Not Support", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Camera set display failed", e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            showErrorDialog("Camera startRecord error", e.getMessage());
        }
        cameraOpen = true;
    }

    private void closeCamera() {
        if (!cameraOpen) return;
        if (cameraManager != null) {
            cameraManager.close();
        }
        cameraOpen = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // do nothing
        Log.d(TAG, "surfaceCreated: surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: surface changed");
        surfaceReady = true;
        openCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: surface destroyed");
        surfaceReady = false;
        closeCamera();
    }

    private void showErrorDialog(String title, String detail) {
        if (errorDialog == null) {
            errorDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(detail)
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setCancelable(false)
                    .create();
        }
        if (!errorDialog.isShowing()) {
            errorDialog.show();
        }
    }

}
