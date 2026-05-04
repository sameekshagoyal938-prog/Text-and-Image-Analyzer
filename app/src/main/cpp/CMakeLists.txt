cmake_minimum_required(VERSION 3.18.1)

project("contentanalyzer")

# OpenCV_DIR is passed from app/build.gradle.kts.
if(NOT OpenCV_DIR)
    # Default fallback path
    set(OpenCV_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../opencv/native/jni")
endif()

message(STATUS "Using OpenCV_DIR: ${OpenCV_DIR}")

set(OPENCV_INCLUDE_DIR "${OpenCV_DIR}/include")
set(OPENCV_LIBS_DIR "${OpenCV_DIR}/../staticlibs/${ANDROID_ABI}")
set(OPENCV_3RDPARTY_LIBS_DIR "${OpenCV_DIR}/../3rdparty/libs/${ANDROID_ABI}")

set(OPENCV_LIBRARIES
    "${OPENCV_LIBS_DIR}/libopencv_photo.a"
    "${OPENCV_LIBS_DIR}/libopencv_imgcodecs.a"
    "${OPENCV_LIBS_DIR}/libopencv_imgproc.a"
    "${OPENCV_LIBS_DIR}/libopencv_core.a"
    "${OPENCV_3RDPARTY_LIBS_DIR}/liblibjpeg-turbo.a"
    "${OPENCV_3RDPARTY_LIBS_DIR}/liblibpng.a"
    "${OPENCV_3RDPARTY_LIBS_DIR}/liblibwebp.a"
    "${OPENCV_3RDPARTY_LIBS_DIR}/libcpufeatures.a"
    "${OPENCV_3RDPARTY_LIBS_DIR}/libtegra_hal.a"
    "${OPENCV_3RDPARTY_LIBS_DIR}/libittnotify.a"
    "${OPENCV_3RDPARTY_LIBS_DIR}/libtbb.a"
    "z"
)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    list(APPEND OPENCV_LIBRARIES
        "${OPENCV_3RDPARTY_LIBS_DIR}/libkleidicv.a"
        "${OPENCV_3RDPARTY_LIBS_DIR}/libkleidicv_hal.a"
        "${OPENCV_3RDPARTY_LIBS_DIR}/libkleidicv_thread.a"
    )
endif()

# Renamed to match what the system is looking for
add_library(prnu SHARED
        prnu.cpp
)

target_include_directories(prnu PRIVATE
        ${OPENCV_INCLUDE_DIR}
        .
)

target_link_libraries(prnu
        ${OPENCV_LIBRARIES}
        log
        android
        jnigraphics
)

# C++ standard - corrected target name from prnu-lib to prnu
set_target_properties(prnu PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
)