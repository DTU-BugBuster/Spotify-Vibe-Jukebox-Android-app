package com.vibejukebox.jukebox.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.Vibe;
import com.vibejukebox.jukebox.adapters.SongListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MusicSearchActivity extends ListActivity {

	private final static String TAG = MusicSearchActivity.class.getSimpleName(); 
	private final static boolean DEBUG = DebugLog.DEBUG;

	/** Vibe Jukebox variables */
	private static final String VIBE_JUKEBOX_ID = "JukeboxID";

    private static final String VIBE_PARSE_SONGID = "songid";

    private static final String VIBE_PARSE_JUKEBOXID = "jukeboxid";

    private static final int VIBE_DISPLAY_QUERY_SEARCH_RESULTS = 1;

	private static String mJukeboxId;
	
	//Ui elements
	private List<Track> mQueryTrackList;

	@Bind(R.id.Search_Music) EditText mEditText;

    private ProgressDialog mProgressDialog;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_DISPLAY_QUERY_SEARCH_RESULTS:
                    displaySearchQuery((List<Track>)msg.obj);
                    break;
            }
            return false;
        }
    });

    /** --------------------------------------------------------------------------------------- */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if(DEBUG) {
            Log.d(TAG, " - onCreate - ");
        }

		setContentView(R.layout.activity_music_search);
		mJukeboxId = getIntent().getStringExtra(VIBE_JUKEBOX_ID);
        ButterKnife.bind(this);
	}

    @Override
    protected void onResume() {
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode){
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            launchSearch();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        super.onResume();
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
	public void searchMusic(View view) {
        launchSearch();
	}

    private void launchSearch() {
        if(DEBUG) {
            Log.d(TAG, "launchSearch Spotify Api");
        }

        String userQuery = "";
        userQuery = mEditText.getText().toString();

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
                mHandler.sendMessage(mHandler.obtainMessage(VIBE_DISPLAY_QUERY_SEARCH_RESULTS, trackList));
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
    private void displaySearchQuery(List<Track> trackList) {
        if(DEBUG){
            Log.d(TAG, "displaySearchQuery -- ");
        }

        ListView songListView = getListView();

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
	protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(DEBUG) {
            Log.d(TAG, "onListItemClick --");
        }

		Track trackClicked = mQueryTrackList.get(position);

		//Launches verification dialog to confirm adding the song
		addSongPopUp(trackClicked);
	}
	
	private void addSongPopUp(final Track trackClicked) {
		if(DEBUG) {
            Log.d(TAG, "addSongPopUp --");
        }
		
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

    private void addTrackToQueue(final Track track) {
        if(DEBUG) {
            Log.d(TAG, "addTrackToQueue");
        }

        String trackUri = track.getTrackUri();
        final  String jukeboxId = mJukeboxId;

		Map<String, String> params = new HashMap<>();
		params.put(VIBE_PARSE_SONGID, trackUri);
		params.put(VIBE_PARSE_JUKEBOXID, jukeboxId);

        //Update Jukebox object in the backend
		ParseCloud.callFunctionInBackground("addSongToQueue", params, new FunctionCallback<Integer>() {
            @Override
            public void done(Integer trackPosition, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successful call to backend function, song added to queue: " + trackPosition);
                    displayUpdatedPlaylist();

                } else {
                    Log.e(TAG, "Something went wrong adding song in the backend function.. ");
                    e.printStackTrace();
                    //mProgressDialog.cancel();
                }
            }
        });
    }

	public void displayUpdatedPlaylist() {
		if(DEBUG) {
            Log.d(TAG, "displayUpdatedPlaylist -- ");
        }

		Intent intent = new Intent(this, JukeboxPlaylistActivity.class);
		intent.putExtra(Vibe.VIBE_JUKEBOX_CALL_REFRESH, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

		//mProgressDialog.dismiss();
		startActivity(intent);
		finish();
	}
}