package com.vibejukebox.jukebox;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.spotify.sdk.android.playback.Connectivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VibeJukeboxMainActivity extends Activity implements
	LocationListener,
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener
{

    /** ----------------------    Fiels -----------------------------------*/
	private static final String TAG = VibeJukeboxMainActivity.class.getSimpleName();
	private static final boolean DEBUG = DebugLog.DEBUG;
	private static final String NEW_JUKEBOX_NAME = "MyJukebox";

    private static final int OBJECT_CREATED = 0;
    private static final int LOAD_CREATED_JUKEBOX = 1;

    public static final String LAST_LOCATION = "lastlocation";
    public static final String NEARBY_JUKEBOX_NAMES = "nearbyjukeboxNames";
    public static final String JUKEBOX_ID = "CreatedObjectID";
    public static final String CREATED_JUKEBOX_ID = "MyCreatedJukeboxId";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String CREATED_OBJECT_ID = "createdObjectId";
    private static final String OWN_JUKEBOX = "on_own_jukebox";

	private String mUrl = null;

    /** ----------------------    Connection and Location Services-----------------------------------*/
	//Define request code to send to Google Play Service
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	
	//update interval
	private static final int UPDATE_INTERVAL_IN_SECONDS = 5;
		
	private static final long UPDATE_INTERVAL_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
			      * UPDATE_INTERVAL_IN_SECONDS;

    // A fast interval ceiling
    private static final int FAST_CEILING_IN_SECONDS = 1;

    private boolean mLocationServicesConnected = false;
    private Location mLastLocation = null;
    private LocationClient mlocationClient;
    private LocationRequest mLocationRequest;

    //Broadcast receiver to get notifications from the system about the current network state
    private BroadcastReceiver mNetworkReceiver;

    /** ----------------------------    Parse Jukebox  -----------------------------------*/
	private List<JukeboxObject> mJukeboxObjectList;
	private int mNumberOfNearbyJukeboxesfound = 0;
	private ArrayList<String> mNearbyJukeboxes = null;

    private static String mDeviceName = null;
    private static ParseGeoPoint mGeoPoint = null;

    public static final String TOKEN = "Spotify_access_token";

    private JukeboxObject mJukeboxObject;
    private String mCreatedObjectId;

    private Handler mHandler = new Handler(new Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case OBJECT_CREATED:
                    launchCreateJukeboxActivity();
                    return true;

                case LOAD_CREATED_JUKEBOX:
                    loadCreatedJukebox();
                    return true;
            }
            return false;
        }
    });

	boolean mBound = false;

	private void setJukeboxList(List<JukeboxObject> objectList)
	{
		if(DEBUG)
			Log.d(TAG, "setJukeboxList");
		
		if(objectList != null)
			mJukeboxObjectList = new ArrayList<JukeboxObject>(objectList);
		else
			mJukeboxObjectList = Collections.<JukeboxObject>emptyList();
	}
	
	private ParseGeoPoint getGeoPointFromMyLocation(Location location) 
	{
		if(servicesConnected() && location != null)
			return new ParseGeoPoint(location.getLatitude(), location.getLongitude());
		else
			return null;
	}

    public static String getDeviceName()
    {
        return mDeviceName;
    }

    public static ParseGeoPoint getLocation()
    {
        return mGeoPoint;
    }

    public String getURL() {
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

        SharedPreferences storage = getSharedPreferences(TOKEN, 0);
        String access_token = storage.getString(ACCESS_TOKEN, null);

        return access_token;
    }

    private void storeCreatedJukeboxId(String id)
    {
        SharedPreferences preferences = getSharedPreferences(JUKEBOX_ID, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(CREATED_JUKEBOX_ID, id);
        editor.apply();
    }

    private String retrieveCreatedJukeboxId()
    {
        SharedPreferences storage = getSharedPreferences(JUKEBOX_ID, 0);
        String objectId = storage.getString(CREATED_JUKEBOX_ID, null);

        return objectId;
    }

    //--------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if(DEBUG)
            Log.d(TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jukebox_main);

 		setJukeboxList(null);
        setUpLocationServices();
		mlocationClient = new LocationClient(this, this, this);
	}
	
	public void setUpLocationServices()
	{
		mNearbyJukeboxes = new ArrayList<String>();
		
		//Location services 
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setFastestInterval(FAST_CEILING_IN_SECONDS);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.vibe_jukebox_main, menu);
		return true;
	}
	
	@Override
	protected void onResume()
    {
		super.onResume();
		if(DEBUG)
			Log.d(TAG, "onResume -- ");

        //Broadcast receiver for network events.
        mNetworkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Connectivity connectivity = getNetworkConnectivity(getBaseContext());
                Log.d(TAG, " ------------------- NETWORK STATE RECEIVED ----------------   " + connectivity.toString());
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);

        Button createButton = (Button)findViewById(R.id.createJukeboxButton);
        if(retrieveCreatedJukeboxId() != null) {
            Log.e(TAG, "RETRIEVING ID --->>>>>  " + retrieveCreatedJukeboxId());
            createButton.setText("My Jukebox");
        }
        else
            createButton.setText("Create Jukebox");

        if(mLocationServicesConnected)
			fetchNearbyJukeboxes();
	}

    private Connectivity getNetworkConnectivity(Context context)
    {
        if(DEBUG)
            Log.d(TAG, "getNetworkConnectivity -- ");

        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if(activeNetwork != null){
            return Connectivity.fromNetworkType(activeNetwork.getType());
        }
        else{
            return Connectivity.OFFLINE;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(DEBUG)
            Log.d(TAG, "onRestart -- ");
    }

    @Override
	protected void onStart()
    {
		super.onStart();
		
		if(DEBUG)
			Log.d(TAG, "onStart -- ");
		
		mlocationClient.connect();
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();
		if(DEBUG)
			Log.d(TAG, "onStop -- ");
		
		mlocationClient.disconnect();
	}

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(DEBUG)
            Log.d(TAG, "onDestroy --  ");
    }

    // Function called when the "Join" button is pressed
	public void joinJukebox(View view)
	{
		if(DEBUG)
			Log.d(TAG, "Joining Jukebox ...");
		Intent intent = new Intent(this, JukeboxListOfJukeboxes.class);
		
		if(mJukeboxObjectList == null){
			Log.d(TAG, "object WAS null ....");
			mJukeboxObjectList = new ArrayList<JukeboxObject>();
		}

		intent.putStringArrayListExtra(NEARBY_JUKEBOX_NAMES, mNearbyJukeboxes);
		intent.putExtra(LAST_LOCATION, mLastLocation);
		startActivity(intent);
	}
	
	public void goToJukebox(View view)
	{
		if(DEBUG)
			Log.d(TAG, "goToJukebox -- ");

        if(retrieveCreatedJukeboxId() != null){
            mCreatedObjectId = retrieveCreatedJukeboxId();
            fetchJukeboxObject();
        }
        else
            createNewJukebox();
	}

    private void createNewJukebox()
    {
        if(DEBUG)
            Log.d(TAG, "createNewJukebox -- ");

        AccountManager manager = (AccountManager)getSystemService(ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();
        String username;

        for (Account account : list)
        {
            Log.d(TAG, "ACCOUNT::    " + account.name);
            if(account.type.equalsIgnoreCase("com.google"))
            {
                username = account.name;
                String[] emailName = username.split("@");
                mDeviceName = emailName[0];
                break;
            }
        }

        //Default name if no google account is set on the device
        if(mDeviceName == null)
            mDeviceName = NEW_JUKEBOX_NAME;

        //Create Parse object
        ParseObject.registerSubclass(JukeboxObject.class);
        final JukeboxObject jukebox = new JukeboxObject();
        jukebox.put("name", mDeviceName);
        jukebox.put("location", getGeoPointFromMyLocation(mLastLocation));
        jukebox.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null){
                    Log.d(TAG, "Success saving object in background ..");
                    String id = jukebox.getObjectId();
                    mCreatedObjectId = id;
                    mHandler.sendEmptyMessage(OBJECT_CREATED);
                }
                else{
                    Log.e(TAG, "Something went wrong saving new jukebox object ...");
                }
            }
        });
    }

    private void loadCreatedJukebox()
    {
        if(DEBUG)
            Log.d(TAG, "loadCreatedJukebox - ");

        String access_token = retrieveSpotifyAccessToken();
        List<String> queueSongs = new ArrayList<String>(mJukeboxObject.getQueueSongIds());

        Intent loadJukeboxIntent = new Intent(this, JukeboxPlaylistActivity.class);
        loadJukeboxIntent.putExtra(ACCESS_TOKEN, access_token);
        loadJukeboxIntent.putExtra("JUKEBOXID", mCreatedObjectId);
        loadJukeboxIntent.putExtra("rawSongIDs", (ArrayList<String>)queueSongs);
        loadJukeboxIntent.putExtra("queueSongIDs", (ArrayList<String>)getSpotifySongUrls(queueSongs));
        loadJukeboxIntent.putExtra(OWN_JUKEBOX, true);
        startActivity(loadJukeboxIntent);
    }

    private void fetchJukeboxObject()
    {
        if(DEBUG)
            Log.d(TAG, "fetchJukeboxObject Main  -- ID:  " + mCreatedObjectId);

        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(mCreatedObjectId, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if(e == null){
                    Log.d(TAG, "SUCCESS , Object Fetched ");
                    mJukeboxObject = jukeboxObject;
                    mHandler.sendEmptyMessage(LOAD_CREATED_JUKEBOX);
                }
                else
                    Log.e(TAG, "Error fetching jukebox object..");
            }
        });
    }

    //TODO: DEPRECATED - Migrate to new Spotify Web API
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
            Log.d(TAG, "Number of songs in the jukbox queue ---- >  " + String.valueOf(requestList.size()));

        return requestList;
    }

    private void launchCreateJukeboxActivity()
    {
        storeCreatedJukeboxId(mCreatedObjectId);
        Intent intent = new Intent();
        String accessToken  = retrieveSpotifyAccessToken();

        if(accessToken != null){
            Log.d(TAG, "TOkEN exists:   " + accessToken);
            intent.setClass(this, StartingPlaylistActivity.class);
        }
        else{
            Log.d(TAG, "TOKEN doesn't exist ...   ");
            // When creating jukebox for the first time, Spotify login is required
            intent.setClass(this, LogInPremiumSpotify.class);
        }

        intent.putExtra(ACCESS_TOKEN, accessToken);
        intent.putExtra(CREATED_OBJECT_ID, mCreatedObjectId);
        startActivity(intent);
    }

	//CONNECTION to Location Services, get Last Location
	//Called when connection to location services finishes successfully
	@Override
	public void onConnected(Bundle connectionHint) 
	{
		if(DEBUG)
			Log.d(TAG, "onConnected - Connected to location Services");
		
		mLocationServicesConnected = true;
		
		//ParseGeoPoint currentLocation = (ParseGeoPoint) user.get("location");
		if(servicesConnected())
			mLastLocation = mlocationClient.getLastLocation();

		if(mLastLocation == null){
			Log.e(TAG, "ERROR CONNECTING ...");
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Failed to connect to Location Services. Try again ?")
			.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(DEBUG)
						Log.d(TAG, "setting up location services ..");
					setUpLocationServices();
				}
			})
			.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "NO");
				}
			});
			
			builder.create();
			builder.show();
			return;
		}

        mGeoPoint = new ParseGeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude());
		
		//Once Location services is properly connected we can get nearby Jukeboxes
		fetchNearbyJukeboxes();	
	}
	
	// Called by location services if the connection to location client drops because of an error
	@Override
	public void onDisconnected() 
	{
		if(DEBUG)
			Log.d(TAG, "onDisconnected -- ");
		
		mLocationServicesConnected = false;
		Toast.makeText(this, "Disconnected, Please reconnect to location services.", Toast.LENGTH_SHORT).show();
	}
		
	//Called by location services if the attempt to connect to location services fails 
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if(DEBUG)
			Log.d(TAG, "onConnectionFailed() -- ");
		if(connectionResult.hasResolution()){
			try {
                Log.d("TAG", "trying to find a resolution ...");
				connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch (IntentSender.SendIntentException e) {
				e.printStackTrace();
			}
		}
		
		else {
            Log.d("TAG", "Didn't find a resolution, launching error Dialog ...");
			showErrorDialog(connectionResult.getErrorCode());
		}
		
		int errorCode = connectionResult.getErrorCode();
		Log.e(TAG, "Error code is:    " + String.valueOf(errorCode));
		showErrorDialog(errorCode);	
	}
		
	@Override
	public void onLocationChanged(Location loc) 
	{
		if(DEBUG)
			Log.d(TAG, "onLocationChanged");
		return;
	}
	
	//Defines callback for service binding passed to BindService()
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			//Log.e(Tag, "On service disconnected");
			mBound = false;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// We've bound to PlaybackService, cast the IBinder and get LocalService instance
			//LocalBinder binder = (LocalBinder) service;
			//mService = binder.getService();
			mBound = true;
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if(DEBUG)
			Log.d(TAG, "onActivityResult -- VibeMainActivity -- ");
		
		switch(requestCode){
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			switch(resultCode){
			case Activity.RESULT_OK:
				Log.d(TAG, "Connected to Google Play Services."); 
				break;
				
			default:
				Log.e(TAG, "Could not connect to Google Play Services.");
				break;
			}
			
			default:
				Log.d(TAG, "Unknown request code received for the activity.");
				break;
		}
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

	//Verify that google services is available before making the request 
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private boolean servicesConnected()
	{
		if(DEBUG)
			Log.d(TAG, "servicesConnected -- ");
		
		//Check that Google play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	
		if(ConnectionResult.SUCCESS == resultCode){
			Log.d(TAG, "Location Updates, Google Play Services is available");
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
			
			return false;
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class ErrorDialogFragment extends DialogFragment
	{
		private Dialog mDialog;
		
		//Default constructor. Sets the dialog field to null
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
	
	/** Get Nearby jukeboxes  -- Parse -----------------------*/
	private void fetchNearbyJukeboxes() 
	{
		if(DEBUG)
			Log.d(TAG, "fetchNearbyJukeboxes -- ");
		
		final List<String> nearbyJukeboxes = new ArrayList<String>();

		ParseObject.registerSubclass(JukeboxObject.class);
		ParseGeoPoint currentLocation = getGeoPointFromMyLocation(mLastLocation);
		
		ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
		query.whereWithinKilometers("location", currentLocation, 0.25); 
		
		final TextView tv = (TextView)findViewById(R.id.nearbyJukeboxesTextView);
		query.findInBackground(new FindCallback<JukeboxObject>() {	
			@Override
			public void done(List<JukeboxObject> jukeboxList, ParseException e) {
				if(e == null){
					Log.d(TAG, "Object is in Jukebox");
					Log.d(TAG, "Number of Jukeboxes near is :   " + jukeboxList.size());
					
					setJukeboxList(jukeboxList);
					mNumberOfNearbyJukeboxesfound = jukeboxList.size();
					
					tv.setText(String.valueOf(mNumberOfNearbyJukeboxesfound)  + " " +getString(R.string.nearby_jukeboxes_found));

					for(JukeboxObject jukebox : jukeboxList)
					{
						String jukeboxName = jukebox.getName();
						Log.d(TAG, "Jukebox name is :  " + jukeboxName);
						nearbyJukeboxes.add(jukeboxName);
					}
					
					mNearbyJukeboxes = (ArrayList<String>) nearbyJukeboxes;
				}
				else{
					Log.e(TAG, "Something went wrong");
					tv.setText(R.string.no_nearby_jukeboxes_found);
					e.printStackTrace();
				}
			}
		}); 
	}
}