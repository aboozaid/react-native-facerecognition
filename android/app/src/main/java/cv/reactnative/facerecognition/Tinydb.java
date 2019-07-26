package cv.reactnative.facerecognition;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Assem Abozaid on 7/12/2018.
 */

public class Tinydb {
    private static String TAG = Tinydb.class.getSimpleName();
    private SharedPreferences preferences;
    private ArrayList<Mat> images;
    private ArrayList<String> labels;
    private static Tinydb instance;

    private Tinydb(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Tinydb getInstance(Context context) {
        if(instance == null)
            instance = new Tinydb(context);
        return instance;
    }

    public void initialize() {
        images = getListMat("images");
        labels = getListString("labels");
    }
    public void save() {
        putListMat("images", images);
        putListString("labels", labels);
    }
    public ArrayList<String> getListString(String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }
    public ArrayList<Mat> getListMat(String key){
        ArrayList<String> objStrings = getListString(key);
        ArrayList<Mat> objects =  new ArrayList<Mat>();

        for (String jObjString : objStrings) {
            byte[] data = Base64.decode(jObjString, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            Mat mat = new Mat();
            Utils.bitmapToMat(bmp, mat);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            objects.add(mat);
        }
        return objects;
    }
    public void putListMat(String key, ArrayList<Mat> objArray){
        ArrayList<String> objStrings = new ArrayList<>();
        Bitmap bmp = null;
        ByteArrayOutputStream baos = null;
        Mat uncolored = null;
        for (Mat mat : objArray) {
            uncolored = new Mat();
            Imgproc.cvtColor(mat, uncolored, Imgproc.COLOR_GRAY2BGR);
            bmp = Bitmap.createBitmap(uncolored.cols(), uncolored.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(uncolored, bmp);
            baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            String dataString = new String(Base64.encode(data, Base64.DEFAULT));
            objStrings.add(dataString);
        }
        putListString(key, objStrings);
    }
    public void putListString(String key, ArrayList<String> stringList) {
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();

    }
    public void setImage(Mat image) {
        images.add(image);
    }
    public void setLabel(String label) {
        labels.add(label);
    }
    public ArrayList<Mat> getImages() {
        return images;
    }
    public ArrayList<String> getLabels() {
        return labels;
    }
    public boolean isEmpty() {
        return images.isEmpty();
    }
    public void datasetLoaded() {
        preferences.edit().putString("dataset", "loaded").apply();
    }
    public boolean isDatasetLoaded() {
        return preferences.contains("dataset");
    }
    /*public void checkForNullKey(String key){
        if (key == null){
            throw new NullPointerException();
        }
    }*/

    public boolean isCleared() {
        images.clear();
        labels.clear();
        preferences.edit().remove("images").commit();
        preferences.edit().remove("labels").commit();
        preferences.edit().remove("dataset").commit();
        save();

        return true;
    }

}
