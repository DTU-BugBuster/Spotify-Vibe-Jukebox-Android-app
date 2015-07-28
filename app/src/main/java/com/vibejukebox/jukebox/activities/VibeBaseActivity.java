package com.vibejukebox.jukebox.activities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

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
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Vibe;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sergex on 7/26/15.
 */
public abstract class VibeBaseActivity extends AppCompatActivity implements
        LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG = VibeBaseActivity.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    private boolean mResolvingError = false;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    /** ----------------------    Connection and Location Services-----------------------------------*/
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;

    //update interval
    private static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
            * UPDATE_INTERVAL_IN_SECONDS;

    // A fast interval ceiling
    private static final int FAST_CEILING_IN_MILLISECONDS = 3;

    private static boolean mLocationServicesConnected = false;

    /** Location object indication the last known location */
    protected Location mLastLocation;

    private LocationRequest mLocationRequest;

    protected boolean mRequestLocationUpdates = false;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        //Connect to location services
        buildGoogleApiClient();

        //TODO:(Not implemented) boolean is always false, check if it will be needed
        if(mRequestLocationUpdates)
            createLocationRequest();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(DEBUG)
            Log.d(TAG, "onStart -- ");

        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUG)
            Log.d(TAG, "onResume --");
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
        super.onStop();
        if(DEBUG)
            Log.d(TAG, "onStop -- ");

        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
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
        Log.i(TAG, "Location Changed -- (Application)");
        Log.d(TAG, "New Location is: " + String.valueOf(location.getLatitude()) + "  "
                + String.valueOf(location.getLongitude()));
    }

    /** Connection Callbacks */
    @Override
    public void onConnected(Bundle bundle)
    {
        if(DEBUG)
            Log.d(TAG, "onConnected - Connected to location Services (Application)");

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
        mGoogleApiClient.connect();
    }

    /** Connection Failed Listener */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
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
                    if(DEBUG)
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
