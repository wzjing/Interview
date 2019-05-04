//
// Created by wzjing on 2019/5/4.
//

#ifndef INTERVIEW_ENCODER_H
#define INTERVIEW_ENCODER_H

#include <jni.h>

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_encode_FFmpegEncoder_nativeOpenVideoEncoder(JNIEnv *env, jobject instance) {

    // TODO

}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_encode_FFmpegEncoder_nativeSendVideoFrame(JNIEnv *env, jobject instance, jint width,
                                                               jint height, jbyteArray data_) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    // TODO

    env->ReleaseByteArrayElements(data_, data, 0);
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_encode_FFmpegEncoder_nativeCloseVideoEncoder(JNIEnv *env, jobject instance) {

    // TODO

}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_encode_FFmpegEncoder_nativeOpenAudioEncoder(JNIEnv *env, jobject instance) {

    // TODO

}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_encode_FFmpegEncoder_nativeSendAudioFrame(JNIEnv *env, jobject instance,
                                                               jint sampleNumber, jbyteArray data_) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    // TODO

    env->ReleaseByteArrayElements(data_, data, 0);
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_encode_FFmpegEncoder_nativeCloseAudioEncoder(JNIEnv *env, jobject instance) {

    // TODO

}

#endif //INTERVIEW_ENCODER_H
