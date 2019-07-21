package assem.base;

import android.content.Context;

public interface CameraModel {

    void onResume();

    void setTorchMode(boolean enabled);

    void setTapToFocus(boolean enabled);

    void setCameraView(int camera);

    void setQuality(int quality);

    void setAspect(int aspect);

    void setRotateMode(boolean isLandscape);

    void disableView();


}
