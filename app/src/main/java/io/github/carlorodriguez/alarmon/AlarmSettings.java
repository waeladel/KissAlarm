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

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * This class contains all of the settings data for a given alarm.  It also
 * provides the mapping from this data to the respective columns in the
 * persistent settings database.
 */
public final class AlarmSettings {
  static public final long DEFAULT_SETTINGS_ID = -1;

    private Uri mediaUri;
    private String mediaName;
    private String mediaType;
  private Uri tone;
  private String toneName;
  private int snoozeMinutes;
  private boolean vibrate;
    private boolean toggled;
    private boolean shown ;
    private int visibility;
  private int volumeStartPercent;
  private int volumeEndPercent;
  private int volumeChangeTimeSec;

  public ContentValues contentValues(long alarmId) {
    ContentValues values = new ContentValues();
    values.put(DbHelper.SETTINGS_COL_ID, alarmId);
      values.put(DbHelper.SETTINGS_COL_MEDIA_URL, mediaUri.toString());
      values.put(DbHelper.SETTINGS_COL_MEDIA_NAME, mediaName);
      values.put(DbHelper.SETTINGS_COL_MEDIA_TYPE, mediaType);
    values.put(DbHelper.SETTINGS_COL_TONE_URL, tone.toString());
    values.put(DbHelper.SETTINGS_COL_TONE_NAME, toneName);
    values.put(DbHelper.SETTINGS_COL_SNOOZE, snoozeMinutes);
    values.put(DbHelper.SETTINGS_COL_VIBRATE, vibrate);
      values.put(DbHelper.SETTINGS_COL_TOGGLED, toggled);
      values.put(DbHelper.SETTINGS_COL_SHOWN, shown);
      values.put(DbHelper.SETTINGS_COL_visibility, visibility);
    values.put(DbHelper.SETTINGS_COL_VOLUME_STARTING, volumeStartPercent);
    values.put(DbHelper.SETTINGS_COL_VOLUME_ENDING, volumeEndPercent);
    values.put(DbHelper.SETTINGS_COL_VOLUME_TIME, volumeChangeTimeSec);
    return values;
  }

  static public String[] contentColumns() {
    return new String[] {
      DbHelper.SETTINGS_COL_ID,
       DbHelper.SETTINGS_COL_MEDIA_URL,
       DbHelper.SETTINGS_COL_MEDIA_NAME,
       DbHelper.SETTINGS_COL_MEDIA_TYPE,
      DbHelper.SETTINGS_COL_TONE_URL,
      DbHelper.SETTINGS_COL_TONE_NAME,
      DbHelper.SETTINGS_COL_SNOOZE,
      DbHelper.SETTINGS_COL_VIBRATE,
         DbHelper.SETTINGS_COL_TOGGLED,
         DbHelper.SETTINGS_COL_SHOWN,
         DbHelper.SETTINGS_COL_visibility,
      DbHelper.SETTINGS_COL_VOLUME_STARTING,
      DbHelper.SETTINGS_COL_VOLUME_ENDING,
      DbHelper.SETTINGS_COL_VOLUME_TIME
    };
  }

  public AlarmSettings() {

      mediaUri = AlarmUtil.getDefaultMediaUri();
      mediaName = "girl.jpg";
      mediaType = "Default";//getResources().getString(R.string.default_media);
    tone = AlarmUtil.getDefaultAlarmUri();
    toneName = "Default";
    snoozeMinutes = 10;
    vibrate = false;
      toggled = false;
      shown = false;
      visibility = 0;
    volumeStartPercent = 0;
    volumeEndPercent = 100;
    volumeChangeTimeSec = 20;
  }

  public AlarmSettings(AlarmSettings rhs) {
      mediaUri = rhs.mediaUri;
      mediaName = rhs.mediaName;
      mediaType = rhs.mediaType;
    tone = rhs.tone;
    toneName = rhs.toneName;
    snoozeMinutes = rhs.snoozeMinutes;
    vibrate = rhs.vibrate;
      toggled = rhs.toggled;
      shown = rhs.shown;
    visibility = rhs.visibility;
    volumeStartPercent = rhs.volumeStartPercent;
    volumeEndPercent = rhs.volumeEndPercent;
    volumeChangeTimeSec = rhs.volumeChangeTimeSec;
  }

