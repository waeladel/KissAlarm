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
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class DbAccessor {
  private SQLiteDatabase rDb;
  private SQLiteDatabase rwDb;

  public DbAccessor(Context context) {
    DbHelper db = new DbHelper(context);
    rwDb = db.getWritableDatabase();
    rDb = db.getReadableDatabase();
  }

  public void closeConnections() {
    rDb.close();
    rwDb.close();
  }

  public long newAlarm(AlarmTime time, boolean enabled, String name) {
    AlarmInfo info = new AlarmInfo(time, enabled, name);

    long id = rwDb.insert(DbHelper.DB_TABLE_ALARMS, null, info.contentValues());
    if (id < 0) {
      throw new IllegalStateException("Unable to insert into database");
    }
    return id;
  }

  public boolean deleteAlarm(long alarmId) {
    int count = rDb.delete(DbHelper.DB_TABLE_ALARMS,
        DbHelper.ALARMS_COL__ID + " = " + alarmId, null);
    // This may or may not exist.  We don't care about the return value.
    rDb.delete(DbHelper.DB_TABLE_SETTINGS,
        DbHelper.SETTINGS_COL_ID + " = " + alarmId, null);
    return count > 0;
  }

  public boolean enableAlarm(long alarmId, boolean enabled) {
    ContentValues values = new ContentValues(1);
    values.put(DbHelper.ALARMS_COL_ENABLED, enabled);
    int count = rwDb.update(DbHelper.DB_TABLE_ALARMS, values,
        DbHelper.ALARMS_COL__ID + " = " + alarmId, null);
    return count != 0;
  }

  public List<Long> getEnabledAlarms() {
    LinkedList<Long> enabled = new LinkedList<>();
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_ALARMS,
        new String[] { DbHelper.ALARMS_COL__ID },
        DbHelper.ALARMS_COL_ENABLED + " = 1", null, null, null, null);
    while (cursor.moveToNext()) {
      long alarmId = cursor.getLong(cursor.getColumnIndex(DbHelper.ALARMS_COL__ID));
      enabled.add(alarmId);
    }
    cursor.close();
    return enabled;
  }

  public List<Long> getAllAlarms() {
    LinkedList<Long> alarms = new LinkedList<>();
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_ALARMS,
        new String[] { DbHelper.ALARMS_COL__ID },
        null, null, null, null, null);
    while (cursor.moveToNext()) {
      long alarmId = cursor.getLong(cursor.getColumnIndex(DbHelper.ALARMS_COL__ID));
      alarms.add(alarmId);
    }
    cursor.close();
    return alarms;
  }

  public boolean writeAlarmInfo(long alarmId, AlarmInfo info) {
    return rwDb.update(DbHelper.DB_TABLE_ALARMS, info.contentValues(),
          DbHelper.ALARMS_COL__ID + " = " + alarmId, null) == 1;
  }

  public Cursor readAlarmInfo() {
    return rDb.query(DbHelper.DB_TABLE_ALARMS, AlarmInfo.contentColumns(),
        null, null, null, null, DbHelper.ALARMS_COL_TIME + " ASC");
  }

  public AlarmInfo readAlarmInfo(long alarmId) {
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_ALARMS, 
        AlarmInfo.contentColumns(),
        DbHelper.ALARMS_COL__ID + " = " + alarmId, null, null, null, null);

    if (cursor.getCount() != 1) {
      cursor.close();
      return null;
    }

    cursor.moveToFirst();
    AlarmInfo info = new AlarmInfo(cursor);
    cursor.close();
    return info;
  }

  public boolean writeAlarmSettings(long alarmId, AlarmSettings settings) {
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_SETTINGS,
        new String[] { DbHelper.SETTINGS_COL_ID },
        DbHelper.SETTINGS_COL_ID + " = " + alarmId, null, null, null, null);

    boolean success;
    if (cursor.getCount() < 1) {
      success = rwDb.insert(DbHelper.DB_TABLE_SETTINGS, null, settings.contentValues(alarmId)) >= 0;
    } else {
      success = rwDb.update(DbHelper.DB_TABLE_SETTINGS, settings.contentValues(alarmId),
          DbHelper.SETTINGS_COL_ID + " = " + alarmId, null) == 1;
    }
    cursor.close();
    return success;
  }

  public AlarmSettings readAlarmSettings(long alarmId) {
    Cursor cursor = rDb.query(DbHelper.DB_TABLE_SETTINGS, 
        AlarmSettings.contentColumns(),
        DbHelper.SETTINGS_COL_ID + " = " + alarmId, null, null, null, null);

    if (cursor.getCount() != 1) {
      cursor.close();
      if (alarmId == AlarmSettings.DEFAULT_SETTINGS_ID) {
        return new AlarmSettings();
      }
      return readAlarmSettings(AlarmSettings.DEFAULT_SETTINGS_ID);
    }

    AlarmSettings settings = new AlarmSettings(cursor);
    cursor.close();
    return settings;
  }

    public StringBuffer getAllData() {
        Cursor cursor = rDb.rawQuery("Select * from "+ DbHelper.DB_TABLE_SETTINGS,null);
        StringBuffer buffer = new StringBuffer();
        while (cursor.moveToNext()){
            buffer.append("id = "+ cursor.getString(0)+"\n");
            buffer.append("media_url = "+ cursor.getString(1)+"\n");
            buffer.append("media_name = "+ cursor.getString(2)+"\n");
            buffer.append("tone_url = "+ cursor.getString(3)+"\n");
            buffer.append("tone_name = "+ cursor.getString(4)+"\n");
            buffer.append("snooze = "+ cursor.getString(5)+"\n");
            buffer.append("vibrate = "+ cursor.getString(6)+"\n");
            buffer.append("vol_start = "+ cursor.getString(7)+"\n");
            buffer.append("vol_end = "+ cursor.getString(8)+"\n");
            buffer.append("vol_time = "+ cursor.getString(9)+"\n\n");
        }
        return buffer;
    }
}
