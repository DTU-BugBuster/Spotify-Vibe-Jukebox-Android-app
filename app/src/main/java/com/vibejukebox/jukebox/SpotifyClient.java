package com.vibejukebox.jukebox;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * Created by Sergex on 8/24/14.
 */

public class SpotifyClient
{
    private static final String TAG = SpotifyClient.class.getSimpleName();
    private static final String BASE_URL = "https://accounts.spotify.com/";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        Log.d(TAG, "absolute URL is =======   " + getAbsoluteUrl(url));
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public static String getAbsoluteUrl(String relativeUrl)
    {
        return BASE_URL + relativeUrl;
    }

    public static void getSimpleReq(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.get(url, params, responseHandler);
    }

    public static void getWebApiType(String url, AsyncHttpResponseHandler responseHandler)
    {
        client.get(url, responseHandler);
    }

}
