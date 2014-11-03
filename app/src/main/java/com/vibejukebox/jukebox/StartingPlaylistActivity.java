package com.vibejukebox.jukebox;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.os.Handler;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.wrapper.spotify.Api;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class StartingPlaylistActivity extends Activity
{
    private static final String TAG = StartingPlaylistActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    public static final String CREATED_OBJECT_ID = "createdObjectId";
    public static final String CURRENT_SPOTIFY_USER = "spotifyUserID";
    public static final String PLAYLIST_NAMES = "playlistNames";
    public static final String PLAYLIST_TRACK_NUMBERS = "trackNumbers";
    public static final String PLAYLIST_IDS = "ids";
    private static final String OWN_JUKEBOX = "on_own_jukebox";

    private List<String> mPlaylistNames;
    private List<String> mPlaylistIds;
    private List<Integer> mPlaylistTrackNumbers;

    private boolean isHttpGood = false;

    public static final String TOKEN = "Spotify_access_token";

    //-------------------------  Spotify App Info  -------------------------------
    //App client ID
    private static final String CLIENT_ID="0cd11be7bf8d4e59a21f5961a7a70723";

    //Secret client ID
    private static final String CLIENT_SECRET_ID = "c3850b0507ab4fdea72cd2474cfccc30";

    //Redirect URI
    private static final String REDIRECT_URI="com.vibejukebox.jukeboxapp://callback";

    private static final String BASE_URL = "https://accounts.spotify.com/";
    private static final String WEB_API_BASE_URL = "https://api.spotify.com/";

    //Set a state to prevent cross site request forgeries.
    private final String mState = "vibestate";

    private String mCurrentUser = null;
    private String mCreatedObjectId;

    private static String mAccessToken = null;
    private String mRefreshToken = null;
    static Api api = null;

    private boolean mTokenValid;

    //TODO: temp variable
    private JukeboxObject mJukeboxObject;
    private static final int LAUNCH_EMPTY_JUKEBOX = 0;

    private Handler mHandler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){
                case LAUNCH_EMPTY_JUKEBOX:
                    launchEmptyJukebox();
                    return true;
            }

            return false;
        }
    });

    /** ----------------------------------------------Getters/Setters-------------------------------------------------- */

    public static String getAccessToken()
    {
        return mAccessToken;
    }

    public void setAccessToken(String token)
    {
        mAccessToken = token;
    }

    private void setRefreshToken(String token)
    {
        mRefreshToken = token;
    }

    /** --------------------------------------------------------------------------------------------------------------- */
    /**
     * Stores the access and refresh tokens to subsequently retrieve new acsess token when needed
     * @param tokens : HashMap of an access and refresh token
     */
    private void storeAuthorizationTokens(HashMap<String, String> tokens)
    {
        if(DEBUG)
            Log.d(TAG, "storeAuthorizationTokens  -- ");

        Log.d(TAG, "Storing Access -->   " + tokens.get("accessToken"));
        Log.d(TAG, "Storing Refresh -->   " + tokens.get("refreshToken"));

        SharedPreferences preferences = getSharedPreferences(TOKEN, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("access_token", tokens.get("accessToken"));
        editor.putString("refresh_token", tokens.get("refreshToken"));

        editor.apply();
    }

    private void storeNewAccessToken(String accessToken)
    {
        SharedPreferences preferences = getSharedPreferences(TOKEN, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("access_token", accessToken);

        editor.apply();
    }

    /**
     * Retrieves access_token from storage
     * @return: returns the access token string.
     */
    private String retrieveAccessToken()
    {
        SharedPreferences storage = getSharedPreferences(TOKEN, 0);
        mAccessToken = storage.getString("access_token", null);

        if(mAccessToken != null)
            return mAccessToken;
        else
            return null;

    }

    private String retrieveRefreshToken()
    {
        SharedPreferences storage = getSharedPreferences(TOKEN, 0);
        mRefreshToken = storage.getString("refresh_token", null);

        if(mRefreshToken != null)
            return mRefreshToken;
        else
            return null;
    }

    /**
     * Retrieves a new access token from a refresh token when it's expired
     * @param refreshToken
     * @return: true on success
     */
    private void getAccessTokenFromRefresh(String refreshToken)
    {
        if(DEBUG)
            Log.d(TAG, "getAccessTokenFromRefresh -- " + refreshToken);

        mTokenValid = false;
        String url = BASE_URL + "api/token";
        String idSecret = CLIENT_ID + ":" + CLIENT_SECRET_ID;
        String header = "Authorization";
        String value = new String(org.apache.commons.codec.binary.Base64.encodeBase64(idSecret.getBytes()));

        RequestParams params = new RequestParams();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(header, "Basic " + value);
        client.post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess - getting access token from refresh token.");

                try {
                    mAccessToken = response.getString("access_token");
                    storeNewAccessToken(mAccessToken);
                    Log.d(TAG, "NEW ACCESS TOKEN SET... ***********");
                    getCurrentSpotifyUser();

                    if (mAccessToken != null)
                        mTokenValid = true;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d(TAG, "onFailure with JSON object response");
                throwable.printStackTrace();

                Log.d(TAG, "JSON:  " + errorResponse.toString());
                Log.d(TAG, "****************************************** MAYBE RE-LOGIN ?");
                mTokenValid = false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d(TAG, "onFailure -- FAILED getting NEW access token");

                throwable.printStackTrace();
            }
        });

        Log.d(TAG, "Leaving getAccessTokenFromRefresh ---- ");
        Log.d(TAG, "Current token ---->   " + mAccessToken);
    }

    /**
     * Checks to see if the current access token is valid, if it's not, it validates it.
     */
    private void validateAccessToken()
    {
        if(DEBUG)
            Log.d(TAG, "validateAccessToken -- ");

        isHttpGood = false;
        String url = WEB_API_BASE_URL + "v1/me";
        String header = "Authorization";
        String value = "Bearer " + mAccessToken;

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(header, value);
        client.get(url, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response)
            {
                super.onSuccess(statusCode, headers, response);
                if(DEBUG)
                    Log.d(TAG,"SUCCEEDED call to get current User profile..");
                isHttpGood = true;
                try{
                    String user = response.getString("id");
                    mCurrentUser = user;
                    mTokenValid = true;

                } catch(JSONException e){
                    Log.e(TAG, "Exception caught validateAccessToken()");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                if(DEBUG)
                    Log.e(TAG, "Token is expired, must retrieve new one or re-login");

                isHttpGood = true;
                    try{
                        if(errorResponse != null){
                            JSONObject errorObj = errorResponse.getJSONObject("error");
                            String errorCode = errorObj.getString("status");
                            mTokenValid = false;

                            if(DEBUG){
                                Log.e(TAG, "Error Code:   "+ errorCode);
                                Log.e(TAG, "Message: " + errorObj.getString("message"));
                            }
                        }

                        //User needs to refresh the access token.
                        getAccessTokenFromRefresh(retrieveRefreshToken());

                    } catch(JSONException e){
                        e.printStackTrace();
                    }
            }
        });

        //TODO: check better solution
        /*if(!isHttpGood)
            startAuthCodeGrant();*/
    }

    // ---------------------------------------------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate -- ");
		setContentView(R.layout.activity_starting_playlist);
        setTitle(VibeJukeboxMainActivity.getDeviceName());

        mCreatedObjectId = getIntent().getStringExtra(CREATED_OBJECT_ID);
        Log.d(TAG, "Created OBJECT ID received   -- " + mCreatedObjectId);

        startAuthFlow();
	}

    private void startAuthFlow()
    {
        if(DEBUG)
            Log.d(TAG, "startAuthFlow -- ");

        mAccessToken = getIntent().getStringExtra("access_token");
        Log.d(TAG, "Access token from intent activity:  " + mAccessToken);

        if(mAccessToken == null){
            startAuthCodeGrant();
        }
        else
            validateAccessToken();
    }

    @Override
    protected void onDestroy()
    {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        if(DEBUG)
            Log.d(TAG, "onNewIntent -- ");

        mCreatedObjectId = intent.getStringExtra(CREATED_OBJECT_ID);
        Uri uri = intent.getData();
        if(uri != null)
        {
            AuthenticationResponse response = SpotifyAuthentication.parseOauthResponse(uri);
            Log.d(TAG, "CODE:  " + response.getCode());
            startTask(response.getCode());
        }
        else {
            Log.i(TAG, "URI is NULL!");
            startAuthFlow();
        }
    }

    public void createStartingPlaylist(View view)
    {
        if(DEBUG)
            Log.d(TAG, "createStartingPlaylist -- ");
        //TODO: temporary start empty jukebox function
        startEmptyJukebox();
    }

    //TODO: temporary function until implementing backend music recommendation system
    private void startEmptyJukebox()
    {
        ParseQuery<JukeboxObject> query = new ParseQuery<JukeboxObject>("JukeBox");
        query.getInBackground(mCreatedObjectId, new GetCallback<JukeboxObject>() {
            @Override
            public void done(JukeboxObject jukeboxObject, ParseException e) {
                if(e == null){
                    mJukeboxObject = jukeboxObject;
                    mCreatedObjectId = jukeboxObject.getObjectId();
                    mHandler.sendEmptyMessage(LAUNCH_EMPTY_JUKEBOX);
                }
                else{
                    Log.e(TAG, "Error getting jukebox to start empty jukebox... ");
                }
            }
        });
    }

    private void launchEmptyJukebox()
    {
        if(DEBUG)
            Log.d(TAG, "launchEmptyJukebox");

        List<String> emptyList = new ArrayList<String>();
        //emptyList.add("spotify:track:0C2C6KKors52FNdjHAIr4F");
        emptyList.clear();

        mJukeboxObject.setDefaultQueueSongIds(emptyList);
        mJukeboxObject.setQueueSongIds(emptyList);

        Intent emptyJukeboxIntent = new Intent(getApplicationContext(),JukeboxPlaylistActivity.class);
        emptyJukeboxIntent.putExtra("JUKEBOXID", mCreatedObjectId);
        emptyJukeboxIntent.putExtra("JUKEBOXNAME", mJukeboxObject.getName());
        emptyJukeboxIntent.putStringArrayListExtra("queueSongIds", (ArrayList<String>)emptyList);
        emptyJukeboxIntent.putExtra(OWN_JUKEBOX, true);
        startActivity(emptyJukeboxIntent);
    }

    public void selectStartingPlaylist(View view)
    {
        if(DEBUG)
            Log.d(TAG, "selectStartingPlaylist -- ");

        // Set current user and get its play lists
        Log.d(TAG, "Current User -->  " + mCurrentUser);
        getCurrentUserPlaylists(mCurrentUser);
    }

    private void getCurrentSpotifyUser()
    {
        String url = WEB_API_BASE_URL + "v1/me";
        if(DEBUG) {
            Log.d(TAG, "getCurrentSpotifyUser");
            Log.d(TAG, "Started with -- " + mCurrentUser);
            Log.d(TAG, "FINAL URL  getCurrentSpotifyUser ---  " + url);
            Log.d(TAG, "Stored Access Token:   " + mAccessToken);
        }

        String header = "Authorization";
        String value = "Bearer " + mAccessToken;

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(header, value);
        client.get(url, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "SUCCEEDED getting User ID... ");

                try{
                    String id = response.getString("id");
                    Log.d(TAG, "ID -------->  " + id);
                    mCurrentUser = id;

                } catch(JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d(TAG, "FAILED.... getting user ID ");
                throwable.printStackTrace();
            }
        });
    }

    private void getCurrentUserPlaylists(String user)
    {
        if(DEBUG)
            Log.d(TAG, "getCurrentUserPlaylists -- ");

        String url = WEB_API_BASE_URL + "v1/users/"  + user  +  "/playlists";
        Log.d(TAG, "FINAL URL  ---  " + url);

        String header = "Authorization";
        String value = "Bearer " + mAccessToken;

        RequestParams params = new RequestParams();
        params.put("limit", 30);
        params.put("offset", 0);

        //TODO: Take care of data structure to have number of tracks and playlist names
        final List<Integer> trackNumbers = new ArrayList<Integer>();

        //Get the names and number of tracks in each of the saved playlists in Spotify user account
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(header, value);
        client.get(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                if(DEBUG)
                    Log.d(TAG, "onSuccess -- getCurrentUserPlaylists  ");

                JSONArray jArray;

                //TODO: Change the sending of playlist information to Hashmap or other better data structure
                List<String> playlistNames = new ArrayList<String>();
                List<String> playlistIds = new ArrayList<String>();

                try{
                    jArray = response.getJSONArray("items");

                    JSONObject jsonHolder;
                    for(int i = 0; i < jArray.length(); i++)
                    {
                        jsonHolder = jArray.getJSONObject(i);
                        String playListName = jsonHolder.getString("name");
                        String playlistId =  jsonHolder.getString("id");

                        playlistNames.add(playListName);
                        playlistIds.add(playlistId);

                        JSONObject trackObject = jsonHolder.getJSONObject("tracks");
                        int numOfTracks = trackObject.getInt("total");
                        trackNumbers.add(Integer.valueOf(numOfTracks));
                    }

                    //Save the list of user playlists that are in Spotify account
                    setSavedPlaylists(playlistNames);
                    setPlaylistIds(playlistIds);
                    setPlaylistTrackNumbers(trackNumbers);

                } catch(JSONException e){
                    e.printStackTrace();
                }

                displaySpotifyUserPlaylists();
                /*for(String playlist: playlistNames)
                {
                    //Log.d(TAG, "PLAYLIST:    " + playlist);
                }*/

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d(TAG, "FAILED.... JSON call  ");
                throwable.printStackTrace();
            }
        });
    }

    private void setSavedPlaylists(List<String> playlists)
    {
        mPlaylistNames = new ArrayList<String>(playlists);
    }

    private void setPlaylistIds(List<String> ids)
    {
        mPlaylistIds = new ArrayList<String>(ids);
    }

    private void setPlaylistTrackNumbers(List<Integer> list)
    {
        mPlaylistTrackNumbers = new ArrayList<Integer>(list);
    }

    private void displaySpotifyUserPlaylists()
    {
        Intent intent = new Intent(this, JukeboxSavedPlaylists.class);
        intent.putExtra(CREATED_OBJECT_ID, mCreatedObjectId);
        intent.putExtra(CURRENT_SPOTIFY_USER, mCurrentUser);
        intent.putStringArrayListExtra(PLAYLIST_NAMES, (ArrayList) mPlaylistNames);
        intent.putIntegerArrayListExtra(PLAYLIST_TRACK_NUMBERS, (ArrayList) mPlaylistTrackNumbers);
        intent.putStringArrayListExtra(PLAYLIST_IDS, (ArrayList) mPlaylistIds);
        startActivity(intent);
    }

    //     _         _   _                _   _           _   _
    //    / \  _   _| |_| |__   ___ _ __ | |_(_) ___ __ _| |_(_) ___  _ __
    //   / _ \| | | | __| '_ \ / _ \ '_ \| __| |/ __/ _` | __| |/ _ \| '_ \
    //  / ___ \ |_| | |_| | | |  __/ | | | |_| | (_| (_| | |_| | (_) | | | |
    // /_/   \_\__,_|\__|_| |_|\___|_| |_|\__|_|\___\__,_|\__|_|\___/|_| |_|
    //

    private void startAuthCodeGrant()
    {
        if(DEBUG)
            Log.d(TAG, "startAuthCodeGrant -- ");

        /* Create a default API instance that will be used to make requests to Spotify */
        final Api api = Api.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET_ID)
                .redirectURI(REDIRECT_URI)
                .build();

        final List<String> scopes = Arrays.asList("user-read-private", "playlist-read", "playlist-read-private", "streaming");
        String authorizeURL = api.createAuthorizeURL(scopes, mState);

        if(DEBUG)
            Log.d(TAG, "AUTH-URL --------------- >   " + authorizeURL);

        setApiObject(api);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeURL));
        startActivity(intent);
    }

    private void setApiObject(Api nApi)
    {
        api = nApi;
    }

    private void startTask(String code)
    {
        if(DEBUG)
            Log.d(TAG, "startTask -- ");

        RequestParams params = new RequestParams();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET_ID);

        // Call to Web API to request refresh and access tokens
        String postURI = "https://accounts.spotify.com/api/token";
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(postURI, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "SUCCESS with POST - Requesting tokens ...");

                try{
                    String access_token = response.getString("access_token");
                    String refresh_token = response.getString("refresh_token");

                    if(DEBUG) {
                        Log.d(TAG, "Refresh Token:   " + refresh_token);
                        Log.d(TAG, "Access Token:   " + access_token);
                    }

                    HashMap<String, String> tokens = new HashMap<String, String>();
                    tokens.put("accessToken", access_token);
                    tokens.put("refreshToken", refresh_token);

                    setAccessToken(access_token);
                    setRefreshToken(refresh_token);

                    storeAuthorizationTokens(tokens);

                    //Set the Spotify user id for the session
                    getCurrentSpotifyUser();

                } catch(JSONException e){

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e(TAG, "FAILED POST ... ");
                throwable.printStackTrace();
            }
        });

        //WrapperTask task = new WrapperTask(code);
        //task.execute(getApiObject());
    }
}
