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
import com.parse.ParseQuery;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class JukeboxPlaylistActivity extends ListActivity implements
        PlayerNotificationCallback, ConnectionStateCallback
{
    private final String TAG = JukeboxPlaylistActivity.class.getSimpleName();
    private final boolean DEBUG = false;
    private final String DEFAULT_QUEUE_SONG_IDS = "defaultQueueSongIDs";
    private final String QUEUE_SONG_IDS = "queueSongIDs";
    private final String LOOKUP_SPOTIFY_ID = "http://ws.spotify.com/lookup/1/.json?uri="; //TODO: migrate to new Web API
    private static final String OWN_JUKEBOX = "on_own_jukebox";
    private static final int JUKEBOX_OBJECT_SET = 1;
    private static final int JUKEBOX_OBJECT_NOT_SET = -1;
    private static final int JUKEBOX_SONG_ADDED = 2;
    private static final int JUKEBOX_TRACK_CHANGED = 3;
    private static final int JUKEBOX_FETCH_OBJECT = 4;
    private static final int JUKEBOX_RESET_OBJECT = 5;
    private static final int JUKEBOX_POP_QUEUE_HEAD = 6;
    private static final int RESET_SKIPPED = 10;

    private boolean mIsOwnJukebox = false;

    private ListView songListView;
    private Track mTrack;
    private List<Track> mTrackList;
    private JukeboxObject mJukeboxObject = null;
    private static String mJukeboxID;
    private static String mJukeboxName;
    private List<String> mRawSongIDs;
    private List<String> mCurrentlyPlayingSongList;
    private int mPosition = 0;

    private boolean mObjectSet = false;
    private boolean mTrackChangedInit = true;
    private boolean mPlayerInit = false;
    private boolean mIsSkipped = false;
    private boolean mRefreshList = false;
    private boolean mPopOnly = false;
    private boolean mFetchbeforeInitPlayer = false;

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

                    return true;

                case JUKEBOX_OBJECT_NOT_SET:
                    mObjectSet = false;

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

                case RESET_SKIPPED:
                    Log.e(TAG, "SKIPPED RESET");
                    mIsSkipped = false;

                case JUKEBOX_RESET_OBJECT:
                    fetchJukeboxObject(mRefreshList);

            }
            return false;
        }
    });

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

            for(String song: mCurrentlyPlayingSongList){
                Log.d(TAG, "Current song:  " + song);
            }

            // TODO: When on a created jukebox, adding songs not supported yet, so hide it
            ActionBar actionBar = getActionBar();
            actionBar.hide();

            fetchJukeboxObject(false);
        }

        // Updates UI buttons according if user created or joined a Jukebox
        updateJukeboxButtons(mIsOwnJukebox);

        // TODO: Get Jukebox object from other Activities
		/*JukeboxObject jObject = (JukeboxObject)getIntent().getParcelableExtra("jukeboxobjectparcelable");
		String objIDTEMP = jObject.getObjectID();
		int num = jObject.getQueueSongIds().size();*/

        if(DEBUG){
            Log.d(TAG, "---------------------------------------------------------------------------------------------------------");
            //if(objIDTEMP != null && objIDTEMP.equals(mJukeboxID))
            //Log.d(TAG, "SUCCESS... IDS are the same   " + String.valueOf(num));
            //else
            //Log.d(TAG, "SOMETHING's WRONG... " + mJukeboxID + " vs " + objIDTEMP + "  vs  " + String.valueOf(num));
            setTitle(mJukeboxName);

            Log.d(TAG, "JUKEBOX ID ------------>>>>>>>>>>>  "  + mJukeboxID + " and NAME: " + mJukeboxName);
        }

        displaySongQueue();

        // Show the Up button in the action bar.
        //setupActionBar();
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
        for(String song: songQueueIds){
            Log.d(TAG, "songssss:   " + song);
        }

        //mCurrentlyPlayingSongList = new ArrayList<String>(songQueueIds);
        if(mJukeboxObject != null) {
            //mJukeboxObject.put("defaultQueueSongIDs", mRawSongIDs);
            //mJukeboxObject.put("queueSongIDs", songQueueIds);
            //mJukeboxObject.saveInBackground();
        }

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

			/*if(mTrackList != null)
				mTrackList.clear();

			for(int i = 0;i < list.size(); i++)
			{
				Log.e(TAG, "MAIN FOR LOOP iteration :   " + String.valueOf(i));
				parseTrackIdList(list.get(i));
			}

			updateTrackList(mTrackList, false);*/

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
        if(songListView == null)
        {
            //Log.e(TAG, "LIST IS NULL!!");
            return;
        }

        mTrackList.clear();

        for (Track track : list)
        {
            mTrackList.add(track);

            /** DEBUG CODE */
            String name = track.getName();
            String artist = track.getArtistName();
            String songID = track.getHref();
			/*if(DEBUG)
				Log.d(TAG, "ARTIST: "  + artist + "     SONG:   " + name + "    songID:    " + songID);*/
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

        if(mJukeboxObject != null)
            mCurrentlyPlayingSongList = mJukeboxObject.getDefaultQueueSongIds();
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
        mTrackChangedInit = true;

        Log.d(TAG, "Pausing and playing with new *Queue* ");
        mPlayer.pause();
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
                    //mPlayer.play(mRawSongIDs.get(0));
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
            //initializePlayer();
        }
        else
            playPauseMusic();
    }

    public void skipToNext(View view)
    {
        if(DEBUG)
            Log.d(TAG, "skipToNext -- (View)");

        if(mPlayer != null)
            mPlayer.skipToNext();
    }

    private void popLastPlayedSong(boolean isObjectSet)
    {
        if(DEBUG)
            Log.d(TAG, "popLastPlayedSong");

        if(isObjectSet){
            List<String>currentList = new ArrayList<String>(mJukeboxObject.getQueueSongIds());
            Log.d(TAG, "ORIGINAL currentSize : " + currentList.size());

            mRefreshList = true;
            mObjectSet = false;

            Log.d(TAG, "boolean VAR if List is empty:  " + currentList.isEmpty() + "  number of tracks:  " + currentList.size());
            if(currentList.size() > 0){
                currentList.subList(0,1).clear();
                mJukeboxObject.setQueueSongIds(currentList);
                if(!currentList.isEmpty())
                    refreshListToPlay(currentList);
                else
                    resetJukebox();
            }
            else{
                resetJukebox();
            }

            //mObjectHandler.sendEmptyMessageDelayed(JUKEBOX_RESET_OBJECT, 500);
        }
    }

    /**
     * Function called when the last song of the jukebox queue has been played.
     * The list gets reset to the original default queue.
     */
    private void resetJukebox()
    {
        if(DEBUG)
            Log.d(TAG, " -- resetJukebox");
        //TODO: to fix

        Spotify.destroyPlayer(this);
        mPlayerInit = false;
        mCurrentlyPlayingSongList = mRawSongIDs;
        mFetchbeforeInitPlayer = false;

        ImageButton playButton = (ImageButton)findViewById(R.id.playButton);
        playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));

        Toast.makeText(this, getString(R.string.jukebox_add_songs_to_host), Toast.LENGTH_LONG).show();
        fetchJukeboxObject(true);
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

        String event = eventType.name().toLowerCase(Locale.ENGLISH).replaceAll("_", " ");
        Log.e(TAG, "Player Event - " + event);

        if(event.equals("became active")){
            mTrackChangedInit = true;
        }

        else if(event.equals("skip next")){
            mObjectSet = false;
            mPopOnly = true;
            mTrackChangedInit = true;
            fetchJukeboxObject2(true);
        }

        else if(event.equals("pause")){
            mTrackChangedInit = true;
        }

        else if(event.equals("play")){
            mTrackChangedInit = true;
            refreshListUI();
        }

        else if(event.equals("track changed") && !mTrackChangedInit /*&& !mIsSkipped*/){
            Log.d(TAG, "TRACK CHANGED *tracked* " + mTrackChangedInit);
            fetchJukeboxObject2(true);

            //mObjectSet = false;
            //mPopOnly = true;
            //fetchJukeboxObject2(true);
            //popLastPlayedSong(mObjectSet);
        }

        else if(event.equals("audio flush")){
            mTrackChangedInit = false;
        }
        else if(event.equals("end of context")){
            //End of Jukebox queue, reset playlist to original without added songs
            //mCurrentlyPlayingSongList = mJukeboxObject.getDefaultQueueSongIds();
            //fetchJukeboxObject(true);
            fetchJukeboxObject2(true);
        }

        mCurrentPlayerState = playerState;
    }

    private void fetchJukeboxObject(final boolean refreshUiList)
    {
        if(DEBUG)
            Log.d(TAG, "fetchJukeboxObject **");

        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxID, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if(e == null){
                    Log.d(TAG, "SUCCESS , Object Fetched ");
                    mJukeboxObject = jukeboxObject;

                    //if(!mPopOnly){
                    //Log.d(TAG, "POP ONLY IS FALSE .. so proceed as usual.");
                    List<String> currentJukeboxPlaylist = jukeboxObject.getQueueSongIds();
                    mCurrentlyPlayingSongList = new ArrayList<String>(currentJukeboxPlaylist);
                    //mTrackChangedInit = false;

                    mRefreshList = refreshUiList;
                    //refreshPlaylist(currentJukeboxPlaylist);

                    mObjectHandler.sendEmptyMessage(JUKEBOX_OBJECT_SET);
                    //}
                    /*else {
                        Log.d(TAG, " ********** POPPING ONLY ....");
                        mObjectHandler.sendEmptyMessage(JUKEBOX_POP_QUEUE_HEAD);
                    }*/

                    //mObjectHandler.sendMessage(mObjectHandler.obtainMessage(JUKEBOX_OBJECT_SET, currentJukeboxPlaylist));
                }
                else
                    Log.e(TAG, "Error fetching jukebox object..");
            }
        });
    }

    private void fetchJukeboxObject2(final boolean refreshUiList)
    {
        if(DEBUG)
            Log.d(TAG, "fetchJukeboxObject2 **");

        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mJukeboxID, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if(e == null){
                    Log.d(TAG, "SUCCESS , Object Fetched ");
                    mJukeboxObject = jukeboxObject;

                    Log.d(TAG, "POP ONLY IS FALSE .. so proceed as usual.");
                    List<String> currentJukeboxPlaylist = jukeboxObject.getQueueSongIds();
                    //mCurrentlyPlayingSongList = new ArrayList<String>(currentJukeboxPlaylist);

                    mRefreshList = refreshUiList;
                    Log.d(TAG, " ********** POPPING ONLY ....");
                    mObjectHandler.sendEmptyMessage(JUKEBOX_POP_QUEUE_HEAD);

                }
                else
                    Log.e(TAG, "Error fetching jukebox object..");
            }
        });
    }

    @Override
    public void onLoggedIn() {
        Log.d(TAG, "onLoggedIn -- ");
    }

    @Override
    public void onLoggedOut() {
        Log.d(TAG, "onLoggedOut -- ");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.d(TAG, "onLoginFailed -- ");
        throwable.printStackTrace();
    }

    @Override
    public void onTemporaryError() {
        Log.d(TAG, "onTemporaryError -- ");
    }

    @Override
    public void onNewCredentials(String s) {
        Log.d(TAG, "onNewCredentials -- ");
    }

    @Override
    public void onConnectionMessage(String message) {
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
            String line = "";

            if(jsonResult == null){
                Log.e(TAG, "JSON RESULT IS NULL .. exiting." );
                return null;
            }

            //TODO: marker of possible error
            BufferedReader reader = new BufferedReader(new InputStreamReader(jsonResult));
            String trackName = null;
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

                //Log.d(TAG, "SONG: " + trackName);
                //Log.d(TAG, "ARTIST:  " + artistName);
                //Log.d(TAG, "****************************");


            } catch (IOException e) {
                e.printStackTrace();
            } catch(JSONException e){
                e.printStackTrace();
            }

            return mActivity.mTrack;
        }

        private InputStream retrieveStream(String url)
        {
            //Log.d(TAG, "retrieveStream() ---* ");
            //Log.d(TAG, "URL:     "+url);
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
    private void setupActionBar() {
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
        //playMusic();
    }
}


