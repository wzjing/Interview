package com.wzjing.interview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaRecorder;
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

import com.wzjing.interview.record.CameraManager;

import java.io.File;
import java.io.IOException;

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

    private Bitmap bitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fabButton = findViewById(R.id.actionFab);
        ImageView imageView = findViewById(R.id.imageView);

        fabButton.setOnClickListener(v -> {
            long start = System.currentTimeMillis();
            filterFrame(Environment.getExternalStorageDirectory().getPath() + File.separator + "frame.yuv");
            Log.d(TAG, "filer time:" + (System.currentTimeMillis() - start));
//            if (bitmap.isMutable()) {
//                Canvas canvas = new Canvas(bitmap);
//                Paint paint = new Paint();
//                paint.setTextSize(30);
//                paint.setColor(Color.WHITE);
//                paint.setTextAlign(Paint.Align.CENTER);
//                canvas.drawColor(Color.BLACK);
//                canvas.drawText("Title", canvas.getWidth() / 2, canvas.getHeight() / 2, paint);
//            } else {
//                showDialog("Warning", "Bitmap is not mutable");
//            }
//            fillBitmap(bitmap, Environment.getExternalStorageDirectory().getPath() + File.separator + "frame.yuv");
//            imageView.setImageBitmap(bitmap);
        });

        bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
        imageView.setImageBitmap(bitmap);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bitmap != null) bitmap.recycle();
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

    native void fillBitmap(Bitmap bitmap, String path);

}
