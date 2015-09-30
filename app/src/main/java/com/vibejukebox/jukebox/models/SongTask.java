package com.vibejukebox.jukebox.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Sergex on 6/29/15.
 */
public class SongTask
{
    @SerializedName("artist_radio")
    boolean artist_radio = false;

    @SerializedName("params")
    Params params;

    @SerializedName("input_list")
    List<String> playlist;

    @SerializedName("common_tracks")
    boolean common_tracks = false;

    public SongTask(){
    }

    public SongTask(boolean isArtistRadio, Params params, List<String> inputList)
    {
        this.artist_radio = isArtistRadio;
        this.params = params;
        this.playlist = inputList;
    }

    public SongTask(boolean isArtistRadio, boolean commonTracks, Params params, List<String> inputList)
    {
        this.artist_radio = isArtistRadio;
        this.common_tracks = commonTracks;
        this.params = params;
        this.playlist = inputList;
    }

    public Params getParams()
    {
        return params;
    }

    public void setParams(Params params)
    {
        this.params = params;
    }

    public List<String> getPlaylist()
    {
        return playlist;
    }

    public void setPlaylist(List<String> playlist)
    {
        this.playlist = playlist;
    }

    public void setArtistRadio(boolean isArtistRadio)
    {
        this.artist_radio = isArtistRadio;
    }
}

