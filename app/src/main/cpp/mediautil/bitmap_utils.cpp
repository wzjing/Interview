//
// Created by android1 on 2019/5/22.
//

#include "bitmap_utils.h"
#include <cstring>
#include "log.h"

#define JNI_CLASS(sub_clazz) "com/wzjing/interview/" sub_clazz

const char *Bimap_TAG = "bitmap_utils";

void drawText(JNIEnv *env, uint8_t *data, int width, int height, const char *text, int font_size, int rotation) {

    jclass TextFilter = env->FindClass(JNI_CLASS("jni/TextFilter"));
    jmethodID constructor = env->GetMethodID(TextFilter, "<init>", "(II)V");
    jobject textFilter = env->NewObject(TextFilter, constructor, width, height);

    jmethodID getBitmap = env->GetMethodID(TextFilter, "getBitmap", "()Landroid/graphics/Bitmap;");
    jobject bitmap = env->CallObjectMethod(textFilter, getBitmap);

    uint8_t *pixels = nullptr;
    size_t size = 0;

    LOGD(Bimap_TAG, "native filled bitmap");
    AndroidBitmapInfo info = {0};
    AndroidBitmap_getInfo(env, bitmap, &info);
    size = info.stride * height;
    LOGD(Bimap_TAG, "Strip: %d\n", info.stride);
    AndroidBitmap_lockPixels(env, bitmap, (void **) &pixels);
    memcpy(pixels, data, size);
    AndroidBitmap_unlockPixels(env, bitmap);

    LOGD(Bimap_TAG, "native draw text");
    jmethodID drawText = env->GetMethodID(TextFilter, "drawText", "(Ljava/lang/String;II)V");;
    env->CallVoidMethod(textFilter, drawText, env->NewStringUTF(text), font_size, rotation);

    LOGD(Bimap_TAG, "native get result bitmap");
    AndroidBitmap_lockPixels(env, bitmap, (void **) &pixels);
    memcpy(data, pixels, size);
    AndroidBitmap_unlockPixels(env, bitmap);

    // free java bitmap object
    LOGD(Bimap_TAG, "native free bitmap");
    jmethodID free = env->GetMethodID(TextFilter, "free", "()V");;
    env->CallVoidMethod(textFilter, free);
    textFilter = nullptr;
}
