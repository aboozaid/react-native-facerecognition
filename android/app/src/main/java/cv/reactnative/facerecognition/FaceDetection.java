package cv.reactnative.facerecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.Facemark;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.tracking.Tracker;
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
    private Tinydb storage;
    private dataset callback;
    private Facemark marks;
    private ArrayList<MatOfPoint2f> landmarks;
    private Rect rect;

    public interface detection {
        int UKNOWN_FACE = 0;
        int BLURRED_IMAGE = 1;
        //int MULTIPLE_FACES = 2;
        int DETECTION_SUCCESS = 3;
    }

    public interface dataset {
        void onDatasetLoaded();
    }

    public FaceDetection(Context context, int quality, String filename) {
        this.context = context;
        this.quality = quality;

        faces = new MatOfRect();
        if(filename.contains("lbfmodel")) {
            loadclassifier(filename);
            loadclassifier("lbp.xml");
        } else
            loadclassifier(filename);
    }

    private void loadclassifier(String filename) {
        getClassifier task = new getClassifier(context, filename);
        task.execute();
    }

    public void detect(Mat gray, Mat rgba) {
        detector.schedule(new FaceTimer(gray, rgba), 0);
    }

    public void detect(ArrayList<Mat> dataset, ArrayList<String> names, dataset callback) {
        this.callback = callback;

        if(storage == null)
            storage = Tinydb.getInstance(context);

        Timer timer = new Timer(true);
        timer.schedule(new DatasetTimer(dataset, names), 0);
    }

    public void update(Mat gray, Mat rgba) {
        if(tracker != null && tracker.update(gray, trackerPoints) && marks == null){
            draw(rgba);
        }

        if(marks != null){
            if(fit(gray)) {
                drawLandmarks(rgba);
            }
        }
    }

    private boolean fit(Mat gray) {
        landmarks = new ArrayList<>();
        return marks.fit(gray, faces, landmarks);
    }

    private void tracker(Mat gray) {
        Rect[] facesArray = faces.toArray();
        trackerPoints = new Rect2d(facesArray[0].tl(), facesArray[0].br());
        tracker = TrackerMedianFlow.create();
        tracker.init(gray, trackerPoints);

    }

    private void clean() {
        if(tracker != null) {
            tracker.clear();
            tracker.empty();
            tracker = null;
            trackerPoints = new Rect2d();
        }

        if(marks != null && landmarks != null) {
            landmarks.clear();
            marks.clear();
            marks.empty();
        }
    }

    public int isFace(Mat gray) {
        if((trackerPoints != null && !trackerPoints.empty()) || (marks != null && !landmarks.isEmpty())) {
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
        Mat cropped;
        if(marks == null)
            cropped = Resources.enhance(gray, faces);
        else
            cropped = Resources.enhance(gray, rect);

        return cropped;
    }

    private void draw(Mat rgba) {
        Imgproc.rectangle(rgba, trackerPoints.tl(), trackerPoints.br(), new Scalar(255, 255, 255), 1);
    }

    private void drawLandmarks(Mat rgba) {

        for (int i=0; i<landmarks.size(); i++) {
            MatOfPoint2f lm = landmarks.get(i);
            Point[] points = lm.toArray();

            if(points.length == 68) {
                drawPolyLines(0,16, lm, rgba);
                drawPolyLines(17,21, lm,rgba);
                drawPolyLines(22,26, lm,rgba);
            }

            rect = createRect(lm);

            Imgproc.rectangle(rgba, rect, new Scalar(255,255,255));
        }
    }

    private Rect createRect(MatOfPoint2f lm) {
        RotatedRect rotaterect = Imgproc.minAreaRect(lm);
        Rect rect = rotaterect.boundingRect();
        return rect;
    }

    private void drawPolyLines(int start, int end, MatOfPoint2f marks, Mat rgba) {
        Point[] points = marks.toArray();
        for(int i= start; i < end; i++) {
            Imgproc.line(rgba, new Point(points[i].x, points[i].y), new Point(points[i+1].x, points[i+1].y) ,new Scalar(255,255,255));
        }
    }

    private void dataset(Mat photo, String name) {
        photo = Resources.enhance(photo, faces);
        storage.setImage(photo);
        storage.setLabel(name);

        //savePic(photo);
    }

    /*public void savePic(Mat cap){
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
    }*/

    private class FaceTimer extends TimerTask {
        Mat gray, rgba;
        String name;

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
                    if(marks == null)
                        tracker(gray);
                } else {
                    clean();
                }
            }
        }
    }

    private class DatasetTimer extends TimerTask {
        ArrayList<Mat> dataset;
        ArrayList<String> names;

        public DatasetTimer(ArrayList<Mat> dataset, ArrayList<String> names) {
            this.dataset = dataset;
            this.names = names;
        }

        @Override
        public void run() {
            while(cascade == null);
            for(int i=0; i<dataset.size(); i++) {
                Mat gray = dataset.get(i);
                cascade.detectMultiScale(dataset.get(i), faces, 1.4, 3, CASCADE_DO_CANNY_PRUNING, new Size(30, 30));
                if(!faces.empty() && faces.toArray().length < 2) {
                    dataset(gray, names.get(i));
                }
            }
            callback.onDatasetLoaded();
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
                    File outFile = new File(context.getDir("files", Context.MODE_PRIVATE), filename);
                    if(!outFile.exists()) {
                        out = new FileOutputStream(outFile);

                        byte[] buffer = new byte[4096];
                        int bytesread;
                        while ((bytesread = inp.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesread);
                        }

                        inp.close();
                        out.flush();
                        out.close();
                        Log.i(TAG, "********** FILE LOADED ***********");
                    }
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
                File file = new File(context.getDir("files", Context.MODE_PRIVATE), filename);
                if(!filename.contains("lbfmodel"))
                    cascade = new CascadeClassifier(file.getAbsolutePath());
                else {
                    marks = Face.createFacemarkLBF();
                    marks.loadModel(file.getAbsolutePath());
                }

            }
        }
    }
    }
