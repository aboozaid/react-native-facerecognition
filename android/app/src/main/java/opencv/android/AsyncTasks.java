package opencv.android;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Assem Abozaid on 8/2/2018.
 */

public class AsyncTasks {
    static String TAG = "AsyncTasks";

    static class loadFiles extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private final Callback callback;
        private String fileName;


        interface Callback {
            void onFileLoadedComplete(boolean result);
        }

        loadFiles(Context context, String file, Callback callback) {
            this.context = context;
            this.fileName = file;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... strings) {
            if(context != null) {
                InputStream inp = null;
                OutputStream out = null;
                try {
                    inp = context.getResources().getAssets().open(fileName);
                    File outFile = new File(context.getCacheDir(), fileName);
                    out = new FileOutputStream(outFile);

                    byte[] buffer = new byte[4096];
                    int bytesread;
                    while ((bytesread = inp.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesread);
                    }

                    inp.close();
                    out.flush();
                    out.close();
                    return true;
                } catch (IOException e) {
                    Log.i(TAG, "Unable to load cascade file" + e);
                }
            }
            Log.d(TAG, "seems that context is null");
            return false;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if(result)
                callback.onFileLoadedComplete(result);
            else
                Log.d(TAG, "Can't load the file");
        }
    }
}
