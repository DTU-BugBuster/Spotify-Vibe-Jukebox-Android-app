package com.vibejukebox.jukebox.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Track;

import java.util.ArrayList;

public class SongListAdapter extends BaseAdapter
{
	private static final String TAG = SongListAdapter.class.getSimpleName();
	private static final boolean DEBUG = true;
	
	private static LayoutInflater mInflator = null;
	Context mContext;
	//private ArrayList<String> mSongList;
	private ArrayList<Track> mTrackList;
	private boolean mShowAddSong;
    private boolean mIsSearch = false;

	public SongListAdapter(Context context, ArrayList<Track> songList, boolean showAddSong)
	{
		if(DEBUG)
			Log.e(TAG, "BaseAdapter -- ");
		
		mContext = context;
		mTrackList = new ArrayList<>(songList);
		mShowAddSong = showAddSong;
		mInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

    public void setSearch(boolean isSearch)
    {
        mIsSearch = isSearch;
    }
		
	@Override
	public int getCount() 
	{
        return mTrackList.size();
	}

	@Override
	public Object getItem(int position) 
	{
		return mTrackList.get(position);
	}
	
	public void setTrackList(ArrayList<Track> mTrackList)
	{
		this.mTrackList = mTrackList;
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View mView = convertView;
		if(mView == null)
			mView = mInflator.inflate(R.layout.viewrow, null);

        /** Song Name Text view */
		TextView songNameTView = (TextView) mView.findViewById(R.id.songName);
		Track track = mTrackList.get(position);
        songNameTView.setTextColor(parent.getResources().getColor(R.color.vibe_white));
        songNameTView.setText(track.getTrackName());

        /** Artist Name Text view */
		TextView artistTView = (TextView) mView.findViewById(R.id.artistName);
        artistTView.setTextColor(parent.getResources().getColor(R.color.vibe_white));
        artistTView.setText(track.getArtistName());

        /** Add song(plus) icon */
		ImageView imageAdd = (ImageView)mView.findViewById(R.id.add_song_plus_icon);
		imageAdd.setFocusable(false);
		imageAdd.setVisibility(ImageView.INVISIBLE);

        /** Speaker icon */
		ImageView imageSpeaker = (ImageView) mView.findViewById(R.id.speaker_icon_playing_track);
		imageSpeaker.setFocusable(false);
	
		if(position == 0 && !mIsSearch) {
			imageSpeaker.setVisibility(ImageView.VISIBLE);
            songNameTView.setTextColor(parent.getResources().getColor(R.color.vibe_border));
		}
		else
			imageSpeaker.setVisibility(ImageView.INVISIBLE);
		
		if(mShowAddSong){
			imageAdd.setVisibility(ImageView.VISIBLE);
			imageSpeaker.setVisibility(ImageView.INVISIBLE);
		}
		return mView;
	}
}
