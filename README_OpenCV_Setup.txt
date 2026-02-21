OpenCV Android SDK setup for WoundCarePro
=========================================

This project expects the OpenCV Android SDK to be placed at:

  third_party/opencv-android-sdk/

Required SDK contents
---------------------
After extraction, these folders must exist:

  third_party/opencv-android-sdk/sdk/java
  third_party/opencv-android-sdk/sdk/native

Download and install
--------------------
1. Download the OpenCV Android SDK zip from the official OpenCV releases page.
2. Unzip the archive.
3. Rename/move the extracted folder to:

     third_party/opencv-android-sdk/

4. Verify these paths exist:

     third_party/opencv-android-sdk/sdk/java/build.gradle
     third_party/opencv-android-sdk/sdk/native/jni/include/opencv2/core.hpp

Notes
-----
- The Gradle settings include ":opencv" from `third_party/opencv-android-sdk/sdk/java`.
- Native CMake config links against `libopencv_java4.so` from the SDK native jni libs.
