package com.vibejukebox.jukebox;

import android.app.Application;
import android.util.Log;
import com.parse.Parse;

/**
 * Created by Sergex on 11/4/14.
 */
public class JukeboxApplication extends Application
{
    public static final String TAG = JukeboxApplication.class.getSimpleName();
    public static final boolean DEBUG = DebugLog.DEBUG;

    @Override
    public void onCreate() {
        super.onCreate();

        if(DEBUG)
            Log.d(TAG, "on Create Application");

        initializeParseLibrary();
    }

    public void initializeParseLibrary()
    {
        if(DEBUG)
            Log.d(TAG, "Parse Initialized.");

        //Parse.enableLocalDatastore(getApplicationContext());
        //Parse API - set application id and client id
        Parse.initialize(this, "L6g7AozXjr5TQ06YtuTXjSs15NZwfYiRnDnaeAn9", "huqTuw04XCuGe3rxVlkhvkD2PH805UemWkUVoYNM");

        //Dev version
        //Parse.initialize(this, "wNcvL6UuEqiUtNZ3TBzCioO6jvzRqbbt0JZXyS7f", "AguDQ6lKwJtWTeoTl6zE75dy2FFki00NtMNfGyX9");
    }
}
