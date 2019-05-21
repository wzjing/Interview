package com.wzjing.interview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;

import com.wzjing.interview.record.CameraManager;
import com.wzjing.interview.record.VideoRecorder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecordActivity extends AppCompatActivity {

    private final String TAG = RecordActivity.class.getSimpleName();

    private final int CODE_PERMISSION_CAMERA = 0x101;
    private final int CODE_PERMISSION_RECORD = 0x102;
    private boolean isPermissionRequesting = false;
    private boolean recording = false;

    private int UI_HIDE;
    private AlertDialog errorDialog;

    private FloatingActionButton fabButton;
    private Chronometer timeCounter;
    private SurfaceView surfaceView;

    //    private CameraManager manager;
    private VideoRecorder recorder;
    private String currentPath = null;
    private int currentIndex = 0;

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
                    }.start();

                }
            });
        }
        setContentView(R.layout.activity_record);
        surfaceView = findViewById(R.id.surfaceView);
        fabButton = findViewById(R.id.actionFab);
        timeCounter = findViewById(R.id.timeCounter);

//        manager = new CameraManager(surfaceView.getHolder(), 1280, 720);
//        manager.setListener(new CameraManager.CameraListener() {
//            @Override
//            public void onStart() {
//                Log.d(TAG, "CameraManager start");
//            }
//
//            @Override
//            public void onClose() {
//                Log.d(TAG, "CameraManager close");
//            }
//
//            @Override
//            public void onError(String msg) {
//                showErrorDialog("Camera error", msg);
//            }
//        });

        recorder = new VideoRecorder(1280, 720, surfaceView);

        recorder.setRecordListener(new VideoRecorder.RecordListener() {
            @Override
            public void onStart() {
                timeCounter.setTextColor(ContextCompat.getColor(RecordActivity.this, R.color.colorAccent));
                timeCounter.setBase(SystemClock.elapsedRealtime());
                timeCounter.start();
                fabButton.setImageResource(R.drawable.ic_pause);
                fabButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(RecordActivity.this, R.color.colorAccent)));
            }

            @Override
            public void onStop() {
                timeCounter.setTextColor(ContextCompat.getColor(RecordActivity.this, R.color.colorPrimary));
                timeCounter.stop();
                fabButton.setImageResource(R.drawable.ic_play);
                fabButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(RecordActivity.this, R.color.colorPrimary)));
                currentIndex++;
            }

            @Override
            public void onError(String message) {
                showErrorDialog("Recorder error", message);
            }
        });

        fabButton.setOnClickListener(v -> {
            if (!recorder.isRecording()) {
                currentPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "video" + currentIndex + ".mp4";
                Log.d(TAG, "current Path: " + currentPath);
                if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO},
                            CODE_PERMISSION_RECORD);
                    isPermissionRequesting = true;
                } else {
                    recorder.startRecord(currentPath);
                }
            } else {
                recorder.stopRecord();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPermissionRequesting) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODE_PERMISSION_CAMERA);
            isPermissionRequesting = true;
        } else {
            recorder.startPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        recorder.stopPreview();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE_PERMISSION_CAMERA) {
            isPermissionRequesting = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    showErrorDialog("Permission Error", "Camera permission was not granted!");
                    return;
                }
            }
            recorder.startPreview();
        } else if (requestCode == CODE_PERMISSION_RECORD) {
            isPermissionRequesting = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    showErrorDialog("Permission Error", "Storage permission was not granted!");
                    return;
                }
            }
            recorder.startRecord(currentPath);
        }
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
