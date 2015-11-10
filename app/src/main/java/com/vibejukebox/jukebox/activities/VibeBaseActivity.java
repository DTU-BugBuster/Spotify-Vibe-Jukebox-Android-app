package com.vibejukebox.jukebox.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
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

import java.util.List;


/**
 * Created by Sergex on 7/26/15.
 */
public abstract class VibeBaseActivity extends AppCompatActivity implements
        LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback
{
    private static final String TAG = VibeBaseActivity.class.getSimpleName();

    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    public static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    public static final String VIBE_JUKEBOX_ACCESS_TOKEN_PREF = "AccessToken";

    private static final int REQUEST_LOCATION_PERMISSION_CODE = 75;

    private boolean mResolvingError = false;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    /** Request code used to verify if result comes from the login Activity */
    private static final int SPOTIFY_API_REQUEST_CODE = 2015;

    /** ----------------------    Connection and Location Services-----------------------------------*/
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;

    //update interval
    private static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
            * UPDATE_INTERVAL_IN_SECONDS;

    // A fast interval ceiling
    private static final int FAST_CEILING_IN_MILLISECONDS = 3;

    public static boolean mLocationServicesConnected = false;

    /** Location object indication the last known location */
    protected Location mLastLocation;

    private LocationRequest mLocationRequest;

    protected boolean mRequestLocationUpdates = false;

    protected boolean mPermissionGranted = false;

    private View mMainLayout;

    /** Entry point to Google Play services */
    protected static GoogleApiClient mGoogleApiClient;

    private ParseGeoPoint getGeoPointFromMyLocation(Location location)
    {
        if(mGoogleApiClient.isConnected() && location != null)
            return new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        else
            return null;
    }

    protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        Log.d(TAG, "Connecting Google Api client");
        mGoogleApiClient.connect();
    }

    protected void createLocationRequest()
    {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FAST_CEILING_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationRequest = locationRequest;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)
            Log.d(TAG, "** onCreate -- ");

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        //Connect to location services
        //buildGoogleApiClient();

        //TODO:(Not implemented) boolean is always false, check if it will be needed
        if(mRequestLocationUpdates)
            createLocationRequest();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(DEBUG)
            Log.d(TAG, "-- onStart -- ");

        //View for SnackBar
        mMainLayout = findViewById(R.id.MainLayout);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            //Location permission not granted yet
            requestLocationPermission();
        }
        else{
            Log.d(TAG, " ---- Permission already granted.. ");
            buildGoogleApiClient();
        }

        //mGoogleApiClient.connect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.e(TAG, "PERMISSIONS RESULT " + requestCode);
        switch(requestCode){
            case REQUEST_LOCATION_PERMISSION_CODE:
                //If request is cancelled the array is empty
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mPermissionGranted = true;
                    buildGoogleApiClient();
                } else {
                    //Disable location functionality
                    Log.i(TAG, "Location permission was not granted.");
                    mPermissionGranted = false;
                    Snackbar.make(mMainLayout, R.string.VIBE_PERMISSION_NOT_GRANTED, Snackbar.LENGTH_SHORT).show();
                }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void requestLocationPermission()
    {
        Log.e(TAG, "Requesting Location permission.");
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)){
            Log.i(TAG, "Displaying an additional explanation of the need of location permission");
            if(mMainLayout == null){
                Log.e(TAG, "VIEW LAYOUT NULL!");
                return;
            }

            Snackbar.make(mMainLayout, R.string.VIBE_PERMISSION_NOT_GRANTED,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.VIBE_PERMISSION_GRANT, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(VibeBaseActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION_CODE);
                        }
                    }).show();
        } else {
            //Location permission was not granted yet, request it directly
            ActivityCompat.requestPermissions(VibeBaseActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUG)
            Log.d(TAG, " ** onResume --");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(DEBUG)
            Log.d(TAG, "onPause -- ");

        if(mRequestLocationUpdates)
            stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        if(DEBUG)
            Log.d(TAG, "onStop -- ");

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(DEBUG)
            Log.d(TAG, "onDestroy -- ");
    }

    protected void startLocationUpdates()
    {
        Log.d(TAG, "starting Location updates (Application)");
        LocationServices.FusedLocationApi.
                requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes Location updates from the FusedLocation API
     */
    protected void stopLocationUpdates()
    {
        //Final argument is a LocationListener
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /** Location Listener */
    @Override
    public void onLocationChanged(Location location)
    {
        Log.i(TAG, "Location Changed -- (BaseActivity)");
        Log.d(TAG, "New Location is (Base): " + String.valueOf(location.getLatitude()) + "  "
                + String.valueOf(location.getLongitude()));
    }

    /** Connection Callbacks */
    @Override
    public void onConnected(Bundle bundle)
    {
        if(DEBUG)
            Log.d(TAG, "onConnected - Connected to location Services (BaseActivity)");

        mLocationServicesConnected = true;
        if(mGoogleApiClient.isConnected())
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        //Request location updates
        if(mRequestLocationUpdates)
            startLocationUpdates();

        //Store Current location globally
        Vibe.setCurrentLocation(mLastLocation);

        //Once Location services is properly connected we can get nearby Jukeboxes
        //fetchNearbyJukeboxes();
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i(TAG, "Connection Suspended. ");
        mLocationServicesConnected = false;
        mGoogleApiClient.connect();
    }

    /** Connection Failed Listener */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        //Call connect to attempt to re-establish the connection to Google Play Services
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        mLocationServicesConnected = false;

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    storeAccessToken(response.getAccessToken());

                    //Checks if the current logged in user has a premium account
                    checkSpotifyProduct(response);
                    break;

                case ERROR:
                    Log.e(TAG, "Error getting result back from Authentication process.");
                    break;

                //Most likely auth flow was cancelled
                default:
                    break;
            }
        }

        else if(requestCode == REQUEST_RESOLVE_ERROR){
            mResolvingError = false;
            if(resultCode == RESULT_OK)
            {
                //Make sure the app is not already connected or attempting to connect
                if(!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()){
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    protected void storeAccessToken(String accessToken)
    {
        if(DEBUG)
            Log.d(TAG, "storeAuthResponse - ");

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(VIBE_JUKEBOX_ACCESS_TOKEN_PREF, accessToken);
        editor.apply();
    }

    protected void loginToSpotify(Activity contextActivity)
    {
        AuthenticationRequest.Builder builder  =
                new AuthenticationRequest.Builder(JukeboxApplication.SPOTIFY_API_CLIENT_ID,
                        AuthenticationResponse.Type.TOKEN,
                        JukeboxApplication.SPOTIFY_API_REDIRECT_URI);

        builder.setShowDialog(false)
                .setScopes(new String[]{"user-read-private",
                        "playlist-read-private",
                        "playlist-read-collaborative",
                        "streaming",
                        "user-library-read"});

        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(contextActivity, SPOTIFY_API_REQUEST_CODE, request);
    }

    private AuthenticationResponse getTestResponse(AuthenticationResponse response)
    {
        AuthenticationResponse.Builder builder = new AuthenticationResponse.Builder();
        builder.setExpiresIn(10);
        builder.setAccessToken(response.getAccessToken());
        builder.setCode(response.getCode());
        builder.setType(response.getType());
        builder.setCode(response.getCode());
        builder.setState(response.getState());
        AuthenticationResponse res = builder.build();
        return res;
    }

    protected abstract void checkSpotifyProduct(final AuthenticationResponse authResponse);

    protected abstract void nearbyJukeboxesFound(List<JukeboxObject> jukeboxList);

    protected void fetchNearbyJukeboxes()
    {
        if(DEBUG)
            Log.d(TAG, "fetchNearbyJukeboxes -- ");

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseGeoPoint currentLocation = getGeoPointFromMyLocation(mLastLocation);

        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.whereWithinKilometers("location", currentLocation, 0.25);
        query.findInBackground(new FindCallback<JukeboxObject>() {
            @Override
            public void done(List<JukeboxObject> jukeboxList, ParseException e) {
                if (e == null) {
                    if (DEBUG)
                        Log.d(TAG, "Number of jukeboxes near: " + jukeboxList.size());

                    nearbyJukeboxesFound(jukeboxList);
                } else {
                    Log.e(TAG, "Error retrieving nearby jukeboxes..");
                    e.printStackTrace();
                }
            }
        });
    }

    /* Creates a dialog for an error message */
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

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
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
            ((VibeBaseActivity)getActivity()).onDialogDismissed();
        }
    }
}