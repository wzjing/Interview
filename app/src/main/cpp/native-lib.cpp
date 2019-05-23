#include <jni.h>
#include "utils/log.h"
#include "concat/concat_add_title.h"
#include "bgm/mix_bgm.h"

#define TAG "native-lib"


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_VideoEditor_nativeMuxVideos(JNIEnv *env, jobject instance,
                                                      jstring outputFilename,
                                                      jobjectArray inputFilenames,
                                                      jobjectArray titles,
                                                      jint inputNum,
                                                      jint fontSize, jint duration) {
    const char *output_filename = env->GetStringUTFChars(outputFilename, 0);
    const char **input_filenames = (const char **) malloc(sizeof(char *) * inputNum);
    for (int i = 0; i < inputNum; ++i) {
        auto jstr = (jstring) env->GetObjectArrayElement(inputFilenames, i);
        input_filenames[i] = env->GetStringUTFChars(jstr, 0);
    }
    const char **titles_ = (const char **) malloc(sizeof(char *) * inputNum);
    for (int i = 0; i < inputNum; ++i) {
        auto jstr = (jstring) env->GetObjectArrayElement(titles, i);
        titles_[i] = env->GetStringUTFChars(jstr, 0);
    }

    int ret = concat_add_title(env, output_filename, input_filenames, titles_, inputNum, fontSize,
                               duration);

    for (int i = 0; i < inputNum; ++i) {
        auto jstr = (jstring) env->GetObjectArrayElement(titles, i);
        env->ReleaseStringUTFChars(jstr, titles_[i]);
    }
    free(input_filenames);
    for (int i = 0; i < inputNum; ++i) {
        auto jstr = (jstring) env->GetObjectArrayElement(inputFilenames, i);
        env->ReleaseStringUTFChars(jstr, input_filenames[i]);
    }
    free(input_filenames);
    env->ReleaseStringUTFChars(outputFilename, output_filename);

    return ret == 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_VideoEditor_nativeAddBGM(JNIEnv *env, jobject instance,
                                                   jstring outputFilename,
                                                   jstring inputFilename, jstring bgmFilename,
                                                   jfloat relativeBGMVolume) {
    const char *output_filename = env->GetStringUTFChars(outputFilename, 0);
    const char *input_filename = env->GetStringUTFChars(inputFilename, 0);
    const char *bgm_filename = env->GetStringUTFChars(bgmFilename, 0);

    int ret = mix_bgm(output_filename, input_filename, bgm_filename, relativeBGMVolume);

    env->ReleaseStringUTFChars(outputFilename, output_filename);
    env->ReleaseStringUTFChars(inputFilename, input_filename);
    env->ReleaseStringUTFChars(bgmFilename, bgm_filename);

    return ret == 0;
}



