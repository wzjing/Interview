package com.wzjing.interview.encode;

public class AudioRecordTask implements Runnable {

    private int sampleRate;
    private int channels;

    AudioRecordTask(int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    @Override
    public void run() {
        
    }
}
