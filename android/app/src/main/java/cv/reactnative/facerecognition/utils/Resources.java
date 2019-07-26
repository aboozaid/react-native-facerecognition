package cv.reactnative.facerecognition.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;

import com.facebook.react.uimanager.ThemedReactContext;

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

public class Resources {

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

    /*public static int checkDetection(final MatOfRect faces, final Mat grayImage) {
        if(!faces.empty()) {
            if(isBlurImage(grayImage))
                return detection.BLURRED_IMAGE;
            else {
                return detection.DETECTION_SUCCESS;
            }
        } else
            return detection.UKNOWN_FACE;
    }*/

    public static Mat enhance(Mat photo, MatOfRect faces) {
        photo = cropImage(faces, photo);
        photo = improvements(photo);

        return photo;
    }

    public static Mat enhance(Mat photo, Rect rect) {
        Mat cropped = new Mat(photo, rect);
        cropped = improvements(cropped);

        return cropped;
    }

    private static Mat improvements(final Mat image) {
        Mat improve = new Mat();
        CLAHE clahe = Imgproc.createCLAHE();

        clahe.setClipLimit(4);
        clahe.apply(image, improve);

        Imgproc.equalizeHist(improve, improve);

        return improve;
    }

    private static Mat cropImage(final MatOfRect faces, final Mat image) {
        Rect rect_crop = null;
        MatOfPoint2f points = new MatOfPoint2f();
        ArrayList<Point> rectPoints = new ArrayList<>();
        for (Rect face : faces.toArray()) {
            rect_crop = new Rect(face.x, face.y, face.width, face.height);
            rectPoints.add(new Point(face.tl().x, face.br().y));
        }
        points.fromList(rectPoints);
        Mat cropedFace = new Mat(image, rect_crop);

        return cropedFace;
    }

    public static boolean isBlurImage(final Mat grayImage) {

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

}
