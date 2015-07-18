package com.vibejukebox.jukebox.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
//import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;
import com.squareup.picasso.Picasso;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.DrawerItem;
import com.vibejukebox.jukebox.DrawerListAdapter;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.SpotifyClient;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.Vibe;
import com.vibejukebox.jukebox.adapters.SongListAdapter;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
This class is used to display the tracks of a current active playlist. It can be either as a
 user hosting the playlist or as a user joining the active playlist. As essential components it
 needs:
 - List of songs as Track objects
 - List of track URIs
 */

public class JukeboxPlaylistActivity extends AppCompatActivity implements
        PlayerNotificationCallback, ConnectionStateCallback
{
    /** SPOTIFY Api variables */
    private AuthenticationResponse mAuthResponse;

    private Player mPlayer;

    private PlayerState mCurrentPlayerState = new PlayerState();

    /** App client Spotify ID */
    private static final String SPOTIFY_API_CLIENT_ID="0cd11be7bf8d4e59a21f5961a7a70723";

    /** Vibe Jukebox variables */
    private static final String VIBE_JUKEBOX_ID = "JukeboxID";

    private static final String VIBE_TRACK_QUEUE_LIST = "TrackQueue";

    /**Vibe Playlist Handler variables */
    private static final int VIBE_REFRESH_ACTIVE_PLAYLIST = 10;

    private static final int VIBE_CREATE_TRACK_FROM_URIS = 20;

    private static final int VIBE_GET_ACTIVE_JUKEBOX_AND_UPDATE = 30;

    private static final int VIBE_CONTINUE_PLAYBACK = 40;

    private static final int VIBE_INSERT_ALBUM_ART = 50;

    /** Side drawer Order of items */
    private static final int VIBE_CHANGE_PLAYLIST_NAME_DRAWER = 0;

    private static final int VIBE_CREATE_NEW_PLAYLIST_DRAWER = 1;

    private String mTrackUriHead;

    private final String TAG = JukeboxPlaylistActivity.class.getSimpleName();
    private final boolean DEBUG = DebugLog.DEBUG;

    private boolean mChangeTrack = true;

    private ListView songListView;

    private static String mJukeboxID;

    private static SongListAdapter mSongListAdapter = null;

    private List<Track> mPlaylistTracks;
    private List<String> mPlayListTrackUris;

    private Handler mPlaylistHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_REFRESH_ACTIVE_PLAYLIST:
                    refreshActivePlaylist((List<Track>)msg.obj);
                    break;
                case VIBE_CREATE_TRACK_FROM_URIS:
                    createTrackListFromURIs((List<String>)msg.obj);
                    break;
                case VIBE_GET_ACTIVE_JUKEBOX_AND_UPDATE:
                    getActiveJukeboxFromCloud(mChangeTrack);
                    break;
                case VIBE_CONTINUE_PLAYBACK:
                    continuePlayback();
                    break;
                case VIBE_INSERT_ALBUM_ART:
                    insertAlbumArt((String)msg.obj);
                    break;
            }

            return false;
        }
    });

    /**Custom Activity Toolbar and side drawer UI */
    private Toolbar mToolbar;
    ListView mDrawerListView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    List<DrawerItem> mDrawerItems = new ArrayList<>();

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        //Get variables sent with intent
        Intent intent = getIntent();
        boolean isActiveUser = intent.getBooleanExtra(Vibe.VIBE_IS_ACTIVE_PLAYLIST, false);
        boolean isJoiningJukebox = intent.getBooleanExtra("joiningJukebox", false);

        String playlistName = getIntent().getStringExtra("playlistName");
        mJukeboxID = getIntent().getStringExtra(Vibe.VIBE_JUKEBOX_ID);
        mPlaylistTracks = getIntent().getParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE);
        mPlayListTrackUris = getIntent().getStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE);

        if(isActiveUser){
            //Set the correct layout for user hosting a jukebox
            setContentView(R.layout.activity_jukebox_playlist);

            //Spotify Authentication response object
            mAuthResponse = getIntent().getParcelableExtra("authresponse");
            initializePlayer();
        } else {
            //Sets the correct layout for user that joined a jukebox
            setContentView(R.layout.activity_jukebox_playlist_join);
            getTrackAlbumArt(mPlayListTrackUris.get(0));
        }

        //Hack for now until better solution
        //if(isJoiningJukebox)
            //isActiveUser = !isActiveUser;

        /** Toolbar setup */
        mToolbar = (Toolbar)findViewById(R.id.mytoolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionbar = getSupportActionBar();
        if(actionbar != null)
        {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeButtonEnabled(true);
            actionbar.setTitle(playlistName);
        }

        //Create side drawer
        createDrawerUi();

        // Display the Playlist (Track name, Artist name)
        displayTrackList(mPlaylistTracks);

        // Updates UI buttons according if user created or joined a Jukebox
        updatePlayerUiButtons(isActiveUser);
    }

    private void createDrawerUi()
    {
        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_CHANGE_PLAYLIST_NAME_ITEM),
                R.drawable.ic_mode_edit_white_36dp));
        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_CREATE_NEW_PLAYLIST_ITEM),
                R.drawable.ic_queue_music_white_36dp));

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);

        // Populate the Drawer with its options
        mDrawerListView = (ListView) findViewById(R.id.left_drawer);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mDrawerItems);
        mDrawerListView.setAdapter(adapter);

        // Drawer Item click listeners
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case VIBE_CHANGE_PLAYLIST_NAME_DRAWER:
                        changeJukeboxNameDialog();

                        break;
                    case VIBE_CREATE_NEW_PLAYLIST_DRAWER:
                        createNewPlaylistFromDrawer();
                        break;
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.VIBE_APP_ITEM1, R.string.VIBE_APP_ITEM2){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.d(TAG, "onDrawerOpened -- ");
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.d(TAG, "onDrawerClosed -- ");
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void createNewPlaylistFromDrawer()
    {
        NavUtils.navigateUpFromSameTask(this);
    }

    /**
     * Launches a dialog to enter a new Jukebox name.
     */
    private void changeJukeboxNameDialog()
    {
        // Use the Builder class for convenient dialog construction
        LayoutInflater inflator = LayoutInflater.from(this);
        final View view = inflator.inflate(R.layout.dialog_layout, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setPositiveButton(R.string.VIBE_APP_DONE, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final EditText editText = (EditText)view.findViewById(R.id.newJukeboxNameText);
                        String name = editText.getText().toString();
                        getJukeboxFromBackend(name);
                        mDrawerLayout.closeDrawers();
                    }
                })
                .setNegativeButton(R.string.VIBE_APP_CANCEL, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG,"Cancel");
                        mDrawerLayout.closeDrawers();
                    }
                 });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState)
    {
        super.onPostCreate(savedInstanceState, persistentState);
        //mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onDestroy()
    {
        if(DEBUG)
            Log.d(TAG, "onDestroy -- ");
        //To not leak resources player must be destroyed
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        //TODO:clean up
        /*if(DEBUG){
            Log.d(TAG, "ON - NEW - INTENT -- ");
            Log.d(TAG, "POSITION = " + String.valueOf(intent.getIntExtra("position", 0)));
        }*/

        getActiveJukeboxFromCloud(false);
    }

    /**
     * Function called when the user presses the refresh button on the Playlist.
     * @param view
     */
    public void refreshTrackButton(View view)
    {
        getActiveJukeboxFromCloud(false);
    }

    /**
     * TODO:Function gets updated track list from the backend and skips to the next track
     * @param view
     */
    public void skipTrackButton(View view)
    {
        if(DEBUG)
            Log.d(TAG, "skipTrackButton -- (View)");
        mChangeTrack = true;
        if(mPlayer != null)
            mPlayer.pause();
    }

    /**
     * Sets the music player bar consistent if the user is a host or joined a jukebox
     * @param hostUser: Boolean value indicating if the current user is the active jukebox
     *                  or just joining a playlist.
     */
    private void updatePlayerUiButtons(boolean hostUser)
    {
        if(DEBUG)
            Log.d(TAG, "updatePlayerUiButtons -- ");

        if(hostUser)
        {
            ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
            if(playButton != null)
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        }
    }

    /**
     * Initializes Spotify player to begin music streaming.
     */
    private void initializePlayer()
    {
        String accessToken = mAuthResponse.getAccessToken();
        if(accessToken != null){
            Config playerConfig = new Config(this, accessToken, SPOTIFY_API_CLIENT_ID);
            mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player player) {
                    mPlayer.addConnectionStateCallback(JukeboxPlaylistActivity.this);
                    mPlayer.addPlayerNotificationCallback(JukeboxPlaylistActivity.this);
                    mPlayer.play(mPlayListTrackUris.get(0));

                    //Update Album Art
                    getTrackAlbumArt(mPlayListTrackUris.get(0));
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e(TAG, "Could not initialize player :  " + throwable.getMessage());
                }
            });
        }
    }

    /**
     * Function to continue playing the music queue once the Playlist has been updated.
     */
    private void continuePlayback()
    {
        if(mPlayer.isInitialized()){
            mPlayer.play(mTrackUriHead);
            //getTrackAlbumArt(mTrackUriHead);
        }
    }

    private void getTrackAlbumArt(String trackUri)
    {
        if(DEBUG)
            Log.d(TAG, "getTrackAlbumArt -->  " + trackUri);

        //Strip only the id of the Spotify URI
        String strippedURI = trackUri.split(":")[2];

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        //Get a Track Api point
        spotify.getTrack(strippedURI, new Callback<kaaes.spotify.webapi.android.models.Track>() {
            @Override
            public void success(kaaes.spotify.webapi.android.models.Track track, Response response) {
                List<Image> images = track.album.images;
                String url = images.get(1).url;

                //Insert the album art image
                mPlaylistHandler.sendMessage(mPlaylistHandler.obtainMessage(VIBE_INSERT_ALBUM_ART, url));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occurred getting the album art of a Track:  " + error.getMessage());
            }
        });
    }

    /**
     * Inserts the Album art into the view
     * @param url: image URL
     */
    private void insertAlbumArt(String url)
    {
        ImageView imageView = (ImageView)findViewById(R.id.trackArt);
        Picasso.with(this).load(url).centerCrop().fit().into(imageView);
    }

    /**
     * Display the playlist track list in the activity.
     * @param trackList: List of tracks of the playlist to play.
     */
    private void displayTrackList(List<Track> trackList)
    {
        songListView = (ListView)findViewById(R.id.list);

        mSongListAdapter = new SongListAdapter(this, (ArrayList<Track>)trackList, false);
        mSongListAdapter.setSearch(false);
        mSongListAdapter.notifyDataSetChanged();
        songListView.setAdapter(mSongListAdapter);
    }

    /**
     * Play/Pause button on the player tool bar to control music streaming.
     * Only enabled when user the host of active playlist
     * TODO: Fix deprecated functions.
     * @param view
     */
    public void playMusic(View view)
    {
        if(DEBUG)
            Log.d(TAG, "playMusic -- (View)");

        mChangeTrack = false;
        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
        if(mCurrentPlayerState.playing)
        {
            //Spotify Player
            mPlayer.pause();

            //Ui play/pause button
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
        }
        else
        {
            //Spotify player
            mPlayer.resume();
            //Ui play/pause button
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        }
    }

    /*@Override
    public Object onRetainNonConfigurationInstance() {
        return mLoadSongListTask;
    }*/

    private void setQueueSongs(String jukeboxId, List<String> songs)
    {
        if(DEBUG)
            Log.d(TAG, "setQueueSongs -- ");

        ParseObject jukebox = ParseObject.createWithoutData("JukeBox", jukeboxId);
        jukebox.put("queueSongIDs", songs);
        jukebox.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    if (DEBUG)
                        Log.d(TAG, "Succeeded saving queue songs for active jukebox. ");
                } else {
                    Log.e(TAG, "ERROR: failed to save songs .");
                    if (DEBUG)
                        e.printStackTrace();
                }
            }
        });
    }

    /**
     * Refreshes the list of tracks on the UI (Track name, Artist name)
     * @param tracks: List of Vibe Tracks
     */
    private void refreshActivePlaylist(List<Track> tracks)
    {
        if(DEBUG)
            Log.d(TAG, "refreshActivePlaylist -- ");

        if(mChangeTrack){
            continuePlayback();
        }

        getTrackAlbumArt(mTrackUriHead);

        mChangeTrack = true;
        mSongListAdapter = new SongListAdapter(this, (ArrayList<Track>)tracks, false);
        mSongListAdapter.notifyDataSetChanged();
        songListView.setAdapter(mSongListAdapter);
    }

    /**
     * Gets the currently active Jukebox object and calls to update the Track list UI.
     */
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

    /**
     * TODO: Refactor, repeated code
     * Function to change the jukebox name
     * @param playlistName
     */
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

    /**
     * Utility function to get a List<Track> from a list of Spotify Uri ids.
     * @param trackURIs: Spotify Uri track ids currently in the queue.
     * TODO: Refactor to avoid repeating code.
     */
    private void createTrackListFromURIs(List<String> trackURIs)
    {
        String trackUriString = SpotifyClient.getTrackIds(trackURIs);

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        final List<Track> trackList = new ArrayList<>();
        mTrackUriHead = trackURIs.get(0);

        Log.e(TAG, "HEAD:  " + mTrackUriHead);

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

                mPlaylistTracks = trackList;
                mPlaylistHandler.sendMessage(mPlaylistHandler.obtainMessage(VIBE_REFRESH_ACTIVE_PLAYLIST, trackList));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occurred get the list of track Ids from Uri list:  " + error.getMessage());
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    /*@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.jukebox_playlist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;

            /** Plus sign on the action bar goes to the search activity to add a song from Spotify
             * Only available when user has joined an active playlist.
             */
            case R.id.addSongFromSpotify:
                Log.d(TAG, "Add songs... ");

                Intent intent = new Intent(this, MusicSearchActivity.class);
                intent.putExtra(VIBE_JUKEBOX_ID, mJukeboxID);
                intent.putParcelableArrayListExtra(VIBE_TRACK_QUEUE_LIST, (ArrayList<Track>)mPlaylistTracks);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /*@Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Log.d(TAG, "CLICKED on playlist activity");
        super.onListItemClick(l, v, position, id);
    }*/

    @Override
    public void onLoggedIn() {
        Log.d(TAG, "onLoggedIn");
    }

    @Override
    public void onLoggedOut() {
        Log.d(TAG, "onLoggedOut");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.e(TAG, "onLoginFailed: " + throwable.getMessage());
    }

    @Override
    public void onTemporaryError() {
        Log.d(TAG, "onTemporaryError");
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.d(TAG, "onConnectionMessage");
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState)
    {
        if(DEBUG)
            Log.d(TAG, "onPlaybackEvent");
        mCurrentPlayerState = playerState;

        switch(eventType){
            case TRACK_CHANGED:
                Log.d(TAG, "TRACK_CHANGED");
                break;
            case PLAY:
                Log.d(TAG, "PLAY_EVENT");
                break;
            case PAUSE:
                Log.d(TAG, "PAUSE_EVENT");
                if(mChangeTrack)
                    mPlaylistHandler.sendEmptyMessage(VIBE_GET_ACTIVE_JUKEBOX_AND_UPDATE);
                break;
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        Log.e(TAG, "onPlaybackError");
    }
}


/*

    private void updatePlayerUiButtons(boolean hostUser)
    {
        Log.d(TAG, "updatePlayerUiButtons --->  "+ hostUser);
        //TODO: Handle deprecated method below
        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
        ImageButton nextTrackButton = (ImageButton)findViewById(R.id.nextTrackButton);

        if(!hostUser){
            playButton.setVisibility(View.GONE);
            nextTrackButton.setVisibility(View.GONE);

            ImageButton refreshButton = (ImageButton)findViewById(R.id.refreshButton);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(200, 60);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            refreshButton.setBackgroundResource(R.drawable.shape_solid);
            refreshButton.setLayoutParams(layoutParams);
        }
        else
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));

//        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
//        if(playButton != null)
//            playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
    }


 */
