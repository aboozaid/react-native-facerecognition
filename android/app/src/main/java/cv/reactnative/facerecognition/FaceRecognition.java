package cv.reactnative.facerecognition;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private final static String TAG = FaceRecognition.class.getName();
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
    private onTrained trainingCallback;
    private onRecognized recognitionCallback;
    private Mat gray;

    public interface onTrained {
        void onComplete();

        void onFail(String err);
    }

    public interface onRecognized {
        void onComplete(String result);

        void onFail(String err);
    }

    public FaceRecognition(Context context, AttributeSet attrs) {
        super(context, attrs);
        getModel(camera);
    }

    private CameraCallbacks camera = new CameraCallbacks() {
        @Override
        public void onCameraStarted() {
            faces = new MatOfRect();
            myTimer = new Timer(true);
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
            storage = new Tinydb(getContext());
            storage.initialize();

            if(!storage.isEmpty()){
                train();
            }
        }

        @Override
        public void onCameraPause() {
            // view disabled
            if(storage != null && !storage.isEmpty()) {
                storage.save();
            }
            if(myTimer != null){
                myTimer.cancel();
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

        getClassifier task = new getClassifier(getContext(), faceModel);
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
        if(!info.hasKey("fname")) {
            trainingCallback.onFail("Unable to find the face name");
            return;
        }

        String name = info.getString("fname");

        storage.setImage(captured);
        storage.setLabel(name);

        train();
    }

    @Override
    public void isRecognized() {
        String result = recognizer.recognize(captured);
        if(result != null) {
            recognitionCallback.onComplete(result);
        } else {
            if(!storage.isEmpty())
                recognitionCallback.onFail("UNRECOGNIZED");
            else
                recognitionCallback.onFail("EMPTY");
        }
    }

    private void train() {
        if(recognizer.train(storage.getImages(), storage.getLabels()))
            trainingCallback.onComplete();
        else
            trainingCallback.onFail("Trained failed");
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
    public void setTrainingCallback(onTrained callback) {
        this.trainingCallback = callback;
    }

    @Override
    public void setRecognitionCallback(onRecognized callback) {
        this.recognitionCallback = callback;
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

    private class getClassifier extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private String file;


        getClassifier (Context context, String file) {
            this.context = context;
            this.file = file;
        }

        @Override
        protected Boolean doInBackground(Void... strings) {
            if(context != null) {
                InputStream inp = null;
                OutputStream out = null;
                try {
                    inp = context.getResources().getAssets().open(file);
                    File outFile = new File(context.getCacheDir(), file);
                    out = new FileOutputStream(outFile);

                    byte[] buffer = new byte[4096];
                    int bytesread;
                    while ((bytesread = inp.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesread);
                    }

                    inp.close();
                    out.flush();
                    out.close();
                    return true;
                } catch (IOException e) {
                    Log.i(TAG, "Unable to load cascade file" + e);
                }
            }
            Log.d(TAG, "seems that context is null");
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            File file = new File(getContext().getCacheDir(), faceModel);
            classifier = new CascadeClassifier(file.getAbsolutePath());
        }
    }
}
