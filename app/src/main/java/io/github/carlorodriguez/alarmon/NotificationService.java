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

import java.util.LinkedList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.Surface;

import static io.github.carlorodriguez.alarmon.App.FIRING_ALARM_CHANNEL_ID;

/**
 * This service is responsible for notifying the user when an alarm is
 * triggered.  The pending intent delivered by the alarm manager service
 * will trigger the alarm receiver.  This receiver will in turn start
 * this service, passing the appropriate alarm url as data in the intent.
 * This service is capable of receiving multiple alarm notifications
 * without acknowledgments and will queue them until they are sequentially
 * acknowledged.  The service is capable of playing a sound, triggering
 * the vibrator and displaying the notification activity (used to acknowledge
 * alarms).
 */
public class NotificationService extends Service {
  private final static String TAG = NotificationService.class.getSimpleName();
  private Notification mNotification;
  private NotificationManagerCompat notificationManager;

  public final static int FIRING_NOTIFICATION_BAR_ID = 44;

  // Binder given to clients
  //private final IBinder mBinder = new LocalBinder();
  public Uri currentTone;

  // Commands to help us know which intent was received when the user click the notification's action button, dismiss or snooze intent
  public final static String COMMAND_EXTRA = "command";
  public final static int COMMAND_UNKNOWN = 1;
  public final static int COMMAND_DISMISS_ALARM = 5;
  public final static int COMMAND_SNOOZE_ALARM = 6;

  public class NoAlarmsException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  // Since the media player objects are expensive to create and destroy,
  // share them across invocations of this service (there should never be
  // more than one instance of this class in a given application).
  public enum MediaSingleton {
    INSTANCE;

    public MediaPlayer mediaPlayer = null;
    private Ringtone fallbackSound = null;
    private Vibrator vibrator = null;
    private int systemNotificationVolume = 0;


    MediaSingleton() {
      mediaPlayer = new MediaPlayer(); //App.getMediaPlayer();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // 21
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
      } else {
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
      }
    }

