package com.vibejukebox.jukebox;

import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Sergex on 5/27/15.
 */
public final class Vibe {

    private static final String TAG = Vibe.class.getSimpleName();

    public static final String VIBE_NEARBY_JUKEBOXES = "NearbyJukeboxes";

    public static final String VIBE_CURRENT_LOCATION = "CurrentLocation";

    public static final String VIBE_JUKEBOX_ID = "JukeboxId";

    public static final String VIBE_IS_ACTIVE_PLAYLIST = "ActivePlaylist";

    public static final String VIBE_JUKEBOX_TRACK_URI_QUEUE= "queueSongIDs";

    public static final String VIBE_JUKEBOX_TRACKS_IN_QUEUE = "tracks";

    public static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    public static final String VIBE_JUKEBOX_STRING_PREFERENCE = "JukeboxStoredId";

    public static final String VIBE_JUKEBOX_LOCATION_PREFERENCE = "currentLocationObject";

    public static final String VIBE_JUKEBOX_ARTIST_RADIO = "ArtistRadio";

    private static Location mCurrentLocation;

    private Vibe(){

    }

    public static Location getCurrentLocation()
    {
        return mCurrentLocation;
    }

    public static void setCurrentLocation(Location location)
    {
        mCurrentLocation = location;
    }

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
    }

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
