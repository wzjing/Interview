package com.wzjing.interview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Chronometer;
import android.widget.ImageView;

import com.wzjing.interview.record.CameraManager;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private AlertDialog errorDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fabButton = findViewById(R.id.actionFab);
        ImageView imageView = findViewById(R.id.imageView);
    }


    private void showDialog(String title, String detail) {
        if (errorDialog == null) {
            errorDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(detail)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create();
        }
        if (!errorDialog.isShowing()) {
            errorDialog.show();
        }
    }

    native void filterFrame(String path);

}
