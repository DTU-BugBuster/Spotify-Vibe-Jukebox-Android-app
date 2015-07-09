package com.vibejukebox.jukebox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.vibejukebox.jukebox.activities.StartingPlaylistActivity;


public class LogInPremiumSpotify extends Activity
{
	private final static String TAG = LogInPremiumSpotify.class.getSimpleName();
	private final static boolean DEBUG = false;
    private static final String CREATED_OBJECT_ID = "createdObjectId";
    private String mId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_in_premium_spotify);

        if(DEBUG)
            Log.d(TAG, "onCreate -- ");

        mId = getIntent().getStringExtra(CREATED_OBJECT_ID);
	}

    @Override
    protected void onStop() {
        super.onStop();

        if(DEBUG)
            Log.d(TAG, "onStop -- ");

        finish();
    }

    public void logInSpotify(View view) {
        if (DEBUG)
            Log.d(TAG, "logInSpotify Service-- ");

        Intent intent  = new Intent(this, StartingPlaylistActivity.class);
        intent.putExtra(CREATED_OBJECT_ID, mId);
        startActivity(intent);
    }
}