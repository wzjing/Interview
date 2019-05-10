cmake_minimum_required(VERSION 3.10)

add_definitions(-DDEBUG)

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
import_library(x264 SHARED libx264.so)

include_directories("${CMAKE_SOURCE_DIR}/thirdpart/include")

add_subdirectory(utils)
#find_library(utils utils-lib)
add_subdirectory(media)