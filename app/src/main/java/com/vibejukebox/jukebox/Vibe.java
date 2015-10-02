package com.vibejukebox.jukebox;


import android.location.Location;
import android.net.NetworkInfo.State;
import android.util.Log;

/**
 * Created by Sergex on 5/27/15.
 */
public final class Vibe
{
    public static final String VIBE_NEARBY_JUKEBOXES = "NearbyJukeboxes";

    public static final String VIBE_CURRENT_LOCATION = "CurrentLocation";

    public static final String VIBE_JUKEBOX_ID = "JukeboxId";

    public static final String VIBE_IS_ACTIVE_PLAYLIST = "ActivePlaylist";

    public static final String VIBE_JUKEBOX_TRACK_URI_QUEUE= "queueSongIDs";

    public static final String VIBE_JUKEBOX_TRACKS_IN_QUEUE = "tracks";

    public static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    public static final String VIBE_JUKEBOX_STRING_PREFERENCE = "JukeboxStoredId";

    public static final String VIBE_JUKEBOX_ARTIST_RADIO = "ArtistRadio";

    public static final String VIBE_JUKEBOX_START_WITH_ARTIST = "ArtistChosen";

    public static final String VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE = "authresponse";

    public static final String VIBE_JUKEBOX_PLAYLIST_NAME = "playlistName";

    public static final String VIBE_JUKEBOX_SERVICE_START_FETCH = "fetchJukeboxWithID";

    private static Location mCurrentLocation;

    private static int MODE_PRIVATE = 1;

    private static boolean isConnected = false;

    private Vibe(){
    }

    /**
     * Retrieves the created jukebox ID from the shared preferences.
     * @return: Jukebox object ID String.
     */
    /*private String getCreatedJukeboxId()
    {
        if (DEBUG)
            Log.d(TAG, "getCreatedJukeboxId -- ");

        Context context = JukeboxApplication.getAppContext();
        SharedPreferences preferences = context.getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(VIBE_JUKEBOX_STRING_PREFERENCE, null);

        if (DEBUG)
            Log.d(TAG, "----------------------------------- Returning ID from service: " + jukeboxId);

        return jukeboxId;
    }*/

    public static Location getCurrentLocation()
    {
        return mCurrentLocation;
    }

    public static void setCurrentLocation(Location location)
    {
        Log.e(" - VIBE CLASS - ", "Location Set:  " + location.getLatitude() + "  " + location.getLongitude());
        mCurrentLocation = location;
    }

    public static void setConnectionState(boolean state)
    {
        isConnected = state;
    }

    public static boolean getConnectivityStatus()
    {
        return isConnected;
    }

    /*
    public static void createTrackListFromURIs(List<String> trackURIs)
    {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        final List<Track> trackList = new ArrayList<>();
        String trackUriString = getTrackIds(trackURIs);

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

                //Join the active playlist and display it
                //mHandler.sendMessage(mHandler.obtainMessage(VIBE_JOIN_CURRENT_PLAYLIST, trackList));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occurred get the list of track Ids from Uri list:  " + error.getMessage());
            }
        });
    }

    public static String getTrackIds(List<String> trackURIs)
    {
        String[] strippedItems;
        String trackIds = "";

        int count = 0;
        for(String uri : trackURIs)
        {
            strippedItems = uri.split(":");
            trackIds += strippedItems[strippedItems.length -1];
            if(count < trackURIs.size()-1)
                trackIds += ",";
            count++;
        }
        return trackIds;
    }*/

   /*
    private String getCreatedJukeboxId()
    {
        Log.d(TAG, "getCreatedJukeboxId -- ");
        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(VIBE_JUKEBOX_STRING_PREFERENCE, null);

        Log.d(TAG, "----------------------------------------------------------- Returning ID: " + jukeboxId);
        return jukeboxId;
    }
    /*
    private void storeJukeboxID(String id)
    {
        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(VIBE_JUKEBOX_STRING_PREFERENCE, id);
        editor.commit();
    }*/
}
