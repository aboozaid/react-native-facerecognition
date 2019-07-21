package cv.reactnative.facerecognition;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by Assem Abozaid on 8/3/2018.
 */

public class ReactMethods {
    private static ReactMethods instance;
    private CLAHE clahe;
    private MatOfPoint2f points;

    private ReactMethods() {
        clahe = Imgproc.createCLAHE();
    }

    static ReactMethods getInstance() {
        if(instance == null)
            instance = new ReactMethods();
        return instance;
    }

    public int checkDetection(final MatOfRect faces, final Mat grayImage) {
        if(!faces.empty()) {
            if(isBlurImage(grayImage))
                return 1;
            else {
                if(faces.toArray().length > 1)
                    return 2;
                else
                    return 3;
            }
        } else
            return 0;
    }

    private boolean isBlurImage(final Mat grayImage) {

        int l = CvType.CV_8UC1; //8-bit grey scale image
        Mat dst2 = new Mat();
        Imgproc.cvtColor(grayImage, dst2, Imgproc.COLOR_GRAY2RGB);
        Mat laplacianImage = new Mat();
        dst2.convertTo(laplacianImage, l);
        Imgproc.Laplacian(grayImage, laplacianImage, CvType.CV_8U);
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
    public Mat improvements(final Mat image) {
        Mat improve = new Mat();

        clahe.setClipLimit(4);
        clahe.apply(image, improve);
        Imgproc.equalizeHist(improve, improve);

        return improve;
    }
    public Mat cropImage(final MatOfRect faces, final Mat image) {
        if(!faces.empty()) {
            Rect rect_crop = null;
            points = new MatOfPoint2f();
            ArrayList<Point> rectPoints = new ArrayList<>();
            for (Rect face : faces.toArray()) {
                rect_crop = new Rect(face.x, face.y, face.width, face.height);
                rectPoints.add(new Point(face.tl().x, face.br().y));
            }
            points.fromList(rectPoints);
            Mat cropedFace = new Mat(image, rect_crop);

            return cropedFace;
        }
        return image;
    }
}
