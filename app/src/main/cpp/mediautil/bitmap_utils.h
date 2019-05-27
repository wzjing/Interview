//
// Created by android1 on 2019/5/22.
//

#ifndef INTERVIEW_BITMAP_UTILS_H
#define INTERVIEW_BITMAP_UTILS_H

#include <android/bitmap.h>
#include <jni.h>

void drawText(JNIEnv* env, uint8_t *data, int width, int height, const char *text, int font_size, int rotation);


#endif //INTERVIEW_BITMAP_UTILS_H