  public AlarmSettings(Cursor cursor) {
    cursor.moveToFirst();
    mediaUri = Uri.parse(cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_MEDIA_URL)));
    mediaName= cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_MEDIA_NAME));
    mediaType = cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_MEDIA_TYPE));
    tone = Uri.parse(cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TONE_URL)));
    toneName = cursor.getString(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TONE_NAME));
    snoozeMinutes = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_SNOOZE));
    vibrate = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VIBRATE)) == 1;
      toggled = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_TOGGLED)) == 1;
      shown = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_SHOWN)) == 1;
    visibility = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_visibility));
    volumeStartPercent = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_STARTING));
    volumeEndPercent = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_ENDING));
    volumeChangeTimeSec = cursor.getInt(cursor.getColumnIndex(DbHelper.SETTINGS_COL_VOLUME_TIME));
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AlarmSettings)) {
      return false;
    }
    AlarmSettings rhs = (AlarmSettings) o;
    return mediaUri.equals(rhs.mediaUri)
      && mediaName.equals(rhs.mediaName)
      && mediaType.equals(rhs.mediaType)
      && tone.equals(rhs.tone)
      && toneName.equals(rhs.toneName)
      && snoozeMinutes == rhs.snoozeMinutes
      && vibrate == rhs.vibrate
            && toggled == rhs.toggled
            && shown == rhs.shown
            && visibility == rhs.visibility
      && volumeStartPercent == rhs.volumeStartPercent
      && volumeEndPercent == rhs.volumeEndPercent
      && volumeChangeTimeSec == rhs.volumeChangeTimeSec;
  }

  public Uri getTone() {
    return tone;
  }
  public Uri getMediaUri() {
    return mediaUri;
  }


  public void setTone(Uri tone, String name) {
    this.tone = tone;
    this.toneName = name;
  }

  public void setMedia(Uri media, String name, String type) {
    this.mediaUri = media;
    this.mediaName = name;
    this.mediaType = type;
  }

  public String getToneName() {
    return toneName;
  }
  public String getMediaType() {
    return mediaType;
  }
    public String getMediaName() {
        return mediaName;
    }

  public int getSnoozeMinutes() {
    return snoozeMinutes;
  }

  public void setSnoozeMinutes(int minutes) {
    if (minutes < 1) {
      minutes = 1;
    } else if (minutes > 60) {
      minutes = 60;
    }
    this.snoozeMinutes = minutes;
  }

    public boolean getToggled() {
        return toggled;
    }
    public void setToggled(boolean toggle) {
        this.toggled = toggle;
    }

    public int getVisibility() {
        return visibility;
    }
    public void setVisibility(int visibility) {
      if (visibility < 0) {
        visibility = 0;
      } else if (visibility > 100) {
        visibility = 100;
      }
        this.visibility = visibility;
    }

  public boolean getShown() {
    return shown;
  }
  public void setShown(boolean show) {
    this.shown = show;
  }

    public boolean getVibrate() {
        return vibrate;
    }

  public void setVibrate(boolean vibrate) {
    this.vibrate = vibrate;
  }

  public int getVolumeStartPercent() {
    return volumeStartPercent;
  }

  public void setVolumeStartPercent(int volumeStartPercent) {
    if (volumeStartPercent < 0) {
      volumeStartPercent = 0;
    } else if (volumeStartPercent > 100) {
      volumeStartPercent = 100;
    }
    this.volumeStartPercent = volumeStartPercent;
  }

  public int getVolumeEndPercent() {
    return volumeEndPercent;
  }

  public void setVolumeEndPercent(int volumeEndPercent) {
    if (volumeEndPercent < 0) {
      volumeEndPercent = 0;
    } else if (volumeEndPercent > 100) {
      volumeEndPercent = 100;
    }
    this.volumeEndPercent = volumeEndPercent;
  }

  public int getVolumeChangeTimeSec() {
    return volumeChangeTimeSec;
  }

  public void setVolumeChangeTimeSec(int volumeChangeTimeSec) {
    if (volumeChangeTimeSec < 1) {
      volumeChangeTimeSec = 1;
    } else if (volumeChangeTimeSec > 600) {
      volumeChangeTimeSec = 600;
    }
    this.volumeChangeTimeSec = volumeChangeTimeSec;
  }
}
