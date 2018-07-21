package opencv.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;

import com.facebook.react.bridge.ReadableMap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


import static org.opencv.objdetect.Objdetect.CASCADE_DO_CANNY_PRUNING;
import static org.opencv.objdetect.Objdetect.CASCADE_FIND_BIGGEST_OBJECT;
import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

/**
 * Created by Assem Abozaid on 7/12/2018.
 */

public class FaceOperations {
    private static String TAG = FaceOperations.class.getName();
    private static String module;
    private CascadeClassifier classifier;
    private MatOfRect faces;
    private double scaleFactor;
    private int minNeighbors;
    private Size objSize;
    private int flag;
    private ArrayList<Mat> images;
    private ArrayList<String> imagesLabels;
    private String[] uniqueLabels;
    private FaceRecognizer recognize;
    private Tinydb tinydb;
    private int count = 1;
    private int radius = 3, neighbors = 8, grid_X = 8, grid_y = 8, maxConfidence = 125;
    private double threshold = 200;
    private Handler task;
    public FaceOperations(Context context) {
        faces = new MatOfRect();
        images = new ArrayList<>();
        imagesLabels = new ArrayList<>();
        tinydb = new Tinydb(context);
    }
    public Mat bitmapToMat(String imageAsBase64) throws Exception {

        byte[] decodedString = Base64.decode(imageAsBase64, Base64.DEFAULT);
        Bitmap image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        Mat converted = new Mat();
        Mat grayScale = new Mat();
        Mat clashed = new Mat();
        Utils.bitmapToMat(image, converted);

        Imgproc.cvtColor(converted, grayScale, Imgproc.COLOR_BGR2GRAY);

        //Imgproc.equalizeHist(grayScale, grayScale);

        CLAHE clash = Imgproc.createCLAHE();
        clash.setClipLimit(2);
        clash.apply(grayScale, clashed);


        return clashed;
    }

    public void DefaultConfig(int detection, Context context) throws Exception {

        switch(detection) {
            case FaceModule.DetectionConfig.DeepDetection:
                scaleFactor = 1.1;
                minNeighbors = 4;
                objSize = new Size(30,30);
                flag = CASCADE_SCALE_IMAGE;
                module = "Default.xml";
                break;
            case FaceModule.DetectionConfig.FasterDetection:
                scaleFactor = 1.35;
                minNeighbors = 5;
                objSize = new Size(30,30);
                flag = CASCADE_DO_CANNY_PRUNING;
                module = "Default.xml";
                break;
            default:
                break;
        }
        classifier = Tinydb.loadXMLS(context, module);
        recognize = LBPHFaceRecognizer.create(radius,neighbors,grid_X,grid_y,threshold);

        TrainFaces();
    }
    public void DetectionConfig(ReadableMap parms, Context context,ReadableMap recognition) throws Exception {

        if(!parms.hasKey("scaleFactor") || !parms.hasKey("minNeighbors")
                || !parms.hasKey("minWidth") || !parms.hasKey("minHeight")
                || !parms.hasKey("flag") || !parms.hasKey("module")) throw new Exception();

        scaleFactor = parms.getDouble("scaleFactor");
        minNeighbors = parms.getInt("minNeighbors");
        objSize = new Size(parms.getInt("minWidth"), parms.getInt("minHeight"));
        switch(parms.getInt("flag")) {
            case FaceModule.DetectionConfig.BiggestObject:
                flag = CASCADE_FIND_BIGGEST_OBJECT;
                break;
            case FaceModule.DetectionConfig.DoCannyDetect:
                flag = CASCADE_DO_CANNY_PRUNING;
                break;
            default:
                flag = CASCADE_SCALE_IMAGE;
                break;
        }
        switch (parms.getInt("module")) {
            case FaceModule.DetectionModule.HaarCascadeFace:
                module = "HaarCascadeFace.xml";
                break;
            case FaceModule.DetectionModule.LBPCascade:
                module = "LBPCascade.xml";
                break;
            default:
                module = "Default.xml";
                break;
        }
        classifier = Tinydb.loadXMLS(context, module);
        RecognizeConfig(recognition);

        task.handleMessage(Message.obtain());

    }
    public void RecognizeConfig(ReadableMap parms) throws Exception {

        if(!parms.hasKey("radius") || !parms.hasKey("neighbors")
                || !parms.hasKey("grid_x") || !parms.hasKey("grid_y")
                    || !parms.hasKey("threshold") || !parms.hasKey("maxConfidence")) throw new Exception();

        radius = parms.getInt("radius");
        neighbors = parms.getInt("neighbors");
        grid_X = parms.getInt("grid_x");
        grid_y = parms.getInt("grid_y");
        threshold = parms.getDouble("threshold");
        maxConfidence = parms.getInt("maxConfidence");

        recognize = LBPHFaceRecognizer.create(radius,neighbors,grid_X,grid_y,threshold);

        task.handleMessage(Message.obtain());
    }
    public int DetectFace(Mat image) {
        classifier.detectMultiScale(image, faces, scaleFactor, minNeighbors, flag, objSize);

        if(faces.empty())
            return 1;
        else {
            if(faces.toArray().length > 1)
                return 2;
            else
                return 0;
        }
    }
    public boolean Training(ReadableMap face) {
        try {
            if(!face.hasKey("image64") || !face.hasKey("Fname")) throw new Exception();
            String image64 = face.getString("image64");
            String label = face.getString("Fname").trim();

            Mat newImage = bitmapToMat(image64);
            Mat cropedFace = cropImage(newImage);
            String fname = label.substring(0, 1).toUpperCase(Locale.US) + label.substring(1).trim().toLowerCase(Locale.US);

            images.add(cropedFace);
            imagesLabels.add(fname);

            TrainFaces();
        } catch(Exception e) {
            return false;
        }
        return true;
    }
    private Mat cropImage(Mat fullFace) {
        Rect  rect_crop = null;
        for(Rect face : faces.toArray())
            rect_crop = new Rect(face.x, face.y, face.width, face.height);
        Mat faceLandmarks = new Mat(fullFace, rect_crop);
        return faceLandmarks;

    }
    public boolean isBlurImage(String imageAsBase64) {
        byte[] decodedString = Base64.decode(imageAsBase64, Base64.DEFAULT);
        Bitmap image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        int l = CvType.CV_8UC1; //8-bit grey scale image
        Mat matImage = new Mat();
        Utils.bitmapToMat(image, matImage);
        Mat matImageGrey = new Mat();
        Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY);

