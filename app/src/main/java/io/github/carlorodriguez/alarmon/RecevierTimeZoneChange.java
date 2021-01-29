package io.github.carlorodriguez.alarmon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class RecevierTimeZoneChange extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
      Intent i = new Intent(context, AlarmClockService.class);
      i.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_TIMEZONE_CHANGE);
      // We must use startForegroundService because we need to start the service
      // even if we received this receiver when the app is in the background
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(i);
      }else{
        context.startService(i);
      }
    }

  }

}
