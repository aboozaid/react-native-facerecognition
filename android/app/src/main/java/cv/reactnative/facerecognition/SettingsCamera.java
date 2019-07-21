package cv.reactnative.facerecognition;

import com.facebook.react.bridge.ReadableMap;

/**
 * Created by Assem Abozaid on 7/31/2018.
 */

public interface SettingsCamera {



    int isDetected();

    boolean isCleared();

    void isTrained(final ReadableMap info);

    void isRecognized();
}
