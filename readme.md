## Overview

An introduction to using OpenCV in Android Studio without having the OpenCV Manager installed.
This allows for lightweight apps that do not require the user to download and install yet another app.

![Canny Edge Detection](screenshots/ImageProc1.jpg)

![Adjust Threshold](screenshots/ImageProc2.jpg)

![Document Edge Detection](screenshots/ImageProc3.jpg)

As I am seemingly unable to embed the screenshots into this doc, some part of my screenshot processing
must be the culprit. If interested in the screenshots, please look in the 'screenshots' folder. It 
is time for breakfast...

## Motivation

OpenCV is a complete vision library offering tools to perform a variety of useful image processing needs.
This project was created to explore the use of the OpenCV libraries with Android, and specifically how to
use the library in such a manner as to not require the Opencv Manager app.

## Installation

There are numerous examples outlining the usage of OpenCV with Android Studio. I have used the more recent
method of using a CMake file to build the native code. Here are a few samples:
https://developer.android.com/studio/projects/add-native-code.html
https://github.com/jlhonora/opencv-android-sample
https://stackoverflow.com/questions/38958876/can-opencv-for-android-leverage-the-standard-c-support-to-get-native-build-sup


## Code Snippets

CMake file:

```cmake

    cmake_minimum_required(VERSION 3.6)
    
    # OpenCV stuff
    SET(OpenCV_DIR /opt/OpenCV-android-sdk/sdk/native/jni/)
    find_package(OpenCV REQUIRED)
    message(STATUS "$$$ opencv found: ${OpenCV_LIBS}")
    include_directories(${CMAKE_CURRENT_SOURCE_DIR} ${OpenCV_DIR}/include/)
    
    # Creates and names a library, sets it as either STATIC
    # or SHARED, and provides the relative paths to its source code.
    # You can define multiple libraries, and CMake builds it for you.
    # Gradle automatically packages shared libraries with your APK.
    
    add_library(
            # Sets the name of the library.
            ImageProcessing
    
           # Sets the library as a shared library.
           SHARED
    
           # Provides a relative path to your source file(s).
           # Associated headers in the same location as their source
           # file are automatically included.
           ImageProcessing.cpp )
    
    # Specifies libraries CMake should link to your target library. You
    # can link multiple libraries, such as libraries you define in the
    # build script, prebuilt third-party libraries, or system libraries.
    
    target_link_libraries(
            # Specifies the target library
            ImageProcessing
    
            # OpenCV lib
            opencv_core
    
            # OpenCV lib
            opencv_imgproc )
```

## License

Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied.
See the License for the specific language governing
permissions and limitations under the License.