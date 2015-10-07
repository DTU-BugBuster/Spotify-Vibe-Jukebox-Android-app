package com.vibejukebox.jukebox;

import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable 
{
	private String artistName;
	private String trackName;
	private String trackUri;

    public Track(){

    }

	public Track(String artist, String trackName)
	{
		this.artistName = artist;
		this.trackName = trackName;
	}
	
	public Track(String artist, String trackName, String trackUri)
	{
		this.artistName = artist;
		this.trackName = trackName;
		this.trackUri = trackUri;
	}

	public String getTrackName(){
		return this.trackName;
	}
	
	public void setTrackName(String trackName)
	{
		this.trackName = trackName;
	}

	public String getArtistName()
	{
		return this.artistName;
	}

	public void setArtistName(String artistName)
	{
		this.artistName = artistName; 
	}

    public String getTrackUri(){
        return this.trackUri;
    }

    public void setTrackUri(String trackUri)
	{
        this.trackUri = trackUri;
    }

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(artistName);
		dest.writeString(trackName);
		dest.writeString(trackUri);
	}
	
	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() 
	{
		public Track createFromParcel(Parcel in) 
		{
			return new Track(in);
		}

		public Track[] newArray(int size) 
		{
			return new Track[size];
		}
	};
	
	private Track(Parcel in)
	{
		artistName = in.readString();
		trackName = in.readString();
		trackUri = in.readString();
	}
}

