package com.wzjing.interview;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

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

public class EditActivity extends AppCompatActivity {

    private final String TAG = EditActivity.class.getSimpleName();

    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);


        PlayerView playerView = findViewById(R.id.playerView);
//        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
//        LoadControl loadControl = new DefaultLoadControl();
        player = ExoPlayerFactory.newSimpleInstance(this);
        playerView.setPlayer(player);

        new Thread(()->{
            String uri = testMux();
            runOnUiThread(()->{
                playVideo(uri);
            });
        }).start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stop();
    }

    private void playVideo(String uri) {
        DataSource.Factory sourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "InterView"));
        MediaSource source = new ExtractorMediaSource.Factory(sourceFactory)
                .createMediaSource(Uri.parse(uri));
        player.prepare(source);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setPlayWhenReady(true);
    }

    private String testMux() {
        File video0 = new File(Environment.getExternalStorageDirectory(), "Download/video0.mp4");
        File video1 = new File(Environment.getExternalStorageDirectory(), "Download/video1.mp4");
        VideoEditor editor = new VideoEditor();
        String uri = Environment.getExternalStorageDirectory() + File.separator + "mux.ts";
        HashMap<String, File> map = new HashMap<>();
        map.put("Question: how old are you", video0);
        map.put("Question: what is your skill", video1);
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
}
