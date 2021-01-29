package io.github.carlorodriguez.alarmon;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ReceiverNotificationRefresh extends BroadcastReceiver {
  private final static String TAG = ReceiverNotificationRefresh.class.getSimpleName();

  public static void startRefreshing(Context context) {
    context.sendBroadcast(intent(context));
  }

  public static void stopRefreshing(Context context) {
    final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    manager.cancel(pendingIntent(context));
  }

  private static Intent intent(Context context) {
    return new Intent(context, ReceiverNotificationRefresh.class);
  }

  private static PendingIntent pendingIntent(Context context) {
    return PendingIntent.getBroadcast(context, 0, intent(context), 0);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    final Intent causeRefresh = new Intent(context, AlarmClockService.class);
    Log.d(TAG, "onReceive: ");
    causeRefresh.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_NOTIFICATION_REFRESH);
    // We must use startForegroundService because we need to start the service
    // even if we received this receiver when the app is in the background
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(causeRefresh);
    }else{
      context.startService(causeRefresh);
    }
    // Don't refresh every minute it's bad for battery life, lets refresh the notification every hour
    //long next = AlarmUtil.nextIntervalInUTC(AlarmUtil.Interval.MINUTE);
    long next = AlarmUtil.nextIntervalInUTC(AlarmUtil.Interval.HOUR);
    final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    manager.set(AlarmManager.RTC, next, pendingIntent(context));
  }
}
