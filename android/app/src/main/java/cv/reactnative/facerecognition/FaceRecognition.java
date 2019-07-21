package cv.reactnative.facerecognition;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;

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

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import assem.base.BaseModelFactory;
import assem.base.CameraCallbacks;
import assem.base.Factory;
import cv.reactnative.facerecognition.utils.Resources;

import static org.opencv.objdetect.Objdetect.CASCADE_DO_CANNY_PRUNING;

public class FaceRecognition {
    private BaseModelFactory factory;
    private Factory face;
    private ArrayList<Mat> images;
    private ArrayList<String> labels;
    private Mat captured;
    private RecognitionMethods recognition;
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
    private static FaceRecognition instance;

    private FaceRecognition(Context context) {
        factory = new BaseModelFactory(context);
        face = factory.getModel("FACEMODEL");
        storage = new Tinydb(context);

        face.getModel(camera);
    }

    static FaceRecognition getInstance(Context context) {
        if(instance == null)
            instance = new FaceRecognition(context);
        return instance;
    }

    public void setTrainingCallback(RecognitionMethods.onTrained callback) {
        this.callback = callback;
    }

    public void setRecognitionCallback(RecognitionMethods.onRecognized callback) {
        this.reconitionCallback = callback;
    }

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
            recognition = new RecognitionMethods(confidence);
            images = storage.getListMat("images");
            labels = storage.getListString("labels");

            if(!images.isEmpty()){
                reTreain();
            }
        }

        @Override
        public void onCameraPause() {
            // view disabled
        }

        @Override
        public void onCameraRotate(boolean isLandscape, Context context) {
            Activity activity = Resources.scanForActivity(context);
            if (activity == null) return;
            activity.setRequestedOrientation(isLandscape
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    };

    public void setModelDetection(int model) {
        switch(model) {
            case 0:
                faceModel = "cascade.xml";
                break;
            case 1:
                faceModel = "lbp.xml";
                break;
        }
    }

    public int isDetected() {
        captured = ReactMethods.getInstance().cropImage(faces, gray);
        captured = ReactMethods.getInstance().improvements(captured);
        int status = ReactMethods.getInstance().checkDetection(faces, captured);
        return status;
    }

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

    public void isTrained(final ReadableMap info) {
        if(!info.hasKey("fname")) callback.onFail("face name is incorrect");

        String name = info.getString("fname");

        images.add(captured);
        labels.add(name);

        reTreain();
    }

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

    public void setConfidence(int confidence) {
        this.confidence = confidence;
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
