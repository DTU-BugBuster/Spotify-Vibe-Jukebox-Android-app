package com.vibejukebox.jukebox.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.Track;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SongListAdapter extends BaseAdapter {

    static class ViewHolder {

        @Bind(R.id.songName) TextView songNameTextView;
        @Bind(R.id.artistName) TextView artistNameTextView;
        @Bind(R.id.add_song_plus_icon) ImageView imageAdd;
        @Bind(R.id.speaker_icon_playing_track) ImageView imageSpeaker;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

	private static LayoutInflater mInflator = null;
	Context mContext;
	private ArrayList<Track> mTrackList;
	private boolean mShowAddSong;
    private boolean mIsSearch = false;

	public SongListAdapter(Context context, ArrayList<Track> songList, boolean showAddSong) {
		mContext = context;
		mTrackList = new ArrayList<>(songList);
		mShowAddSong = showAddSong;
		mInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

    public void setSearch(boolean isSearch) {
        mIsSearch = isSearch;
    }
		
	@Override
	public int getCount() {
        return mTrackList.size();
	}

	@Override
	public Object getItem(int position) {
		return mTrackList.get(position);
	}
	
	public void setTrackList(ArrayList<Track> mTrackList) {
		this.mTrackList = mTrackList;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if(convertView != null){
            viewHolder = (ViewHolder)convertView.getTag();
        } else {
            convertView = mInflator.inflate(R.layout.viewrow, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        /** Song Name Text view */
        TextView songNameTView = viewHolder.songNameTextView;
        Track track = mTrackList.get(position);
        songNameTView.setTextColor(parent.getResources().getColor(R.color.vibe_white));
        songNameTView.setText(track.getTrackName());

        /** Artist Name Text view */
        TextView artistTView = viewHolder.artistNameTextView;
        artistTView.setTextColor(parent.getResources().getColor(R.color.vibe_white));
        artistTView.setText(track.getArtistName());

        /** Add song(plus) icon */
        ImageView imageAdd = viewHolder.imageAdd;
        imageAdd.setFocusable(false);
		imageAdd.setVisibility(ImageView.INVISIBLE);

        /** Speaker icon */
        ImageView imageSpeaker = viewHolder.imageSpeaker;
        imageSpeaker.setFocusable(false);
	
		if(position == 0 && !mIsSearch) {
			imageSpeaker.setVisibility(ImageView.VISIBLE);
            songNameTView.setTextColor(parent.getResources().getColor(R.color.vibe_border));
		} else {
            imageSpeaker.setVisibility(ImageView.INVISIBLE);
        }
		
		if(mShowAddSong) {
			imageAdd.setVisibility(ImageView.VISIBLE);
			imageSpeaker.setVisibility(ImageView.INVISIBLE);
		}

		return convertView;
	}
}
