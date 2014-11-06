package com.vibejukebox.jukebox;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class JukeboxListOfJukeboxes extends FragmentActivity 
{
	private final static String TAG = JukeboxListOfJukeboxes.class.getSimpleName();
	private static final boolean DEBUG = DebugLog.DEBUG;
	
	//Define request code to send to Google Play Service
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
		  
	//update interval
	private static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	
	private static final long UPDATE_INTERVAL_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
		      * UPDATE_INTERVAL_IN_SECONDS;

	private String chosenJukeBoxID;
	private List<JukeboxObject> mJukeboxObjectList;
	private List<String> mNearByJukeboxes;

	private Location mLastLocation = null;
	private LocationClient mlocationClient;
	private LocationRequest mLocationRequest;

	// Getters and setters
	public String getChosenJukeboxID()
	{
		return chosenJukeBoxID;
	}
		
	private void setJukeboxList(List<JukeboxObject> objectList)
	{
		mJukeboxObjectList = new ArrayList<JukeboxObject>(objectList);
	}
	
	private ParseGeoPoint getGeoPointFromMyLocation(Location location)
	{
		if(servicesConnected() && location != null)
			return new ParseGeoPoint(location.getLatitude(), location.getLongitude());
		else
			return null;
	}

	/** ----------------------------------------------------------------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		if(DEBUG)
			Log.d(TAG, "onCreate - JukeboxListOfJukeboxes");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jukebox_list_of_jukeboxes);

		Intent intent = getIntent();
		mLastLocation = intent.getParcelableExtra("lastlocation");

        // Go display a list of jukeboxes according to location
		fetchNearbyJukeboxes();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if(DEBUG)
			Log.d(TAG, "onStart() -- ");
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if(DEBUG)
			Log.d(TAG, "onResume ---- ");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(DEBUG)
			Log.d(TAG, "onStop() -- ");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.jukebox_list_of_jukeboxes, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if(DEBUG)
			Log.d(TAG, "onActivityResult -- JukeboxListOFJukeboxes -- ");
		
		//TODO: remove
		switch(requestCode){
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			switch(resultCode){
			case Activity.RESULT_OK:
				Log.d(TAG, "Connected to Google Play Services."); 
				break;
			default:
				Log.d(TAG, "Could not connect to Google Play Services.");
				break;
			}
			default:
				Log.d(TAG, "Unknown request code received for the activity.");
				break;
		}
	}
	
	private void fetchNearbyJukeboxes()
	{
		final List<String> nearbyJukeboxes = new ArrayList<String>();

		ParseObject.registerSubclass(JukeboxObject.class);
		ParseGeoPoint currentLocation = getGeoPointFromMyLocation(mLastLocation);

		ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
		query.whereWithinKilometers("location", currentLocation, 0.25); 
		query.findInBackground(new FindCallback<JukeboxObject>() {	
			@Override
			public void done(List<JukeboxObject> jukeBoxList, ParseException e) {
				if(e == null){
                    if(DEBUG)
                        Log.d(TAG, "Number of nearby Jukeboxes is:   " + jukeBoxList.size());

					setJukeboxList(jukeBoxList);
					
					for(JukeboxObject jukebox : jukeBoxList) 
					{
						String jukeboxName = jukebox.getName();
						Log.d(TAG, "Jukebox name is :  " + jukeboxName);
						nearbyJukeboxes.add(jukeboxName);
					}
					
					mNearByJukeboxes = new ArrayList<String>(nearbyJukeboxes);
					updateJukeboxList(nearbyJukeboxes);
				}
				else{
					Log.e(TAG, "Something went wrong getting nearby jukeboxes.");
					 showPoorConnectionToast();
					e.printStackTrace();
				}
			}
		});
	}
	
	private void showPoorConnectionToast()
	{
		Toast.makeText(this, "Poor Connection please check your network connection and try again", Toast.LENGTH_LONG).show();
	}
	
	private void updateJukeboxList(List<String> list)
	{
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1 , list);
	
		// Get the nearby jukeboxes
		ListView nearbyJukeboxesView = (ListView)findViewById(R.id.nearJukeBokesView);
		nearbyJukeboxesView.setAdapter(adapter);
		
		nearbyJukeboxesView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListView listView = (ListView)findViewById(R.id.nearJukeBokesView);
				String name = listView.getItemAtPosition(position).toString();
				
				if(DEBUG){
					Log.d(TAG, "Retrieved ------- > " + name);
					Log.d(TAG, "Object ID ----- " + mJukeboxObjectList.get(position).getObjectId());
				}
	
				JukeboxObject chosenJukebox = mJukeboxObjectList.get(position);
				chosenJukeBoxID = chosenJukebox.getObjectId();

				List<String> songList = chosenJukebox.getQueueSongIds();

				//TODO: fix null pointer crash when accessing an empty jukebox
				if(songList == null){
					Log.d(TAG, "Song list is NULL");
					Log.e(TAG, "Number of songs in QUEUE   " + String.valueOf(songList.size()));
				}
				
				songList = getSpotifySongUrls(songList);

				/** Launch Intent with the selected Jukebox Song IDS *************/
				Intent songListIntent = new Intent(getApplicationContext(), 
													JukeboxPlaylistActivity.class);
				songListIntent.putExtra("JUKEBOXID", chosenJukeBoxID);
				songListIntent.putExtra("JUKEBOXNAME", name);
				songListIntent.putStringArrayListExtra("queueSongIDs", (ArrayList<String>)songList);
				startActivity(songListIntent);
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

		return requestList;
	}
	
	//Verify that the google services is available before making the request
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private boolean servicesConnected()
	{
		//Check that Google play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	
		if(ConnectionResult.SUCCESS == resultCode){
			Log.e(TAG, "Location Updates, Google Play Services is available");
			return true;
		}
		
		else{
			//get the error code
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
			if(dialog != null){
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(dialog);
				errorFragment.show(getFragmentManager(), "Location Updates");
			}
		}
		return false;
	}
	
	/**
	 * In response to a request to start updates send a request to 
	 * location services
	 */
   //TODO: request location udpates
	private void startPeriodicUpdates()
	{
		mlocationClient.requestLocationUpdates(mLocationRequest, (LocationListener) this);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showErrorDialog(int errorCode)
    {
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
		
		if(errorDialog != null){
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();
			errorFragment.setDialog(errorDialog);
			errorFragment.show(getFragmentManager(), TAG);
		}
	}
	
	//Dialog fragment that displays the error dialog
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class ErrorDialogFragment extends DialogFragment
	{
		private Dialog mDialog;
		
		//Default constructor. Sets the dialog field to null
		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		public ErrorDialogFragment(){
			super();
			mDialog = null;
		}
		
		//Set the dialog to display
		public void setDialog(Dialog dialog){
			mDialog = dialog;	
		}
		
		//return a dialog to the dialog fragment
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}
}
