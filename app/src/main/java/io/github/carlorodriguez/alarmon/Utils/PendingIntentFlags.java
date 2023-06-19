package io.github.carlorodriguez.alarmon.Utils;

import android.app.PendingIntent;
import android.os.Build;

public class PendingIntentFlags {

    public static int pendingIntentNoCreateFlag() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
        } else {
            return PendingIntent.FLAG_NO_CREATE;
        }
    }

    public static int pendingIntentNoFlag() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }

    public static int pendingIntentUpdateCurrentFlag() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

}
