package cv.reactnative.facerecognition;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import org.opencv.android.Utils;
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
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerMedianFlow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    private LBPHFRecognizer recognizer;
    protected int confidence;
    private String faceModel;
    private Tinydb storage;
    private Timer myTimer = new Timer(true);
    private onTrained trainingCallback;
    private onRecognized recognitionCallback;
    private Mat mGray, mRgba;
    private Mat captured;
    private boolean datasetEnabled = false;
    private FaceDetection mDetection;

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
            myTimer = new Timer(true);
            mDetection = new FaceDetection(getContext(), quality, faceModel);
            mGray = new Mat();
            mRgba = new Mat();

            if (datasetEnabled && !storage.isDatasetLoaded()) {
                if(faceModel.contains("lbf")) {
                    throw new Error("you cannot use landmarks model with dataset enabled");
                }else {
                    getDataset task = new getDataset(getContext());
                    task.execute();
                }
            }else {
                startTimer();
            }
        }

        @Override
        public void onCameraStopped() {

        }

        @Override
        public void onCameraFrame(Mat rgba, Mat gray) {

            mGray = gray;
            mRgba = rgba;

            mDetection.update(mGray, mRgba);

        }

        @Override
        public void onCameraPause() {
            // view disabled
            if (storage != null && !storage.isEmpty()) {
                storage.save();
            }
            if (myTimer != null) {
                myTimer.cancel();
            }
        }
    };

    private void startTimer() {
        if(faceModel.contains("lbf"))
            myTimer.scheduleAtFixedRate(new FrameTimer(), 0, 500);
        else
            myTimer.scheduleAtFixedRate(new FrameTimer(), 0, 2*1000);
    }

    @Override
    public void setModelDetection(int model) {
        switch (model) {
            case FaceModule.Model.LANDMARKS:
                faceModel = "lbfmodel.yaml";
                break;
            case FaceModule.Model.LBP:
                faceModel = "cascade.xml";
                break;
        }
    }

    @Override
    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }


    @Override
    public void isDetected(Promise promise) {
        captured = mGray.clone();
        switch (mDetection.isFace(captured)) {
            case FaceDetection.detection.UKNOWN_FACE:
                promise.reject("UNKNOWN_FACE", "Find a face!");
                break;
            case FaceDetection.detection.BLURRED_IMAGE:
                promise.reject("BLURRED_IMAGE", "Photo is blurred. Snap new one!");
                break;
            case FaceDetection.detection.DETECTION_SUCCESS:
                captured = mDetection.crop(captured);
                promise.resolve(null);
                break;
        }
    }

    @Override
    public boolean isCleared() {
        boolean cleared = storage.isCleared();
        train();
        return cleared;
    }

    @Override
    public void toTrain(final ReadableMap info) {
        if (!info.hasKey("fname")) {
            trainingCallback.onFail("UNKNOWN_NAME");
            return;
        }

        String name = info.getString("fname");

        storage.setImage(captured);
        storage.setLabel(name);
        savePic(captured);
        train();
    }

    @Override
    public void isRecognized() {
        savePic(captured);
        String result = recognizer.recognize(captured);
        if (result != null) {
            recognitionCallback.onComplete(result);
        } else {
            if (!storage.isEmpty())
                recognitionCallback.onFail("UNRECOGNIZED");
            else
                recognitionCallback.onFail("EMPTY");
        }
    }

    private void train() {
        if (recognizer.train(storage.getImages(), storage.getLabels()))
            trainingCallback.onComplete();
        else
            trainingCallback.onFail("TRAINED_FAILED");
    }

    public void savePic(Mat cap){
        Bitmap bitmap = Bitmap.createBitmap(cap.cols(), cap.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cap, bitmap);
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/facerecognition");
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(dir, Math.random() + ".png");
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onResume() {
        if (getContext() == null) return;
        super.loadOpenCV();

        recognizer = new LBPHFRecognizer(confidence);
        storage = Tinydb.getInstance(getContext());
        storage.initialize();

        if (!storage.isEmpty()) {
            train();
        }
    }


    @Override
    public void setDataset(boolean enable) {
        this.datasetEnabled = enable;
    }

    @Override
    public void setTrainingCallback(onTrained callback) {
        this.trainingCallback = callback;
    }

    @Override
    public void setRecognitionCallback(onRecognized callback) {
        this.recognitionCallback = callback;
    }


    private class FrameTimer extends TimerTask {

        @Override
        public void run() {
            if (mGray != null) {
                mDetection.detect(mGray, mRgba);
            }
        }
    }

    FaceDetection.dataset dscallback = new FaceDetection.dataset() {
        @Override
        public void onDatasetLoaded() {
            train();
            storage.datasetLoaded();
            startTimer();
        }
    };

    private class getDataset extends AsyncTask<Boolean, Void, Boolean> {
        private Context context;

        getDataset(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(Boolean... strings) {
            if (context != null) {
                InputStream inp = null;
                AssetManager manager = context.getAssets();
                try {
                    String[] images = manager.list("dataset");
                    ArrayList<String> listImages = new ArrayList<String>(Arrays.asList(images));
                    ArrayList<Mat> dataset = new ArrayList<>();
                    ArrayList<String> names = new ArrayList<>();

                    Collections.sort(listImages);

                    for (String image : listImages) {
                        inp = manager.open("dataset/" + image);
                        if (inp != null) {
                            Bitmap bitmap = BitmapFactory.decodeStream(inp);
                            String[] exp = image.split("_");
                            Mat photo = bitmapToMat(bitmap);
                            dataset.add(photo);
                            names.add(exp[0]);
                        }
                    }

                    mDetection.detect(dataset, names, dscallback);
                    return true;

                } catch (IOException e) {
                    Log.i(TAG, "Unable to load cascade file" + e);
                }
            }

            Log.d(TAG, "seems that context is null");
            return false;
        }

        private Mat bitmapToMat(Bitmap bitmap) {
            Mat output = new Mat();
            Utils.bitmapToMat(bitmap, output);
            Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2GRAY);
            return output;
        }

        @Override
        protected void onPostExecute(Boolean result) {


        }
    }
}
