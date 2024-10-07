package by.chemerisuk.cordova.firebase;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import by.chemerisuk.cordova.support.CordovaMethod;
import by.chemerisuk.cordova.support.ReflectiveCordovaPlugin;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Scanner;

public class FirebaseAnalyticsPlugin extends ReflectiveCordovaPlugin {
    private static final String TAG = "FirebaseAnalyticsPlugin";

    private FirebaseAnalytics firebaseAnalytics;
    private CallbackContext callback;

    @Override
    protected void pluginInitialize() {
        Log.d(TAG, "Starting Firebase Analytics plugin");
        Context context = this.cordova.getActivity().getApplicationContext();
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    @CordovaMethod
    protected void logEvent(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        String name = args.getString(0);
        JSONObject params = args.getJSONObject(1);
        firebaseAnalytics.logEvent(name, parse(params));
        callbackContext.success();
    }

    @CordovaMethod
    protected void setUserId(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        String userId = args.getString(0);
        firebaseAnalytics.setUserId(userId);
        callbackContext.success();
    }

    @CordovaMethod
    protected void setUserProperty(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        String name = args.getString(0);
        String value = args.getString(1);
        firebaseAnalytics.setUserProperty(name, value);
        callbackContext.success();
    }

    @CordovaMethod
    protected void resetAnalyticsData(CordovaArgs args, CallbackContext callbackContext) {
        firebaseAnalytics.resetAnalyticsData();
        callbackContext.success();
    }

    @CordovaMethod
    protected void setEnabled(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        boolean enabled = args.getBoolean(0);
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
        callbackContext.success();
    }

    @CordovaMethod
    protected void setCurrentScreen(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        String screenName = args.getString(0);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        callbackContext.success();
    }

    @CordovaMethod
    protected void writeFCMToken(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        getToken();
        return true;
    }

    @CordovaMethod
    protected void getFCMToken(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        readFCMToken();
        return true;
    }

    @CordovaMethod
    protected void setDefaultEventParameters(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        JSONObject params = args.getJSONObject(0);
        firebaseAnalytics.setDefaultEventParameters(parse(params));
        callbackContext.success();
    }

    public void readFCMToken () {
        String str = "";
        try {
            File filePath = new File(FirebaseUtils.rootDirectory + "/FCMToken" + ".txt");
            if (filePath.exists()) {
                String content = new Scanner(filePath).useDelimiter("\\A").next();
                System.out.println("Readed  token value is: " + content);
                callback.success(content);
            } else {
                callback.error("There is no file at the given path..");
            }
        } catch (Exception e) {
            callback.error(e.getMessage());
        }
    }
    public void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }
                // Get new FCM registration token
                String token = task.getResult();
                System.out.println("token value is: " + token);
                File appDirectory;
                FileWriter fileWriterObj;
                try {
                    appDirectory = new File(FirebaseUtils.rootDirectory + "/FCMToken" + ".txt");
                    System.out.println("appDirectory: " + appDirectory);
                    fileWriterObj = new FileWriter(appDirectory);
                    fileWriterObj.write(token);
                    fileWriterObj.flush();
                    fileWriterObj.close();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("token", token);
                    callback.success(jsonObject);
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    private static Bundle parse(JSONObject params) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator<String> it = params.keys();

        while (it.hasNext()) {
            String key = it.next();
            Object value = params.get(key);

            if (value instanceof String) {
                bundle.putString(key, (String)value);
            } else if (value instanceof Integer) {
                bundle.putInt(key, (Integer)value);
            } else if (value instanceof Double) {
                bundle.putDouble(key, (Double)value);
            } else if (value instanceof Long) {
                bundle.putLong(key, (Long)value);
            } else if (value instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray)value;
                ArrayList<Bundle> items = new ArrayList<>();
                for (int i = 0, n = jsonArray.length(); i < n; i++) {
                    items.add(parse(jsonArray.getJSONObject(i)));
                }
                bundle.putParcelableArrayList(key, items);
            } else {
                Log.w(TAG, "Value for key " + key + " is not supported");
            }
        }

        return bundle;
    }
}
