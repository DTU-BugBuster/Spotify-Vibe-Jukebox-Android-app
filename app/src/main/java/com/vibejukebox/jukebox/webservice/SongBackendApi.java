package com.vibejukebox.jukebox.webservice;

import com.vibejukebox.jukebox.DebugLog;

import retrofit.RestAdapter;

/**
 * Created by Sergex on 6/29/15.
 */
public class SongBackendApi
{
    private static final boolean DEBUG = DebugLog.DEBUG;

    private static SongBackendApi INSTANCE;

    private SongBackendService mService;

    private SongBackendApi()
    {
        RestAdapter.LogLevel logLevel;
        if(DEBUG) {
            logLevel = RestAdapter.LogLevel.FULL;
        } else {
            logLevel = RestAdapter.LogLevel.BASIC;
        }

        mService = new RestAdapter.Builder()
                .setLogLevel(logLevel)
                .setEndpoint("https://frozen-atoll-2814.herokuapp.com")
                .build()
                .create(SongBackendService.class);
    }

    public static SongBackendApi getInstance(){
        if(INSTANCE == null){
            INSTANCE = new SongBackendApi();
        }

        return INSTANCE;
    }

    public SongBackendService getBackendService(){
        return mService;
    }
}
