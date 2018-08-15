package opencv.android;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

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
    public FaceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);
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
    public interface CameraAspect {
        int CameraAspectFill = 0;
        int CameraAspectFit = 1;
        int CameraAspectStretch = 2;
    }
    public interface CameraCaptureSessionPreset {
        int CameraCaptureSessionPresetLow = 0;
        int CameraCaptureSessionPresetMedium = 1;
        int CameraCaptureSessionPresetHigh = 2;

    }
    public interface CameraTorchMode {
        int CameraTorchModeOff = 0;
        int CameraTorchModeOn = 1;
    }
    public interface CameraType {
        int CameraFront = 0;
        int CameraBack = 1;
    }
    public interface CameraRotateMode {
        int CameraRotateModeOff = 0;
        int CameraRotateModeOn = 1;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        WritableMap module = Arguments.createMap();
        module.putInt("cascade", Model.DefaultModule);
        module.putInt("lbp", Model.LBPCascade);

        WritableMap aspectMap = Arguments.createMap();
        aspectMap.putInt("stretch", CameraAspect.CameraAspectStretch);
        aspectMap.putInt("fit", CameraAspect.CameraAspectFit);
        aspectMap.putInt("fill", CameraAspect.CameraAspectFill);


        WritableMap captureQualityMap = Arguments.createMap();
        captureQualityMap.putInt("low", CameraCaptureSessionPreset.CameraCaptureSessionPresetLow);
        captureQualityMap.putInt("medium", CameraCaptureSessionPreset.CameraCaptureSessionPresetMedium);
        captureQualityMap.putInt("high", CameraCaptureSessionPreset.CameraCaptureSessionPresetHigh);


        WritableMap torchModeMap = Arguments.createMap();
        torchModeMap.putInt("off", CameraTorchMode.CameraTorchModeOff);
        torchModeMap.putInt("on", CameraTorchMode.CameraTorchModeOn);

        WritableMap cameraTypeMap = Arguments.createMap();
        cameraTypeMap.putInt("front", CameraType.CameraFront);
        cameraTypeMap.putInt("back", CameraType.CameraBack);

        WritableMap rotateModeMap = Arguments.createMap();
        rotateModeMap.putInt("off", CameraRotateMode.CameraRotateModeOff);
        rotateModeMap.putInt("on", CameraRotateMode.CameraRotateModeOn);

        constants.put("Model", module);
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
                final SettingsCamera camera = (SettingsCamera) view.findViewById(R.id.camera_view);
                switch(camera.isDetected()) {
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
                final SettingsCamera camera = (SettingsCamera) view.findViewById(R.id.camera_view);
                camera.isTrained(info);
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
                final SettingsCamera camera = (SettingsCamera) view.findViewById(R.id.camera_view);
                camera.isRecognized();
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
                final SettingsCamera camera = (SettingsCamera) view.findViewById(R.id.camera_view);
                if(camera.isCleared())
                    status.resolve(null);
                else
                    status.reject("Error", "Uncleared");
            }
        });
    }
}
