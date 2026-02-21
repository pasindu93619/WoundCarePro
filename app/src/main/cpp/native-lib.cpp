#include <jni.h>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_pasindu_woundcarepro_nativebridge_NativeBridge_openCvVersion(JNIEnv* env, jobject /* thiz */) {
    const std::string version = cv::getVersionString();
    __android_log_print(ANDROID_LOG_INFO, "WoundNative", "OpenCV version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}
