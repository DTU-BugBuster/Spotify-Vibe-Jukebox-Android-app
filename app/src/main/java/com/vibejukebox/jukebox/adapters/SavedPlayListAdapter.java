package com.vibejukebox.jukebox.adapters;

import android.content.Context;
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

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by Sergex on 10/3/14.
 */
public class SavedPlayListAdapter extends BaseAdapter{

    private Context mContext = null;
    private List<String> mListOfPlaylists;
    private Map<String, Integer> mListOfTrackNumbers;
    private Map<String, String> mPlaylistImages;
    private static LayoutInflater mInflator = null;

    public SavedPlayListAdapter(Context context, List<String> listOfPlaylists, Map<String, Integer> trackNumbers, Map<String, String> albumImageUrls)
    {
        mContext = context;
        mListOfPlaylists = new ArrayList<>(listOfPlaylists);
        mListOfTrackNumbers = new HashMap<>(trackNumbers);
        mPlaylistImages = new HashMap<>(albumImageUrls);
        mInflator = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        int listSize = mListOfPlaylists.size();
        return listSize;
    }

    @Override
    public Object getItem(int position)
    {
        return null;
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View mView = convertView;
        if(mView == null)
            mView = mInflator.inflate(R.layout.playlistrow, parent, false);

        //Name of playlist chosen
        TextView tvName = (TextView)mView.findViewById(R.id.playlistName);
        String playlistName = mListOfPlaylists.get(position);
        tvName.setTextColor(parent.getResources().getColor(R.color.vibe_white));
        tvName.setText(playlistName);

        //Playlist Art
        ImageView artView = (ImageView)mView.findViewById(R.id.playlistArt);
        Picasso.with(mView.getContext()).load(mPlaylistImages.get(playlistName)).centerCrop().fit().into(artView);

        //Number of tracks in each Playlist
        TextView tvNumOfTracks = (TextView)mView.findViewById(R.id.playlistNumTracks);
        tvNumOfTracks.setTextColor(parent.getResources().getColor(R.color.vibe_white));
        tvNumOfTracks.setText(String.valueOf(mListOfTrackNumbers.get(playlistName) + " tracks"));

        return mView;
    }
}
