package com.vibejukebox.jukebox;

import android.util.Log;

import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;*/

/**
 * Created by Sergex on 8/24/14.
 */

public final class SpotifyClient
{
    private static final String TAG = SpotifyClient.class.getSimpleName();
    private SpotifyClient(){

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

    /**
     * Function strips the ids of each uri and in the List and makes a comma separated string of all ids
     * @param trackURIs: list of spotify track uris
     * @return
     */
    public static String getTrackIds(List<String> trackURIs)
    {
        String[] strippedItems;
        String trackIds = "";

        // Playlist limit request is 50 songs, so list must be trimmed if more requested
        /*List<String> modList = null;
        if(trackURIs.size() > 50){
            int offset = trackURIs.size() - 50;
            modList = new ArrayList<>(trackURIs.subList(offset, trackURIs.size()-1));
        }*/

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
    private static final String TAG = SpotifyClient.class.getSimpleName();
    private static final String BASE_URL = "https://accounts.spotify.com/";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        Log.d(TAG, "absolute URL is =======   " + getAbsoluteUrl(url));
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public static String getAbsoluteUrl(String relativeUrl)
    {
        return BASE_URL + relativeUrl;
    }

    public static void getSimpleReq(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.get(url, params, responseHandler);
    }

    public static void getWebApiType(String url, AsyncHttpResponseHandler responseHandler)
    {
        client.get(url, responseHandler);
    }*/

}
