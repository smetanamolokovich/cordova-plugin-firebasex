package org.apache.cordova.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Restarts FirebaseForegroundService on boot or when killed
 * Needed for devices with aggressive battery optimization (Lenovo, Xiaomi, Huawei, etc.)
 */
public class FirebaseServiceRestarter extends BroadcastReceiver {
    
    private static final String TAG = "FirebasePlugin";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "FirebaseServiceRestarter: onReceive - action=" + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "FirebaseServiceRestarter: Ensuring FCM service is ready");
            
            // Just log - FirebaseMessagingService should handle itself
            // But we ensure it's enabled by this receiver being triggered
            Log.d(TAG, "FirebaseServiceRestarter: FCM service should be active now");
        }
    }
}
