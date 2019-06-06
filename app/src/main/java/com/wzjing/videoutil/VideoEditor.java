package com.wzjing.videoutil;


import android.os.Handler;
import android.support.annotation.FloatRange;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoEditor {

    static {
        System.loadLibrary("x264");
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("swscale");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avfilter");
        System.loadLibrary("native-lib");
    }

    private final String TAG = VideoEditor.class.getSimpleName();

    private ExecutorService executor;


    public VideoEditor() {
        executor = Executors.newSingleThreadExecutor();
    }

    public void concatVideos(String outputFile, Map<String, File> videos, int fontSize, int titleDuration,
                             boolean encode, VideoEditorListener listener) {

        Handler handler = new Handler(msg -> {
            switch (msg.what) {
                case 0:
                    if (listener != null) listener.onError((String) msg.obj);
                    break;
                case 1:
                    if (listener != null) listener.onProgress(msg.arg1);
                    break;
                case 2:
                    if (listener != null) listener.onFinished();
                    break;
            }
            return true;
        });
        executor.submit(() -> {
            String[] titles = new String[videos.size()];
            String[] filenames = new String[videos.size()];

            int i = 0;
            for (String s : videos.keySet()) {
                titles[i] = s;
                i++;
            }
            i = 0;
            for (File file : videos.values()) {
                if (!file.exists()) {
                    handler.obtainMessage(0, "file not exists: " + file.getAbsolutePath()).sendToTarget();
                    return;
                }
                filenames[i] = file.getAbsolutePath();
                i++;
            }

            nativeConcatVideos(outputFile, filenames, titles, videos.size(), fontSize, titleDuration,
                    encode, new VideoEditorListener() {
                        @Override
                        void onError(String msg) {
                            handler.obtainMessage(0, msg).sendToTarget();
                        }

                        @Override
                        void onProgress(int progress) {
                            handler.obtainMessage(1, progress, 0).sendToTarget();
                        }

                        @Override
                        void onFinished() {
                            handler.obtainMessage(2).sendToTarget();
                        }
                    });

        });
    }

    public void addBGM(String outputFile, File video, File bgm,
                       @FloatRange(from = 0, to = 2.0) float relativeBGMVolume,
                       VideoEditorListener listener) {
        Handler handler = new Handler(msg -> {
            switch (msg.what) {
                case 0:
                    if (listener != null) listener.onError((String) msg.obj);
                    break;
                case 1:
                    if (listener != null) listener.onProgress(msg.arg1);
                    break;
                case 2:
                    if (listener != null) listener.onFinished();
                    break;
            }
            return true;
        });

        executor.submit(() -> {

            if (!video.exists()) {
                handler.obtainMessage(0, "video file not exists: " + video.getAbsolutePath())
                        .sendToTarget();
                return;
            }

            if (!bgm.exists()) {
                handler.obtainMessage(0, "bgm file not exists: " + bgm.getAbsolutePath())
                        .sendToTarget();
                return;
            }

            nativeAddBGM(outputFile, video.getAbsolutePath(), bgm.getAbsolutePath(), relativeBGMVolume,
                    new VideoEditorListener() {
                        @Override
                        void onError(String msg) {
                            handler.obtainMessage(0, msg).sendToTarget();
                        }

                        @Override
                        void onProgress(int progress) {
                            handler.obtainMessage(1, progress, 0).sendToTarget();
                        }

                        @Override
                        void onFinished() {
                            handler.obtainMessage(2).sendToTarget();
                        }
                    });
        });
    }

    public void clip(String outputFile, File video, @FloatRange(from = 0) float from,
                     @FloatRange(from = 0) float to,
                     VideoEditorListener listener) {
        Handler handler = new Handler(msg -> {
            switch (msg.what) {
                case 0:
                    if (listener != null) listener.onError((String) msg.obj);
                    break;
                case 1:
                    if (listener != null) listener.onProgress(msg.arg1);
                    break;
                case 2:
                    if (listener != null) listener.onFinished();
                    break;
            }
            return true;
        });

        executor.submit(() -> {
            if (!video.exists()) {
                handler.obtainMessage(0, "file not found: " + video.getAbsolutePath()).sendToTarget();
                return;
            }

            nativeClip(outputFile, video.getAbsolutePath(), from, to, new VideoEditorListener() {
                @Override
                void onError(String msg) {
                    handler.obtainMessage(0, msg).sendToTarget();
                }

                @Override
                void onProgress(int progress) {
                    handler.obtainMessage(1, progress, 0).sendToTarget();
                }

                @Override
                void onFinished() {
                    handler.obtainMessage(2).sendToTarget();
                }
            });
        });

    }

    private native boolean nativeConcatVideos(String outputFilename, String[] inputFilenames,
                                              String[] titles, int inputNum, int fontSize, int duration,
                                              boolean encode, VideoEditorListener listener);

    private native boolean nativeAddBGM(String outputFilename, String inputFilename, String bgmFilename,
                                        float relativeBGMVolume,
                                        VideoEditorListener listener);

    private native boolean nativeClip(String outputFilename, String inputFilename, float from, float to,
                                      VideoEditorListener listener);


    public static abstract class VideoEditorListener {

        abstract void onError(String msg);

        abstract void onProgress(int progress);

        abstract void onFinished();
    }

}
