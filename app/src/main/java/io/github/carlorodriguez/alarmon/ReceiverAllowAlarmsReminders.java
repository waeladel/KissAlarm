package io.github.carlorodriguez.alarmon;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class ReceiverAllowAlarmsReminders extends BroadcastReceiver {

  private static String TAG = ReceiverAllowAlarmsReminders.class.getSimpleName();
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
      Intent i = new Intent(context, AlarmClockService.class);
      i.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_DEVICE_BOOT);
      // We must use startForegroundService because we need to start the service
      // even if we received this receiver when the app is in the background
      ContextCompat.startForegroundService(context, i);
    }

  }

}
