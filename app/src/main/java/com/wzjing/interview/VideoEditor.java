package com.wzjing.interview;


import android.support.annotation.FloatRange;
import android.util.Log;

import java.io.File;
import java.util.Map;

public class VideoEditor {

    private final String TAG = VideoEditor.class.getSimpleName();

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("utils");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avutil");
        System.loadLibrary("avfilter");
        System.loadLibrary("swscale");
        System.loadLibrary("swresample");
        System.loadLibrary("postproc");
        System.loadLibrary("x264");
    }


    public VideoEditor() {
    }

    public boolean muxVideos(String outputFile, Map<String, File> videos, int fontSize, int titleDuration) {
        String[] titles = new String[videos.size()];
        String[] filenames = new String[videos.size()];
        int i = 0;
        for (String name: videos.keySet()) {
            titles[i] = name;
            i++;
        }
        i = 0;
        for (File file : videos.values()) {
            if (!file.exists()) {
                Log.e(TAG, "file not exists: " + file.getAbsolutePath());
                return false;
            }
            filenames[i] = file.getAbsolutePath();
            Log.d(TAG, "input: " + filenames[i]);
            i++;
        }

        return nativeMuxVideos(outputFile, filenames, titles, videos.size(), fontSize, titleDuration);
    }

    public boolean addBGM(String outputFile, File video, File bgm, @FloatRange(from = 0, to = 2.0) float relativeBGMVolume) {
        if (!video.exists()) {
            Log.e(TAG, "video file not exists: "+video.getAbsolutePath());
            return false;
        }

        if (!bgm.exists()) {
            Log.e(TAG, "bgm file not exists: "+bgm.getAbsolutePath());
        }

        return nativeAddBGM(outputFile, video.getAbsolutePath(), bgm.getAbsolutePath(), relativeBGMVolume);
    }

    private native boolean nativeMuxVideos(String outputFilename, String[] inputFilenames, String[] titles, int inputNum, int fontSize, int duration);

    private native boolean nativeAddBGM(String outputFilename, String inputFilename, String bgmFilename, float relativeBGMVolume);

}
