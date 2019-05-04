package com.wzjing.interview.encode;

public abstract class Encoder {

    abstract boolean start();

    abstract void encodeFrame(byte[] data);

    abstract void stop();

    abstract int getWidth();

    abstract int getHeight();

    abstract void addCallback(Callback cb);

    public static interface Callback {
        // handle the encoded data
        void onEncodec(byte[] data);
    }
}
