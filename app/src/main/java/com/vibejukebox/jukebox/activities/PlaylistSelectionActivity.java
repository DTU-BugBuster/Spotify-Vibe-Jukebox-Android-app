package com.vibejukebox.jukebox.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Vibe;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * This class is used to choose the method of getting a playlist to start. It can be either selecting
 * a playlist from the users' Spotify account or making a playlist using the Music Recommender system
 * based on the users's starred tracks.
 */

public class PlaylistSelectionActivity extends AppCompatActivity {

    private static final String TAG = PlaylistSelectionActivity.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final String SPOTIFY_API_AUTH_RESPONSE = "authresponse";

    //private static final String SPOTIFY_API_USER_PLAYLISTS = "userplaylists";


    //private static final String SPOTIFY_API_NUMBER_OF_TRACKS_PER_PLAYLIST = "numberoftracks";

    private static final String SPOTIFY_API_USER_ID = "userID";

    /** Spotify Api objects */
    private SpotifyService mSpotify;

    private AuthenticationResponse mAuthResponse;

    /** Map to store the names and ids of user playlists */
    //private Map<String, String>mPlayListNamesAndIds;

    /** List stores the number of tracks in each playlist */
    //private Map<String, Integer> mNumOfTracks;

    /** Id of the Spotify user account */
    private String mUserId;

    /** Name of the user created Jukebox object */
    private String mJukeboxId;

    /** Current location */
    private Location mCurrentLocation;

