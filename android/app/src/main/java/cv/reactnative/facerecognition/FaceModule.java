package cv.reactnative.facerecognition;

import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

import java.util.HashMap;
import java.util.Map;

import assem.base.CameraModel;
import assem.base.CameraSettings;
import cv.reactnative.R;


/**
 * Created by Assem Abozaid on 7/9/2018.
 */

public class FaceModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    final static String Lib = "Face";
    final static String TAG = FaceModule.class.getName();
    private FaceRecognition recog;

    public FaceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);
        recog = FaceRecognition.getInstance(reactContext);
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {

    }

    public interface Model {
        int DefaultModule = 0;
        int LBPCascade = 1;
    }


    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        WritableMap module = Arguments.createMap();
        module.putInt("cascade", Model.DefaultModule);
        module.putInt("lbp", Model.LBPCascade);

        WritableMap aspectMap = Arguments.createMap();
        aspectMap.putInt("stretch", CameraSettings.CameraAspectStretch);
        aspectMap.putInt("fit", CameraSettings.CameraAspectFit);
        aspectMap.putInt("fill", CameraSettings.CameraAspectFill);


        WritableMap captureQualityMap = Arguments.createMap();
        captureQualityMap.putInt("low", CameraSettings.CameraCaptureSessionPresetLow);
        captureQualityMap.putInt("medium", CameraSettings.CameraCaptureSessionPresetMedium);
        captureQualityMap.putInt("high", CameraSettings.CameraCaptureSessionPresetHigh);


        WritableMap torchModeMap = Arguments.createMap();
        torchModeMap.putInt("off", CameraSettings.CameraTorchModeOff);
        torchModeMap.putInt("on", CameraSettings.CameraTorchModeOn);

        WritableMap cameraTypeMap = Arguments.createMap();
        cameraTypeMap.putInt("front", CameraSettings.CameraFront);
        cameraTypeMap.putInt("back", CameraSettings.CameraBack);

        WritableMap rotateModeMap = Arguments.createMap();
        rotateModeMap.putInt("off", CameraSettings.CameraRotateModeOff);
        rotateModeMap.putInt("on", CameraSettings.CameraRotateModeOn);

        constants.put("CameraModel", module);
        constants.put("Aspect", aspectMap);
        constants.put("CaptureQuality", captureQualityMap);
        constants.put("TorchMode", torchModeMap);
        constants.put("CameraType", cameraTypeMap);
        constants.put("RotateMode", rotateModeMap);

        return constants;
    }
    @Override
    public String getName() {
        return Lib;
    }

    @ReactMethod
    public void detection(final int viewFlag,final Promise errorCallback) {
        final ReactApplicationContext rctx = getReactApplicationContext();
        UIManagerModule uiManager = rctx.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                final FrameLayout view = (FrameLayout) nativeViewHierarchyManager.resolveView(viewFlag);
                //final SettingsCamera camera = (SettingsCamera) view.findViewById(R.id.camera_view);
                switch(recog.isDetected()) {
                    case 0:
                        errorCallback.reject("Error", "Detection has timed out");
                        break;
                    case 1:
                        errorCallback.reject("Error", "Photo is blurred. Snap new one!");
                        break;
                    case 2:
                        errorCallback.reject("Error", "Multiple faces detection is not supported!");
                        break;
                    default:
                        errorCallback.resolve(null);
                        break;
                }
            }
        });
    }
    @ReactMethod
    public void train(final ReadableMap info,final int viewFlag) {
        final ReactApplicationContext rctx = getReactApplicationContext();
        UIManagerModule uiManager = rctx.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                final FrameLayout view = (FrameLayout) nativeViewHierarchyManager.resolveView(viewFlag);
                //final CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
                recog.isTrained(info);
            }
        });
    }
    @ReactMethod
    public void recognize(final int viewFlag) {
        final ReactApplicationContext rctx = getReactApplicationContext();
        UIManagerModule uiManager = rctx.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                final FrameLayout view = (FrameLayout) nativeViewHierarchyManager.resolveView(viewFlag);
                //final SettingsCamera camera = (SettingsCamera) view.findViewById(R.id.camera_view);
                recog.isRecognized();
            }
        });
    }
    @ReactMethod
    public void clear(final int viewFlag, final Promise status) {
        final ReactApplicationContext rctx = getReactApplicationContext();
        UIManagerModule uiManager = rctx.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                final FrameLayout view = (FrameLayout) nativeViewHierarchyManager.resolveView(viewFlag);
                //final SettingsCamera camera = (SettingsCamera) view.findViewById(R.id.camera_view);
                if(recog.isCleared())
                    status.resolve(null);
                else
                    status.reject("Error", "Uncleared");
            }
        });
    }
}
