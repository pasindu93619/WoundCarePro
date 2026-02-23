cmake_minimum_required(VERSION 3.22.1)

project(wound_native)

add_library(wound_native SHARED native-lib.cpp)

find_library(log-lib log)

# ✅ Correct OpenCV root for your folder structure
set(OpenCV_SDK_DIR "${CMAKE_SOURCE_DIR}/../../../../third_party/opencv-android-sdk/OpenCV-android-sdk/sdk")
set(OpenCV_INCLUDE_DIR "${OpenCV_SDK_DIR}/native/jni/include")
set(OpenCV_LIB_DIR "${OpenCV_SDK_DIR}/native/libs/${ANDROID_ABI}")

include_directories(${OpenCV_INCLUDE_DIR})

add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES
        IMPORTED_LOCATION "${OpenCV_LIB_DIR}/libopencv_java4.so"
)

target_link_libraries(
        wound_native
        opencv_java4
        ${log-lib}
)