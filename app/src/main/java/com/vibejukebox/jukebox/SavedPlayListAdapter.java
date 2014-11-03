package com.vibejukebox.jukebox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sergex on 10/3/14.
 */
public class SavedPlayListAdapter extends BaseAdapter{

    private Context mContext = null;
    private List<String> mListOfPlaylists;
    private List<Integer> mListOfTrackNumbers;
    private static LayoutInflater mInflator = null;

    public SavedPlayListAdapter(Context context, List<String> listOfPlaylists, List<Integer> trackNumbers)
    {
        mContext = context;
        mListOfPlaylists = new ArrayList<String>(listOfPlaylists);
        mListOfTrackNumbers = new ArrayList<Integer>(trackNumbers);
        mInflator = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        int listSize = mListOfPlaylists.size();
        return listSize;
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
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View mView = convertView;
        if(mView == null)
            mView = mInflator.inflate(R.layout.viewrow, null);

        TextView tvName = (TextView)mView.findViewById(R.id.songName);
        tvName.setText(mListOfPlaylists.get(position));

        TextView tvNumOfTracks = (TextView)mView.findViewById(R.id.artistName);
        tvNumOfTracks.setText(String.valueOf(mListOfTrackNumbers.get(position)) + " tracks");

        //Since reusing viewrow.xml, we set the images to invisible in this case
        ImageView imageAdd = (ImageView)mView.findViewById(R.id.add_song_plus_icon);
        imageAdd.setVisibility(ImageView.INVISIBLE);

        ImageView imageSpeaker = (ImageView) mView.findViewById(R.id.speaker_icon_playing_track);
        imageSpeaker.setVisibility(ImageView.INVISIBLE);

        return mView;
    }
}
