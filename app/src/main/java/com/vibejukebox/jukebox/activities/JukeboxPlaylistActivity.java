package com.vibejukebox.jukebox.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;

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

import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
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
import com.vibejukebox.jukebox.service.VibeService;

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

public class JukeboxPlaylistActivity extends VibeBaseActivity implements
        PlayerNotificationCallback, ConnectionStateCallback
{
    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_ACCESS_TOKEN_PREF = "AccessToken";

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

    public static final int VIBE_TEST = 60;

    /** Side drawer Order of items */
    private static final int VIBE_CHANGE_PLAYLIST_NAME_DRAWER = 0;

    private static final int VIBE_CREATE_NEW_PLAYLIST_DRAWER = 1;

    private String mTrackUriHead;

    private final String TAG = JukeboxPlaylistActivity.class.getSimpleName();
    private final boolean DEBUG = DebugLog.DEBUG;

    private boolean mChangeTrack = true;

    private ListView songListView;

    private static String mJukeboxID;

    private Location mStoredLocation;

    private static SongListAdapter mSongListAdapter = null;

    private String mPlaylistName;

    private List<Track> mPlaylistTracks;
    private List<String> mPlayListTrackUris;

    /**
     * Used to get notifications from the system about the current network state in order
     * to pass them along to the player
     */
    private BroadcastReceiver mNetworkStateReceiver;

    private String mConnectivity;

    /**Custom Activity Toolbar and side drawer UI */
    private Toolbar mToolbar;
    ListView mDrawerListView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    List<DrawerItem> mDrawerItems = new ArrayList<>();

    /**  Vibe Bound Service fields */
    private Messenger mService = null;

    private Messenger mReplyMessenger = null;

    private Handler mPlaylistHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_REFRESH_ACTIVE_PLAYLIST:
                    refreshActivePlaylist((List<Track>)msg.obj);
                    break;
                /*case VIBE_CREATE_TRACK_FROM_URIS:
                    createTrackListFromURIs((List<String>)msg.obj);
                    break;*/
                case VIBE_GET_ACTIVE_JUKEBOX_AND_UPDATE:
                    Log.e(TAG, "About to get Jukebox from update -- > " + mChangeTrack);
                    getJukeboxFromCloud(mChangeTrack);
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

    //TODO - Set static to avoid leaks
    class ReplyHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case VIBE_CREATE_TRACK_FROM_URIS:
                    mChangeTrack = msg.getData().getBoolean("trackChanged");

                    //Update the Jukebox title
                    ActionBar actionbar = getSupportActionBar();
                    if(actionbar != null)
                        actionbar.setTitle(msg.getData().getString("name"));

                    createTrackListFromURIs(msg.getData().getStringArrayList("list"));
                    break;

                case VIBE_TEST:
                    Log.e(TAG, "Handling message in Activity (reply)");
                    List<String> list = new ArrayList<>(msg.getData().getStringArrayList("list"));
                    for(String item : list){
                        Log.d(TAG, "ITEM:  " + item);
                    }
                    break;
            }
        }
    }

    /**
     * Retrieves the stored Access token from Spotify Api
     * @return: Access token string
     */
    private String getAccessToken()
    {
        if(DEBUG)
            Log.d(TAG, "getCreatedJukeboxId -- ");

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String accessToken = preferences.getString(VIBE_JUKEBOX_ACCESS_TOKEN_PREF, null);
        return accessToken;
    }

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

        String playlistName = getIntent().getStringExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME);
        mJukeboxID = getIntent().getStringExtra(Vibe.VIBE_JUKEBOX_ID);
        mPlaylistTracks = getIntent().getParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE);
        mPlayListTrackUris = getIntent().getStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE);
        mStoredLocation = getIntent().getParcelableExtra("storedLocation");

        if(isActiveUser){
            Log.d(TAG, " -- ACTIVE Jukebox -- ");
            //Set the correct layout for user hosting a jukebox
            setContentView(R.layout.activity_jukebox_playlist);

            //Spotify Authentication response object
            mAuthResponse = getIntent().getParcelableExtra("authresponse");
            initializePlayer();
        } else {
            Log.d(TAG, " -- Joined Jukebox -- ");
            //Sets the correct layout for user that joined a jukebox
            setContentView(R.layout.activity_jukebox_playlist_join);
            getTrackAlbumArt(mPlayListTrackUris.get(0));
        }

        mPlaylistName = playlistName;
        setupMainLayout(playlistName, isActiveUser);

        /** Toolbar setup */
        /*mToolbar = (Toolbar)findViewById(R.id.mytoolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionbar = getSupportActionBar();
        if(actionbar != null)
        {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeButtonEnabled(true);
            actionbar.setTitle(playlistName);
        }

        //Create side drawer
        createDrawerUi(isActiveUser);

        // Display the Playlist (Track name, Artist name)
        displayTrackList(mPlaylistTracks);

        // Updates UI buttons according if user created or joined a Jukebox
        updatePlayerUiButtons(isActiveUser);*/

        //Establish a connection to the Vibe Service
        mReplyMessenger = new Messenger(new ReplyHandler());
        bindToService();
    }

    private void setupMainLayout(String playlistName, boolean isActiveUser)
    {
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
        createDrawerUi(isActiveUser);

        // Display the Playlist (Track name, Artist name)
        displayTrackList(mPlaylistTracks);

        // Updates UI buttons according if user created or joined a Jukebox
        updatePlayerUiButtons(isActiveUser);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(DEBUG)
                Log.d(TAG, " Vibe Service connected.");
            mService = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(DEBUG)
                Log.d(TAG, "Vibe Service disconnected.");
            mService = null;
        }
    };

    private void bindToService()
    {
        Intent intent = new Intent(getApplicationContext(), VibeService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void createDrawerUi(boolean isActive)
    {
        mDrawerItems.clear();
        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_CHANGE_PLAYLIST_NAME_ITEM),
                R.drawable.ic_mode_edit_white_36dp));

        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_CREATE_NEW_PLAYLIST_ITEM),
                R.drawable.ic_queue_music_white_36dp));

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);

        // Populate the Drawer with its options
        mDrawerListView = (ListView) findViewById(R.id.left_drawer);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mDrawerItems);

        //Able to change Jukebox name only available for Active jukebox
        if(!isActive){
            adapter.setJukeboxStatus(false);
        }

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

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.VIBE_APP_ITEM1, R.string.VIBE_APP_ITEM2)
        {
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
        if(DEBUG)
            Log.d(TAG, "Create New Playlist from drawer, Navigating back...");

        NavUtils.navigateUpFromSameTask(this);
    }

    /**
     * Launches a dialog to enter a new Jukebox name.
     */
    private void changeJukeboxNameDialog()
    {
        if(DEBUG)
            Log.d(TAG, "Change Name Dialog");

        // Use the Builder class for convenient dialog construction
        LayoutInflater inflator = LayoutInflater.from(this);
        final View view = inflator.inflate(R.layout.dialog_layout, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setPositiveButton(R.string.VIBE_APP_DONE, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final EditText editText = (EditText)view.findViewById(R.id.newJukeboxNameText);
                        String name = editText.getText().toString();

                        //Update the name of active jukebox in the Toolbar
                        if(getSupportActionBar() != null)
                            getSupportActionBar().setTitle(name);

                        mPlaylistName = name;

                        //Make the change in the backend
                        updateJukeboxInCloud(name);

                        /**getJukeboxFromBackend(name); */
                        mDrawerLayout.closeDrawers();
                    }
                })
                .setNegativeButton(R.string.VIBE_APP_CANCEL, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Cancel");
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
    protected void onResume() {
        super.onResume();

        mNetworkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                    if(mPlayer != null){
                        Connectivity connectivity = getNetworkConnectivity(getBaseContext());
                        Log.d(TAG, "Network state changed:  " + connectivity.toString());
                        mConnectivity = connectivity.toString();
                        mPlayer.setConnectivityStatus(connectivity);
                    }
                } else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                    Log.d(TAG, "PHONE AWAKEN");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mNetworkStateReceiver, filter);

        if(mPlayer != null && mPlayer.isInitialized()){
            mPlayer.addPlayerNotificationCallback(JukeboxPlaylistActivity.this);
            mPlayer.addConnectionStateCallback(JukeboxPlaylistActivity.this);
        }
    }

    /**
     * Registering for connectivity changes in Android does not actually deliver them to
     * us in the delivered intent.
     *
     * @param context Android context
     * @return Connectivity state to be passed to the SDK
     * @see com.spotify.sdk.android.player.Player#setConnectivityStatus(com.spotify.sdk.android.player.Connectivity)
     */
    private Connectivity getNetworkConnectivity(Context context)
    {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if(activeNetwork != null && activeNetwork.isConnected()){
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(DEBUG)
            Log.d(TAG, "onRestart -- ");

        Location location = Vibe.getCurrentLocation();
        if(!location.equals(mStoredLocation)){
            //Update location in the backend
            updateJukeboxInCloud(location);
            mStoredLocation = location;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mNetworkStateReceiver);

        //Not strictly necessary, but good practice.
        // Will prevent app from doing extra work in background when paused
        /*if(mPlayer != null){
            mPlayer.removePlayerNotificationCallback(JukeboxPlaylistActivity.this);
            mPlayer.removeConnectionStateCallback(JukeboxPlaylistActivity.this);
        }*/
    }

    @Override
    protected void onDestroy()
    {
        if(DEBUG)
            Log.e(TAG, "onDestroy -- ");

        if(mPlayer != null)
            mPlayer.pause();

        //To not leak resources player must be destroyed
        Spotify.destroyPlayer(this);

        //Unbind to the service
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if(DEBUG)
            Log.d(TAG, "Configuration Changed...");

        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            Log.d(TAG, "LANDSCAPE");
            setContentView(R.layout.activity_jukebox_playlist_land);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.d(TAG, "PORTRAIT");
            setContentView(R.layout.activity_jukebox_playlist);
        }

        handleConfigurationChange();
    }

    private void handleConfigurationChange()
    {
        setupMainLayout(mPlaylistName, true);

        //get update from backend
        getJukeboxFromCloud(false);
    }

    /*@Override
    public Object onRetainNonConfigurationInstance() {
        return mLoadSongListTask;
    }*/

    @Override
    protected void nearbyJukeboxesFound(List<JukeboxObject> jukeboxList) {
        /** Not needed in this Activity */
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        //TODO:clean up
        /*if(DEBUG){
            Log.d(TAG, "ON - NEW - INTENT -- ");
            Log.d(TAG, "POSITION = " + String.valueOf(intent.getIntExtra("position", 0)));
        }*/

        getJukeboxFromCloud(false);
    }

    /**
     * Function called when the user presses the refresh button on the Playlist.
     * @param view
     */
    public void refreshTrackButton(View view)
    {
        getJukeboxFromCloud(false);
    }

    /**
     * Function calls the Vibe service and gets the jukebox object from the backend and updates it.
     * @param trackChanged: boolean parameter to indicate if the player is changing the track to be played
     */
    private void getJukeboxFromCloud(boolean trackChanged)
    {
        //TODO: clean up function (not erase)
        Log.e(TAG, "getJukeboxFromCloud --> " + trackChanged);

        Bundle data = new Bundle();
        data.putBoolean("trackChanged", trackChanged);
        data.putString("jukeboxId", mJukeboxID);

        Message requestMessage = Message.obtain(null, VibeService.VIBE_GET_JUKEBOX_FOR_REFRESH);
        requestMessage.replyTo = mReplyMessenger;
        requestMessage.setData(data);

        try{
            mService.send(requestMessage);
        } catch(RemoteException e){
            e.printStackTrace();
        }
    }

    /**
     * Function sends a request message to update the active jukebox name in the backend.
     * @param name
     */
    private void updateJukeboxInCloud(String name)
    {
        if(DEBUG)
            Log.d(TAG, "updateJukeboxInCloud with Name:   " + name);

        Bundle data = new Bundle();
        data.putString("playlistName", name);

        Message requestMessage = Message.obtain(null, VibeService.VIBE_SERVICE_UPDATE_TITLE);
        requestMessage.setData(data);

        try{
            mService.send(requestMessage);
        } catch(RemoteException e){
            e.printStackTrace();
        }
    }

    /**
     * Function sends a request message to update the active jukebox name in the backend.
     * @param location
     */
    private void updateJukeboxInCloud(Location location)
    {
        if(DEBUG)
            Log.d(TAG, "updateJukeboxInCloud (location) ");

        Bundle data = new Bundle();
        data.putParcelable("updatedLocation", location);

        Message requestMessage = Message.obtain(null, VibeService.VIBE_SERVICE_UPDATE_LOCATION);
        requestMessage.setData(data);

        try{
            mService.send(requestMessage);
        } catch(RemoteException e){
            e.printStackTrace();
        }
    }

    /**
     * Function skips to next song in the current music queue
     * @param view
     */
    public void skipTrackButton(View view)
    {
        skipTrack();
    }

    private void skipTrack()
    {
        if(DEBUG)
            Log.d(TAG, "skipTrack --");

        if(mConnectivity.equals("Offline")){
            Log.d(TAG,"Connection Lost...");
            return;
        }

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
        String accessToken = getAccessToken();
        if(accessToken != null){
            Config playerConfig = new Config(this, accessToken, SPOTIFY_API_CLIENT_ID);
            mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player player) {
                    mPlayer.setConnectivityStatus(getNetworkConnectivity(JukeboxPlaylistActivity.this));
                    mPlayer.addConnectionStateCallback(JukeboxPlaylistActivity.this);
                    mPlayer.addPlayerNotificationCallback(JukeboxPlaylistActivity.this);
                    mTrackUriHead = mPlayListTrackUris.get(0);
                    mPlayer.play(mTrackUriHead);

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
     * Function to continue playing the music queue once the playlist has been updated.
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
     * Utility function to get a List<Track> from a list of Spotify Uri ids.
     * @param trackURIs: Spotify Uri track ids currently in the queue.
     * TODO: Refactor to avoid repeating code.
     */
    private void createTrackListFromURIs(List<String> trackURIs)
    {
        String trackUriString = SpotifyClient.getTrackIds(trackURIs);

        Log.e(TAG, "Uri String:  " + trackUriString);

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        final List<Track> trackList = new ArrayList<>();
        mTrackUriHead = trackURIs.get(0);
        mPlayListTrackUris = new ArrayList<>(trackURIs);

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

                mPlaylistTracks = new ArrayList<>(trackList);
                mPlaylistHandler.sendMessage(mPlaylistHandler.obtainMessage(VIBE_REFRESH_ACTIVE_PLAYLIST, trackList));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occurred getting the list of track Ids from Uri list:  " + error.getMessage());
            }
        });
    }

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
        Log.d(TAG, "onLoginFailed: " + throwable.getMessage());
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
        Log.e(TAG, "onPlaybackError: " + errorType.toString());

        switch(errorType){
            case TRACK_UNAVAILABLE:
                skipTrack();
                break;
            case ERROR_PLAYBACK:
                if(!mPlayListTrackUris.isEmpty())
                    mPlaylistHandler.sendEmptyMessage(VIBE_GET_ACTIVE_JUKEBOX_AND_UPDATE);
                break;
        }
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


/**
 * TODO: Refactor, repeated code
 * Function to change the jukebox name
 * @param playlistName
 */
    /*
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
    }*/

/* TODO: cleanup
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
    } */




/**
 * Gets the currently active Jukebox object and calls to update the Track list UI.
 */
    /*private void getActiveJukeboxFromCloud(final boolean changeTrack)
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
    }*/
