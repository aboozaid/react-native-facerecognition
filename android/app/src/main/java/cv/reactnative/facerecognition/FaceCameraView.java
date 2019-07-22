package cv.reactnative.facerecognition;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.lang.ref.WeakReference;
import java.util.Map;

import cv.reactnative.facerecognition.base.CameraModel;
import cv.reactnative.facerecognition.base.CameraSettings;
import cv.reactnative.R;
import cv.reactnative.facerecognition.utils.Resources;


/**
 * Created by Assem Abozaid on 7/31/2018.
 */

public class FaceCameraView extends SimpleViewManager<FrameLayout> implements LifecycleEventListener {
    public static final String REACT_CLASS = "FaceCamera";
    public static final String TAG = ReactContextBaseJavaModule.class.getSimpleName();
    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    private WeakReference<ViewGroup> layoutRef;
    private BroadcastReceiver receiver;

    public FaceCameraView(ReactApplicationContext reactContext) {
        super();
        receiver = createOrientationReceiver();
        reactContext.addLifecycleEventListener(this);
    }
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected FrameLayout createViewInstance(final ThemedReactContext reactContext) {
        // onCreate in android


        LayoutInflater inflater = LayoutInflater.from(reactContext);
        final FrameLayout preview = (FrameLayout) inflater.inflate(R.layout.camera_view, null);
        layoutRef = new WeakReference<ViewGroup>(preview);
        CameraModel camera = (CameraModel) preview.findViewById(R.id.camera_view);

        camera.setTrainingCallback(new FaceRecognition.onTrained() {
            @Override
            public void onComplete() {
                ReactContext context = reactContext;
                context.getJSModule(RCTEventEmitter.class).receiveEvent(preview.getId(),"trainCompleted", null);
            }

            @Override
            public void onFail(String err) {
                ReactContext context = reactContext;
                WritableMap event = Arguments.createMap();
                event.putString("error", err);
                context.getJSModule(RCTEventEmitter.class).receiveEvent(preview.getId(),"TrainUncompleted", event);
            }
        });
        camera.setRecognitionCallback(new FaceRecognition.onRecognized() {
            @Override
            public void onComplete(String result) {
                ReactContext context = reactContext;
                String[] data = result.split(" ");
                WritableMap event = Arguments.createMap();
                event.putString("name", data[0]);
                event.putString("confidence", data[1]);
                context.getJSModule(RCTEventEmitter.class).receiveEvent(preview.getId(),"recognizeCompleted", event);
            }

            @Override
            public void onFail(String err) {
                ReactContext context = reactContext;
                WritableMap event = Arguments.createMap();
                event.putString("error", err);
                context.getJSModule(RCTEventEmitter.class).receiveEvent(preview.getId(),"recognizeUncompleted", event);
            }
        });


        return preview;
    }

    @ReactProp(name = "mounted")
    public void setMounted(FrameLayout view, @Nullable boolean mounted) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        if (mounted) {
            registerReceiver(view.getContext());
            camera.onResume();
        } else {
            camera.disableView();
        }
    }
    @ReactProp(name = "captureQuality")
    public void setCaptureQuality(FrameLayout view, @Nullable int captureQuality) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setQuality(captureQuality);
    }
    @ReactProp(name = "aspect")
    public void setAspect(FrameLayout view, @Nullable int aspect) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setAspect(aspect);
    }
    @ReactProp(name = "torchMode")
    public void setTorchMode(FrameLayout view, @Nullable int torchMode) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setTorchMode(torchMode == CameraSettings.CameraTorchModeOn);
    }
    @ReactProp(name = "cameraType")
    public void setCameraType(FrameLayout view, @Nullable int cameraType) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setCameraView(cameraType);
    }
    @ReactProp(name = "touchToFocus", defaultBoolean = false)
    public void setTouchToFocus(FrameLayout view, @Nullable Boolean touchToFocus) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setTapToFocus(touchToFocus);
    }
    @ReactProp(name = "dataset", defaultBoolean = true)
    public void setDataset(FrameLayout view, @Nullable Boolean enable) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setDataset(enable);
    }
    @ReactProp(name = "model")
    public void setModelDetection(FrameLayout view, @Nullable int model) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setModelDetection(model);
    }
    @ReactProp(name = "distance")
    public void setDistance(FrameLayout view, @Nullable int confidence) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setConfidence(confidence);
    }
    @ReactProp(name = "rotateMode")
    public void setRotateMode(FrameLayout view, @Nullable int rotateMode) {
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.setRotateMode(rotateMode == CameraSettings.CameraRotateModeOn);

    }


    @Override
    public void onDropViewInstance(FrameLayout view) {
        super.onDropViewInstance(view);
        CameraModel camera = (CameraModel) view.findViewById(R.id.camera_view);
        camera.disableView();
    }
    @Override
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                "trainCompleted",
                MapBuilder.of("registrationName", "onTrained"),
                "TrainUncompleted",
                MapBuilder.of("registrationName", "onUntrained"),
                "recognizeCompleted",
                MapBuilder.of("registrationName", "onRecognized"),
                "recognizeUncompleted",
                MapBuilder.of("registrationName", "onUnrecognized")
        );
    }


    @Override
    public void onHostResume() {
        Log.d(TAG, "onHostResume");
        if (layoutRef == null) return;
        ViewGroup layout = layoutRef.get();
        if (layout == null) return;
        registerReceiver(layout.getContext());
        CameraModel camera = (CameraModel) layout.findViewById(R.id.camera_view);
        camera.onResume();
    }

    @Override
    public void onHostPause() {
        Log.d(TAG, "onHostPause");
        if (layoutRef == null) return;
        ViewGroup layout = layoutRef.get();
        if (layout == null) return;
        unregisterReceiver(layout.getContext());
        CameraModel camera = (CameraModel) layout.findViewById(R.id.camera_view);
        camera.disableView();
    }

    @Override
    public void onHostDestroy() {
        Log.d(TAG, "onHostDestroy");
        if (layoutRef == null) return;
        ViewGroup layout = layoutRef.get();
        if (layout == null) return;
        unregisterReceiver(layout.getContext());
    }

    private BroadcastReceiver createOrientationReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(BCAST_CONFIGCHANGED)) return;
                Log.d(TAG, "onReceive() CONFIGURATION_CHANGED");
                if (layoutRef == null) return;
                ViewGroup layout = layoutRef.get();
                if (layout == null) return;
                CameraModel camera = (CameraModel) layout.findViewById(R.id.camera_view);
                camera.disableView();
                camera.onResume();
            }
        };
    }
    private void registerReceiver(Context context) {
        Log.d(TAG, "registerReceiver()");
        final Activity activity = Resources.scanForActivity(context);
        if (activity != null) {
            activity.registerReceiver(receiver, new IntentFilter(BCAST_CONFIGCHANGED));
        }
    }

    private void unregisterReceiver(Context context) {
        final Activity activity = Resources.scanForActivity(context);
        if (activity == null) return;
        try {
            activity.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "receiver already unregistered");
        }
    }
}
