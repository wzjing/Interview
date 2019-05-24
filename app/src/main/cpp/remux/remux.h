//
// Created by android1 on 2019/5/24.
//

#ifndef INTERVIEW_REMUX_H
#define INTERVIEW_REMUX_H

extern "C" {
#include <libavformat/avformat.h>
#include <libavutil/timestamp.h>
}

int remux(const char * in_filename, const char * out_filename);

#endif //INTERVIEW_REMUX_H
