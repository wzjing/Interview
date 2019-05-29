package com.wzjing.interview;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditActivity extends AppCompatActivity {

    private final String TAG = EditActivity.class.getSimpleName();

    private ExoPlayer player;
    private PlayerView playerView;
    private static ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        playerView = findViewById(R.id.playerView);

        FloatingActionButton concatBtn = findViewById(R.id.concatBtn);
        FloatingActionButton bgmBtn = findViewById(R.id.bgmBtn);
        FloatingActionButton clipBtn = findViewById(R.id.clipBtn);

        concatBtn.setOnClickListener(v -> {
            executorService.submit(() -> {
                String uri = testConcat();
                runOnUiThread(() -> {
                    if (uri != null) {
                        Toast.makeText(EditActivity.this, "concat finished: " + uri, Toast.LENGTH_SHORT).show();
                        playVideo(uri);
                    } else {
                        Toast.makeText(EditActivity.this, "unable to concat", Toast.LENGTH_SHORT).show();
                    }
                });

            });
        });

        bgmBtn.setOnClickListener(v -> {
            executorService.submit(() -> {
                String uri = testBGM();
                runOnUiThread(() -> {
                    if (uri != null) {
                        Toast.makeText(EditActivity.this, "bgm finished: " + uri, Toast.LENGTH_SHORT).show();
                        playVideo(uri);
                    } else {
                        Toast.makeText(EditActivity.this, "unable to add bgm", Toast.LENGTH_SHORT).show();
                    }
                });

            });
        });

        clipBtn.setOnClickListener(v -> {
            executorService.submit(() -> {
                String uri = testClip();
                runOnUiThread(() -> {
                    if (uri != null) {
                        Toast.makeText(EditActivity.this, "clip finished: " + uri, Toast.LENGTH_SHORT).show();
                        playVideo(uri);
                    } else {
                        Toast.makeText(EditActivity.this, "unable to clip", Toast.LENGTH_SHORT).show();
                    }
                });

            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(this);
            playerView.setPlayer(player);
        }
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void playVideo(String uri) {
        player.stop(true);
        DataSource.Factory sourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "InterView"));
        MediaSource source = new ExtractorMediaSource.Factory(sourceFactory)
                .createMediaSource(Uri.parse(uri));
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.prepare(source);
        player.setPlayWhenReady(true);
    }

    private String testConcat() {
        File video0 = new File(Environment.getExternalStorageDirectory(), "Download/video0.mp4");
        File video1 = new File(Environment.getExternalStorageDirectory(), "Download/video1.mp4");
        VideoEditor editor = new VideoEditor();
        String uri = Environment.getExternalStorageDirectory() + File.separator + "mux.mp4";
        HashMap<String, File> map = new HashMap<>();
        map.put("Question: how old are you", video0);
        map.put("Question: what is your skill", video1);
        return editor.concatVideos(uri, map, 40, 2) ? uri : null;
    }

    private String testBGM() {
        File video = new File(Environment.getExternalStorageDirectory(), "mux.mp4");
        File bgm = new File(Environment.getExternalStorageDirectory(), "Download/bgm.aac");
        String uri = Environment.getExternalStorageDirectory() + File.separator + "mix.mp4";
        VideoEditor editor = new VideoEditor();

        return editor.addBGM(uri, video, bgm, 1.6f) ? uri : null;
    }

    private String testClip() {
        File video = new File(Environment.getExternalStorageDirectory(), "mux.mp4");
        String uri = Environment.getExternalStorageDirectory() + File.separator + "clip.mp4";
        VideoEditor editor = new VideoEditor();

        return editor.clip(uri, video, 2, 5) ? uri : null;
    }
}
