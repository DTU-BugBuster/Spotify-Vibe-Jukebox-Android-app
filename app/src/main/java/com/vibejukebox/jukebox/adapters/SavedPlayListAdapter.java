package com.vibejukebox.jukebox.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.vibejukebox.jukebox.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Sergex on 10/3/14.
 */
public class SavedPlayListAdapter extends BaseAdapter {

    static class ViewHolder {
        @Bind(R.id.playlistName) TextView playlistNameTextView;
        @Bind(R.id.playlistArt) ImageView playlistArtView;
        @Bind(R.id.playlistNumTracks) TextView playlistNumTracksView;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    private Context mContext = null;

    private List<String> mListOfPlaylists;

    private Map<String, Integer> mListOfTrackNumbers;

    private Map<String, String> mPlaylistImages;

    private static LayoutInflater mInflator = null;

    public SavedPlayListAdapter(Context context, List<String> listOfPlaylists,
                                Map<String, Integer> trackNumbers, Map<String,
                                String> albumImageUrls) {
        mContext = context;
        mListOfPlaylists = new ArrayList<>(listOfPlaylists);
        mListOfTrackNumbers = new HashMap<>(trackNumbers);
        mPlaylistImages = new HashMap<>(albumImageUrls);
        mInflator = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return  mListOfPlaylists.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if(convertView != null) {
            viewHolder = (ViewHolder) convertView.getTag();
        } else {
            convertView = mInflator.inflate(R.layout.playlistrow, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        TextView playlistNameText = viewHolder.playlistNameTextView;
        String playlistName = mListOfPlaylists.get(position);
        playlistNameText.setTextColor(parent.getResources().getColor(R.color.vibe_white));
        playlistNameText.setText(playlistName);

        //Playlist Art
        ImageView playlistArt = viewHolder.playlistArtView;
        Picasso.with(convertView.getContext()).load(mPlaylistImages.get(playlistName)).centerCrop().fit().into(playlistArt);

        //Number of tracks in each Playlist
        TextView playlistNumTracksView = viewHolder.playlistNumTracksView;
        playlistNumTracksView.setTextColor(ContextCompat.getColor(mContext, R.color.vibe_white));
        playlistNumTracksView.setText(String.valueOf(mListOfTrackNumbers.get(playlistName) + " tracks"));

        return convertView;
    }
}