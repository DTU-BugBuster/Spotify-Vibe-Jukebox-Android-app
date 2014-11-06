package com.vibejukebox.jukebox;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JukeboxPlaylistActivity extends ListActivity implements
        PlayerNotificationCallback, ConnectionStateCallback
{
    private final String TAG = JukeboxPlaylistActivity.class.getSimpleName();
    private final boolean DEBUG = DebugLog.DEBUG;
    private final String DEFAULT_QUEUE_SONG_IDS = "defaultQueueSongIDs";
    private final String QUEUE_SONG_IDS = "queueSongIDs";
    private final String LOOKUP_SPOTIFY_ID = "http://ws.spotify.com/lookup/1/.json?uri="; //TODO: migrate to new Web API
    private static final String OWN_JUKEBOX = "on_own_jukebox";
    private static final int JUKEBOX_OBJECT_SET = 1;
    private static final int JUKEBOX_SONG_ADDED = 2;
    private static final int JUKEBOX_TRACK_CHANGED = 3;
    private static final int JUKEBOX_FETCH_OBJECT = 4;
    private static final int JUKEBOX_RESTART = 5;
    private static final int JUKEBOX_POP_QUEUE_HEAD = 6;

    private boolean mIsOwnJukebox = false;

    private ListView songListView;
    private Track mTrack;
    private List<Track> mTrackList;
    private JukeboxObject mJukeboxObject = null;
    private static String mJukeboxID;
    private static String mJukeboxName;
    private List<String> mRawSongIDs;
    private List<String> mCurrentlyPlayingSongList;

    private boolean mObjectSet = false;
    private boolean mChangeTrack = false;
    private boolean mPlayerInit = false;
    private boolean mIsSkipped = false;
    private boolean mRefreshList = false;
    private boolean mFetchbeforeInitPlayer = false;

    //TODO: Showing the speaker icon
    private boolean mCurrentPlayingTrack = false;
    private boolean mQueueOver = false;

    static SongListAdapter mSongListAdapter = null;

    private LoadJukeboxSongList mLoadSongListTask = null;
    private Player mPlayer;
    private PlayerState mCurrentPlayerState = new PlayerState();

    AsyncHttpClient mClient = new AsyncHttpClient();

    Handler mObjectHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){
                case JUKEBOX_OBJECT_SET:
                    Log.d(TAG, "JUKEBOX_OBJECT_SET");
                    mObjectSet = true;
                    if(mRefreshList) {
                        refreshListUI();
                    }

                    if(mFetchbeforeInitPlayer)
                        initializePlayer();
                    if(mQueueOver)
                        resetJukebox();
                    return true;

                case JUKEBOX_FETCH_OBJECT:
                    if(!mObjectSet)
                        fetchJukeboxObject(mRefreshList);

                case JUKEBOX_SONG_ADDED:
                    if(mJukeboxObject != null)
                        setSongQueue(mCurrentlyPlayingSongList);
                    return true;

                case JUKEBOX_POP_QUEUE_HEAD:
                    mObjectSet = true;
                    popLastPlayedSong(mObjectSet);
                    return true;

                case JUKEBOX_RESTART:
                    restartJukebox(mRawSongIDs.get(0));
                    return true;

            }
            return false;
        }
    });

    private void restartJukebox(String songHead)
    {
        if(DEBUG)
            Log.d(TAG, "restartJukebox -- ");

        if(mPlayerInit) {
            //mPlayer.pause();
            mPlayer.play(songHead);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jukebox_playlist);

        mJukeboxID = getIntent().getStringExtra("JUKEBOXID");
        mJukeboxName = getIntent().getStringExtra("JUKEBOXNAME");
        mIsOwnJukebox = getIntent().getBooleanExtra(OWN_JUKEBOX, false);

        if(mIsOwnJukebox) {
            mRawSongIDs = getIntent().getStringArrayListExtra("rawSongIDs");

            if(mRawSongIDs != null)
                mCurrentlyPlayingSongList = new ArrayList<String>(mRawSongIDs);
            else {
                mCurrentlyPlayingSongList = new ArrayList<String>();
                mRawSongIDs = new ArrayList<String>();
            }

            // TODO: When on a created jukebox, adding songs not supported yet, so hide it
            ActionBar actionBar = getActionBar();
            actionBar.hide();

            fetchJukeboxObject(false);
        }

        // Updates UI buttons according if user created or joined a Jukebox
        updateJukeboxButtons(mIsOwnJukebox);

        setTitle(mJukeboxName);
        displaySongQueue();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(DEBUG)
            Log.d(TAG, "onStart -- ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUG)
            Log.d(TAG, "onResume -- ");
    }

    private List<String> getSongQueue()
    {
        if(DEBUG)
            Log.d(TAG, "getSongQueue - ");

        if(mJukeboxObject != null && mObjectSet)
            return mJukeboxObject.getQueueSongIds();
        else
            return null;
    }

    private void setSongQueue(List<String> songQueueIds)
    {
        /*for(String song: songQueueIds){
            Log.d(TAG, "songssss:   " + song);
        }*/

        /*//mCurrentlyPlayingSongList = new ArrayList<String>(songQueueIds);
        if(mJukeboxObject != null) {
            //mJukeboxObject.put("defaultQueueSongIDs", mRawSongIDs);
            //mJukeboxObject.put("queueSongIDs", songQueueIds);
            //mJukeboxObject.saveInBackground();
        }*/

        if(mPlayerInit)
            mObjectHandler.sendEmptyMessage(JUKEBOX_TRACK_CHANGED);
    }

    private void updateJukeboxButtons(boolean ownJukebox)
    {
        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
        Button nextTrackButton = (Button)findViewById(R.id.nextTrackButton);


        if(!ownJukebox){
            playButton.setVisibility(View.GONE);
            nextTrackButton.setVisibility(View.GONE);
        }
        else
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        if(DEBUG){
            Log.d(TAG, "ON - NEW - INTENT -- ");
            Log.d(TAG, "POSITION = " + String.valueOf(intent.getIntExtra("position", 0)));
        }

        List<Track> trackList = intent.getParcelableArrayListExtra("trackadded");
        Track track = trackList.get(0);

        mTrackList.add(intent.getIntExtra("position", 0), track);
        mObjectHandler.sendEmptyMessage(JUKEBOX_SONG_ADDED);

        mSongListAdapter = new SongListAdapter(this, (ArrayList<Track>)mTrackList, false);
        mSongListAdapter.notifyDataSetChanged();
        songListView.setAdapter(mSongListAdapter);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void displaySongQueue()
    {
        if(DEBUG)
            Log.d(TAG, "displaySongQueue ---  ");

        Intent intent = getIntent();
        List<String> list;
        mTrackList = new ArrayList<Track>();

        if(intent.hasExtra(QUEUE_SONG_IDS))
        {
            Log.e(TAG, "HAS QUEUE SONGS  --- -- - ");
            list = intent.getStringArrayListExtra(QUEUE_SONG_IDS);
            mRawSongIDs = intent.getStringArrayListExtra("rawSongIDs");
            Log.d(TAG, "SIZE:  "  + String.valueOf(list.size()));

            mLoadSongListTask = (LoadJukeboxSongList) getLastNonConfigurationInstance();
            if(mLoadSongListTask != null)
            {
                mLoadSongListTask.mActivity = this;
            }

            else
            {
                mLoadSongListTask = new LoadJukeboxSongList(this);
                mLoadSongListTask.mActivity = this;
                mLoadSongListTask.execute(list);
            }

            //new LoadJukeboxSongList().execute(list);
            int numSongs = list.size();
            Log.e(TAG, "NUmber of tracks in the jukebox QUEUE ----->   " + String.valueOf(list.size()));
            if(numSongs == 0)
            {
                Toast.makeText(this, getString(R.string.jukebox_empty), Toast.LENGTH_LONG).show();
                return;
            }
        }
        else
            Log.e(TAG, "Receiving nothing from intent .... ");
    }

    public void addTrackToList(Track track)
    {
        if(DEBUG)
            Log.d(TAG, "addTrackToList -- ");

        mTrackList.add(track);
    }

    public JukeboxPlaylistActivity getActivity()
    {
        return this;
    }

    //Update Ui with list of songs from chosen jukebox, called once Asynctask is finished
    private void updateTrackList(List<Track> list, boolean showAddSong)
    {
        if(DEBUG)
            Log.d(TAG, "updateTRACKList --   " + String.valueOf(list.size()));

        songListView = getListView();
        if(songListView == null){
            return;
        }

        mTrackList.clear();

        for (Track track : list)
        {
            mTrackList.add(track);
        }

        mSongListAdapter = new SongListAdapter(this, (ArrayList<Track>)list, showAddSong);
        mSongListAdapter.notifyDataSetChanged();
        songListView.setAdapter(mSongListAdapter);
    }

    public static SongListAdapter getSongAdapter()
    {
        return mSongListAdapter;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mLoadSongListTask;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(DEBUG)
            Log.d(TAG, "onPause -- ");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if(DEBUG)
            Log.d(TAG, "onStop -- ");

        if(mLoadSongListTask != null)
            mLoadSongListTask.cancel(true);
    }

    @Override
    protected void onDestroy()
    {
        if(DEBUG)
            Log.d(TAG, "onDestroy -- ");

        //To not leak resources player must be destroyed
        if(mIsOwnJukebox) {
            Log.d(TAG, "Player Destroyed.");
            Spotify.destroyPlayer(this);
        }
        super.onDestroy();

        //TODO: fix error - "unable to destroy activity.."
        if(this.isFinishing() && mLoadSongListTask != null)
            mLoadSongListTask.cancel(false);

        mLoadSongListTask.mActivity = null;
    }

    /** Function called from refresh button on UI  */
    public void refreshList(View view) throws JSONException
    {
        if(DEBUG){
            Log.d(TAG, "RefreshListUI -- ");
            Log.d(TAG, "***************");
        }

        refreshListUI();
    }

    private void refreshListUI()
    {
        if(DEBUG)
            Log.d(TAG, "refreshListUI -- ");

        mLoadSongListTask = null;
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxID, new GetCallback<JukeboxObject>() {

            @SuppressWarnings("unchecked")
            @Override
            public void done(JukeboxObject jukeboxObj, ParseException e) {
                if(e == null){
                    Log.e(TAG, "Successfully retrieved object to refresh List UI");
                    List<String> songList = new ArrayList<String>();
                    List<String> rawIds = jukeboxObj.getQueueSongIds();

                    for (String id : rawIds) {
                        songList.add(LOOKUP_SPOTIFY_ID + id);
                    }

                    if(mIsSkipped) {
                        songList.subList(0, 1).clear();
                        mIsSkipped = false;
                    }

                    mLoadSongListTask = new LoadJukeboxSongList(getActivity());
                    mLoadSongListTask.execute(songList);
                }
                else
                    Log.e(TAG, "Something went wrong retrieving jukebox object");
            }
        });
    }

    private void refreshListToPlay(List<String> updatedList)
    {
        if(DEBUG)
            Log.d(TAG, " -- refreshListToPlay -- > number of songs : " + String.valueOf(updatedList.size()));

        mCurrentlyPlayingSongList = new ArrayList<String>(updatedList);
        mChangeTrack = false;

        refreshListUI();
        mPlayer.play(updatedList.get(0));
    }

    private void initializePlayer()
    {
        if(DEBUG)
            Log.d(TAG, "initializePlayer -- ");

        String accessToken = StartingPlaylistActivity.getAccessToken();
        if(accessToken!= null){
            Spotify spotify = new Spotify(accessToken);
            mPlayer = spotify.getPlayer(this, "Vibe", this, new Player.InitializationObserver() {
                @Override
                public void onInitialized() {
                    Log.d(TAG, "Player Initialized ... ");
                    mPlayer.addConnectionStateCallback(JukeboxPlaylistActivity.this);
                    mPlayer.addPlayerNotificationCallback(JukeboxPlaylistActivity.this);
                    mPlayerInit = true;
                    mCurrentlyPlayingSongList = new ArrayList<String>(mJukeboxObject.getQueueSongIds());
                    mPlayer.play(mJukeboxObject.getQueueSongIds().get(0));
                }
                @Override
                public void onError(Throwable throwable) {
                    Log.e(TAG, "Could not Initialize player....  " + throwable.getMessage());
                }
            });

            ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_pause));
        }
    }

    public void playMusic(View view)
    {
        if(DEBUG)
            Log.d(TAG, "playMusic -- (View)");

        if(!mPlayerInit){
            mFetchbeforeInitPlayer = true;
            fetchJukeboxObject(false);
        }
        else
            playPauseMusic();
    }

    public void skipToNext(View view)
    {
        if(DEBUG)
            Log.d(TAG, "skipToNext -- (View)");

        skipSong();

        /*if(mPlayer != null)
            mPlayer.skipToNext();*/
    }

    private void skipSong()
    {
        mChangeTrack = true;
        fetchJukeboxAndPopSong(true);
    }

    private void popLastPlayedSong(boolean isObjectSet)
    {
        if(DEBUG)
            Log.d(TAG, "popLastPlayedSong");

        if(isObjectSet){
            List<String>currentList = new ArrayList<String>(mJukeboxObject.getQueueSongIds());
            Log.d(TAG, "ORIGINAL currentSize : " + currentList.size());
            mObjectSet = false;

            Log.d(TAG, "boolean VAR if List is empty:  " + currentList.isEmpty() + "  number of tracks:  " + currentList.size());
            if(currentList.size() > 0){
                currentList.subList(0,1).clear();
                setQueueSongs(mJukeboxID, currentList);
                if(!currentList.isEmpty())
                    refreshListToPlay(currentList);
                else
                    resetJukebox();
            }
            else{
                resetJukebox();
            }
        }
    }

    private void setQueueSongs(String jukeboxId, List<String> songs)
    {
        if(DEBUG)
            Log.d(TAG, "setQueueSongs -- ");

        ParseObject jukebox = ParseObject.createWithoutData("JukeBox", jukeboxId);
        jukebox.put("queueSongIDs", songs);
        jukebox.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
               if(e == null){
                   if(DEBUG)
                    Log.d(TAG, "Succeeded saving queue songs for active jukebox. ");
               }
                else{
                   Log.e(TAG, "ERROR: failed to save songs .");
                   if(DEBUG)
                    e.printStackTrace();
               }
            }
        });
    }

    /**
     * Function called when the last song of the jukebox queue has been played.
     * The list gets reset to the original default queue.
     */
    private void resetJukebox()
    {
        if(DEBUG)
            Log.d(TAG, " -- resetJukebox");

        mCurrentlyPlayingSongList = mRawSongIDs;
        mFetchbeforeInitPlayer = false;
        mChangeTrack = false;

        ParseObject jukebox = ParseObject.createWithoutData("JukeBox", mJukeboxID);
        jukebox.put("queueSongIDs", mRawSongIDs);
        jukebox.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null){
                    Log.d(TAG, "Successfully reset jukebox to default Queue");
                    mQueueOver = false;
                    mObjectHandler.sendEmptyMessage(JUKEBOX_RESTART);
                }
                else{
                    Log.e(TAG, "Failed reset Jukebox ... ");
                }
            }
        });

        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
        playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_pause));
    }

    private void playPauseMusic()
    {
        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);

        if(mCurrentPlayerState.playing) {
            mPlayer.pause();
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));
        }
        else {
            mPlayer.resume();
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_pause));
        }
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState)
    {
        if(DEBUG)
            Log.d(TAG, "onPlaybackEvent -- ");

        //String event = eventType.name().toLowerCase(Locale.ENGLISH).replaceAll("_", " ");
        //Log.e(TAG, "Player Event - " + event);

        switch (eventType){
            case BECAME_ACTIVE:
                Log.e(TAG, "BECAME_ACTIVE");
                mChangeTrack = false;
                break;

            case PAUSE:
                Log.e(TAG, "PAUSE");
                mChangeTrack = false;
                break;

            case PLAY:
                Log.e(TAG, "PLAY");
                mChangeTrack = false;
                //refreshListUI();
                break;

            case TRACK_CHANGED:
                Log.e(TAG, "TRACK_CHANGED");
                if(mChangeTrack){
                    Log.d(TAG, "TRACK CHANGED *tracked* " + mChangeTrack);
                    fetchJukeboxAndPopSong(true);
                }
                mChangeTrack = false;
                break;

            case END_OF_CONTEXT:
                //Log.e(TAG, "END_OF_CONTEXT");
                //End of Jukebox queue, reset playlist to original without added songs
                //Log.e(TAG, "Current Queue list size  -- >   " + mCurrentlyPlayingSongList.size());
                if(mCurrentlyPlayingSongList.size() <= 1){
                    mFetchbeforeInitPlayer = false;
                    mQueueOver = true;
                    fetchJukeboxObject(true);
                    break;
                }
                else{
                    fetchJukeboxAndPopSong(true);
                }
        }
        mCurrentPlayerState = playerState;
    }

    private void fetchJukeboxObject(final boolean refreshUiList)
    {
        if(DEBUG)
            Log.d(TAG, "fetchJukeboxObject **");

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxID, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if(e == null){
                    Log.d(TAG, "SUCCESS , Object Fetched ");
                    mJukeboxObject = jukeboxObject;

                    mRefreshList = refreshUiList;
                    mObjectHandler.sendEmptyMessage(JUKEBOX_OBJECT_SET);

                }
                else
                    Log.e(TAG, "Error fetching jukebox object..");
            }
        });
    }

    private void fetchJukeboxAndPopSong(final boolean refreshUiList)
    {
        if(DEBUG)
            Log.d(TAG, "fetchJukeboxAndPopSong **");

        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxID, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if(e == null){
                    Log.d(TAG, "SUCCESS , Object Fetched, ready to pop ");
                    mJukeboxObject = jukeboxObject;
                    mRefreshList = refreshUiList;
                    mObjectHandler.sendEmptyMessage(JUKEBOX_POP_QUEUE_HEAD);

                }
                else
                    Log.e(TAG, "Error fetching jukebox object..");
            }
        });
    }

    @Override
    public void onLoggedIn() {
        if(DEBUG)
            Log.d(TAG, "onLoggedIn -- ");
    }

    @Override
    public void onLoggedOut() {
        if(DEBUG)
            Log.d(TAG, "onLoggedOut -- ");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        if(DEBUG)
            Log.d(TAG, "onLoginFailed -- ");
        throwable.printStackTrace();
    }

    @Override
    public void onTemporaryError() {
        if(DEBUG)
            Log.d(TAG, "onTemporaryError -- ");
    }

    @Override
    public void onNewCredentials(String s) {
        if(DEBUG)
            Log.d(TAG, "onNewCredentials -- ");
    }

    @Override
    public void onConnectionMessage(String message) {
        if(DEBUG)
            Log.d(TAG, "onConnectionMessage -- " + message);
    }

    /** ----------------------------------------------------------------------LOAD SONGLIST TASK ----------------------------------------------------------------------------*/
    static class LoadJukeboxSongList extends AsyncTask<List<String>, Void, List<Track>>
    {
        private final String TAG = LoadJukeboxSongList.class.getSimpleName();
        private JukeboxPlaylistActivity mActivity = null;
        private ProgressDialog mProgressDialog;
        private CharSequence Progress_title = "Loading Playlist ...";

        public LoadJukeboxSongList(Context context)
        {
            mActivity = (JukeboxPlaylistActivity)context;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setTitle(Progress_title);
            mProgressDialog.show();
        }

        @Override
        protected List<Track> doInBackground(List<String>... params) {
            List<String> songIds = params[0];
            List<Track> trackList = new ArrayList<Track>();

            if(songIds == null){
                Log.e(TAG, "SONGLIST IS NULL... ");
                return null;
            }

            for (String id : songIds) {
                if(!isCancelled()){
                    InputStream stream = retrieveStream(id);
                    Track track = parseJsonResponse(stream);
                    if(track != null)
                        trackList.add(track);
                }
            }

            mProgressDialog.dismiss();
            return trackList;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(List<Track> result) {
            //boolean value is to show the 'plus(add song)' icon in each row
            mActivity.updateTrackList(result, false);
        }

        //TODO: Review the need of mTrack
        private Track parseJsonResponse(InputStream jsonResult)
        {
            //Log.d(TAG, "parseJsonResponse ** ");
            String line;

            if(jsonResult == null){
                Log.e(TAG, "JSON RESULT IS NULL .. exiting." );
                return null;
            }

            //TODO: marker of possible error
            BufferedReader reader = new BufferedReader(new InputStreamReader(jsonResult));
            String trackName;
            try {
                line = reader.readLine();

                JSONObject jsonObj = new JSONObject(line);
                JSONObject track = jsonObj.getJSONObject("track");
                trackName = track.getString("name");

                JSONArray jArray = track.getJSONArray("artists");
                String artistName = jArray.getJSONObject(0).getString("name");

                mActivity.mTrack = new Track(artistName, trackName);
                mActivity.mTrack.setTrackName(trackName);
                mActivity.mTrack.setArtistName(artistName);

            } catch (IOException e) {
                e.printStackTrace();
            } catch(JSONException e){
                e.printStackTrace();
            }

            return mActivity.mTrack;
        }

        private InputStream retrieveStream(String url)
        {
            DefaultHttpClient client = new DefaultHttpClient();

            HttpGet getRequest = new HttpGet(url);
            try {
                HttpResponse getResponse = client.execute(getRequest);
                final int statusCode = getResponse.getStatusLine().getStatusCode();

                if(statusCode != HttpStatus.SC_OK){
                    Log.d(TAG, "STATUS NOT OK, returning null object");
                    return null;
                }

                HttpEntity getResponsEntity = getResponse.getEntity();
                return getResponsEntity.getContent();

            } catch (IOException e) {
                getRequest.abort();
                Log.e(TAG, "Error for URL --");
            }
            return null;
        }


    } // End of class LoadJukeboxSongList

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
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

            case R.id.addSongFromSpotify:
                Log.e(TAG, "Add songs... ");
                Intent intent = new Intent(this, MusicSearchActivity.class);
                intent.putExtra("JUKEBOXID", mJukeboxID);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Log.e(TAG, "CLICKED on playlist activity");
        super.onListItemClick(l, v, position, id);
    }
}