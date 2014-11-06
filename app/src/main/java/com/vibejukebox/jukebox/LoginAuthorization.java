package com.vibejukebox.jukebox;

import android.content.SharedPreferences;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Sergex on 11/5/14.
 */
public final class LoginAuthorization
{
    public static final String TAG = LoginAuthorization.class.getSimpleName();
    public static final boolean DEBUG = DebugLog.DEBUG;

    //-------------------------  Spotify App Info  -------------------------------
    //App client ID
    private static final String CLIENT_ID="0cd11be7bf8d4e59a21f5961a7a70723";

    //Secret client ID
    private static final String CLIENT_SECRET_ID = "c3850b0507ab4fdea72cd2474cfccc30";

    //Redirect URI
    private static final String REDIRECT_URI="com.vibejukebox.jukeboxapp://callback";

    public static final String TOKEN = "Spotify_access_token";
    private String mRefreshToken;
    private String mAccessToken;

    private boolean isHttpGood = false;
    private static final String BASE_URL = "https://accounts.spotify.com/";
    private static final String WEB_API_BASE_URL = "https://api.spotify.com/";


    private LoginAuthorization(){
        //private constructor avoids instance creation
    }

    public static void init(){
        //startAuthFlow();
    }

    /*private void startAuthFlow()
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
    }*/


    /*public static void storeNewAccessToken(String accessToken)
    {
        SharedPreferences preferences =
                getSharedPreferences(TOKEN, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("access_token", accessToken);

        editor.apply();
    }

    /**
     * Retrieves access_token from storage
     * @return: returns the access token string.
     */
    /*public static String retrieveAccessToken()
    {
        SharedPreferences storage = getSharedPreferences(TOKEN, 0);
        mAccessToken = storage.getString("access_token", null);

        if(mAccessToken != null)
            return mAccessToken;
        else
            return null;

    }

    /**
     * Retrieves refresh_token from storage
     * @return: returns the access token string.
     */
    /*private String retrieveRefreshToken()
    {
        SharedPreferences storage = getSharedPreferences(TOKEN, 0);
        mRefreshToken = storage.getString("refresh_token", null);

        if(mRefreshToken != null)
            return mRefreshToken;
        else
            return null;
    }

    public void validateAccessToken(String accessToken)
    {
        if(DEBUG)
            Log.d(TAG, "validateAccessToken -- ");

        isHttpGood = false;
        String url = WEB_API_BASE_URL + "v1/me";
        String header = "Authorization";
        String value = "Bearer " + accessToken;

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
                    //mCurrentUser = user;
                    //mTokenValid = true;

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
                        //mTokenValid = false;

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
    }*/

    /**
     * Retrieves a new access token from a refresh token when it's expired
     * @param refreshToken
     * @return: true on success
     */
    /*private void getAccessTokenFromRefresh(String refreshToken)
    {
        if(DEBUG)
            Log.d(TAG, "getAccessTokenFromRefresh -- " + refreshToken);

        //mTokenValid = false;
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
                    //getCurrentSpotifyUser();
                    *//*if (mAccessToken != null)
                        mTokenValid = true;*//*

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
                //mTokenValid = false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d(TAG, "onFailure -- FAILED getting NEW access token");

                throwable.printStackTrace();
            }
        });
    }*/
}
