package io.github.carlorodriguez.alarmon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class ReceiverDeviceBoot extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    // There doesn't seem to be any way to filter on the scheme-specific
    // portion of the ACTION_PACKANGE_REPLACED intent (the package
    // being replaced is in the ssp).  Since we can't filter for it in the
    // Manifest file, we get every package replaced event and cancel this
    // event if it's not our package.
    if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
      if (!intent.getData().getSchemeSpecificPart().equals(context.getPackageName())) {
        return;
      }
    }
    Intent i = new Intent(context, AlarmClockService.class);
    i.putExtra(AlarmClockService.COMMAND_EXTRA, AlarmClockService.COMMAND_DEVICE_BOOT);
    // We must use startForegroundService because we need to start the service
    // even if we received this receiver when the app is in the background
    ContextCompat.startForegroundService(context, i);

  }

}
