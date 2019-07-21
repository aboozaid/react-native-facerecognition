package assem.base.facerecognition;

import android.content.Context;

import assem.base.BaseCameraView;
import assem.base.CameraCallbacks;
import assem.base.Factory;
import assem.base.CameraModel;


public class FaceModel extends BaseCameraView implements CameraModel, Factory {
    private static String TAG = "FaceCameraManage";

    public FaceModel(Context context, int cameraId) {
        super(context, cameraId);
    }

    @Override
    public void getModel(CameraCallbacks callback){
        this.callback = callback;
    }

    @Override
    public void onResume() {
        if (getContext() == null) return;
        loadOpenCV();
    }

    @Override
    public void setTorchMode(boolean enabled) {
        this.torchEnabled = enabled;
        super.setFlashMode();
    }

    @Override
    public void setTapToFocus(boolean tapToFocusEnabled) {
        this.tapToFocusEnabled = tapToFocusEnabled;
    }


}
