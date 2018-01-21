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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.InputStream;



/**
 * This is the activity responsible for alerting the user when an alarm goes
 * off.  It is the activity triggered by the NotificationService.  It assumes
 * that the intent sender has acquired a screen wake lock.
 * NOTE: This class assumes that it will never be instantiated nor active
 * more than once at the same time. (ie, it assumes
 * android:launchMode="singleInstance" is set in the manifest file).
 */
public final class ActivityAlarmNotification extends AppCompatActivity implements SurfaceHolder.Callback {

    private static String TAG = ActivityAlarmNotification.class.getSimpleName();

    public static final String TIMEOUT_COMMAND = "timeout";

    public static final int TIMEOUT = 0;

    private NotificationServiceBinder notifyService;
    private DbAccessor db;
    private Handler handler;
    private Runnable timeTick;
    //private VideoView mVideoView;
    private SurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;
    private Surface mSurface;
    private FaceView overlay;
   //private ImageView mImageView;

    private InputStream mStream ;
    private Bitmap mBitmap ;
    private SparseArray<Face> mFaces;
    private FaceDetector mDetector;
    private Frame mFrame ;
    private static ProgressDialog progressDialog;
    public static Button lipButton;// lip button for onClick listener
    public static LinearLayout lipLayout; // lip button layout to control it's width and height

    public static Button foreheadButton;// lip button for onClick listener
    public static LinearLayout foreheadLayout; // lip button layout to control it's width and height

    //private FrameLayout fl_surfaceview_container;

    // Dialog state
    int snoozeMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.
                getDefaultSharedPreferences(getBaseContext());

        String theme = sharedPref.getString(AppSettings.APP_THEME_KEY, "0");

        switch (theme) {
            case "1":
                setTheme(R.style.AppThemeLightNoActionBar);
                break;
            case "2":
                setTheme(R.style.AppThemeLightNoActionBar);
                break;
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.notification);

        // Make sure this window always shows over the lock screen.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        db = new DbAccessor(getApplicationContext());

        // Start the notification service and bind to it.
        notifyService = new NotificationServiceBinder(getApplicationContext());

        notifyService.bind();
        // Setup a self-scheduling event loops.
        handler = new Handler();
        timeTick = new Runnable() {
            @Override
            public void run() {
                notifyService.call(new NotificationServiceBinder.
                        ServiceCallback() {
                    @Override
                    public void run(NotificationServiceInterface service) {
                        /*try {
                            TextView volume = (TextView)
                                    findViewById(R.id.volume);

                            String volumeText = "Volume: " + service.volume();

                            volume.setText(volumeText);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }*/

                        long next = AlarmUtil.millisTillNextInterval(
                                AlarmUtil.Interval.SECOND);

                        handler.postDelayed(timeTick, next);
                    }
                });
            }
        };
        Log.d(TAG, "mamaMediaPlayer= onCreate");

        // Setup individual UI elements.

        //mVideoView = findViewById(R.id.videoView);
        /*fl_surfaceview_container =
                (FrameLayout)findViewById(R.id.fragment_file_videoplayer_surface_container);
        mSurfaceView = new SurfaceView(this);
        fl_surfaceview_container.addView(mSurfaceView);*/

        //mImageView = findViewById(R.id.imageView);
        //the layout on which you are working
        lipButton = findViewById(R.id.lip_button);
        lipLayout = (LinearLayout) findViewById(R.id.lip_Layout);

        foreheadButton = findViewById(R.id.forehead_button);
        foreheadLayout = (LinearLayout) findViewById(R.id.forehead_Layout);

        overlay = findViewById(R.id.faceView);
        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurface = mSurfaceHolder.getSurface();

        Log.d(TAG, "mamaMediaPlayer= mSurfaceHolder= "+mSurfaceView.getHolder());
        mSurfaceHolder.addCallback(this);

        /////////code when i was using VideoView instead of SurfaceView/////////

        /*notifyService.call(new NotificationServiceBinder.ServiceCallback() {

            public void run(NotificationServiceInterface service) {
                try {
                    mVideoView.setVideoURI(service.currentTone());
                    Log.d(TAG, "getMediaPlayerUri= "+service.currentTone());
                    Log.d(TAG, "MediaSingleton= "+NotificationService.MediaSingleton.INSTANCE.mediaPlayer);
                    mVideoView.start();
                } catch (RemoteException e) {
                    //return;
                }
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });*/

