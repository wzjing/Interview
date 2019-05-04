cmake_minimum_required(VERSION 3.10)

add_definitions(-DDEBUG)

set(SOURCES native-lib.cpp
        encoder/x264_encode.h
        encoder/x264_encode.cpp
        encoder/encoder.h
        encoder/encoder.cpp
        utils/io.h
        utils/io.cpp
        utils/log.h)

add_library(native-lib SHARED ${SOURCES})

find_library(log-lib log)

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -fuse-ld=lld -DCMAKE_SYSROOT=C:/opt/android_sdk/ndk/18.1.5063045/toolchains/llvm/prebuilt/windows-x86_64/sysroot")

function(import_library name type filename)
    add_library(${name} ${type} IMPORTED)
    set_target_properties(${name} PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/${filename})
endfunction()

import_library(avcodec SHARED libavcodec.so)
import_library(avformat SHARED libavformat.so)
import_library(avfilter SHARED libavfilter.so)
import_library(avutil SHARED libavutil.so)
import_library(swresample SHARED libswresample.so)
import_library(swscale SHARED libswscale.so)
import_library(x264 SHARED libx264.so)

include_directories("${CMAKE_SOURCE_DIR}/include")

target_link_libraries(native-lib
        ${log-lib}
        avcodec
        avformat
        avfilter
        avutil
        swresample
        swscale
        x264)