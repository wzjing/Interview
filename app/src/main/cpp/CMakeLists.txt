cmake_minimum_required(VERSION 3.10)

add_definitions(-DDEBUG)

add_library(native-lib SHARED
        bgm/mix_bgm.h
        bgm/mix_bgm.cpp
        concat/concat_add_title.h
        concat/concat_add_title.cpp
        filters/audio_filter.h
        filters/audio_filter.cpp
        filters/video_filter.h
        filters/video_filter.cpp
        native-lib.cpp
        utils/log.h
        utils/log.cpp
        utils/error.h
        utils/bitmap_utils.h
        utils/bitmap_utils.cpp)

function(import_library name type filename)
    add_library(${name} ${type} IMPORTED)
    set_target_properties(${name} PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/thirdpart/lib/${CMAKE_ANDROID_ARCH_ABI}/${filename})
endfunction()

import_library(avcodec SHARED libavcodec.so)
import_library(avformat SHARED libavformat.so)
import_library(avfilter SHARED libavfilter.so)
import_library(avutil SHARED libavutil.so)
import_library(swresample SHARED libswresample.so)
import_library(swscale SHARED libswscale.so)
import_library(postproc SHARED libpostproc.so)
import_library(x264 SHARED libx264.so)

include_directories("${CMAKE_SOURCE_DIR}/thirdpart/include")

find_library(log-lib log)

target_link_libraries(native-lib
        ${log-lib}
        avcodec
        avformat
        avfilter
        avutil
        postproc
        swresample
        swscale
        x264
        jnigraphics
        android)