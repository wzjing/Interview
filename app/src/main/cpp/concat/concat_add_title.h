
#ifndef VIDEOBOX_CONCAT_ADD_TITLE_H
#define VIDEOBOX_CONCAT_ADD_TITLE_H

struct Video {
    AVFormatContext *formatContext = nullptr;
    AVStream *videoStream = nullptr;
    AVStream *audioStream = nullptr;
    AVCodecContext *audioCodecContext = nullptr;
    AVCodecContext *videoCodecContext = nullptr;
    int isTsVideo = 0;

};

int concat_add_title(JNIEnv *env, const char *output_filename,
                     const char **input_filenames, const char **titles, size_t nb_inputs, int font_size,
                     int title_duration);

#endif //VIDEOBOX_CONCAT_ADD_TITLE_H
