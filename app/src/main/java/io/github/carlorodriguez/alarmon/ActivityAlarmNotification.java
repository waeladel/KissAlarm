
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.os.Process;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * This is the activity responsible for alerting the user when an alarm goes
 * off.  It is the activity triggered by the NotificationService.  It assumes
 * that the intent sender has acquired a screen wake lock.
 * NOTE: This class assumes that it will never be instantiated nor active
 * more than once at the same time. (ie, it assumes
 * android:launchMode="singleInstance" is set in the manifest file).
 */
public final class ActivityAlarmNotification extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    private static String TAG = ActivityAlarmNotification.class.getSimpleName();

    public static final String TIMEOUT_COMMAND = "timeout";

    public static final int TIMEOUT = 0;

    private NotificationServiceBinder notifyService;
    private DbAccessor db;
    private Handler handler;
    private Runnable timeTick;
    //private VideoView mVideoView;
    private SurfaceView mSurfaceView;
    private volatile TextureView mTextureView;
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
   private ImageView mImageView;

    private InputStream mStream ;
    private volatile Bitmap mBitmap ;
    private volatile Bitmap TransformBitmap;
    private volatile Bitmap scaledBitmap;
    private SparseArray<Face> mFaces;
    private FaceDetector mDetector;
    private Frame mFrame ;
    private static ProgressDialog progressDialog;
    public static  Button lipButton;// lip button for onClick listener

    public static Button foreheadButton;// lip button for onClick listener
    public  static Button snoozeButton;
    private Button aspectRatio;
    private Button aspectRatioWider;

    public Slider dismiss;


    private volatile Boolean shouldhideButtons;
    private Boolean toastExecuted;

    // Original video size
    private volatile float mVideoWidth;
    private volatile float mVideoHeight;
    private volatile int mRotation;

    public static boolean isToggled;
    public static boolean isShown;
    public static int visibility;




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

        mImageView = findViewById(R.id.imageView);
        //the layout on which you are working
        lipButton = findViewById(R.id.lip_button);
        foreheadButton = findViewById(R.id.forehead_button);

        overlay = findViewById(R.id.faceView);
        //mSurfaceView = findViewById(R.id.surfaceView);
        mTextureView = findViewById(R.id.textureView);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        /*// Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            //createCameraSource();
        } else {
            requestCameraPermission();
        }
*/
        shouldhideButtons = false;
        toastExecuted = false;

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

        aspectRatio = (Button) findViewById(R.id.aspect_ratio);
        aspectRatioWider = (Button) findViewById(R.id.aspect_ratio_wider);

        snoozeButton = (Button) findViewById(R.id.notify_snooze);

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


        dismiss = (Slider) findViewById(R.id.dismiss_slider);

        dismiss.setOnCompleteListener(new Slider.OnCompleteListener() {
            @Override
            public void complete() {
                //getMediaPlayerUri();
                notifyService.acknowledgeCurrentNotification(0);
                finish();
            }
        });

        /*dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "dismiss clicked ");
            }
        });*/

        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                try {
                    if(service.getMediaType().equalsIgnoreCase("Video")){
                        /*//mImageView.setVisibility(View.INVISIBLE);
                        mTextureView.setVisibility(View.VISIBLE);*/

                        //overlay.setVisibility(View.INVISIBLE);
                        mTextureView.setVisibility(View.VISIBLE);

                    }else if(service.getMediaType().equalsIgnoreCase("Photo")){
                        //mImageView.setVisibility(View.VISIBLE);
                        //overlay.setVisibility(View.VISIBLE);
                        mTextureView.setVisibility(View.INVISIBLE);

                        //mImageView.setImageURI(service.getPhotoUri());
                        //mBitmap = BitmapFactory.decodeFile(service.getPhotoUri().toString());

                        try {
                            InputStream stream = getContentResolver().openInputStream(service.getPhotoUri());
                            mBitmap = BitmapFactory.decodeStream(stream);
                            Log.d(TAG, "stream= "+stream+"mBitmap= "+mBitmap);
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
                        //mSurfaceView.setVisibility(View.INVISIBLE);

                        //overlay.setVisibility(View.VISIBLE);
                        mTextureView.setVisibility(View.INVISIBLE);

                        mBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.mipmap.girl);
                        Log.d(TAG, "mama mBitmap= "+mBitmap);
                        new FaceDetectorAsyncTask().execute(mBitmap);
                    }
                    Log.d(TAG, "getMediaType= "+service.getMediaType()+ service.getPhotoUri());
                } catch (RemoteException e) {
                }
            }
        });

        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                long alarmId;

                try {
                    alarmId = service.currentAlarmId();
                    isToggled = db.readAlarmSettings(alarmId).getToggled();
                    isShown = db.readAlarmSettings(alarmId).getShown();
                    visibility = db.readAlarmSettings(alarmId).getVisibility();
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
        releaseSurfaceHolder();
        db.closeConnections();
        notifyService.unbind();
        Log.d(TAG, "mamaMediaPlayer= onDestroy");

        /*if (mDetector!= null){
            mDetector.release();
            Log.d(TAG, "mDetector released onDestroy");
        }*/
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

    /*@Override
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

        *//*mMediaPlayer = NotificationService.MediaSingleton.INSTANCE.mediaPlayer;
        mMediaPlayer.setDisplay(mSurfaceHolder);*//*
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
    }*/

    //=============================================================================================
    // Activity Methods Texture view overrride
    //==============================================================================================

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, final int width, final int height) {
        mSurface = new Surface(surface);
        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                try {
                    service.setPlayerSurface(mSurface);// attach media player to SurfaceTexture
                    calculateVideoSize(service.currentTone());// get  video's with and height
                } catch (RemoteException e) {
                    //return;
                }
            }
        });
        /*mMediaPlayer = NotificationService.MediaSingleton.INSTANCE.mediaPlayer;
        mMediaPlayer.setDisplay(mSurfaceHolder);*/
        Log.d(TAG, "mamaMediaPlayer surfaceCreated + MediaPlayer=" + mMediaPlayer);
        //startCameraSource();

        Log.d(TAG, "mVideoWidth="+ mVideoWidth+ "mVideoHeight="+mVideoHeight);

        //adjustAspectRatio((int) mVideoWidth,(int) mVideoHeight);// function to preserve fixed aspect ratio
        //updateTextureViewSize(mTextureView.getWidth(), mTextureView.getHeight());
        updateTextureViewSize(width, height); //function to center crop the video
        
        //mBitmap = mTextureView.getBitmap();
        //new FaceDetectorAsyncTask().execute(mBitmap);
        createDetector();

        /*//wait 4 seconds
           Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    //detectCurrentBitmap();

                    Mat currentFrame = new Mat(height + height / 2, width + width/2, CvType.CV_8UC1);
                    Mat yuvMat = new Mat(height + height / 2, width + width/2, CvType.CV_8UC1);

                    yuvMat.put(0, 0, data);

                    //Imgproc.cvtColor(yuvMat, currentFrame, Imgproc.COLOR_YUV420sp2RGB);
                    //Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_BGR2GRAY);
                    try {
                        //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
                        //Imgproc.cvtColor(RGBmat, grayMat, Imgproc.COLOR_BGR2GRAY);

                        Bitmap bitmap = Bitmap.createBitmap(currentFrame.cols(), currentFrame.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(currentFrame, bitmap);
                        mImageView.setImageBitmap(bitmap);
                    }
                    catch (CvException e){
                        Log.d("Exception",e.getMessage());
                    }
                    mImageView.setVisibility(View.VISIBLE);
                    mTextureView.setVisibility(View.INVISIBLE);
                }
            }, 4000);*/

    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurface = new Surface(surface);
        //releaseSurfaceHolder();
        mTextureView.setSurfaceTextureListener(null);
        Log.d(TAG, "mama2 onSurfaceTextureDestroyed= surfaceDestroyed");
        return true;
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

            // his should not use AsyncTask.  The AsyncTask worker thread is run at
            // a lower priority, making it unsuitable for benchmarks.  We can counteract
            // it in the current implementation, but this is not guaranteed to work in
            // future releases.
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

            // By default, landmark detection is not enabled since it increases detection time.  We
            // enable it here in order to visualize detected landmarks.
            mDetector = new FaceDetector.Builder(getApplicationContext())
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                    .setMode(FaceDetector.ACCURATE_MODE )
                    .setProminentFaceOnly(true)
                    .setMinFaceSize(0.3f)
                    .build();
            Log.d(TAG, "FaceDetector built.");

            if (!mDetector.isOperational()) {
                Log.d(TAG, "Face detector dependencies are not yet available.");

                // Check for low storage.  If there is low storage, the native library will not be
                // downloaded, so detection will not become operational.
                IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                if (hasLowStorage) {
                    Log.d(TAG, "Sorry, there isn't enough storage.");
                    Toast.makeText(ActivityAlarmNotification.this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                }
            }

            if (bitmapParams[0] != null) {
                // Create a frame from the bitmap and run face detection on the frame.
                mFrame = new Frame.Builder().setBitmap(bitmapParams[0]).build();
                mFaces = mDetector.detect(mFrame);
                Log.d(TAG, "faces frame.");
            }

            return mFaces;
        }

        @Override
        protected void onPostExecute(SparseArray<Face> faces) {

            overlay.setVisibility(View.VISIBLE);
            Log.d(TAG, "mBitmap getWidth= " +mBitmap.getWidth());
            overlay.setContent(mBitmap, faces); // draw original bitmap and face buttons
            Log.d(TAG, "overlay setContent.");

            // Although detector may be used multiple times for different images, it should be released
            // when it is no longer needed in order to free native resources.
            mDetector.release();

            Log.d(TAG, "detector released.");

            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
            Log.d(TAG, "wael faces ="+faces.size());
            if(faces.size() == 0){
                Toast.makeText(ActivityAlarmNotification.this,(R.string.toast_cant_detect_face),
                        Toast.LENGTH_LONG).show();
            }else{
                hideButtons();
            }

        }
    }

    public class VideoFaceDetectorAsyncTask extends AsyncTask<Bitmap, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            //showProgressDialog();
        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitmapParams) {

            // his should not use AsyncTask.  The AsyncTask worker thread is run at
            // a lower priority, making it unsuitable for benchmarks.  We can counteract
            // it in the current implementation, but this is not guaranteed to work in
            // future releases.
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

            if(isCancelled()){
                Log.d(TAG, "VideoFaceDetectorAsyncTask isCancelled="+ isCancelled());
            }else{
                Log.d(TAG, "VideoFaceDetectorAsyncTask isCancelled="+ isCancelled());
                Log.d(TAG, "Before mFrame Builder="+ mFrame);
                if (bitmapParams[0] != null) {
                    mFrame = new Frame.Builder()
                            .setBitmap(bitmapParams[0])
                            .build();
                    Log.d(TAG, "After mFrame Builder="+ mFrame);

                    //mDetector.detect(mFrame);
                    Log.d(TAG, "Before Detector.receiveFrame="+ mFrame+ isCancelled());
                    if(mDetector != null){
                        mDetector.receiveFrame(mFrame);// feed the detector with bitmap
                        Log.d(TAG, "After Detector.receiveFrame="+ mFrame);
                    }
                }
            }

            return bitmapParams[0];
        }

        @Override
        protected void onPostExecute(Bitmap bitmapParams) {

            if (bitmapParams != null) {
                detectCurrentBitmap(); // loop for detecting another frame
            }else{
                if (mDetector!= null){
                    mDetector.release();
                    Log.d(TAG, "mDetector released");
                }
            }

            if (shouldhideButtons){
                hideButtons();
            }

            /*else{
                if(!toastExecuted) {
                    Toast.makeText(ActivityAlarmNotification.this,(R.string.toast_cant_detect_face),
                            Toast.LENGTH_LONG).show();
                    toastExecuted = true;
                }
            }*/

            //wait 1o seconds
           /* Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    //detectCurrentBitmap();
                }
            }, 2000);*/
        }
    }


    private void showProgressDialog() {
        progressDialog = ProgressDialog.show(ActivityAlarmNotification.this,
                getString(R.string.Scanning),
                getString(R.string.detecting_you_partner_face), true, true);

    }


    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createDetector() {

        Context context = getApplicationContext();
        mDetector = new FaceDetector.Builder(context)
                //.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(false)
                .setMinFaceSize(0.3f)
                .build();

        mDetector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!mDetector.isOperational()) {
            Log.d(TAG, "Face detector dependencies are not yet available.");
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(ActivityAlarmNotification.this, R.string.low_storage_error, Toast.LENGTH_LONG).show();

            }
        }

        /*mCameraSource = new CameraSource.Builder(context, mDetector)
                //.setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();*/

        // Create a frame from the bitmap and run face detection on the frame

        //wait 1o seconds
       /* Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
            }
        }, 6000);*/

        Log.d(TAG, "Before getBitmap mTextureView mBitmap="+ mBitmap);
        //mBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.mipmap.girl);
        mBitmap = mTextureView.getBitmap();
        Log.d(TAG, "After getBitmap mTextureView mBitmap="+ mBitmap);
        new VideoFaceDetectorAsyncTask().execute(mBitmap);

        //startCameraSource();

            /*// new Thread Start new thread for face detector and bitmaps
            new Thread(new Runnable() {
                @Override public void run() {
                }
            }).start();*/
    }

    /**
     * detect the newest Bitmap
     */

    private void detectCurrentBitmap() {
        Log.d(TAG, "Before getBitmap mTextureView mBitmap="+ mBitmap);
        mBitmap = mTextureView.getBitmap();
        //mTextureView.getMatrix();

        /*Matrix bitmapMatrix = new Matrix();
        mTextureView.getTransform(bitmapMatrix);*/

        if(mBitmap != null){
            //get hte bitmap's transformation from the TextureView
             TransformBitmap = Bitmap.createBitmap( mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), mTextureView.getTransform(null), true );
            //TransformBitmap= cropCenter(TransformBitmap);
            new VideoFaceDetectorAsyncTask().execute(cropCenter(TransformBitmap));
        }
    }

    /**
     * get the video's original width and height
     */

    private void calculateVideoSize( Uri toneUri) {
        try {
            //AssetFileDescriptor afd = getAssets().openFd(FILE_NAME);
            Log.d(TAG, "calculateVideoSize: video uri= "+toneUri);
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            //metaRetriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            metaRetriever.setDataSource(this, toneUri);
            //metaRetriever.setDataSource(getFileUri(toneUri, this));

            final String height = metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

            final String width = metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

            Log.d(TAG, "cropCenter updateTextureViewSize: width= "+ width + " height= "+height);
            if (Build.VERSION.SDK_INT >= 17) {//17
                mRotation = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                Log.d(TAG,"updateTextureViewSize:  Rotation"+ mRotation);
               if (mRotation == 90 || mRotation == 270){// width and height  are transposed
                    mVideoWidth = Float.parseFloat(height);
                    mVideoHeight = Float.parseFloat(width);
                }else{
                    mVideoHeight = Float.parseFloat(height);
                    mVideoWidth = Float.parseFloat(width);
                }
            }else{
                mVideoHeight = Float.parseFloat(height);
                mVideoWidth = Float.parseFloat(width);
                aspectRatio.setVisibility(View.VISIBLE);
                aspectRatio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "aspectRatio clicked ");
                        mVideoWidth = Float.parseFloat(height);
                        mVideoHeight = Float.parseFloat(width);
                        updateTextureViewSize(mTextureView.getWidth(), mTextureView.getHeight()); //function to center crop the video
                        aspectRatio.setVisibility(View.GONE);
                        aspectRatioWider.setVisibility(View.VISIBLE);
                        //showButtons();
                    }
                });

                aspectRatioWider.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "aspectRatioWider clicked ");
                        mVideoHeight = Float.parseFloat(height);
                        mVideoWidth = Float.parseFloat(width);
                        updateTextureViewSize(mTextureView.getWidth(), mTextureView.getHeight()); //function to center crop the video
                        aspectRatio.setVisibility(View.VISIBLE);
                        aspectRatioWider.setVisibility(View.GONE);
                        //showButtons();
                    }
                });
            }

        } catch (NumberFormatException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * crop the TextureView to make it center crop
     */

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        Log.d(TAG, "cropCenter updateTextureViewSize: viewWidth= "+ viewWidth + " viewHeight= "+viewHeight);
        Log.d(TAG, "cropCenter updateTextureViewSize: videoWidth= "+ mVideoWidth + " VideoHeight= "+mVideoHeight);

        if (mVideoWidth > viewWidth && mVideoHeight > viewHeight) {
            scaleX = mVideoWidth / viewWidth;
            scaleY = mVideoHeight / viewHeight;
            Log.d(TAG, "cropCenter updateTextureViewSize: Video is bigger than view. scaleX= "+ scaleX + " scaleY= "+scaleY);
        } else if (mVideoWidth < viewWidth && mVideoHeight < viewHeight) {
            scaleY = viewWidth / mVideoWidth;
            scaleX = viewHeight / mVideoHeight;
            Log.d(TAG, "cropCenter updateTextureViewSize: Video is smaller than view. scaleX= "+ scaleX + " scaleY= "+scaleY);
        } else if (viewWidth > mVideoWidth) {
            scaleY = (viewWidth / mVideoWidth) / (viewHeight / mVideoHeight);
            Log.d(TAG, "cropCenter updateTextureViewSize: Video width is smaller than view width. scaleX= "+ scaleX + " scaleY= "+scaleY);
        } else if (viewHeight > mVideoHeight) {
            scaleX = (viewHeight / mVideoHeight) / (viewWidth / mVideoWidth);
            Log.d(TAG, "cropCenter updateTextureViewSize: Video Height is smaller than view Height. scaleX= "+ scaleX + " scaleY= "+scaleY);
        }

        // Calculate pivot points, in our case crop from center
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;

        Matrix matrix = new Matrix();
        //matrix.setRotate(-270);
        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);

        mTextureView.setTransform(matrix);
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }

    /**
     * crop the TextureView's bitmap, as it doesn't look like the cropped TextureView
     */

    public Bitmap cropCenter(Bitmap bitmap) {
        /*int dimension = Math.min(bmp.getWidth(), bmp.getHeight());
        return ThumbnailUtils.extractThumbnail(bmp, dimension, dimension);*/
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float imageWidth = bitmap.getWidth();
        float imageHeight = bitmap.getHeight();
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
        
        /*Log.d(TAG, "cropCenter: viewWidth= "+ viewWidth + " viewHeight= "+viewHeight);
        Log.d(TAG, "cropCenter: VideoWidth= "+ mVideoWidth + " VideoHeight= "+mVideoHeight);
        Log.d(TAG, "cropCenter: ImageWidth= "+ imageWidth + " ImageHeight= "+ imageHeight);

        if (imageWidth > viewWidth && imageHeight > viewHeight) {
            scaleX = imageWidth / viewWidth;
            scaleY = imageHeight / viewHeight;
            Log.d(TAG, "cropCenter: Image is bigger than view. scaleX= "+ scaleX + " scaleY= "+scaleY);
        } else if (imageWidth < viewWidth && imageHeight < viewHeight) {
            scaleY = viewWidth / imageWidth;
            scaleX = viewHeight / imageHeight;
            Log.d(TAG, "cropCenter: Image is smaller than view. scaleX= "+ scaleX + " scaleY= "+scaleY);
        } else if (viewWidth > imageWidth) {
            scaleY = (viewWidth / imageWidth) / (viewHeight / imageHeight);
            Log.d(TAG, "cropCenter: Image width is smaller than view width. scaleX= "+ scaleX + " scaleY= "+scaleY);
        } else if (viewHeight > imageHeight) {
            scaleX = (viewHeight / imageHeight) / (viewWidth / imageWidth);
            Log.d(TAG, "cropCenter: Image Height is smaller than view Height. scaleX= "+ scaleX + " scaleY= "+scaleY);
        }else if(imageWidth == viewWidth && imageHeight == viewHeight){
            Log.d(TAG, "cropCenter: Image and view are identical");
        }
        // Calculate pivot points, in our case crop from center
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;
        Log.d(TAG, "cropCenter: pivotPointX= "+ pivotPointX + " pivotPointY= "+pivotPointY);
*/
        int imagePivotPointX = (int) ((imageWidth/2)- (viewWidth / 2));
        int imagePivotPointY = (int) ((imageHeight/2)- (viewHeight / 2));


        Log.d(TAG, "cropCenter: imagePivotPointX= "+ imagePivotPointX);


        scaledBitmap = Bitmap.createBitmap(bitmap, imagePivotPointX, imagePivotPointY, viewWidth ,
                viewHeight);

        /*scaledBitmap = Bitmap.createBitmap(bitmap, 426, 0, 480 ,
                bitmap.getHeight()-35, matrix, true);*/

        /*scaledBitmap = Bitmap.createScaledBitmap(scaledBitmap,viewWidth,
                viewHeight, false);*/

        //mCompassHud.setImageBitmap(scaledBitmap);

      /*  Bitmap scaledBitmap = Bitmap.createBitmap (viewWidth, viewHeight, Bitmap.Config.ARGB_8888);

        Canvas offscreenCanvas = new Canvas (scaledBitmap);
        offscreenCanvas.setMatrix (matrix);
        offscreenCanvas.drawBitmap (bitmap, 0, 0, new Paint(Paint.DITHER_FLAG));*/
        /*RectF imageRect = new RectF(0, 0, imageWidth, imageHeight);
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        matrix.setRectToRect(imageRect, viewRect, Matrix.ScaleToFit.FILL);

        Bitmap scaledBitmap ;
        scaledBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
        );*/

        /*if (bitmap.getWidth() >= bitmap.getHeight()){

            scaledBitmap = Bitmap.createBitmap(
                    bitmap,
                    bitmap.getWidth()/2 - bitmap.getHeight()/2,
                    0,
                    bitmap.getHeight(),
                    viewHeight
            );

        }else{

            scaledBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.getHeight()/2 - bitmap.getWidth()/2,
                    bitmap.getWidth(),
                    bitmap.getWidth()
            );
        }*/

        //mTextureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));

        return scaledBitmap;
        //return bitmap;
    }

    /**
     * transform the bitmap to greyscale for quicker detection (not really significant and needs opencv)
     */

    /*private Bitmap greyscale (Bitmap bitmap) {

        Mat rgbMat = new Mat(mTextureView.getHeight() + mTextureView.getHeight() / 2, mTextureView.getWidth(), CvType.CV_8UC3);
        Mat greyMat = new Mat(mTextureView.getHeight() + mTextureView.getHeight() / 2, mTextureView.getWidth(), CvType.CV_8UC1);

        Utils.bitmapToMat(bitmap, rgbMat);
        //greyMat.put(0, 0, data);

        //Imgproc.cvtColor(yuvMat, currentFrame, Imgproc.COLOR_YUV420sp2RGB);
        Imgproc.cvtColor(rgbMat, greyMat, Imgproc.COLOR_BGR2GRAY);

        try {
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
            //Imgproc.cvtColor(RGBmat, grayMat, Imgproc.COLOR_BGR2GRAY);

            Bitmap greyBitmap = Bitmap.createBitmap(greyMat.cols(), greyMat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(greyMat, greyBitmap);
            return greyBitmap;

        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
            return null;
        }
    }*/

    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();

        double aspectRatio = (double) videoHeight  / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.d(TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.setScale((float) newWidth / viewWidth, (float) 1.4);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    private void releaseSurfaceHolder() {

        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            public void run(NotificationServiceInterface service) {
                try {
                    service.releaseSurfaceHolder();
                    Log.d("mama2", "releaseSurfaceHolder= "+mSurface);
                } catch (RemoteException e) {
                    Log.d("mama2", "surface error= "+e.getMessage());
                    //return;
                }

            }
        });
    }

    private void hideButtons() {

        snoozeButton.setVisibility(View.INVISIBLE);
        dismiss.setVisibility(View.INVISIBLE);
    }

    /*private void showButtons() {

        snoozeButton.setVisibility(View.VISIBLE);
        dismiss.setVisibility(View.VISIBLE);
    }*/

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
            Log.d(TAG, "GraphicFaceTrackerFactory  face=" + face);
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
            //Log.d(TAG, "GraphicFaceTracker mOverlay= " + mOverlay);
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);

            shouldhideButtons = true;

            /*Log.d(TAG, "GraphicFaceTracker onNewItem faceId= " + faceId + shouldhideButtons);
            PointF point = item.getPosition();
            Log.d(TAG, "GraphicFaceTracker onNewItem Face Position= " + item.getPosition()+ "x="
            + point.x+ "y= "+ point.y);
            //detectCurrentBitmap();*/
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            //Log.d(TAG, "GraphicFaceTracker onUpdate face= " + face + shouldhideButtons);
            //detectCurrentBitmap();
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
            //Log.d(TAG, "GraphicFaceTracker onMissing"+ shouldhideButtons);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
            //Log.d(TAG, "GraphicFaceTracker onDone" + shouldhideButtons);
            //showButtons();
            //shouldhideButtons = false;

        }
    }




}
