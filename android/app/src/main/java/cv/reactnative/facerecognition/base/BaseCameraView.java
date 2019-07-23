package cv.reactnative.facerecognition.base;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import cv.reactnative.facerecognition.utils.Resources;

public class BaseCameraView extends JavaCameraView {
    private static String TAG = "FaceCameraManage";
    private Camera.Size highResolution;
    private Camera.Size mediumResolution;
    private Camera.Size lowResolution;
    protected int quality = Quality.MEDIUM;
    private Mat rgba, gray;
    protected CameraCallbacks callback;
    private int rotation;
    private boolean clockwise = true;
    protected boolean torchEnabled = false;
    protected boolean tapToFocusEnabled;

    public interface Quality {
        int LOW = 0;
        int MEDIUM = 1;
        int HIGH = 2;
    }

    public BaseCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public BaseCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void getModel(CameraCallbacks callback) {
        this.callback = callback;
    }

    private CvCameraViewListener2 createCvCameraViewListener() {
        return new CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                initResolutions();
                setFlashMode();
                rgba = new Mat();
                gray = new Mat();

                callback.onCameraStarted();
            }

            @Override
            public void onCameraViewStopped() {
                rgba.release();
                gray.release();

                callback.onCameraStopped();
            }

            @Override
            public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
                rgba = inputFrame.rgba();

                applyOrientation(rgba, clockwise , rotation);
                Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGB2GRAY);

                callback.onCameraFrame(rgba, gray);

                return rgba;
            }
        };
    }


    public void loadOpenCV() {
        BaseLoaderCallback loaderCallback = new BaseLoaderCallback(getContext()) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case BaseLoaderCallback.SUCCESS: {
                        Log.i(TAG, "OpenCV loaded successfully");
                        if (getContext() != null) {
                            setCvCameraViewListener(createCvCameraViewListener());
                            enableView();
                        }
                    }
                    break;
                    default: {
                        super.onManagerConnected(status);
                    }
                    break;
                }
            }
        };

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getContext(), loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }


    private void applyOrientation(Mat rgba, boolean clockwise, int rotation) {
        if (rotation == Surface.ROTATION_0) {
            // Rotate clockwise / counter clockwise 90 degrees
            Mat rgbaT = rgba.t();
            Core.flip(rgbaT, rgba, clockwise ? 1 : -1);
            rgbaT.release();
        } else if (rotation == Surface.ROTATION_270) {
            // Rotate clockwise / counter clockwise 180 degrees
            Mat rgbaT = rgba.t();
            Core.flip(rgba.t(), rgba, clockwise ? 1 : -1);
            rgbaT.release();
            Mat rgbaT2 = rgba.t();
            Core.flip(rgba.t(), rgba, clockwise ? 1 : -1);
            rgbaT2.release();
        } else if(this.mCameraIndex == 98) {
            Mat rgbaT = rgba.t();
            Core.flip(rgba.t(), rgba,  1);
            rgbaT.release();
            Mat rgbaT2 = rgba.t();
            Core.flip(rgba.t(), rgba,  -1);
            rgbaT2.release();
        }
    }

    public void initResolutions() {
        List<android.hardware.Camera.Size> resolutionList = mCamera.getParameters().getSupportedPreviewSizes();
        highResolution = mCamera.getParameters().getPreviewSize();
        mediumResolution = highResolution;
        lowResolution = mediumResolution;

        ListIterator<android.hardware.Camera.Size> resolutionItr = resolutionList.listIterator();
        while (resolutionItr.hasNext()) {
            Camera.Size s = resolutionItr.next();
            if (s.width < highResolution.width && s.height < highResolution.height && mediumResolution.equals(highResolution)) {
                mediumResolution = s;
            } else if (s.width < mediumResolution.width && s.height < mediumResolution.height) {
                lowResolution = s;
            }
        }
        if (lowResolution.equals(highResolution)) {
            lowResolution = mediumResolution;
        }
        applyQuality(quality);
    }

    private void applyQuality(int quality) {
        switch (quality) {
            case Quality.LOW:
                setResolution(lowResolution);
                break;
            case Quality.MEDIUM:
                setResolution(mediumResolution);
                break;
            case Quality.HIGH:
                setResolution(highResolution);
                break;
        }
    }

    private void setResolution(Camera.Size resolution) {
        if (resolution == null) return;
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public void setTapToFocus(boolean tapToFocusEnabled) {
        this.tapToFocusEnabled = tapToFocusEnabled;
    }

    public void setTorchMode(boolean enabled) {
        this.torchEnabled = enabled;
        setFlashMode();
    }

    protected void setFlashMode() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters params = mCamera.getParameters();
        List<String> FlashModes = params.getSupportedFlashModes();
        if (torchEnabled) {
            if (FlashModes != null && FlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                Log.e(TAG, "Torch Mode not supported");
            }
        } else {
            if (FlashModes != null && FlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        }
        mCamera.setParameters(params);
    }

    public void setAspect(int aspect) {
        disableView();
        switch (aspect) {
            case CameraSettings.CameraAspectFill:
                this.aspect = CameraAspects.CameraAspectFill;
                break;
            case CameraSettings.CameraAspectFit:
                this.aspect = CameraAspects.CameraAspectFit;
                break;
            case CameraSettings.CameraAspectStretch:
                this.aspect = CameraAspects.CameraAspectStretch;
                break;
        }
        loadOpenCV();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (tapToFocusEnabled && mCamera != null) {
            Camera camera = mCamera;
            camera.cancelAutoFocus();
            android.graphics.Rect focusRect = new android.graphics.Rect(-1000, -1000, 1000, 0);


            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> mylist = new ArrayList<Camera.Area>();
                mylist.add(new Camera.Area(focusRect, 1000));
                parameters.setFocusAreas(mylist);
            }

            try {
                camera.cancelAutoFocus();
                camera.setParameters(parameters);
                camera.startPreview();
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (camera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            Camera.Parameters parameters = camera.getParameters();
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                            if (parameters.getMaxNumFocusAreas() > 0) {
                                parameters.setFocusAreas(null);
                            }
                            camera.setParameters(parameters);
                            camera.startPreview();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "onTouchEvent", e);
            }
        }
        return true;
    }

    public void setCameraView(int camera) {
        switch(camera) {
            case CameraSettings.CameraFront:
                this.mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
                clockwise = false;
                break;
            case CameraSettings.CameraBack:
                this.mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
                break;
        }
    }

    public void setQuality(int captureQuality) {
        switch (captureQuality) {
            case CameraSettings.CameraCaptureSessionPresetLow:
                this.quality = BaseCameraView.Quality.LOW;
                this.setQuality = 0;
                break;
            case CameraSettings.CameraCaptureSessionPresetMedium:
                this.quality = BaseCameraView.Quality.MEDIUM;
                this.setQuality = 1;
                break;
            case CameraSettings.CameraCaptureSessionPresetHigh:
                this.quality = BaseCameraView.Quality.HIGH;
                this.setQuality = 2;
                break;

        }
        applyQuality(quality);
    }

    public void setRotateMode(boolean isLandscape) {
        Context context = getContext();
        if (context == null) return;
        Activity activity = Resources.scanForActivity(context);
        if (activity == null) return;
        activity.setRequestedOrientation(isLandscape
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void disableView() {
        removeCvCameraViewListener();
        super.disableView();

        callback.onCameraPause();
    }



}
