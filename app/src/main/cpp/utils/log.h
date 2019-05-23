//
// Created by android1 on 2019/4/22.
//

#ifndef VIDEOBOX_LOG_H
#define VIDEOBOX_LOG_H

#include <cstdio>
#include <android/log.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
#include <libavutil/pixdesc.h>
#include <libavutil/opt.h>
#include <libavutil/timestamp.h>
}

#define LOGV(TAG, format, ...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, format, ## __VA_ARGS__)
#ifdef DEBUG
#define LOGD(TAG, format, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG, format, ## __VA_ARGS__)
#else
#define LOGD(TAG, format, ...)
#endif
#define LOGW(TAG, format, ...) __android_log_print(ANDROID_LOG_WARN, TAG, format, ## __VA_ARGS__)
#define LOGE(TAG, format, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, format, ## __VA_ARGS__)


void logMetadata(AVDictionary* metadata, const char * tag);

void logContext(AVCodecContext *context, const char *tag, int isVideo);

void logStream(AVStream* stream, const char * tag, int isVideo);

void logPacket(AVPacket *packet, const char *tag);

void logFrame(AVFrame *frame, const char *tag, int isVideo);

#endif //VIDEOBOX_LOG_H
