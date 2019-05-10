//
// Created by wzjing on 2019/5/9.
//

#include "mux_listener.h"

MuxLixtener::MuxLixtener(JNIEnv *env, jobject *obj) {
    java_env = env;
    java_class = env->FindClass("com/wzjing/interview/muxer/Muxer$MuxListener");
    java_object = obj;
}

void MuxLixtener::onStart() {
    jmethodID method_id = java_env->GetMethodID(java_class, "onStart", "()V");
    java_env->CallVoidMethod(*java_object, method_id);
}

void MuxLixtener::onProgress(int progress) {
    jmethodID method_id = java_env->GetMethodID(java_class, "onProgress", "(I)V");
    java_env->CallVoidMethod(*java_object, method_id, progress);
}

void MuxLixtener::onFinish() {
    jmethodID method_id = java_env->GetMethodID(java_class, "onFinish", "()V");
    java_env->CallVoidMethod(*java_object, method_id);
}

void MuxLixtener::onError(const char *err) {
    jmethodID method_id = java_env->GetMethodID(java_class, "onError", "(Ljava/lang/String;)V");
    java_env->CallVoidMethod(*java_object, method_id, java_env->NewStringUTF(err));
}
