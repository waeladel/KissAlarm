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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;



/**
 * This is the activity responsible for alerting the user when an alarm goes
 * off.  It is the activity triggered by the NotificationService.  It assumes
 * that the intent sender has acquired a screen wake lock.
 * NOTE: This class assumes that it will never be instantiated nor active
 * more than once at the same time. (ie, it assumes
 * android:launchMode="singleInstance" is set in the manifest file).
 */
public final class ActivityAlarmNotification extends AppCompatActivity implements SurfaceHolder.Callback , TextureView.SurfaceTextureListener{

    private static String TAG = ActivityAlarmNotification.class.getSimpleName();

    public static final String TIMEOUT_COMMAND = "timeout";

    public static final int TIMEOUT = 0;

    private NotificationServiceBinder notifyService;
    private DbAccessor db;
    private Handler handler;
    private Runnable timeTick;
    //private VideoView mVideoView;
    private SurfaceView mSurfaceView;
    private TextureView mTextureView;
    private GraphicOverlay mGraphicOverlay;
    private CameraSourcePreview mPreview;
    private CameraSource mCameraSource = null;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;


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

        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
        //mSurfaceView = findViewById(R.id.surfaceView);
        mTextureView = findViewById(R.id.textureView);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }


        mTextureView.setSurfaceTextureListener(this);
        /*mSurfaceHolder = mSurfaceView.getHolder();
        mSurface = mSurfaceHolder.getSurface();

        Log.d(TAG, "mamaMediaPlayer= mSurfaceHolder= "+mSurfaceView.getHolder());
        mSurfaceHolder.addCallback(this);*/

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
                        mTextureView.setVisibility(View.VISIBLE);


                    }else if(service.getMediaType().equalsIgnoreCase("Photo")){
                        //mImageView.setVisibility(View.VISIBLE);
                        mTextureView.setVisibility(View.INVISIBLE);

                        //mImageView.setImageURI(service.getPhotoUri());
                        //mBitmap = BitmapFactory.decodeFile(service.getPhotoUri().toString());

                        try {
                            InputStream stream = getContentResolver().openInputStream(service.getPhotoUri());
                            mBitmap = BitmapFactory.decodeStream(stream);
                            new FaceDetectorAsyncTask().execute(mBitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

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
        startCameraSource();
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

    //=============================================================================================
    // Activity Methods
    //==============================================================================================

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                try {
                    service.setPlayerSurface(mSurface);
                    mBitmap = mTextureView.getBitmap();
                    //new FaceDetectorAsyncTask().execute(mBitmap);
                    createCameraSource();

                } catch (RemoteException e) {
                    //return;
                }
            }
        });
        /*mMediaPlayer = NotificationService.MediaSingleton.INSTANCE.mediaPlayer;
        mMediaPlayer.setDisplay(mSurfaceHolder);*/
        Log.d(TAG, "mamaMediaPlayer surfaceCreated + MediaPlayer=" + mMediaPlayer);
        //startCameraSource();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
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
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

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

            // Create a frame from the bitmap and run face detection on the frame.
            mFrame = new Frame.Builder().setBitmap(bitmapParams[0]).build();
            mFaces = mDetector.detect(mFrame);
            Log.d(TAG, "faces frame.");

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

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }
    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
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

        mCameraSource = new CameraSource.Builder(context, detector)
                //.setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

        // Create a frame from the bitmap and run face detection on the frame.
        //mFrame = new Frame.Builder().setBitmap(bitmap).build();
        //mFaces = mDetector.detect(mFrame);
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

}
