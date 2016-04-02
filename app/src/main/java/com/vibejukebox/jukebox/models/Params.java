package com.vibejukebox.jukebox.models;


import com.google.gson.annotations.SerializedName;

/**
 * Created by Sergex on 6/29/15.
 *
 */
public class Params
{
    @SerializedName("acousticness")
    private Double acousticness;

    @SerializedName("energy")
    private Double energy;

    @SerializedName("danceability")
    private Double danceability;

    public Params(){}

    public double getAcousticness() {
        return acousticness;
    }

    public void setAcousticness(Double acousticness) {
        this.acousticness = acousticness;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(Double energy) {
        this.energy = energy;
    }

    public double getDanceability() {
        return danceability;
    }

    public void setDanceability(Double danceability) {
        this.danceability = danceability;
    }
}
