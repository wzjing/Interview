//
// Created by android1 on 2019/5/22.
//

#ifndef VIDEOUTIL_BITMAP_UTILS_H
#define VIDEOUTIL_BITMAP_UTILS_H

#include <android/bitmap.h>
#include <jni.h>

void drawText(JNIEnv* env, uint8_t *data, int width, int height, const char *text, int font_size, int rotation);


#endif //VIDEOUTIL_BITMAP_UTILS_H
