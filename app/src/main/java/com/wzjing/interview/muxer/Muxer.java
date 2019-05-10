package com.wzjing.interview.muxer;

import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.List;

public class Muxer {

    static {
        System.loadLibrary("media");
        System.loadLibrary("utils");
        System.loadLibrary("avutil");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avfilter");
        System.loadLibrary("swscale");
        System.loadLibrary("swresample");
        System.loadLibrary("x264");
    }

    private List<Pair<String, String>> sourceList;
    private String destPath;
    public MuxListener mListener;

    public Muxer(@NonNull List<Pair<String, String>> sourceList, String destPath) {
        this.sourceList = sourceList;
        this.destPath = destPath;
    }

    public void muxAsync() {
        new Thread(

        ).start();
    }

    public void mux() {
        if (sourceList == null || sourceList.size() == 0) {
            if (mListener != null) mListener.onError("no video or audio source");
            return;
        }
        String[] videoSource = new String[sourceList.size()];
        String[] audioSource = new String[sourceList.size()];
        for (int i = 0; i < sourceList.size(); ++i) {
            videoSource[i] = sourceList.get(i).first;
            audioSource[i] = sourceList.get(i).second;
        }

        nativeMux(videoSource, audioSource);
    }

    public void setMuxListener(MuxListener listener) {
        mListener = listener;
    }

    public static abstract class MuxListener {
        public abstract void onStart();

        public abstract void onProgress(int progress);

        public abstract void onFinish();

        public abstract void onError(String msg);
    }

    private native void nativeMux(String[] videoSource, String[] audioSource);

    private native void nativeSetListener(MuxListener listener);
}
