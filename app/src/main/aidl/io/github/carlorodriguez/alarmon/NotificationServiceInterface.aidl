package io.github.carlorodriguez.alarmon;

interface NotificationServiceInterface {
  long currentAlarmId();
  int firingAlarmCount();
  float volume();
  Uri currentTone();
  void acknowledgeCurrentNotification(int snoozeMinutes);

}