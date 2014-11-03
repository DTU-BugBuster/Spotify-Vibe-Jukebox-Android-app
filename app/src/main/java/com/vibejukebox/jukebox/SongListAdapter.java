package com.vibejukebox.jukebox;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SongListAdapter extends BaseAdapter
{
	private static final String TAG = SongListAdapter.class.getSimpleName();
	private static final boolean DEBUG = false;
	
	private static LayoutInflater mInflator = null;
	Context mContext;
	//private ArrayList<String> mSongList;
	private ArrayList<Track> mTrackList;
	private boolean mShowAddSong;
	
	
	public SongListAdapter(Context context, ArrayList<Track> songList, boolean showAddSong)
	{
		if(DEBUG)
			Log.e(TAG, "BaseAdapter -- ");
		
		mContext = context;
		mTrackList = songList;
		mShowAddSong = showAddSong;
		mInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
		
	@Override
	public int getCount() 
	{
		int listSize = mTrackList.size();
		return listSize;
	}

	@Override
	public Object getItem(int position) 
	{
		return mTrackList.get(position);
	}
	
	public void setmTrackList(ArrayList<Track> mTrackList) 
	{
		this.mTrackList = mTrackList;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View mView = convertView;
		if(mView == null)
			mView = mInflator.inflate(R.layout.viewrow, null);

		TextView textView = (TextView) mView.findViewById(R.id.songName);
		Track track = (Track)mTrackList.get(position);
		textView.setText(track.getName());
		
		TextView tV = (TextView) mView.findViewById(R.id.artistName);
		tV.setText(track.getArtistName());
		
		ImageView imageAdd = (ImageView)mView.findViewById(R.id.add_song_plus_icon);
		imageAdd.setFocusable(false);
		imageAdd.setVisibility(ImageView.INVISIBLE);
		
		ImageView imageSpeaker = (ImageView) mView.findViewById(R.id.speaker_icon_playing_track);
		imageSpeaker.setFocusable(false);
	
		if(position == 0)
			imageSpeaker.setVisibility(ImageView.VISIBLE);
		else
			imageSpeaker.setVisibility(ImageView.INVISIBLE);
		
		if(mShowAddSong){
			imageAdd.setVisibility(ImageView.VISIBLE);
			imageSpeaker.setVisibility(ImageView.INVISIBLE);
		}
	
		return mView;
	}
}
