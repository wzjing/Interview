package com.wzjing.interview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.VideoView;

import com.wzjing.interview.record.CameraManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("avcodec");
        System.loadLibrary("avutil");
        System.loadLibrary("avformat");
        System.loadLibrary("avfilter");
        System.loadLibrary("swresample");
        System.loadLibrary("swscale");
        System.loadLibrary("postproc");
        System.loadLibrary("utils");
        System.loadLibrary("media");
    }

    private final String TAG = MainActivity.class.getSimpleName();

    private AlertDialog errorDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fabButton = findViewById(R.id.actionFab);
        VideoView videoView = findViewById(R.id.videoView);

        fabButton.setOnClickListener(v -> {
            new Thread(() -> {
                Log.d(TAG, "start");
//                long start = System.currentTimeMillis();
//                String uri = testMux();
                String uri = "http://10.0.2.2:8080/video.mp4";
//                Log.d(TAG, "end: " + (System.currentTimeMillis() - start));
                runOnUiThread(()->{
                    videoView.setVideoURI(Uri.parse(uri));
                    videoView.setOnPreparedListener((player)->{
                        videoView.start();
                    });
                });
            }).start();
        });

    }

    private String testMux() {
        File video = new File(Environment.getExternalStorageDirectory(), "Download/video.ts");
        VideoEditor editor = new VideoEditor();
        String uri = Environment.getExternalStorageDirectory() + File.separator + "mux.ts";
        HashMap<String, File> map = new HashMap<>();
        map.put("Question: how old are you", video);
        map.put("Question: what is your skill", video);
        editor.muxVideos(uri, map, 30, 1);
        return uri;
    }

    private String testBGM() {
        File video = new File(Environment.getExternalStorageDirectory(), "Download/video.ts");
        File bgm = new File(Environment.getExternalStorageDirectory(), "Download/bgm.aac");
        String uri = Environment.getExternalStorageDirectory() + File.separator + "mix.ts";
        VideoEditor editor = new VideoEditor();
        editor.addBGM(uri, video, bgm, 1.6f);
        return uri;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

}
