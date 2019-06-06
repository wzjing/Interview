package com.wzjing.videoutil;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.HashMap;

public class EditActivity extends AppCompatActivity {

    private final String TAG = EditActivity.class.getSimpleName();

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private ProgressBar progressBar;
    private String uri = null;
    private long playbackPosition;
    private int currentWindow;
    private boolean playWhenReady = true;

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
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        } else {
            player.seekTo(currentWindow, playbackPosition);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        } else {
            currentWindow = player.getCurrentWindowIndex();
            playbackPosition = player.getCurrentPosition();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(this, new DefaultRenderersFactory(this),
                    new DefaultTrackSelector(), new DefaultLoadControl());
            playerView.setPlayer(player);
            player.setPlayWhenReady(playWhenReady);
            player.seekTo(currentWindow, playbackPosition);
        }
//        MediaSource source = buildMediaSource(Uri.parse("/storage/emulated/0/mux.mp4"));
//        player.prepare(source);
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource.Factory(new DefaultDataSourceFactory(this, "video-util"))
                .createMediaSource(uri);
    }

    private void playVideo(String uri) {
        Log.d(TAG, "playing: "+uri);
        MediaSource source = buildMediaSource(Uri.parse(uri));
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.prepare(source);
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
        editor.concatVideos(uri, map, 40, 2, true, mListener);
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
            progressBar.setProgress(progress);
        }

        @Override
        void onFinished() {
            progressBar.setVisibility(View.INVISIBLE);
            if (uri != null) playVideo(uri);
        }
    };
}
