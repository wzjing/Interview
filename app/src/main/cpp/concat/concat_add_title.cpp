#include "../utils/bitmap_utils.h"
#include "../utils/log.h"
#include "../filters/video_filter.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavutil/timestamp.h>
#include <libavutil/avutil.h>
}

#include "concat_add_title.h"


static int TITLE_DURATION = 2;

#define TAG "Concat"

int encode_title(JNIEnv *env, const char *title, int fontSize, AVFormatContext *formatContext,
                 AVFrame *srcAudioFrame, AVFrame *srcVideoFrame,
                 AVCodecContext *audioCodecContext, AVCodecContext *videoCodecContext,
                 AVStream *audioStream, AVStream *videoStream,
                 int64_t &audio_start_pts, int64_t &audio_start_dts,
                 int64_t &video_start_pts, int64_t &video_start_dts) {

    int encode_video = 1;
    int encode_audio = 1;
    int ret = 0;

    AVFrame *videoFrame = av_frame_alloc();
    AVFrame *audioFrame = av_frame_alloc();
    AVPacket *packet = av_packet_alloc();

    videoFrame->width = videoCodecContext->width;
    videoFrame->height = videoCodecContext->height;
    videoFrame->format = videoCodecContext->pix_fmt;
    ret = av_frame_get_buffer(videoFrame, 0);
    if (ret < 0) {
        LOGW(TAG, "warning: unable to get video buffers");
    }

    int64_t video_frame_pts = 0;
    int64_t audio_frame_pts = 0;

    int64_t next_video_pts = 0;
    int64_t next_video_dts = 0;
    int64_t next_audio_pts = 0;
    int64_t next_audio_dts = 0;

    int sample_size = 0;

    int first_video_set = 0;
    int first_audio_set = 0;

    VideoFilter filter;
    char filter_description[128];
    snprintf(filter_description, 128,
             "gblur=sigma=20:steps=6[blur];[blur]drawtext=fontsize=52:fontcolor=white:text='%s':x=w/2-text_w/2:y=h/2-text_h/2",
             title);
    VideoConfig inConfig((AVPixelFormat) srcVideoFrame->format, srcVideoFrame->width,
                         srcVideoFrame->height);
    VideoConfig rgbaConfig(AV_PIX_FMT_RGBA, videoFrame->width,
                           videoFrame->height);
    VideoConfig outConfig((AVPixelFormat) videoFrame->format, videoFrame->width,
                          videoFrame->height);
    filter.create("gblur=sigma=20:steps=6[blur];[blur]format=pix_fmts=rgba", &inConfig,
                  &rgbaConfig);
    filter.filter(srcVideoFrame, srcVideoFrame);
    if (ret < 0) {
        LOGE(TAG, "unable to filter frame to rgba color\n");
        goto error;
    }
    filter.destroy();

    drawText(env, srcVideoFrame->data[0], srcVideoFrame->width, srcVideoFrame->height, title,
             fontSize);

    filter.create("format=pix_fmts=yuv420p", &rgbaConfig, &outConfig);
    filter.filter(srcVideoFrame, srcVideoFrame);

    ret = filter.filter(srcVideoFrame, srcVideoFrame);
    if (ret < 0) {
        LOGE(TAG, "unable to filter frame to yuv420p color\n");
        goto error;
    }
    filter.destroy();

    sample_size = av_get_bytes_per_sample((AVSampleFormat) srcAudioFrame->format);
    for (int i = 0; i < srcAudioFrame->channels; i++) {
        memset(srcAudioFrame->data[i], '0', srcAudioFrame->nb_samples * sample_size);
    }

    while (encode_video || encode_audio) {
        if (!encode_audio ||
            (encode_video && av_compare_ts(video_frame_pts, videoCodecContext->time_base,
                                           audio_frame_pts, audioCodecContext->time_base) <= 0)) {
            if (av_compare_ts(video_frame_pts, videoCodecContext->time_base,
                              TITLE_DURATION, (AVRational) {1, 1}) > 0) {
                encode_video = 0;
            } else {
                ret = av_frame_copy(videoFrame, srcVideoFrame);
                if (ret < 0) LOGW(TAG, "\tget copy video frame failed\n");
                ret = av_frame_copy_props(videoFrame, srcAudioFrame);
                if (ret < 0) LOGW(TAG, "\tget copy video frame props failed\n");
                if (!first_video_set) {
                    videoFrame->pict_type = AV_PICTURE_TYPE_I;
                    first_video_set = 1;
                } else {
                    videoFrame->pict_type = AV_PICTURE_TYPE_NONE;
                }
                videoFrame->pts = video_frame_pts;
                video_frame_pts += 1;
//                logFrame(videoFrame, "Out", 1);
                avcodec_send_frame(videoCodecContext, videoFrame);
            }
            while (true) {
                ret = avcodec_receive_packet(videoCodecContext, packet);
                if (ret == 0) {
                    av_packet_rescale_ts(packet, videoCodecContext->time_base,
                                         videoStream->time_base);
                    packet->stream_index = videoStream->index;
                    packet->pts += video_start_pts;
                    packet->dts += video_start_dts;
                    next_video_pts = packet->pts + srcVideoFrame->pkt_duration;
                    next_video_dts = packet->dts + srcVideoFrame->pkt_duration;
//                    logPacket(packet, "V");
                    ret = av_interleaved_write_frame(formatContext, packet);
                    if (ret < 0) {
                        LOGE(TAG, "write video frame error: %s\n", av_err2str(ret));
                        goto error;
                    }
                } else if (ret == AVERROR(EAGAIN)) {
                    break;
                } else if (ret == AVERROR_EOF) {
                    LOGW(TAG, "Stream Video finished\n");
                    encode_video = 0;
                    break;
                } else {
                    LOGE(TAG, "encode video frame error: %s\n", av_err2str(ret));
                    goto error;
                }
            }
        } else {
            if (av_compare_ts(audio_frame_pts, audioCodecContext->time_base,
                              TITLE_DURATION, (AVRational) {1, 1}) >= 0) {
                encode_audio = 0;
            } else {
                audioFrame->format = audioCodecContext->sample_fmt;
                audioFrame->nb_samples = srcAudioFrame->nb_samples;
                audioFrame->sample_rate = audioCodecContext->sample_rate;
                audioFrame->channel_layout = audioCodecContext->channel_layout;
                ret = av_frame_get_buffer(audioFrame, 0);
                if (ret < 0) LOGW(TAG, "\tget audio buffer failed\n");
                ret = av_frame_copy(audioFrame, srcAudioFrame);
                if (ret < 0) LOGW(TAG, "\tcopy audio failed\n");
                av_frame_copy_props(audioFrame, srcAudioFrame);
                if (ret < 0) LOGW(TAG, "\tcopy audio prop failed\n");
                audioFrame->pict_type = AV_PICTURE_TYPE_NONE;
                audioFrame->pts = audio_frame_pts;
                audio_frame_pts += srcAudioFrame->nb_samples;
//                logFrame(audioFrame, "Out", 0);
                avcodec_send_frame(audioCodecContext, audioFrame);
            }
            while (true) {
                ret = avcodec_receive_packet(audioCodecContext, packet);
                if (ret == 0) {
//                    logPacket(packet, "A");
                    if (packet->pts == 0) {
//                        first_audio = packet->pts;
                        first_audio_set = 1;
                    }
//                    if (packet->pts < 0) continue;
                    if (!first_audio_set) continue;
                    av_packet_rescale_ts(packet, audioCodecContext->time_base,
                                         audioStream->time_base);
                    packet->stream_index = audioStream->index;
                    packet->pts += audio_start_pts;
                    packet->dts += audio_start_dts;
//                    if (packet->pts < first_audio) continue;
//                    if (packet->pts <= (next_audio_pts - srcAudioFrame->pkt_duration)) continue;
                    next_audio_pts = packet->pts + srcAudioFrame->pkt_duration;
                    next_audio_dts = packet->dts + srcAudioFrame->pkt_duration;
//                    logPacket(packet, "A");
                    ret = av_interleaved_write_frame(formatContext, packet);
                    if (ret < 0) {
                        LOGE(TAG, "write audio frame error: %s\n", av_err2str(ret));
                        goto error;
                    }
                    break;
                } else if (ret == AVERROR(EAGAIN)) {
                    break;
                } else if (ret == AVERROR_EOF) {
                    LOGW(TAG, "Stream Audio finished\n");
                    encode_audio = 0;
                    break;
                } else {
                    LOGE(TAG, "encode audio frame error: %s\n", av_err2str(ret));
                    goto error;
                }
            }
        }
    }

    video_start_pts = next_video_pts;
    video_start_dts = next_video_dts;
    audio_start_pts = next_audio_pts;
    audio_start_dts = next_audio_dts;

    av_frame_free(&audioFrame);
    av_frame_free(&videoFrame);
    av_packet_free(&packet);
    return 0;
    error:
    av_frame_free(&audioFrame);
    av_frame_free(&videoFrame);
    av_packet_free(&packet);
    return -1;
}

