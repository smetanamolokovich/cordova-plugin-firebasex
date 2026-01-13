package org.apache.cordova.firebase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class OnNotificationReceiverActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(FirebasePlugin.TAG, "OnNotificationReceiverActivity.onCreate()");
        handleNotification(this, getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(FirebasePlugin.TAG, "OnNotificationReceiverActivity.onNewIntent()");
        handleNotification(this, intent);
        finish();
    }

    private static void handleNotification(Context context, Intent intent) {
        try{
            Bundle data = intent.getExtras();
            if(!data.containsKey("messageType")) data.putString("messageType", "notification");
            
            // Dismiss the notification if notificationId is present
            int notificationId = data.getInt("notificationId", -1);
            if (notificationId != -1) {
                android.app.NotificationManager notificationManager = 
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(notificationId);
                    Log.d(FirebasePlugin.TAG, "OnNotificationReceiverActivity: Dismissed notification " + notificationId);
                }
            }
            
            // Check if this is an action button click
            String action = data.getString("action");
            if (action != null && !action.isEmpty()) {
                // This is an action button click
                data.putString("tap", "action");
                Log.d(FirebasePlugin.TAG, "OnNotificationReceiverActivity: Action button clicked - " + action);
            } else {
                // Regular notification tap
                data.putString("tap", FirebasePlugin.inBackground() ? "background" : "foreground");
            }

            Log.d(FirebasePlugin.TAG, "OnNotificationReceiverActivity.handleNotification(): "+data.toString());

            // Send message to JavaScript with immediate delivery for action events
            if (action != null && !action.isEmpty()) {
                data.putBoolean("_isActionEvent", true);
            }
            FirebasePlugin.sendMessage(data, context);

            // Launch main activity to bring app to foreground
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(context.getPackageName());
            // Use SINGLE_TOP to reuse existing activity instead of creating new one
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launchIntent.putExtras(data);
            context.startActivity(launchIntent);
        }catch (Exception e){
            FirebasePlugin.handleExceptionWithoutContext(e);
        }
    }
}