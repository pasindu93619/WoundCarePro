#include <jni.h>
#include <android/log.h>
#include <string>

#if __has_include(<opencv2/core/version.hpp>)
#include <opencv2/core/version.hpp>
#define WOUNDCARE_OPENCV_HEADER_AVAILABLE 1
#else
#define WOUNDCARE_OPENCV_HEADER_AVAILABLE 0
#endif

extern "C" JNIEXPORT jstring JNICALL
Java_com_pasindu_woundcarepro_native_NativeBridge_openCvVersion(JNIEnv* env, jobject /* thiz */) {
#if defined(WOUNDCARE_HAS_OPENCV) && WOUNDCARE_OPENCV_HEADER_AVAILABLE
    const auto version = cv::getVersionString();
    __android_log_print(ANDROID_LOG_INFO, "WoundNative", "OpenCV version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
#else
    const std::string fallback = "OpenCV SDK missing (install third_party/opencv-android-sdk)";
    __android_log_print(ANDROID_LOG_WARN, "WoundNative", "%s", fallback.c_str());
    return env->NewStringUTF(fallback.c_str());
#endif
}
