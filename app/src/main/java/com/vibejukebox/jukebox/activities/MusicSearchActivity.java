package com.vibejukebox.jukebox.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

//import com.loopj.android.http.JsonHttpResponseHandler;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxApplication;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.adapters.SongListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MusicSearchActivity extends ListActivity
{
	private final static String TAG = MusicSearchActivity.class.getSimpleName(); 
	private final static boolean DEBUG = DebugLog.DEBUG;

	/** Vibe Jukebox variables */
	private static final String VIBE_JUKEBOX_ID = "JukeboxID";

	private static final String VIBE_TRACK_QUEUE_LIST = "TrackQueue";

    private static final int VIBE_TEST = 1;
	
	private static final String WEB_SERVICE_SEARCH = "http://ws.spotify.com/search/1/track.json?q=";
	private String mUrl = null;
	private ListView songListView;
	private ProgressDialog mProgressDialog;
	
	private static String mJukeboxID;
	private int mAddedPosition = 0;
	JukeboxPlaylistActivity mPlayListActivity = null;
	
	//Ui elements
	private List<Track> mQueryTrackList;
	private List<Track> mQueueTracks;


	private EditText mEditText;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_TEST:
                    displaySearchQuery((List<Track>)msg.obj);
                    break;
            }
            return false;
        }
    });

    public String getUrl()
    {
        return mUrl;
    }

    /**
     * Function to retrieve the previously stored access_token from Shared Preferences
     * @return: Access code string
     */
    public String retrieveSpotifyAccessToken()
    {
        if(DEBUG)
            Log.d(TAG, "Retrieving Spotify access token form storage ...");

        SharedPreferences storage = getSharedPreferences(JukeboxApplication.TOKEN, 0);
        String access_token = storage.getString(JukeboxApplication.ACCESS_TOKEN, null);

        return access_token;
    }

    /** --------------------------------------------------------------------------------------- */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_search);
		mJukeboxID = getIntent().getStringExtra(VIBE_JUKEBOX_ID);
		mQueueTracks = getIntent().getParcelableArrayListExtra(VIBE_TRACK_QUEUE_LIST);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.music_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Launch the user query to the Spotify Web Api
     * @param view
     */
	public void searchMusic(View view)
	{
		if(DEBUG)
			Log.d(TAG, "searchMusic()");
	
		mEditText = (EditText)findViewById(R.id.Search_Music);
		String userQuery = mEditText.getText().toString();

        final List<Track> trackList = new ArrayList<>();
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        Map<String, Object> options = new HashMap<>();
        options.put("limit", 50);

        spotify.searchTracks(userQuery, options, new Callback<TracksPager>() {
            @Override
            public void success(TracksPager tracksPager, Response response) {
                Log.d(TAG, "Successful search for tracks");
                for (kaaes.spotify.webapi.android.models.Track track : tracksPager.tracks.items) {
                    Track vibeTrack = new Track(track.artists.get(0).name, track.name);
                    vibeTrack.setTrackUri(track.uri);
                    trackList.add(vibeTrack);
                }

                //Display the results of the query
                //displaySearchQuery(trackList);
                mHandler.sendMessage(mHandler.obtainMessage(VIBE_TEST, trackList));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to perform search on last query: " + error.getMessage());
            }
        });
	}

    /**
     * Funcion displays the tracks from the result of the user query
     * @param trackList: Each track is a Track object (Vibe)
     */
    private void displaySearchQuery(List<Track> trackList)
    {
        Log.d(TAG, "displaySearchQuery -- ");
        songListView = getListView();

        //Display the tracks in the Song Adapter with the boolean value to true to show the plus sign to add songs
        SongListAdapter songListAdapter = new SongListAdapter(this, (ArrayList<Track>)trackList, true);
        songListAdapter.notifyDataSetChanged();
		songListAdapter.setSearch(true);
        songListView.setAdapter(songListAdapter);
        songListView.setVisibility(View.VISIBLE);

		mQueryTrackList = new ArrayList<>(trackList);

        //Hide software keyboard when search is done.
        InputMethodManager manager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

	// --- ADD SONG ---
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) 
	{	
		if(DEBUG)
			Log.d(TAG, "onListItemClick --");
		super.onListItemClick(l, v, position, id);
		
		Track trackClicked = mQueryTrackList.get(position);
		//If positive goes to add song to queue function
		addSongPopUp(trackClicked);
	}
	
	private void addSongPopUp(final Track trackClicked)
	{
		if(DEBUG)
			Log.d(TAG, "addSongPopUp --");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.ADD_SONG_TO_QUEUE)
		.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//launchAddingTrackDialog();
				addTrackToQueue(trackClicked);
			}
		})
		.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).create();

        //Show popUp
		builder.show();
	}
	
	/*private void launchAddingTrackDialog()
	{
		CharSequence Progress_title = "Adding Track ...";
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setTitle(Progress_title);
		mProgressDialog.show();
	}*/

    private void addTrackToQueue(final Track track)
    {
        Log.d(TAG, "addTrackToQueue");

        //Update Jukebox object in the backend
        String trackUri = track.getTrackUri();
        String[] items = trackUri.split(":");

        final String trackId = items[items.length -1];
        final  String jukeboxId = mJukeboxID;

		Map<String, String> params = new HashMap<>();
		params.put("songid", trackUri);
		params.put("jukeboxid", jukeboxId);

		ParseCloud.callFunctionInBackground("addSongToQueue", params, new FunctionCallback<Integer>() {
            @Override
            public void done(Integer trackPosition, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successful call to backend function, song added to queue." + trackPosition);
                    updateTrackList(track, trackPosition);

                } else {
                    Log.e(TAG, "Something went wrong with backend function: ");
                    e.printStackTrace();
                    mProgressDialog.cancel();
                }
            }
        });
    }

	/**
	 * This function updates the track queue and calls the function @displayUpdatedPlaylist to launch
	 * the activity to display.
	 */
	private void updateTrackList(Track track , Integer trackPosition)
	{
		//Update the queue with the newly added track in the correct position
		if(track != null)
			mQueueTracks.add(trackPosition, track);

		displayUpdatedPlaylist(trackPosition);

	}

	public void displayUpdatedPlaylist(Integer trackPosition)
	{
		if(DEBUG)
			Log.d(TAG, "displayUpdatedPlaylist -- ");

		Intent intent = new Intent(this, JukeboxPlaylistActivity.class);
        //intent.putExtra("position", trackPosition);
		intent.putParcelableArrayListExtra("tracks", (ArrayList<Track>) mQueueTracks);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		//mProgressDialog.dismiss();
		startActivity(intent);
		finish();
	}
}

