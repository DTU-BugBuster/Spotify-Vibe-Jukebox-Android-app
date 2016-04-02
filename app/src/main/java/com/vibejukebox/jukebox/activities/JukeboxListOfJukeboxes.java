package com.vibejukebox.jukebox.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.Vibe;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.util.ArrayList;
import java.util.List;

public class JukeboxListOfJukeboxes extends VibeBaseActivity {
	private static final String TAG = JukeboxListOfJukeboxes.class.getSimpleName();
	private static final boolean DEBUG = DebugLog.DEBUG;

    /** Vibe Jukebox variables */
    private static final int VIBE_JOIN_CURRENT_PLAYLIST = 1;

	/** Define request code to send to Google Play Service */
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	private List<JukeboxObject> mJukeboxes;

    private String mJukeboxId;

    private String mPlaylistName;

    /** List of URIs of the chosen playlist to join */
    private List<String> mTrackUriList;

	private void setJukeboxList(List<JukeboxObject> jukeboxes) {
        mJukeboxes = new ArrayList<>(jukeboxes);
	}

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_JOIN_CURRENT_PLAYLIST:
                    launchPlaylist((List<Track>)msg.obj);
                    break;
            }

            return false;
        }
    });

	/** -----------------------------------------------------------------------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG) {
            Log.d(TAG, "onCreate - JukeboxListOfJukeboxes");
        }
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jukebox_list_of_jukeboxes);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
	}

    @Override
    protected void onRestart() {
        super.onRestart();
        if(mLocationServicesConnected) {
            fetchNearbyJukeboxes();
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.jukebox_list_of_jukeboxes, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(DEBUG) {
            Log.d(TAG, "onActivityResult -- JukeboxListOFJukeboxes -- ");
        }
		
		//TODO: remove
		switch(requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			switch(resultCode){
			case Activity.RESULT_OK:
                if(DEBUG)
			    	Log.d(TAG, "Connected to Google Play Services.");
				break;
			default:
                if(DEBUG)
				    Log.d(TAG, "Could not connect to Google Play Services.");
				break;
			}
			default:
                if(DEBUG)
				    Log.d(TAG, "Unknown request code received for the activity.");
				break;
		}
	}

    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);

        //Calls the function in the base class
        fetchNearbyJukeboxes();
    }

    /**
     * Implementation of abstract class to handle the nearby jukeboxes found
     * @param jukeboxList
     */
    @Override
    protected void nearbyJukeboxesFound(List<JukeboxObject> jukeboxList) {
        setJukeboxList(jukeboxList);
        List<String> nearbyJukeboxes = new ArrayList<>();

        ViewGroup layout = (ViewGroup)findViewById(R.id.noJukeboxNearbyLayout);
        if(jukeboxList.size() > 0) {
            layout.setVisibility(View.GONE);
            for(JukeboxObject jukebox : jukeboxList) {
                nearbyJukeboxes.add(jukebox.getName());
            }

            //Display the list of jukeboxes nearby user
            updateJukeboxList(nearbyJukeboxes);
        } else {
            layout = (ViewGroup)findViewById(R.id.noJukeboxNearbyLayout);
            layout.setVisibility(View.VISIBLE);

            ListView nearbyJukeboxesView = (ListView)findViewById(R.id.nearJukeBokesView);
            nearbyJukeboxesView.setVisibility(View.GONE);
        }
    }

	private void showPoorConnectionToast() {
		Toast.makeText(this, getString(R.string.VIBE_APP_POOR_CONNECTION_MESSAGE), Toast.LENGTH_LONG).show();
	}

    private void updateJukeboxList(List<String> nearbyJukeboxNames) {
        // Set the nearby jukebox names
        ListView nearbyJukeboxesView = (ListView)findViewById(R.id.nearJukeBokesView);
        nearbyJukeboxesView.setVisibility(View.VISIBLE);
        nearbyJukeboxesView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nearbyJukeboxNames){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(getResources().getColor(R.color.vibe_white));
                textView.setPadding(10,30,10,30);
                textView.setTextSize(20);
                return textView;
            }
        });

        nearbyJukeboxesView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                ListView listView = (ListView) findViewById(R.id.nearJukeBokesView);
                String name = listView.getItemAtPosition(position).toString();

                if (DEBUG) {
                    Log.d(TAG, "Retrieved ------- > " + name);
                    Log.i(TAG, "Object ID ----- " + mJukeboxes.get(position).getObjectId());
                }

                JukeboxObject chosenJukebox = mJukeboxes.get(position);
                mTrackUriList = new ArrayList<>(chosenJukebox.getQueueSongIds());
                mJukeboxId = chosenJukebox.getObjectID();
                mPlaylistName = name;

                //TODO: fix null pointer crash when accessing an empty jukebox
                if (mTrackUriList == null) {
                    Log.e(TAG, "Song list is NULL");
                    return;
                }

                //Get the track list as a list of track objects
                createTrackListFromURIs(getTrackIds(mTrackUriList));
            }
        });
    }

    private void createTrackListFromURIs(String trackURIs) {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        final List<Track> trackList = new ArrayList<>();

        spotify.getTracks(trackURIs, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                Log.d(TAG, "Successful call to get Several tracks from Uri list");

                for (kaaes.spotify.webapi.android.models.Track track : tracks.tracks) {
                    if(DEBUG)
                        Log.d(TAG, "Track name:  " + track.name);

                    Track vibeTrack = new Track(track.artists.get(0).name, track.name);
                    vibeTrack.setTrackName(track.name);
                    vibeTrack.setArtistName(track.artists.get(0).name);
                    trackList.add(vibeTrack);
                }

                //Join the active playlist and display it
                mHandler.sendMessage(mHandler.obtainMessage(VIBE_JOIN_CURRENT_PLAYLIST, trackList));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occurred get the list of track Ids from Uri list:  " + error.getMessage());
            }
        });
    }

    private String getTrackIds(List<String> trackURIs) {
        String[] strippedItems;
        String trackIds = "";

        int count = 0;
        for(String uri : trackURIs) {
            strippedItems = uri.split(":");
            trackIds += strippedItems[strippedItems.length -1];

            if(count < trackURIs.size()-1) {
                trackIds += ",";
            }
            count++;
        }
        return trackIds;
    }

    /**
     * Launches the Activity that displays the playlist just joined by the user.
     * @param trackList: List of Track objects currently in the playlist.
     */
    private void launchPlaylist(List<Track> trackList) {
        Intent trackListIntent = new Intent(getApplicationContext(),
                JukeboxPlaylistActivity.class);

        trackListIntent.putExtra(Vibe.VIBE_JUKEBOX_ID, mJukeboxId);
        trackListIntent.putExtra(Vibe.VIBE_IS_ACTIVE_PLAYLIST, false);
        trackListIntent.putExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME, mPlaylistName);
        //trackListIntent.putStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE, (ArrayList<String>) mTrackUriList);
        trackListIntent.putExtra(Vibe.VIBE_JUKEBOX_QUEUE_HEAD_URI, mTrackUriList.get(0));
        trackListIntent.putParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE, (ArrayList<Track>)trackList);
        startActivity(trackListIntent);
    }

    public void createJukebox(View view) {
        loginToSpotify(this);
    }

    @Override
    protected void loginToSpotify(Activity contextActivity) {
        super.loginToSpotify(contextActivity);
    }

    @Override
    protected void checkSpotifyProduct(AuthenticationResponse authResponse) {

    }
}


