package org.apache.cordova.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import android.util.Log;
import android.app.Notification;
import android.text.TextUtils;
import android.text.Html;
import android.text.Spanned;
import android.content.ContentResolver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Canvas;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";

    static final String defaultSmallIconName = "ic_notification";
    static final String defaultLargeIconName = "ic_notification_large";

    static final String imageTypeCircle = "circle";
    static final String imageTypeBigPicture = "big_picture";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String refreshedToken) {
        try{
            super.onNewToken(refreshedToken);
            Log.d(TAG, "Refreshed token: " + refreshedToken);
            FirebasePlugin.sendToken(refreshedToken);
        }catch (Exception e){
            FirebasePlugin.handleExceptionWithoutContext(e);
        }
    }

    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Called when message is received.
     * Called IF message is a data message (i.e. NOT sent from Firebase console)
     * OR if message is a notification message (e.g. sent from Firebase console) AND app is in foreground.
     * Notification messages received while app is in background will not be processed by this method;
     * they are handled internally by the OS.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try{
            // [START_EXCLUDE]
            // There are two types of messages data messages and notification messages. Data messages are handled
            // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
            // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
            // is in the foreground. When the app is in the background an automatically generated notification is displayed.
            // When the user taps on the notification they are returned to the app. Messages containing both notification
            // and data payloads are treated as notification messages. The Firebase console always sends notification
            // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
            // [END_EXCLUDE]

            // Pass the message to the receiver manager so any registered receivers can decide to handle it
            boolean wasHandled = FirebasePluginMessageReceiverManager.onMessageReceived(remoteMessage);
            if (wasHandled) {
                Log.d(TAG, "Message was handled by a registered receiver");

                // Don't process the message in this method.
                return;
            }

            if(FirebasePlugin.applicationContext == null){
                FirebasePlugin.applicationContext = this.getApplicationContext();
            }

            // TODO(developer): Handle FCM messages here.
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            String messageType;
            String title = null;
            String titleLocKey = null;
            String[] titleLocArgs = null;
            String body = null;
            String bodyLocKey = null;
            String[] bodyLocArgs = null;
            String bodyHtml = null;
            String id = null;
            String sound = null;
            String vibrate = null;
            String light = null;
            String color = null;
            String icon = null;
            String channelId = null;
            String visibility = null;
            String priority = null;
            String image = null;
            String imageType = null;
            boolean foregroundNotification = false;

            Map<String, String> data = remoteMessage.getData();
            
            // Check if message contains action buttons - if so, we need to handle it as data message
            // even if notification block is present, so we can create custom notification with actions
            boolean hasActions = data != null && data.containsKey("actions");

            if (remoteMessage.getNotification() != null && !hasActions) {
                // Notification message payload (only if no actions)
                Log.i(TAG, "Received message: notification");
                messageType = "notification";
                id = remoteMessage.getMessageId();
                RemoteMessage.Notification notification = remoteMessage.getNotification();
                title = notification.getTitle();
                titleLocKey = notification.getTitleLocalizationKey();
                titleLocArgs = notification.getTitleLocalizationArgs();
                body = notification.getBody();
                bodyLocKey = notification.getBodyLocalizationKey();
                bodyLocArgs = notification.getBodyLocalizationArgs();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    channelId = notification.getChannelId();
                }
                sound = notification.getSound();
                color = notification.getColor();
                icon = notification.getIcon();
                if (notification.getImageUrl() != null) {
                    image = notification.getImageUrl().toString();
                }
                if (!TextUtils.isEmpty(titleLocKey)) {
                    int titleId = getResources().getIdentifier(titleLocKey, "string", getPackageName());
                    title = String.format(getResources().getString(titleId), (Object[])titleLocArgs);
                }
                if (!TextUtils.isEmpty(bodyLocKey)) {
                    int bodyId = getResources().getIdentifier(bodyLocKey, "string", getPackageName());
                    body = String.format(getResources().getString(bodyId), (Object[])bodyLocArgs);
                }
            }else{
                Log.i(TAG, "Received message: data" + (hasActions ? " (with actions)" : ""));
                messageType = "data";
            }

            if (data != null) {
                // Data message payload
                if(data.containsKey("notification_foreground")){
                    foregroundNotification = true;
                }
                if(data.containsKey("notification_title")) title = data.get("notification_title");
                if(data.containsKey("notification_body")) body = data.get("notification_body");
                if(data.containsKey("notification_android_body_html")) bodyHtml = data.get("notification_android_body_html");
                if(data.containsKey("notification_android_channel_id")) channelId = data.get("notification_android_channel_id");
                if(data.containsKey("notification_android_id")) id = data.get("notification_android_id");
                if(data.containsKey("notification_android_sound")) sound = data.get("notification_android_sound");
                if(data.containsKey("notification_android_vibrate")) vibrate = data.get("notification_android_vibrate");
                if(data.containsKey("notification_android_light")) light = data.get("notification_android_light"); //String containing hex ARGB color, miliseconds on, miliseconds off, example: '#FFFF00FF,1000,3000'
                if(data.containsKey("notification_android_color")) color = data.get("notification_android_color");
                if(data.containsKey("notification_android_icon")) icon = data.get("notification_android_icon");
                if(data.containsKey("notification_android_visibility")) visibility = data.get("notification_android_visibility");
                if(data.containsKey("notification_android_priority")) priority = data.get("notification_android_priority");
                if(data.containsKey("notification_android_image")) image = data.get("notification_android_image");
                if(data.containsKey("notification_android_image_type")) imageType = data.get("notification_android_image_type");
            }

            if (TextUtils.isEmpty(id)) {
                Random rand = new Random();
                int n = rand.nextInt(50) + 1;
                id = Integer.toString(n);
            }

            Log.d(TAG, "From: " + remoteMessage.getFrom());
            Log.d(TAG, "Id: " + id);
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Body: " + body);
            Log.d(TAG, "Sound: " + sound);
            Log.d(TAG, "Vibrate: " + vibrate);
            Log.d(TAG, "Light: " + light);
            Log.d(TAG, "Color: " + color);
            Log.d(TAG, "Icon: " + icon);
            Log.d(TAG, "Channel Id: " + channelId);
            Log.d(TAG, "Visibility: " + visibility);
            Log.d(TAG, "Priority: " + priority);
            Log.d(TAG, "Image: " + image);
            Log.d(TAG, "image Type: " + imageType);


            if (!TextUtils.isEmpty(body) || !TextUtils.isEmpty(title) || (data != null && !data.isEmpty())) {
                // Parse action buttons from data payload
                List<NotificationAction> actions = parseActions(data);
                
                // Update hasActions based on parsed actions (not just presence of "actions" key)
                hasActions = actions != null && !actions.isEmpty();
                
                // Show notification if:
                // - App is in background OR
                // - No notification callback registered OR
                // - notification_foreground is set OR
                // - Message has action buttons (need to show buttons to user)
                boolean showNotification = (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback() || foregroundNotification || hasActions) && (!TextUtils.isEmpty(body) || !TextUtils.isEmpty(title));
                
                if (hasActions) {
                    Log.d(TAG, "Message has " + actions.size() + " action buttons");
                    
                    // Start foreground service temporarily to keep process alive for action button handling
                    // Service will auto-stop after 3 seconds to minimize notification visibility
                    if (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback()) {
                        try {
                            Intent serviceIntent = new Intent(this, FirebaseForegroundService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent);
                            } else {
                                startService(serviceIntent);
                            }
                            Log.d(TAG, "Started temporary foreground service for action button handling");
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to start foreground service", e);
                        }
                    }
                }
                
                sendMessage(remoteMessage, data, messageType, id, title, body, bodyHtml, showNotification, sound, vibrate, light, color, icon, channelId, priority, visibility, image, imageType, actions);
            }
        }catch (Exception e){
            FirebasePlugin.handleExceptionWithoutContext(e);
        }
    }

    private void sendMessage(RemoteMessage remoteMessage, Map<String, String> data, String messageType, String id, String title, String body, String bodyHtml, boolean showNotification, String sound, String vibrate, String light, String color, String icon, String channelId, String priority, String visibility, String image, String imageType, List<NotificationAction> actions) {
        Log.d(TAG, "sendMessage(): messageType="+messageType+"; showNotification="+showNotification+"; id="+id+"; title="+title+"; body="+body+"; sound="+sound+"; vibrate="+vibrate+"; light="+light+"; color="+color+"; icon="+icon+"; channel="+channelId+"; actions="+(actions != null ? actions.size() : 0)+"; data="+data.toString());
        Bundle bundle = new Bundle();
        for (String key : data.keySet()) {
            bundle.putString(key, data.get(key));
        }
        bundle.putString("messageType", messageType);
        this.putKVInBundle("id", id, bundle);
        this.putKVInBundle("title", title, bundle);
        this.putKVInBundle("body", body, bundle);
        this.putKVInBundle("body_html", bodyHtml, bundle);
        this.putKVInBundle("sound", sound, bundle);
        this.putKVInBundle("vibrate", vibrate, bundle);
        this.putKVInBundle("light", light, bundle);
        this.putKVInBundle("color", color, bundle);
        this.putKVInBundle("icon", icon, bundle);
        this.putKVInBundle("channel_id", channelId, bundle);
        this.putKVInBundle("priority", priority, bundle);
        this.putKVInBundle("visibility", visibility, bundle);
        this.putKVInBundle("image", image, bundle);
        this.putKVInBundle("image_type", imageType, bundle);
        this.putKVInBundle("show_notification", String.valueOf(showNotification), bundle);
        this.putKVInBundle("from", remoteMessage.getFrom(), bundle);
        this.putKVInBundle("collapse_key", remoteMessage.getCollapseKey(), bundle);
        this.putKVInBundle("sent_time", String.valueOf(remoteMessage.getSentTime()), bundle);
        this.putKVInBundle("ttl", String.valueOf(remoteMessage.getTtl()), bundle);
        
        // Store actions in bundle for JavaScript
        if (actions != null && !actions.isEmpty()) {
            try {
                JSONArray actionsJson = new JSONArray();
                for (NotificationAction action : actions) {
                    JSONObject actionJson = new JSONObject();
                    actionJson.put("id", action.id);
                    actionJson.put("title", action.title);
                    actionJson.put("icon", action.icon);
                    actionsJson.put(actionJson);
                }
                bundle.putString("actions", actionsJson.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Error serializing actions", e);
            }
        }

        if (showNotification) {

            Intent intent;
            PendingIntent pendingIntent;
            final int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;  // Only add on platform levels that support FLAG_MUTABLE

            if(getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intent = new Intent(this, OnNotificationReceiverActivity.class);
                intent.putExtras(bundle);
                pendingIntent = PendingIntent.getActivity(this, id.hashCode(), intent, flag);
            }else{
                intent = new Intent(this, OnNotificationOpenReceiver.class);
                intent.putExtras(bundle);
                pendingIntent = PendingIntent.getBroadcast(this, id.hashCode(), intent, flag);
            }


            // Channel
            if(channelId == null || !FirebasePlugin.channelExists(channelId)){
                channelId = FirebasePlugin.defaultChannelId;
            }
            // Fallback if app was killed and defaultChannelId not initialized
            if(channelId == null) {
                channelId = "fcm_default_channel";
                // Create default channel if it doesn't exist
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if(notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                        NotificationChannel channel = new NotificationChannel(
                            channelId,
                            "Push notifications",
                            NotificationManager.IMPORTANCE_HIGH
                        );
                        channel.setDescription("Notifications from server");
                        notificationManager.createNotificationChannel(channel);
                        Log.d(TAG, "Created fallback notification channel: " + channelId);
                    }
                }
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Log.d(TAG, "Channel ID: "+channelId);
            }


            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            notificationBuilder
                    .setContentTitle(title)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            if(bodyHtml != null) {
                notificationBuilder
                    .setContentText(fromHtml(body))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(fromHtml(body)));
            }else{
                notificationBuilder
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body));
            }


            // On Android O+ the sound/lights/vibration are determined by the channel ID
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                // Sound
                if (sound == null) {
                    Log.d(TAG, "Sound: none");
                }else if (sound.equals("default")) {
                    notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    Log.d(TAG, "Sound: default");
                }else{
                    Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/" + sound);
                    Log.d(TAG, "Sound: custom=" + sound+"; path="+soundPath.toString());
                    notificationBuilder.setSound(soundPath);
                }

                // Light
                if (light != null) {
                    try {
                        String[] lightsComponents = color.replaceAll("\\s", "").split(",");
                        if (lightsComponents.length == 3) {
                            int lightArgb = Color.parseColor(lightsComponents[0]);
                            int lightOnMs = Integer.parseInt(lightsComponents[1]);
                            int lightOffMs = Integer.parseInt(lightsComponents[2]);
                            notificationBuilder.setLights(lightArgb, lightOnMs, lightOffMs);
                            Log.d(TAG, "Lights: color="+lightsComponents[0]+"; on(ms)="+lightsComponents[2]+"; off(ms)="+lightsComponents[3]);
                        }

                    } catch (Exception e) {}
                }

                // Vibrate
                if (vibrate != null){
                    try {
                        String[] sVibrations = vibrate.replaceAll("\\s", "").split(",");
                        long[] lVibrations = new long[sVibrations.length];
                        int i=0;
                        for(String sVibration: sVibrations){
                            lVibrations[i] = Integer.parseInt(sVibration.trim());
                            i++;
                        }
                        notificationBuilder.setVibrate(lVibrations);
                        Log.d(TAG, "Vibrate: "+vibrate);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }


            // Icon
            int defaultSmallIconResID = getResources().getIdentifier(defaultSmallIconName, "drawable", getPackageName());
            int customSmallIconResID = 0;
            if(icon != null){
                customSmallIconResID = getResources().getIdentifier(icon, "drawable", getPackageName());
            }

            if (customSmallIconResID != 0) {
                notificationBuilder.setSmallIcon(customSmallIconResID);
                Log.d(TAG, "Small icon: custom="+icon);
            }else if (defaultSmallIconResID != 0) {
                Log.d(TAG, "Small icon: default="+defaultSmallIconName);
                notificationBuilder.setSmallIcon(defaultSmallIconResID);
            } else {
                // Fallback: use ic_launcher_foreground if available (typically monochrome-friendly)
                // or android.R.drawable.ic_dialog_info as last resort
                int fallbackIcon = getResources().getIdentifier("ic_launcher_foreground", "drawable", getPackageName());
                if (fallbackIcon != 0) {
                    Log.d(TAG, "Small icon: ic_launcher_foreground");
                    notificationBuilder.setSmallIcon(fallbackIcon);
                } else {
                    Log.d(TAG, "Small icon: android default");
                    notificationBuilder.setSmallIcon(android.R.drawable.ic_dialog_info);
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                int defaultLargeIconResID = getResources().getIdentifier(defaultLargeIconName, "drawable", getPackageName());
                int customLargeIconResID = 0;
                if(icon != null){
                    customLargeIconResID = getResources().getIdentifier(icon+"_large", "drawable", getPackageName());
                }

                int largeIconResID;
                if (customLargeIconResID != 0 || defaultLargeIconResID != 0) {
                    if (customLargeIconResID != 0) {
                        largeIconResID = customLargeIconResID;
                        Log.d(TAG, "Large icon: custom="+icon);
                    }else{
                        Log.d(TAG, "Large icon: default="+defaultLargeIconName);
                        largeIconResID = defaultLargeIconResID;
                    }
                    notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), largeIconResID));
                }
            }

            // Image
            if (image != null) {
                Log.d(TAG, "Large icon: image="+image);
                Bitmap bitmap = getBitmapFromURL(image);
                if(bitmap != null) {
                    if(imageTypeCircle.equalsIgnoreCase(imageType)) {
                        bitmap = getCircleBitmap(bitmap);
                    }
                    else if(imageTypeBigPicture.equalsIgnoreCase(imageType)) {
                        notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon((Bitmap) null));
                    }
                    notificationBuilder.setLargeIcon(bitmap);
                }
            }

            // Color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                int defaultColor = getResources().getColor(getResources().getIdentifier("accent", "color", getPackageName()), null);
                if(color != null){
                    notificationBuilder.setColor(Color.parseColor(color));
                    Log.d(TAG, "Color: custom="+color);
                }else{
                    Log.d(TAG, "Color: default");
                    notificationBuilder.setColor(defaultColor);
                }
            }

            // Visibility
            int iVisibility = NotificationCompat.VISIBILITY_PUBLIC;
            if(visibility != null){
                iVisibility = Integer.parseInt(visibility);
            }
            Log.d(TAG, "Visibility: " + iVisibility);
            notificationBuilder.setVisibility(iVisibility);

            // Priority
            int iPriority = NotificationCompat.PRIORITY_MAX;
            if(priority != null){
                iPriority = Integer.parseInt(priority);
            }
            Log.d(TAG, "Priority: " + iPriority);
            notificationBuilder.setPriority(iPriority);

            // Action Buttons
            if (actions != null && !actions.isEmpty()) {
                addActionButtons(notificationBuilder, actions, bundle, id);
            }

            // Build notification
            Notification notification = notificationBuilder.build();

            // Display notification
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Log.d(TAG, "show notification: "+notification.toString());
            notificationManager.notify(id.hashCode(), notification);
        }
        // Send to plugin
        FirebasePlugin.sendMessage(bundle, this.getApplicationContext());
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {

        if (bitmap == null) {
            return null;
        }

        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        float cx = bitmap.getWidth() / 2;
        float cy = bitmap.getHeight() / 2;
        float radius = cx < cy ? cx : cy;
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    private Spanned fromHtml(String source) {
        if (source != null)
            return Html.fromHtml(source);
        else
            return null;
    }

    private void putKVInBundle(String k, String v, Bundle b){
        if(v != null && !b.containsKey(k)){
            b.putString(k, v);
        }
    }

    /**
     * Helper class to store notification action data
     */
    private static class NotificationAction {
        String id;
        String title;
        String icon;
        boolean requiresInput;
        String inputPlaceholder;

        NotificationAction(String id, String title, String icon, boolean requiresInput, String inputPlaceholder) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.requiresInput = requiresInput;
            this.inputPlaceholder = inputPlaceholder;
        }
    }

    /**
     * Parse action buttons from the push payload data
     * Expected format: data.actions = [{"id":"accept","title":"Accept","icon":"ic_accept"},{"id":"reject","title":"Reject","icon":"ic_reject"}]
     */
    private List<NotificationAction> parseActions(Map<String, String> data) {
        List<NotificationAction> actions = new ArrayList<>();
        
        if (data == null || !data.containsKey("actions")) {
            return actions;
        }
        
        String actionsJson = data.get("actions");
        if (actionsJson == null || actionsJson.isEmpty()) {
            return actions;
        }
        
        try {
            JSONArray actionsArray = new JSONArray(actionsJson);
            Log.d(TAG, "Parsing " + actionsArray.length() + " action buttons");
            
            for (int i = 0; i < actionsArray.length(); i++) {
                JSONObject actionObj = actionsArray.getJSONObject(i);
                String id = actionObj.optString("id", null);
                String title = actionObj.optString("title", null);
                String icon = actionObj.optString("icon", null);
                boolean requiresInput = actionObj.optBoolean("requiresInput", false);
                String inputPlaceholder = actionObj.optString("inputPlaceholder", null);
                
                if (id != null && title != null) {
                    actions.add(new NotificationAction(id, title, icon, requiresInput, inputPlaceholder));
                    Log.d(TAG, "Added action: id=" + id + ", title=" + title + ", icon=" + icon + ", requiresInput=" + requiresInput);
                } else {
                    Log.w(TAG, "Skipping action with missing id or title");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing actions JSON", e);
        }
        
        return actions;
    }

    /**
     * Add action buttons to the notification
     * Compatible with Android 13+ (API 33+) using FLAG_IMMUTABLE
     */
    private void addActionButtons(NotificationCompat.Builder notificationBuilder, 
                                   List<NotificationAction> actions, 
                                   Bundle originalBundle, 
                                   String notificationId) {

        for (NotificationAction action : actions) {
            Bundle actionBundle = new Bundle(originalBundle);
            actionBundle.putString("action", action.id);
            actionBundle.putInt("notificationId", notificationId.hashCode());

            // Create unique request code to ensure each action gets its own PendingIntent
            int requestCode = (notificationId + "_" + action.id).hashCode();
            
            PendingIntent actionPendingIntent;
            
            // For reply and mark_read actions, use BroadcastReceiver (don't open app)
            if (action.requiresInput || action.id.equals("reply") || action.id.equals("mark_read") || action.id.equals("dismiss")) {
                Intent actionIntent = new Intent(this, FirebaseActionReceiver.class);
                actionIntent.setAction(FirebaseActionReceiver.ACTION_CLICK);
                actionIntent.putExtras(actionBundle);
                
                // RemoteInput requires MUTABLE PendingIntent
                int flag = (action.requiresInput || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                
                actionPendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    actionIntent,
                    flag
                );
            } else {
                // For other actions (view_commodity, dismiss), use Activity to bring app to foreground
                Intent actionIntent = new Intent(this, OnNotificationReceiverActivity.class);
                actionIntent.putExtras(actionBundle);
                actionIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                
                // Regular actions use IMMUTABLE for Android 13+ security
                int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;
                
                actionPendingIntent = PendingIntent.getActivity(
                    this,
                    requestCode,
                    actionIntent,
                    flag
                );
            }

            // Get icon resource if specified
            int iconResId = 0;
            if (action.icon != null && !action.icon.isEmpty()) {
                iconResId = getResources().getIdentifier(action.icon, "drawable", getPackageName());
            }

            // Add the action to the notification
            NotificationCompat.Action.Builder actionBuilder = 
                new NotificationCompat.Action.Builder(iconResId, action.title, actionPendingIntent);
            
            // Add inline reply if action requires input
            if (action.requiresInput) {
                String replyLabel = action.inputPlaceholder != null ? action.inputPlaceholder : "Enter your reply...";
                RemoteInput remoteInput = new RemoteInput.Builder(FirebaseActionReceiver.KEY_TEXT_REPLY)
                    .setLabel(replyLabel)
                    .build();
                actionBuilder.addRemoteInput(remoteInput);
                Log.d(TAG, "Added inline reply to action: " + action.title);
            }
            
            notificationBuilder.addAction(actionBuilder.build());
            
            Log.d(TAG, "Added notification action: " + action.title + " (" + action.id + ")");
        }
    }
}
