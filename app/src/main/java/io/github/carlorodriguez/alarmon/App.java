package io.github.carlorodriguez.alarmon;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by hp on 26/12/2017.
 */

public class App extends Application {

    private static Context sApplicationContext;
    private static MediaPlayer sMediaPlayer;
    private AudioAttributes audioAttributes;

    public static final String ALARM_CHANNEL_ID = "Alarm_channel_id";
    private final static String TAG = App.class.getSimpleName();



    @Override
    public void onCreate() {

        super.onCreate();

        FirebaseAnalytics.getInstance(this); // to start analytics before DynamicLinks

        sApplicationContext = getApplicationContext();
        //sMediaPlayer = new MediaPlayer();

        // Initialize the SDK before executing any other operations,
        //FacebookSdk.sdkInitialize(sApplicationContext);
        createNotificationsChannels();
    }

    private void createNotificationsChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library

            // Create audioAttributes for notification's sound
            audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            NotificationChannel alarmChannel = new NotificationChannel(
                    ALARM_CHANNEL_ID,
                    getString(R.string.alarm_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            alarmChannel.setDescription(getString(R.string.alarm_notification_channel_description));

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(alarmChannel);
            }
        }
    }

    public static Context getContext() {
        return sApplicationContext;
        //return instance.getApplicationContext();
    }

    /*public static MediaPlayer getMediaPlayer() {
        return sMediaPlayer;
        //return instance.getApplicationContext();
    }*/

}

