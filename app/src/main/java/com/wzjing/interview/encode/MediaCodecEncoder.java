package com.wzjing.interview.encode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 基于Android MediaCodec API的编码器，支持在硬件加速可用时自动使用硬件加速进行编码
 */
public class MediaCodecEncoder extends Encoder {

    private final String TAG = "MediaCodecEncoder";

    private MediaCodec mediaCodec;
    private MediaFormat format;
    private int width;
    private int height;
    private Callback mCallback;

    MediaCodecEncoder(int width, int height) {
        this.width = width;
        this.height = height;
        format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_WIDTH, width);
        format.setInteger(MediaFormat.KEY_HEIGHT, height);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
    }


    @Override
    boolean start() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            Log.e(TAG, "unable to create MediaCodec");
            e.printStackTrace();
            return false;
        }
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        Log.d(TAG, "encoder start");
        return true;
    }

    @Override
    void encodeFrame(byte[] data) {
        Log.d(TAG, "encodeFrame: start");
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

        int inIdx = mediaCodec.dequeueInputBuffer(-1);

        if (inIdx >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inIdx];
            inputBuffer.clear();
            inputBuffer.put(data);
            mediaCodec.queueInputBuffer(inIdx, 0, data.length, 0, 0);
            Log.d(TAG, "encodeFrame: send success");
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outIdx = mediaCodec.dequeueOutputBuffer(bufferInfo, 10);
        while (outIdx >= 0) {
            Log.d(TAG, "encodeFrame: receive success");
            ByteBuffer outBuffer = outputBuffers[outIdx];
            byte[] outData = new byte[bufferInfo.size];
            outBuffer.get(outData);
            if (mCallback != null) {
                mCallback.onEncodec(outData);
            }
            mediaCodec.releaseOutputBuffer(outIdx, false);
            outIdx = mediaCodec.dequeueOutputBuffer(bufferInfo, 10);
        }
        Log.d(TAG, "encodeFrame: finished");
    }

    @Override
    void stop() {
        if (mediaCodec != null) {
            try {
                mediaCodec.stop();
                mediaCodec.release();
            } catch (Exception e) {
                //No need to handle this exception.
                e.printStackTrace();
            }
        }
        Log.d(TAG, "encoder stopped");
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    void addCallback(Callback cb) {
        mCallback = cb;
    }
}
