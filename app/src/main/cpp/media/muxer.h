//
// Created by wzjing on 2019/5/9.
//

#ifndef INTERVIEW_MUXER_H
#define INTERVIEW_MUXER_H

#include <jni.h>

extern "C"
JNIEXPORT void JNICALL
Java_com_wzjing_interview_muxer_Muxer_nativeMux(JNIEnv *env, jobject instance,
                                                jobjectArray videoSource_,
                                                jobjectArray audioSource);

extern "C"
JNIEXPORT void JNICALL
Java_com_wzjing_interview_muxer_Muxer_nativeSetListener(JNIEnv *env, jobject thiz,
                                                        jobject listener);
#endif //INTERVIEW_MUXER_H
