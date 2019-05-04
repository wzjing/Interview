#include <jni.h>
#include <string>
#include "utils/log.h"
#include "encoder/x264_encode.h"
#include <chrono>

extern "C" {
#include <libavcodec/avcodec.h>
}

float getSeconds() {
    auto now = std::chrono::high_resolution_clock::now();
    long nanoSec = now.time_since_epoch().count() / 1000000;
    float sec = nanoSec / 1000.0f;
    return sec;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wzjing_interview_MainActivity_test(JNIEnv *env, jobject instance, jstring input_,
                                            jstring output_) {
    const char *input = env->GetStringUTFChars(input_, 0);
    const char *output = env->GetStringUTFChars(output_, 0);

    float startTime = getSeconds();
    encode(input, output);
    LOGD("Total time: %f s\n", getSeconds() - startTime);

    env->ReleaseStringUTFChars(input_, input);
    env->ReleaseStringUTFChars(output_, output);
}