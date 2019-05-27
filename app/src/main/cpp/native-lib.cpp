#include <jni.h>
#include <cstring>
#include <regex>
#include "mediautil/bgm.h"
#include "mediautil/concat.h"
#include "mediautil/log.h"

#define TAG "native-lib"



extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wzjing_interview_VideoEditor_nativeMuxVideos(JNIEnv *env, jobject instance,
                                                      jstring outputFilename,
                                                      jobjectArray inputFilenames,
                                                      jobjectArray titles,
                                                      jint inputNum,
                                                      jint fontSize, jint duration) {
    const char *output_filename = env->GetStringUTFChars(outputFilename, JNI_FALSE);
    const char **input_filenames = (const char **) malloc(sizeof(char *) * inputNum);
    for (int i = 0; i < inputNum; ++i) {
        auto jstr = (jstring) env->GetObjectArrayElement(inputFilenames, i);
        input_filenames[i] = env->GetStringUTFChars(jstr, JNI_FALSE);
    }
    const char **titles_ = (const char **) malloc(sizeof(char *) * inputNum);
    for (int i = 0; i < inputNum; ++i) {
        auto jstr = (jstring) env->GetObjectArrayElement(titles, i);
        titles_[i] = env->GetStringUTFChars(jstr, JNI_FALSE);
    }

    char *inputs_arr = (char*)calloc(512, sizeof(char));
    for (int i = 0; i < inputNum; ++i) {
        strcat(inputs_arr, input_filenames[i]);
        strcat(inputs_arr, ", ");
    }

    char *titles_arr = (char*)calloc(512, sizeof(char));
    for (int i = 0; i < inputNum; ++i) {
        strcat(titles_arr, titles_[i]);
        strcat(titles_arr, ", ");
    }

    LOGD(TAG, "nativeMuxVideos(%s, {%s}, {%s}, %d, %d, %d)\n", output_filename, inputs_arr,
         titles_arr, inputNum, fontSize, duration);


//    std::string cache_filename;
//
//    cache_filename = std::regex_replace(output_filename, std::regex(".[0-9a-zA-Z]+$"), "_cache.ts");
//
//    LOGD(TAG, "cache file: %s\n", cache_filename.c_str());


//    char cache_filename[128];
//    snprintf(cache_filename, sizeof(cache_filename), "%s.ts", output_filename);

    int ret = concat_encode(env, output_filename, input_filenames, titles_, inputNum,
                               fontSize,
                               duration);

//    if (ret == 0) {
//        ret = remux(cache_filename.c_str(), output_filename);
//    }


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
    const char *output_filename = env->GetStringUTFChars(outputFilename, JNI_FALSE);
    const char *input_filename = env->GetStringUTFChars(inputFilename, JNI_FALSE);
    const char *bgm_filename = env->GetStringUTFChars(bgmFilename, JNI_FALSE);

    int ret = add_bgm(output_filename, input_filename, bgm_filename, relativeBGMVolume);

    env->ReleaseStringUTFChars(outputFilename, output_filename);
    env->ReleaseStringUTFChars(inputFilename, input_filename);
    env->ReleaseStringUTFChars(bgmFilename, bgm_filename);

    return ret == 0;
}



