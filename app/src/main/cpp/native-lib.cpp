#include <jni.h>
#include <cstring>
#include <regex>
#include "mediautil/bgm.h"
#include "mediautil/concat.h"
#include "mediautil/log.h"
#include "mediautil/clip.h"
#include "VideoEditorListener.h"
#include "mediautil/remux.h"

#define TAG "native-lib"


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_videoutil_VideoEditor_nativeConcatVideos(JNIEnv *env, jobject /* this */,
                                                         jstring outputFilename,
                                                         jobjectArray inputFilenames,
                                                         jobjectArray titles,
                                                         jint inputNum,
                                                         jint fontSize, jint duration,
                                                         jboolean encode,
                                                         jobject listener) {
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

#ifdef DEBUG
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
#endif

    VideoEditorListener mListener(env, &listener);
    int ret = 0;

    if (encode) {
        ret = concat_encode(env, output_filename_, input_filenames_, titles_, inputNum,
                            fontSize, duration, [&mListener](int progress) -> void {
                    mListener.onProgrss(progress);
                });
    } else {
        std::string cache_filename;

        cache_filename = std::regex_replace(output_filename_, std::regex(".[0-9a-zA-Z]+$"),
                                            "_cache.ts");

        LOGD(TAG, "cache file: %s\n", cache_filename.c_str());

        Mp4Meta *meta = nullptr;
        getMeta(&meta, input_filenames_[0]);

        ret = concat_no_encode(env, output_filename_, input_filenames_, titles_, inputNum, fontSize,
                               duration, [&mListener](int progress) -> void {
                    mListener.onProgrss(progress * 90 / 100);
                });


        if (ret == 0) {
            ret = remux(cache_filename.c_str(), output_filename_, meta, [&mListener](int progress) -> void {
                mListener.onProgrss(90 + progress * 10 / 100);
            });
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

    if (ret == 0) {
        mListener.onFinished();
        return 1;
    } else {
        mListener.onError("concat video error");
        return 0;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_videoutil_VideoEditor_nativeAddBGM(JNIEnv *env, jobject /* this */,
                                                   jstring outputFilename, jstring inputFilename,
                                                   jstring bgmFilename, jfloat relativeBGMVolume,
                                                   jobject listener) {
    const char *output_filename = env->GetStringUTFChars(outputFilename, nullptr);
    const char *input_filename = env->GetStringUTFChars(inputFilename, nullptr);
    const char *bgm_filename = env->GetStringUTFChars(bgmFilename, nullptr);

    LOGD(TAG, "nativeAddBGM(%s, %s, %s, %f)\n", output_filename, input_filename, bgm_filename,
         relativeBGMVolume);

    VideoEditorListener mListener(env, &listener);

    int ret = add_bgm(output_filename, input_filename, bgm_filename, relativeBGMVolume,
                      [&mListener](int progress) -> void {
                          mListener.onProgrss(progress);
                      });

    env->ReleaseStringUTFChars(outputFilename, output_filename);
    env->ReleaseStringUTFChars(inputFilename, input_filename);
    env->ReleaseStringUTFChars(bgmFilename, bgm_filename);

    if (ret == 0) {
        mListener.onFinished();
        return 1;
    } else {
        mListener.onError("add bgm error");
        return 0;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_videoutil_VideoEditor_nativeClip(JNIEnv *env, jobject /* this */,
                                                 jstring output_filename, jstring input_filename,
                                                 jfloat from, jfloat to, jobject listener) {
    const char *output_filename_ = env->GetStringUTFChars(output_filename, nullptr);
    const char *input_filename_ = env->GetStringUTFChars(input_filename, nullptr);

    LOGD(TAG, "nativeClip(%s, %s, %f, %f)\n", output_filename_, input_filename_, from, to);

    VideoEditorListener mListener(env, &listener);
    int ret = clip(output_filename_, input_filename_, from, to, [&mListener](int progress) -> void {
        mListener.onProgrss(progress);
    });

    env->ReleaseStringUTFChars(output_filename, output_filename_);
    env->ReleaseStringUTFChars(input_filename, input_filename_);

    if (ret == 0) {
        mListener.onFinished();
        return 1;
    } else {
        mListener.onError("clip error");
        return 0;
    }
}