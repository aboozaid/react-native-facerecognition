package opencv.android;

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
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Assem Abozaid on 7/12/2018.
 */

public class Tinydb {
    private static String TAG = Tinydb.class.getSimpleName();
    private SharedPreferences preferences;
    public Tinydb(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    private static boolean loadFile(Context context, String cascadeName) {
        InputStream inp = null;
        OutputStream out = null;
        boolean completed = false;
        try {
            inp = context.getResources().getAssets().open(cascadeName);
            File outFile = new File(context.getCacheDir(), cascadeName);
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[4096];
            int bytesread;
            while((bytesread = inp.read(buffer)) != -1) {
                out.write(buffer, 0, bytesread);
            }

            completed = true;
            inp.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.i(TAG, "Unable to load cascade file" + e);
        }
        return completed;
    }
    public static CascadeClassifier loadXMLS(Context context, String cascadeName) {

        CascadeClassifier classifier = null;

        if(loadFile(context, cascadeName)) {
            File cascade = new File(context.getCacheDir(), cascadeName);
            classifier = new CascadeClassifier(cascade.getAbsolutePath());
            Log.i(TAG, "Cascade File Loaded Successfully");
            cascade.delete();
        } else {
            Log.i(TAG, "Path Direction May Be Wrong");
        }
        return classifier;
    }
    public ArrayList<Mat> getListMat(String key){
        ArrayList<String> objStrings = getListString(key);
        ArrayList<Mat> objects =  new ArrayList<>();

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
    public ArrayList<String> getListString(String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }
    public void putListMat(String key, ArrayList<Mat> objArray){
        checkForNullKey(key);
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
        checkForNullKey(key);
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }
    public void checkForNullKey(String key){
        if (key == null){
            throw new NullPointerException();
        }
    }
}
