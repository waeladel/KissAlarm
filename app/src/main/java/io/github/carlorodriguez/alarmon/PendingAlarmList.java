/****************************************************************************
 * Copyright 2010 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ****************************************************************************/

package io.github.carlorodriguez.alarmon;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import static io.github.carlorodriguez.alarmon.Utils.PendingIntentFlags.pendingIntentNoFlag;
import static io.github.carlorodriguez.alarmon.Utils.PendingIntentFlags.pendingIntentUpdateCurrentFlag;

import java.util.TreeMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import io.github.carlorodriguez.alarmon.Utils.CheckPermissions;

/**
 * This container holds a list of all currently scheduled alarms.
 * Adding/removing alarms to this container schedules/unschedules PendingIntents
 * with the android AlarmManager service.
 */
public final class PendingAlarmList {
  // Maps alarmId -> alarm.
  private static String TAG = PendingAlarmList.class.getSimpleName();

  private TreeMap<Long, PendingAlarm> pendingAlarms;
  // Maps alarm time -> alarmId.
  private TreeMap<AlarmTime, Long> alarmTimes;
  private AlarmManager alarmManager;
  private Context context;

  private AlertDialog permissionDialog;

  public PendingAlarmList(Context context) {
    pendingAlarms = new TreeMap<>();
    alarmTimes = new TreeMap<>();
    alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    this.context = context;
  }

  public int size() {
    if (pendingAlarms.size() != alarmTimes.size()) {
      throw new IllegalStateException("Inconsistent pending alarms: "
          + pendingAlarms.size() + " vs " + alarmTimes.size());
    }
    return pendingAlarms.size();
  }

  public void put(long alarmId, AlarmTime time) {
    // Remove this alarm if it exists already.
    remove(alarmId);

    // Intents are considered equal if they have the same action, data, type,
    // class, and categories.  In order to schedule multiple alarms, every
    // pending intent must be different.  This means that we must encode
    // the alarm id in the data section of the intent rather than in
    // the extras bundle.
    Intent notifyIntent = new Intent(context, ReceiverAlarm.class);
    notifyIntent.setData(AlarmUtil.alarmIdToUri(alarmId));

    PendingIntent scheduleIntent = PendingIntent.getBroadcast(context, 624, notifyIntent, pendingIntentNoFlag());

    // Schedule the alarm with the AlarmManager.
    // Previous instances of this intent will be overwritten in
    // the alarm manager.

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CheckPermissions.isNotificationPermissionGranted(context)){
        // Starting from Api 33 we must grant post notification permission at run time
        Toast.makeText(context, R.string.allow_notification_toast, Toast.LENGTH_LONG).show();
        return;
      }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
        // in App 32/31, user may disable Schedule Exact Alarms from app settings, we must check
        Toast.makeText(context, R.string.allow_alarm_settings_toast, Toast.LENGTH_LONG).show();
        return;
      }else if(!CheckPermissions.isNotificationEnabled(context)){
        // User disabled notifications channels
        Toast.makeText(context, R.string.allow_notification_toast, Toast.LENGTH_LONG).show();
        return;
      }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Intent intent = new Intent(context, ActivityAlarmClock.class);

        PendingIntent showIntent = PendingIntent.getActivity(context, 623, intent, pendingIntentUpdateCurrentFlag());

          AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.
                  AlarmClockInfo(time.calendar().getTimeInMillis(),
                  showIntent
                  );

          alarmManager.setAlarmClock(alarmClockInfo, scheduleIntent);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                  time.calendar().getTimeInMillis(), scheduleIntent);
      } else {
          alarmManager.set(AlarmManager.RTC_WAKEUP,
                  time.calendar().getTimeInMillis(), scheduleIntent);
      }

    // Keep track of all scheduled alarms.
    pendingAlarms.put(alarmId, new PendingAlarm(time, scheduleIntent));
    alarmTimes.put(time, alarmId);

    if (pendingAlarms.size() != alarmTimes.size()) {
      throw new IllegalStateException("Inconsistent pending alarms: "
          + pendingAlarms.size() + " vs " + alarmTimes.size());
    }
  }

  public boolean remove(long alarmId) {
    PendingAlarm alarm = pendingAlarms.remove(alarmId);
    if (alarm == null) {
      return false;
    }
    Long expectedAlarmId = alarmTimes.remove(alarm.time());
    alarmManager.cancel(alarm.pendingIntent());
    alarm.pendingIntent().cancel();

    if (expectedAlarmId != alarmId) {
      throw new IllegalStateException("Internal inconsistency in PendingAlarmList");
    }

    if (pendingAlarms.size() != alarmTimes.size()) {
      throw new IllegalStateException("Inconsistent pending alarms: "
          + pendingAlarms.size() + " vs " + alarmTimes.size());
    }

    return true;
  }

  public AlarmTime nextAlarmTime() {
    if (alarmTimes.size() == 0) {
      return null;
    }
    return alarmTimes.firstKey();
  }

    public long nextAlarmId() {
        if (alarmTimes.size() == 0) {
            return AlarmClockServiceBinder.NO_ALARM_ID;
        }
        return alarmTimes.get(alarmTimes.firstKey());
    }

  public AlarmTime pendingTime(long alarmId) {
    PendingAlarm alarm = pendingAlarms.get(alarmId);
    return alarm == null ? null : alarm.time();
  }

  public AlarmTime[] pendingTimes() {
    AlarmTime[] times = new AlarmTime[alarmTimes.size()];
    alarmTimes.keySet().toArray(times);
    return times;
  }

  public Long[] pendingAlarms() {
    Long[] alarmIds = new Long[pendingAlarms.size()];
    pendingAlarms.keySet().toArray(alarmIds);
    return alarmIds;
  }

  private class PendingAlarm {
    private AlarmTime time;
    private PendingIntent pendingIntent;

    PendingAlarm(AlarmTime time, PendingIntent pendingIntent) {
      this.time = time;
      this.pendingIntent = pendingIntent;
    }
    public AlarmTime time() {
      return time;
    }
    public PendingIntent pendingIntent() {
      return pendingIntent;
    }
  }
}
