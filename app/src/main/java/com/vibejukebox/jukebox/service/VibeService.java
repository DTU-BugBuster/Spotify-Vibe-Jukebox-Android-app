package com.vibejukebox.jukebox.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.Vibe;
import com.vibejukebox.jukebox.activities.JukeboxPlaylistActivity;

import java.util.ArrayList;
import java.util.List;

public class VibeService extends Service {

    private static final String TAG = VibeService.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_STRING_PREFERENCE = "JukeboxStoredId";

    private List<String> mTrackUris;

    private List<Track> mPlaylistTracks;

    private Location mLocation;

    private String mJukeboxId;

    private String mPlaylistName;

    private AuthenticationResponse mAuthresponse;

    private static final int VIBE_JUKEBOX_OBJECT_CREATION_DONE = 10;

    //Binder passed back to clients
    private final IBinder mBinder = new VibeBinder();

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_JUKEBOX_OBJECT_CREATION_DONE:
                    storeJukeboxID(mJukeboxId);
                    launchActivePlaylist();
                    stopSelf();
            }
            return false;
        }
    });

    public class VibeBinder extends Binder
    {
        VibeService getService(){
            //Return the instance of the service
            return VibeService.this;
        }
    }

    /**
     * Retrieves the created jukebox ID from the shared preferences.
     * @return: Jukebox object ID String.
     */
    private String getCreatedJukeboxId()
    {
        Log.d(TAG, "getCreatedJukeboxId -- ");
        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(VIBE_JUKEBOX_STRING_PREFERENCE, null);

        Log.d(TAG, "------------------------------------------- Returning ID from service: " + jukeboxId);
        return jukeboxId;
    }

    /**
     * Stores the Jukebox ID to the shared preferences
     * @param id
     */
    private void storeJukeboxID(String id)
    {
        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(VIBE_JUKEBOX_STRING_PREFERENCE, id);
        editor.commit();
    }

    private ParseGeoPoint getGeoPointFromMyLocation(Location location)
    {
        if(mLocation != null)
            return new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        else {
            Log.e(TAG, "An error occured getting the correct location object.");
            return null;
        }
    }
    // ------------------------------------------------------------------------------------------------------

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mTrackUris = intent.getStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE);
        mPlaylistTracks = intent.getParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE);
        //mLocation = intent.getParcelableExtra(Vibe.VIBE_CURRENT_LOCATION);
        mLocation = Vibe.getCurrentLocation();
        mAuthresponse = intent.getParcelableExtra("authresponse");
        mJukeboxId = getCreatedJukeboxId();

        mPlaylistName = intent.getStringExtra("playlistname");

        // If the Jukebox ID is not null means the user has already created a jukebox so we reuse it.
        if(mJukeboxId != null)
            getJukeboxFromBackend(mPlaylistName);
        else
            createJukebox(mPlaylistName);
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    private void launchActivePlaylist()
    {
        Intent intent = new Intent(getApplicationContext(), JukeboxPlaylistActivity.class);
        intent.putExtra("authresponse", mAuthresponse);
        intent.putExtra(Vibe.VIBE_IS_ACTIVE_PLAYLIST, true);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ID, mJukeboxId);
        intent.putExtra("playlistName", mPlaylistName);
        intent.putParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE, (ArrayList<Track>) mPlaylistTracks);
        intent.putStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE, (ArrayList<String>) mTrackUris);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void getJukeboxFromBackend(final String playlistName)
    {

        if(DEBUG){
            Location location = Vibe.getCurrentLocation();
            if(location.equals(mLocation))
                Log.d(TAG, "Locations are the same..");
            else
                Log.e(TAG, "Locations are different, static method not working correctly.");
        }

        final ParseGeoPoint geoPoint = getGeoPointFromMyLocation(mLocation);
        if(geoPoint == null){
            Log.e(TAG, "An error occurred getting the current location");
            return;
        }

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxId, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukebox, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully retrieved Jukebox from cloud with ID:  " + mJukeboxId);
                    jukebox.put("name", playlistName);
                    jukebox.put("location", geoPoint);
                    jukebox.put("queueSongIDs", mTrackUris);
                    jukebox.put("defaultQueueSongIDs", mTrackUris);
                    jukebox.saveInBackground();
                    mHandler.sendEmptyMessage(VIBE_JUKEBOX_OBJECT_CREATION_DONE);
                } else {
                    Log.e(TAG, "Error fetching jukebox object, ID: " + mJukeboxId);
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.VIBE_APP_POOR_CONNECTION_MESSAGE),
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates a Jukebox object with a given name
     * @param playlistName: Name of the playlist to be created
     */
    public void createJukebox(String playlistName)
    {
        if(DEBUG)
            Log.d(TAG, "createJukebox -  " + playlistName);

        ParseGeoPoint geoPoint = getGeoPointFromMyLocation(mLocation);
        if(geoPoint == null){
            Log.e(TAG, "An error occurred getting the current location");
            return;
        }

        //Create Parse object
        ParseObject.registerSubclass(JukeboxObject.class);
        final JukeboxObject jukebox = new JukeboxObject();
        jukebox.put("name", playlistName);
        jukebox.put("location", geoPoint);
        jukebox.put("queueSongIDs", mTrackUris);
        jukebox.put("defaultQueueSongIDs", mTrackUris);
        jukebox.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null){
                    Log.d(TAG, "Success saving object in background ..");
                    mJukeboxId = jukebox.getObjectId();
                    mHandler.sendEmptyMessage(VIBE_JUKEBOX_OBJECT_CREATION_DONE);
                    //mCreatedObjectId = id;
                    //mHandler.sendEmptyMessage(OBJECT_CREATED);
                }
                else{
                    Log.e(TAG, "Something went wrong creating new jukebox object ...");
                }
            }
        });
    }
}
