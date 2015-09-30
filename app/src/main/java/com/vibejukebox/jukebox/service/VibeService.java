package com.vibejukebox.jukebox.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
import com.vibejukebox.jukebox.SpotifyClient;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.Vibe;
import com.vibejukebox.jukebox.activities.JukeboxPlaylistActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class VibeService extends Service {

    private static final String TAG = VibeService.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_STRING_PREFERENCE = "JukeboxStoredId";

    public static final int VIBE_SERVICE_UPDATE_ALL = 0;

    public static final int VIBE_SERVICE_UPDATE_TITLE = 1;

    public static final int VIBE_SERVICE_UPDATE_LOCATION = 2;

    public static final int VIBE_SERVICE_UPDATE_TRACKS = 3;

    public static final int VIBE_SERVICE_FETCH_LAST_JUKEBOX = 4;

    public static final int VIBE_GET_JUKEBOX_FOR_REFRESH = 100;

    //public static final int VIBE_GET_JUKEBOX_FOR_UPDATE = 200;

    private List<String> mTrackUris;

    private List<Track> mPlaylistTracks;

    private Location mLocation;

    private String mJukeboxId;

    private String mPlaylistName;

    private AuthenticationResponse mAuthResponse;

    private static final int VIBE_JUKEBOX_OBJECT_CREATION_DONE = 10;

    private static final int VIBE_JUKEBOX_REFRESH_TRACK_LIST_UI = 12;

    private static final int VIBE_CREATE_TRACK_FROM_URIS = 20;

    private static final int VIBE_CONNECTION_ERROR = 60;

    //Messenger (Binder) passed back to clients
    private Messenger mRequestMessenger = null;

    private Message mRequestMessage = null;

    private boolean mTrackChanged = false;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_JUKEBOX_OBJECT_CREATION_DONE:
                    storeJukeboxID(mJukeboxId);
                    launchActivePlaylist();
                    stopSelf();
                    return true;
                case VIBE_JUKEBOX_REFRESH_TRACK_LIST_UI:
                    sendReplyMessage((List<String>)msg.obj);
                    return true;
            }
            return false;
        }
    });

    public static class RequestHandler extends Handler
    {
        WeakReference<VibeService> mService;

        public RequestHandler(VibeService service){
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_GET_JUKEBOX_FOR_REFRESH:
                    boolean isChangeTrack = msg.getData().getBoolean("trackChanged");
                    mService.get().mJukeboxId = msg.getData().getString("jukeboxId");
                    mService.get().setRequestMessage(msg);
                    mService.get().getJukeboxForRefresh(isChangeTrack);
                    break;

                case VIBE_SERVICE_UPDATE_TITLE:
                    mService.get().mPlaylistName = msg.getData().getString("playlistName");
                    mService.get().getJukeboxForUpdate(VIBE_SERVICE_UPDATE_TITLE);
                    break;

                case VIBE_SERVICE_UPDATE_LOCATION:
                    mService.get().mLocation = msg.getData().getParcelable("updatedLocation");
                    mService.get().getJukeboxForUpdate(VIBE_SERVICE_UPDATE_LOCATION);
                    break;
            }
        }
    }

    private void sendReplyMessage(List<String> trackUris)
    {
        if(DEBUG)
            Log.d(TAG, "sendReplyMessage -- num of songs: " + trackUris.size());

        Bundle data = new Bundle();
        data.putStringArrayList("list", (ArrayList<String>) trackUris);
        data.putString("name", mPlaylistName);
        data.putBoolean("trackChanged", mTrackChanged);

        final Messenger replyMessenger = mRequestMessage.replyTo;
        Message replyMessage = Message.obtain(null, VIBE_CREATE_TRACK_FROM_URIS);
        replyMessage.setData(data);

        try {
            replyMessenger.send(replyMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setRequestMessage(Message msg)
    {
        if(DEBUG)
            Log.d(TAG, "setRequestMessage -- ");
        mRequestMessage = Message.obtain(msg);
    }

    /**
     * Retrieves the created jukebox ID from the shared preferences.
     * @return: Jukebox object ID String.
     */
    private String getCreatedJukeboxId()
    {
        if(DEBUG)
            Log.d(TAG, "getCreatedJukeboxId -- ");

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(VIBE_JUKEBOX_STRING_PREFERENCE, null);

        if(DEBUG)
            Log.d(TAG, "----------------------------------- Returning ID from service: " + jukeboxId);
        return jukeboxId;
    }

    /**
     * Stores the Jukebox ID to the shared preferences
     * @param id
     */
    private void storeJukeboxID(String id)
    {
        Log.e(TAG, "STORING (SERVICE):  " + id);

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
    /** ------------------------------------------------------------------------------------------------------*/

    @Override
    public void onCreate()
    {
        super.onCreate();
        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        mJukeboxId = getCreatedJukeboxId();
        mLocation = Vibe.getCurrentLocation();
        mRequestMessenger = new Messenger(new RequestHandler(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(DEBUG)
            Log.d(TAG, "onStartCommand  -- Vibe Service");

        boolean isGettingLastJukebox = intent.getBooleanExtra(Vibe.VIBE_JUKEBOX_SERVICE_START_FETCH, false);
        mAuthResponse = intent.getParcelableExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE);

        if(!isGettingLastJukebox){
            mTrackUris = intent.getStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE);
            mPlaylistTracks = intent.getParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE);
            mPlaylistName = intent.getStringExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME);
        }

        // If the Jukebox ID is not null means the user has already created a jukebox so we reuse it.
        if(mJukeboxId != null && !isGettingLastJukebox)
            getJukeboxForUpdate(VIBE_SERVICE_UPDATE_ALL);
        else if(mJukeboxId == null)
            createJukebox(mPlaylistName);
        else
            getJukeboxForUpdate(VIBE_SERVICE_FETCH_LAST_JUKEBOX);

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        if(DEBUG)
            Log.d(TAG, "onBind Vibe Service");
        return mRequestMessenger.getBinder();
    }

    private void launchActivePlaylist()
    {
        if(DEBUG)
            Log.d(TAG, "launchActivePlaylist (Service)");

        Intent intent = new Intent(getApplicationContext(), JukeboxPlaylistActivity.class);
        intent.putExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE, mAuthResponse);
        intent.putExtra(Vibe.VIBE_IS_ACTIVE_PLAYLIST, true);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ID, mJukeboxId);
        intent.putExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME, mPlaylistName);
        //intent.putExtra("storedLocation", mLocation);
        intent.putParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE, (ArrayList<Track>) mPlaylistTracks);
        intent.putStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE, (ArrayList<String>) mTrackUris);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    public void getJukeboxForUpdate(final int mode)
    {
        if(DEBUG)
            Log.d(TAG, "getJukeboxForUpdate -- " + mode);

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxId, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukebox, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully retrieved Jukebox from cloud with ID:  " + mJukeboxId);
                    switch (mode) {
                        case VIBE_SERVICE_UPDATE_ALL:
                            updateJukeboxAll(jukebox);
                            break;
                        case VIBE_SERVICE_UPDATE_TITLE:
                            updateJukeboxTitle(jukebox, mPlaylistName);
                            break;
                        case VIBE_SERVICE_UPDATE_LOCATION:
                            updateJukeboxLocation(jukebox, mLocation);
                            break;
                        case VIBE_SERVICE_UPDATE_TRACKS:
                            updateJukeboxSongLists(jukebox, mTrackUris);
                            break;
                        case VIBE_SERVICE_FETCH_LAST_JUKEBOX:
                            fetchLastJukebox(jukebox);
                    }

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

    private void getJukeboxForRefresh(final boolean changeTrack)
    {
        if(DEBUG)
            Log.d(TAG, "getJukeboxForRefresh with ID:  " + mJukeboxId);

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxId, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully retrieved Jukebox from cloud. ");
                    List<String> trackURIs = new ArrayList<>(jukeboxObject.getQueueSongIds());

                    mPlaylistName = jukeboxObject.getName();
                    mTrackChanged = changeTrack;

                    if (changeTrack) {
                        //Variable to update music that is playing
                        trackURIs.subList(0, 1).clear();

                        if (trackURIs.size() == 0)
                            trackURIs = jukeboxObject.getDefaultQueueSongIds();

                        updateJukeboxQueue(jukeboxObject, trackURIs);
                    }

                    Log.d(TAG, "Done updating Jukebox in cloud ...." + trackURIs.size());
                    mHandler.sendMessage(mHandler.obtainMessage(VIBE_JUKEBOX_REFRESH_TRACK_LIST_UI, trackURIs));
                } else {
                    Log.e(TAG, "Error fetching jukebox object in service.");
                    connectionError();
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateJukeboxAll(JukeboxObject jukebox)
    {
        if(DEBUG)
            Log.d(TAG, " -- update ALL --");

        final ParseGeoPoint geoPoint = getGeoPointFromMyLocation(mLocation);
        if(geoPoint == null){
            Log.e(TAG, "An error occurred getting the current location");
            return;
        }

        jukebox.put("name", mPlaylistName);
        jukebox.put("location", geoPoint);
        jukebox.put("queueSongIDs", mTrackUris);
        jukebox.put("defaultQueueSongIDs", mTrackUris);
        saveInBackground(jukebox);
        mHandler.sendEmptyMessage(VIBE_JUKEBOX_OBJECT_CREATION_DONE);
    }

    private void updateJukeboxTitle(JukeboxObject jukebox, String name)
    {
        if(DEBUG)
            Log.d(TAG, "update Jukebox Title in backend");

        jukebox.put("name", name);
        saveInBackground(jukebox);
    }

    private void updateJukeboxLocation(JukeboxObject jukebox, Location location )
    {
        final ParseGeoPoint geoPoint = getGeoPointFromMyLocation(location);
        if(geoPoint == null){
            Log.e(TAG, "An error occurred getting the current location");
            return;
        }

        jukebox.put("location", geoPoint);
        saveInBackground(jukebox);
    }

    private void updateJukeboxSongLists(JukeboxObject jukebox, List<String> trackUris)
    {
        jukebox.put("queueSongIDs", trackUris);
        jukebox.put("defaultQueueSongIDs", trackUris);
        saveInBackground(jukebox);
    }

    private void updateJukeboxQueue(JukeboxObject jukebox, List<String> trackUris)
    {
        jukebox.put("queueSongIDs", trackUris);
        saveInBackground(jukebox);
    }

    private void saveInBackground(JukeboxObject jukebox)
    {
        jukebox.saveInBackground();
    }

    private void fetchLastJukebox(JukeboxObject jukebox)
    {
        if(DEBUG)
            Log.d(TAG, " -- fetchLastJukebox -- ");

        mPlaylistName = jukebox.getName();
        mTrackUris = jukebox.getQueueSongIds();
        createTrackListFromURIs(mTrackUris);
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
                if (e == null) {
                    Log.d(TAG, "Success saving object in background ..");
                    mJukeboxId = jukebox.getObjectId();
                    mHandler.sendEmptyMessage(VIBE_JUKEBOX_OBJECT_CREATION_DONE);
                } else {
                    Log.e(TAG, "Something went wrong creating new jukebox object in Parse ...");
                }
            }
        });
    }

    /**
     * Utility function to get a List<Track> from a list of Spotify Uri ids.
     * @param trackURIs: Spotify Uri track ids currently in the queue.
     */
    private void createTrackListFromURIs(List<String> trackURIs)
    {
        String trackUriString = SpotifyClient.getTrackIds(trackURIs);

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        final List<Track> trackList = new ArrayList<>();

        //Get Several tracks Api point
        spotify.getTracks(trackUriString, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                Log.d(TAG, "Successful call to get Several tracks from Uri list");

                for (kaaes.spotify.webapi.android.models.Track track : tracks.tracks) {
                    Log.d(TAG, "Track name:  " + track.name);
                    Track vibeTrack = new Track(track.artists.get(0).name, track.name);
                    vibeTrack.setTrackName(track.name);
                    vibeTrack.setArtistName(track.artists.get(0).name);
                    trackList.add(vibeTrack);
                }

                mPlaylistTracks = new ArrayList<>(trackList);
                mHandler.sendEmptyMessage(VIBE_JUKEBOX_OBJECT_CREATION_DONE);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occurred getting the list of track Ids from Uri list:  " + error.getMessage());
            }
        });
    }

    private void connectionError()
    {
        if(DEBUG)
            Log.d(TAG, "connectionError" );

        final Messenger replyMessenger = mRequestMessage.replyTo;
        Message replyMessage = Message.obtain(null, VIBE_CONNECTION_ERROR);

        try {
            replyMessenger.send(replyMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

/** TO check */
    /*public void getJukeboxFromBackend(final String playlistName)
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
    }*/


 /*public void testFunction(Message msg)
    {
        Log.d(TAG, "TEST Function .....");

        boolean changed = msg.getData().getBoolean("trackChanged");

        Log.d(TAG, "TRACK CHANGED --->  " + changed);

        final Messenger replyMessenger = msg.replyTo;
        Message replyMessage = Message.obtain(null, VIBE_TEST);

        List<String> list = new ArrayList<>();
        list.add("One");
        list.add("Two");
        list.add("Three");

        Bundle data = new Bundle();
        data.putStringArrayList("list", (ArrayList<String>)list);

        replyMessage.setData(data);

        try {
            replyMessenger.send(replyMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }*/

/**

    private void getActiveJukeboxFromCloud(final boolean changeTrack)
    {
        if(DEBUG)
            Log.d(TAG, "getActiveJukeboxFromCloud with ID:  " + mJukeboxID);

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxID, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully retrieved Jukebox from cloud. ");
                    List<String> trackURIs = new ArrayList<>(jukeboxObject.getQueueSongIds());

                    mChangeTrack = changeTrack;

                    Log.d(TAG, "TRACK LIST SIZE:  " + trackURIs.size());
                    if(changeTrack ){
                        //Variable to update music that is playing
                        trackURIs.subList(0,1).clear();

                        if(trackURIs.size() == 0){
                            trackURIs = jukeboxObject.getDefaultQueueSongIds();
                        }
                        mPlayListTrackUris = new ArrayList<>(trackURIs);
                        mTrackUriHead = mPlayListTrackUris.get(0);
                        jukeboxObject.setQueueSongIds(mPlayListTrackUris);
                    }
                    mPlaylistHandler.sendMessage(mPlaylistHandler.obtainMessage(VIBE_CREATE_TRACK_FROM_URIS, trackURIs));
                } else {
                    Log.e(TAG, "Error fetching jukebox object..");
                    e.printStackTrace();
                }
            }
        });
    }

    private void getJukeboxFromBackend(final String playlistName)
    {
        ParseObject.registerSubclass(JukeboxObject.class);
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxID, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukebox, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully retrieved Jukebox from cloud with ID:  " + mJukeboxID);
                    jukebox.put("name", playlistName);
                    jukebox.saveInBackground();

                    if(getSupportActionBar() != null)
                        getSupportActionBar().setTitle(playlistName);

                } else {
                    Log.e(TAG, "Error fetching jukebox object, ID: " + mJukeboxID);
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.VIBE_APP_POOR_CONNECTION_MESSAGE),
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }


 */



