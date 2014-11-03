package com.vibejukebox.jukebox;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


public class JukeboxUserPlaylistActivity extends Activity {

    private static final String TAG = JukeboxUserPlaylistActivity.class.getSimpleName();
    private final boolean DEBUG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jukebox_user_playlist);

        List<String> tempList = new ArrayList<String>();
        tempList.add("House");
        tempList.add("Rock");
        tempList.add("Pachanga");

        displayUserPlaylists(tempList);

    }

    private void displayUserPlaylists(List<String> userPlaylists)
    {
        if(DEBUG)
            Log.d(TAG, "displayUserPlayLists -- ");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, userPlaylists);

        ListView playlistsView  = (ListView)findViewById(R.id.playlistsView);
        playlistsView.setAdapter(adapter);

        playlistsView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "position is:   " + String.valueOf(position));



            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.jukebox_user_playlist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
