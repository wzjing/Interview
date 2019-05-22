//
// Created by android1 on 2019/5/22.
//

#ifndef INTERVIEW_JNI_COMMON_H
#define INTERVIEW_JNI_COMMON_H

#define JNI_SIG Java_com_wzjing_interview_
#define JNI_FUNC(sub_clazz, type) extern "C" JNIEXPORT type JNICALL JNI_SIG_ sub_clazz
#define JNI_CLASS(sub_clazz) "com/wzjing/interview/" sub_clazz

#endif //INTERVIEW_JNI_COMMON_H
