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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.wdullaer.materialdatetimepicker.time.*;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import java.util.ArrayList;
import java.util.Calendar;

//import com.facebook.FacebookSdk; // for facebook ads app installs
//import com.facebook.appevents.AppEventsLogger; // for facebook analytics

/**
 * This is the main Activity for the application.  It contains a ListView
 * for displaying all alarms, a simple clock, and a button for adding new
 * alarms.  The context menu allows the user to edit default settings.  Long-
 * clicking on the clock will trigger a dialog for enabling/disabling 'debug
 * mode.'
 */
public final class ActivityAlarmClock extends AppCompatActivity implements
        TimePickerDialog.OnTimeSetListener,
        TimePickerDialog.OnTimeChangedListener {

    private static String TAG = ActivityAlarmClock.class.getSimpleName();


    public static final int DELETE_CONFIRM = 1;
    public static final int DELETE_ALARM_CONFIRM = 2;

    public static final int ACTION_TEST_ALARM = 0;
    public static final int ACTION_PENDING_ALARMS = 1;
    public static final int ACTION_INVITE = 2;

    private static final int REQUEST_INVITE = 0;


    private TimePickerDialog picker;
    public static ActivityAlarmClock activityAlarmClock;

    private static AlarmClockServiceBinder service;
    private static NotificationServiceBinder notifyService;
    private DbAccessor db;
    private static AlarmAdapter adapter;
    private Cursor cursor;
    private Handler handler;
    private Runnable tickCallback;
    private static RecyclerView alarmList;
    private int mLastFirstVisiblePosition;
    // [START define_variables]
    //private GoogleApiClient mGoogleApiClient;
    // [END define_variables]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.setMainActivityTheme(getBaseContext(),
                ActivityAlarmClock.this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.alarm_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        activityAlarmClock = this;

        //Only use if you need to know the key hash for facebook
        // printHashKey(getBaseContext());

        // Access to in-memory and persistent data structures.
        service = new AlarmClockServiceBinder(getApplicationContext());

        db = new DbAccessor(getApplicationContext());

        handler = new Handler();

        // Setup the alarm list and the underlying adapter. Clicking an individual
        // item will start the settings activity.
        alarmList = (RecyclerView) findViewById(R.id.alarm_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        alarmList.setLayoutManager(layoutManager);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final AlarmInfo alarmInfo = adapter.getAlarmInfos().
                        get(viewHolder.getAdapterPosition());

                final long alarmId = alarmInfo.getAlarmId();

                removeItemFromList(ActivityAlarmClock.this, alarmId,
                        viewHolder.getAdapterPosition());

                Snackbar.make(findViewById(R.id.coordinator_layout),
                        getString(R.string.alarm_deleted), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        undoAlarmDeletion(alarmInfo.getTime(),
                                db.readAlarmSettings(alarmId),
                                alarmInfo.getName(), alarmInfo.enabled());
                    }
                })
                .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);

        itemTouchHelper.attachToRecyclerView(alarmList);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();

                picker = TimePickerDialog.newInstance(
                        ActivityAlarmClock.this,
                        ActivityAlarmClock.this,
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(ActivityAlarmClock.this)
                );

                if (AppSettings.isThemeDark(ActivityAlarmClock.this)) {
                    picker.setThemeDark(true);
                }

                picker.setAccentColor(AppSettings.getTimePickerColor(
                        ActivityAlarmClock.this));

                picker.vibrate(true);

                if (AppSettings.isDebugMode(ActivityAlarmClock.this)) {
                    picker.enableSeconds(true);
                } else {
                    picker.enableSeconds(false);
                }

                AlarmTime time = new AlarmTime(now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE), 0);

                picker.setTitle(time.timeUntilString(ActivityAlarmClock.this));

                picker.show(getFragmentManager(), "TimePickerDialog");
            }
        });

        // This is a self-scheduling callback that is responsible for refreshing
        // the screen.  It is started in onResume() and stopped in onPause().
        tickCallback = new Runnable() {
            @Override
            public void run() {
                // Redraw the screen.
                redraw();

                // Schedule the next update on the next interval boundary.
                AlarmUtil.Interval interval = AlarmUtil.Interval.MINUTE;

                if (AppSettings.isDebugMode(getApplicationContext())) {
                    interval = AlarmUtil.Interval.SECOND;
                }

                long next = AlarmUtil.millisTillNextInterval(interval);

                handler.postDelayed(tickCallback, next);
            }
        };

        if (!AppIntro.isAlarmDeletionShowcased(this)) {
            requery();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (adapter.getItemCount() >= 1 && alarmList.getChildAt(0) != null) {
                        AppIntro.showcaseAlarmDeletion(ActivityAlarmClock.this,
                                alarmList.getChildAt(0));
                        // use Toast better
                        Toast.makeText(ActivityAlarmClock.this, R.string.swipe_right_to_delete, Toast.LENGTH_LONG).show();
                    }
                }
            }, 1500);
        }

        // Create an auto-managed GoogleApiClient with access to App Invites.
        /*mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppInvite.API)
                .enableAutoManage(this, this)
                .build();*/

        // Check for App Invite invitations and launch deep-link activity if possible.
        // Requires that an Activity is registered in AndroidManifest.xml to handle
        // deep-link URLs.
        //boolean autoLaunchDeepLink = true;
        //boolean autoLaunchDeepLink = false;
        /*AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, this, autoLaunchDeepLink)
                .setResultCallback(
                        new ResultCallback<AppInviteInvitationResult>() {
                            @Override
                            public void onResult(AppInviteInvitationResult result) {
                                Log.d(TAG, "getInvitation:onResult:" + result.getStatus());
                                if (result.getStatus().isSuccess()) {
                                    // Extract information from the intent
                                    Intent intent = result.getInvitationIntent();
                                    String deepLink = AppInviteReferral.getDeepLink(intent);
                                    //String invitationId = AppInviteReferral.getInvitationId(intent);
                                    Log.d(TAG, "getInvitation:deepLink:" + deepLink);

                                    // Because autoLaunchDeepLink = true we don't have to do anything
                                    // here, but we could set that to false and manually choose
                                    // an Activity to launch to handle the deep link here.
                                    // ...
                                    Intent congratulationIntent = new Intent(ActivityAlarmClock.this, DeepLinkActivity.class);
                                    //startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(congratulationIntent);
                                }
                            }
                        });*/


        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();

                            Log.d(TAG, "FirebaseDynamicLinks. deepLink:" + deepLink);
                            // Handle the deep link. For example, open the linked
                            // content, or apply promotional credit to the user's account.
                            Intent congratulationIntent = new Intent(ActivityAlarmClock.this, DeepLinkActivity.class);
                            //startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(congratulationIntent);

                        }

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getDynamicLink:onFailure", e);
                    }
                });

    }
    // [END on_create]

    /*@Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        //showMessage(getString(R.string.google_play_services_error));
        Toast.makeText(ActivityAlarmClock.this, getString(R.string.google_play_services_error),
                Toast.LENGTH_LONG).show();
        // Sending failed or it was canceled
    }*/

    private void undoAlarmDeletion(AlarmTime alarmTime,
            AlarmSettings alarmSettings, String alarmName, boolean enabled) {
        long newAlarmId = service.resurrectAlarm(alarmTime, alarmName, enabled);

        if (newAlarmId != AlarmClockServiceBinder.NO_ALARM_ID) {
            db.writeAlarmSettings(newAlarmId, alarmSettings);

            requery();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        invalidateOptionsMenu();

        service.bind();

        handler.post(tickCallback);

        requery();

        alarmList.getLayoutManager().scrollToPosition(mLastFirstVisiblePosition);

        notifyService = new NotificationServiceBinder(getApplicationContext());

        notifyService.bind();

        notifyService.call(new NotificationServiceBinder.ServiceCallback() {
            @Override
            public void run(NotificationServiceInterface service) {
                int count;

                try {
                    count = service.firingAlarmCount();
                } catch (RemoteException e) {
                    return;
                }

                if (count > 0) {
                    Intent notifyActivity = new Intent(getApplicationContext(),
                            ActivityAlarmNotification.class);

                    notifyActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(notifyActivity);
                }
            }
        });

        TimePickerDialog tpd = (TimePickerDialog) getFragmentManager().
                findFragmentByTag("TimePickerDialog");

        if (tpd != null) {
            picker = tpd;

            tpd.setOnTimeSetListener(this);

            tpd.setOnTimeChangedListener(this);
        }

        Log.d(TAG, "onResume");

    }

    @Override
    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(tickCallback);

        service.unbind();

        if (notifyService != null) {
            notifyService.unbind();
        }

        mLastFirstVisiblePosition = ((LinearLayoutManager)
                alarmList.getLayoutManager()).
                findFirstCompletelyVisibleItemPosition();

        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        db.closeConnections();

        activityAlarmClock = null;

        notifyService = null;

        cursor.close();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (AppSettings.isDebugMode(getApplicationContext())) {
            menu.add(Menu.NONE, ACTION_TEST_ALARM, 5, R.string.test_alarm);

            menu.add(Menu.NONE, ACTION_PENDING_ALARMS, 6, R.string.pending_alarms);
        }

        menu.add(Menu.NONE, ACTION_INVITE, 7, R.string.menu_invite);

        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Get the invitation IDs of all sent messages
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                for (String id : ids) {
                    Log.d(TAG, "onActivityResult: sent invitation " + id);
                }
            } else {
                Toast.makeText(ActivityAlarmClock.this, getString(R.string.invitation_failed),
                        Toast.LENGTH_LONG).show();
                // Sending failed or it was canceled, show failure message to the user
                // ...
            }
        }
    }*/
    // [END on_activity_result]

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        AlarmTime time = new AlarmTime(hourOfDay, minute, second);

        service.createAlarm(time);
        requery();
        Log.d(TAG, "onTimeSet");
        Log.d(TAG, "getItemCount= "+adapter.getItemCount()+ "getChildAt(0)="+alarmList.getChildAt(0));

        if (adapter.getItemCount()==1){ // if it's the first alarm

            // Schedule the next update to be Immediately.
            handler.removeCallbacks(tickCallback); //Remove any existing callbacks to the handler so we don't get more than one callback event
            AlarmUtil.Interval interval = AlarmUtil.Interval.SECOND;
            long next = AlarmUtil.millisTillNextInterval(interval);
            handler.postDelayed(tickCallback, next);
            Log.d(TAG, "interval SECOND: "+ next);
        }

    }

    @Override
    public void onTimeChanged(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        AlarmTime time = new AlarmTime(hourOfDay, minute, second);

        picker.setTitle(time.timeUntilString(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all:
                showDialogFragment(DELETE_CONFIRM);
                break;
            case R.id.action_default_settings:
                Intent alarm_settings = new Intent(getApplicationContext(),
                        ActivityAlarmSettings.class);

                alarm_settings.putExtra(ActivityAlarmSettings.EXTRAS_ALARM_ID,
                        AlarmSettings.DEFAULT_SETTINGS_ID);

                startActivity(alarm_settings);
                break;
            case R.id.action_app_settings:
                Intent app_settings = new Intent(getApplicationContext(),
                        ActivityAppSettings.class);

                startActivity(app_settings);
                break;
            case ACTION_TEST_ALARM:
                // Used in debug mode.  Schedules an alarm for 5 seconds in the future
                // when clicked.
                final Calendar testTime = Calendar.getInstance();

                testTime.add(Calendar.SECOND, 5);

                AlarmTime time = new AlarmTime(
                        testTime.get(Calendar.HOUR_OF_DAY),
                        testTime.get(Calendar.MINUTE),
                        testTime.get(Calendar.SECOND));

                service.createAlarm(time);

                requery();
                break;
            case ACTION_PENDING_ALARMS:
                // Displays a list of pending alarms (only visible in debug mode).
                startActivity(new Intent(getApplicationContext(),
                        ActivityPendingAlarms.class));
                break;
            case ACTION_INVITE:
                Log.d(TAG, "INVITE clicked ");
                onInviteClicked();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onInviteClicked() {
        /*Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                //.setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);*/

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invitation_message)+"\n\n" + Uri.parse(getString(R.string.invitation_deep_link)));
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invitation_subject));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.invitation_cta)));
    }

    private void showDialogFragment(int id) {
        DialogFragment dialog = new ActivityDialogFragment().newInstance(
                id);

        dialog.show(getFragmentManager(), "ActivityDialogFragment");
    }

    private void redraw() {
        // Recompute expiration times in the list view
        adapter.notifyDataSetChanged();

        Calendar now = Calendar.getInstance();

        AlarmTime time = new AlarmTime(now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), 0);

        ((CollapsingToolbarLayout) findViewById(R.id.toolbar_layout)).setTitle(
                time.localizedString(this));
    }

    private void requery() {
        cursor = db.readAlarmInfo();

        ArrayList<AlarmInfo> infos = new ArrayList<>();

        while (cursor.moveToNext()) {
            infos.add(new AlarmInfo(cursor));
        }

        adapter = new AlarmAdapter(infos, service, this);

        alarmList.setAdapter(adapter);

        setEmptyViewIfEmpty(this);
    }

    public static void setEmptyViewIfEmpty(Activity activity) {
        if (adapter.getItemCount() == 0) {
            activity.findViewById(R.id.empty_view).setVisibility(View.VISIBLE);

            alarmList.setVisibility(View.GONE);
        } else {
            activity.findViewById(R.id.empty_view).setVisibility(View.GONE);

            alarmList.setVisibility(View.VISIBLE);
        }
    }

    public static void removeItemFromList(Activity activity, long alarmId, int position) {
        if (adapter.getItemCount() == 1) {
            ((AppBarLayout) activity.findViewById(R.id.app_bar)).
                    setExpanded(true);
        }

        service.deleteAlarm(alarmId);

        adapter.removeAt(position);

        setEmptyViewIfEmpty(activity);
    }

    /*public static void printHashKey(Context pContext) {
        try {
            PackageInfo info = pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i(TAG, "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "printHashKey()", e);
        } catch (Exception e) {
            Log.e(TAG, "printHashKey()", e);
        }
    }*/

    public static class ActivityDialogFragment extends DialogFragment {

        public ActivityDialogFragment newInstance(int id) {
            ActivityDialogFragment fragment = new ActivityDialogFragment();

            Bundle args = new Bundle();

            args.putInt("id", id);

            fragment.setArguments(args);

            return fragment;
        }

        public ActivityDialogFragment newInstance(int id, AlarmInfo info,
                int position) {
            ActivityDialogFragment fragment = new ActivityDialogFragment();

            Bundle args = new Bundle();

            args.putInt("id", id);

            args.putLong("alarmId", info.getAlarmId());

            args.putInt("position", position);

            fragment.setArguments(args);

            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            switch (getArguments().getInt("id")) {
                case ActivityAlarmClock.DELETE_CONFIRM:
                    final AlertDialog.Builder deleteConfirmBuilder =
                            new AlertDialog.Builder(getActivity());

                    deleteConfirmBuilder.setTitle(R.string.delete_all);

                    deleteConfirmBuilder.setMessage(R.string.confirm_delete);

                    deleteConfirmBuilder.setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            service.deleteAllAlarms();

                            adapter.removeAll();

                            setEmptyViewIfEmpty(getActivity());

                            dismiss();
                        }
                    });

                    deleteConfirmBuilder.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
                    return deleteConfirmBuilder.create();
                case ActivityAlarmClock.DELETE_ALARM_CONFIRM:
                    final AlertDialog.Builder deleteAlarmConfirmBuilder =
                            new AlertDialog.Builder(getActivity());

                    deleteAlarmConfirmBuilder.setTitle(R.string.delete);

                    deleteAlarmConfirmBuilder.setMessage(
                            R.string.confirm_delete);

                    deleteAlarmConfirmBuilder.setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    removeItemFromList(getActivity(),
                                            getArguments().getLong("alarmId"),
                                            getArguments().getInt("position"));

                                    dismiss();
                                }
                            });

                    deleteAlarmConfirmBuilder.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    dismiss();
                                }
                            });
                    return deleteAlarmConfirmBuilder.create();
                default:
                    return super.onCreateDialog(savedInstanceState);
            }
        }

    }

}
