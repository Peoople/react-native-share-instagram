package com.reactlibrary;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Base64;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.os.Environment;
import android.os.StrictMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import android.content.pm.PackageManager;

public class RNReactNativeSharingWinstagramModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private final ReactApplicationContext reactContext;
    private Callback callback;

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private File saveImage(final Context context, String fileName, final String imageData) {
        final byte[] imgBytesData = Base64.decode(imageData, Base64.DEFAULT);
        final File file;

        if(isExternalStorageWritable() == false || isExternalStorageReadable() == false) {
          return null;
        }

        file = new File(context.getExternalFilesDir(null), fileName);

        try {
          FileOutputStream fop = new FileOutputStream(file);

    			// if file doesn't exists, then create it
          try {
            if (!file.exists()) {
              file.createNewFile();
            }

            fop.write(imgBytesData);
            fop.flush();
            fop.close();
          } catch (IOException e) {
            e.printStackTrace();
            return null;
          }

    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
          return null;
    		}
        return file;
    }

    final int INSTAGRAM_SHARE_REQUEST = 500;

    public RNReactNativeSharingWinstagramModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(new RNInstagramShareActivityEventListener());
    }

    private class RNInstagramShareActivityEventListener extends BaseActivityEventListener {
        @Override
        public void onActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent intent) {
            Log.d("------------>resultCode", "" + resultCode);
            if (requestCode == INSTAGRAM_SHARE_REQUEST) {
                callback.invoke("Image shared successfully with instagram.");
            }
        }
    }

    @Override
    public String getName() {
      return "RNReactNativeSharingWinstagram";
    }

     @ReactMethod
    public void shareWithTwitter(String fileName, String base64str, String message, Callback callback, Callback secondCallback) {
       Activity currentActivity = getCurrentActivity();
       this.callback = callback;

       String type = "image/jpeg";

       File media = saveImage(getReactApplicationContext(), fileName, base64str);

         if(isAppInstalled("com.twitter.android") == false) {
           callback.invoke("Sorry, twitter is not installed in your device.");
         } else {
           if(media.exists()) {
               
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            Uri uri = Uri.fromFile(media);
           
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, message);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("image/jpeg");
            intent.setPackage("com.twitter.android");

             // Broadcast the Intent.
            currentActivity.startActivityForResult(intent, INSTAGRAM_SHARE_REQUEST);
          } else {
             callback.invoke("Sorry, image could not be loaded from disk.");
          }
       }
    }

    @ReactMethod
    public void shareWithInstagram(String fileName, String base64str, String mode, Callback callback, Callback secondCallback) {
       Activity currentActivity = getCurrentActivity();
       this.callback = callback;

       String type = "image/jpeg";

       File media = saveImage(getReactApplicationContext(), fileName, base64str);

         if(isAppInstalled("com.instagram.android") == false) {
           callback.invoke("Sorry, instagram is not installed in your device.");
         } else {
           if(media.exists()) {
             // Create the new Intent using the 'Send' action.
             Intent share = new Intent("com.instagram.share."+mode);

             // Set the MIME type
             share.setType(type);
             share.setPackage("com.instagram.android");

             StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
             StrictMode.setVmPolicy(builder.build());

             Uri uri = Uri.fromFile(media);

             share.setDataAndType(uri, type);

             // Add the URI to the Intent.
             share.putExtra(Intent.EXTRA_STREAM, uri);

             // Broadcast the Intent.
             currentActivity.startActivityForResult(share, INSTAGRAM_SHARE_REQUEST);
          } else {
             callback.invoke("Sorry, image could not be loaded from disk.");
          }
       }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @ReactMethod
    public void hasInstagramInstalled(Callback callback) {
        boolean equal = isAppInstalled("com.instagram.android");
        callback.invoke(equal);
    }

    private boolean isAppInstalled(String packageName) {
        Activity currentActivity = getCurrentActivity();
        PackageManager pm = currentActivity.getPackageManager();
        boolean installed = false;
        try {
           pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
           installed = true;
        } catch (PackageManager.NameNotFoundException e) {
           installed = false;
        }
        return installed;
    }

}
