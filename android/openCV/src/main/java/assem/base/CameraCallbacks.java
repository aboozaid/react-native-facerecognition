package assem.base;

import android.content.Context;

import org.opencv.core.Mat;

public interface CameraCallbacks {
    void onCameraStarted();
    void onCameraStopped();
    void onCameraFrame(Mat rgba, Mat gray);
    void onCameraResume();
    void onCameraPause();
    void onCameraRotate(boolean isLandscape, Context context);
}
