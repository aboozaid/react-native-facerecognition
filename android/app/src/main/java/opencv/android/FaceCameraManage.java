package opencv.android;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.ThemedReactContext;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.Facemark;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.tracking.MultiTracker;
import org.opencv.tracking.TrackerMedianFlow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import static org.opencv.core.Core.FILLED;
import static org.opencv.objdetect.Objdetect.CASCADE_DO_CANNY_PRUNING;
import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

/**
 * Created by Assem Abozaid on 7/12/2018.
 */

public class FaceCameraManage extends JavaCameraView implements SettingsCamera {
    private static String TAG = "FaceCameraManage";
    private CascadeClassifier classifier;
    private MatOfRect faces;
    private MultiTracker tracker;
    private MatOfRect2d trackerPoints;
    private Mat rgba, gray;
    private String modelName;
    private Timer myTimer = new Timer(true);
    private Camera.Size highResolution;
    private Camera.Size mediumResolution;
    private Camera.Size lowResolution;
    private int rotation;
    private int quality = Quality.MEDIUM;
    private boolean torchEnabled = false;
    private boolean tapToFocusEnabled;
    private boolean clockwise = true;
    private ArrayList<Mat> images;
    private ArrayList<String> labels;
    private Mat captured;
    private RecognitionMethods recognition;
    private Tinydb storage;
    private RecognitionMethods.onTrained callback;
    private RecognitionMethods.onRecognized reconitionCallback;
    private int maxConfidence;

    public FaceCameraManage(Context context, int cameraId) {
        super(context, cameraId);
    }
    public FaceCameraManage(Context context, AttributeSet attrs) {
        super(context, attrs);
        storage = new Tinydb(getContext());
    }

    public interface Quality {
        int LOW = 0;
        int MEDIUM = 1;
        int HIGH = 2;
    }

    private CvCameraViewListener2 createCvCameraViewListener() {
        return new CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                initResolutions();
                setFlashMode(torchEnabled);
                faces = new MatOfRect();
                gray = new Mat();
            }

            @Override
            public void onCameraViewStopped() {
                rgba.release();
                gray.release();
            }

            @Override
            public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
                rgba = inputFrame.rgba();

