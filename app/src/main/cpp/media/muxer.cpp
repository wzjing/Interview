//
// Created by wzjing on 2019/5/9.
//

#include "muxer.h"
#include "mux_listener.h"
#include <cstdio>
#include <cstring>
#include <utils/log.h>

const char *TAG = "native-muxer";

MuxLixtener *mux_listener;

extern "C"
JNIEXPORT void JNICALL
Java_com_wzjing_interview_muxer_Muxer_nativeMux(JNIEnv *env, jobject instance,
                                                jobjectArray videoSource,
                                                jobjectArray audioSource) {
    int size = env->GetArrayLength(videoSource);
    auto **video_source = new char *[size];
    auto **audio_source = new char *[size];
    for (int i = 0; i < size; ++i) {
        auto video = (jstring) env->GetObjectArrayElement(videoSource, i);
        video_source[i] = new char[env->GetStringLength(video)];
        strcpy(video_source[i], env->GetStringUTFChars(video, JNI_FALSE));

        auto audio = (jstring) env->GetObjectArrayElement(audioSource, i);
        audio_source[i] = new char[env->GetStringLength(audio)];
        strcpy(audio_source[i], env->GetStringUTFChars(audio, JNI_FALSE));
    }

    for (int i = 0; i < size; ++i) {
        LOGD(TAG, "Video: %s -- Audio: %s", video_source[i], audio_source[i]);
    }

    if (mux_listener != nullptr) {
        mux_listener->onStart();
        mux_listener->onProgress(10);
        mux_listener->onError("native mux error");
        mux_listener->onFinish();
    }

    delete mux_listener;
    for (int i = 0; i < size; ++i) {
        delete[] video_source[i];
        delete[] audio_source[i];
    }
    delete[] video_source;
    delete[] audio_source;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wzjing_interview_muxer_Muxer_nativeSetListener(JNIEnv *env, jobject thiz,
                                                        jobject listener) {
    mux_listener = new MuxLixtener(env, &listener);
}
