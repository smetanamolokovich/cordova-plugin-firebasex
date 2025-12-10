package org.apache.cordova.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Restarts FirebaseForegroundService on boot or when killed
 * DISABLED - not needed, FirebaseMessagingService works without it
 */
public class FirebaseServiceRestarter extends BroadcastReceiver {
    
    private static final String TAG = "FirebasePlugin";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "FirebaseServiceRestarter: onReceive - action=" + action);
        
        // Foreground service disabled to avoid "App is running" notification
        // FirebaseMessagingService works without it
        /*
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "FirebaseServiceRestarter: Starting FirebaseForegroundService");
            
            Intent serviceIntent = new Intent(context, FirebaseForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
        */
    }
}
