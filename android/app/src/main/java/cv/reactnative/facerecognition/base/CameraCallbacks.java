package cv.reactnative.facerecognition.base;

import android.content.Context;

import org.opencv.core.Mat;

public interface CameraCallbacks {
    void onCameraStarted();
    void onCameraStopped();
    void onCameraFrame(Mat rgba, Mat gray);
    void onCameraPause();
}
