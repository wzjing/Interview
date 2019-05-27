package com.wzjing.interview;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class StreamActivity extends AppCompatActivity {

    private SimpleExoPlayer player;
    private static final DefaultBandwidthMeter BANDWIDTH_METER =
            new DefaultBandwidthMeter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        PlayerView playerView = findViewById(R.id.playerView);

        Uri dashUri = Uri.parse("http://10.0.2.2:3000/mux.mpd");
        TrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(adaptiveTrackSelectionFactory));
        MediaSource mediaSource = buildMediaSource(dashUri);
        playerView.setPlayer(player);
        player.setPlayWhenReady(true);

        player.prepare(mediaSource);
    }

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory manifestDataSourceFactory =
                new DefaultHttpDataSourceFactory("ua");
        DashChunkSource.Factory dashChunkSourceFactory =
                new DefaultDashChunkSource.Factory(
                        new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER));
        return new DashMediaSource.Factory(dashChunkSourceFactory,
                manifestDataSourceFactory).createMediaSource(uri);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
    }

    private void playVideo(String uri) {
        DataSource.Factory sourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "InterView"));
        MediaSource source = new ExtractorMediaSource.Factory(sourceFactory)
                .createMediaSource(Uri.parse(uri));
        player.prepare(source);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setPlayWhenReady(true);
    }
}
