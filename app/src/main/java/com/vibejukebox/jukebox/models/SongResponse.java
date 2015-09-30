package com.vibejukebox.jukebox.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Sergex on 6/30/15.
 */
public class SongResponse
{
    @SerializedName("playlist")
    List<String> playlist;

    public SongResponse(){
    }

    public List<String> getPlaylist()
    {
        return playlist;
    }

    public void setPlaylist(List<String> playlist)
    {
        this.playlist = playlist;
    }
}
