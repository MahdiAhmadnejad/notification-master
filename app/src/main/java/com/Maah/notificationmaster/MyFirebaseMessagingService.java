package com.Maah.notificationmaster;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = String.valueOf(R.string.my_channel_id);
    private static final String CHANNEL_NAME = String.valueOf(R.string.my_channel);
    private static final String TAG = "MyFirebaseMsgService";
    int id = 0;

    private static final Map<String, Class<? extends Activity>> DESTINATION_MAP;

    static {
        Map<String, Class<? extends Activity>> map = new HashMap<>();
        map.put("main", MainActivity.class);

        DESTINATION_MAP = Collections.unmodifiableMap(map);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived: " + remoteMessage.getData());

        // Manually handle data message
        if (remoteMessage.getData().size() > 0) {
            createNotificationChannel();

            // Extract title and body from data
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String imageUrl = remoteMessage.getData().get("image");

            String action = remoteMessage.getData().get("action");
            String action_destination = remoteMessage.getData().get("action_destination");

            String idString = remoteMessage.getData().get("id");
            getInt(idString);

            if (action != null){
            try {
                Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                showNotificationWithImage(title, body, resource , action, action_destination, id );
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                showNotificationWithImage(title, body, null, action, action_destination, id);
                                Log.d(TAG, "Image Load Failed");
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: ", e);
                showNotificationWithImage(title, body, null, action, action_destination, id);
                }
            }
        }
    }

    private void showNotificationWithImage(String title, String body, @Nullable Bitmap image, String action, String actionDestination, int id)
    {
        createNotificationChannel();


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_android_black)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        Class<? extends Activity> destinationActivity = DESTINATION_MAP.get(actionDestination);
        Log.d(TAG, "mapping destination is : " + destinationActivity);

        // Add action buttons
        if (action.equals("alarming") && destinationActivity != null) {
            // Create PendingIntents for actions
            Intent actionIntent = new Intent(this, destinationActivity);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_android_black, getString(R.string.show), pendingIntent);
        }


        if (image != null) {
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                    .bigPicture(image)
                    .bigLargeIcon(null);
            builder.setStyle(bigPictureStyle);
        } else {
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                    .bigText(body);
            builder.setStyle(bigTextStyle);
        }


        // Create a unique notification ID
        int notificationId = (int) System.currentTimeMillis();

        // Get the notification manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Notify with the specified notification ID
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Handle permission issue
            return;
        }
        notificationManager.notify(notificationId, builder.build());

        sendDataToAPI(id);
    }

    private void createNotificationChannel() {
        // Check for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alarming");

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void getInt(String idString){
        if (idString != null) {
            try {
                id = Integer.parseInt(idString);
                Log.e(TAG, " 'id' is: " + id);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid format for 'id': " + idString, e);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // Handle the new FCM token here
        // You can send it to your server or perform any necessary actions
        FirebaseMessaging.getInstance().subscribeToTopic("Alarm");
        Log.d(TAG, "onNewToken: " + token);
    }

    private void sendDataToAPI(int id) {
        // Send notification ID to API
        String url = "https://myapi.com/logNotification";

        JSONObject data = new JSONObject();
        try {
            data.put("id", String.valueOf(id));
            // ... other data
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON object:", e);
            // Handle the error appropriately (e.g., log, notify user, try again)
            return; // Or take other appropriate action
        }


        RequestBody body = RequestBody.create(data.toString(), MediaType.get("application/json"));

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Handle error
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    // API call succeeded
                    Log.d(TAG, "onResponse: ");
                }
            }
        });
    }
}