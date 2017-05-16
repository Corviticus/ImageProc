//
// Created by bobcat on 5/15/17.
//

#ifndef IMAGEPROC_IMAGEPROCESSING_H
#define IMAGEPROC_IMAGEPROCESSING_H

#include <opencv2/core/types_c.h>
#include <jni.h>

extern "C" jboolean Java_com_software_corvidae_imageproc_CameraActivity_ImageProcessing (
        JNIEnv* env,
        const jobject thiz,
        const jint width,
        const jint height,
        const int lowThreshold,
        const jbyteArray cameraData,
        jintArray outPixels);

#endif //IMAGEPROC_IMAGEPROCESSING_H
