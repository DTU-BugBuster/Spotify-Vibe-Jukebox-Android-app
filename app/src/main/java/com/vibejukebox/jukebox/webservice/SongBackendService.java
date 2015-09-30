package com.vibejukebox.jukebox.webservice;

import com.vibejukebox.jukebox.models.Params;
import com.vibejukebox.jukebox.models.SongResponse;
import com.vibejukebox.jukebox.models.SongTask;

import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.Callback;
import retrofit.http.Part;


/**
 * Created by Sergex on 6/27/15.
 */
public interface SongBackendService
{
    //@Headers("Content-Type: application/json")
    @POST("/")
    void requestTracksFromFavorites(@Body SongTask songTask, Callback<SongResponse> response);
}
