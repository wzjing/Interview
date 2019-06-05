package com.wzjing.videoutil;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class StreamActivity extends AppCompatActivity {

    private final String TAG = StreamActivity.class.getSimpleName();

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private PlayerView fullScreenPlayerView;

    private long playbackPosition;
    private int currentWindow;
    private boolean playWhenReady = true;
    private boolean isFullScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        playerView = findViewById(R.id.playerView);
        fullScreenPlayerView = findViewById(R.id.playerViewFull);
        ImageButton fullScreenBtn = playerView.findViewById(R.id.exo_fullscreen);
        if (fullScreenBtn != null) {
            fullScreenBtn.setOnClickListener(v -> enterFullScreen());
        }
        ImageButton exitFullScreenBtn = fullScreenPlayerView.findViewById(R.id.exo_fullscreen);
        if (exitFullScreenBtn != null) {
            // TODO: because R.drawable.ic_fullscreen_exit is an svg icon, this function may not work on low api
            exitFullScreenBtn.setImageResource(R.drawable.ic_fullscreen_exit);
            exitFullScreenBtn.setOnClickListener(v -> exitFullScreen());
        }
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
        if (isFullScreen) enterFullScreen();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        } else {
            player.seekTo(currentWindow, playbackPosition);
//            player.setPlayWhenReady(true);
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

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            exitFullScreen();
        } else {
            super.onBackPressed();
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
        MediaSource mediaSource = buildMediaSource(Uri.parse("http://10.0.2.2:3000/media/video.mp4"));
//        MediaSource mediaSource = buildDashMediaSource(Uri.parse("http://10.0.2.2:3000/media/mux.mpd"));
        player.prepare(mediaSource, true, false);
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
        return new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory("video-util"))
                .createMediaSource(uri);
    }

    private MediaSource buildDashMediaSource(Uri uri) {
        DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(
                new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER));
        DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSourceFactory("ua");
        return new DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory).
                createMediaSource(uri);
    }

    @SuppressLint("InlinedApi")
    private void enterFullScreen() {
        isFullScreen = true;
        playerView.setPlayer(null);
        playerView.setVisibility(View.GONE);
        fullScreenPlayerView.setVisibility(View.VISIBLE);
        fullScreenPlayerView.setPlayer(player);
        fullScreenPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void exitFullScreen() {
        isFullScreen = false;
        fullScreenPlayerView.setPlayer(null);
        fullScreenPlayerView.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);
        playerView.setPlayer(player);
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

}