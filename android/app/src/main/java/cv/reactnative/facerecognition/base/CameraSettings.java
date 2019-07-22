package cv.reactnative.facerecognition.base;

public interface CameraSettings {
    // aspects
    int CameraAspectFill = 0;
    int CameraAspectFit = 1;
    int CameraAspectStretch = 2;

    // quality
    int CameraCaptureSessionPresetLow = 3;
    int CameraCaptureSessionPresetMedium = 4;
    int CameraCaptureSessionPresetHigh = 5;

    // flash
    int CameraTorchModeOff = 6;
    int CameraTorchModeOn = 7;

    // camera type
    int CameraFront = 8;
    int CameraBack = 9;

    // rotate mode
    int CameraRotateModeOff = 10;
    int CameraRotateModeOn = 11;

}
