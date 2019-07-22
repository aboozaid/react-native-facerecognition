package cv.reactnative.facerecognition.base;


import com.facebook.react.bridge.ReadableMap;

import cv.reactnative.facerecognition.RecognitionMethods;

public interface CameraModel {

    void onResume();

    void setTorchMode(boolean enabled);

    void setTapToFocus(boolean tapToFocusEnabled);

    void setCameraView(int camera);

    void setQuality(int quality);

    void setAspect(int aspect);

    void setRotateMode(boolean isLandscape);

    void disableView();

    void setConfidence(int confidence);

    void setModelDetection(int model);

    int isDetected();

    boolean isCleared();

    void toTrain(final ReadableMap info);

    void isRecognized();

    void setTrainingCallback(RecognitionMethods.onTrained callback);

    void setRecognitionCallback(RecognitionMethods.onRecognized callback);

}