int concat_add_title(JNIEnv *env, const char *output_filename,
                     const char **input_filenames, const char **titles, size_t nb_inputs,
                     int font_size,
                     int title_duration) {
    TITLE_DURATION = title_duration;
    int ret = 0;
    // input fragments
    auto **videos = (Video **) malloc(nb_inputs * sizeof(Video *));

    LOGD(TAG, "d1");

    for (int i = 0; i < nb_inputs; ++i) {
        videos[i] = (Video *) malloc(sizeof(Video));
    }
    LOGD(TAG, "d2");

    // output video
    AVFormatContext *outFmtContext = nullptr;
    AVStream *outVideoStream = nullptr;
    AVStream *outAudioStream = nullptr;
    AVCodecContext *outVideoContext = nullptr;
    AVCodecContext *outAudioContext = nullptr;
    AVCodec *outVideoCodec = nullptr;
    AVCodec *outAudioCodec = nullptr;

    for (int i = 0; i < nb_inputs; ++i) {
        LOGD(TAG, "loop: %s\n", input_filenames[i]);
        ret = avformat_open_input(&videos[i]->formatContext, input_filenames[i], nullptr, nullptr);
        if (ret < 0) {
            LOGE(TAG, "input format error: %s\n", av_err2str(ret));
            return -1;
        }
        LOGD(TAG, "loop2: %s\n", input_filenames[i]);
        ret = avformat_find_stream_info(videos[i]->formatContext, nullptr);
        if (ret < 0) {
            LOGE(TAG, "input format info error: %s\n", av_err2str(ret));
            return -1;
        }
        LOGD(TAG, "loop3: %s\n", input_filenames[i]);
        for (int j = 0; j < videos[i]->formatContext->nb_streams; ++j) {
            if (videos[i]->formatContext->streams[j]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                videos[i]->videoStream = videos[i]->formatContext->streams[j];
                AVCodec *codec = avcodec_find_decoder(videos[i]->videoStream->codecpar->codec_id);
                videos[i]->videoCodecContext = avcodec_alloc_context3(codec);
                avcodec_parameters_to_context(videos[i]->videoCodecContext,
                                              videos[i]->videoStream->codecpar);
                avcodec_open2(videos[i]->videoCodecContext, codec, nullptr);
            } else if (videos[i]->formatContext->streams[j]->codecpar->codec_type ==
                       AVMEDIA_TYPE_AUDIO) {
                videos[i]->audioStream = videos[i]->formatContext->streams[j];
                AVCodec *codec = avcodec_find_decoder(videos[i]->audioStream->codecpar->codec_id);
                videos[i]->audioCodecContext = avcodec_alloc_context3(codec);
                avcodec_parameters_to_context(videos[i]->audioCodecContext,
                                              videos[i]->audioStream->codecpar);
                avcodec_open2(videos[i]->audioCodecContext, codec, nullptr);
            }
            if (videos[i]->videoStream && videos[i]->audioStream) break;
        }

        LOGD(TAG, "loop4: %s\n", input_filenames[i]);
        videos[i]->isTsVideo = strcmp(videos[i]->formatContext->iformat->name, "mpegts") == 0;

        LOGD(TAG, "\n"
                  "%s:\t%s/%s -> %s\n\n",
             videos[i]->isTsVideo ? "TS" : "--",
             videos[i]->videoStream ? "Video" : "--",
             videos[i]->audioStream ? "Audio" : "--",
             input_filenames[i]);
    }

    LOGD(TAG, "debug1");

    // create output AVFormatContext
    ret = avformat_alloc_output_context2(&outFmtContext, nullptr, "mpegts", output_filename);
    if (ret < 0) {
        LOGE(TAG, "output format error: %s\n", av_err2str(ret));
        return -1;
    }
    LOGD(TAG, "debug2");

    // Copy codec from input video AVStream
    outVideoCodec = avcodec_find_encoder(videos[0]->videoStream->codecpar->codec_id);
    outAudioCodec = avcodec_find_encoder(videos[0]->audioStream->codecpar->codec_id);
    LOGD(TAG, "debug3");

    // create output Video AVStream
    outVideoStream = avformat_new_stream(outFmtContext, outVideoCodec);
    if (ret < 0) {
        LOGE(TAG, "create video stream error: %s\n", av_err2str(ret));
        return -1;
    }
    outVideoStream->id = 0;
    LOGD(TAG, "debug4");

    outAudioStream = avformat_new_stream(outFmtContext, outAudioCodec);
    if (ret < 0) {
        LOGE(TAG, "create audio stream error: %s\n", av_err2str(ret));
        return -1;
    }
    outAudioStream->id = 1;
    LOGD(TAG, "debug5");

    Video *baseVideo = videos[0]; // the result code is based on this video

    // Copy Video Stream Configure from base Video
    outVideoContext = avcodec_alloc_context3(outVideoCodec);
    outVideoContext->codec_id = baseVideo->videoCodecContext->codec_id;
    outVideoContext->width = baseVideo->videoCodecContext->width;
    outVideoContext->height = baseVideo->videoCodecContext->height;
    outVideoContext->pix_fmt = baseVideo->videoCodecContext->pix_fmt;
    outVideoContext->bit_rate = baseVideo->videoCodecContext->bit_rate;
    outVideoContext->has_b_frames = baseVideo->videoCodecContext->has_b_frames;
    outVideoContext->gop_size = baseVideo->videoCodecContext->gop_size;
    outVideoContext->qmin = baseVideo->videoCodecContext->qmin;
    outVideoContext->qmax = baseVideo->videoCodecContext->qmax;
    outVideoContext->time_base = (AVRational) {baseVideo->videoStream->r_frame_rate.den,
                                               baseVideo->videoStream->r_frame_rate.num};
    outVideoContext->profile = baseVideo->videoCodecContext->profile;
    outVideoStream->time_base = outVideoContext->time_base;
    AVDictionary *opt = nullptr;
    if (outVideoContext->codec_id == AV_CODEC_ID_H264) {
        av_dict_set(&opt, "preset", "fast", 0);
        av_dict_set(&opt, "tune", "zerolatency", 0);
    }
    ret = avcodec_open2(outVideoContext, outVideoCodec, &opt);
    if (ret < 0) {
        LOGE(TAG, "open output video AVCodecContext error: %s\n", av_err2str(ret));
        return -1;
    }
    ret = avcodec_parameters_from_context(outVideoStream->codecpar, outVideoContext);
    if (ret < 0) {
        LOGE(TAG, "copy output video parameter error: %s\n", av_err2str(ret));
        return -1;
    }
    LOGD(TAG, "debug6");

    // Copy Audio Stream Configure from base Video
    outAudioContext = avcodec_alloc_context3(outAudioCodec);
    outAudioContext->codec_type = baseVideo->audioCodecContext->codec_type;
    outAudioContext->codec_id = baseVideo->audioCodecContext->codec_id;
    outAudioContext->sample_fmt = baseVideo->audioCodecContext->sample_fmt;
    outAudioContext->sample_rate = baseVideo->audioCodecContext->sample_rate;
    outAudioContext->bit_rate = baseVideo->audioCodecContext->bit_rate;
    outAudioContext->channel_layout = baseVideo->audioCodecContext->channel_layout;
    outAudioContext->channels = baseVideo->audioCodecContext->channels;
    outAudioContext->flags |= AV_CODEC_FLAG_LOW_DELAY;
    outAudioContext->time_base = (AVRational) {1, outAudioContext->sample_rate};
    outAudioStream->time_base = outAudioContext->time_base;
    av_dict_free(&opt);
    opt = nullptr;
//    if (outVideoContext->codec_id == AV_CODEC_ID_AAC) {
//        av_dict_set(&opt, "profile", "23", 0);
//    }
    ret = avcodec_open2(outAudioContext, outAudioCodec, &opt);
    if (ret < 0) {
        LOGE(TAG, "open output audio AVCodecContext error: %s\n", av_err2str(ret));
        return -1;
    }
    ret = avcodec_parameters_from_context(outAudioStream->codecpar, outAudioContext);
    if (ret < 0) {
        LOGE(TAG, "copy output audio parameter error: %s\n", av_err2str(ret));
        return -1;
    }

    if (!(outFmtContext->oformat->flags & AVFMT_NOFILE)) {
        LOGD(TAG, "Opening file: %s\n", output_filename);
        ret = avio_open(&outFmtContext->pb, output_filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE(TAG, "could not open %s (%s)\n", output_filename, av_err2str(ret));
            return -1;
        }
    }
    LOGD(TAG, "debug7");

    ret = avformat_write_header(outFmtContext, nullptr);
    if (ret < 0) {
        LOGE(TAG, "write header error: %s\n", av_err2str(ret));
        return -1;
    }

    LOGD(TAG, "debug8");

    av_dump_format(outFmtContext, 0, output_filename, 1);
    LOGD(TAG, "debug9");

    AVPacket *packet = av_packet_alloc();
    AVFrame *videoFrame = av_frame_alloc();
    AVFrame *audioFrame = av_frame_alloc();

    int64_t last_video_pts = 0;
    int64_t last_video_dts = 0;
    int64_t last_audio_pts = 0;
    int64_t last_audio_dts = 0;
    LOGD(TAG, "debug10");


    for (int i = 0; i < nb_inputs; ++i) {
        AVFormatContext *inFormatContext = videos[i]->formatContext;

        AVStream *inVideoStream = videos[i]->videoStream;
        AVStream *inAudioStream = videos[i]->audioStream;
        AVCodecContext *audioContext = videos[i]->audioCodecContext;
        AVCodecContext *videoContext = videos[i]->videoCodecContext;
        AVBSFContext *bsfContext = nullptr;

        if (!videos[i]->isTsVideo) {
            // bitstream filter: convert mp4 packet to annexb
            const AVBitStreamFilter *bsfFilter = av_bsf_get_by_name("h264_mp4toannexb");

            if (!bsfFilter) {
                LOGE(TAG, "unable to find bsf tiler\n");
                return -1;
            }
            ret = av_bsf_alloc(bsfFilter, &bsfContext);
            if (ret < 0) {
                LOGE(TAG, "unable to create bsf context");
                return -1;
            }
            ret = avcodec_parameters_from_context(bsfContext->par_in, videoContext);
            if (ret < 0) {
                LOGE(TAG, "unable to copy in parameters to bsf context");
                return -1;
            }
            bsfContext->time_base_in = inVideoStream->time_base;
            ret = av_bsf_init(bsfContext);
            if (ret < 0) {
                LOGE(TAG, "unable to init bsf context");
                return -1;
            }

            if (ret < 0) {
                LOGE(TAG, "unable to copy out parameters to bsf context");
                return -1;
            }
            LOGD(TAG, "bsf timebase: {%d, %d} -> {%d, %d}\n", inVideoStream->time_base.num,
                 inVideoStream->time_base.den,
                 bsfContext->time_base_out.num, bsfContext->time_base_out.den);
        }

        int64_t first_video_pts = 0;
        int64_t first_video_dts = 0;
        int64_t first_audio_pts = 0;
        int64_t first_audio_dts = 0;
        int video_ts_set = 0;
        int audio_ts_set = 0;

        int64_t next_video_pts = 0;
        int64_t next_video_dts = 0;
        int64_t next_audio_pts = 0;
        int64_t next_audio_dts = 0;


        // use first frame to make a title
        int got_video = 0;
        int got_audio = 0;
        do {
            ret = av_read_frame(inFormatContext, packet);
            if (ret < 0) break;
            if (!got_video && packet->stream_index == inVideoStream->index) {
                ret = avcodec_send_packet(videoContext, packet);
                if (ret < 0) continue;
                ret = avcodec_receive_frame(videoContext, videoFrame);
                if (ret < 0) continue;
                else got_video = 1;
            } else if (!got_audio && packet->stream_index == inAudioStream->index) {
                ret = avcodec_send_packet(audioContext, packet);
                if (ret < 0) continue;
                ret = avcodec_receive_frame(audioContext, audioFrame);
                if (ret < 0) continue;
                else got_audio = 1;
            }
        } while (!got_video || !got_audio);

        if (!got_video) {
            LOGW(TAG, "unable to get input video frame\n");
            videoFrame->width = outVideoContext->width;
            videoFrame->height = outVideoContext->height;
            videoFrame->format = outVideoContext->pix_fmt;
            av_frame_get_buffer(videoFrame, 0);
        }

        if (!got_audio) {
            LOGW(TAG, "unable to get input audio frame\n");
            audioFrame->nb_samples = 1024;
            audioFrame->sample_rate = outAudioContext->sample_rate;
        }

        logFrame(videoFrame, "First", 1);
        logFrame(audioFrame, "First", 1);

        encode_title(env, titles[i], font_size, outFmtContext, audioFrame, videoFrame,
                     outAudioContext,
                     outVideoContext,
                     outAudioStream, outVideoStream, last_audio_pts, last_audio_dts, last_video_pts,
                     last_video_dts);


        av_seek_frame(inFormatContext, inAudioStream->index, 0, 0);
        av_seek_frame(inFormatContext, inVideoStream->index, 0, 0);

        LOGD(TAG, "\nlast timestamp: A(%lld/%lld) V(%lld/%lld)\n\n", last_audio_pts, last_audio_dts,
             last_video_pts,
             last_video_dts);

        // copy the video file
        do {
            ret = av_read_frame(inFormatContext, packet);
            if (ret == AVERROR_EOF) {
                LOGW(TAG, "\tread fragment end of file\n");
                break;
            } else if (ret < 0) {
                LOGE(TAG, "read fragment error: %s\n", av_err2str(ret));
                break;
            }

            if (packet->dts < 0) continue;
            if (packet->stream_index == inVideoStream->index) {
                if (packet->flags & AV_PKT_FLAG_DISCARD) {
                    LOGW(TAG, "\nPacket is discard\n");
                    continue;
                }

                if (!videos[i]->isTsVideo) {
                    AVPacket *annexPacket = av_packet_alloc();
                    ret = av_bsf_send_packet(bsfContext, packet);
                    if (ret < 0)
                        LOGW(TAG, "unable to convert packet to annexb: %s\n", av_err2str(ret));
                    ret = av_bsf_receive_packet(bsfContext, annexPacket);
                    if (ret != 0)
                        LOGW(TAG, "unable to receive converted annexb packet: %s\n",
                             av_err2str(ret));
//                    LOGI(TAG, "\t mp4 to annexb\n");
                    if (!video_ts_set) {
                        first_video_pts = annexPacket->pts;
                        first_video_dts = annexPacket->dts;
                        video_ts_set = 1;
                    }
                    annexPacket->stream_index = outVideoStream->index;

                    annexPacket->pts -= first_video_pts;
                    annexPacket->dts -= first_video_dts;
                    av_packet_rescale_ts(annexPacket, bsfContext->time_base_out,
                                         outVideoStream->time_base);
                    annexPacket->pts += last_video_pts;
                    annexPacket->dts += last_video_dts;
                    next_video_pts = annexPacket->pts + annexPacket->duration;
                    next_video_dts = annexPacket->dts + annexPacket->duration;

//                    logPacket(annexPacket, "V");
                    av_interleaved_write_frame(outFmtContext, annexPacket);
                    av_packet_free(&annexPacket);
                } else {
                    if (!video_ts_set) {
                        first_video_pts = packet->pts;
                        first_video_dts = packet->dts;
                        video_ts_set = 1;
                    }

                    packet->stream_index = outVideoStream->index;

                    packet->pts -= first_video_pts;
                    packet->dts -= first_video_dts;
                    av_packet_rescale_ts(packet, inVideoStream->time_base,
                                         outVideoStream->time_base);
                    packet->pts += last_video_pts;
                    packet->dts += last_video_dts;
                    next_video_pts = packet->pts + packet->duration;
                    next_video_dts = packet->dts + packet->duration;

//                    logPacket(packet, "V");

                    av_interleaved_write_frame(outFmtContext, packet);
                }

            } else if (packet->stream_index == inAudioStream->index) {

                packet->stream_index = outAudioStream->index;

                if (!audio_ts_set) {
                    first_audio_pts = packet->pts;
                    first_audio_dts = packet->dts;
                    audio_ts_set = 1;
                }
                packet->pts -= first_audio_pts;
                packet->dts -= first_audio_dts;
                av_packet_rescale_ts(packet, inAudioStream->time_base, outAudioStream->time_base);
                packet->pts += last_audio_pts;
                packet->dts += last_audio_dts;
                next_audio_pts = packet->pts + packet->duration;
                next_audio_dts = packet->dts + packet->duration;
//                logPacket(packet, "A");
                av_interleaved_write_frame(outFmtContext, packet);
            }
        } while (true);
        last_video_pts = next_video_pts;
        last_video_dts = next_video_dts;
        last_audio_pts = next_audio_pts;
        last_audio_dts = next_audio_dts;

        if (!videos[i]->isTsVideo) av_bsf_free(&bsfContext);

        LOGD(TAG,
             "--------------------------------------------------------------------------------\n");
    }


    av_write_trailer(outFmtContext);


    if (!(outFmtContext->oformat->flags & AVFMT_NOFILE)) {
        avio_closep(&outFmtContext->pb);
    }


    av_packet_free(&packet);
    avformat_free_context(outFmtContext);

    for (int i = 0; i < nb_inputs; ++i) {
        avformat_free_context(videos[i]->formatContext);
        if (videos[i]->videoCodecContext)
            avcodec_free_context(&videos[i]->videoCodecContext);
        if (videos[i]->audioCodecContext)
            avcodec_free_context(&videos[i]->audioCodecContext);
        free(videos[i]);
    }
    free(videos);

    return 0;
}
