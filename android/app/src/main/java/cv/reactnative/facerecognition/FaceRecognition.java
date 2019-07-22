package cv.reactnative.facerecognition;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.AttributeSet;

import com.facebook.react.bridge.ReadableMap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.tracking.MultiTracker;
import org.opencv.tracking.TrackerMedianFlow;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import cv.reactnative.facerecognition.base.BaseCameraView;
import cv.reactnative.facerecognition.base.CameraCallbacks;
import cv.reactnative.facerecognition.base.CameraModel;
import cv.reactnative.facerecognition.recognizer.LBPHFRecognizer;
import cv.reactnative.facerecognition.utils.Resources;

import static org.opencv.objdetect.Objdetect.CASCADE_DO_CANNY_PRUNING;

public class FaceRecognition extends BaseCameraView implements CameraModel {


    private Mat captured;
    private LBPHFRecognizer recognizer;
    protected int confidence;
    private String faceModel;
    private Tinydb storage;
    private MatOfRect faces;
    private CascadeClassifier classifier;
    private MultiTracker tracker;
    private MatOfRect2d trackerPoints;
    private Timer myTimer = new Timer(true);
    private RecognitionMethods.onTrained callback;
    private RecognitionMethods.onRecognized reconitionCallback;
    private Mat gray;


    public FaceRecognition(Context context, AttributeSet attrs) {
        super(context, attrs);
        storage = new Tinydb(context);
        getModel(camera);
    }

    private AsyncTasks.loadFiles.Callback fileLoaded = new AsyncTasks.loadFiles.Callback() {
        @Override
        public void onFileLoadedComplete(boolean result) {
            File file = new File(getContext().getCacheDir(), faceModel);
            classifier = new CascadeClassifier(file.getAbsolutePath());
        }
    };

    private CameraCallbacks camera = new CameraCallbacks() {
        @Override
        public void onCameraStarted() {
            faces = new MatOfRect();
        }

        @Override
        public void onCameraStopped() {

        }

        @Override
        public void onCameraFrame(Mat rgba, Mat gray) {

            FaceRecognition.this.gray = gray;
            if(classifier != null) {
                if(tracker != null && tracker.update(gray, trackerPoints)) {
                    Rect2d[] facePoints = trackerPoints.toArray();
                    for(int i=0; i<facePoints.length; i++)
                    {
                        Imgproc.rectangle(rgba, facePoints[i].tl(), facePoints[i].br(), new Scalar(255, 255, 255), 1);
                    }
                } else {
                    myTimer.scheduleAtFixedRate(new trackFace(gray), 1000, 5*1000);
                }
            }
        }

        @Override
        public void onCameraResume() {
            // opencv loaded
            recognizer = new LBPHFRecognizer(confidence);
            storage.initialize();

            if(!storage.isEmpty()){
                train();
            }
        }

        @Override
        public void onCameraPause() {
            // view disabled
            if(!storage.isEmpty()) {
                storage.save();
            }
        }
    };

    @Override
    public void setModelDetection(int model) {
        switch(model) {
            case 0:
                faceModel = "cascade.xml";
                break;
            case 1:
                faceModel = "lbp.xml";
                break;
        }

        AsyncTasks.loadFiles task = new AsyncTasks.loadFiles(getContext(), faceModel, fileLoaded);
        task.execute();
    }

    @Override
    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }


    @Override
    public int isDetected() {
        captured = Resources.cropImage(faces, gray);
        captured = Resources.improvements(captured);
        int status = Resources.checkDetection(faces, captured);
        return status;
    }

    @Override
    public boolean isCleared() {
        boolean cleared = storage.isCleared();
        train();
        return cleared;
    }

    @Override
    public void toTrain(final ReadableMap info) {
        if(!info.hasKey("fname")) callback.onFail("face name is incorrect");

        String name = info.getString("fname");

        storage.setImage(captured);
        storage.setLabel(name);

        train();
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

    private void train() {
        if(recognizer.train(storage.getListMat("images"), storage.getListString("labels")))
            callback.onComplete();
        else
            callback.onFail("Trained failed");
    }


    @Override
    public void onResume() {
        if (getContext() == null) return;
        loadOpenCV();
    }

    @Override
    public void setTorchMode(boolean enabled) {
        this.torchEnabled = enabled;
        super.setFlashMode();
    }

    @Override
    public void setTapToFocus(boolean tapToFocusEnabled) {
        this.tapToFocusEnabled = tapToFocusEnabled;
    }

    @Override
    public void setTrainingCallback(RecognitionMethods.onTrained callback) {
        this.callback = callback;
    }

    @Override
    public void setRecognitionCallback(RecognitionMethods.onRecognized callback) {
        this.reconitionCallback = callback;
    }

    private class trackFace extends TimerTask {
        private Mat gray;

        public trackFace(Mat gray) {
            this.gray = gray;
        }
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
}