    // Force the alarm stream to be maximum volume.  This will allow the user
    // to select a volume between 0 and 100 percent via the settings activity.
    private void normalizeVolume(Context c, float startVolume) {
      final AudioManager audio =
        (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
      systemNotificationVolume =
          audio.getStreamVolume(AudioManager.STREAM_ALARM);
      audio.setStreamVolume(AudioManager.STREAM_ALARM,
          audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
      setVolume(startVolume);
    }

    private void setVolume(float volume) {
      mediaPlayer.setVolume(volume, volume);
    }

    private void resetVolume(Context c) {
      final AudioManager audio =
        (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
      audio.setStreamVolume(AudioManager.STREAM_ALARM, systemNotificationVolume,
              0);
    }

    private void useContext(Context c) {
      // The media player can fail for lots of reasons.  Try to setup a backup
      // sound for use when the media player fails.
      fallbackSound = RingtoneManager.getRingtone(c, AlarmUtil.getDefaultAlarmUri());
      if (fallbackSound == null) {
        Uri superFallback = RingtoneManager.getValidRingtoneUri(c);
        fallbackSound = RingtoneManager.getRingtone(c, superFallback);
      }
      // Make the fallback sound use the alarm stream as well.
      if (fallbackSound != null) {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
              fallbackSound.setStreamType(AudioManager.STREAM_ALARM);
          } else {
              fallbackSound.setAudioAttributes(new AudioAttributes.Builder()
                      .setUsage(AudioAttributes.USAGE_ALARM)
                      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                      .build());
          }
      }

      // Instantiate a vibrator.  That's fun to say.
      vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void ensureSound() {
      if (!mediaPlayer.isPlaying() &&
          fallbackSound != null && !fallbackSound.isPlaying()) {
        fallbackSound.play();
      }
    }

    private void vibrate() {
      if (vibrator != null) {
        vibrator.vibrate(new long[] {500, 500}, 0);
      }
    }

    public void play(Context c, Uri tone) {
      mediaPlayer.reset();
      mediaPlayer.setLooping(true);
      try {
        mediaPlayer.setDataSource(c, tone);
        mediaPlayer.prepare();
        mediaPlayer.start();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void stop() {
      mediaPlayer.stop();
      if (vibrator != null) {
        vibrator.cancel();
      }
      if (fallbackSound != null) {
        fallbackSound.stop();
      }
    }

      public void release() {
          mediaPlayer.release();
      }
  }

  // Data
  private LinkedList<Long> firingAlarms;
  private AlarmClockServiceBinder service;
  private DbAccessor db;
  // Notification tools
  private NotificationManager manager;
  private PendingIntent notificationActivity;
  private Handler handler;
  private VolumeIncreaser volumeIncreaseCallback;
  private Runnable soundCheck;
  private Runnable notificationBlinker;
  private Runnable autoCancel;

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "onBind: ");
    return new NotificationServiceInterfaceStub(this);
    //return mBinder;
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate: ");
    super.onCreate();
    firingAlarms = new LinkedList<>();
    // Access to in-memory and persistent data structures.
    service = new AlarmClockServiceBinder(getApplicationContext());
    service.bind();
    db = new DbAccessor(getApplicationContext());

    // Setup audio.
    MediaSingleton.INSTANCE.useContext(getApplicationContext());

    // Setup notification bar.
    //manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager = NotificationManagerCompat.from(this);

    // Setup a self-scheduling event loops.
    handler = new Handler();
    volumeIncreaseCallback = new VolumeIncreaser();
    soundCheck = new Runnable() {
      @Override
      public void run() {
        // Some sound should always be playing.
        MediaSingleton.INSTANCE.ensureSound();

        long next = AlarmUtil.millisTillNextInterval(AlarmUtil.Interval.SECOND);
        handler.postDelayed(soundCheck, next);
      }
    };

    notificationBlinker = new Runnable() {
      @Override
      public void run() {
        String notifyText;
        try {
          AlarmInfo info = db.readAlarmInfo(currentAlarmId());
          notifyText = (info == null || info.getName() == null) ? "" : info.getName();
          if (notifyText.equals("") && info != null) {
            notifyText = info.getTime().localizedString(getApplicationContext());
          }
        } catch (NoAlarmsException e) {
          return;
        }

        // Use the notification activity explicitly in this intent just in case the
        // activity can't be viewed via the root activity.
        // Starts the heads up notification which is the foreground notification as weel
        Intent intent = new Intent(getApplicationContext(), ActivityAlarmNotification.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notificationActivity = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent to for the action button to dismiss the alarm
        Intent dismissIntent = new Intent(getApplicationContext(), NotificationService.class);
        dismissIntent.putExtra(AlarmClockService.COMMAND_EXTRA, NotificationService.COMMAND_DISMISS_ALARM);
        PendingIntent dismissPendingIntent = PendingIntent.getService(getApplicationContext(), 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent to for the action button to snooze the alarm
        Intent snoozeIntent = new Intent(getApplicationContext(), NotificationService.class);
        snoozeIntent.putExtra(AlarmClockService.COMMAND_EXTRA, NotificationService.COMMAND_SNOOZE_ALARM);
        PendingIntent snoozePendingIntent = PendingIntent.getService(getApplicationContext(), 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification mNotification = new NotificationCompat.Builder(getApplicationContext(), FIRING_ALARM_CHANNEL_ID)
                .setContentTitle(notifyText)
                .setContentText(getApplicationContext().getString(R.string.notification_alarm_body))
                //.setSmallIcon(R.drawable.ic_stat_notify_alarm)
                .setSmallIcon(R.mipmap.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSecondary))
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
                .addAction(R.drawable.ic_baseline_snooze_24, getApplicationContext().getString(R.string.snooze), snoozePendingIntent) // Snooze button
                .addAction(R.drawable.ic_baseline_alarm_off_24, getApplicationContext().getString(R.string.dismiss), dismissPendingIntent) // Dismiss button
                .setOnlyAlertOnce(true) // to make sure we pop the heads up notification only once.
                .build();

        mNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          // use startForeground because startForegroundService is called from the ReceiverAlarm
          startForeground(FIRING_NOTIFICATION_BAR_ID, mNotification);
          // Never blink here because the heads up notification will keeps popping over and over every time it's clicked
        }else{
          notificationManager.notify(FIRING_NOTIFICATION_BAR_ID, mNotification);
          // blinking notification only works in old androids. handler.post will be called later in soundAlarm function
          long next = AlarmUtil.millisTillNextInterval(AlarmUtil.Interval.SECOND);
          handler.postDelayed(notificationBlinker, next);
        }
      }
    };

    autoCancel = new Runnable() {
      @Override
      public void run() {
        try {
          acknowledgeCurrentNotification(0);
        } catch (NoAlarmsException e) {
          return;
        }
        Intent notifyActivity = new Intent(getApplicationContext(), ActivityAlarmNotification.class);
        notifyActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notifyActivity.putExtra(ActivityAlarmNotification.TIMEOUT_COMMAND, true);
        startActivity(notifyActivity);
      }
    };
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
    db.closeConnections();

    // This service wouldn't be destroy until the last clint unbound, that why we remove the notification when stopping the player
    // We already canceling the foreground notification when user stops the sound, but lets do it here just in case
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      stopForeground(true);
    }else{
      notificationManager.cancel(FIRING_NOTIFICATION_BAR_ID); // To remove the heads up notification that starts the activity
    }

    service.unbind();

    boolean debug = AppSettings.isDebugMode(getApplicationContext());
    if (debug && firingAlarms.size() != 0) {
      throw new IllegalStateException("Notification service terminated with pending notifications.");
    }
    try {
      WakeLock.assertNoneHeld();
    } catch (WakeLock.WakeLockException e) {
      if (debug) { throw new IllegalStateException(e.getMessage()); }
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "onStartCommand: start id= "+startId);
    handleStart(intent);
    return START_NOT_STICKY;
  }

  private void handleStart(Intent intent) {

    // Check if we must start the alarm sound or the user just clicked on action button in the notification and we must stop the service?
    if (intent != null && intent.hasExtra(COMMAND_EXTRA)) {
      Bundle extras = intent.getExtras();
      int command = extras.getInt(COMMAND_EXTRA, COMMAND_UNKNOWN);
      try {
        long alarmId = currentAlarmId(); // get the current alarm
        switch (command) {
          case COMMAND_DISMISS_ALARM:
            Log.d(TAG, "action button: dismiss is clicked");
            acknowledgeCurrentNotification(0);
            break;
          case COMMAND_SNOOZE_ALARM:
            Log.d(TAG, "action button: snooze is clicked");
            int snoozeMinutes = db.readAlarmSettings(alarmId).getSnoozeMinutes(); // get snooze minutes time from the database
            acknowledgeCurrentNotification(snoozeMinutes);
            break;
        }

        // finish the notification activity because it may be already exists. Send a local broadcast that will be received by the activity
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(NotificationService.this);
        localBroadcastManager.sendBroadcast(new Intent("com.kiss.alarm.action.close"));

        // To refresh the alarm's "next time" if the main activity was already opened
        if(ActivityAlarmClock.isActive){
          Intent i =new Intent(getApplicationContext(), ActivityAlarmClock.class);
          i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(i);
        }

      } catch (NoAlarmsException e) {
        e.printStackTrace();
      }


      return; // Don't start the alarm sound, return because this intent is started by a user clicked on dismiss or snooze action button
    }

    // startService called from alarm receiver with an alarm id url. onStartCommand is not starting by an action button
    if (intent != null && intent.getData() != null) {
      long alarmId = AlarmUtil.alarmUriToId(intent.getData());
      try {
        WakeLock.assertHeld(alarmId);
      } catch (WakeLock.WakeLockException e) {
        if (AppSettings.isDebugMode(getApplicationContext())) {
          throw new IllegalStateException(e.getMessage());
        }
      }
      // We starts the notification activity by the notification now, so that the system Ui can choose to show
      // heads up notification instead of the activity if the user is busy using the device right now
      /*Intent notifyActivity = new Intent(getApplicationContext(), ActivityAlarmNotification.class);
      notifyActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(notifyActivity);*/

      boolean firstAlarm = firingAlarms.size() == 0;
      if (!firingAlarms.contains(alarmId)) {
        firingAlarms.add(alarmId);
      }

      if (firstAlarm) {
        soundAlarm(alarmId);
      }
    }
  }

  public long currentAlarmId() throws NoAlarmsException {
    if (firingAlarms.size() == 0) {
      throw new NoAlarmsException();
    }
    return firingAlarms.getFirst();
  }

  public int firingAlarmCount() {
    return firingAlarms.size();
  }

  public float volume() {
    return volumeIncreaseCallback.volume();
  }

  public Uri currentTone() {
    return currentTone;
  }

  public Uri getPhotoUri() throws NoAlarmsException {
    AlarmSettings settings = db.readAlarmSettings(currentAlarmId());
    return settings.getMediaUri();
  }

  public String getMediaType() throws NoAlarmsException{
    AlarmSettings settings = db.readAlarmSettings(currentAlarmId());
    return settings.getMediaType();
  }

  public void setPlayerSurfaceOnService(Surface surface) {
    MediaSingleton.INSTANCE.mediaPlayer.setSurface(surface);
      Log.d("mama2", "mediaPlayer.setSurface ");

  }

  public void releasePlayerSurface() {

    MediaSingleton.INSTANCE.mediaPlayer.setDisplay(null);
    //MediaSingleton.INSTANCE.mediaPlayer.stop();
    //MediaSingleton.INSTANCE.mediaPlayer.reset();
    //MediaSingleton.INSTANCE.mediaPlayer.release();
      Log.d("mama2", "mediaPlayer.released from service");
  }

  public void acknowledgeCurrentNotification(int snoozeMinutes) throws NoAlarmsException {
    long alarmId = currentAlarmId();
    if (firingAlarms.contains(alarmId)) {
      firingAlarms.remove(alarmId);
      if (snoozeMinutes <= 0) {
        service.acknowledgeAlarm(alarmId);
      } else {
        service.snoozeAlarmFor(alarmId, snoozeMinutes);
      }
    }
    stopNotifying();

    // If this was the only alarm firing, stop the service.  Otherwise,
    // start the next alarm in the stack.
    if (firingAlarms.size() == 0) {
      // We must clear the foreground notification here because in OnDestroy is not called until all activities unbound
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // This service wouldn't be destroy until the last clint unbound, that why we remove the notification when stopping the player
        stopForeground(true);
      }else{
        notificationManager.cancel(FIRING_NOTIFICATION_BAR_ID); //  Remove the heads up notification that starts the activity
      }

      stopSelf();
    } else {
      soundAlarm(alarmId);
    }
    try {
      WakeLock.release(alarmId);
    } catch (WakeLock.WakeLockException e) {
      if (AppSettings.isDebugMode(getApplicationContext())) {
        throw new IllegalStateException(e.getMessage());
      }
    }
  }

  private void soundAlarm(long alarmId) {
    // Begin notifying based on settings for this alaram.
    AlarmSettings settings = db.readAlarmSettings(alarmId);
    if (settings.getVibrate()) {
      MediaSingleton.INSTANCE.vibrate();
    }

    volumeIncreaseCallback.reset(settings);
    MediaSingleton.INSTANCE.normalizeVolume(
        getApplicationContext(), volumeIncreaseCallback.volume());
    MediaSingleton.INSTANCE.play(getApplicationContext(), settings.getTone());
    //Store getTone value for notification activity
    currentTone = settings.getTone();
    // Start periodic events for handling this notification.
    handler.post(volumeIncreaseCallback);
    handler.post(soundCheck);
    handler.post(notificationBlinker);
    // Set up a canceler if this notification isn't acknowledged by the timeout.
    int timeoutMillis = 60 * 1000 * AppSettings.alarmTimeOutMins(getApplicationContext());
    handler.postDelayed(autoCancel, timeoutMillis);
  }

  private void stopNotifying() {
    // Stop periodic events.
    handler.removeCallbacks(volumeIncreaseCallback);
    handler.removeCallbacks(soundCheck);
    handler.removeCallbacks(notificationBlinker);
    handler.removeCallbacks(autoCancel);

    // Stop notifying.
    MediaSingleton.INSTANCE.stop();
    MediaSingleton.INSTANCE.resetVolume(getApplicationContext());
  }

  /**
   * Helper class for gradually increasing the volume of the alarm audio
   * stream.
   */
  private final class VolumeIncreaser implements Runnable {
    float start;
    float end;
    float increment;

    public float volume() {
      return start;
    }

    public void reset(AlarmSettings settings) {
      start = (float) (settings.getVolumeStartPercent() / 100.0);
      end = (float) (settings.getVolumeEndPercent() / 100.0);
      increment = (end - start) / (float) settings.getVolumeChangeTimeSec();
    }

    @Override
    public void run() {
      start += increment;
      if (start > end) {
        start = end;
      }
      MediaSingleton.INSTANCE.setVolume(start);

      if (Math.abs(start - end) > (float) 0.0001) {
        handler.postDelayed(volumeIncreaseCallback, 1000);
      }
    }
  }

  /**
   * Class used for the client Binder.  Because we know this service always
   * runs in the same process as its clients, we don't need to deal with IPC.
   */
  /*public class LocalBinder extends Binder {
    NotificationService getService() {
      // Return this instance of LocalService so clients can call public methods
      return NotificationService.this;
    }
  }*/
}



