package com.wzjing.interview.encode;


/**
 * 基于ffmpeg的编码器，使用libx264进行软件编码，利用CPU的多核计算能力进行软件编码
 */
public class FFmpegEncoder {
    static {
        System.loadLibrary("x264");
        System.loadLibrary("avcodec");
        System.loadLibrary("format");
        System.loadLibrary("filter");
        System.loadLibrary("avutil");
        System.loadLibrary("native-lib");
    }

    // open video encoder, get ready for encoding
    void openVideoEncoder() {

    }

    // send one YUV420p frame to video encoder
    void sendVideoFrame(int width, int height, byte[] data) {

    }

    // close encoder and flush un-encode frames
    void closeVideoEncoder() {

    }

    // open audio encoder, get ready for audio encoding
    void openAudioEncoder() {

    }

    // send one PCM frame to audio encoder, an aac frame usually contains 1024 pcm sample
    void sendAudioFrame(int sampleNumber, byte[] data) {

    }

    void closeAudioEncoder() {

    }

    native boolean nativeOpenVideoEncoder();

    native boolean nativeSendVideoFrame(int width, int height, byte[] data);

    native boolean nativeCloseVideoEncoder();

    native boolean nativeOpenAudioEncoder();

    native boolean nativeSendAudioFrame(int sampleNumber, byte[] data);

    native boolean nativeCloseAudioEncoder();

}