        final Button snoozeButton = (Button) findViewById(R.id.notify_snooze);

        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyService.acknowledgeCurrentNotification(snoozeMinutes);
                finish();
            }
        });

        /*final Button decreaseSnoozeButton = (Button) findViewById(
                R.id.notify_snooze_minus_five);

        decreaseSnoozeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int snooze = snoozeMinutes - 5;

                if (snooze < 5) {
                    snooze = 5;
                }

                snoozeMinutes = snooze;

                redraw();
            }
        });

        final Button increaseSnoozeButton = (Button) findViewById(
                R.id.notify_snooze_plus_five);

        increaseSnoozeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int snooze = snoozeMinutes + 5;

                if (snooze > 60) {
                    snooze = 60;
                }

                snoozeMinutes = snooze;

                redraw();
            }
        });*/

        lipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "lipButton clicked ");
                notifyService.acknowledgeCurrentNotification(0);
                finish();
            }
        });

        foreheadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "foreheadButton clicked ");
                notifyService.acknowledgeCurrentNotification(snoozeMinutes);
                finish();
            }
        });

        final Slider dismiss = (Slider) findViewById(R.id.dismiss_slider);

        dismiss.setOnCompleteListener(new Slider.OnCompleteListener() {
            @Override
            public void complete() {
                //getMediaPlayerUri();
                notifyService.acknowledgeCurrentNotification(0);
                finish();
            }
        });

        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                try {
                    if(service.getMediaType().equalsIgnoreCase("Video")){
                        //mImageView.setVisibility(View.INVISIBLE);
                        mSurfaceView.setVisibility(View.VISIBLE);
                    }else if(service.getMediaType().equalsIgnoreCase("Photo")){
                        //mImageView.setVisibility(View.VISIBLE);
                        mSurfaceView.setVisibility(View.INVISIBLE);

                        //mImageView.setImageURI(service.getPhotoUri());
                        mBitmap = BitmapFactory.decodeFile(service.getPhotoUri().toString());
                        new FaceDetectorAsyncTask().execute(mBitmap);

                        //mBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.mipmap.girl);
                        //mBitmap = BitmapFactory.decodeStream(stream);
                        //InputStream stream = getResources().openRawResource(R.raw.girlraw);
                        //mBitmap = BitmapFactory.decodeStream(stream);
                        //mImageView.setImageBitmap(mBitmap);

                    }else{
                        mSurfaceView.setVisibility(View.INVISIBLE);
                        mBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.mipmap.girl);
                        new FaceDetectorAsyncTask().execute(mBitmap);
                    }
                    Log.d(TAG, "getMediaType= "+service.getMediaType()+ service.getPhotoUri());
                } catch (RemoteException e) {
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        handler.post(timeTick);

        redraw();

        Log.d(TAG, "mamaMediaPlayer= onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(timeTick);
        //fl_surfaceview_container.removeAllViews();
        Log.d(TAG, "mamaMediaPlayer= onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeConnections();
        notifyService.unbind();
        Log.d(TAG, "mamaMediaPlayer= onDestroy");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();

        if (extras == null || !extras.getBoolean(TIMEOUT_COMMAND, false)) {
            return;
        }

        // The notification service has signaled this activity for a second time.
        // This represents a acknowledgment timeout.  Display the appropriate error.
        // (which also finish()es this activity.
        showDialogFragment(TIMEOUT);
    }

    private void redraw() {
        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            @Override
            public void run(NotificationServiceInterface service) {
                long alarmId;

                try {
                    alarmId = service.currentAlarmId();
                } catch (RemoteException e) {
                    return;
                }

                AlarmInfo alarmInfo = db.readAlarmInfo(alarmId);

                if (snoozeMinutes == 0) {
                    snoozeMinutes = db.readAlarmSettings(alarmId).
                            getSnoozeMinutes();
                }

                String infoTime = "";

                String infoName = "";

                if (alarmInfo != null) {
                    infoTime = alarmInfo.getTime().toString();

                    infoName = alarmInfo.getName();
                }

                /*String info = infoTime + "\n" + infoName;

                if (AppSettings.isDebugMode(getApplicationContext())) {
                    info += " [" + alarmId + "]";

                    findViewById(R.id.volume).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.volume).setVisibility(View.GONE);
                }
                TextView infoText = (TextView) findViewById(R.id.alarm_info);

                infoText.setText(info);

                TextView snoozeInfo = (TextView) findViewById(
                        R.id.notify_snooze_time);

                String snoozeInfoText = getString(R.string.snooze) + "\n"
                        + getString(R.string.minutes, snoozeMinutes);

                snoozeInfo.setText(snoozeInfoText);*/
            }
        });
    }

    private Uri getMediaPlayerUri() {
        final Uri[] alarmId = new Uri[1];
        notifyService.call(new NotificationServiceBinder.ServiceCallback() {

            public void run(NotificationServiceInterface service) {
                try {
                    //MediaPlayer mediaPlayer = service.currentMediaPlayer();
                    Log.d(TAG, "mamaMediaPlayer= "+service.currentTone());
                    alarmId[0] = service.currentTone();
                } catch (RemoteException e) {
                    //return;
                }
            }
        });
        return alarmId[0];
    }


    private void showDialogFragment(int id) {
        DialogFragment dialog = new ActivityDialogFragment().newInstance(
                id);

        dialog.show(getFragmentManager(), "ActivityDialogFragment");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                try {
                    service.setPlayerSurface(mSurface);
                } catch (RemoteException e) {
                    //return;
                }
            }
        });

        /*mMediaPlayer = NotificationService.MediaSingleton.INSTANCE.mediaPlayer;
        mMediaPlayer.setDisplay(mSurfaceHolder);*/
        Log.d(TAG, "mamaMediaPlayer surfaceCreated + MediaPlayer=" + mMediaPlayer);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "mamaMediaPlayer= surfaceChanged");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //mMediaPlayer.setDisplay(null);
        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                try {
                    service.releasePlayerSurfaceHolder(null);
                } catch (RemoteException e) {
                    //return;
                }
            }
        });
        Log.d(TAG, "mamaMediaPlayer= surfaceDestroyed");
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
                case TIMEOUT:
                    final AlertDialog.Builder timeoutBuilder =
                            new AlertDialog.Builder(getActivity());

                    timeoutBuilder.setIcon(android.R.drawable.ic_dialog_alert);

                    timeoutBuilder.setTitle(R.string.time_out_title);

                    timeoutBuilder.setMessage(R.string.time_out_error);

                    timeoutBuilder.setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    });

                    AlertDialog dialog = timeoutBuilder.create();

                    dialog.setOnDismissListener(new DialogInterface.
                            OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            getActivity().finish();
                        }});

                    return dialog;
                default:
                    return super.onCreateDialog(savedInstanceState);
            }
        }

    }

    public class FaceDetectorAsyncTask extends AsyncTask<Bitmap, Void , SparseArray<Face>> {

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected SparseArray<Face> doInBackground(Bitmap... bitmapParams) {

            // By default, landmark detection is not enabled since it increases detection time.  We
            // enable it here in order to visualize detected landmarks.
            mDetector = new FaceDetector.Builder(getApplicationContext())
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .setMode(FaceDetector.FAST_MODE )
                    .setProminentFaceOnly(true)
                    .build();
            Log.d(TAG, "FaceDetector built.");


            // Create a frame from the bitmap and run face detection on the frame.
            mFrame = new Frame.Builder().setBitmap(bitmapParams[0]).build();
            mFaces = mDetector.detect(mFrame);
            Log.d(TAG, "faces frame.");

            if (!mDetector.isOperational()) {
                Log.d(TAG, "Face detector dependencies are not yet available.");

                // Check for low storage.  If there is low storage, the native library will not be
                // downloaded, so detection will not become operational.
                IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                if (hasLowStorage) {
                    //Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                    Log.d(TAG, getString(R.string.tone));
                }
            }

            return mFaces;
        }

        @Override
        protected void onPostExecute(SparseArray<Face> faces) {

            overlay.setVisibility(View.VISIBLE);
            Log.d(TAG, "mBitmap getWidth= " +mBitmap.getWidth());
            overlay.setContent(mBitmap, faces);
            Log.d(TAG, "overlay setContent.");

            // Although detector may be used multiple times for different images, it should be released
            // when it is no longer needed in order to free native resources.
            mDetector.release();
            Log.d(TAG, "detector released.");

            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }

        }
    }

    private void showProgressDialog() {
        progressDialog = ProgressDialog.show(ActivityAlarmNotification.this,
                getString(R.string.Scaning),
                getString(R.string.detecting_you_partner_face), true, true);

    }

}
