package io.github.carlorodriguez.alarmon;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

import static io.github.carlorodriguez.alarmon.App.FIRING_ALARM_CHANNEL_ID;
import static io.github.carlorodriguez.alarmon.NotificationService.FIRING_NOTIFICATION_BAR_ID;


public class ReceiverAlarm extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent recvIntent) {
    Uri alarmUri = recvIntent.getData();
    long alarmId = AlarmUtil.alarmUriToId(alarmUri);

    // We need to assert WakeLock, even when using the full screen intent from the notification
    try {
      WakeLock.acquire(context, alarmId);
    } catch (WakeLock.WakeLockException e) {
      if (AppSettings.isDebugMode(context)) {
        throw new IllegalStateException(e.getMessage());
      }
    }

    Intent notifyService = new Intent(context, NotificationService.class);
    notifyService.setData(alarmUri);
    // We must use startForegroundService because we need to start the service even if we received this receiver when the app is in the background
    // even the activity bound to the service immediately, to start the service from the background we must use startForegroundService
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(notifyService);
    }else{
      context.startService(notifyService);
    }

    // Get the time of the alarm to display it as the notification title
    DbAccessor db = new DbAccessor(context);
    String notifyText;
    try {
      AlarmInfo info = db.readAlarmInfo(alarmId);
      notifyText = (info == null || info.getName() == null) ? "" : info.getName();
      if (notifyText.equals("") && info != null) {
        notifyText = info.getTime().localizedString(context);
      }
    } catch (Exception e) {
      notifyText = context.getString(R.string.notification_alarm_title);
    }

    // Starts the heads up notification with full screen intent here, because the service will have it's own foreground notification
    Intent intent = new Intent(context, ActivityAlarmNotification.class);
    intent.setData(alarmUri);
    //intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    //PendingIntent notificationActivity = PendingIntent.getActivity(context, 0, intent, 0);
    PendingIntent notificationActivity = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

    Notification mNotification = new NotificationCompat.Builder(context, FIRING_ALARM_CHANNEL_ID)
            .setContentTitle(notifyText)
            .setContentText(context.getString(R.string.notification_alarm_body))
            //.setSmallIcon(R.drawable.ic_stat_notify_alarm)
            .setSmallIcon(R.mipmap.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.colorSecondary))
            .setPriority(NotificationCompat.PRIORITY_MAX) // just to be above lower priority notifications
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            //.setNotificationSilent() //it prevents the heads up notification and fires the full screen intent
            //.setAutoCancel(true)
            .setOngoing(true)
            // not needed for Android oreo and later when we set FullScreenIntent, but a must for older androids for the notification to be clickable
            .setContentIntent(notificationActivity)
            // Use a full-screen intent only for the highest-priority alerts where you
            // have an associated activity that you would like to launch after the user
            // interacts with the notification. Also, if your app targets Android 10
            // or higher, you need to request the USE_FULL_SCREEN_INTENT permission in
            // order for the platform to invoke this notification.
            .setFullScreenIntent(notificationActivity, true)
            .build();

    //mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

    // If the Id is already used by existing notification the system Ui will show heads up notification instead of the full Screen Intent
    //notificationManager.notify((int)System.currentTimeMillis(), mNotification);
    notificationManager.notify(FIRING_NOTIFICATION_BAR_ID, mNotification);

    db.closeConnections();

  }
}
