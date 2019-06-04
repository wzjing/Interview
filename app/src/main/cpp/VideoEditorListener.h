//
// Created by android1 on 2019/6/3.
//

#ifndef VIDEOUTIL_VIDEOEDITORLISTENER_H
#define VIDEOUTIL_VIDEOEDITORLISTENER_H

#include <jni.h>

class VideoEditorListener {
private:
    JNIEnv* env;
    jclass clazz;
    jobject * obj;
    jmethodID onError_;
    jmethodID onProgress_;
    jmethodID onFinished_;
public:
    VideoEditorListener(JNIEnv* env, jobject* obj);

    void onError(const char * msg);

    void onProgrss(int progress);

    void onFinished();
};


#endif //VIDEOUTIL_VIDEOEDITORLISTENER_H
