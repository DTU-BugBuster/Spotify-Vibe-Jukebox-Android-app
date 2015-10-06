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
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Connectivity;
import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.JukeboxApplication;
import com.vibejukebox.jukebox.JukeboxObject;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.SpotifyLoginInterface;
import com.vibejukebox.jukebox.Vibe;
import com.vibejukebox.jukebox.service.VibeService;

import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * This class is used to choose the method of getting a playlist to start. It can be either selecting
 * a playlist from the users' Spotify account or making a playlist using the Music Recommender system
 * based on the users's starred tracks.
 */

public class PlaylistSelectionActivity extends AppCompatActivity
    implements SpotifyLoginInterface
{
    private static final String TAG = PlaylistSelectionActivity.class.getSimpleName();

    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final int VIBE_GET_SPOTIFY_ARTIST_URI = 1;

    private static final int VIBE_USER_NOT_PREMIUM = 2;

    private static final int VIBE_JUKEBOX_NO_NETWORK = 3;

    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_ACCESS_TOKEN_PREF = "AccessToken";

    //private static final String SPOTIFY_API_AUTH_RESPONSE = "authresponse";

    private static final String SPOTIFY_API_USER_ID = "userID";

    /** Request code used to verify if result comes from the login Activity */
    private static final int SPOTIFY_API_REQUEST_CODE = 2015;

    /** Map to store the names and ids of user playlists */
    //private Map<String, String>mPlayListNamesAndIds;

    /** List stores the number of tracks in each playlist */
    //private Map<String, Integer> mNumOfTracks;

    /** Id of the Spotify user account */
    private String mUserId;

    private String mJukeboxName;

    private boolean mStateConnected = false;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case VIBE_GET_SPOTIFY_ARTIST_URI:
                    getArtistUri((String)msg.obj);
                    return true;
                case VIBE_USER_NOT_PREMIUM:
                    showUserNotPremiumDialog();
                    return true;
                case VIBE_JUKEBOX_NO_NETWORK:
                    showNoNetworkToast();
                    return true;
            }
            return false;
        }
    });

    private void showUserNotPremiumDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.VIBE_APP_NON_PREMIUM_ALERT_TITLE)
                .setMessage(R.string.VIBE_APP_USER_NOT_PREMIUM_MSG);
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logoutSpotify();
            }
        });

        builder.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                goBackToMain();
            }
        });

        //Create and show dialog
        builder.create().show();
    }

    private void logoutSpotify()
    {
        AuthenticationClient.logout(this);
    }

    private void goBackToMain()
    {
        NavUtils.navigateUpFromSameTask(this);
    }

    private String getCreatedJukeboxId()
    {
        Log.d(TAG, "getCreatedJukeboxId -- ");
        SharedPreferences preferences = getSharedPreferences(Vibe.VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        String jukeboxId = preferences.getString(Vibe.VIBE_JUKEBOX_STRING_PREFERENCE, null);

        Log.d(TAG, "----------------------------------------- Returning ID: " + jukeboxId);
        return jukeboxId;
    }

    /**
     * Retrieves the stored Access token from Spotify Api
     * @return: Access token string
     */
    private String getAccessToken()
    {
        if(DEBUG)
            Log.d(TAG, "getAccessToken -- ");

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        return preferences.getString(VIBE_JUKEBOX_ACCESS_TOKEN_PREF, null);
    }

    /**
     * Function stores a null jukebox id to have a new one be created in case the stored one has been corrupted.
     */
    private void storeNullJukeboxID()
    {
        SharedPreferences preferences = getSharedPreferences(Vibe.VIBE_JUKEBOX_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(Vibe.VIBE_JUKEBOX_STRING_PREFERENCE, null);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        // Get the response to obtain the authorization code
        //mAuthResponse = getIntent().getParcelableExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE);
        //mCurrentLocation = getIntent().getParcelableExtra(Vibe.VIBE_CURRENT_LOCATION);

        /*if(mAuthResponse != null){
            Log.e(TAG, "EXPIRES IN ---->  " +  mAuthResponse.getExpiresIn());
        }*/

        if(getCreatedJukeboxId() != null)
            setContentView(R.layout.activity_playlist_selection_alt);
        else
            setContentView(R.layout.activity_playlist_selection);

        Toolbar toolbar = (Toolbar)findViewById(R.id.mytoolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(R.string.VIBE_APP_GET_YOUR_VIBE);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(DEBUG)
            Log.d(TAG, "onRestart -- ");
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Checks if jukebox object still valid in the backend
        checkJukebox();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //Get the current state of network connectivity
        setConnectivityStatus();

        //Sets the current Spotify logged in user
        getAndSetCurrentUser();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playlist_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == android.R.id.home){
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a playlist based on the current users's saved tracks
     * @param view: View
     */
    public void createPlaylistFromUserFavorites(View view)
    {
        if(DEBUG){
            Log.d(TAG, "createPlaylistFromUserFavorites");
        }

        if(getNetworkConnectivity(getBaseContext()) != Connectivity.OFFLINE){
            //Start the Music Parameter Activity
            Intent intent = new Intent(this, MusicParameterActivty.class);
            intent.putExtra(Vibe.VIBE_JUKEBOX_ARTIST_RADIO, false);
            startActivity(intent);
        }
    }

    /**
     * Create a playlist based on an artist
     * @param view: View
     */
    public void getArtistRadioTracks(View view)
    {
        if(DEBUG){
            Log.d(TAG, "getArtistRadioTracks");
        }

        if(getNetworkConnectivity(getBaseContext()) != Connectivity.OFFLINE){
            //Input artist and launch Parameter activity
            setArtistRadioDialog();
        }
    }

    public void startLastTimeJukebox(View view)
    {
        if(getNetworkConnectivity(getBaseContext()) != Connectivity.OFFLINE &&
                getCreatedJukeboxId() != null)
            fetchLastJukebox();
    }

    /**
     * Select a playlist to load as starting track list to play (My Music)
     * @param view: View
     */
    public void selectListFromUserPlaylists(View view)
    {
        if(DEBUG){
            Log.d(TAG, "selectListFromUserPlaylists");
        }

        if(getNetworkConnectivity(getBaseContext()) != Connectivity.OFFLINE){
            //Starts the activity to view all user playlists
            Intent intent = new Intent(this, JukeboxSavedPlaylists.class);
            intent.putExtra(SPOTIFY_API_USER_ID, mUserId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Lost Connection to Network, please connect and try again", Toast.LENGTH_LONG).show();
            return;
        }
    }

    /**
     * Function checks if the jukebox object associated with the stored jukebox ID is valid.
     */
    private void checkJukebox()
    {
        if(DEBUG)
            Log.d(TAG, "checkJukebox -- " );

        final String jukeboxID = getCreatedJukeboxId();

        ParseObject.registerSubclass(JukeboxObject.class);
        ParseQuery<JukeboxObject> query = ParseQuery.getQuery("JukeBox");
        query.getInBackground(jukeboxID, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukebox, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully retrieved Jukebox from cloud with ID:  " + jukeboxID);

                } else {
                    if(e.getMessage().equals("no results found for query")) {
                        Log.e(TAG, "Jukebox stored no longer valid, setting ID to null. " + e.getMessage());
                        storeNullJukeboxID();
                    }

                    /*if (getNetworkConnectivity(getBaseContext()) != Connectivity.OFFLINE)
                        storeNullJukeboxID();*/
                }
            }
        });
    }

    private void getArtistUri(String artist)
    {
        if(DEBUG)
            Log.d(TAG, "getArtistUri --  "  + artist);

        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(getAccessToken());

        SpotifyService spotify = api.getService();
        spotify.searchArtists(artist, new Callback<ArtistsPager>() {
            @Override
            public void success(ArtistsPager artistsPager, Response response) {
                if (DEBUG)
                    Log.d(TAG, "Successful artist search");

                List<Artist> items = artistsPager.artists.items;

                //Checks if the query is a valid artist, if it is it will have items
                if (items.size() > 0) {
                    String name = items.get(0).uri;
                    launchParameterActivityWithArtist(name, mJukeboxName);
                } else {
                    showInvalidArtistToast();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed getting artist Uri.");
                error.printStackTrace();
            }
        });
    }

    private void showInvalidArtistToast()
    {
        Toast.makeText(this, R.string.VIBE_APP_INVALID_ARTIST_QUERY, Toast.LENGTH_LONG).show();
    }

    private void showNoNetworkToast()
    {
        Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.VIBE_APP_POOR_CONNECTION_MESSAGE),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Get the currently logged in user id from Spotify
     */
    private void getAndSetCurrentUser()
    {
        if(DEBUG)
            Log.d(TAG, "    - -  getAndSetCurrentUser");

        final String accessToken = getAccessToken();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SpotifyApi.SPOTIFY_WEB_API_ENDPOINT)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization","Bearer " +accessToken);
                    }
                })
                .build();

        SpotifyService spotify = restAdapter.create(SpotifyService.class);
        spotify.getMe(new Callback<UserPrivate>() {
            @Override
            public void success(UserPrivate user, Response response) {
                if (DEBUG)
                    Log.d(TAG, "Successful call to get current logged in user");
                mUserId = user.id;
                Log.d(TAG, "user:  " + user.display_name);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to get current logged in user...");
                if(getNetworkConnectivity(getBaseContext()) != Connectivity.OFFLINE)
                    loginSpotify();
                else
                    mHandler.sendEmptyMessage(VIBE_JUKEBOX_NO_NETWORK);
            }
        });
    }

    private Connectivity getNetworkConnectivity(Context context)
    {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if(activeNetwork != null && activeNetwork.isConnected()){
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    private void setArtistRadioDialog()
    {
        // Use the Builder class for convenient dialog construction
        LayoutInflater inflator = LayoutInflater.from(this);
        final View view = inflator.inflate(R.layout.artist_dialog_layout, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setPositiveButton(R.string.VIBE_APP_DONE, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final EditText editArtistText = (EditText)view.findViewById(R.id.artistForRadio);
                        String artist = editArtistText.getText().toString();

                        final EditText jukeboxEdit = (EditText)view.findViewById(R.id.newJukeboxName);
                        mJukeboxName = jukeboxEdit.getText().toString();

                        Log.e(TAG, "ARTIST: " + artist);
                        if(mJukeboxName.equals(""))
                            mJukeboxName = "Vibed Playlist";

                        //Start Activity with artist name
                        mHandler.sendMessage(mHandler.obtainMessage(VIBE_GET_SPOTIFY_ARTIST_URI, artist));

                    }
                })
                .setNegativeButton(R.string.VIBE_APP_CANCEL, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Cancel");
                    }
                });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    /**
     * Launches the parameter activity for playlist generation based on an artist
     * @param artistName: Name of the artist chosen by the user
     * @param jukeboxName: name of the jukebox to play
     */
    private void launchParameterActivityWithArtist(String artistName, String jukeboxName)
    {
        if(DEBUG)
            Log.d(TAG, "launchParameterActivityWithArtist -- > " + artistName);

        //Start the Music Parameter Activity
        Intent intent = new Intent(this, MusicParameterActivty.class);
        intent.putExtra(Vibe.VIBE_JUKEBOX_START_WITH_ARTIST, artistName);
        intent.putExtra(Vibe.VIBE_JUKEBOX_ARTIST_RADIO, true);
        intent.putExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME, jukeboxName);
        startActivity(intent);
    }

    /**
     * Launches the service to create a jukebox with the playlist data in Parse backend
     */
    private void fetchLastJukebox()
    {
        if(DEBUG)
            Log.d(TAG, "fetchLastJukebox -- ");

        Intent intent = new Intent(getApplicationContext(), VibeService.class);
        //intent.putExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE, mAuthResponse);
        intent.putExtra(Vibe.VIBE_JUKEBOX_SERVICE_START_FETCH, true);

        //The jukebox gets fetched in the Service
        startService(intent);
    }

    private void setConnectivityStatus()
    {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if(activeNetwork != null)
            mStateConnected = true; //Vibe.setConnectionState(true);
        else
            mStateConnected = false; //Vibe.setConnectionState(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(DEBUG)
            Log.d(TAG, "onActivityResult ");

        //Check if the result comes from the correct activity
        if(requestCode == SPOTIFY_API_REQUEST_CODE){
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            switch(response.getType()){
                //Response was successful and contains auth token
                case TOKEN:
                    //Successful response, launch activity to choose which playlist will start
                    storeAccessToken(response.getAccessToken());

                    //Checks if the current logged in user has a premium account
                    checkSpotifyProduct();
                    break;

                case ERROR:
                    Log.e(TAG, "Error getting result back from Authentication process.");
                    break;

                //Most likely auth flow was cancelled
                default:
                    break;
            }
        }
    }

    @Override
    public void loginSpotify() {
        Log.d(TAG, " -- loginSpotify -- ");

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
        AuthenticationClient.openLoginActivity(this, SPOTIFY_API_REQUEST_CODE, request);
    }

    @Override
    public void checkSpotifyProduct() {
        if(DEBUG)
            Log.d(TAG, "checkSpotifyProduct () -- ");

        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(getAccessToken());

        SpotifyService spotify = api.getService();
        spotify.getMe(new Callback<UserPrivate>() {
            @Override
            public void success(UserPrivate userPrivate, Response response) {
                String product = userPrivate.product;
                if (!product.equals("premium"))
                    mHandler.sendEmptyMessage(VIBE_USER_NOT_PREMIUM);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Error getting current user -> " + error.getMessage());
                error.printStackTrace();
            }
        });
    }

    @Override
    public void storeAccessToken(String accessToken)
    {
        if(DEBUG)
            Log.d(TAG, "storeAuthResponse - ");

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(VIBE_JUKEBOX_ACCESS_TOKEN_PREF, accessToken);
        editor.apply();
    }
}