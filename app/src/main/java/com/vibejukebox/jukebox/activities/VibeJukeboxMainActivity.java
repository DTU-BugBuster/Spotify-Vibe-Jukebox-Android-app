package com.vibejukebox.jukebox.activities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxApplication;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Vibe;

import java.util.ArrayList;
import java.util.List;

public class VibeJukeboxMainActivity extends VibeBaseActivity
{
    /** ----------------------    Fields -----------------------------------*/
	private static final String TAG = VibeJukeboxMainActivity.class.getSimpleName();
	private static final boolean DEBUG = DebugLog.DEBUG;

    /** Request code used to verify if result comes from the login Activity */
    private static final int SPOTIFY_API_REQUEST_CODE = 2015;

    //Broadcast receiver to get notifications from the system about the current network state
    private BroadcastReceiver mNetworkReceiver;

    /** ----------------------------    Parse Jukebox  -----------------------------------*/
	private List<JukeboxObject> mJukeboxObjectList;
	private int mNumberOfNearbyJukeboxesfound = 0;
	private ArrayList<String> mNearbyJukeboxes = null;

    private static String mDeviceName = null;

    public static String getDeviceName()
    {
        return mDeviceName;
    }

    private String getCreatedJukeboxId()
    {
        Log.d(TAG, "getCreatedJukeboxId -- ");
        SharedPreferences preferences = getSharedPreferences(Vibe.VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(Vibe.VIBE_JUKEBOX_STRING_PREFERENCE, null);

        Log.d(TAG, "----------------------------------------------------------- Returning ID: " + jukeboxId);
        return jukeboxId;
    }

    /**
     * TODO:  FOR TEST, DELETE THIS FUNCTION LATER..
     */
    private void storeNULLJukeboxID()
    {
        SharedPreferences preferences = getSharedPreferences("JukeboxPreferences", 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("JukeboxStoredId", null);
        editor.commit();
    }

    //--------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if(DEBUG)
            Log.d(TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jukebox_main);

 		//setJukeboxList(null);
        //buildGoogleApiClient();

        //TODO: Testing only
        //storeNULLJukeboxID();
	}

    private void updateUiStartAppButtons()
    {
        Button createJukeboxButton = (Button)findViewById(R.id.createJukeboxButton);
        if(getCreatedJukeboxId() != null){
            createJukeboxButton.setText(R.string.VIBE_APP_START_JUKEBOX);
        } else {
            createJukeboxButton.setText(R.string.VIBE_APP_CREATE_JUKEBOX);
        }
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

        // Update the main start button depending if a jukebox was already created or not.
        updateUiStartAppButtons();

        //Broadcast receiver for network events.
        /*mNetworkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Connectivity connectivity = getNetworkConnectivity(getBaseContext());
                //Log.d(TAG, " ------------------- NETWORK STATE RECEIVED ----------------   " + connectivity.toString());
            }
        };*/

        //IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        //registerReceiver(mNetworkReceiver, filter);

	}

    @Override
	protected void onStart()
    {
		super.onStart();
		if(DEBUG)
			Log.d(TAG, "onStart -- ");

		//mGoogleApiClient.connect();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if(DEBUG)
			Log.d(TAG, "onStop -- ");

		/*if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }*/

        //unregisterReceiver(mNetworkReceiver);
	}

    /**
     * Function called when a user requests to join an active playlist
     * @param view: the view that got clicked
     */
	public void joinJukebox(View view)
	{
		if(DEBUG)
			Log.d(TAG, "Joining Jukebox ...");

		Intent intent = new Intent(this, JukeboxListOfJukeboxes.class);
        startActivity(intent);
	}

    /**
     * Function called when a user wants to start a playlist of its own
     * @param view
     */
    public void startJukebox(View view)
    {
        if(DEBUG)
            Log.d(TAG, "createNewJukebox -- ");

        //User must login to Spotify account to be able to stream music
        loginToSpotifyAccount();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);

        fetchNearbyJukeboxes();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
        super.onActivityResult(requestCode, resultCode, data);
		if(DEBUG)
			Log.d(TAG, "onActivityResult -- VibeMainActivity -- ");

        //Check if the result comes from the correct activity
        if(requestCode == SPOTIFY_API_REQUEST_CODE){
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);

            switch(response.getType()){
                //Response was successful and contains auth token
                case TOKEN:
                    //Successful response, launch activity to choose which playlist will start
                    launchPlayListSelection(response);
                    break;

                case ERROR:
                    Log.e(TAG, "Error getting result back from Authentication process.");
                    break;

                //Most likely auth flow was cancelled
                default:
                    //Handle other cases
            }
        }
	}

    /**
     * Log users with the Spotify authentication flow
     */
    private void loginToSpotifyAccount()
    {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(JukeboxApplication.SPOTIFY_API_CLIENT_ID,
                        AuthenticationResponse.Type.TOKEN,
                        JukeboxApplication.SPOTIFY_API_REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-private",
                "playlist-read-private",
                "playlist-read-collaborative",
                "streaming",
                "user-library-read"});

        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, SPOTIFY_API_REQUEST_CODE, request);
    }

    /**
     * Launch Activity to choose starting playlist
     */
    private void launchPlayListSelection(AuthenticationResponse authResponse)
    {
        Intent intent = new Intent(this, PlaylistSelectionActivity.class);
        intent.putExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE, authResponse);
        intent.putExtra(Vibe.VIBE_CURRENT_LOCATION, mLastLocation);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ID, getCreatedJukeboxId());
        startActivity(intent);
    }

    @Override
    protected void nearbyJukeboxesFound(List<JukeboxObject> jukeboxList)
    {
        List<String> nearbyJukeboxes = new ArrayList<>();
        mNumberOfNearbyJukeboxesfound = jukeboxList.size();
        final TextView tv = (TextView)findViewById(R.id.nearbyJukeboxesTextView);

        if(mNumberOfNearbyJukeboxesfound == 0)
            tv.setText(R.string.no_nearby_jukeboxes_found);
        else
            tv.setText(String.valueOf(mNumberOfNearbyJukeboxesfound) + " " +
                    getString(R.string.nearby_jukeboxes_found));

        for(JukeboxObject jukebox : jukeboxList){
            nearbyJukeboxes.add(jukebox.getName());
        }

        mNearbyJukeboxes = new ArrayList<>(nearbyJukeboxes);
    }
}




