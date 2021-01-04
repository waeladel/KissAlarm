package io.github.carlorodriguez.alarmon;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.carlorodriguez.alarmon.interfaces.ItemClickListener;


public class CameraPermissionAlertFragment extends DialogFragment {
    private final static String TAG = CameraPermissionAlertFragment.class.getSimpleName();

    private static final int REQUEST_STORAGE_PERMISSIONS_CODE = 124;

    private Context mActivityContext;
    private Activity activity;
    private static Context sContext;
    // click listener to pass click events to parent fragment
    private static ItemClickListener sItemClickListen;

    private static int sMediaType;
    // click listener to pass click events to parent fragment
    //private static ItemClickListener itemClickListen;

    private CameraPermissionAlertFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static CameraPermissionAlertFragment newInstance(Context context, ItemClickListener itemClickListener, int type) {

        // instantiate click listener to pass click events to parent fragment
        sContext = context;

        // instantiate click listener to pass click events to parent fragment
        sItemClickListen = itemClickListener;

        // To know if it's a photo or video, to make a different request code for each one
        sMediaType = type;
        CameraPermissionAlertFragment fragment = new CameraPermissionAlertFragment();
        Bundle args = new Bundle();
        //args.putParcelableArrayList(PRIVET_CONTACTS_KEY, privateContacts);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivityContext = context;
        if (context instanceof Activity){// check if fragmentContext is an activity
            activity =(Activity) context;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // created options to select from
        //CharSequence options[] = new CharSequence[]{getString(R.string.alert_dialog_edit), getString(R.string.alert_dialog_unreveal)};
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(sContext);
        alertDialogBuilder.setTitle(getString(R.string.storage_permission_dialog_title));
        alertDialogBuilder.setMessage(R.string.storage_permission_dialog_message);
        alertDialogBuilder.setPositiveButton(R.string.ok,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // on success
                Log.i(TAG, "onClick on success: request Permissions");
                if(sItemClickListen != null){
                    sItemClickListen.onClick(null, sMediaType, false);
                }
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        return alertDialogBuilder.create();

    }


    /*@Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(null != getDialog()){
            getDialog().setTitle(R.string.user_request_dialog_title);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }*/

    /*@Override
    public void onClick(View view, int position, boolean isLongClick) {
        Log.i(TAG, "onClick view= " + view.getTag()+ " position= " +position);

        if(itemClickListen != null && position != RecyclerView.NO_POSITION){
            itemClickListen.onClick(view, position, false);
        }
    }

    // needed only if i want the listener to be inside the adapter
        public void setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListen = itemClickListener;
        }*/

}
