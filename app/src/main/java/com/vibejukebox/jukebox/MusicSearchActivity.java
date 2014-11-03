package com.vibejukebox.jukebox;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;

import org.apache.http.Header;
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
import java.util.HashMap;
import java.util.List;

public class MusicSearchActivity extends ListActivity
{
	private final static String TAG = MusicSearchActivity.class.getSimpleName(); 
	private final static boolean DEBUG = false;
	
	private static final String WEB_SERVICE_SEARCH = "http://ws.spotify.com/search/1/track.json?q=";
	public static final int FLAG_ACTIVITY_REORDER_TO_FRONT =  131072;
	private String mUrl = null;
	private ListView songListView;
	private ProgressDialog mProgressDialog;
	
	private static String mJukeboxID;
	private int mAddedPosition = 0;
	JukeboxPlaylistActivity mPlayListActivity = null;
	
	//Ui elements
	private List<Track> mTrackList;
	private EditText mEditText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_search);
		mJukeboxID = getIntent().getStringExtra("JUKEBOXID");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.music_search, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
	}
	
	public void searchMusic(View view)
	{
		if(DEBUG)
			Log.d(TAG, "searchMusic()");
	
		mEditText = (EditText)findViewById(R.id.Search_Music);
		String userSearch = mEditText.getText().toString();
		userSearch = userSearch.replaceAll(" ", "%20");
		String url = WEB_SERVICE_SEARCH + userSearch;
		mUrl = url;
		
		try {
			displaySongQueue();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public String getUrl()
	{
		return mUrl;
	}
	
	private void displaySongQueue() throws JSONException
	{
		if(DEBUG)
			Log.d(TAG, "displaySongQueue -- ");
		
		final ArrayList<String> hRefs = new ArrayList<String>();
		final List<Track> trackList = new ArrayList<Track>();
		SpotifyClient.getSimpleReq(mUrl, null, new JsonHttpResponseHandler() {
			
			@Override
			public void onSuccess(int statusCode, Header[] headers,
					JSONObject response) 
			{
				if(DEBUG)
					Log.d(TAG, "onSuccess - JSONObject  ");
				
				JSONArray jArray;
				try {
					jArray = response.getJSONArray("tracks");
					
					Track track;
					JSONObject tmpObject;
					for(int i = 0; i < jArray.length(); i++)
					{
						tmpObject = jArray.getJSONObject(i);
						//get the song id
						String hRef = tmpObject.getString("href");
						hRefs.add(hRef);
						// Get the name of the song
						String songName = tmpObject.getString("name");
						
						// get the Artist name
						JSONArray jArtistArray = tmpObject.getJSONArray("artists");
						String artistName = jArtistArray.getJSONObject(0).getString("name");
					
						//TODO: Check constructors, unnecessary code seems to be present in argument list
						track = new Track(artistName, songName, hRef);
						track.setTrackName(songName);
						track.setArtistName(artistName);
						track.setHref(hRef);
						
						trackList.add(track);
					}
				
					//Sets boolean value of updateTrackList to true to show the plus signs in a music search
					if(trackList.size() != 0){
						if(DEBUG)
							Log.d(TAG, "TrackLIST has   -------- " + String.valueOf(trackList.size()));
						mTrackList = trackList;
						updateTrackList(trackList, true);
					}	
					
				} catch(JSONException e){
				e.printStackTrace();
				}
			};
		});
	}
	
	private void updateTrackList(List<Track> list, boolean showAddSong)
	{
		if(DEBUG)
			Log.d(TAG, "updateTRACKList --   " + String.valueOf(list.size()));
		
		songListView = getListView();

		if(songListView == null)
		{
			Log.e(TAG, "LIST IS NULL!!");
			return;
		}
		
		if(DEBUG)
			Log.d(TAG, "LIST SIZE  " + String.valueOf(list.size()));
		mTrackList = new ArrayList<Track>();
		
		for (Track track : list) 
		{
			mTrackList.add(track);
			String name = track.getName();
			String artist = track.getArtistName();
			String songID = track.getHref();
			if(DEBUG)
				Log.d(TAG, "ARTIST: "  + artist + "     SONG:   " + name + "    songID:    " + songID);
		}
		
		SongListAdapter songListAdapter = new SongListAdapter(this, (ArrayList<Track>)list, showAddSong);
		songListAdapter.notifyDataSetChanged();
		songListView.setAdapter(songListAdapter);
		
		//Hide software keyboard when search is done.
		InputMethodManager manager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		manager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	}
	
	// --- ADD SONG ---
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) 
	{	
		if(DEBUG)
			Log.d(TAG, "CLICKED on MUSIC SEARCH activity");
		super.onListItemClick(l, v, position, id);
		
		Track trackClicked = mTrackList.get(position);
		
		//If positive goes to add song to queue function
		addSongPopUp(trackClicked);
	}
	
	private void addSongPopUp(final Track trackClicked)
	{
		if(DEBUG)
			Log.d(TAG, "addSongPopUP --");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.ADD_SONG_TO_QUEUE)
		.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addSongToQueue(trackClicked);
			}
		})
		.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		
		builder.create();
		builder.show();
	}
	
	private void launchAddingTrackDialog()
	{
		CharSequence Progress_title = "Adding Track ...";
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setTitle(Progress_title);
		mProgressDialog.show();
	}
	
	private void addSongToQueue(Track trackClicked)
	{
		launchAddingTrackDialog();
		
		final String songId = trackClicked.getHref();
		final String jukeboxId = mJukeboxID;
		
		if(DEBUG){
			Log.d(TAG, "PARAMETERS ARE --------------");
			Log.d(TAG, "SONGID: " + songId + "  JUKEBOXID:  " + jukeboxId);
		}
		
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("songid", songId);
		params.put("jukeboxid", jukeboxId);
		
		ParseCloud.callFunctionInBackground("addSongToQueue", params, new FunctionCallback<Integer>() {
			@Override
			public void done(Integer songPosition, ParseException e) {
				if(e == null){
					if(DEBUG){
						Log.d(TAG, "Song added to jukebox");
						Log.d(TAG, "Song position --->>   " + songPosition);
					}
					
					runAddedSongParsingTask(songId, songPosition);
				}
				
				else{
					Log.e(TAG, "Something went wrong with background function adding song to jukebox.");
					e.printStackTrace();
					mProgressDialog.cancel();
				}
			}
		});
	}
	
	private void runAddedSongParsingTask(String songID, int position)
	{
		if(DEBUG)
			Log.d(TAG, "runAddedSongParsingTask ----");
		
		mAddedPosition = position;
		AddedSongInfoTask task = new AddedSongInfoTask(this);
		task.execute(songID);
	}
	
	public void postAddedResultToPlayList(List<Track> result)
	{
		if(DEBUG)
			Log.d(TAG, "postADDED-ResultTo PlayList ---- ");

		Intent intent = new Intent(this, JukeboxPlaylistActivity.class);
		intent.putExtra("position", mAddedPosition);
		intent.putParcelableArrayListExtra("trackadded", (ArrayList<Track>)result);
		intent.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
	
		mProgressDialog.dismiss();
		startActivity(intent);
		finish();
	}
	
	/**  -- Parse Added song AsyncTask   --- */
	static class AddedSongInfoTask extends AsyncTask<String, Void, List<Track> >
	{
		MusicSearchActivity mActivity = null;
		
		public AddedSongInfoTask(Context context)
		{
			mActivity = (MusicSearchActivity)context;
		}
		
		@Override
		protected void onPreExecute(){	
			super.onPreExecute();
		}
		
		@Override
		protected List<Track> doInBackground(String... params) {
			String songId = params[0];
			List<Track> trackList = new ArrayList<Track>();
			if(songId == null){
				Log.e(TAG, "SONGLIST IS NULL... ");
				return null;
			}
			
			if(!isCancelled()){
				InputStream stream = retrieveStream(songId);
				Track track = parseJsonResponse(stream);
				trackList.add(track);
			}
		
			//Log.d(TAG, "Returning Track list ========== ");
			return trackList;
		}
		
		@Override
		protected void onPostExecute(List<Track> result) 
		{
			mActivity.postAddedResultToPlayList(result);
		}
		
		//JSON parsing function
		private Track parseJsonResponse(InputStream jsonResult) 
		{
			Log.e(TAG, "parseJsonResponse ** ");
			String line = "";
			Track trackObj = null;
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(jsonResult));
			String trackName = null;
			try {
				line = reader.readLine();
				
				JSONObject jsonObj = new JSONObject(line);
				JSONObject track = jsonObj.getJSONObject("track");
				trackName = track.getString("name");
				
				JSONArray jArray = track.getJSONArray("artists");
				String artistName = jArray.getJSONObject(0).getString("name");
		
				trackObj = new Track(artistName, trackName);
				trackObj.setTrackName(trackName);
				trackObj.setArtistName(artistName);
			
			} catch (IOException e) {
				e.printStackTrace();
			} catch(JSONException e){
				e.printStackTrace();
			}
			
			return trackObj;
		}

		private InputStream retrieveStream(String url) 
		{
			Log.e(TAG, "retrieveStream() --- ");
			Log.e(TAG, "URL:     "+url);
			DefaultHttpClient client = new DefaultHttpClient();

			HttpGet getRequest = new HttpGet("http://ws.spotify.com/lookup/1/.json?uri="+url);
			try {
				HttpResponse getResponse = client.execute(getRequest);
				final int statusCode = getResponse.getStatusLine().getStatusCode();
				
				if(statusCode != HttpStatus.SC_OK){
					Log.e(TAG, "STATUS NOT OK, returning null object");
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
	
	}
}