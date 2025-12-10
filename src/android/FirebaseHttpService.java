package org.apache.cordova.firebase;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Temporary service to send HTTP request when app is killed
 * Starts, sends request, and immediately stops
 */
public class FirebaseHttpService extends Service {
    
    private static final String TAG = "FirebasePlugin";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getExtras() == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final Bundle bundle = intent.getExtras();
        
        // Send HTTP request in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendHttpRequest(bundle);
                // Stop service immediately after request
                stopSelf();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void sendHttpRequest(Bundle bundle) {
        HttpURLConnection connection = null;
        try {
            String apiUrl = bundle.getString("apiUrl");
            String authToken = bundle.getString("authToken");
            
            if (apiUrl == null || authToken == null) {
                Log.w(TAG, "FirebaseHttpService: Missing apiUrl or authToken");
                return;
            }

            Log.d(TAG, "FirebaseHttpService: Sending HTTP request to " + apiUrl);
            
            // Build message data
            JSONObject messageData = new JSONObject();
            messageData.put("sender_id", bundle.getString("recipientId")); // recipient becomes sender
            messageData.put("recipient_id", bundle.getString("senderId")); // sender becomes recipient
            messageData.put("commodity_id", bundle.getString("commodityId"));
            messageData.put("offer_id", bundle.getString("offerId"));
            messageData.put("text", bundle.getString("replyText", ""));
            messageData.put("notificationTitle", "Reply from notification");
            
            JSONObject payload = new JSONObject();
            payload.put("message", messageData);
            
            Log.d(TAG, "FirebaseHttpService: Request payload: " + payload.toString());
            
            // Setup HTTP connection
            URL url = new URL(apiUrl + "api/messages");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Auth-Token", authToken);
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            // Send request
            OutputStream os = connection.getOutputStream();
            os.write(payload.toString().getBytes("UTF-8"));
            os.flush();
            os.close();
            
            // Get response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "FirebaseHttpService: HTTP response code: " + responseCode);
            
            if (responseCode >= 200 && responseCode < 300) {
                // Read response body
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                Log.d(TAG, "FirebaseHttpService: Response: " + response.toString());
                Log.d(TAG, "FirebaseHttpService: Message sent successfully");
                showToast("Reply sent");
            } else {
                Log.e(TAG, "FirebaseHttpService: Server returned error: " + responseCode);
                showToast("Failed to send reply");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "FirebaseHttpService: HTTP request failed", e);
            showToast("Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void showToast(final String message) {
        android.os.Handler mainHandler = new android.os.Handler(getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FirebaseHttpService.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
