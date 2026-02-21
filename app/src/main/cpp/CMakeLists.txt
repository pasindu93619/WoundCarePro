cmake_minimum_required(VERSION 3.22.1)

project(wound_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../../third_party/opencv-android-sdk/sdk/native/jni")
if(EXISTS "${OpenCV_DIR}/OpenCVConfig.cmake")
    find_package(OpenCV REQUIRED CONFIG)
    add_compile_definitions(WOUNDCARE_HAS_OPENCV=1)
else()
    message(WARNING "OpenCV Android SDK not found at ${OpenCV_DIR}. Build will use fallback JNI response.")
endif()

add_library(
    wound_native
    SHARED
    native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(
    wound_native
    ${log-lib}
)

if(OpenCV_FOUND)
    target_link_libraries(
        wound_native
        ${OpenCV_LIBS}
    )
endif()