    private String mPlaylistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        // Get the response to obtain the authorization code
        mAuthResponse = getIntent().getParcelableExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE);
        mCurrentLocation = getIntent().getParcelableExtra(Vibe.VIBE_CURRENT_LOCATION);
        mJukeboxId = getIntent().getStringExtra(Vibe.VIBE_JUKEBOX_ID);

        if(mJukeboxId != null)
            setContentView(R.layout.activity_playlist_selection_alt);
        else
            setContentView(R.layout.activity_playlist_selection);

        Toolbar toolbar = (Toolbar)findViewById(R.id.mytoolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(R.string.VIBE_APP_GET_YOUR_VIBE);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //TODO: Location
        if(mCurrentLocation == null)
            Log.e(TAG, "LOCATION NULL");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(DEBUG)
            Log.d(TAG, "onRestart -- ");
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //Sets the current Spotify logged in user
        getAndSetCurrentUser();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playlist_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == android.R.id.home){
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a playlist based on the current users's starred tracks
     * @param view
     */
    public void createPlaylistFromUserFavorites(View view)
    {
        if(DEBUG){
            Log.d(TAG, "createPlaylistFromUserFavorites");
        }

        //Start the Music Parameter Activity
        Intent intent = new Intent(this, MusicParameterActivty.class);
        intent.putExtra(SPOTIFY_API_AUTH_RESPONSE, mAuthResponse);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ARTIST_RADIO, false);
        startActivity(intent);
    }

    /**
     * Create a playlist based on an artist
     * @param view
     */
    public void getArtistRadioTracks(View view)
    {
        if(DEBUG){
            Log.d(TAG, "getArtistRadioTracks");
        }

        //Input artist and launch Parameter activity
        setArtistRadioDialog();
    }

    public void startLastTimeJukebox(View view)
    {
        Log.d(TAG, "Start Last Time Jukebox.");
    }

    /**
     * Select a playlist to load as starting track list to play
     * @param view
     */
    public void selectListFromUserPlaylists(View view)
    {
        if(DEBUG){
            Log.d(TAG, "selectListFromUserPlaylists");
        }

        //Starts the activity to view all user playlists
        Intent intent = new Intent(this, JukeboxSavedPlaylists.class);
        intent.putExtra(SPOTIFY_API_AUTH_RESPONSE, mAuthResponse);
        intent.putExtra(Vibe.VIBE_CURRENT_LOCATION, mCurrentLocation);
        intent.putExtra(SPOTIFY_API_USER_ID, mUserId);
        startActivity(intent);


        //launchUserPlaylistsActivity();
        //TODO: CLEAN UP
        /*SpotifyApi api = new SpotifyApi();
        api.setAccessToken(mAuthResponse.getAccessToken());

        Map<String, Object> params = new HashMap<>();
        params.put("limit", 40);
        params.put("offset", 1);

        //Get a List of a User's Playlists Api endpoint
        SpotifyService spotifyService = api.getService();
        spotifyService.getPlaylists(mUserId, params, new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                Log.d(TAG, "Successful call to get User playlists.");
                for (PlaylistSimple playlist : playlistSimplePager.items) {
                    //mPlaylists.add(playlist.name);
                    mPlayListNamesAndIds.put(playlist.name, playlist.id);
                    mNumOfTracks.put(playlist.name, playlist.tracks.total);
                }

                //Launch Activity to display all user playLists
                launchUserPlaylistsActivity(mPlayListNamesAndIds);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Failed getting user playlists...");

            }
        });*/
    }

    /**
     * Get the currently logged in user id from Spotify
     */
    private void getAndSetCurrentUser()
    {
        if(mAuthResponse != null){
            final String accessToken = mAuthResponse.getAccessToken();
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(SpotifyApi.SPOTIFY_WEB_API_ENDPOINT)
                    .setRequestInterceptor(new RequestInterceptor() {
                        @Override
                        public void intercept(RequestFacade request) {
                            request.addHeader("Authorization","Bearer " +accessToken);
                        }
                    })
                    .build();

            mSpotify = restAdapter.create(SpotifyService.class);
            mSpotify.getMe(new Callback<UserPrivate>() {
                @Override
                public void success(UserPrivate user, Response response) {
                    if (DEBUG)
                        Log.d(TAG, "Successful call to get current logged in user");
                    mUserId = user.id;
                    Log.d(TAG, "user:  " + user.display_name);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, "Failed to get current logged in user...");
                }
            });
        } else {
            Log.e(TAG, "Authentication Response object is NULL.");
        }
    }

    private void setArtistRadioDialog()
    {
        // Use the Builder class for convenient dialog construction
        LayoutInflater inflator = LayoutInflater.from(this);
        final View view = inflator.inflate(R.layout.artist_dialog_layout, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setPositiveButton(R.string.VIBE_APP_DONE, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final EditText editArtistText = (EditText)view.findViewById(R.id.artistForRadio);
                        String artist = editArtistText.getText().toString();

                        final EditText jukeboxedit = (EditText)view.findViewById(R.id.newJukeboxName);
                        String jukeboxName = jukeboxedit.getText().toString();

                        Log.e(TAG, "ARTIST: " + jukeboxName);
                        if(jukeboxName.equals(""))
                            jukeboxName = "Vibed Playlist";

                        //Start Activity with artist name
                        launchParameterActivityWithArtist(artist, jukeboxName);
                    }
                })
                .setNegativeButton(R.string.VIBE_APP_CANCEL, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG,"Cancel");
                    }
                });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    /**
     * TODO: set Chosen Artist for Artist radio
     * @param artistName
     * @param jukeboxName
     */
    private void launchParameterActivityWithArtist(String artistName, String jukeboxName)
    {
        //Start the Music Parameter Activity
        Intent intent = new Intent(this, MusicParameterActivty.class);
        intent.putExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE, mAuthResponse);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ARTIST_RADIO, true);
        intent.putExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME, jukeboxName);
        startActivity(intent);
    }



    private void launchUserPlaylistsActivity()
    {
        Intent intent = new Intent(this, JukeboxSavedPlaylists.class);
        intent.putExtra(Vibe.VIBE_CURRENT_LOCATION, mCurrentLocation);
        intent.putExtra(SPOTIFY_API_USER_ID, mUserId);
        intent.putExtra(SPOTIFY_API_AUTH_RESPONSE, mAuthResponse);
        startActivity(intent);
    }
}