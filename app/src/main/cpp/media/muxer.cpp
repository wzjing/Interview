//
// Created by wzjing on 2019/5/9.
//

#include "muxer.h"
#include "mux_listener.h"
#include <cstdio>
#include <cstring>
#include <utils/log.h>
#include "filter.h"

extern "C" {
#include <libavutil/frame.h>
}
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
    if (listener != nullptr) {
        mux_listener = new MuxLixtener(env, &listener);
    } else {
        mux_listener = nullptr;
    }
}

int save_yuv(uint8_t **buf, const int *wrap, int width, int height, const char *filename) {
    FILE *f;
    f = fopen(filename, "wb");

    for (int i = 0; i < height; i++) {
        fwrite(buf[0] + wrap[0] * i, 1, width, f);
    }

    for (int i = 0; i < height / 2; i++) {
        fwrite(buf[1] + wrap[1] * i, 1, width / 2, f);
    }

    for (int i = 0; i < height / 2; i++) {
        fwrite(buf[2] + wrap[2] * i, 1, width / 2, f);
    }
    fflush(f);
    fclose(f);
    LOGD(TAG, "savefile: %s\n", filename);
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wzjing_interview_MainActivity_filterFrame(JNIEnv *env, jobject thiz, jstring path) {

    const char *path_ = env->GetStringUTFChars(path, JNI_FALSE);

    int width = 1920;
    int height = 1080;
    FILE *file = fopen(path_, "rb");
    AVFrame *frame = av_frame_alloc();
    frame->width = 1920;
    frame->height = 1080;
    frame->format = AV_PIX_FMT_YUV420P;

    frame->linesize[0] = width;
    frame->linesize[1] = width / 2;
    frame->linesize[2] = width / 2;
    frame->data[0] = (uint8_t *) (malloc(sizeof(uint8_t) * width * height));
    frame->data[1] = (uint8_t *) (malloc(sizeof(uint8_t) * width * height / 4));
    frame->data[2] = (uint8_t *) (malloc(sizeof(uint8_t) * width * height / 4));
    fread(frame->data[0], 1, width * height, file);
    fread(frame->data[1], 1, width * height / 4, file);
    fread(frame->data[2], 1, width * height / 4, file);

    Filter filter;
    filter.init("gblur=sigma=20:steps=6[blur];[blur]format=pix_fmts=rgba",AV_PIX_FMT_YUV420P, AV_PIX_FMT_RGBA);
    filter.dumpGraph();
    filter.filter(frame);

    LOGD(TAG, "call java\n");
    jclass VideoEditor = env->FindClass("com/wzjing/interview/VideoEditor");
    jmethodID drawText = env->GetStaticMethodID(VideoEditor, "drawText",
                                                "([IIILjava/lang/String;)V");

    size_t arrSize = frame->linesize[0]*frame->height;
    jintArray pixels = env->NewIntArray(arrSize);

    auto *pPixels = (jint*) calloc(arrSize, sizeof(jint));
    for (int i = 0; i < arrSize; i++) {
        *(pPixels + i) = frame->data[0][i];
    }

    env->SetIntArrayRegion(pixels, 0, arrSize, pPixels);
    env->CallStaticVoidMethod(VideoEditor, drawText, pixels, width, height,
                              env->NewStringUTF("title"));
    LOGD(TAG, "call java finished\n");

    filter.init("format=pix_fmts=yuv420p",AV_PIX_FMT_RGBA, AV_PIX_FMT_YUV420P);
    filter.dumpGraph();
    filter.filter(frame);

    char new_path[129];
    snprintf(new_path, 128, "%s_filter.yuv", path_);

    save_yuv(frame->data, frame->linesize, frame->width, frame->height, new_path);

    env->ReleaseStringUTFChars(path, path_);
}
