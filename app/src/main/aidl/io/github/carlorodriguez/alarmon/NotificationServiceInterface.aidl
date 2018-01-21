package io.github.carlorodriguez.alarmon;

interface NotificationServiceInterface {
  long currentAlarmId();
  int firingAlarmCount();
  float volume();
  Uri currentTone();
  Uri getPhotoUri();
  String getMediaType();
  void acknowledgeCurrentNotification(int snoozeMinutes);
  void setPlayerSurface(in Surface surface);
  void releasePlayerSurfaceHolder(in Surface surface);


}