package cv.reactnative.facerecognition.recognizer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LBPHFRecognizer {
    private int confidence;
    private FaceRecognizer recognizer;
    private String[] uniqueLabels;

    public interface recognition {
        int ON_RECOGNIZED = 0;
        int ON_UNRECOGNIZED = 1;
        int ON_ERROR = 2;
    }

    public LBPHFRecognizer(int confidence) {
        this.confidence = confidence;
        recognizer = LBPHFaceRecognizer.create(3, 8, 8, 8, confidence);
    }


    public boolean train(ArrayList<Mat> images, ArrayList<String> labels) {
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
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String recognize(Mat face) {
        try {
            int label[] = new int[1];
            double confidence[] = new double[1];
            recognizer.predict(face, label, confidence);
            if (label[0] != -1 && (int)confidence[0] < this.confidence*0.6) {
                return uniqueLabels[label[0] - 1] + " " + (int) confidence[0];
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
