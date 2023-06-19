package io.github.carlorodriguez.alarmon;

import androidx.multidex.MultiDexApplication;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by hp on 26/12/2017.
 */

public class App extends MultiDexApplication {

    private static Context sApplicationContext;
    private static MediaPlayer sMediaPlayer;
    private AudioAttributes audioAttributes;

    public static final String FIRING_ALARM_CHANNEL_ID = "Firing_alarm_channel_id";
    public static final String UPCOMING_ALARM_CHANNEL_ID = "Upcoming_alarm_channel_id";
    public static final String MISSED_ALARM_CHANNEL_ID = "Missed_alarm_channel_id";
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

            // No need to Create audioAttributes for notification's sound because all notification's music is played in the service
            /*audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();*/

            // Create a notification channel for firing alarms. It's heads up notification with full screen intent when the alarm first start
            NotificationChannel firingAlarmChannel = new NotificationChannel(
                    FIRING_ALARM_CHANNEL_ID,
                    getString(R.string.firing_alarms_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH // to display a heads up notification. We use null sound because we already play sound in the service
            );
            firingAlarmChannel.setDescription(getString(R.string.firing_alarm_notification_channel_description));
            firingAlarmChannel.enableLights(true);
            //alarmChannel.setSound(null, audioAttributes);
            firingAlarmChannel.setSound(null, null);

            // Create a notification channel for upcoming alarms
            NotificationChannel upcomingAlarmChannel = new NotificationChannel(
                    UPCOMING_ALARM_CHANNEL_ID,
                    getString(R.string.upcoming_alarms_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW // to display a heads up notification. We use null sound because we already play sound in the service
            );
            upcomingAlarmChannel.setDescription(getString(R.string.upcoming_alarm_notification_channel_description));

            // Create a notification channel for missed alarms
            NotificationChannel missedAlarmChannel = new NotificationChannel(
                    MISSED_ALARM_CHANNEL_ID,
                    getString(R.string.missed_alarms_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW // to display a heads up notification. We use null sound because we already play sound in the service
            );
            upcomingAlarmChannel.setDescription(getString(R.string.missed_alarm_notification_channel_description));

            //NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            if (manager != null) {
                manager.createNotificationChannel(firingAlarmChannel);
                manager.createNotificationChannel(upcomingAlarmChannel);
                manager.createNotificationChannel(missedAlarmChannel);
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

