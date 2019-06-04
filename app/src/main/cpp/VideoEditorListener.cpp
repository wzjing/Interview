//
// Created by android1 on 2019/6/3.
//

#include "VideoEditorListener.h"

VideoEditorListener::VideoEditorListener(JNIEnv* env, jobject *obj): env(env), obj(obj) {
    clazz = env->GetObjectClass(*obj);
    onError_ = env->GetMethodID(clazz, "onError", "(Ljava/lang/String;)V");
    onProgress_ = env->GetMethodID(clazz, "onProgress", "(I)V");
    onFinished_ = env->GetMethodID(clazz, "onFinished", "()V");
}

void VideoEditorListener::onError(const char *msg) {
    if (onError_) {
        env->CallVoidMethod(*obj, onError_, env->NewStringUTF(msg));
    }
}

void VideoEditorListener::onProgrss(int progress) {
    if (onProgress_) {
        env->CallVoidMethod(*obj, onProgress_, progress);
    }
}

void VideoEditorListener::onFinished() {
    if (onFinished_) {
        env->CallVoidMethod(*obj, onFinished_, onFinished_);
    }
}