/** Provides the entry point to Google Play Services */
//protected GoogleApiClient mGoogleApiClient;

/** Last known Location */
//private Location mLastLocation;

/** Location Request Object */
//private LocationRequest mLocationRequest;

/**
 * Main entry point for interacting with the fused location provider
 */
    /*protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }*/

/**
 * Settings control the accuracy of the current location
 */
    /*protected void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FAST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }*/



/**
 * Fetches nearby jukeboxes within a given range of the user's current location
 */
    /*private void fetchNearbyJukeboxes()
    {
        final List<String> nearbyJukeboxes = new ArrayList<>();

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseGeoPoint currentLocation = getGeoPointFromMyLocation(mLastLocation);

        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.whereWithinKilometers("location", currentLocation, 0.25);
        query.findInBackground(new FindCallback<JukeboxObject>() {
            @Override
            public void done(List<JukeboxObject> jukeBoxList, ParseException e) {
                if (e == null) {
                    if (DEBUG)
                        Log.d(TAG, "Number of nearby Jukeboxes is:   " + jukeBoxList.size());

                    setJukeboxList(jukeBoxList);

                    for (JukeboxObject jukebox : jukeBoxList) {
                        String jukeboxName = jukebox.getName();
                        nearbyJukeboxes.add(jukeboxName);
                    }

                    //Display the list of jukeboxes nearby user
                    updateJukeboxList(nearbyJukeboxes);
                } else {
                    Log.e(TAG, "Something went wrong getting nearby jukeboxes.");
                    showPoorConnectionToast();
                    e.printStackTrace();
                }
            }
        });
    }*/
