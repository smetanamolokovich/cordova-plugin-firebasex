package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * BroadcastReceiver that handles notification action button clicks.
 * Compatible with Android 13+ and cordova-android 14.
 */
public class FirebaseActionReceiver extends BroadcastReceiver {

    private static final String TAG = "FirebasePlugin";
    public static final String ACTION_CLICK = "org.apache.cordova.firebase.ACTION_CLICK";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(TAG, "FirebaseActionReceiver: onReceive");

            if (intent == null || intent.getExtras() == null) {
                Log.w(TAG, "FirebaseActionReceiver: Intent or extras is null");
                return;
            }

            Bundle extras = intent.getExtras();
            String action = extras.getString("action");
            int notificationId = extras.getInt("notificationId", -1);

            if (action == null) {
                Log.w(TAG, "FirebaseActionReceiver: Action is null");
                return;
            }

            Log.d(TAG, "FirebaseActionReceiver: Action=" + action + ", NotificationId=" + notificationId);

            // Dismiss the notification
            if (notificationId != -1) {
                NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(notificationId);
                }
            }

            // Build the result bundle to send to JavaScript
            Bundle resultBundle = new Bundle();
            resultBundle.putString("action", action);
            resultBundle.putString("tap", "action");

            // Copy all original notification data
            for (String key : extras.keySet()) {
                if (!key.equals("action") && !key.equals("notificationId")) {
                    Object value = extras.get(key);
                    if (value instanceof String) {
                        resultBundle.putString(key, (String) value);
                    }
                }
            }

            // Send the action result to JavaScript
            sendActionToJavaScript(resultBundle, context);

        } catch (Exception e) {
            Log.e(TAG, "FirebaseActionReceiver: Error handling action", e);
            FirebasePlugin.handleExceptionWithoutContext(e);
        }
    }

    /**
     * Sends the action button result to JavaScript via FirebasePlugin.sendMessage
     */
    private void sendActionToJavaScript(Bundle bundle, Context context) {
        try {
            // Use the existing sendMessage method to deliver the action to JavaScript
            FirebasePlugin.sendMessage(bundle, context);
            Log.d(TAG, "FirebaseActionReceiver: Action sent to JavaScript");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseActionReceiver: Error sending action to JavaScript", e);
        }
    }
}
