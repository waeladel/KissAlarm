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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.theartofdev.edmodo.cropper.CropImage;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import io.github.carlorodriguez.alarmon.Utils.FilesHelper;
import io.github.carlorodriguez.alarmon.interfaces.ItemClickListener;

import static io.github.carlorodriguez.alarmon.Utils.FilesHelper.addAudioToMediaStore;
import static io.github.carlorodriguez.alarmon.Utils.FilesHelper.createImageUri;
import static io.github.carlorodriguez.alarmon.Utils.FilesHelper.createVideoUri;
import static io.github.carlorodriguez.alarmon.Utils.FilesHelper.getFileName;
import static io.github.carlorodriguez.alarmon.Utils.FilesHelper.getMimeTyp;


/**
 * This activity is used for editing alarm settings.  Settings are broken
 * into two pieces: alarm information and actual settings.  Every alarm will
 * have alarm information.  Alarms will only have alarm settings if the user
 * has overridden the default settings for a given alarm.  This dialog is used
 * to edit both the application default settings and individual alarm settings.
 * When editing the application default settings, no AlarmInfo object will
 * be present.  When editing an alarm which hasn't yet had specific settings
 * set, AlarmSettings will contain the default settings.  There is one required
 * EXTRA that must be supplied when starting this activity: EXTRAS_ALARM_ID,
 * which should contain a long representing the alarmId of the settings
 * being edited.  AlarmSettings.DEFAULT_SETTINGS_ID can be used to edit the
 * default settings.
 */
