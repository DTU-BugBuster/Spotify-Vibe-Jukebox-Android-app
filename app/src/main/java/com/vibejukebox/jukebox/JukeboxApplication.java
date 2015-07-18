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


    /** Spotify App Client keys */
    public static final String SPOTIFY_API_CLIENT_ID="*********REMOVED DATA****************";

    private static final String SPOTIFY_API_CLIENT_SECRET_ID = "*********REMOVED DATA****************";

    /** Redirect URI */
    public static final String SPOTIFY_API_REDIRECT_URI="com.vibejukebox.jukeboxapp://callback";

    public static final String TOKEN = "Spotify_access_token";
    public static final String ACCESS_TOKEN = "access_token";

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
        //Parse.initialize(this, "*********REMOVED DATA****************", "*********REMOVED DATA****************");

        //Dev version
        Parse.initialize(this, "*********REMOVED DATA****************", "*********REMOVED DATA****************");
    }
}
