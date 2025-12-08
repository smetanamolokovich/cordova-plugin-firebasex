package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.RemoteInput;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * BroadcastReceiver that handles notification action button clicks.
 * Compatible with Android 13+ and cordova-android 14.
 * Supports inline reply actions.
 */
public class FirebaseActionReceiver extends BroadcastReceiver {

    private static final String TAG = "FirebasePlugin";
    public static final String ACTION_CLICK = "org.apache.cordova.firebase.ACTION_CLICK";
    public static final String KEY_TEXT_REPLY = "key_text_reply";

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

            // Extract inline reply text if present
            CharSequence replyText = null;
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                replyText = remoteInput.getCharSequence(KEY_TEXT_REPLY);
            }

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
            
            // Add reply text if present
            if (replyText != null) {
                resultBundle.putString("replyText", replyText.toString());
                Log.d(TAG, "FirebaseActionReceiver: Reply text=" + replyText);
            }

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
     * When app is in background, message will be queued and delivered when app opens
     */
    private void sendActionToJavaScript(Bundle bundle, Context context) {
        try {
            // Use the existing sendMessage method which handles both foreground and background
            // In background: adds to notificationStack and delivers when app opens
            // In foreground: delivers immediately to JavaScript
            FirebasePlugin.sendMessage(bundle, context);
            Log.d(TAG, "FirebaseActionReceiver: Action sent to JavaScript");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseActionReceiver: Error sending action to JavaScript", e);
        }
    }
}
