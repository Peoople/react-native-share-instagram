package com.reactnative.share;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import android.content.pm.ResolveInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ActivityNotFoundException;
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
  private Callback successCallback;

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
    if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      return true;
    }
    return false;
  }

  private File saveImage(final Context context, String fileName, final String imageData) {
    final byte[] imgBytesData = Base64.decode(imageData, Base64.DEFAULT);
    final File file;

    if (isExternalStorageWritable() == false || isExternalStorageReadable() == false) {
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
  final int TWITTER_SHARE_REQUEST = 400;
  final int INSTAGRAM_SHARE_REQUEST_WITH_CHOOSER = 501;

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
        successCallback.invoke("Image shared successfully with instagram. RESULT CODE -> " + resultCode);
      }
      if (requestCode == INSTAGRAM_SHARE_REQUEST_WITH_CHOOSER) {
        successCallback.invoke("Image shared successfully with instagram w chooser. RESULT CODE -> " + resultCode);
      }
      if (requestCode == TWITTER_SHARE_REQUEST) {
        successCallback.invoke("Image shared successfully with twitter. RESULT CODE -> " + resultCode);
      }
    }
  }

  @Override
  public String getName() {
    return "RNReactNativeSharingWinstagram";
  }

  @ReactMethod
  public void hasTwitterInstalled(Callback callback) {
    boolean equal = isAppInstalled("com.instagram.android");
    callback.invoke(equal);
  }

  @ReactMethod
  public void shareWithTwitter(String message, Callback successCallback, Callback failureCallback) {
    Activity currentActivity = getCurrentActivity();
    this.successCallback = successCallback;

    if (isAppInstalled("com.twitter.android") == false) {
      failureCallback.invoke("Sorry, twitter is not installed in your device.");
    } else {
      try {

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setPackage("com.twitter.android");
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);

        ResolveInfo canResolve = currentActivity.getPackageManager().resolveActivity(intent, TWITTER_SHARE_REQUEST);
        if (canResolve != null) {
          currentActivity.startActivityForResult(intent, TWITTER_SHARE_REQUEST);
        } else {
          failureCallback.invoke("The image could not be shared.");
        }
      } catch (ActivityNotFoundException ex) {
        failureCallback.invoke(ex.getMessage());
      }

    }
  }

  @ReactMethod
  public void shareWithInstagram(String fileName, String base64str, String mode, Callback successCallback,
      Callback failureCallback) {
    Activity currentActivity = getCurrentActivity();
    this.successCallback = successCallback;

    String type = "image/*";

    File media = saveImage(getReactApplicationContext(), fileName, base64str);

    if (isAppInstalled("com.instagram.android") == false) {
      failureCallback.invoke("Sorry, instagram is not installed in your device.");
    } else {
      if (media.exists()) {
        try {
          Intent share = new Intent("com.instagram.share." + mode);

          StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
          StrictMode.setVmPolicy(builder.build());
          Uri uri = Uri.fromFile(media);
          share.setDataAndType(uri, type);
          share.putExtra(Intent.EXTRA_STREAM, uri);
          ResolveInfo canResolve = currentActivity.getPackageManager().resolveActivity(share, INSTAGRAM_SHARE_REQUEST);
          if (canResolve != null) {
            currentActivity.startActivityForResult(share, INSTAGRAM_SHARE_REQUEST);
          } else {
            Intent regShare = new Intent(Intent.ACTION_SEND);
            regShare.setDataAndType(uri, type);
            regShare.putExtra(Intent.EXTRA_STREAM, uri);
            regShare.setPackage("com.instagram.android");
            currentActivity.startActivityForResult(Intent.createChooser(regShare, "Compartir usando..."),
                INSTAGRAM_SHARE_REQUEST_WITH_CHOOSER);
          }
        } catch (ActivityNotFoundException ex) {
          failureCallback.invoke(ex.getMessage());
        }
      } else {
        failureCallback.invoke("Sorry, image could not be loaded from disk.");
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
