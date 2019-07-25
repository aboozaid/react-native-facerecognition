package cv.reactnative.facerecognition;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerMedianFlow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import cv.reactnative.facerecognition.base.BaseCameraView;
import cv.reactnative.facerecognition.utils.Resources;

import static org.opencv.objdetect.Objdetect.CASCADE_DO_CANNY_PRUNING;

public class FaceDetection {
    private final static String TAG = FaceDetection.class.getName();
    private Context context;
    private CascadeClassifier cascade;
    private int quality;
    private MatOfRect faces;
    private Tracker tracker;
    private Rect2d trackerPoints;
    private Timer detector = new Timer(true);

    public interface detection {
        int UKNOWN_FACE = 0;
        int BLURRED_IMAGE = 1;
        //int MULTIPLE_FACES = 2;
        int DETECTION_SUCCESS = 3;
    }

    public FaceDetection(Context context, int quality, String filename) {
        this.context = context;
        this.quality = quality;

        faces = new MatOfRect();
        loadclassifier(filename);
    }

    private void loadclassifier(String filename) {
        getClassifier task = new getClassifier(context, filename);
        task.execute();
    }

    public void detect(Mat gray, Mat rgba) {
        detector.schedule(new FaceTimer(gray, rgba), 0);
    }

    public void update(Mat gray, Mat rgba) {
        if(tracker != null && tracker.update(gray, trackerPoints)){
            draw(rgba);
        }
    }

    private void tracker(Mat gray, Mat rgba) {
        Rect[] facesArray = faces.toArray();
        trackerPoints = new Rect2d(facesArray[0].tl(), facesArray[0].br());
        tracker = TrackerMedianFlow.create();
        tracker.init(gray, trackerPoints);

        draw(rgba);
    }

    private void clean() {
        if(tracker != null) {
            tracker.clear();
            tracker.empty();
            tracker = null;
            trackerPoints = new Rect2d();
        }
    }

    public int isFace(Mat gray) {
        if(trackerPoints != null && !trackerPoints.empty()) {
            if(Resources.isBlurImage(gray)) {
                return detection.BLURRED_IMAGE;
            }else {
                return detection.DETECTION_SUCCESS;
            }
        }else{
            return detection.UKNOWN_FACE;
        }
    }

    public Mat crop(Mat gray) {
        Mat cropped = Resources.enhance(gray, faces);
        return cropped;
    }

    private void draw(Mat rgba) {
        Imgproc.rectangle(rgba, trackerPoints.tl(), trackerPoints.br(), new Scalar(255, 255, 255), 1);
    }

    private class FaceTimer extends TimerTask {
        Mat gray, rgba;


        public FaceTimer(Mat gray, Mat rgba) {
            this.gray = gray;
            this.rgba = rgba;
        }

        @Override
        public void run() {
            if(cascade != null) {
                if (quality == BaseCameraView.Quality.MEDIUM)
                    cascade.detectMultiScale(gray, faces, 1.4, 3, CASCADE_DO_CANNY_PRUNING, new Size(30, 30));
                else
                    cascade.detectMultiScale(gray, faces, 1.6, 3, CASCADE_DO_CANNY_PRUNING, new Size(30, 30));

                if (!faces.empty() && faces.toArray().length < 2) {
                    tracker(gray, rgba);
                } else{
                    clean();
                }
            }
        }
    }

    private class getClassifier extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private String filename;

        getClassifier(Context context, String filename) {
            this.context = context;
            this.filename = filename;
        }

        @Override
        protected Boolean doInBackground(Void... strings) {
            if (context != null) {
                InputStream inp = null;
                OutputStream out = null;
                try {
                    inp = context.getResources().getAssets().open(filename);
                    File outFile = new File(context.getCacheDir(), filename);
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
            if (result) {
                File file = new File(context.getCacheDir(), filename);
                cascade = new CascadeClassifier(file.getAbsolutePath());

            }
        }
    }
    }
