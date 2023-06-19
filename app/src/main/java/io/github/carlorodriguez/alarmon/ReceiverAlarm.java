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
    ContextCompat.startForegroundService(context, notifyService);

  }
}
