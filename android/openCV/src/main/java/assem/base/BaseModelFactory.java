package assem.base;

import android.content.Context;

import assem.base.facerecognition.FaceModel;

public class BaseModelFactory {
    private Context context;

    public BaseModelFactory(Context context) {
        this.context = context;
    }

    public Factory getModel(String modelType){
        if(modelType.equalsIgnoreCase("FACEMODEL")){
            return new FaceModel(context, 0);
        }
        return null;
    }
}