/*protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }*/

//TODO: Implement
    /*protected void createLocationRequest()
    {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FAST_CEILING_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }*/

/**
 * Fetches nearby jukeboxes within a 25 meter radius using the Parse API
 */
	/*private void fetchNearbyJukeboxes()
	{
		if(DEBUG)
			Log.d(TAG, "fetchNearbyJukeboxes -- ");

		final List<String> nearbyJukeboxes = new ArrayList<>();
		ParseObject.registerSubclass(JukeboxObject.class);
		ParseGeoPoint currentLocation = getGeoPointFromMyLocation(mLastLocation);

		ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
		query.whereWithinKilometers("location", currentLocation, 0.25);

		final TextView tv = (TextView)findViewById(R.id.nearbyJukeboxesTextView);
		query.findInBackground(new FindCallback<JukeboxObject>() {
            @Override
            public void done(List<JukeboxObject> jukeboxList, ParseException e) {
                if (e == null) {
                    if(DEBUG)
                        Log.d(TAG, "Number of jukeboxes near: " + jukeboxList.size());

                    //setJukeboxList(jukeboxList);
                    mNumberOfNearbyJukeboxesfound = jukeboxList.size();
                    tv.setText(String.valueOf(mNumberOfNearbyJukeboxesfound) + " " + getString(R.string.nearby_jukeboxes_found));

                    for (JukeboxObject jukebox : jukeboxList) {
                        String jukeboxName = jukebox.getName();

                        if(DEBUG)
                            Log.d(TAG, "Jukebox name is :  " + jukeboxName);

                        nearbyJukeboxes.add(jukeboxName);
                    }

                    mNearbyJukeboxes = (ArrayList<String>) nearbyJukeboxes;
                } else {
                    Log.e(TAG, "Error retrieving nearby jukeboxes..");
                    tv.setText(R.string.no_nearby_jukeboxes_found);
                    e.printStackTrace();
                }
            }
        });
	}*/

    /*@Override
    public void onConnected(Bundle connectionHint)
    {
        if(DEBUG)
            Log.d(TAG, "onConnected - Connected to location Services");

        mLocationServicesConnected = true;
        if(mGoogleApiClient.isConnected())
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        //Store Current location globally
        Vibe.setCurrentLocation(mLastLocation);

        //Once Location services is properly connected we can get nearby Jukeboxes
        fetchNearbyJukeboxes();
    }*/

    /*@Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection Suspended. ");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Call connect to attempt to re-establish the connection to Google Play Services
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());

        if(mResolvingError){
            //Already attempting to resolve an error
            return;
        } else if(connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }

    }

	@Override
	public void onLocationChanged(Location loc)
	{
		if(DEBUG)
			Log.d(TAG, "onLocationChanged");
        //TODO: update location of jukebox
	}*/




    /* Creates a dialog for an error message
    private void showErrorDialog(int errorCode)
    {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);

        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    // Called from ErrorDialogFragment when the dialog is dismissed.
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    // A fragment to display an error dialog
    public static class ErrorDialogFragment extends DialogFragment
    {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((VibeJukeboxMainActivity)getActivity()).onDialogDismissed();
        }
    }*/
