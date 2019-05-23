package com.wzjing.interview;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.util.HashMap;

public class EditActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
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
}
