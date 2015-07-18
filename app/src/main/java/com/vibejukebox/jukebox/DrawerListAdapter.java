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
 * Created by Sergex on 7/3/15.
 */
public class DrawerListAdapter extends BaseAdapter
{
    private Context mContext;
    private List<DrawerItem> mDrawerItems;

    public DrawerListAdapter(Context context, List<DrawerItem> drawerItems)
    {
        mContext = context;
        mDrawerItems = new ArrayList<>(drawerItems);
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
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.drawer_item, null);
        } else {
            view = convertView;
        }

        //Action added to side drawer pane
        TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText( mDrawerItems.get(position).mTitle);

        //Corresponding image
        ImageView iconView = (ImageView) view.findViewById(R.id.drawerItemIcon);
        iconView.setImageResource(mDrawerItems.get(position).mIcon);

        return view;
    }
}