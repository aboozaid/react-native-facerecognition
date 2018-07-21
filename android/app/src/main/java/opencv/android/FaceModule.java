package opencv.android;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Assem Abozaid on 7/9/2018.
 */

public class FaceModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    final static String Lib = "Face";
    final static String TAG = FaceModule.class.getName();
    private FaceOperations operation;
    public FaceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);

    }

    public interface DetectionConfig {
        int DeepDetection = 0;
        int FasterDetection = 1;
        int ScaleImage = 2;
        int BiggestObject = 3;
        int DoCannyDetect = 4;
    }
    public interface DetectionModule {
        int DefaultModule = 0;
        int HaarCascadeFace = 1;
        int LBPCascade = 2;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        WritableMap DETECTION_MODULE = Arguments.createMap();
        DETECTION_MODULE.putInt("Default", DetectionModule.DefaultModule);
        DETECTION_MODULE.putInt("Cascade", DetectionModule.HaarCascadeFace);
        DETECTION_MODULE.putInt("LBP", DetectionModule.LBPCascade);

        WritableMap DETECTION_CONFIG = Arguments.createMap();
        DETECTION_CONFIG.putInt("DEEP", DetectionConfig.DeepDetection);
        DETECTION_CONFIG.putInt("FAST", DetectionConfig.FasterDetection);
        DETECTION_CONFIG.putInt("Scale", DetectionConfig.ScaleImage);
        DETECTION_CONFIG.putInt("Biggest", DetectionConfig.BiggestObject);
        DETECTION_CONFIG.putInt("Canny", DetectionConfig.DoCannyDetect);
        DETECTION_CONFIG.putMap("Module", DETECTION_MODULE);



        constants.put("Detection", DETECTION_CONFIG);


        return constants;
    }
    @Override
    public String getName() {
        return Lib;
    }
    @ReactMethod
    public void Detect(String imageAsBase64, Callback successCallback, Callback errorCallback) {
        try {
            if(!operation.isBlurImage(imageAsBase64)) {
                Mat matImage = operation.bitmapToMat(imageAsBase64);

                if(operation.DetectFace(matImage) == 0) {
                    successCallback.invoke("Face Detected");
                } else if(operation.DetectFace(matImage) == 1) {
                    errorCallback.invoke("Error! no face inside image");
                }else {
                    errorCallback.invoke("Can't detect the face keep detecting");
                }
            } else {
                errorCallback.invoke("Photo is blurred capture new one");
            }


        } catch(Exception e){
            errorCallback.invoke(e.getMessage());
        }

    }
    @ReactMethod
    public void Start(int detection, Callback successCallback, Callback errorCallback) {
        try {
            operation.DefaultConfig(detection, getCurrentActivity());
            successCallback.invoke();

        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }
    @ReactMethod
    public void Initialize(ReadableMap detection, ReadableMap recognition, Callback successCallback, Callback errorCallback) {
        try {
            operation.DetectionConfig(detection, getCurrentActivity(), recognition);
            successCallback.invoke();
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }
    @ReactMethod
    public void Training(ReadableMap face, Callback successCallback, Callback errorCallback) {
        if(operation.Training(face)){
            successCallback.invoke("Trained");
        } else {
            errorCallback.invoke("Untrained");
        }
    }
    @ReactMethod
    public void Identify(String imageAsBase64 , Callback unrecognized) {
        try {
            Mat face = operation.bitmapToMat(imageAsBase64);
            String predicted = operation.Recognition(face);
            if(predicted.contains("Unknown"))
                unrecognized.invoke("Keep training!");
            else {
                String faceInfo[] = predicted.split(" ");
                WritableMap data = Arguments.createMap();
                data.putString("name", faceInfo[0]);
                data.putString("distance", faceInfo[1]);
                this.getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onFaceRecognized", data);
            }
        } catch (Exception e) {
            unrecognized.invoke("Please train some faces");
        }
    }
    @ReactMethod
    public void Clean() {
        operation.Clean();
        this.getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onClean", "Cleaned");

    }
    @Override
    public void onHostResume() {
        if (getCurrentActivity() == null) return;
        BaseLoaderCallback loaderCallback = new BaseLoaderCallback(getCurrentActivity()) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case BaseLoaderCallback.SUCCESS: {
                        Log.i(TAG, "OpenCV Running");
                        operation = new FaceOperations(getCurrentActivity());
                        operation.OnResume();
                    }
                    break;
                    default: {
                        super.onManagerConnected(status);
                    }
                    break;
                }
            }
        };
        if(OpenCVLoader.initDebug()) {
            Log.i(TAG, "System Library Loaded Successfully");
            loaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.i(TAG, "Unable To Load System Library");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getCurrentActivity(), loaderCallback);
        }
    }

    @Override
    public void onHostPause() {
        operation.OnPause();
    }

    @Override
    public void onHostDestroy() {

    }
}
