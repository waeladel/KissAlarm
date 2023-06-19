package io.github.carlorodriguez.alarmon;

import java.util.TreeMap;

import android.content.Context;
import android.os.PowerManager;

public class WakeLock {
  public static class WakeLockException extends Exception {
    private static final long serialVersionUID = 1L;
    public WakeLockException(String e) {
      super(e);
    }
  }

  private static final TreeMap<Long, PowerManager.WakeLock> wakeLocks =
    new TreeMap<>();

  public static void acquire(Context context, long alarmId) throws WakeLockException {
    if (wakeLocks.containsKey(alarmId)) {
      if (AppSettings.isDebugMode(context)) {
        throw new WakeLockException("Multiple acquisitions of wake lock for id: " + alarmId);
      }
    }

    // Use Partial wake lock instead of SCREEN_DIM_WAKE_LOCK amd ACQUIRE_CAUSES_WAKEUP because screen dim wakes up the screen
    // so the system thinks that user is using the phone so it displays heads-up notification instead of Full intent notification
    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK ,
        "Alarm Notification Wake Lock id " + alarmId);
    wakeLock.setReferenceCounted(false);
    wakeLock.acquire(60 * 60000);

    wakeLocks.put(alarmId, wakeLock);
  }

  public static void assertHeld(long alarmId) throws WakeLockException {
    PowerManager.WakeLock wakeLock = wakeLocks.get(alarmId);
    if (wakeLock == null || !wakeLock.isHeld()) {
      if (AppSettings.isDebugMode(App.getContext())) {
        throw new WakeLockException("Wake lock not held for alarm id: " + alarmId);
      }
    }
  }

  public static void assertNoneHeld() throws WakeLockException {
    for (PowerManager.WakeLock wakeLock : wakeLocks.values()) {
      if (wakeLock.isHeld()) {
        if (AppSettings.isDebugMode(App.getContext())) {
          throw new WakeLockException("No wake locks are held.");
        }
      }
    }
  }

  public static void release(long alarmId) throws WakeLockException {
    assertHeld(alarmId);
    PowerManager.WakeLock wakeLock = wakeLocks.remove(alarmId);
    wakeLock.release();
  }
}