public final class ActivityAlarmSettings extends AppCompatActivity implements
        TimePickerDialog.OnTimeChangedListener,
        TimePickerDialog.OnTimeSetListener,
        ItemClickListener {

    private static String TAG = ActivityAlarmSettings.class.getSimpleName();
    private static final int SELECT_MULTIMEDIA = 2;
    private static final int MIC_PERMISSION_REQUEST_CODE = 7000;
    private static final int REQUEST_CAMERA_PHOTO_PERMISSIONS_CODE = 124;
    private static final int REQUEST_CAMERA_VIDEO_PERMISSIONS_CODE = 125;
    private static final int REQUEST_RECORD_AUDIO_CODE = 4;

    private static final int SELECT_MEDIA_REQUEST_CODE = 102;
    private static final int CROP_IMAGE_REQUEST_CODE = 104;

    private static final String APP_AUTHORITY = BuildConfig.APPLICATION_ID +".fileprovider";
    private  static final String PERMISSION_RATIONALE_FRAGMENT = "storagePermissionFragment";
    private File mOutputFile, mCroppedImage;
    private Uri mRecordedAudioUri;

    public static final String EXTRAS_ALARM_ID = "alarm_id";
    private static final int MISSING_EXTRAS = -69;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 11;
    //private static final int SELECT_Media_REQUEST = 22;

    private static final int EXPLAIN_MIC_PERMISSION = 43;
    private static final String SETTINGS_VIBRATE_KEY = "SETTINGS_VIBRATE_KEY";
    private static final String SETTINGS_SNOOZE_KEY = "SETTINGS_SNOOZE_KEY";
    private static final String SETTINGS_TIME_HOUR_OF_DAY_KEY = "SETTINGS_TIME_HOUR_OF_DAY_KEY";
    private static final String SETTINGS_TIME_MINUTE_KEY = "SETTINGS_TIME_MINUTE_KEY";
    private static final String SETTINGS_TIME_SECOND_KEY = "SETTINGS_TIME_SECOND_KEY";
    private static final String SETTINGS_NAME_KEY = "SETTINGS_NAME_KEY";
    private static final String SETTINGS_VOLUME_START_PERCENT_KEY = "SETTINGS_VOLUME_START_PERCENT_KEY";
    private static final String SETTINGS_VOLUME_END_PERCENT_KEY = "SETTINGS_VOLUME_END_PERCENT_KEY";
    private static final String SETTINGS_VOLUME_CHANGE_TIME_SEC_KEY = "SETTINGS_VOLUME_CHANGE_TIME_SEC_KEY";
    private static final String SETTINGS_TONE_NAME_KEY = "SETTINGS_TONE_NAME_KEY";
    private static final String SETTINGS_TONE_URI_KEY = "SETTINGS_TONE_URI_KEY";

    private static final String SETTINGS_MEDIA_URI_KEY = "SETTINGS_MEDIA_URI_KEY";
    private static final String SETTINGS_MEDIA_NAME_KEY = "SETTINGS_MEDIA_NAME_KEY";
    private static final String SETTINGS_MEDIA_TYPE_KEY = "SETTINGS_MEDIA_TYPE_KEY";

    private static final String SETTINGS_BUTTON_TOGGLED_KEY = "SETTINGS_BUTTON_TOGGLED_KEY";
    private static final String SETTINGS_BUTTON_SHOWN_KEY = "SETTINGS_BUTTON_SHOWN_KEY";
    private static final String SETTINGS_BUTTON_VISIBILITY_KEY = "SETTINGS_BUTTON_VISIBILITY _KEY";

    private static final String SETTINGS_DAYS_OF_WEEK_KEY = "SETTINGS_DAYS_OF_WEEK_KEY";

    private int mMediaType ;
    private String  mMediaName ;
    private Uri mMediaUri  ;
    private FragmentManager mFragmentManager;


    private enum SettingType {
    TIME,
    NAME,
    DAYS_OF_WEEK,
    TONE,MEDIA, SNOOZE,TOGGLED,SHOWN,VISIBILITY ,
    VIBRATE,
    VOLUME_FADE
  }

    public static final int NAME_PICKER = 1;
    public static final int DOW_PICKER = 2;
    public static final int TONE_PICKER = 3;
    public static final int SNOOZE_PICKER = 4;
    public static final int VOLUME_FADE_PICKER = 5;
    public static final int DELETE_CONFIRM = 6;
    public static final int EXPLAIN_READ_EXTERNAL_STORAGE = 7;
    public static final int PERMISSION_NOT_GRANTED = 8;
    public static final int MEDIA_PICKER = 9;

    //Media selection popup Menu
    public static final int MENU_SELECT_PICTURE = 0;
    public static final int MENU_CAPTURE_PICTURE = 1;
    public static final int MENU_SELECT_VIDEO = 2;
    public static final int MENU_RECORD_VIDEO = 3;

    private TimePickerDialog picker;

  private static long alarmId;
  private static AlarmClockServiceBinder service;
  private DbAccessor db;
  private AlarmInfo originalInfo;
  private static AlarmInfo info;
  private static AlarmSettings originalSettings;
  private static AlarmSettings settings;
  static SettingsAdapter settingsAdapter;
  private static ProgressDialog progressDialog;

    @Override
  protected void onCreate(Bundle savedInstanceState) {
      AppSettings.setTheme(getBaseContext(), ActivityAlarmSettings.this);

    super.onCreate(savedInstanceState);

    mFragmentManager = getSupportFragmentManager(); // Needed to open the rational dialog

    setContentView(R.layout.settings);

      setTitle(getString(R.string.settings));

      ActionBar actionBar = getSupportActionBar();

      if (actionBar != null) {
          actionBar.setDisplayHomeAsUpEnabled(true);
      }

      // Step 4: Set output file for audio
      //mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_Audio);
      // Step 4: Set output file for notification audio
      mOutputFile = FilesHelper.getOutputMediaFile(this, FilesHelper.MEDIA_TYPE_Audio);

    // An alarm id is required in the extras bundle.
    alarmId = getIntent().getExtras().getLong(EXTRAS_ALARM_ID, MISSING_EXTRAS);
    if (alarmId == MISSING_EXTRAS) {
      throw new IllegalStateException("EXTRAS_ALARM_ID not supplied in intent.");
    }

    // Access to in-memory and persistent data structures.
    service = new AlarmClockServiceBinder(getApplicationContext());
    db = new DbAccessor(getApplicationContext());

    // Read the current settings from the database.  Keep a copy of the
    // original values so that we can write new values only if they differ
    // from the originals.
    originalInfo = db.readAlarmInfo(alarmId);
    // Info will not be available for the default settings.
    if (originalInfo != null) {
      info = new AlarmInfo(originalInfo);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SETTINGS_TIME_HOUR_OF_DAY_KEY)
                    && savedInstanceState.containsKey(SETTINGS_TIME_MINUTE_KEY)
                    && savedInstanceState.containsKey(SETTINGS_TIME_SECOND_KEY)) {
                final AlarmTime time = info.getTime();

                info.setTime(new AlarmTime(
                                savedInstanceState.getInt(SETTINGS_TIME_HOUR_OF_DAY_KEY),
                                savedInstanceState.getInt(SETTINGS_TIME_MINUTE_KEY),
                                savedInstanceState.getInt(SETTINGS_TIME_SECOND_KEY),
                                time.getDaysOfWeek()
                        ));
            }

            if (savedInstanceState.containsKey(SETTINGS_NAME_KEY)) {
                info.setName(savedInstanceState.getString(SETTINGS_NAME_KEY));
            }

            if (savedInstanceState.containsKey(SETTINGS_DAYS_OF_WEEK_KEY)) {
                info.setDaysOfWeek(new Week(savedInstanceState.getBooleanArray(
                        SETTINGS_DAYS_OF_WEEK_KEY)));
            }
        }
    }
    originalSettings = db.readAlarmSettings(alarmId);
    settings = new AlarmSettings(originalSettings);

      Log.d(TAG, "settings= "+ settings);
      Log.d(TAG, "originalSettings= "+ originalSettings);

      if (savedInstanceState != null) {
          if (savedInstanceState.containsKey(SETTINGS_SNOOZE_KEY)) {
              settings.setSnoozeMinutes(savedInstanceState.getInt(
                      SETTINGS_SNOOZE_KEY));
              Log.d(TAG, "setSnoozeMinutes= "+ savedInstanceState.getInt(
                      SETTINGS_SNOOZE_KEY));

          }

          if (savedInstanceState.containsKey(SETTINGS_VIBRATE_KEY)) {
              settings.setVibrate(savedInstanceState.getBoolean(
                      SETTINGS_VIBRATE_KEY));
              Log.d(TAG, "setVibrate= "+ savedInstanceState.getBoolean(
                      SETTINGS_VIBRATE_KEY));
          }

          if (savedInstanceState.containsKey(SETTINGS_BUTTON_TOGGLED_KEY)) {
              settings.setToggled(savedInstanceState.getBoolean(
                      SETTINGS_BUTTON_TOGGLED_KEY));
              Log.d(TAG, "setToggle= "+ savedInstanceState.getBoolean(
                      SETTINGS_BUTTON_TOGGLED_KEY));
          }

          if (savedInstanceState.containsKey(SETTINGS_BUTTON_SHOWN_KEY)) {
              settings.setShown(savedInstanceState.getBoolean(
                      SETTINGS_BUTTON_SHOWN_KEY));
              Log.d(TAG, "setShow= "+ savedInstanceState.getBoolean(
                      SETTINGS_BUTTON_SHOWN_KEY));
          }

          if (savedInstanceState.containsKey(SETTINGS_BUTTON_VISIBILITY_KEY)) {
              settings.setVisibility(savedInstanceState.getInt(
                      SETTINGS_BUTTON_VISIBILITY_KEY));
              //Log.d(TAG, "setShow= "+ savedInstanceState.getBoolean(SETTINGS_BUTTON_VISIBILITY_KEY));
          }

          if (savedInstanceState.containsKey(SETTINGS_VOLUME_START_PERCENT_KEY)
                  && savedInstanceState.containsKey(SETTINGS_VOLUME_END_PERCENT_KEY)
                  && savedInstanceState.containsKey(SETTINGS_VOLUME_CHANGE_TIME_SEC_KEY)) {
              settings.setVolumeStartPercent(savedInstanceState.getInt(
                      SETTINGS_VOLUME_START_PERCENT_KEY));

              settings.setVolumeEndPercent(savedInstanceState.getInt(
                      SETTINGS_VOLUME_END_PERCENT_KEY));

              settings.setVolumeChangeTimeSec(savedInstanceState.getInt(
                      SETTINGS_VOLUME_CHANGE_TIME_SEC_KEY));
          }

          if (savedInstanceState.containsKey(SETTINGS_TONE_URI_KEY)
                  && savedInstanceState.containsKey(SETTINGS_TONE_NAME_KEY)) {
              settings.setTone((Uri) savedInstanceState.getParcelable(SETTINGS_TONE_URI_KEY),
                      savedInstanceState.getString(SETTINGS_TONE_NAME_KEY));
          }

          if (savedInstanceState.containsKey(SETTINGS_MEDIA_URI_KEY)
                  && savedInstanceState.containsKey(SETTINGS_MEDIA_NAME_KEY)
                  && savedInstanceState.containsKey(SETTINGS_MEDIA_TYPE_KEY)) {
              settings.setMedia((Uri) savedInstanceState.getParcelable(SETTINGS_MEDIA_URI_KEY),
                      savedInstanceState.getString(SETTINGS_MEDIA_NAME_KEY),
                      savedInstanceState.getString(SETTINGS_MEDIA_TYPE_KEY));
          }
      }

    // Setup the list of settings.  Each setting is represented by a Setting
    // object.  Create one here for each setting type.
    final ArrayList<Setting> settingsObjects =
      new ArrayList<>(SettingType.values().length);
    // Only display AlarmInfo if the user is editing an actual alarm (as
    // opposed to the default application settings).
    if (alarmId != AlarmSettings.DEFAULT_SETTINGS_ID) {
      // The alarm time.
      settingsObjects.add(new Setting() {
        @Override
        public String name() { return getString(R.string.time); }
        @Override
        public String value() { return info.getTime().localizedString(getApplicationContext()); }
        @Override
        public SettingType type() { return SettingType.TIME; }
      });
      // A human-readable label for the alarm.
      settingsObjects.add(new Setting() {
        @Override
        public String name() { return getString(R.string.label); }
        @Override
        public String value() { return info.getName().equals("") ?
            getString(R.string.none) : info.getName(); }
        @Override
        public SettingType type() { return SettingType.NAME; }
      });
      // Days of the week this alarm should repeat.
      settingsObjects.add(new Setting() {
        @Override
        public String name() { return getString(R.string.repeat); }
        @Override
        public String value() { return info.getTime().getDaysOfWeek().toString(getApplicationContext()); }
        @Override
        public SettingType type() { return SettingType.DAYS_OF_WEEK; }
      });
    }
      // The notification media used for this alarm.
      settingsObjects.add(new Setting() {
          @Override
          public String name() { return getString(R.string.partner_video_or_photo); }
          @Override
          public String value() {
              String value = settings.getMediaType();

              /*switch (value) {
                  case "Video":
                      value = getString(R.string.media_video);
                      value += ": " + settings.getMediaName();
                      if (AppSettings.isDebugMode(getApplicationContext())) {
                          value += " " + settings.getMediaUri().toString();
                      }
                      break;
                  case "Photo":
                      value = getString(R.string.media_photo);
                      value += ": " + settings.getMediaName();
                      if (AppSettings.isDebugMode(getApplicationContext())) {
                          value += " " + settings.getMediaUri().toString();
                      }
                      break;
                  default:
                      value = getString(R.string.debug_default);
                      break;
              }*/

              if (value.equalsIgnoreCase("Default")){
                  value = getString(R.string.debug_default);
              }else{
                  value = settings.getMediaName();
              }

              if (AppSettings.isDebugMode(getApplicationContext())) {
                  value += " " + settings.getMediaUri().toString();
              }

              return value;
          }
          @Override
          public SettingType type() { return SettingType.MEDIA; }
      });
    // The notification tone used for this alarm.
    settingsObjects.add(new Setting() {
      @Override
      public String name() { return getString(R.string.tone); }
      @Override
      public String value() {
        String value = settings.getToneName();

          if (value.equalsIgnoreCase("Default")){
              value = getString(R.string.debug_default);
          }

        if (AppSettings.isDebugMode(getApplicationContext())) {
          value += " " + settings.getTone().toString();
        }
        return value;
      }
      @Override
      public SettingType type() { return SettingType.TONE; }
    });


      // The toggle lips/forehead buttons setting for this alarm.
      settingsObjects.add(new Setting() {
          @Override
          public String name() { return getString(R.string.toggle_button); }
          @Override
          public String value() { return getString(R.string.toggle_button_hint); }
          //settings.getToggled() ? getString(R.string.enabled) : getString(R.string.disabled);
          @Override
          public SettingType type() { return SettingType.TOGGLED; }
      });

      // The show semitransparent lips/forehead buttons setting for this alarm.
      settingsObjects.add(new Setting() {
          @Override
          public String name() { return getString(R.string.show_button); }
          @Override
          public String value() { return getString(R.string.show_button_hint); }
          //settings.getShown() ? getString(R.string.enabled) : getString(R.string.disabled);
          @Override
          public SettingType type() { return SettingType.SHOWN; }
      });

      // The show visibility sitting for lips/forehead buttons
      settingsObjects.add(new Setting() {
          @Override
          public String name() { return getString(R.string.visibility_button); }
          @Override
          public String value() { return "" + settings.getVisibility(); }
          //settings.getShown() ? getString(R.string.enabled) : getString(R.string.disabled);
          @Override
          public SettingType type() { return SettingType.VISIBILITY; }
      });

      // The snooze duration for this alarm.
      settingsObjects.add(new Setting() {
          @Override
          public String name() { return getString(R.string.snooze_minutes); }
          @Override
          public String value() { return "" + settings.getSnoozeMinutes(); }
          @Override
          public SettingType type() { return SettingType.SNOOZE; }
      });

      // The vibrator setting for this alarm.
      settingsObjects.add(new Setting() {
          @Override
          public String name() { return getString(R.string.vibrate); }
          @Override
          public String value() { return settings.getVibrate() ?
                  getString(R.string.enabled) : getString(R.string.disabled); }
          @Override
          public SettingType type() { return SettingType.VIBRATE; }
      });

      // How the volume should be controlled while this alarm is triggering.
    settingsObjects.add(new Setting() {
      @Override
      public String name() { return getString(R.string.alarm_fade); }
      @Override
      public String value() { return getString(R.string.fade_description,
          settings.getVolumeStartPercent(), settings.getVolumeEndPercent(),
          settings.getVolumeChangeTimeSec()); }
      @Override
      public SettingType type() { return SettingType.VOLUME_FADE; }
    });

    final ListView settingsList = (ListView) findViewById(R.id.settings_list);
    settingsAdapter = new SettingsAdapter(getApplicationContext(),
            settingsObjects);
    settingsList.setAdapter(settingsAdapter);
    settingsList.setOnItemClickListener(new SettingsListClickListener());
      //settingsAdapter.getItem(SettingType.VOLUME_FADE);
      //settingsObjects.remove(4);
      //settingsList.getChildAt(0).setEnabled(false);
      //settingsList.getItemAtPosition(4).setVisibility(View.GONE);
      //settingsList.getItemAtPosition(4);.setEnabled(false);

  }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (alarmId == AlarmSettings.DEFAULT_SETTINGS_ID) {
            return super.onCreateOptionsMenu(menu);
        } else {
            MenuInflater inflater = getMenuInflater();

            inflater.inflate(R.menu.alarm_settings_menu, menu);

            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_alarm:
                showDialogFragment(DELETE_CONFIRM);
                return true;
            case android.R.id.home:
                saveAlarmSettings();

                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        saveAlarmSettings();
    }

    @Override
  protected void onResume() {
    super.onResume();
    service.bind();

      TimePickerDialog tpd = (TimePickerDialog) getFragmentManager().
              findFragmentByTag("TimePickerDialog");

      if (tpd != null) {
          picker = tpd;

          tpd.setOnTimeSetListener(this);

          tpd.setOnTimeChangedListener(this);
      }
  }

  @Override
  protected void onPause() {
    super.onPause();
    service.unbind();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    db.closeConnections();

      if (progressDialog != null) {
          progressDialog.dismiss();
      }

      progressDialog = null;

      picker = null;

      settingsAdapter = null;
  }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_CODE:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Great! User has recorded and saved the audio file");
                    Log.d(TAG, "Intent getData= "+data);
                    // Great! User has recorded and saved the audio file
                    settings.setTone(Uri.parse(mOutputFile.getAbsolutePath()), mOutputFile.getName());// Set video url on tone
                    settingsAdapter.notifyDataSetChanged();
                    // Update the MediaStore and copy the recorded file to shared location in android 10 and above, because it's in the app specific location
                    addAudioToMediaStore(this, mOutputFile);
                } else if (resultCode == RESULT_CANCELED) {
                    // Oops! User has canceled the recording
                    Log.w(TAG, "Oops! User has canceled the recording");
                }
                break;
            case SELECT_MEDIA_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    // User selected a media file or captured a new one
                    /*List<Uri> selectedMedias = Matisse.obtainResult(data);
                    Uri selectedMediaUri = selectedMedias.get(0);
                    Log.d(TAG, "mSelected: " + selectedMediaUri);*/
                    if(null != data && null != data.getData()){
                        // We have intent data because we only select a photo or video not capturing a new one
                        Log.d(TAG, "intent= " + data);
                        mMediaUri = data.getData();
                    }
                    // if intent or getData() is null it's because we provide the camera with file for saving
                    Log.d(TAG, "mSelected mMediaUri= " + mMediaUri);
                    if(mMediaUri == null){
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }
                        return; // Return because we don't have the location of the saved media file
                    }

                    String mediaType = getMimeTyp(mMediaUri, this);
                    Log.d(TAG, "mediaType: " + mediaType);

                    mMediaName = getFileName(mMediaUri, this);
                    if (TextUtils.isEmpty(mMediaName)) {
                        mMediaName = getString(R.string.unknown_name);
                    }
                    Log.d(TAG, "mMediaName: " + mMediaName);

                    if (mediaType.contains("image/")){
                        // It's a photo
                        cropImage(mMediaUri);
                    }else {
                        // It's a video
                        Log.d(TAG, "Video content:// mMediaUri= " + mMediaUri);
                        //Log.d(TAG, "Video file:// mMediaUri= " + getRealPathFromURI(mMediaUri, this));
                        if(null != data){
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){
                                int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                ContentResolver resolver = this.getContentResolver();
                                //this.grantUriPermission(getPackageName(), mMediaUri, Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                                resolver.takePersistableUriPermission(mMediaUri, takeFlags);
                            }
                        }
                        settings.setMedia(mMediaUri,mMediaName ,"Video");// Set photo url on media getResources().getString(R.string.media_video)
                        settings.setTone(mMediaUri, mMediaName);// Set video url on tone
                        settingsAdapter.notifyDataSetChanged();
                    }

                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                }else{
                    // The user canceled the selection, hide the progress dialog
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    // Delete the photo uri from database so that we don't add a blank photo
                    if(mMediaUri != null){
                        ContentResolver resolver = this.getContentResolver();
                        resolver.delete(mMediaUri, null, null);
                    }
                    Log.w(TAG, "Select Media error. maybe the user canceled");
                }

                break;
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                Log.d(TAG, "CROP_PICTURE requestCode="+ requestCode);
                CropImage.ActivityResult result =  CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Uri MediaUri = result.getUri();
                    Log.d(TAG, "CROP_PICTURE getUri="+ MediaUri);
                    settings.setMedia(MediaUri,mMediaName  ,"Photo");// Set photo url on media getResources().getString(R.string.media_photo)
                    settingsAdapter.notifyDataSetChanged();

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                    Toast.makeText(ActivityAlarmSettings.this, error.toString(),
                            Toast.LENGTH_LONG).show();
                    Log.w(TAG, "CROP_PICTURE error ="+ result.getError());

                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION_REQUEST:
                Log.d(TAG, "PERMISSION READ_EXTERNAL_STORAGE");
                if (permissions.length == 1 &&
                        permissions[0].equals(
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showProgressDialog();

                    showDialogFragment(TONE_PICKER);
                } else {
                    showDialogFragment(PERMISSION_NOT_GRANTED);
                }
                break;
            case MIC_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // init task you need to do.
                    recordAudio();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showDialogFragment(PERMISSION_NOT_GRANTED);
                }
                break;
            case REQUEST_CAMERA_PHOTO_PERMISSIONS_CODE: // To write a file to image directory for api < 29
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the task you need to do.
                    Log.i(TAG, "onRequestPermissionsResult permission was granted");
                    selectMedia(MENU_CAPTURE_PICTURE);
                    //showMediaMenu(settingsAdapter.getView(0, null, null));
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i(TAG, "onRequestPermissionsResult permission denied");
                }
                break;
            case REQUEST_CAMERA_VIDEO_PERMISSIONS_CODE: // To write a file to video directory for api < 29
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the task you need to do.
                    Log.i(TAG, "onRequestPermissionsResult permission was granted");
                    selectMedia(MENU_RECORD_VIDEO);
                    //showMediaMenu(settingsAdapter.getView(0, null, null));
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i(TAG, "onRequestPermissionsResult permission denied");
                }
                break;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SETTINGS_VIBRATE_KEY, settings.getVibrate());
        outState.putBoolean(SETTINGS_BUTTON_TOGGLED_KEY, settings.getToggled());
        outState.putBoolean(SETTINGS_BUTTON_SHOWN_KEY, settings.getShown());
        outState.putInt(SETTINGS_BUTTON_VISIBILITY_KEY, settings.getVisibility());

        outState.putInt(SETTINGS_SNOOZE_KEY, settings.getSnoozeMinutes());

        if (originalInfo != null && info != null) {
            final AlarmTime infoTime = info.getTime();

            outState.putInt(SETTINGS_TIME_HOUR_OF_DAY_KEY,
                    infoTime.calendar().get(Calendar.HOUR_OF_DAY));

            outState.putInt(SETTINGS_TIME_MINUTE_KEY,
                    infoTime.calendar().get(Calendar.MINUTE));

            outState.putInt(SETTINGS_TIME_SECOND_KEY,
                    infoTime.calendar().get(Calendar.SECOND));

            outState.putString(SETTINGS_NAME_KEY, info.getName());

            outState.putBooleanArray(SETTINGS_DAYS_OF_WEEK_KEY,
                    info.getTime().getDaysOfWeek().bitmask());
        }

        outState.putParcelable(SETTINGS_TONE_URI_KEY, settings.getTone());

        outState.putString(SETTINGS_TONE_NAME_KEY, settings.getToneName());

        outState.putParcelable(SETTINGS_MEDIA_URI_KEY, settings.getMediaUri());

        outState.putString(SETTINGS_MEDIA_NAME_KEY, settings.getMediaName());

        outState.putString(SETTINGS_MEDIA_TYPE_KEY, settings.getMediaType());

        outState.putInt(SETTINGS_VOLUME_START_PERCENT_KEY,
                settings.getVolumeStartPercent());

        outState.putInt(SETTINGS_VOLUME_END_PERCENT_KEY,
                settings.getVolumeEndPercent());

        outState.putInt(SETTINGS_VOLUME_CHANGE_TIME_SEC_KEY,
                settings.getVolumeChangeTimeSec());
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        final AlarmTime time = info.getTime();

        info.setTime(new AlarmTime(hourOfDay, minute, second, time.getDaysOfWeek()));

        settingsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTimeChanged(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        final AlarmTime infoTime = info.getTime();

        final AlarmTime time = new AlarmTime(hourOfDay, minute, second,
                infoTime.getDaysOfWeek());

        picker.setTitle(time.timeUntilString(this));
    }

    private void saveAlarmSettings() {
        // Write AlarmInfo if it changed.
        if (originalInfo != null && !originalInfo.equals(info)) {
            db.writeAlarmInfo(alarmId, info);

            // Explicitly enable the alarm if the user changed the time.
            // This will reschedule the alarm if it was already enabled.
            // It's also probably the right thing to do if the alarm wasn't
            // enabled.
            if (!originalInfo.getTime().equals(info.getTime())) {
                service.scheduleAlarm(alarmId);
            } else if (originalInfo.enabled()
                    && !originalInfo.getName().equals(info.getName())) {
                service.scheduleAlarm(alarmId);
            }
        }

        // Write AlarmSettings if they have changed.
        if (!originalSettings.equals(settings)) {
            db.writeAlarmSettings(alarmId, settings);
        }
    }

    public static void _requestReadExternalStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityCompat.requestPermissions(activity,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST);
        }
    }

    public void requestReadExternalStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showDialogFragment(EXPLAIN_READ_EXTERNAL_STORAGE);
        } else {
            _requestReadExternalStoragePermission(this);
        }
    }

  /**
   * This is a helper class for mapping SettingType to action.  Each Setting
   * in the list view returns a unique SettingType.  Trigger a dialog
   * based off of that SettingType.
   */
  private final class SettingsListClickListener implements OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      final SettingsAdapter adapter = (SettingsAdapter) parent.getAdapter();
      SettingType type = adapter.getItem(position).type();
        Log.d(TAG, "AdapterType position" + position);
        Log.d(TAG, "AdapterType" + type);
      switch (type) {
        case TIME:
            final AlarmTime time = info.getTime();
            Calendar c = time.calendar();

            picker = TimePickerDialog.newInstance(
                    ActivityAlarmSettings.this,
                    ActivityAlarmSettings.this,
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(ActivityAlarmSettings.this)
            );

            if (AppSettings.isThemeDark(ActivityAlarmSettings.this)) {
                picker.setThemeDark(true);
            }

            picker.setAccentColor(AppSettings.getTimePickerColor(
                    ActivityAlarmSettings.this));

            picker.vibrate(true);

            if (AppSettings.isDebugMode(ActivityAlarmSettings.this)) {
                picker.enableSeconds(true);
            } else {
                picker.enableSeconds(false);
            }

            picker.setTitle(time.timeUntilString(ActivityAlarmSettings.this));

            picker.show(getFragmentManager(), "TimePickerDialog");
          break;
        case NAME:
          showDialogFragment(NAME_PICKER);
          break;

        case DAYS_OF_WEEK:
          showDialogFragment(DOW_PICKER);
          break;

        case TONE:
            Log.d(TAG, "type = photo1"+ settings.getMediaType());
            if(!settings.getMediaType().equals("Video")){
                Log.d(TAG, "type = photo1");
                String audioSourceTitle = getResources().getString(R.string.audio_source);
                String cancelButton = getResources().getString(R.string.cancel);
                String[] audioSourceArray = getResources().getStringArray(R.array.audio_source);

                showAudioDialog(audioSourceTitle,audioSourceArray ,cancelButton );
                /*showProgressDialog();
                showDialogFragment(TONE_PICKER);*/
            }
            break;

          case MEDIA:
              Log.d(TAG, "MEDIA clicked");
              Log.d(TAG, "type" + type);
              Log.d(TAG, "getAllData buffer= "+db.getAllData().toString());
              showMediaMenu(view);
              //showMediaMenu(settingsAdapter.getView(0, null, null));
              break;

        case SNOOZE:
          showDialogFragment(SNOOZE_PICKER);
          break;

        case VOLUME_FADE:
          showDialogFragment(VOLUME_FADE_PICKER);
          break;
      }
    }
  }

    public void showMediaMenu(View view) {

        // Create a popup Menu if null. To enable user to select to select or capture media
        PopupMenu popupBlockMenu = new PopupMenu(this, view);
        popupBlockMenu.getMenu().add(Menu.NONE, MENU_SELECT_PICTURE, 0, R.string.menu_item_select_picture);
        popupBlockMenu.getMenu().add(Menu.NONE, MENU_CAPTURE_PICTURE, 1, R.string.menu_item_capture_picture);
        popupBlockMenu.getMenu().add(Menu.NONE, MENU_SELECT_VIDEO, 2, R.string.menu_item_select_video);
        popupBlockMenu.getMenu().add(Menu.NONE, MENU_RECORD_VIDEO, 3, R.string.menu_item_record_video);

        popupBlockMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case MENU_SELECT_PICTURE:
                        Log.i(TAG, "onMenuItemClick. item select_picture clicked ");
                        showProgressDialog();
                        selectMedia(MENU_SELECT_PICTURE);
                        return true;
                    case MENU_CAPTURE_PICTURE:
                        Log.i(TAG, "onMenuItemClick. item capture_picture clicked ");
                        // We need write permission in Api < 29 to save file at shared storage
                        if (!isWritePermissionsGranted()) {
                            requestWritePermission(MENU_CAPTURE_PICTURE);
                        }else{
                            showProgressDialog();
                            selectMedia(MENU_CAPTURE_PICTURE);
                        }
                        return true;
                    case MENU_SELECT_VIDEO:
                        Log.i(TAG, "onMenuItemClick. item select_video clicked ");
                        showProgressDialog();
                        selectMedia(MENU_SELECT_VIDEO);
                        return true;
                    case MENU_RECORD_VIDEO:
                        Log.i(TAG, "onMenuItemClick. item record_video clicked ");
                        // We need write permission in Api < 29 to save file at shared storage
                        if (!isWritePermissionsGranted()) {
                            requestWritePermission(MENU_RECORD_VIDEO);
                        }else{
                            showProgressDialog();
                            selectMedia(MENU_RECORD_VIDEO);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        popupBlockMenu.show();

    }
    public void selectMedia(int menuItem) {
        //final int mediaType = AlbumFile.getMediaType();

        switch (menuItem) {
            case MENU_SELECT_PICTURE:
                Log.i(TAG, "selectMedia. select picture");
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickPhoto.setType("image/*"); //"video/*"
                //pickPhoto.addCategory(Intent.CATEGORY_OPENABLE); addCategory is only a part of storage api. works with ACTION_GET_CONTENT only

                try {
                    startActivityForResult(pickPhoto, SELECT_MEDIA_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // display error state to the user
                }
                break;
            case MENU_CAPTURE_PICTURE:
                Log.i(TAG, "selectMedia. capture picture");
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // Ensure that there's a camera activity to handle the intent
                List<ResolveInfo> listPhotosCam = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (listPhotosCam.size() <= 0) {
                    Log.i(TAG, "No camera app to handel the intent");
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    Toast.makeText(ActivityAlarmSettings.this, R.string.no_camera_app_error, Toast.LENGTH_LONG).show();
                    return;
                }
                // Create a file location to tell the camera to save the new photo at it
                mMediaUri = createImageUri(this);
                if(mMediaUri != null){
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri); // Pass the new file uri in the intent extras
                }
                Log.d(TAG, "mMediaUri= "+mMediaUri);
                /*takePictureIntent.setClipData(ClipData.newRawUri("", mediaUri));
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);*/
                try {
                    startActivityForResult(takePictureIntent, SELECT_MEDIA_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // display error state to the user
                }
                break;
            case MENU_SELECT_VIDEO:
                Log.i(TAG, "selectMedia. select video");
                Intent pickVideo = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    pickVideo = new Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    pickVideo.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                }else{
                    pickVideo = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    pickVideo.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                pickVideo.setType("video/*"); //"image/*"
                pickVideo.addCategory(Intent.CATEGORY_OPENABLE);

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pickVideo.putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                }

                try {
                    startActivityForResult(pickVideo, SELECT_MEDIA_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // display error state to the user
                }
                break;
            case MENU_RECORD_VIDEO:
                Log.i(TAG, "selectMedia. record video");
                Intent recordVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                List<ResolveInfo> listVideosCams = getPackageManager().queryIntentActivities(recordVideoIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (listVideosCams.size() <= 0) {
                    Log.i(TAG, "No camera app to handel the intent");
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    Toast.makeText(ActivityAlarmSettings.this, R.string.no_camera_app_error, Toast.LENGTH_LONG).show();
                    return;
                }
                // Create a file location to tell the camera to save the new photo at it
                mMediaUri = createVideoUri(this);
                if(mMediaUri != null){
                    recordVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri); // Pass the new file uri in the intent extras
                }
                Log.d(TAG, "mMediaUri= "+mMediaUri);
                try {
                    startActivityForResult(recordVideoIntent, SELECT_MEDIA_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // display error state to the user
                }
                break;
        }

        //Don't enable capturing photos by camera without the permission
        /*if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            // Enable camera
            Matisse.from(this)
                    .choose(MimeType.ofAll(), false)
                    .theme(R.style.Matisse_Dracula)
                    .countable(false)
                    .maxSelectable(1)
                    .capture(true)
                    //.captureStrategy(new CaptureStrategy(true, BuildConfig.APPLICATION_ID +".fileprovider", "Basbes"))
                    .captureStrategy(new CaptureStrategy(false, APP_AUTHORITY))
                    .showSingleMediaType(false)
                    //.addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                    //.gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.album_item_height))
                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    .thumbnailScale(0.85f)
                    .imageEngine(new MyGlideEngine())
                    .showPreview(true) // Default is `true`
                    .autoHideToolbarOnSingleTap(true)
                    .forResult(SELECT_MEDIA_REQUEST_CODE);
        }else{
            // Disable camera
            Matisse.from(this)
                    .choose(MimeType.ofAll(), false)
                    .theme(R.style.Matisse_Dracula)
                    .countable(false)
                    .maxSelectable(1)
                    .showSingleMediaType(false)
                    //.addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                    //.gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.album_item_height))
                    .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    .thumbnailScale(0.85f)
                    .imageEngine(new MyGlideEngine())
                    .showPreview(true) // Default is `true`
                    .autoHideToolbarOnSingleTap(true)
                    .forResult(SELECT_MEDIA_REQUEST_CODE);
        }*/


        /*Album.album(this) // Image and video mix options.
                .singleChoice() // Multi-Mode, Single-Mode: singleChoice().
                .requestCode(SELECT_Media_REQUEST) // The request code will be returned in the listener.
                .columnCount(SELECT_MULTIMEDIA) // The number of columns in the page list.
                //.selectCount(1)  // Choose up to a few images.
                .camera(true) // Whether the camera appears in the Item.
                .onResult(new Action<ArrayList<AlbumFile>>() {
                    @Override
                    public void onAction(int requestCode, @NonNull ArrayList<AlbumFile> result) {
                        mMediaFiles = result;
                        AlbumFile albumFile =mMediaFiles.get(0);
                        mMediaType = albumFile.getMediaType();
                        mMediaName  = albumFile.getName();
                        Uri MediaUri = Uri.parse(albumFile.getPath()) ;

                        Log.d(TAG, "MediaType" +albumFile.getMediaType());
                        Log.d(TAG, "Name" +albumFile.getName());
                        Log.d(TAG, "Path" +albumFile.getPath());
                        Log.d(TAG, "MediaUri" +MediaUri);


                        if (mMediaName.length() == 0) {
                            mMediaName = getString(R.string.unknown_name);
                        }

                        if (mMediaType == AlbumFile.TYPE_IMAGE){

                            cropImage(MediaUri);
                        }else {
                            settings.setMedia(MediaUri,mMediaName ,"Video");// Set photo url on media getResources().getString(R.string.media_video)
                            settings.setTone(MediaUri, mMediaName);// Set video url on tone
                            settingsAdapter.notifyDataSetChanged();
                        }

                        if (progressDialog != null) {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }
                    }
                })
                .onCancel(new Action<String>() {
                    @Override
                    public void onAction(int requestCode, @NonNull String result) {
                        // The user canceled the operation.
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }

                    }
                })
                .start();*/
    }

    private void cropImage(Uri mediaUri) {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d(TAG, "width= " +width + "height= "+height);

        //CropImage.activity(Uri.fromFile(new File(mediaUri.toString())))
        CropImage.activity(mediaUri)
                //.setGuidelines(CropImageView.Guidelines.ON)
                //.setOutputUri(Uri.parse(mCroppdImage.getPath()))
                .setAspectRatio(width,height)
                .setAllowRotation(true)
                .setAutoZoomEnabled(true)
                //.setFixAspectRatio(true)
                //.setMultiTouchEnabled(true)
                //.setMaxCropResultSize(width, height)
                .setMinCropResultSize(260,260)
                .setRequestedSize(width,height) //resize
                .start(ActivityAlarmSettings.this);
    }



    private void showProgressDialog() {
        progressDialog = ProgressDialog.show(ActivityAlarmSettings.this,
                getString(R.string.loading),
                getString(R.string.please_wait), true, true);
    }

  /**
   * A helper interface to encapsulate the data displayed in the list view of
   * this activity.  Consists of a setting name, a setting value, and a type.
   * The type is used to trigger the appropriate action from the onClick
   * handler.
   */
  private abstract class Setting {
    public abstract String name();
    public abstract String value();
    public abstract SettingType type();
  }

    /**
     * This adapter populates the settings_items view with the data encapsulated
     * in the individual Setting objects.
     */
    private final class SettingsAdapter extends ArrayAdapter<Setting> {

        List<Setting> settingsObjects;

        public SettingsAdapter(Context context, List<Setting> settingsObjects) {
            super(context, 0, settingsObjects);

            this.settingsObjects = settingsObjects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

            LayoutInflater inflater = getLayoutInflater();

            if (settingsObjects.get(position).name().
                    equalsIgnoreCase(getString(R.string.vibrate))) {
                convertView = inflater.inflate(R.layout.vibrate_settings_item, parent,
                        false);
            }else if(settingsObjects.get(position).name().
                    equalsIgnoreCase(getString(R.string.toggle_button))){
                convertView = inflater.inflate(R.layout.toggled_settings_item, parent,false);
            }else if (settingsObjects.get(position).name().
                    equalsIgnoreCase(getString(R.string.show_button))){
                convertView = inflater.inflate(R.layout.button_shown_settings_item, parent,false);

            }else if (settingsObjects.get(position).name().
                    equalsIgnoreCase(getString(R.string.visibility_button))){
                convertView = inflater.inflate(R.layout.visibility_settings_item, parent,false);

            }else {
                convertView = inflater.inflate(R.layout.settings_item, parent,
                        false);
            }

            holder = new ViewHolder(convertView);

            convertView.setTag(holder);

            if (settingsObjects.get(position).name().
                    equalsIgnoreCase(getString(R.string.tone))) {

                if(settings.getMediaType().equals("Video")){
                    convertView.setAlpha(0.2f);
                }else {
                    convertView.setAlpha(1);
                    //holder.row.setAlpha(0.2f);
                }
            }

            /*if (settingsObjects.get(position).name().
                    equalsIgnoreCase(getString(R.string.visibility_button))) {

                if(settings.getShown()){
                    convertView.setAlpha(1);
                }else {
                    convertView.setAlpha(0.2f);
                    //holder.row.setAlpha(0.2f);
                }
            }*/

			holder.populateFrom(settingsObjects.get(position));

			return(convertView);
        }

    }

	private class ViewHolder {

        private View row;

        private SeekBar seekBar;


        ViewHolder(View row) {
            this.row = row;
		}

		void populateFrom(Setting setting) {
            ((TextView) row.findViewById(R.id.setting_name)).
                    setText(setting.name());

            if (setting.name().equalsIgnoreCase(getString(R.string.vibrate))) {
                SwitchCompat vibrateSwitch = (SwitchCompat) row.findViewById(
                        R.id.setting_vibrate_sc);

                if (settings.getVibrate()) {
                    vibrateSwitch.setChecked(true);
                } else {
                    vibrateSwitch.setChecked(false);
                }

                vibrateSwitch.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            settings.setVibrate(true);
                        } else {
                            settings.setVibrate(false);
                        }
                    }
                });
            }else if(setting.name().equalsIgnoreCase(getString(R.string.toggle_button))) {

                ((TextView) row.findViewById(R.id.setting_value)).
                        setText(setting.value());

                SwitchCompat toggleSwitch = (SwitchCompat) row.findViewById(
                        R.id.setting_toggle_sc);

                if (settings.getToggled()) {
                    toggleSwitch.setChecked(true);
                    Log.d(TAG, "toggleSwitch = " + settings.getToggled());

                } else {
                    toggleSwitch.setChecked(false);
                    Log.d(TAG, "toggleSwitch = " + settings.getToggled());
                }

                toggleSwitch.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    settings.setToggled(true);
                                    Log.d(TAG, "toggleSwitch = " + true);

                                } else {
                                    settings.setToggled(false);
                                    Log.d(TAG, "toggleSwitch = " + false);
                                }
                            }
                        });

            }else if(setting.name().equalsIgnoreCase(getString(R.string.show_button))) {

                ((TextView) row.findViewById(R.id.setting_value)).
                        setText(setting.value());

                SwitchCompat showSwitch = (SwitchCompat) row.findViewById(
                        R.id.setting_show_button_sc);


                if (settings.getShown()) {
                    showSwitch.setChecked(true);
                } else {
                    showSwitch.setChecked(false);
                }

                showSwitch.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                SeekBar disSeekBar = findViewById(R.id.setting_seekBar_sc); // to disable visibility seek bar

                                if (isChecked) {
                                    settings.setShown(true);
                                    disSeekBar.setEnabled(true);
                                } else {
                                    settings.setShown(false);
                                    disSeekBar.setEnabled(false);
                                }
                            }
                        });

            } else if(setting.name().equalsIgnoreCase(getString(R.string.visibility_button))){

                ((TextView) row.findViewById(R.id.setting_value)).
                        setText(setting.value());

                SeekBar seekBar =  row.findViewById(R.id.setting_seekBar_sc);

                if (settings.getShown()) {
                    seekBar.setEnabled(true);
                } else {
                    seekBar.setEnabled(false);
                }

                seekBar.setProgress(settings.getVisibility());

                seekBar.setOnSeekBarChangeListener(
                        new SeekBar.OnSeekBarChangeListener() {
                            int progressValue;

                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                progressValue = progress;
                                settings.setVisibility(progressValue);
                                ((TextView) row.findViewById(R.id.setting_value)).
                                        setText(""+progressValue);

                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                                settings.setVisibility(progressValue);
                            }
                        }
                );

            }else {
                ((TextView) row.findViewById(R.id.setting_value)).
                        setText(setting.value());

            }
		}

	}

    private void showDialogFragment(int id) {
        DialogFragment dialog = new ActivityDialogFragment().newInstance(
                id);
        dialog.show(getFragmentManager(), "ActivityDialogFragment");
    }

    private void showAudioDialog(String title, String[] items , String cancel) {

        SharedPreferences sharedPref = PreferenceManager.
                getDefaultSharedPreferences(getBaseContext());

        String theme = sharedPref.getString(AppSettings.APP_THEME_KEY, "0");
        Drawable icon = null;
        Log.d(TAG, "theme= " + theme);
        switch (theme) {
            case "0":
                icon= getResources().getDrawable(R.drawable.ic_mic_white_36dp);
                break;
            case "1":
                icon =  getResources().getDrawable(R.drawable.ic_mic_black_36dp);
                break;
            case "2":
                icon =  getResources().getDrawable(R.drawable.ic_mic_black_36dp);
                break;
        }

        new MaterialDialog.Builder(this)
                .title(title)
                .items(items)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        Log.d(TAG, "which= " + which);
                        Log.d(TAG, "CharSequence= " + text);
                        switch (which) {
                            case 0://Select a file

                                if (ContextCompat.checkSelfPermission(ActivityAlarmSettings.this,
                                        Manifest.permission.READ_EXTERNAL_STORAGE)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    showProgressDialog();
                                    showDialogFragment(TONE_PICKER);
                                } else {
                                    requestReadExternalStoragePermission();
                                }

                                break;
                            case 1: //Record via microphone
                                if(checkMicPermissions()) {
                                    recordAudio();
                                }
                                break;
                        }

                    }
                })
                .negativeText(cancel)
                .icon(icon)
                .show();
    }

    private boolean checkMicPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API level 29 Android 10 and higher Don't need WRITE_EXTERNAL_STORAGE permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)== PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WAKE_LOCK)== PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestMicPermissions();
                return false;
            }
        }else{
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)== PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WAKE_LOCK)== PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestMicPermissions();
                return false;
            }
        }

    }

    private void requestMicPermissions() {
        // Permission has not been granted and must be requested.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API level 29 Android 10 and higher don't need WRITE_EXTERNAL_STORAGE permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO ) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WAKE_LOCK)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                showDialogFragment(EXPLAIN_MIC_PERMISSION);
            } else {
                // No explanation needed; request the permission
                //Snackbar.make(findViewById(R.id.settings), R.string.mic_unavailable, Snackbar.LENGTH_SHORT).show();
                // Request the permission. The result will be received in onRequestPermissionResult().
                ActivityCompat.requestPermissions(ActivityAlarmSettings.this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO,
                                android.Manifest.permission.WAKE_LOCK},
                        MIC_PERMISSION_REQUEST_CODE);
            }

        }else{
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO ) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WAKE_LOCK)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // Display a SnackBar with cda button to request the missing permission.
                showDialogFragment(EXPLAIN_MIC_PERMISSION);
            /*Snackbar.make(findViewById(R.id.settings ), R.string.mic_permission_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(ActivityAlarmSettings.this,
                            new String[]{android.Manifest.permission.RECORD_AUDIO,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.WAKE_LOCK},
                            MIC_PERMISSION_REQUEST_CODE);
                }
            }).show();*/

            } else {
                // No explanation needed; request the permission
                //Snackbar.make(findViewById(R.id.settings), R.string.mic_unavailable, Snackbar.LENGTH_SHORT).show();
                // Request the permission. The result will be received in onRequestPermissionResult().
                ActivityCompat.requestPermissions(ActivityAlarmSettings.this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                android.Manifest.permission.WAKE_LOCK},
                        MIC_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // If Write to storage permissions are granted return true so that we stop asking for permissions
    private boolean isWritePermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "is permission Granted= "+ true); // API 29 doesn't need write permission
            return true;
        }else{
            Log.d(TAG, "is permission Granted= "+(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }

    }

    private void requestWritePermission(int type) {
        // Permission is not granted
        // Should we show an explanation?
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.i(TAG, "requestPermission: permission should show Rationale");
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showPermissionRationaleDialog(type);
            } else {
                // No explanation needed; request the permission
                Log.i(TAG, "requestPermission: No explanation needed; request the permission");
                // using requestPermissions(new String[] instead of ActivityCompat.requestPermissions(this, new String[] to get onRequestPermissionsResult in the fragment
                // Use different request code for photo and video so when permission is granted we know whether we should take photo or record video
                if(type == MENU_CAPTURE_PICTURE){
                    ActivityCompat.requestPermissions(ActivityAlarmSettings.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PHOTO_PERMISSIONS_CODE);
                }else{
                    ActivityCompat.requestPermissions(ActivityAlarmSettings.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_VIDEO_PERMISSIONS_CODE);
                }
            }
        }
    }

    private void showPermissionRationaleDialog(int type) {
        // Use different request code for photo and video so when permission is granted we know whether we should take photo or record video
        CameraPermissionAlertFragment permissionRationaleDialog = CameraPermissionAlertFragment.newInstance(this, this,  type);
        permissionRationaleDialog.show(mFragmentManager, PERMISSION_RATIONALE_FRAGMENT);
        Log.i(TAG, "showPermissionRationaleDialog: permission AlertFragment show clicked ");
    }

    // When Permission Rationale Dialog dialog is clicked
    @Override
    public void onClick(View view, int position, boolean isLongClick) {
        Log.d(TAG, "item clicked position= " + position + " View= "+view);
        if(view == null && position == MENU_CAPTURE_PICTURE){
            // User grant the permission after selecting capture a photo
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // OK button of the permission dialog is clicked, lets ask for permissions
                ActivityCompat.requestPermissions(ActivityAlarmSettings.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PHOTO_PERMISSIONS_CODE);
            }
        }else if(view == null && position == MENU_RECORD_VIDEO){
            // User grant the permission after selecting record a video
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // OK button of the permission dialog is clicked, lets ask for permissions
                ActivityCompat.requestPermissions(ActivityAlarmSettings.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_VIDEO_PERMISSIONS_CODE);
            }
        }
    }

    private void recordAudio() {
        //String filePath = Environment.getExternalStorageDirectory() + "/recorded_audio.wav";
        // Create a file location to tell the camera to save the new photo at it
        if(mOutputFile == null){
            Toast.makeText(ActivityAlarmSettings.this, R.string.no_audio_file_error,
                    Toast.LENGTH_LONG).show();
            return;
        }
        /*Intent recordAudioIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);

            // Ensure that there's a recorders activity to handle the intent
            List<ResolveInfo> listRecorders = getPackageManager().queryIntentActivities(recordAudioIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (listRecorders.size() <= 0) {
                Log.i(TAG, "No listRecorders app to handel the intent");
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
                Toast.makeText(ActivityAlarmSettings.this, R.string.no_camera_app_error, Toast.LENGTH_LONG).show();
                //return;
            }

            if(mMediaUri != null){
                recordAudioIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri); // Pass the new file uri in the intent extras
            }
            Log.d(TAG, "mMediaUri= "+mMediaUri);
            try {
                startActivityForResult(recordAudioIntent, REQUEST_RECORD_AUDIO_CODE);
            } catch (ActivityNotFoundException e) {
                // display error state to the user
            }
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.OGG);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            ContentResolver resolver = this.getContentResolver();
            try {
                ParcelFileDescriptor parcelFileDescriptor = resolver.openFileDescriptor(mRecordedAudioUri, "rwt");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                mRecorder.setOutputFile(fileDescriptor);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                mRecorder.prepare();
                mRecorder.start();   // Recording is now started
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Recorder error="+e);
            }

            new Thread(new Runnable() {
                int sleepCounter = 0;
                @Override
                public void run() {
                    try {
                        while(sleepCounter <=10) {
                            //Keep looping as long as items size is 0
                            Thread.sleep(1000);
                            Log.d(TAG, "Sleep for one more second. sleepCounter="+sleepCounter++);
                            if(sleepCounter == 10){
                                break;
                            }
                            //handler.post(this);
                        }
                        Log.d(TAG, "Stop recording. as sleepCounter= "+ sleepCounter);
                        mRecorder.pause();
                        mRecorder.stop();
                        //recorder.reset();   // You can reuse the object by going back to setAudioSource() step
                        mRecorder.release(); // Now the object cannot be reused

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();*/

            /*ContentResolver resolver = this.getContentResolver();
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                parcelFileDescriptor = resolver.openFileDescriptor(uri, "rw");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }*/
        //mRecordedAudio = Uri.parse(getRealPathFromURI(uri, this));
        //File file = new File(uri.getPath(), mOutputFile.getName());
        //mRecordedAudio = new File(this.getExternalCacheDir(), mOutputFile.getName());
            /* mRecordedAudio = new File(String.valueOf(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)), mOutputFile.getName());
            try {
                mRecordedAudio.createNewFile();
                OutputStream outputStream = new FileOutputStream(mRecordedAudio);
                InputStream inputStream = this.getContentResolver().openInputStream(uri);
                FileUtils.copy(inputStream, outputStream); //Simply reads input to output stream
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        Log.d(TAG, "recordAudio: mOutputFile getAbsolutePath= "+String.valueOf(mOutputFile.getAbsolutePath()));
        //Log.d(TAG, "recordAudio: resolver mMediaUri= "+mMediaUri);
        int color = getResources().getColor(R.color.colorSecondary);
        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(mOutputFile.getAbsolutePath())//
                .setColor(color)
                .setRequestCode(REQUEST_RECORD_AUDIO_CODE)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.STEREO)
                .setSampleRate(AudioSampleRate.HZ_48000)
                //.setAutoStart(true)
                .setKeepDisplayOn(true)

                // Start recording
                .record();

    }

    public static class ActivityDialogFragment extends DialogFragment {

    public ActivityDialogFragment newInstance(int id) {
      ActivityDialogFragment fragment = new ActivityDialogFragment();
      Bundle args = new Bundle();
      args.putInt("id", id);
      fragment.setArguments(args);
      return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      switch (getArguments().getInt("id")) {
          case NAME_PICKER:
              final View nameView = View.inflate(getActivity(),
                      R.layout.name_settings_dialog, null);
              final TextView label = (TextView) nameView.findViewById(R.id.name_label);
              label.setText(info.getName());
              final AlertDialog.Builder nameBuilder = new AlertDialog.Builder(getActivity());
              nameBuilder.setTitle(R.string.alarm_label);
              nameBuilder.setView(nameView);
              nameBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      info.setName(label.getEditableText().toString());
                      settingsAdapter.notifyDataSetChanged();
                      dismiss();
                  }
              });
              nameBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      dismiss();
                  }
              });
              return nameBuilder.create();

          case DOW_PICKER:
              final AlertDialog.Builder dowBuilder = new AlertDialog.Builder(getActivity());
              dowBuilder.setTitle(R.string.scheduled_days);
              dowBuilder.setMultiChoiceItems(
                      info.getTime().getDaysOfWeek().names(getActivity()),
                      info.getTime().getDaysOfWeek().bitmask(),
                      new OnMultiChoiceClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                              if (isChecked) {
                                  info.getTime().getDaysOfWeek().addDay(Week.Day.values()[which]);
                              } else {
                                  info.getTime().getDaysOfWeek().removeDay(Week.Day.values()[which]);
                              }
                              settingsAdapter.notifyDataSetChanged();
                          }
                      });
              dowBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      dismiss();
                  }
              });
              return dowBuilder.create();

          case TONE_PICKER:
              MediaPickerDialog mediaPicker = new MediaPickerDialog(getActivity());
              mediaPicker.setOnShowListener(new DialogInterface.OnShowListener() {
                  @Override
                  public void onShow(DialogInterface dialog) {
                      if (progressDialog != null) {
                          progressDialog.dismiss();

                          progressDialog = null;
                      }
                  }
              });
              mediaPicker.setPickListener(new MediaPickerDialog.OnMediaPickListener() {
                  @Override
                  public void onMediaPick(String name, Uri media) {
                      if (name.length() == 0) {
                          name = getString(R.string.unknown_name);
                      }
                      settings.setTone(media, name);
                      settingsAdapter.notifyDataSetChanged();
                  }
              });
              return mediaPicker;

          case SNOOZE_PICKER:
              // This currently imposes snooze times between 1 and 60 minutes,
              // which isn't really necessary, but I think the picker is easier
              // to use than a free-text field that you have to type numbers into.
              final CharSequence[] items = new CharSequence[60];
              // Note the array index is one-off from the value (the value of 1 is
              // at index 0).
              for (int i = 1; i <= 60; ++i) {
                  items[i-1] = Integer.toString(i);
              }
              final AlertDialog.Builder snoozeBuilder = new AlertDialog.Builder(getActivity());
              snoozeBuilder.setTitle(R.string.snooze_minutes);
              snoozeBuilder.setSingleChoiceItems(items, settings.getSnoozeMinutes() - 1,
                      new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int item) {
                              settings.setSnoozeMinutes(item + 1);
                              settingsAdapter.notifyDataSetChanged();
                              dismiss();
                          }
                      });
              return snoozeBuilder.create();

          case VOLUME_FADE_PICKER:
              final View fadeView = View.inflate(getActivity(), R.layout.fade_settings_dialog, null);

              final SeekBar seekBar = (SeekBar) fadeView.findViewById(R.id.volume_start_sb);

              seekBar.setMax(100);

              seekBar.setProgress(settings.getVolumeStartPercent());

              final TextView textView = (TextView) fadeView.findViewById(
                      R.id.volume_start_tv);

              textView.setText(getString(R.string.volume_start_label,
                      settings.getVolumeStartPercent()));

              seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                  @Override
                  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                      textView.setText(getString(R.string.volume_start_label,
                              progress));
                  }

                  @Override
                  public void onStartTrackingTouch(SeekBar seekBar) {

                  }

                  @Override
                  public void onStopTrackingTouch(SeekBar seekBar) {

                  }
              });

              final SeekBar endSeekBar = (SeekBar) fadeView.findViewById(R.id.volume_end_sb);

              endSeekBar.setMax(100);

              endSeekBar.setProgress(settings.getVolumeEndPercent());

              final TextView endTextView = (TextView) fadeView.findViewById(
                      R.id.volume_end_tv);

              endTextView.setText(getString(R.string.volume_end_label,
                      settings.getVolumeEndPercent()));

              endSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                  @Override
                  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                      endTextView.setText(getString(R.string.volume_end_label,
                              progress));
                  }

                  @Override
                  public void onStartTrackingTouch(SeekBar seekBar) {

                  }

                  @Override
                  public void onStopTrackingTouch(SeekBar seekBar) {

                  }
              });

              final SeekBar durationSeekBar = (SeekBar) fadeView.findViewById(
                      R.id.volume_duration_sb);

              durationSeekBar.setMax(300);

              durationSeekBar.setProgress(settings.getVolumeChangeTimeSec());

              final TextView durationTextView = (TextView) fadeView.findViewById(
                      R.id.volume_duration_tv);

              durationTextView.setText(getString(R.string.volume_duration_label,
                              settings.getVolumeChangeTimeSec()));

              durationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                  @Override
                  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                      durationTextView.setText(getString(R.string.volume_duration_label,
                                      progress));
                  }

                  @Override
                  public void onStartTrackingTouch(SeekBar seekBar) {

                  }

                  @Override
                  public void onStopTrackingTouch(SeekBar seekBar) {

                  }
              });

              final AlertDialog.Builder fadeBuilder = new AlertDialog.Builder(getActivity());
              fadeBuilder.setTitle(R.string.alarm_fade);
              fadeBuilder.setView(fadeView);
              fadeBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      settings.setVolumeStartPercent(seekBar.getProgress());
                      settings.setVolumeEndPercent(endSeekBar.getProgress());
                      settings.setVolumeChangeTimeSec(durationSeekBar.getProgress());

                      settingsAdapter.notifyDataSetChanged();

                      dismiss();
                  }
              });
              fadeBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      dismiss();
                  }
              });
              return fadeBuilder.create();

          case DELETE_CONFIRM:
              final AlertDialog.Builder deleteConfirmBuilder = new AlertDialog.Builder(getActivity());
              deleteConfirmBuilder.setTitle(R.string.delete);
              deleteConfirmBuilder.setMessage(R.string.confirm_delete);
              deleteConfirmBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      service.deleteAlarm(alarmId);
                      dismiss();
                      getActivity().finish();
                  }
              });
              deleteConfirmBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      dismiss();
                  }
              });
              return deleteConfirmBuilder.create();
        case EXPLAIN_READ_EXTERNAL_STORAGE:
            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    getActivity());

            builder.setTitle(R.string.read_external_storage_title);

            builder.setMessage(R.string.read_external_storage_message);

            builder.setPositiveButton(R.string.proceed,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();

                    _requestReadExternalStoragePermission(getActivity());
                }
            });

            builder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });

            return builder.create();
          case EXPLAIN_MIC_PERMISSION:
              final AlertDialog.Builder micBuilder = new AlertDialog.Builder(
                      getActivity());

              micBuilder.setTitle(R.string.access_mic);

              micBuilder.setMessage(R.string.mic_permission_required);

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  // API level 29 Android 10 and higher don't need WRITE_EXTERNAL_STORAGE permission
                  micBuilder.setPositiveButton(R.string.proceed,
                          new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialog, int which) {
                                  dismiss();
                                  // Request the permission
                                  ActivityCompat.requestPermissions(getActivity(),
                                          new String[]{android.Manifest.permission.RECORD_AUDIO,
                                                  android.Manifest.permission.WAKE_LOCK},
                                          MIC_PERMISSION_REQUEST_CODE);
                              }
                          });
              }else{
                  micBuilder.setPositiveButton(R.string.proceed,
                          new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialog, int which) {
                                  dismiss();
                                  // Request the permission
                                  ActivityCompat.requestPermissions(getActivity(),
                                          new String[]{android.Manifest.permission.RECORD_AUDIO,
                                                  android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                  android.Manifest.permission.WAKE_LOCK},
                                          MIC_PERMISSION_REQUEST_CODE);
                              }
                          });
              }

              micBuilder.setNegativeButton(R.string.cancel,
                      new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                              dismiss();
                          }
                      });

              return micBuilder.create();
        case PERMISSION_NOT_GRANTED:
            final AlertDialog.Builder permissionBuilder = new AlertDialog.Builder(
                    getActivity());

            permissionBuilder.setTitle(R.string.permission_not_granted_title);

            permissionBuilder.setMessage(R.string.permission_not_granted);

            permissionBuilder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });

            return permissionBuilder.create();
        default:
          return super.onCreateDialog(savedInstanceState);
      }
    }

  }
}
