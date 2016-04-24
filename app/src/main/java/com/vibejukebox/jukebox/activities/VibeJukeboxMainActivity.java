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
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Vibe;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.util.List;

public class VibeJukeboxMainActivity extends VibeBaseActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    /** ----------------------    Fields -----------------------------------*/
	private static final String TAG = VibeJukeboxMainActivity.class.getSimpleName();

    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final int VIBE_LAUNCH_PLAYLIST = 100;

    private static final int VIBE_USER_NOT_PREMIUM = 200;

    private int mNumJukeboxesNear = 0;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_LAUNCH_PLAYLIST:
                    launchPlayListSelection(/*(AuthenticationResponse)msg.obj*/);
                    return true;
                case VIBE_USER_NOT_PREMIUM:
                    showUserNotPremiumDialog();
                    return true;
            }
            return false;
        }
    });

    private void showUserNotPremiumDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.VIBE_APP_NON_PREMIUM_ALERT_TITLE)
                .setMessage(R.string.VIBE_APP_USER_NOT_PREMIUM_MSG);
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Free account type user can join jukebox only
                //joinJukebox();
                logoutSpotify();
            }
        });

        builder.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSuggestionToast();
            }
        });

        //Create and show dialog
        builder.create().show();
    }

    private void logoutSpotify() {
        AuthenticationClient.clearCookies(this);
    }

    private void showSuggestionToast() {
        Toast.makeText(this, R.string.VIBE_APP_JOIN_JUKEBOX_INSTEAD, Toast.LENGTH_LONG).show();
    }

    /** ----------------------------    Parse Jukebox  -----------------------------------*/
    private String getCreatedJukeboxId() {
        if(DEBUG) {
            Log.d(TAG, "getCreatedJukeboxId -- ");
        }

        SharedPreferences preferences = getSharedPreferences(Vibe.VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(Vibe.VIBE_JUKEBOX_STRING_PREFERENCE, null);

        //Log.d(TAG, "------------------------------------------------ Returning ID: " + jukeboxId);
        return jukeboxId;
    }

    /**
     * TODO:  FOR TEST, DELETE THIS FUNCTION LATER..
     */
    private void storeNULLJukeboxID() {
        SharedPreferences preferences = getSharedPreferences("JukeboxPreferences", 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("JukeboxStoredId", null);
        editor.apply();
    }

    //--------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) {
            Log.d(TAG, "onCreate()");
        }

        String runtime = System.getProperty("java.vm.version");
        Log.e(TAG, "RUNTIME:  " + runtime);

        // Possible work around for market launches. See http://code.google.com/p/android/issues/detail?id=2373
        // for more details. Essentially, the market launches the main activity on top of other activities.
        // we never want this to happen. Instead, we check if we are the root and if not, we finish.
        if(!isTaskRoot()) {
            final Intent intent = getIntent();
            if(intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
                Log.w(TAG, getResources().getString(R.string.VIBE_NOT_ROOT_TASK));
                finish();
                return;
            }
        }

		setContentView(R.layout.activity_jukebox_main);
        setConnectivityStatus();

        //TODO: Testing only
        //storeNULLJukeboxID();
	}

    private void setConnectivityStatus() {

        ConnectivityManager connectivityManager =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if(activeNetwork != null) {
            Vibe.setConnectionState(true);
        } else {
            Vibe.setConnectionState(false);
        }
    }

    private void updateUiStartAppButtons() {
        Button createJukeboxButton = (Button)findViewById(R.id.createJukeboxButton);

        if(createJukeboxButton != null) {
            if(getCreatedJukeboxId() != null) {
                createJukeboxButton.setText(R.string.VIBE_APP_START_JUKEBOX);
            } else {
                createJukeboxButton.setText(R.string.VIBE_APP_CREATE_JUKEBOX);
            }
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.vibe_jukebox_main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(DEBUG) {
            Log.d(TAG, "onResume -- ");
        }

        // Update the main start button depending if a jukebox was already created or not.
        updateUiStartAppButtons();
	}

    /**
     * Function called when a user requests to join an active playlist
     * @param view: the view that got clicked
     */
	public void join(View view) {
		if(DEBUG) {
            Log.d(TAG, "Joining Jukebox ...");
        }

        joinJukebox();
	}

    private void joinJukebox() {
        if(mNumJukeboxesNear > 0){
            Intent intent = new Intent(this, JukeboxListOfJukeboxes.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.VIBE_APP_NO_NEARBY_JUKEBOXES_MSG, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Function called when a user wants to start a playlist of its own
     * @param view
     */
    public void startJukebox(View view) {
        if(DEBUG) Log.d(TAG, "createNewJukebox -- ");

        if(!Vibe.getConnectivityStatus()){
            Toast.makeText(this, "Lost connection to Network, please connect and try again",
                    Toast.LENGTH_LONG).show();
            return;
        }

        //User must login to Spotify account to be able to stream music
        //loginToSpotifyAccount(); TODO: inTEST

        if(Vibe.getCurrentLocation() == null) {
            Toast.makeText(this, "Grant Location permission to create a new jukebox. ", Toast.LENGTH_LONG).show();
        } else {
            loginToSpotify(this);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);

        fetchNearbyJukeboxes();
    }

    @Override
    protected void checkSpotifyProduct(final AuthenticationResponse authResponse) {
        if(DEBUG) {
            Log.d(TAG, "checkSpotifyProduct () -- ");
        }

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
     * Launch Activity to choose starting playlist
     */
    private void launchPlayListSelection(/*AuthenticationResponse authResponse*/) {
        Intent intent = new Intent(this, PlaylistSelectionActivity.class);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ID, getCreatedJukeboxId());
        startActivity(intent);
    }

    @Override
    protected void nearbyJukeboxesFound(List<JukeboxObject> jukeboxList) {
        int numberOfJukeboxFound = jukeboxList.size();
        mNumJukeboxesNear = numberOfJukeboxFound;

        final TextView tv = (TextView)findViewById(R.id.nearbyJukeboxesTextView);

        if(numberOfJukeboxFound == 0) {
            tv.setText(R.string.no_nearby_jukeboxes_found);
        } else {
            String text = String.valueOf(numberOfJukeboxFound) + " " +
                    getString(R.string.nearby_jukeboxes_found);
            tv.setText(text);
        }
    }
}