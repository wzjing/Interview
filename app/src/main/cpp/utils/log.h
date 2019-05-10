//
// Created by android1 on 2019/4/22.
//

#ifndef VIDEOBOX_LOG_H
#define VIDEOBOX_LOG_H

#include <cstdio>
#include <android/log.h>

#define LOGV(TAG, format, ...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, format, ## __VA_ARGS__)
#ifdef DEBUG
#define LOGD(TAG, format, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG, format, ## __VA_ARGS__)
#else
#define LOGD(TAG, format, ...)
#endif
#define LOGE(TAG, format, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, format, ## __VA_ARGS__)

#endif //VIDEOBOX_LOG_H
