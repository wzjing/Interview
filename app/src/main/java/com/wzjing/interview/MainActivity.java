package com.wzjing.interview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.Toast;

import com.wzjing.interview.audio.AudioManager;
import com.wzjing.interview.camera.CameraManager;
import com.wzjing.interview.encode.EncodeManager;
import com.wzjing.interview.muxer.Muxer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private final String TAG = MainActivity.class.getSimpleName();

    private FloatingActionButton fabButton;
    private Chronometer timeCounter;

    private CameraManager cameraManager;
    private AudioManager audioManager;
    private boolean surfaceReady = false;
    private boolean cameraOpen = false;
    private final int CODE_PERMISSION_CAMERA = 0x101;
    private final int CODE_PERMISSION_STORAGE = 0x102;
    private boolean isPermissionRequesting = false;
    private int UI_HIDE;
    private AlertDialog errorDialog;

    private EncodeManager encodeManager;
    private final int width = 1280;
    private final int height = 720;
    private boolean isEof = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                    };

                }
            });
        }

        setContentView(R.layout.activity_main);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        fabButton = findViewById(R.id.actionFab);
        timeCounter = findViewById(R.id.timeCounter);

        surfaceView.getHolder().addCallback(this);
        fabButton.setOnClickListener(v -> {
            if (encodeManager == null) return;
            if (!encodeManager.isEncoding()) {
                File videoFile = getFile("Download/video.h264");
                File audioFile = getFile("Download/audio.pcm");
                if (videoFile == null || audioFile == null) return;
                encodeManager.startEncode(videoFile);
                audioManager.startRecord(audioFile);
            } else {
                encodeManager.stopEncode();
                audioManager.stopRecord();
            }
        });

        cameraManager = new CameraManager(surfaceView.getHolder(), width, height);
        audioManager = new AudioManager(44100, 2);
        encodeManager = new EncodeManager(width, height);

        audioManager.setRecordListener(new AudioManager.AudioRecordCallback() {
            @Override
            public void onError(String msg) {
                showErrorDialog("Audio Record Error", msg);
            }

            @Override
            public void onStart() {

            }
        });
        encodeManager.setEncodeListener(new EncodeManager.EncodeListener() {
            @Override
            public void onStart() {
                Log.d(TAG, "onStart: listener start");
                isEof = false;
                cameraManager.addCallback(((data, camera) -> {
                    if (encodeManager.isEncoding()) {
                        encodeManager.addFrame(data, false);
                    } else if (isEof) {
                        Log.d(TAG, "onPreview: eof frame");
                        isEof = false;
                        encodeManager.addFrame(data, true);
                        cameraManager.addCallback(null);
                    }
                }));
                timeCounter.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                timeCounter.setBase(SystemClock.elapsedRealtime());
                timeCounter.start();
                fabButton.setImageResource(R.drawable.ic_pause);
                fabButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.colorAccent)));

            }

            @Override
            public void onError(String message) {
                showErrorDialog("Encode Error", message);
            }

            @Override
            public void onStop() {
                Log.d(TAG, "onStart: listener stop");
                isEof = true;
                timeCounter.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                timeCounter.stop();
                fabButton.setImageResource(R.drawable.ic_play);
                fabButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary)));
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onStart: listener finished");
                Toast.makeText(MainActivity.this, "Write finished", Toast.LENGTH_SHORT).show();
            }
        });

        ArrayList<Pair<String, String>> sources = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sources.add(new Pair<>("video" + i, "audio" + i));
        }

//        Muxer muxer = new Muxer(sources, "dest.mp4");
//        muxer.setMuxListener(new Muxer.MuxListener() {
//
//            @Override
//            public void onStart() {
//                Log.d(TAG, "onStart: ");
//            }
//
//            @Override
//            public void onProgress(int progress) {
//                Log.d(TAG, "onProgress: ");
//            }
//
//            @Override
//            public void onError(String msg) {
//                Log.d(TAG, "onError: ");
//            }
//
//            @Override
//            public void onFinish() {
//                Log.d(TAG, "onFinish: ");
//            }
//        });
//        muxer.mux();
    }

    @Override
    protected void onPause() {
        super.onPause();
        encodeManager.stopEncode();
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

    private void openCamera() {
        if (isPermissionRequesting) return;

        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODE_PERMISSION_CAMERA);
            isPermissionRequesting = true;
        }

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

    private File getFile(String name) {
        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_PERMISSION_STORAGE);
            return null;
        }
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath(), name);
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
