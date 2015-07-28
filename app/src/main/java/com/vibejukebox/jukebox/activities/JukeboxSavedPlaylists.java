package com.vibejukebox.jukebox.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.Vibe;
import com.vibejukebox.jukebox.VibePlaylist;
import com.vibejukebox.jukebox.service.VibeService;
import com.vibejukebox.jukebox.adapters.SavedPlayListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class JukeboxSavedPlaylists extends AppCompatActivity
{
    private static final String TAG = JukeboxSavedPlaylists.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_STRING_PREFERENCE = "JukeboxStoredId";

    private static final int VIBE_START_JUKEBOX_CREATION = 10;

    private static final int VIBE_GET_USER_PLAYLISTS = 20;

    //private static final String SPOTIFY_API_USER_PLAYLISTS = "userplaylists";
    //private static final String SPOTIFY_API_NUMBER_OF_TRACKS_PER_PLAYLIST = "numberoftracks";

    private static final String SPOTIFY_API_USER_ID = "userID";

    private String mUserId;

    private Map<String, Integer> mNumOfTracks;

    private Map<String, String> mPlaylistNamesAndIds;

    //TODO: Implement object.
    private Map<String, VibePlaylist> mVibePlaylistObject;


    private AuthenticationResponse mAuthResponse;

    private List<String> mPlaylistTrackUris;

    private List<Track> mPlaylistTracks;

    private Map<String, String> mPlaylistImages;

    private String mChosenPlaylistName;

    private Location mCurrentLocation;

    private String mJukeboxId;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){
                case VIBE_START_JUKEBOX_CREATION:
                    startJukeboxCreation(mChosenPlaylistName);
                    break;
                case VIBE_GET_USER_PLAYLISTS:
                    displayPlaylists((List<String>)msg.obj);
                    break;
            }
            return false;
        }
    });

    /**
     * Retrieves the created jukebox ID from the stored preferences.
     * @return: Jukebox object ID String.
     */
    private String getCreatedJukeboxId()
    {
        if(DEBUG)
            Log.d(TAG, "getCreatedJukeboxId -- ");

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(VIBE_JUKEBOX_STRING_PREFERENCE, null);
        Log.d(TAG, "------------------------------------------------------ Returning ID: " +  jukeboxId);
        return jukeboxId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(DEBUG)
            Log.d(TAG, "onCreate -- ");
        setContentView(R.layout.activity_jukebox_saved_playlists);

        Toolbar toolbar = (Toolbar)findViewById(R.id.mytoolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(R.string.title_activity_jukebox_saved_playlists);
        }

        Intent intent = getIntent();
        mCurrentLocation = intent.getParcelableExtra(Vibe.VIBE_CURRENT_LOCATION);

        // Spotify API variables
        mUserId = intent.getStringExtra(SPOTIFY_API_USER_ID);
        mAuthResponse = intent.getParcelableExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE);

        mPlaylistNamesAndIds = new HashMap<>();
        mVibePlaylistObject = new HashMap<>();
        mNumOfTracks = new HashMap<>();
        getUserPlaylists();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(DEBUG)
            Log.d(TAG,"onRestart -- ");
        mJukeboxId = getCreatedJukeboxId();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.jukebox_saved_playlists, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets the user's Spotify account saved playlists
     */
    private void getUserPlaylists()
    {
        if(DEBUG){
            Log.d(TAG, "getUserPlaylists");
        }

        mPlaylistImages = new HashMap<>();
        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(mAuthResponse.getAccessToken());

        Map<String, Object> params = new HashMap<>();
        params.put("limit", 50);
        params.put("offset", 1);

        //TODO: user id value null after a crash
        //Get a List of a User's Playlists Api endpoint
        SpotifyService spotifyService = api.getService();
        spotifyService.getPlaylists(mUserId, params, new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                Log.d(TAG, "Successful call to get User playlists.");
                for (PlaylistSimple playlist : playlistSimplePager.items) {

                    //If the playlist is empty, just move along
                    if (playlist.tracks.total <= 0)
                        continue;

                    //mPlaylists.add(playlist.name);

                    //TODO: (In test) Sort out if object will be used
                    VibePlaylist vPlayListObject = new VibePlaylist(playlist.id, playlist.name, playlist.owner.id);
                    vPlayListObject.setNumOfTracks(playlist.tracks.total);
                    mVibePlaylistObject.put(playlist.id, vPlayListObject);

                    mPlaylistNamesAndIds.put(playlist.name, playlist.id);
                    mNumOfTracks.put(playlist.name, playlist.tracks.total);

                    List<Image> images = playlist.images;
                    String url = images.get(0).url;
                    mPlaylistImages.put(playlist.name, url);
                }

                List<String> list = new ArrayList<>(mPlaylistNamesAndIds.keySet());
                mHandler.sendMessage(mHandler.obtainMessage(VIBE_GET_USER_PLAYLISTS, list));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed getting user playlists..." + error.getMessage());
            }
        });
    }

    /**
     * Get all the tracks of a user playlist.
     * @param playlistId : The id of the playlist chosen
     */
    private void getPlaylistTracks(String playlistId, String owner)
    {
        if(DEBUG)
            Log.d(TAG, "getPlaylistTracks -- owned by: " + owner);

        Log.d(TAG, "AUTH RESPONSE:   " + mAuthResponse.getAccessToken());

        mPlaylistTrackUris = new ArrayList<>();
        mPlaylistTracks = new ArrayList<>();

        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(mAuthResponse.getAccessToken());

        //Get a playlist's tracks Api endpoint
        SpotifyService spotify = api.getService();
        spotify.getPlaylistTracks(owner, playlistId, new Callback<Pager<PlaylistTrack>>() {
            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                Log.d(TAG, "Successful call to get Playlist tracks");
                for (PlaylistTrack track : playlistTrackPager.items) {
                    Log.d(TAG, "Track:  " + track.track.name);

                    //Local tracks not supported, yet.
                    if(!track.is_local)
                        mPlaylistTrackUris.add(track.track.uri);

                    List<ArtistSimple> artistSimple = new ArrayList<>(track.track.artists);
                    Track song = new Track();
                    song.setArtistName(artistSimple.get(0).name);
                    song.setTrackName(track.track.name);
                    mPlaylistTracks.add(song);
                }

                //Currently Spotify Api has a limit of 50 songs in each query, so for larger playlists
                //se get the last 30 tracks only.
                if(mPlaylistTrackUris.size() > 50){
                    Log.e(TAG, "**************************   Playlist is too big:  " + mPlaylistTrackUris.size());
                    int offset = mPlaylistTrackUris.size() - 30;
                    mPlaylistTrackUris = new ArrayList<>(mPlaylistTrackUris.subList(offset, mPlaylistTrackUris.size()-1));
                    mPlaylistTracks = new ArrayList<>(mPlaylistTracks.subList(offset, mPlaylistTracks.size()-1));
                }
                mHandler.sendEmptyMessage(VIBE_START_JUKEBOX_CREATION);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to get Tracks for Playlist...  " + error.getMessage());
                error.printStackTrace();
            }
        });
    }

    /**
     * Display the list of user playlists in the activity
     * @param list
     */
    private void displayPlaylists(List<String> list)
    {
        //Get adapter view from layout
        ListView playlistView = (ListView)findViewById(R.id.listV);

        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                if(DEBUG)
                    Log.d(TAG, "onItemClicked -- ");

                List<String> ids = new ArrayList<>(mPlaylistNamesAndIds.values());
                //List<String> ids = new ArrayList<>(mVibePlaylistObject.keySet());
                String chosenID = ids.get(position);
                Log.e(TAG, "PLAYLIST ID CHOSEN  -- >   " + chosenID);

                TextView tv = (TextView)v.findViewById(R.id.playlistName);
                mChosenPlaylistName = tv.getText().toString();

                // Create a jukebox with the right name and respective Tracks
                getPlaylistTracks(chosenID, mVibePlaylistObject.get(chosenID).getOwner());
            }
        });

        SavedPlayListAdapter sAdapter = new SavedPlayListAdapter(this, list, mNumOfTracks, mPlaylistImages);
        playlistView.setAdapter(sAdapter);
    }

    /*@Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(DEBUG)
            Log.d(TAG, "onItemClicked -- ");

        List<String> ids = new ArrayList<>(mPlaylistNamesAndIds.values());
        String chosenID = ids.get(position);
        Log.e(TAG, "PLAYLIST ID CHOSEN  -- >   " + chosenID);

        TextView tv = (TextView)v.findViewById(R.id.playlistName);
        mChosenPlaylistName = tv.getText().toString();

        // Create a jukebox with the right name and respective Tracks
        getPlaylistTracks(chosenID);
    }*/

    /**
     * Launches the service to create a jukebox with the playlist data in Parse backend
     */
    private void startJukeboxCreation(String playlistName)
    {
        if(DEBUG)
            Log.d(TAG, "Starting Jukebox Creation with playlist:  " + playlistName);

        Intent intent = new Intent(getApplicationContext(), VibeService.class);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ID, mJukeboxId);
        intent.putExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE, mAuthResponse);
        intent.putExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME, playlistName);
        intent.putExtra(Vibe.VIBE_CURRENT_LOCATION, mCurrentLocation);
        intent.putStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE, (ArrayList<String>) mPlaylistTrackUris);
        intent.putParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE, (ArrayList<Track>) mPlaylistTracks);

        //The jukebox gets created in the Service
        startService(intent);
    }
}