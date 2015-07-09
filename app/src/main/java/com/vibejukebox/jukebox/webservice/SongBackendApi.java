package com.vibejukebox.jukebox.webservice;

import retrofit.RestAdapter;

/**
 * Created by Sergex on 6/29/15.
 */
public class SongBackendApi
{
    private static SongBackendApi INSTANCE;

    private SongBackendService mService;

    private SongBackendApi()
    {
        mService = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
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
