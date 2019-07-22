package cv.reactnative.facerecognition;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Assem Abozaid on 8/4/2018.
 */

public class RecognitionMethods {
    private FaceRecognizer recognizer;
    private String[] uniqueLabels;
    private int maxConfidence;

    public RecognitionMethods(int maxConfidence) {
        this.maxConfidence = maxConfidence;
        recognizer = LBPHFaceRecognizer.create(3, 8, 8, 8, this.maxConfidence);
    }

    public interface onTrained {
        void onComplete();

        void onFail(String err);
    }

    public interface onRecognized {
        void onComplete(String result);

        void onFail(String err);
    }

    public void isTrained(ArrayList<Mat> images, ArrayList<String> labels, onTrained callback) {
        try {
            List<Mat> imagesMatrix = new ArrayList<>();
            for (int i = 0; i < images.size(); i++)
                imagesMatrix.add(images.get(i));
            Set<String> uniqueLabelsSet = new HashSet<>(labels);
            uniqueLabels = uniqueLabelsSet.toArray(new String[uniqueLabelsSet.size()]);
            int[] classesNumbers = new int[uniqueLabels.length];
            for (int i = 0; i < classesNumbers.length; i++)
                classesNumbers[i] = i + 1;
            int[] classes = new int[labels.size()];
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                for (int j = 0; j < uniqueLabels.length; j++) {
                    if (label.equals(uniqueLabels[j])) {
                        classes[i] = classesNumbers[j];
                        break;
                    }
                }
            }

            Mat vectorClasses = new Mat(classes.length, 1, CvType.CV_32S);
            vectorClasses.put(0, 0, classes);

            if(!images.isEmpty())
                recognizer.train(imagesMatrix, vectorClasses);
            else
                recognizer.update(imagesMatrix, vectorClasses);
            callback.onComplete();
        } catch (Exception e) {
            callback.onFail("Failed to train");
        }
    }

    public void isRecognized(Mat face, onRecognized callback) {
        try {
            int label[] = new int[1];
            double confidence[] = new double[1];
            recognizer.predict(face, label, confidence);
            if (label[0] != -1 && (int)confidence[0] < maxConfidence*0.6) {
                callback.onComplete(uniqueLabels[label[0] - 1] + " " + (int) confidence[0]);
            } else {
                callback.onFail("Unpredictable face");
            }
        } catch (Exception e) {
            callback.onFail("Please train some faces");
        }
    }
}
