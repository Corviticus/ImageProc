## Overview

An itroduction to using OpenCV in Android Studio without having the OpenCV Manager installed.
This allows for lightweight aps that do not require the user to download and install yet another app.

![alt text](https://github.com/corviticus/image_proc_ndk/blob/master/screenshots/ImageProc1.png "Simple edge detection using Canny")

![alt text](https://github.com/corviticus/image_proc_ndk/blob/master/screenshots/ImageProc2.png "Adjusting the slider changes the Canny threshold")

![alt text](https://github.com/corviticus/image_proc_ndk/blob/master/screenshots/ImageProc3.png "Edge detection of objects is possible")

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