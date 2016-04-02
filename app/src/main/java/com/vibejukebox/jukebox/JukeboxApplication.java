package com.vibejukebox.jukebox;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.vibejukebox.jukebox.internal.di.components.DaggerSpotifyComponent;
import com.vibejukebox.jukebox.internal.di.components.SpotifyComponent;
import com.vibejukebox.jukebox.internal.di.modules.SpotifyModule;

/**
 * Created by Sergex on 11/4/14.
 *
 */
public class JukeboxApplication extends Application {

    private static final String TAG = JukeboxApplication.class.getSimpleName();

    private static final boolean DEBUG = DebugLog.DEBUG;

    /** Spotify App Client keys */
    public static final String SPOTIFY_API_CLIENT_ID="xxxxxxxxxxxx";

    private static final String SPOTIFY_API_CLIENT_SECRET_ID = "xxxxxxxxxxx";

    /** Redirect URI */
    public static final String SPOTIFY_API_REDIRECT_URI="com.vibejukebox.jukeboxapp://callback";

    private SpotifyComponent mComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeParseLibrary();
        initializeSpotifyComponent();
    }

    private void initializeParseLibrary() {
        if(DEBUG) {
            Log.d(TAG, "Parse Initialized.");
        }

        //Parse.enableLocalDatastore(getApplicationContext());
        //Parse API - set application id and client id
        //Parse.initialize(this, "xxxxxxx", "xxxxxxx");

        //Dev version
        Parse.initialize(this, "xxxxxxxx", "xxxxxxxxxx");
    }

    private void initializeSpotifyComponent() {
        mComponent = DaggerSpotifyComponent.builder()
                .spotifyModule(new SpotifyModule())
                .build();
    }

    public SpotifyComponent getSpotifyComponent() {
        return this.mComponent;
    }
}