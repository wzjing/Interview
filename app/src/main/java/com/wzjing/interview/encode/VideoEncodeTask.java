package com.wzjing.interview.encode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class VideoEncodeTask implements Runnable {

    private final String TAG = "VideoEncodeTask";

    private MediaFormat format;
    private int width;
    private int height;

    // IO
    private final int MAX_CACHE_SIZE = 10;
    private ArrayBlockingQueue<Pair<byte[], Boolean>> frameQueue;
    private File destFile;
    private EncodeManager manager;

    public VideoEncodeTask(EncodeManager manager, int width, int height, File destFile) {
        this.manager = manager;
        this.destFile = destFile;
        this.width = width;
        this.height = height;
    }

    @Override
    public void run() {
        // Init encoder
        frameQueue = new ArrayBlockingQueue<>(MAX_CACHE_SIZE);
        format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_WIDTH, width);
        format.setInteger(MediaFormat.KEY_HEIGHT, height);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 7200000);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 12);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        MediaCodec mediaCodec;
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            Log.e(TAG, "unable to create MediaCodec");
            e.printStackTrace();
            manager.notifyEvent(EncodeManager.MSG_ERROR, "unable to create MediaCodec");
            return;
        }

        if (Build.VERSION.SDK_INT > 21) {
            for (String type : mediaCodec.getCodecInfo().getSupportedTypes()) {
                Log.d(TAG, "Type: " + type);
            }
            for (int pix : mediaCodec.getCodecInfo().getCapabilitiesForType("video/avc").colorFormats) {
                Log.d(TAG, "Pixel: " + Integer.toHexString(pix));
            }
        }

        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();

        OutputStream oStream;
        try {
            oStream = new FileOutputStream(destFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            manager.notifyEvent(EncodeManager.MSG_ERROR, "File not found");
            return;
        }
        boolean inputEof = false;
        manager.notifyEvent(EncodeManager.MSG_START, null);

        int encodeCount = 0;
        int receivedCount = 0;

        long timestamp = 0;
        long timestep = 1000 / 30;

        Pair<byte[], Boolean> inputBuffer = null;
//        byte[] tmpBuf = new byte[width * height * 3 / 2];
        while (true) {
            if (!inputEof || frameQueue.size() > 0) {
                if (inputBuffer == null) inputBuffer = frameQueue.poll();
                if (inputBuffer != null) {
                    int inIdx = mediaCodec.dequeueInputBuffer(1000);
                    if (inIdx >= 0) {
                        ByteBuffer inBuf = mediaCodec.getInputBuffers()[inIdx];
                        inBuf.clear();
                        inputEof = inputBuffer.second;
                        inBuf.put(inputBuffer.first, 0, inputBuffer.first.length);
                        mediaCodec.queueInputBuffer(inIdx, 0, inputBuffer.first.length, timestamp++,
                                inputEof ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                        inputBuffer = null;
                        encodeCount++;
                        Log.d(TAG, String.format("run: input --- %d\t%s", encodeCount, inputEof ? "EOF" : "->"));
                    }
                }
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outIdx = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
            boolean outputEof = false;
            switch (outIdx) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.v(TAG, "run: output -- try again later");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.v(TAG, "run: output -- format changed");
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.v(TAG, "run: output -- buffer changed");
                    break;
                default:
                    receivedCount++;
                    outputEof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0;
                    ByteBuffer outBuffer = mediaCodec.getOutputBuffers()[outIdx];
                    byte[] tmpBuf = new byte[bufferInfo.size];
                    Log.d(TAG, String.format("run: output -- %d\t%d\t%s", receivedCount, bufferInfo.size, outputEof ? "EOF" : "->"));
                    outBuffer.get(tmpBuf);
                    try {
                        oStream.write(tmpBuf, 0, bufferInfo.size);
                    } catch (IOException e) {
                        e.printStackTrace();
                        e.printStackTrace(System.err);
                    }
                    mediaCodec.releaseOutputBuffer(outIdx, false);
                    break;
            }
            if (outputEof) break;

        }

        Log.d(TAG, "run: encode done");

        // flush
        mediaCodec.flush();

        // close
        try {
            oStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            manager.notifyEvent(EncodeManager.MSG_ERROR, "Error while close file");
        }
        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            //No need to handle this exception.
            e.printStackTrace();
        }
        manager.notifyEvent(EncodeManager.MSG_FINISH, null);
    }

    public void addFrame(byte[] data, boolean eof) {
        if (frameQueue.size() >= MAX_CACHE_SIZE) {
            frameQueue.poll();
            Log.e(TAG, "addFrame: queue full, abandon last one");
        }
        frameQueue.add(new Pair<>(data, eof));
    }


}
