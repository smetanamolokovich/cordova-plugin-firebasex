package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.RemoteInput;
import androidx.core.app.NotificationCompat;

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

            // Send the action result to JavaScript - force immediate delivery
            sendActionToJavaScript(resultBundle, context);
            
            // Open the app only for actions that require it (not reply, mark_read or dismiss)
            if (!action.equals("reply") && !action.equals("mark_read") && !action.equals("dismiss")) {
                Log.d(TAG, "FirebaseActionReceiver: Launching main activity");
                
                try {
                    // Get the launch intent for the app
                    String packageName = context.getPackageName();
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                    
                    if (launchIntent != null) {
                        // Critical: Add FLAG_ACTIVITY_NEW_TASK to allow starting from non-Activity context
                        // The notification click gives us a temporary allowlist to start activities
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        launchIntent.putExtras(resultBundle);
                        
                        // Start the activity directly - we have allowlist from notification click
                        context.startActivity(launchIntent);
                        Log.d(TAG, "FirebaseActionReceiver: Activity started");
                    } else {
                        Log.e(TAG, "FirebaseActionReceiver: Cannot get launch intent");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "FirebaseActionReceiver: Failed to launch activity", e);
                }
            } else {
                Log.d(TAG, "FirebaseActionReceiver: " + action + " action - not opening app");
            }
            
            // If app is killed (no callback registered), send via HTTP service
            if (!FirebasePlugin.hasNotificationsCallback()) {
                Log.d(TAG, "FirebaseActionReceiver: No callback registered, starting HTTP service");
                
                String apiUrl = resultBundle.getString("apiUrl");
                String authToken = resultBundle.getString("authToken");
                
                if (apiUrl != null && authToken != null) {
                    Intent serviceIntent = new Intent(context, FirebaseHttpService.class);
                    serviceIntent.putExtras(resultBundle);
                    context.startService(serviceIntent);
                } else {
                    Log.w(TAG, "FirebaseActionReceiver: Missing apiUrl or authToken, cannot send HTTP request");
                }
            }

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
            Log.d(TAG, "FirebaseActionReceiver: Calling FirebasePlugin.sendMessage()");
            Log.d(TAG, "FirebaseActionReceiver: Bundle keys: " + bundle.keySet());
            
            // Mark this as an action event that should be delivered immediately
            bundle.putBoolean("_isActionEvent", true);
            
            // Use the existing sendMessage method which handles both foreground and background
            // Action events are delivered immediately even when app is in background
            FirebasePlugin.sendMessage(bundle, context);
            
            Log.d(TAG, "FirebaseActionReceiver: Action sent to JavaScript");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseActionReceiver: Error sending action to JavaScript", e);
        }
    }
    
    /**
     * Shows a notification to open the app when it's killed
     * This is needed because Android doesn't allow starting Activity from BroadcastReceiver when app is dead
     */
    private void showOpenAppNotification(Context context, Bundle bundle) {
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launchIntent == null) {
                Log.e(TAG, "FirebaseActionReceiver: Cannot get launch intent");
                return;
            }
            
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchIntent.putExtras(bundle);
            
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );
            
            String action = bundle.getString("action", "action");
            String replyText = bundle.getString("replyText");
            String title = "Action processed";
            String body = replyText != null ? "Reply: " + replyText : "Tap to open app";
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "fcm_default_channel")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(context.getApplicationInfo().icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
            
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.notify(999998, builder.build());
                Log.d(TAG, "FirebaseActionReceiver: Open app notification shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "FirebaseActionReceiver: Error showing open app notification", e);
        }
    }
}

