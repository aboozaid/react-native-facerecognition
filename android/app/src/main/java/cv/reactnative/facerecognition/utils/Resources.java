package cv.reactnative.facerecognition.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import com.facebook.react.uimanager.ThemedReactContext;

public class Resources {
    public static Activity scanForActivity(Context viewContext) {
        if (viewContext == null)
            return null;
        else if (viewContext instanceof Activity)
            return (Activity) viewContext;
        else if (viewContext instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) viewContext).getBaseContext());
        else if (viewContext instanceof ThemedReactContext)
            return ((ThemedReactContext) viewContext).getCurrentActivity();
        return null;
    }
}
