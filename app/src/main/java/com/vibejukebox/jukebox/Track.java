package com.vibejukebox.jukebox;

import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable 
{
	public Track(String artist, String song)
	{
		artistName = artist;
		name = song;
	}
	
	public Track(String artist, String song, String songId)
	{
		artistName = artist;
		name = song;
		href = songId;
	}
	
	private String name;
	public String getName(){
		return this.name;
	}
	
	public void setTrackName(String trackName)
	{
		trackName = name;
	}
	
	private float popularity;
	public float getPopularity()
    {
		return this.popularity;
	}
	
	private long length;
	public long getLength()
    {
		return this.length;
	}
	
	private String href;
	public String getHref()
    {
		return this.href;
	}
	
	public void setHref(String id)
    {
		href = id;
	}

	public String artistName;
	public String getArtistName()
	{
		return artistName;
	}

	public void setArtistName(String artistName)
	{
		this.artistName = artistName; 
	}
	
	private String trackNumber;
	public String getTrackNumber()
    {
		return this.trackNumber;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(artistName);
		dest.writeString(name);
		dest.writeString(href);
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
		name = in.readString();
		href = in.readString();
	}
}

