package io.github.carlorodriguez.alarmon;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

import io.github.carlorodriguez.alarmon.Utils.CheckPermissions;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ContentViewHolder> {

    private ArrayList<AlarmInfo> alarmInfos;
    private AlarmClockServiceBinder service;
    private Context context;
    private AlarmManager alarmManager;

    public AlarmAdapter(ArrayList<AlarmInfo> alarmInfos,
            AlarmClockServiceBinder service, Context context) {
        this.alarmInfos = alarmInfos;
        this.service = service;
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    }

    public ArrayList<AlarmInfo> getAlarmInfos() {
        return alarmInfos;
    }

    public void removeAt(int position) {
        alarmInfos.remove(position);

        notifyItemRemoved(position);

        notifyItemRangeChanged(position, alarmInfos.size());
    }

    public void removeAll() {
        int size = alarmInfos.size();

        if (size > 0) {
            for (int i = 0; i < size; i++) {
                alarmInfos.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
    }

    @Override
    public void onBindViewHolder(ContentViewHolder holder, final int position) {
        final AlarmInfo info = alarmInfos.get(position);

        AlarmTime time = null;
        // See if there is an instance of this alarm scheduled.
        if (service.clock() != null) {
            try {
                time = service.clock().pendingAlarm(info.getAlarmId());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        // If we couldn't find a pending alarm, display the configured time.
        if (time == null) {
            time = info.getTime();
        }

        String timeStr = time.localizedString(context);
        String alarmId = "";
        if (AppSettings.isDebugMode(context)) {
            alarmId = " [" + info.getAlarmId() + "]";
        }
        String timeText = timeStr + alarmId;

        holder.timeView.setText(timeText);

        holder.nextView.setText(time.timeUntilString(context));

        holder.labelView.setText(info.getName());

        if (!info.getTime().getDaysOfWeek().equals(Week.NO_REPEATS)) {
            holder.repeatView.setText(info.getTime().getDaysOfWeek().
                    toString(context));
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CheckPermissions.isNotificationPermissionGranted(context)){
            // Starting from Api 33 we must grant post notification permission at run time
            holder.enabledView.setEnabled(false);
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
            // in App 32/31, user may disable Schedule Exact Alarms from app settings, we must check
            holder.enabledView.setEnabled(false);
        }else if(!CheckPermissions.isNotificationEnabled(context)){
            // User disabled notifications channels
            holder.enabledView.setEnabled(false);
        }else{
            holder.enabledView.setChecked(info.enabled());
        }

        holder.enabledView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final AlarmInfo info = alarmInfos.get(position);

                if (isChecked) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CheckPermissions.isNotificationPermissionGranted(context)){
                        // Starting from Api 33 we must grant post notification permission at run time
                        buttonView.setChecked(false);
                        CheckPermissions.requestNotificationPermission(context);
                    }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
                        // in App 32/31, user may disable Schedule Exact Alarms from app settings, we must check
                        buttonView.setChecked(false);
                        CheckPermissions.showAlarmsRemindersDialog(context);
                    }else if(!CheckPermissions.isNotificationEnabled(context)){
                        // User disabled notifications channels
                        buttonView.setChecked(false);
                        CheckPermissions.showNotificationSettingsDialog(context);
                    }else{
                        info.setEnabled(true);

                        service.scheduleAlarm(info.getAlarmId());
                    }

                } else {
                    info.setEnabled(false);

                    service.unscheduleAlarm(info.getAlarmId());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return alarmInfos.size();
    }

    @Override
    public ContentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.alarm_list_item, parent, false);

        return new ContentViewHolder(itemView);
    }

    public class ContentViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        protected TextView timeView;
        protected TextView nextView;
        protected TextView labelView;
        protected TextView repeatView;
        protected SwitchCompat enabledView;

        public ContentViewHolder(View view) {
            super(view);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);

            timeView = (TextView) view.findViewById(R.id.alarm_time);
            nextView = (TextView) view.findViewById(R.id.next_alarm);
            labelView = (TextView) view.findViewById(R.id.alarm_label);
            repeatView = (TextView) view.findViewById(R.id.alarm_repeat);
            enabledView = (SwitchCompat) view.findViewById(R.id.alarm_enabled);
        }

        public void openAlarmSettings(Context context) {

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CheckPermissions.isNotificationPermissionGranted(context)){
                // Starting from Api 33 we must grant post notification permission at run time
                CheckPermissions.requestNotificationPermission(context);
                return;
            }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
                // in App 32/31, user may disable Schedule Exact Alarms from app settings, we must check
                CheckPermissions.showAlarmsRemindersDialog(context);
                return;
            }else if(!CheckPermissions.isNotificationEnabled(context)){
                // User disabled notifications channels
                CheckPermissions.showNotificationSettingsDialog(context);
                return;
            }

            final AlarmInfo info = alarmInfos.get(getAdapterPosition());

            final Intent i = new Intent(context, ActivityAlarmSettings.class);

            i.putExtra(ActivityAlarmSettings.EXTRAS_ALARM_ID, info.getAlarmId());

            context.startActivity(i);
        }

        @Override
        public void onClick(View v) {
            openAlarmSettings(v.getContext());
        }

        @Override
        public boolean onLongClick(View v) {
            final CharSequence actions[] = new CharSequence[] {
                    context.getString(R.string.settings),
                    context.getString(R.string.delete)
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setItems(actions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (actions[which].equals(actions[0])) {
                        openAlarmSettings(context);
                    } else if (actions[which].equals(actions[1])) {
                        final AlarmInfo info = alarmInfos.get(getAdapterPosition());

                        DialogFragment delete = new ActivityAlarmClock.ActivityDialogFragment().newInstance(
                                ActivityAlarmClock.DELETE_ALARM_CONFIRM, info,
                                getAdapterPosition());

                        delete.show(((Activity) context).getFragmentManager(),
                                "ActivityDialogFragment");
                    }
                }
            });
            builder.show();

            return true;
        }
    }

}
