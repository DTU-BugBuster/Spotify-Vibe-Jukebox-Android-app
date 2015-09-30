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
import java.util.List;

/**
 * Created by Sergex on 7/3/15.
 */
public class DrawerListAdapter extends BaseAdapter
{
    private static final String TAG = DrawerListAdapter.class.getSimpleName();
    private Context mContext;
    private List<DrawerItem> mDrawerItems;
    private boolean isActiveJukebox = true;

    public DrawerListAdapter(Context context, List<DrawerItem> drawerItems)
    {
        mContext = context;
        mDrawerItems = new ArrayList<>(drawerItems);
    }

    public void setJukeboxStatus(boolean isActive)
    {
        isActiveJukebox = isActive;
    }

    @Override
    public int getCount()
    {
        return mDrawerItems.size();
    }

    @Override
    public Object getItem(int position)
    {
        return mDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position)
    {
        //Log.d(TAG, "isEnabled (Drawer) position:  " + position);
        if(position == 0 && !isActiveJukebox){
            return false;
        } else {
            return true;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.drawer_item, parent, false);
        } else {
            view = convertView;
        }

        //Action added to side drawer pane
        TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText( mDrawerItems.get(position).mTitle);
        if(position == 0 && !isActiveJukebox){
            titleView.setTextColor(mContext.getResources().getColor(R.color.vibe_blue_grey));
        }

        //Corresponding image
        ImageView iconView = (ImageView) view.findViewById(R.id.drawerItemIcon);
        iconView.setImageResource(mDrawerItems.get(position).mIcon);

        return view;
    }
}
