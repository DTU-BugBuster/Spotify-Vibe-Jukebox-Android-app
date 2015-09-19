package com.vibejukebox.jukebox.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxApplication;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Vibe;

import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class VibeJukeboxMainActivity extends VibeBaseActivity
{
    /** ----------------------    Fields -----------------------------------*/
	private static final String TAG = VibeJukeboxMainActivity.class.getSimpleName();

    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_ACCESS_TOKEN_PREF = "AccessToken";

    private static final int VIBE_LAUNCH_PLAYLIST = 100;

    private static final int VIBE_USER_NOT_PREMIUM = 200;

    /** Request code used to verify if result comes from the login Activity */
    private static final int SPOTIFY_API_REQUEST_CODE = 2015;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_LAUNCH_PLAYLIST:
                    launchPlayListSelection((AuthenticationResponse)msg.obj);
                    return true;
                case VIBE_USER_NOT_PREMIUM:
                    showUserNotPremiumDialog();
                    return true;
            }
            return false;
        }
    });

    private void showUserNotPremiumDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.VIBE_APP_NON_PREMIUM_ALERT_TITLE)
                .setMessage(R.string.VIBE_APP_ARTIST_NOT_PREMIUM_MSG);
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Free account type user can join jukebox only
                joinJukebox();
            }
        });

        builder.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        //Create and show dialog
        builder.create().show();
    }

    /** ----------------------------    Parse Jukebox  -----------------------------------*/
    private String getCreatedJukeboxId()
    {
        Log.d(TAG, "getCreatedJukeboxId -- ");
        SharedPreferences preferences = getSharedPreferences(Vibe.VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(Vibe.VIBE_JUKEBOX_STRING_PREFERENCE, null);

        Log.e(TAG, "------------------------------------------------ Returning ID: " + jukeboxId);
        return jukeboxId;
    }

    /**
     * Stores the Access token from Spotify in the shared preferences
     * @param accessToken
     */
    private void storeAccessToken(String accessToken)
    {
        if(DEBUG)
            Log.d(TAG, "storeAuthResponse - ");

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(VIBE_JUKEBOX_ACCESS_TOKEN_PREF, accessToken);
        editor.commit();
    }

    /**
     * TODO:  FOR TEST, DELETE THIS FUNCTION LATER..
     */
    private void storeNULLJukeboxID()
    {
        SharedPreferences preferences = getSharedPreferences("JukeboxPreferences", 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("JukeboxStoredId", null);
        editor.apply();
    }

    //--------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        if(DEBUG)
            Log.d(TAG, "onCreate()");

		setContentView(R.layout.activity_jukebox_main);
        setConnectivityStatus();

        //TODO: Testing only
        //storeNULLJukeboxID();
	}

    @Override
    protected void onStart()
    {
        super.onStart();
        if(DEBUG)
            Log.d(TAG, "onStart -- ");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if(DEBUG)
            Log.d(TAG, "onStop -- ");
    }

    private void setConnectivityStatus(){
        ConnectivityManager connectivityManager =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if(activeNetwork != null)
            Vibe.setConnectionState(true);
        else
            Vibe.setConnectionState(false);
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
	}

    /**
     * Function called when a user requests to join an active playlist
     * @param view: the view that got clicked
     */
	public void join(View view)
	{
		if(DEBUG)
			Log.d(TAG, "Joining Jukebox ...");

		joinJukebox();
	}

    private void joinJukebox()
    {
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

        if(!Vibe.getConnectivityStatus()){
            Toast.makeText(this, "Lost connection to Network, please connect and try again", Toast.LENGTH_LONG).show();
            return;
        }

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
                    storeAccessToken(response.getAccessToken());

                    //Checks if the current logged in user has a premium account
                    checkSpotifyProduct(response);
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
     * Function checks the currently logged in user type of account
     * @param authResponse: Spotify Authentication response of current logged in user
     */
    private void checkSpotifyProduct(final AuthenticationResponse authResponse)
    {
        if(DEBUG)
            Log.d(TAG, "checkSpotifyProduct -- ");

        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(authResponse.getAccessToken());

        SpotifyService spotify = api.getService();
        spotify.getMe(new Callback<UserPrivate>() {
            @Override
            public void success(UserPrivate userPrivate, Response response) {
                String product = userPrivate.product;
                if (product.equals("premium"))
                    mHandler.sendMessage(mHandler.obtainMessage(VIBE_LAUNCH_PLAYLIST, authResponse));
                else
                    mHandler.sendEmptyMessage(VIBE_USER_NOT_PREMIUM);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Error getting current user -> " + error.getMessage());
                error.printStackTrace();
            }
        });
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

        builder.setShowDialog(true)
                .setScopes(new String[]{"user-read-private",
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
        int numberOfJukeboxFound = jukeboxList.size();
        final TextView tv = (TextView)findViewById(R.id.nearbyJukeboxesTextView);

        if(numberOfJukeboxFound == 0)
            tv.setText(R.string.no_nearby_jukeboxes_found);
        else {
            String text = String.valueOf(numberOfJukeboxFound) + " " +
                    getString(R.string.nearby_jukeboxes_found);
            tv.setText(text);
        }
    }
}