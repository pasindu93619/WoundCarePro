# OpenCV Android SDK setup

1. Download the OpenCV Android SDK ZIP from the official OpenCV releases page:
   - https://opencv.org/releases/
   - Choose a recent 4.x release and download `opencv-<version>-android-sdk.zip`.

2. Unzip it into this project at:
   - `third_party/opencv-android-sdk/`

   After unzipping, this path must exist:
   - `third_party/opencv-android-sdk/sdk/java/build.gradle`

3. Sync and build:
   - In Android Studio: **Sync Project with Gradle Files**
   - Then run: `./gradlew assembleDebug`

If Gradle cannot find the `:opencv` module, verify that `third_party/opencv-android-sdk/sdk/java/` exists and contains the OpenCV SDK Gradle module files.
