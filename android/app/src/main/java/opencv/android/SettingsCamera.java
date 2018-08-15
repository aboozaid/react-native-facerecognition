package opencv.android;

import com.facebook.react.bridge.ReadableMap;

/**
 * Created by Assem Abozaid on 7/31/2018.
 */

public interface SettingsCamera {

    void setTorchMode(boolean enabled);

    void setTapToFocus(boolean enabled);

    void setCameraView(int camera);

    void setModelDetection(int model);

    void setQuality(int quality);

    void setAspect(int aspect);

    void setConfidence(int confidence);

    void setRotateMode(boolean enabled);

    void onResume();

    void disableView();

    int isDetected();

    boolean isCleared();

    void isTrained(final ReadableMap info);

    void isRecognized();

    void setTrainingCallback(RecognitionMethods.onTrained callback);

    void setRecognitionCallback(RecognitionMethods.onRecognized callback);
}