        Bitmap destImage;
        destImage = Bitmap.createBitmap(image);
        Mat dst2 = new Mat();
        Utils.bitmapToMat(destImage, dst2);
        Mat laplacianImage = new Mat();
        dst2.convertTo(laplacianImage, l);
        Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U);
        Mat laplacianImage8bit = new Mat();
        laplacianImage.convertTo(laplacianImage8bit, l);

        Bitmap bmp = Bitmap.createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(laplacianImage8bit, bmp);
        int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        int maxLap = -16777216; // 16m
        for (int pixel : pixels) {
            if (pixel > maxLap)
                maxLap = pixel;
        }
//            int soglia = -6118750;
        int soglia = -8118750;
        if (maxLap <= soglia) {
            return true;
        }
        return false;
    }
    private void TrainFaces()  {
        if(images.isEmpty()) return;
        List<Mat> imagesMatrix = new ArrayList<>();
        for (int i = 0; i < images.size(); i++)
            imagesMatrix.add(images.get(i));
        Set<String> uniqueLabelsSet = new HashSet<>(imagesLabels);
        uniqueLabels = uniqueLabelsSet.toArray(new String[uniqueLabelsSet.size()]);
        int[] classesNumbers = new int[uniqueLabels.length];
        for (int i = 0; i < classesNumbers.length; i++)
            classesNumbers[i] = i + 1;
        int[] classes = new int[imagesLabels.size()];
        for (int i = 0; i < imagesLabels.size(); i++) {
            String label = imagesLabels.get(i);
            for (int j = 0; j < uniqueLabels.length; j++) {
                if (label.equals(uniqueLabels[j])) {
                    classes[i] = classesNumbers[j];
                    break;
                }
            }
        }
        Mat vectorClasses = new Mat(classes.length, 1, CvType.CV_32S);
        vectorClasses.put(0, 0, classes);

        recognize.train(imagesMatrix, vectorClasses);

    }
    public String Recognition(Mat recognizeFace) throws Exception {
        Mat faceCroped = cropImage(recognizeFace);
        int label[] = new int[1];
        double confidence[] = new double[1];
        recognize.predict(faceCroped, label, confidence);
        if(label[0] != -1 && (int)confidence[0] < maxConfidence)
            return uniqueLabels[label[0]-1] + " " + (int)confidence[0];
        else
            return "Unknown";
    }
    public void Clean() {
        images.clear();
        imagesLabels.clear();

        TrainFaces();


    }
    public void OnResume() {
        images = tinydb.getListMat("images");
        imagesLabels = tinydb.getListString("imagesLabels");

        task = new Handler() {
            public void handleMessage(Message msg) {
                TrainFaces();
            }
        };
    }
    public void OnPause() {
        if(images == null) return;
        tinydb.putListMat("images", images);
        tinydb.putListString("imagesLabels", imagesLabels);
    }
}
