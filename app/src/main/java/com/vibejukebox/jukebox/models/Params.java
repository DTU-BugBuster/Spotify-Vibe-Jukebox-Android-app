package com.vibejukebox.jukebox.models;


import com.google.gson.annotations.SerializedName;

/**
 * Created by Sergex on 6/29/15.
 */
public class Params
{
    @SerializedName("acousticness")
    private double acousticness;

    @SerializedName("energy")
    private double energy;

    @SerializedName("danceability")
    private double danceability;

    public Params(){

    }

    public double getAcousticness() {
        return acousticness;
    }

    public void setAcousticness(double acousticness) {
        this.acousticness = acousticness;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public double getDanceability() {
        return danceability;
    }

    public void setDanceability(double danceability) {
        this.danceability = danceability;
    }
}
