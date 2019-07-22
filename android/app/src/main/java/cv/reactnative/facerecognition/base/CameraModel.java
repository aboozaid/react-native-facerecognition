package cv.reactnative.facerecognition.base;


import com.facebook.react.bridge.ReadableMap;

import cv.reactnative.facerecognition.FaceRecognition;

public interface CameraModel {

    void onResume();

    void setTorchMode(boolean enabled);

    void setTapToFocus(boolean tapToFocusEnabled);

    void setCameraView(int camera);

    void setQuality(int quality);

    void setAspect(int aspect);

    void setRotateMode(boolean isLandscape);

    void setDataset(boolean enable);

    void disableView();

    void setConfidence(int confidence);

    void setModelDetection(int model);

    int isDetected();

    boolean isCleared();

    void toTrain(final ReadableMap info);

    void isRecognized();

    void setTrainingCallback(FaceRecognition.onTrained callback);

    void setRecognitionCallback(FaceRecognition.onRecognized callback);

}
