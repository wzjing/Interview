#include <jni.h>
#include <cstring>
#include <regex>
#include "mediautil/bgm.h"
#include "mediautil/concat.h"
#include "mediautil/log.h"
#include "mediautil/clip.h"

#define TAG "native-lib"


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_VideoEditor_nativeConcatVideos(JNIEnv *env, jobject /* this */,
                                                         jstring outputFilename,
                                                         jobjectArray inputFilenames,
                                                         jobjectArray titles,
                                                         jint inputNum,
                                                         jint fontSize, jint duration,
                                                         jboolean encode) {
    const char *output_filename_ = env->GetStringUTFChars(outputFilename, nullptr);
    const char **input_filenames_ = (const char **) malloc(sizeof(char *) * inputNum);
    for (int i = 0; i < inputNum; ++i) {
        auto str = (jstring) env->GetObjectArrayElement(inputFilenames, i);
        input_filenames_[i] = env->GetStringUTFChars(str, nullptr);
    }
    const char **titles_ = (const char **) malloc(sizeof(char *) * inputNum);
    for (int i = 0; i < inputNum; ++i) {
        auto str = (jstring) env->GetObjectArrayElement(titles, i);
        titles_[i] = env->GetStringUTFChars(str, nullptr);
    }

    char inputs_arr[512];
    for (int i = 0; i < inputNum; ++i) {
        strcat(inputs_arr, input_filenames_[i]);
        strcat(inputs_arr, ", ");
    }

    char titles_arr[512];
    for (int i = 0; i < inputNum; ++i) {
        strcat(titles_arr, titles_[i]);
        strcat(titles_arr, ", ");
    }

    LOGD(TAG, "nativeMuxVideos(%s, {%s}, {%s}, %d, %d, %d, %s)\n", output_filename_, inputs_arr,
         titles_arr, inputNum, fontSize, duration, encode ? "TRUE" : "FALSE");

    int ret = 0;

    if (encode) {
        ret = concat_encode(env, output_filename_, input_filenames_, titles_, inputNum,
                            fontSize,
                            duration);
    } else {
        std::string cache_filename;

        cache_filename = std::regex_replace(output_filename_, std::regex(".[0-9a-zA-Z]+$"),
                                            "_cache.ts");

        LOGD(TAG, "cache file: %s\n", cache_filename.c_str());

        ret = concat_no_encode(env, output_filename_, input_filenames_, titles_, inputNum, fontSize,
                               duration);


        if (ret == 0) {
            ret = remux(cache_filename.c_str(), output_filename);
        }

        remove(cache_filename.c_str());
    }


    free(input_filenames_);
    for (int i = 0; i < inputNum; ++i) {
        auto jstr = (jstring) env->GetObjectArrayElement(inputFilenames, i);
        env->ReleaseStringUTFChars(jstr, input_filenames_[i]);
    }
    free(input_filenames_);
    env->ReleaseStringUTFChars(outputFilename, output_filename_);

    return (jboolean) (ret == 0);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_VideoEditor_nativeAddBGM(JNIEnv *env, jobject /* this */,
                                                   jstring outputFilename,
                                                   jstring inputFilename, jstring bgmFilename,
                                                   jfloat relativeBGMVolume) {
    const char *output_filename = env->GetStringUTFChars(outputFilename, nullptr);
    const char *input_filename = env->GetStringUTFChars(inputFilename, nullptr);
    const char *bgm_filename = env->GetStringUTFChars(bgmFilename, nullptr);

    LOGD(TAG, "nativeAddBGM(%s, %s, %s, %f)\n", output_filename, input_filename, bgm_filename,
         relativeBGMVolume);

    int ret = add_bgm(output_filename, input_filename, bgm_filename, relativeBGMVolume);

    env->ReleaseStringUTFChars(outputFilename, output_filename);
    env->ReleaseStringUTFChars(inputFilename, input_filename);
    env->ReleaseStringUTFChars(bgmFilename, bgm_filename);

    return (jboolean) (ret == 0);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_VideoEditor_nativeClip(JNIEnv *env, jobject /* this */,
                                                 jstring output_filename,
                                                 jstring input_filename, jfloat from, jfloat to) {
    const char *output_filename_ = env->GetStringUTFChars(output_filename, nullptr);
    const char *input_filename_ = env->GetStringUTFChars(input_filename, nullptr);

    LOGD(TAG, "nativeClip(%s, %s, %f, %f)\n", output_filename_, input_filename_, from, to);

    int ret = clip(output_filename_, input_filename_, from, to);

    env->ReleaseStringUTFChars(output_filename, output_filename_);
    env->ReleaseStringUTFChars(input_filename, input_filename_);

    return (jboolean) (ret == 0);
}