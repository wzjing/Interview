package com.wzjing.videoutil;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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
    private ProgressBar progressBar;
    private static ExecutorService executorService;

    private String uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        playerView = findViewById(R.id.playerView);
        progressBar = findViewById(R.id.progressbar);

        FloatingActionButton concatBtn = findViewById(R.id.concatBtn);
        FloatingActionButton bgmBtn = findViewById(R.id.bgmBtn);
        FloatingActionButton clipBtn = findViewById(R.id.clipBtn);

        concatBtn.setOnClickListener(v -> testConcat());

        bgmBtn.setOnClickListener(v -> testBGM());

        clipBtn.setOnClickListener(v -> testClip());
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
        Log.d(TAG, "playing: "+uri);
        player.stop(true);
        DataSource.Factory sourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "InterView"));
        MediaSource source = new ExtractorMediaSource.Factory(sourceFactory)
                .createMediaSource(Uri.parse(uri));
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.prepare(source);
        player.setPlayWhenReady(true);
    }

    private void testConcat() {
        progressBar.setVisibility(View.VISIBLE);
        File root = Environment.getExternalStorageDirectory();
        File video0 = new File(root, "Download/video0.mp4");
        File video1 = new File(root, "Download/video1.mp4");
        VideoEditor editor = new VideoEditor();
        uri = root.getAbsolutePath() + File.separator + "mux.mp4";
        HashMap<String, File> map = new HashMap<>();
        map.put("Question: how old are you", video0);
        map.put("Question: what is your skill", video1);
        editor.concatVideos(uri, map, 40, 2, false, mListener);
    }

    private void testBGM() {
        progressBar.setVisibility(View.VISIBLE);
        File root = Environment.getExternalStorageDirectory();
        File video = new File(root, "Download/test.mp4");
        File bgm = new File(root, "Download/bgm.aac");
        uri = root.getAbsolutePath() + File.separator + "mix.mp4";
        VideoEditor editor = new VideoEditor();
        editor.addBGM(uri, video, bgm, 1.6f, mListener);
    }

    private void testClip() {
        progressBar.setVisibility(View.VISIBLE);
        File root = Environment.getExternalStorageDirectory();
        File video = new File(root, "Download/test.mp4");
        uri = root.getAbsolutePath() + File.separator + "clip.mp4";
        VideoEditor editor = new VideoEditor();
        editor.clip(uri, video, 2, 5, mListener);
    }

    private VideoEditor.VideoEditorListener mListener = new VideoEditor.VideoEditorListener() {
        @Override
        void onError(String msg) {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(EditActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        void onProgress(int progress) {
            Log.d(TAG, "progress: " + progress);
            progressBar.setProgress(progress);
        }

        @Override
        void onFinished() {
            progressBar.setVisibility(View.INVISIBLE);
            if (uri != null) playVideo(uri);
        }
    };
}
