package org.apache.cordova.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service to keep app alive for FCM message processing
 * This ensures onMessageReceived is called even when app is in background
 */
public class FirebaseForegroundService extends Service {
    
    private static final String TAG = "FirebasePlugin";
    private static final String CHANNEL_ID = "firebase_fcm_service";
    private static final int NOTIFICATION_ID = 999999;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FirebaseForegroundService: onCreate");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Firebase Service",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Background service for push notifications");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        
        // Create minimal invisible notification (required for foreground service)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.screen_background_dark_transparent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(false)
            .setSound(null)
            .setVibrate(null)
            .setSilent(true)
            .build();
            
        startForeground(NOTIFICATION_ID, notification);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FirebaseForegroundService: onStartCommand");
        
        // Auto-stop after 3 seconds to minimize notification visibility
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "FirebaseForegroundService: Auto-stopping after 3 seconds");
                stopSelf();
            }
        }, 3000);
        
        // Check if this is a request to launch the app for action button
        if (intent != null && "LAUNCH_APP_FOR_ACTION".equals(intent.getAction())) {
            Log.d(TAG, "FirebaseForegroundService: Launch app request received");
            launchMainActivity(intent.getExtras());
        }
        
        return START_NOT_STICKY; // Don't restart if killed
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "FirebaseForegroundService: onTaskRemoved - restarting service");
        
        // Restart the service when app is swiped away from recent apps
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartServiceIntent);
        } else {
            getApplicationContext().startService(restartServiceIntent);
        }
        
        super.onTaskRemoved(rootIntent);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * Launches the main activity from the foreground service
     * This works even when the app is killed because the service is still running
     */
    private void launchMainActivity(Bundle extras) {
        try {
            String packageName = getPackageName();
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            
            if (launchIntent == null) {
                Log.e(TAG, "FirebaseForegroundService: Cannot get launch intent for package: " + packageName);
                return;
            }
            
            // Important flags for launching from background service:
            // FLAG_ACTIVITY_NEW_TASK - Required when starting activity from Service
            // FLAG_ACTIVITY_CLEAR_TOP - Clear all activities on top
            // FLAG_ACTIVITY_SINGLE_TOP - Reuse existing activity if available
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | 
                Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            
            if (extras != null) {
                launchIntent.putExtras(extras);
                Log.d(TAG, "FirebaseForegroundService: Added extras to launch intent");
            }
            
            // Start activity directly from foreground service
            // We have the necessary privileges because we're running as a foreground service
            // with the notification service allowlist
            Log.d(TAG, "FirebaseForegroundService: Starting activity to bring app to foreground");
            startActivity(launchIntent);
            Log.d(TAG, "FirebaseForegroundService: Activity started successfully");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseForegroundService: Failed to launch main activity: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FirebaseForegroundService: onDestroy");
    }
}
