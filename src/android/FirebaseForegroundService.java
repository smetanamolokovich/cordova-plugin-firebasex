package org.apache.cordova.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
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
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps app alive for push notifications");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App is running")
            .setContentText("Ready to receive notifications")
            .setSmallIcon(getApplicationInfo().icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
            
        startForeground(NOTIFICATION_ID, notification);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FirebaseForegroundService: onStartCommand");
        return START_STICKY; // Restart service if killed
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FirebaseForegroundService: onDestroy");
    }
}
