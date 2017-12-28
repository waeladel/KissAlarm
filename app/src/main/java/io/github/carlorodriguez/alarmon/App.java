package io.github.carlorodriguez.alarmon;

import android.app.Application;
import android.content.Context;

/**
 * Created by hp on 26/12/2017.
 */

public class App extends Application {

    private static Context sApplicationContext;

    @Override
    public void onCreate() {

        super.onCreate();

        sApplicationContext = getApplicationContext();

        // Initialize the SDK before executing any other operations,
        //FacebookSdk.sdkInitialize(sApplicationContext);

    }

    public static Context getContext() {
        return sApplicationContext;
        //return instance.getApplicationContext();
    }

}

