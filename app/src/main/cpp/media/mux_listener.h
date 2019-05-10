//
// Created by wzjing on 2019/5/9.
//

#ifndef INTERVIEW_MUX_LISTENER_H
#define INTERVIEW_MUX_LISTENER_H

#include <jni.h>

class MuxLixtener {
private:
    JNIEnv *java_env;
    jclass java_class;
    jobject* java_object;
public:
    MuxLixtener(JNIEnv *env, jobject *obj);

    void onStart();

    void onProgress(int progress);

    void onFinish();

    void onError(const char *err);
};

#endif //INTERVIEW_MUX_LISTENER_H
