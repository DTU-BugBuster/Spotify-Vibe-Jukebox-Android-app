package com.vibejukebox.jukebox;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParseClassName("JukeBox")
public class JukeboxObject extends ParseObject implements Parcelable 
{
	private static final String TAG = JukeboxObject.class.getSimpleName();
	private static final boolean DEBUG = false;

    private static final String QUEUE_SONGS = "queuesongs";
	
	private String mName;
	private String mObjectID;
	private List<String> mQueueSongIDs;
    private List<String> mDefaultQueueSongIDs;
    private ParseGeoPoint mLocation;
    private boolean mCurrentlyPlaying;

	public JukeboxObject(){
	}
	
	private JukeboxObject(Parcel in){
		mName = in.readString();
		mObjectID = in.readString();

		Bundle bundle = in.readBundle();
        mQueueSongIDs = bundle.getStringArrayList(QUEUE_SONGS);
	}
	
	public String getName()
    {
		return getString("name");
	}
	
	public void setName(String name)
    {
		this.mName = name;
	}

	public String getObjectID()
    {
		return getObjectId();
	}
	
	public void setId(String id)
    {
		//setObjectId(id);
		this.mObjectID = id;
	}

	public ParseGeoPoint getLocation()
    {
        return (ParseGeoPoint)get("location");
    }

    public void setLocation(ParseGeoPoint locPoint)
    {
        mLocation = locPoint;
    }
	
	public List<String> getQueueSongIds()
	{
		if(DEBUG)
			Log.e(TAG, "getQueueSongIds -- JukeboxObject");
		
		ArrayList<String> songQueue = (ArrayList<String>) get("queueSongIDs"); 

		if(songQueue == null)
			return Collections.<String>emptyList();
		else
			return songQueue;
	}
	
	public void setQueueSongIds(List<String> ids)
	{
        if(DEBUG)
            Log.d(TAG, "Saving Queue songs in background");

        mQueueSongIDs = new ArrayList<>(ids);
        put("queueSongIDs", mQueueSongIDs);
        saveInBackground();
	}

    public List<String> getDefaultQueueSongIds()
    {
        if(DEBUG)
            Log.e(TAG, "getDefaultQueueSongIds -");

        List<String> songQueue = (ArrayList<String>) get("defaultQueueSongIDs");
        if(songQueue == null)
            return Collections.<String>emptyList();
        else
            return songQueue;
    }

    public void setDefaultQueueSongIds(List<String> ids)
    {
        mDefaultQueueSongIDs = new ArrayList<String>(ids);
        put("defaultQueueSongIDs", mDefaultQueueSongIDs);
        saveInBackground();
    }

	public static final Parcelable.Creator<JukeboxObject> CREATOR = 
			new Parcelable.Creator<JukeboxObject>(){

		@Override
		public JukeboxObject createFromParcel(Parcel source) {
			return new JukeboxObject(source);
		}

		@Override
		public JukeboxObject[] newArray(int size) {
			return new JukeboxObject[size];
		}
	};

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeString(mObjectID);
		Bundle bundle = new Bundle();
		bundle.putStringArrayList(QUEUE_SONGS, (ArrayList<String>)mQueueSongIDs);
		dest.writeBundle(bundle);
	}
}
