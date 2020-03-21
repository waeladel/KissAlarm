package io.github.carlorodriguez.alarmon;

import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
/**
 * Created by hp on 26/12/2017.
 */

public class App extends Application {

    private static Context sApplicationContext;
    private static MediaPlayer sMediaPlayer;


    @Override
    public void onCreate() {

        super.onCreate();

        sApplicationContext = getApplicationContext();
        //sMediaPlayer = new MediaPlayer();

        // Initialize the SDK before executing any other operations,
        //FacebookSdk.sdkInitialize(sApplicationContext);

    }

    public static Context getContext() {
        return sApplicationContext;
        //return instance.getApplicationContext();
    }

    /*public static MediaPlayer getMediaPlayer() {
        return sMediaPlayer;
        //return instance.getApplicationContext();
    }*/

}

