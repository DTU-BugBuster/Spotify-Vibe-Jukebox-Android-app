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
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
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
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
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

import butterknife.Bind;
import butterknife.ButterKnife;
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
        PlayerNotificationCallback, ConnectionStateCallback {

    private static final String TAG = JukeboxPlaylistActivity.class.getSimpleName();

    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_ACCESS_TOKEN_PREF = "AccessToken";

    /** SPOTIFY Api variables */
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

    private static final int VIBE_CONNECTION_ERROR = 60;

    public static final int VIBE_TEST = 70;

    /** Side drawer Order of items */
    private static final int VIBE_NAVIGATE_HOME = 1;

    private static final int VIBE_CHANGE_PLAYLIST_NAME_DRAWER = 2;

    private static final int VIBE_CREATE_NEW_PLAYLIST_DRAWER = 3;

    private static final int VIBE_LOGOUT_SPOTIFY = 4;

    private final boolean DEBUG = DebugLog.DEBUG;

    private boolean mChangeTrack = true;

    private boolean mIsActiveUser = false;

    private boolean mRefresh = true;

    private ListView songListView;

    private static String mJukeboxId;

    private static SongListAdapter mSongListAdapter = null;

    private String mPlaylistName;

    private List<Track> mPlaylistTracks;

    private String mPlayListHeadUri;

    /**
     * Used to get notifications from the system about the current network state in order
     * to pass them along to the player
     */
    private BroadcastReceiver mNetworkStateReceiver;

    private String mConnectivity;

    /**Custom Activity Toolbar and side drawer UI */
    @Bind(R.id.mytoolbar) Toolbar mToolbar;
    @Bind(R.id.left_drawer) ListView mDrawerListView;
    @Bind(R.id.drawerLayout) DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;
    List<DrawerItem> mDrawerItems = new ArrayList<>();

    /** Circular Progress Bar */
    //CircularProgressView mProgressBar;

    /**  Vibe Bound Service fields */
    private Messenger mService = null;
    private Messenger mReplyMessenger = null;
    private boolean mServiceBound = false;

    /** Media Session Managers */
    private MediaSessionManager mSessionManager;
    private MediaSession mMediaSession;

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
    class ReplyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case VIBE_CREATE_TRACK_FROM_URIS:
                    mChangeTrack = msg.getData().getBoolean("trackChanged");

                    //Update the Jukebox title
                    ActionBar actionbar = getSupportActionBar();

                    if(actionbar != null) {
                        actionbar.setTitle(msg.getData().getString("name"));
                    }

                    createTrackListFromURIs(msg.getData().getStringArrayList("list"));
                    break;

                case VIBE_CONNECTION_ERROR:
                    errorConnectionToast();
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

    private void errorConnectionToast() {
        Toast.makeText(this, R.string.VIBE_APP_POOR_CONNECTION_MESSAGE, Toast.LENGTH_LONG).show();
    }

    /**
     * Retrieves the stored Access token from Spotify Api
     * @return: Access token string
     */
    private String getAccessToken() {
        if(DEBUG) {
            Log.d(TAG, "getCreatedJukeboxId -- ");
        }

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        return preferences.getString(VIBE_JUKEBOX_ACCESS_TOKEN_PREF, null);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(DEBUG) {
            Log.d(TAG, "onCreate -- ");
        }

        //Get variables sent with intent
        Intent intent = getIntent();
        mIsActiveUser = intent.getBooleanExtra(Vibe.VIBE_IS_ACTIVE_PLAYLIST, false);

        String playlistName = intent.getStringExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME);
        mJukeboxId = intent.getStringExtra(Vibe.VIBE_JUKEBOX_ID);
        mPlaylistTracks = intent.getParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE);

        //mPlayListTrackUris = intent.getStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE);
        mPlayListHeadUri = intent.getStringExtra(Vibe.VIBE_JUKEBOX_QUEUE_HEAD_URI);

        mPlaylistName = playlistName;

        if(mIsActiveUser) {
            Log.d(TAG, " -- ACTIVE Jukebox -- ");
            //Set the correct layout for user hosting a jukebox
            setContentView(R.layout.activity_jukebox_playlist);

            //Spotify Authentication response object
            //mAuthResponse = getIntent().getParcelableExtra("authresponse");
            initializePlayer();
        } else {
            Log.d(TAG, " -- Joined Jukebox -- ");
            //Sets the correct layout for user that joined a jukebox
            setContentView(R.layout.activity_jukebox_playlist_join);
            //getTrackAlbumArt(mPlayListTrackUris.get(0));
            getTrackAlbumArt(mPlayListHeadUri);
        }

        ButterKnife.bind(this);

        //Setup main Player Ui
        setupMainLayout(playlistName, mIsActiveUser);

        //Establish a connection to the Vibe Service
        mReplyMessenger = new Messenger(new ReplyHandler());
        bindToService();
    }

    /**
     * Function sets up the main components of the activity according to if it is a joined jukebox
     * or active jukebox.
     * @param playlistName: Name of the current active Jukebox
     * @param isActiveUser: boolean value if user joined or created a jukebox
     */
    private void setupMainLayout(String playlistName, boolean isActiveUser) {
        //Toolbar setup
        setSupportActionBar(mToolbar);

        ActionBar actionbar = getSupportActionBar();
        if(actionbar != null) {
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
            if(DEBUG) {
                Log.d(TAG, " Vibe Service connected.");
            }
            mService = new Messenger(service);
            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(DEBUG) {
                Log.d(TAG, "Vibe Service disconnected.");
            }
            mService = null;
            mServiceBound = false;
        }
    };

    private void bindToService() {
        Intent intent = new Intent(getApplicationContext(), VibeService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void createDrawerUi(boolean isActive) {
        mDrawerItems.clear();
        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_HOME),
                R.drawable.ic_home_white_24dp));

        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_CHANGE_PLAYLIST_NAME_ITEM),
                R.drawable.ic_mode_edit_white_36dp));

        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_CREATE_NEW_PLAYLIST_ITEM),
                R.drawable.ic_queue_music_white_36dp));

        mDrawerItems.add(new DrawerItem(getResources().getString(R.string.VIBE_APP_DRAWER_LOGOUT_SPOTIFY),
                R.drawable.ic_person_white_24dp));

        // Populate the Drawer with its options
        //List header
        View headerView = getLayoutInflater().inflate(R.layout.drawer_header, mDrawerListView, false);
        mDrawerListView.addHeaderView(headerView, null, false);

        DrawerListAdapter adapter = new DrawerListAdapter(this, mDrawerItems);

        // Change Jukebox name feature only available for Active jukebox
        if(!isActive) {
            adapter.setJukeboxStatus(false);
        }

        mDrawerListView.setAdapter(adapter);
        // Drawer Item click listeners
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case VIBE_NAVIGATE_HOME:
                        navigateHome();
                        break;
                    case VIBE_CHANGE_PLAYLIST_NAME_DRAWER:
                        changeJukeboxNameDialog();
                        break;
                    case VIBE_CREATE_NEW_PLAYLIST_DRAWER:
                        createNewPlaylistFromDrawer();
                        break;
                    case VIBE_LOGOUT_SPOTIFY:
                        logoutSpotify();
                        break;
                    default:
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
        mDrawerToggle.syncState();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    private void navigateHome() {
        Intent intent = new Intent(this, VibeJukeboxMainActivity.class);
        startActivity(intent);

        finish();
    }

    private void createNewPlaylistFromDrawer() {
        if(DEBUG) {
            Log.d(TAG, "Create New Playlist from drawer, Navigating back...");
        }

        storeAccessToken(getAccessToken());
        NavUtils.navigateUpFromSameTask(this);
    }

    /**
     * Launches a dialog to enter a new Jukebox name.
     */
    private void changeJukeboxNameDialog() {
        if(DEBUG) Log.d(TAG, "Change Name Dialog");

        // Use the Builder class for convenient dialog construction
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.dialog_layout, null);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            CharSequence title = actionBar.getTitle();
            if(title != null) {
                EditText name = (EditText)view.findViewById(R.id.newJukeboxNameText);
                name.setText(title.toString());
            }
        }

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
    protected void onNewIntent(Intent intent) {
        if(DEBUG) {
            Log.d(TAG, "onNewIntent -- ");
        }

        mRefresh = intent.getBooleanExtra(Vibe.VIBE_JUKEBOX_CALL_REFRESH, true);
        getJukeboxFromCloud(false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(DEBUG) {
            Log.d(TAG, "onRestart -- ");
        }

        //Refresh Jukebox from cloud when user navigates back to app.
        //Jukebox is not refreshed when navigating back from adding a song.
        if(mRefresh) {
            refreshJukebox();
        }
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

                        //Display to the user if connectivity status is offline
                        if(mConnectivity.equals("Offline"))
                            displayConnectivity(true);
                        else
                            displayConnectivity(false);

                        mPlayer.setConnectivityStatus(connectivity);
                    }
                } else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                    Log.e(TAG, "PHONE AWOKEN");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mNetworkStateReceiver, filter);

        if(mPlayer != null && mPlayer.isInitialized()) {
            mPlayer.addPlayerNotificationCallback(JukeboxPlaylistActivity.this);
            mPlayer.addConnectionStateCallback(JukeboxPlaylistActivity.this);
        }
    }

    private void displayConnectivity(boolean isOffline) {
        TextView offlineBar = (TextView)findViewById(R.id.offlineBar);
        if(isOffline) {
            offlineBar.setVisibility(View.VISIBLE);
        } else {
            offlineBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Set to true to refresh jukebox when user navigates back to app.
        mRefresh = true;
    }

    /**
     * Registering for connectivity changes in Android does not actually deliver them to
     * us in the delivered intent.
     * @param context Android context
     * @return Connectivity state to be passed to the SDK
     * @see com.spotify.sdk.android.player.Player#setConnectivityStatus(com.spotify.sdk.android.player.Connectivity)
     */
    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if(activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mNetworkStateReceiver);
    }

    @Override
    protected void onDestroy() {
        if(DEBUG) {
            Log.d(TAG, "onDestroy -- ");
        }

        mChangeTrack = false;
        if(mPlayer != null) {
            mPlayer.pause();
        }

        //To not leak resources player must be destroyed
        Spotify.destroyPlayer(this);

        //Unbind to the service
        unbindService(mConnection);
        mServiceBound = false;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);

        if(DEBUG) {
            Log.d(TAG, "Configuration Changed...");
        }

        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "LANDSCAPE");
            if(mIsActiveUser) {
                setContentView(R.layout.activity_jukebox_playlist_land);
            } else {
                setContentView(R.layout.activity_jukebox_playlist_join_land);
            }
        }

        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "PORTRAIT");
            if(mIsActiveUser) {
                setContentView(R.layout.activity_jukebox_playlist);
            } else {
                setContentView(R.layout.activity_jukebox_playlist_join);
            }
        }

        handleConfigurationChange();
    }

    private void handleConfigurationChange() {
        if(DEBUG) {
            Log.d(TAG, "handleConfigurationChange -- ");
        }

        setupMainLayout(mPlaylistName, mIsActiveUser);

        //get update from backend, boolean value is trackChanged
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

    /**
     * Function called when the user presses the refresh button on the Playlist.
     * @param view
     */
    public void refreshTrackButton(View view) {
        refreshJukebox();
    }

    private void refreshJukebox() {
        getJukeboxFromCloud(false);
    }

    /**
     * Function calls the Vibe service and gets the jukebox object from the backend and updates it.
     * @param trackChanged: boolean parameter to indicate if the player is changing the track to be played
     */
    private void getJukeboxFromCloud(boolean trackChanged) {
        if(DEBUG) {
            Log.d(TAG, "getJukeboxFromCloud --> " + trackChanged);
        }

        Bundle data = new Bundle();
        data.putBoolean("trackChanged", trackChanged);
        data.putString("jukeboxId", mJukeboxId);

        Message requestMessage = Message.obtain(null, VibeService.VIBE_GET_JUKEBOX_FOR_REFRESH);
        requestMessage.replyTo = mReplyMessenger;
        requestMessage.setData(data);

        try {
            mService.send(requestMessage);
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function sends a request message to update the active jukebox name in the backend.
     * @param name
     */
    private void updateJukeboxInCloud(String name)
    {
        if(DEBUG) {
            Log.d(TAG, "updateJukeboxInCloud with Name:   " + name);
        }

        Bundle data = new Bundle();
        data.putString("playlistName", name);

        Message requestMessage = Message.obtain(null, VibeService.VIBE_SERVICE_UPDATE_TITLE);
        requestMessage.setData(data);

        try {
            mService.send(requestMessage);
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function sends a request message to update the active jukebox location in the backend.
     * @param location
     */
    private void updateJukeboxInCloud(Location location) {
        if(DEBUG) {
            Log.d(TAG, "updateJukeboxInCloud (location) ");
        }

        Bundle data = new Bundle();
        data.putParcelable("updatedLocation", location);

        Message requestMessage = Message.obtain(null, VibeService.VIBE_SERVICE_UPDATE_LOCATION);
        requestMessage.setData(data);

        try {
            mService.send(requestMessage);
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function skips to next song in the current music queue
     * @param view
     */
    public void skipTrackButton(View view) {
        skipTrack();
    }

    private void skipTrack() {
        if(DEBUG) {
            Log.d(TAG, "skipTrack --");
        }

        if(mConnectivity.equals("Offline")) {
            Log.d(TAG,"Connection Lost...");
            Toast.makeText(this, R.string.VIBE_APP_CONNECTION_LOST, Toast.LENGTH_LONG).show();
            return;
        }

        mChangeTrack = true;
        if(mPlayer != null) {
            mPlayer.pause();
        }
    }


    /**
     * Sets the music player bar consistent if the user is a host or joined a jukebox
     * @param hostUser: Boolean value indicating if the current user is the active jukebox
     *                  or just joining a playlist.
     */
    private void updatePlayerUiButtons(boolean hostUser)
    {
        if(DEBUG) {
            Log.d(TAG, "updatePlayerUiButtons -- ");
        }

        if(hostUser) {
            ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
            if(playButton != null) {
                if(mPlayer != null) {
                    if(mCurrentPlayerState.playing) {
                        playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                    } else {
                        playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
                    }
                }
            }
        }
    }

    /**
     * Initializes Spotify player to begin music streaming.
     */
    private void initializePlayer() {
        String accessToken = getAccessToken();
        if(accessToken != null) {
            Config playerConfig = new Config(this, accessToken, SPOTIFY_API_CLIENT_ID);
            mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player player) {
                    //Sometimes this check
                    if(mPlayer == null)
                        mPlayer = player;

                    mPlayer.setConnectivityStatus(getNetworkConnectivity(JukeboxPlaylistActivity.this));
                    mPlayer.addConnectionStateCallback(JukeboxPlaylistActivity.this);
                    mPlayer.addPlayerNotificationCallback(JukeboxPlaylistActivity.this);
                    mPlayer.play(mPlayListHeadUri);

                    //Update Player button
                    ImageButton play = (ImageButton)findViewById(R.id.playButton);
                    play.setImageDrawable(getResources().getDrawable(R.drawable.pause));

                    //Update Album Art
                    //getTrackAlbumArt(mPlayListTrackUris.get(0));
                    getTrackAlbumArt(mPlayListHeadUri);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e(TAG, "Could not initialize player :  " + throwable.getMessage());
                }
            });
        }
    }

    /*private void makeNotification()
    {
        NotificationCompat.Builder mNotifBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.play)
                .setContentTitle("My Notification")
                .setContentText("Hello Notifcations!");

        Intent resultIntent = new Intent(this, JukeboxPlaylistActivity.class);

        //The stack builder has an artificial back stack for the started activity
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(JukeboxPlaylistActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotifBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifManager.notify(1, mNotifBuilder.build());

    }*/

    /**
     * Function to continue playing the music queue once the playlist has been updated.
     */
    private void continuePlayback() {
        if(mPlayer.isInitialized()) {
            mPlayer.play(mPlayListHeadUri);
            //getTrackAlbumArt(mTrackUriHead);
        }
    }

    private void getTrackAlbumArt(String trackUri) {
        if(DEBUG) {
            Log.d(TAG, "getTrackAlbumArt -->  " + trackUri);
        }

//        mProgressBar = (CircularProgressView)findViewById(R.id.progress_bar);
//        mProgressBar.setColor(getResources().getColor(R.color.vibe_border));
//        mProgressBar.setVisibility(View.VISIBLE);
//        mProgressBar.startAnimation();

        //Strip only the id of the Spotify URI
        String strippedURI = trackUri.split(":")[2];

        //SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = mApi.getService();

        //Get a Track Api point
        spotify.getTrack(strippedURI, new Callback<kaaes.spotify.webapi.android.models.Track>() {
            @Override
            public void success(kaaes.spotify.webapi.android.models.Track track, Response response) {
                if (DEBUG)
                    Log.d(TAG, "Successful call to get Track Art.");

                String url;
                List<Image> images = track.album.images;

                if (images.size() > 0)
                    url = images.get(1).url;
                else
                    url = "";

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
    private void insertAlbumArt(String url) {
        if(DEBUG) {
            Log.d(TAG, "insertAlbumArt - " + url);
        }

        //mProgressBar.setVisibility(View.GONE);

        ImageView imageView = (ImageView)findViewById(R.id.trackArt);
        if(url.equals("")) {
            return;
        } else {
            Picasso.with(this).load(url).centerCrop().fit().into(imageView);
        }
    }

    /**
     * Display the playlist track list in the activity.
     * @param trackList: List of tracks of the playlist to play.
     */
    private void displayTrackList(List<Track> trackList) {
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
    public void playMusic(View view) {
        if(DEBUG) {
            Log.d(TAG, "playMusic -- (View)");
        }

        //mChangeTrack = false;
        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
        if(mCurrentPlayerState.playing) {
            //Set changeTrack to false so no update from the backend is requested
            mChangeTrack = false;

            //Spotify Player
            mPlayer.pause();

            //Ui play/pause button
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
        } else {
            //Spotify player
            mPlayer.resume();
            mChangeTrack = true;
            //Ui play/pause button
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        }
    }

    /**
     * Refreshes the list of tracks on the UI (Track name, Artist name)
     * @param tracks: List of Vibe Tracks
     */
    private void refreshActivePlaylist(List<Track> tracks) {
        if(DEBUG) {
            Log.d(TAG, "refreshActivePlaylist -- ");
        }

        if(mChangeTrack && mIsActiveUser){
            continuePlayback();
        }

        getTrackAlbumArt(mPlayListHeadUri);

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
    private void createTrackListFromURIs(List<String> trackURIs) {
        if(DEBUG) {
            Log.d(TAG, "createTrackListFromURIs -- ");
        }

        String trackUriString = SpotifyClient.getTrackIds(trackURIs);

        //SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = mApi.getService();
        final List<Track> trackList = new ArrayList<>();
        mPlayListHeadUri = trackURIs.get(0);

        //Get Several tracks Api point
        spotify.getTracks(trackUriString, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                if (DEBUG)
                    Log.d(TAG, "Successful call to get Several tracks from Uri list");

                for (kaaes.spotify.webapi.android.models.Track track : tracks.tracks) {
                    /*if (DEBUG)
                        Log.d(TAG, "Track name:  " + track.name);*/
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
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //NavUtils.navigateUpFromSameTask(this);
                return true;

            /** Plus sign on the action bar goes to the search activity to add a song from Spotify
             * Only available when user has joined an active playlist.
             */
            case R.id.addSongFromSpotify:
                Intent intent = new Intent(this, MusicSearchActivity.class);
                intent.putExtra(VIBE_JUKEBOX_ID, mJukeboxId);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateLocation(Location location) {
        if(DEBUG) {
            Log.d(TAG, "updateLocation");
        }

        if(mServiceBound) {
            updateJukeboxInCloud(location);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);
        updateLocation(mLastLocation);
    }

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
        if(DEBUG) {
            Log.e(TAG, " onLoginFailed: " + throwable.getMessage());
        }

        if(mCurrentPlayerState.playing) {
            mChangeTrack = false;
            mPlayer.pause();
        }

        //Credentials have expired, must re-login to Spotify
        loginToSpotify(this);
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
        if(DEBUG) {
            Log.d(TAG, "onPlaybackEvent");
        }
        mCurrentPlayerState = playerState;

        switch(eventType){
            case TRACK_CHANGED:
                if(DEBUG) Log.d(TAG, "TRACK_CHANGED");
                break;
            case PLAY:
                if(DEBUG) Log.d(TAG, "PLAY_EVENT");
                break;
            case PAUSE:
                if(DEBUG) Log.d(TAG, "PAUSE_EVENT  " + mChangeTrack);
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
                if(!mPlaylistTracks.isEmpty()) {
                    mPlaylistHandler.sendEmptyMessage(VIBE_GET_ACTIVE_JUKEBOX_AND_UPDATE);
                }
                break;
        }
    }

    @Override
    protected void checkSpotifyProduct(final AuthenticationResponse authResponse) {
        if(DEBUG) {
            Log.d(TAG, "checkSpotifyProduct -- ");
        }

        //Reset variable set to false in onLoginFailed
        mChangeTrack = true;

        //Re-login to Spotify
        mPlayer.login(getAccessToken());
    }

    public void logoutSpotify() {
        if(DEBUG){
            Log.d(TAG, "logoutSpotify ..... ");
        }

        if(mCurrentPlayerState.playing) {
            mPlayer.pause();
            mPlayer.logout();
        }

        AuthenticationClient.clearCookies(this);
        Intent intent = new Intent(this, VibeJukeboxMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}