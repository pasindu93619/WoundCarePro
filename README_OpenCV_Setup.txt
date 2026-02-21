OpenCV Android SDK setup for WoundCarePro
=========================================

1) Download the official OpenCV Android SDK archive from:
   https://opencv.org/releases/
   (look for the Android SDK package)

2) From the project root, create the third_party directory if it does not exist:
   third_party/

3) Unzip the downloaded archive so the SDK is located exactly at:
   third_party/opencv-android-sdk/

4) Verify the extracted SDK contains these folders:
   - third_party/opencv-android-sdk/sdk/java
   - third_party/opencv-android-sdk/sdk/native

5) Sync Gradle in Android Studio.

Notes
-----
- The Gradle settings in this project include OpenCV as module :opencv from:
  third_party/opencv-android-sdk/sdk/java
- Native C++ integration expects OpenCV headers and JNI libs from:
  third_party/opencv-android-sdk/sdk/native/jni