                applyOrientation(rgba, clockwise, rotation);

                Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGB2GRAY);

                if(classifier != null) {
                    if(tracker != null && tracker.update(gray, trackerPoints)) {
                        Rect2d[] facePoints = trackerPoints.toArray();
                        for(int i=0; i<facePoints.length; i++)
                        {
                            Imgproc.rectangle(rgba, facePoints[i].tl(), facePoints[i].br(), new Scalar(255, 255, 255), 1);
                        }
                    } else {
                        myTimer.scheduleAtFixedRate(new trackFace(), 1000, 5*1000);
                    }
                }

                return rgba;
            }
        };
    }
    private AsyncTasks.loadFiles.Callback fileLoaded = new AsyncTasks.loadFiles.Callback() {
        @Override
        public void onFileLoadedComplete(boolean result) {
            File file = new File(getContext().getCacheDir(), modelName);
            classifier = new CascadeClassifier(file.getAbsolutePath());
        }
    };

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
        }
    }
    public void initResolutions() {
        List<Camera.Size> resolutionList = mCamera.getParameters().getSupportedPreviewSizes();
        highResolution = mCamera.getParameters().getPreviewSize();
        mediumResolution = highResolution;
        lowResolution = mediumResolution;

        ListIterator<Camera.Size> resolutionItr = resolutionList.listIterator();
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
    public void setQuality(int captureQuality) {
        switch (captureQuality) {
            case FaceModule.CameraCaptureSessionPreset.CameraCaptureSessionPresetLow:
                this.quality = FaceCameraManage.Quality.LOW;
                this.setQuality = 0;
                break;
            case FaceModule.CameraCaptureSessionPreset.CameraCaptureSessionPresetMedium:
                this.quality = FaceCameraManage.Quality.MEDIUM;
                this.setQuality = 1;
                break;
            case FaceModule.CameraCaptureSessionPreset.CameraCaptureSessionPresetHigh:
                this.quality = FaceCameraManage.Quality.HIGH;
                this.setQuality = 2;
                break;

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

    @Override
    public void setAspect(int aspect) {
        disableView();
        switch (aspect) {
            case FaceModule.CameraAspect.CameraAspectFill:
                this.aspect = JavaCameraView.CameraAspects.CameraAspectFill;
                break;
            case FaceModule.CameraAspect.CameraAspectFit:
                this.aspect = JavaCameraView.CameraAspects.CameraAspectFit;
                break;
            case FaceModule.CameraAspect.CameraAspectStretch:
                this.aspect = JavaCameraView.CameraAspects.CameraAspectStretch;
                break;
        }
        onResume();
    }

    @Override
    public void setConfidence(int confidence) {
        this.maxConfidence = confidence;
    }

    private void setFlashMode(boolean torchEnabled) {
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

    public static Activity scanForActivity(Context viewContext) {
        if (viewContext == null)
            return null;
        else if (viewContext instanceof Activity)
            return (Activity) viewContext;
        else if (viewContext instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) viewContext).getBaseContext());
        else if (viewContext instanceof ThemedReactContext)
            return ((ThemedReactContext) viewContext).getCurrentActivity();
        return null;
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

    @Override
    public void onResume() {
        if (getContext() == null) return;
        BaseLoaderCallback loaderCallback = new BaseLoaderCallback(getContext()) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case BaseLoaderCallback.SUCCESS: {
                        Log.i(TAG, "OpenCV loaded successfully");
                        if (getContext() != null) {
                            setCvCameraViewListener(createCvCameraViewListener());
                            FaceCameraManage.this.enableView();
                            recognition = new RecognitionMethods(maxConfidence);
                            images = storage.getListMat("images");
                            labels = storage.getListString("labels");

                            if(!images.isEmpty()){
                                reTreain();
                            }
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


    @Override
    public void setTorchMode(boolean enabled) {
        this.torchEnabled = enabled;
        setFlashMode(enabled);
    }
    @Override
    public void setTapToFocus(boolean enabled) {
        tapToFocusEnabled = enabled;
    }
    @Override
    public void setModelDetection(int model) {
        switch(model) {
            case FaceModule.Model.DefaultModule:
                this.modelName = "cascade.xml";
                break;
            case FaceModule.Model.LBPCascade:
                this.modelName = "lbp.xml";
        }
        AsyncTasks.loadFiles task = new AsyncTasks.loadFiles(getContext(), modelName, fileLoaded);
        task.execute();
    }
    @Override
    public void setRotateMode(boolean isLandscape) {
        Context context = getContext();
        if (context == null) return;
        Activity activity = scanForActivity(context);
        if (activity == null) return;
        activity.setRequestedOrientation(isLandscape
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    @Override
    public void setCameraView(int camera) {
        switch(camera) {
            case FaceModule.CameraType.CameraFront:
                this.mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
                clockwise = false;
                break;
            case FaceModule.CameraType.CameraBack:
                this.mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
                break;
        }
    }
    @Override
    public void disableView() {
        removeCvCameraViewListener();
        super.disableView();
        if(images != null && !images.isEmpty()) {
            storage.putListMat("images", images);
            storage.putListString("labels", labels);
            images.clear();
            labels.clear();
        }
    }
    private class trackFace extends TimerTask {
        @Override
        public void run() {
            classifier.detectMultiScale(gray, faces, 1.3, 6, CASCADE_DO_CANNY_PRUNING, new Size(30, 30));
            if (!faces.empty()) {
                Rect[] facesArray = faces.toArray();
                Rect2d[] trackerArr = new Rect2d[facesArray.length];
                trackerPoints = new MatOfRect2d();
                ArrayList<Rect2d> points = new ArrayList<>();
                tracker = MultiTracker.create();
                for (int i = 0; i < facesArray.length; i++) {
                    points.add(new Rect2d(facesArray[i].tl(), facesArray[i].br()));
                    tracker.add(TrackerMedianFlow.create(), gray, points.get(i));
                    trackerArr[i] = points.get(i);
                }
                trackerPoints.fromArray(trackerArr);
            }
        }
    }
    @Override
    public int isDetected() {
        captured = ReactMethods.getInstance().cropImage(faces, gray);
        captured = ReactMethods.getInstance().improvements(captured);
        int status = ReactMethods.getInstance().checkDetection(faces, captured);
        return status;
    }

    @Override
    public boolean isCleared() {
        if(storage.isCleared("images") && storage.isCleared("labels"))
        {
            images.clear();
            labels.clear();
            reTreain();
            return true;
        }
        return false;
    }

    @Override
    public void isTrained(final ReadableMap info) {
        if(!info.hasKey("fname")) callback.onFail("face name is incorrect");

        String name = info.getString("fname");

        images.add(captured);
        labels.add(name);

        reTreain();
    }

    @Override
    public void isRecognized() {
        switch(isDetected()) {
            case 0:
                reconitionCallback.onFail("Detection has timed out");
                return;
            case 1:
                reconitionCallback.onFail("Photo is blurred. Snap new one!");
                return;
            case 2:
                reconitionCallback.onFail("Multiple faces detection is not supported!");
                return;
        }

        recognition.isRecognized(captured, new RecognitionMethods.onRecognized() {
            @Override
            public void onComplete(String result) {
                reconitionCallback.onComplete(result);
            }
            @Override
            public void onFail(String err) {
                reconitionCallback.onFail(err);
            }
        });
    }

    @Override
    public void setTrainingCallback(RecognitionMethods.onTrained callback) {
        this.callback = callback;
    }

    @Override
    public void setRecognitionCallback(RecognitionMethods.onRecognized callback) {
        this.reconitionCallback = callback;
    }
    private void reTreain() {
        recognition.isTrained(images, labels, new RecognitionMethods.onTrained() {
            @Override
            public void onComplete() {
                callback.onComplete();
            }

            @Override
            public void onFail(String err) {
                callback.onFail(err);
            }
        });
    }
}
