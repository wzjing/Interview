
add_library(media SHARED muxer.h muxer.cpp mux_listener.h mux_listener.cpp filter.h filter.cpp)

find_library(log-lib log)

include_directories(${PROJECT_SOURCE_DIR})

target_link_libraries(media
        utils
        ${log-lib}
        avcodec
        avformat
        avfilter
        avutil
        postproc
        swresample
        swscale
        x264)