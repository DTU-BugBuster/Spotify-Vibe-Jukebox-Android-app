package com.vibejukebox.jukebox;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class JukeboxSavedPlaylists extends ListActivity
{
    private static final String TAG = JukeboxSavedPlaylists.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    public static final String TOKEN = "Spotify_access_token";
    private static final int GET_TRACK_PLAYLIST_DONE = 0;
    private static final int LAUNCH_JUKEBOX = 1;
    private static final String OWN_JUKEBOX = "on_own_jukebox";

    public static final String CREATED_OBJECT_ID = "createdObjectId";
    public static final String CURRENT_SPOTIFY_USER = "spotifyUserID";
    public static final String PLAYLIST_NAMES = "playlistNames";
    public static final String PLAYLIST_TRACK_NUMBERS = "trackNumbers";
    public static final String PLAYLIST_IDS = "ids";

    private String mCreatedObjectId;
    private String mCurrentUserId;
    private List<String> mPlaylistNames;
    private List<Integer> mTrackNumbers;
    private List<String> mPlaylistIds;

    private String mJukeboxName;
    private String mJukeboxId;
    private List<String> mJukeboxSongQueue;

    private JukeboxObject mJukebox;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){
                case GET_TRACK_PLAYLIST_DONE:
                    setPlaylistTracks((List<String>) msg.obj);
                    return true;
                case LAUNCH_JUKEBOX:
                    launchPlaylistActivity();
            }
            return false;
        }
    });

    private void setPlaylistTracks(List<String> songIds)
    {
        mJukeboxSongQueue = new ArrayList<String>(songIds);

        String id = mJukebox.getObjectID();
        String name = mJukebox.getName();

        Log.d(TAG, "setPlayListTracks ------ >  " + "  name: " + name + "  id: " + id);
        for(String song: songIds){
            Log.d(TAG, song);
        }

        ParseObject jukebox = ParseObject.createWithoutData("JukeBox", id);
        jukebox.put("queueSongIDs", songIds);
        jukebox.put("defaultQueueSongIDs", songIds);
        jukebox.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null){
                    Log.d(TAG, "Succeeded saving all songs in the jukebox object.");
                    mHandler.sendEmptyMessage(LAUNCH_JUKEBOX);
                }
                else {
                    Log.e(TAG, "ERROR: Songs not saved in Jukebox..");
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jukebox_saved_playlists);

        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        setTitle("Playlists");
        Intent intent = getIntent();

        //TODO: find a better way of passing data (Bundle?)
        mCreatedObjectId = intent.getStringExtra(CREATED_OBJECT_ID);
        mPlaylistNames = intent.getStringArrayListExtra(PLAYLIST_NAMES);
        mPlaylistIds = intent.getStringArrayListExtra(PLAYLIST_IDS);
        mTrackNumbers = intent.getIntegerArrayListExtra(PLAYLIST_TRACK_NUMBERS);
        mCurrentUserId = intent.getStringExtra(CURRENT_SPOTIFY_USER);

        Log.d(TAG, "OBJECT ID RECEIVED IN SAVED PLAYLISTS -- " + mCreatedObjectId + " user: " +  mCurrentUserId);

        displayPlaylists(mPlaylistNames);
    }

    private void launchPlaylistActivity()
    {
        if(DEBUG)
            Log.d(TAG, "launchPlaylistActivity **  " + " -- " + mJukebox.getObjectID());

        Intent songListIntent = new Intent(getApplicationContext(),JukeboxPlaylistActivity.class);
        songListIntent.putExtra("JUKEBOXID", mJukeboxId);
        songListIntent.putExtra("JUKEBOXNAME", mJukeboxName);
        songListIntent.putStringArrayListExtra("queueSongIDs", (ArrayList<String>)getSpotifySongUrls(mJukeboxSongQueue));
        songListIntent.putStringArrayListExtra("rawSongIDs", (ArrayList<String>)mJukeboxSongQueue);
        songListIntent.putExtra(OWN_JUKEBOX, true);
        startActivity(songListIntent);
    }

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(DEBUG)
            Log.d(TAG, "onItemClicked. ");

        String ID = mPlaylistIds.get(position);
        Log.e(TAG, "PLAYLIST ID CHOSEN  -- >   " + ID);

        ParseQuery<JukeboxObject> query = new ParseQuery<JukeboxObject>("JukeBox");
        query.getInBackground(mCreatedObjectId, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if(e == null){
                    Log.d(TAG, "Fetching stored object.");
                    if(jukeboxObject != null) {
                        Log.d(TAG, "OBJECT name:   " + jukeboxObject.getName());

                        mJukebox = jukeboxObject;
                        mJukeboxId = jukeboxObject.getObjectId();
                        mJukeboxName = jukeboxObject.getName();

                        // Gets and sets the songQueue with its respective song ids
                        getPlaylistTracks(mPlaylistIds.get(position));
                    }
                }
                else{
                    Log.d(TAG, "SOMETHING IS WRONG ");
                }
            }
        });
    }

    private void getPlaylistTracks(String playlistId)
    {
        if(DEBUG)
            Log.d(TAG, "getPlaylistTracks -- WEB API");

        final List<String> listOfIds = new ArrayList<String>();


        String url = "https://api.spotify.com/v1/users/" + mCurrentUserId + "/playlists/" + playlistId + "/tracks";
        String header = "Authorization";
        String value = "Bearer " + retrieveAccessToken();

        if(DEBUG) {
            Log.d(TAG, "VALUE --->  " + value);
            Log.d(TAG, "url:  " + url);
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(header, value);
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "Success getting playlists' tracks ");

                try{
                    JSONArray jArray = response.getJSONArray("items");
                    JSONObject jsonObject;

                    for(int i = 0; i < jArray.length(); i++){
                        jsonObject = jArray.getJSONObject(i).getJSONObject("track");
                        listOfIds.add(jsonObject.getString("uri"));
                    }

                    List<String> trackList = new ArrayList<String>(listOfIds);
                    mHandler.sendMessage(mHandler.obtainMessage(GET_TRACK_PLAYLIST_DONE, trackList));

                } catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e(TAG, "FAILED getting songs from playlist ..." + String.valueOf(statusCode));
                throwable.printStackTrace();
            }
        });
    }

    //TODO: Migrate to new Spotify Web API - method SOON DEPRECATED
    private List<String> getSpotifySongUrls(List<String> songIds)
    {
        if(DEBUG)
            Log.d(TAG, "getSpotifySongUrls()-- ");

        String webURL = "http://ws.spotify.com/lookup/1/.json?uri=";
        List<String> requestList = new ArrayList<String>();

        for (String spotifyID : songIds) {
            String request = webURL + spotifyID;
            requestList.add(request);
        }

        if(DEBUG)
            Log.d(TAG, "Number of songs in the jukebox queue ---- >  " + String.valueOf(requestList.size()));

        return requestList;
    }

    private String retrieveAccessToken()
    {
        SharedPreferences storage = getSharedPreferences(TOKEN, 0);
        String accessToken = storage.getString("access_token", null);

        if(accessToken != null)
            return accessToken;
        else
            return null;
    }

    private void displayPlaylists(List<String> list)
    {
        SavedPlayListAdapter sAdapter = new SavedPlayListAdapter(this, list, mTrackNumbers);

        //Get adapter view from layout
        ListView playlistView = getListView();
        playlistView.setAdapter(sAdapter);
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
}