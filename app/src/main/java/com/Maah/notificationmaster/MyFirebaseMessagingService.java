package com.Maah.notificationmaster;

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

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = String.valueOf(R.string.my_channel_id);
    private static final String CHANNEL_NAME = String.valueOf(R.string.my_channel);
    private static final String TAG = "MyFirebaseMsgService";
    int id = 0;
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

    private void showNotificationWithImage(String title, String body, @Nullable Bitmap image, String action, String actionDestination, int id)
    {
        createNotificationChannel();


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Add action buttons
        if (action.equals("alarming") && actionDestination != null) {
            // Create PendingIntents for actions
            Intent actionIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent1 = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_launcher_foreground, getString(R.string.show), pendingIntent1);
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
}