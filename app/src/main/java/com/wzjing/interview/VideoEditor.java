package com.wzjing.interview;


import android.support.annotation.FloatRange;
import android.util.Log;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoEditor {

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avutil");
        System.loadLibrary("avfilter");
        System.loadLibrary("swscale");
        System.loadLibrary("swresample");
        System.loadLibrary("x264");
    }

    private final String TAG = VideoEditor.class.getSimpleName();

    private ExecutorService executor = null;


    public VideoEditor() {
        executor = Executors.newSingleThreadExecutor();
    }

    public void concatVideos(String outputFile, Map<String, File> videos, int fontSize, int titleDuration, boolean encode) {
        String[] titles = new String[videos.size()];
        String[] filenames = new String[videos.size()];
        int i = 0;
        for (String name : videos.keySet()) {
            titles[i] = name;
            i++;
        }
        i = 0;
        for (File file : videos.values()) {
            if (!file.exists()) {
                Log.e(TAG, "file not exists: " + file.getAbsolutePath());
                return;
            }
            filenames[i] = file.getAbsolutePath();
            Log.d(TAG, "input: " + filenames[i]);
            i++;
        }

        nativeConcatVideos(outputFile, filenames, titles, videos.size(), fontSize, titleDuration, encode);
    }

    public void addBGM(String outputFile, File video, File bgm, @FloatRange(from = 0, to = 2.0) float relativeBGMVolume) {
        if (!video.exists()) {
            Log.e(TAG, "video file not exists: " + video.getAbsolutePath());
            return;
        }

        if (!bgm.exists()) {
            Log.e(TAG, "bgm file not exists: " + bgm.getAbsolutePath());
        }

        nativeAddBGM(outputFile, video.getAbsolutePath(), bgm.getAbsolutePath(), relativeBGMVolume);
    }

    public void clip(String outputFile, File video, @FloatRange(from = 0) float from, @FloatRange(from = 0) float to) {
        if (!video.exists()) {
            Log.e(TAG, "video file not exists: " + video.getAbsolutePath());
            return;
        }

        nativeClip(outputFile, video.getAbsolutePath(), from, to);
    }

    private native boolean nativeConcatVideos(String outputFilename, String[] inputFilenames, String[] titles, int inputNum, int fontSize, int duration, boolean encode);

    private native boolean nativeAddBGM(String outputFilename, String inputFilename, String bgmFilename, float relativeBGMVolume);

    private native boolean nativeClip(String outputFilename, String inputFilename, float from, float to);


    public abstract class VideoEditorListener {

        abstract void onError();

        abstract void onProgress();

        abstract void onFinished();
    }

}
