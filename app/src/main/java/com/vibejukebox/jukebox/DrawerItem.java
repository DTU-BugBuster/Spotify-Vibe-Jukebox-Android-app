package com.vibejukebox.jukebox;

/**
 * Created by Sergex on 7/3/15.
 */
public class DrawerItem
{
    String mTitle;
    int mIcon;

    public DrawerItem(String title, int icon)
    {
        mTitle = title;
        mIcon = icon;
    }

    public String getTitle(){
        return mTitle;
    }
}