/*private void changeTrack()
    {
        Log.e(TAG, "changeTrack -- ");

        //List<String> currentList = mJukeboxObject.getQueueSongIds();
        //currentList.subList(0,1).clear();
        //mJukeboxObject.setQueueSongIds(currentList);

        //mTrackChangedInit = false;

        updateJukeboxQueueInBackend();
        playUpdatedPlaylist();

        //refreshPlaylist(mCurrentlyPlayingSongList);

        //LAST
        //Log.d(TAG, "Size of current list -- > " + currentList.size() + "    last list size -- >  " + mCurrentlyPlayingSongList.size());
        //if(currentList.size() > mCurrentlyPlayingSongList.size()){
            //Log.d(TAG, "Currently playing list at the time --- >  " + mCurrentlyPlayingSongList.size());

            //mPosition++;
            //mTrackChangedInit = false;
            //refreshPlaylist(currentList);
        //}
    }*/

/*private void playUpdatedPlaylist()
    {
        if(DEBUG)
            Log.d(TAG, "playUpdatedPlaylist" + String.valueOf(mPosition));

        if(mCurrentPlayerState.playing) {
            mPlayer.pause();
        }

        Log.d(TAG, "Playing with position  " + mPosition);
        mPlayer.play(mCurrentlyPlayingSongList);
    }

    private void refreshPlaylist(List<String> newList)
    {
        Log.d(TAG, "Playlist Refreshed ----- ");
        //mCurrentlyPlayingSongList = new ArrayList<String>(newList);
        //playUpdatedPlaylist();
    }*/

