package com.Maah.notificationmaster;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = String.valueOf(R.string.my_channel_id);
    private static final String CHANNEL_NAME = String.valueOf(R.string.my_channel);
    private static final String TAG = "MyFirebaseMsgService";

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

            try {
                Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                showNotificationWithImage(title, body, resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                showNotificationWithImage(title, body, null);
                                Log.d(TAG, "Image Load Failed");
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: ", e);
                showNotificationWithImage(title, body, null);
            }
        }
    }


    private void showNotificationWithImage(String title, String body, @Nullable Bitmap image)
    {
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

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
            channel.setDescription("Channel Description");

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // Handle the new FCM token here
        // You can send it to your server or perform any necessary actions
        FirebaseMessaging.getInstance().subscribeToTopic("your_topic_name");
        Log.d(TAG, "onNewToken: " + token);
        Toast.makeText(this, "your token is generated", Toast.LENGTH_SHORT).show()     ;
    }
}