//TODO: REMOVE, notify data set changed
/*public void insertAddedSongToQueue(int location, List<Track> track)
	{
		mTrackList.add(location, track.get(0));

		mSongListAdapter = new SongListAdapter(this, (ArrayList<Track>)mTrackList, false);
		mSongListAdapter.notifyDataSetChanged();
		songListView.setAdapter(mSongListAdapter);
	}*/

/*private void updateJukeboxQueueInBackend()
    {
        Log.e(TAG, "updateJukeboxQueueInBackend -- ");

        List<String> currentList = mJukeboxObject.getQueueSongIds();
        currentList.subList(0,1).clear();
        mJukeboxObject.setQueueSongIds(currentList);
        mCurrentlyPlayingSongList = currentList;
    }*/

/*private void popLastPlayedSong(boolean isObjectSet)
    {
        if(DEBUG)
            Log.d(TAG, "popLastPlayedSong");

        List<String> currentList;

        if(isObjectSet){
            currentList = new ArrayList<String>(mJukeboxObject.getQueueSongIds());
            Log.d(TAG, "ORIGINAL currentSize : " + currentList.size());

            currentList.subList(0,1).clear();
            mJukeboxObject.setQueueSongIds(currentList);

            mRefreshList = true;
            mObjectSet = false;

            //TODO: DEBUG CODE , to remove
            Log.d(TAG, "*************************************************************************************************************");

            if(currentList.size() >= mCurrentlyPlayingSongList.size())
                Log.e(TAG, "NEW UPDATED LIST IS BIGGER THAN CURRENT ONE  .. ");
            else
                Log.e(TAG, "NEW UPDATED LIST IS NOT BIGGER THAN CURRENT ONE  .. ");

            for(String fromNewCurrentlist: currentList){
                Log.d(TAG, "curr:  " + fromNewCurrentlist);
            }

            for(String fromLast: mCurrentlyPlayingSongList)
                Log.d(TAG, "last:  " + fromLast);
            Log.d(TAG, "*************************************************************************************************************");

            //Check to see if a song has been added to the jukebox by another user
            if(currentList.size() >= mCurrentlyPlayingSongList.size())
                refreshListToPlay(currentList);
            else
                refreshListUI();

            //mObjectHandler.sendEmptyMessageDelayed(JUKEBOX_RESET_OBJECT, 500);
        }
    }*/