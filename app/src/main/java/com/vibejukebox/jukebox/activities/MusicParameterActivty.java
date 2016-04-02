package com.vibejukebox.jukebox.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vibejukebox.jukebox.DebugLog;
import com.vibejukebox.jukebox.R;
import com.vibejukebox.jukebox.SpotifyClient;
import com.vibejukebox.jukebox.Track;
import com.vibejukebox.jukebox.Vibe;
import com.vibejukebox.jukebox.service.VibeService;
import com.vibejukebox.jukebox.models.Params;
import com.vibejukebox.jukebox.models.SongResponse;
import com.vibejukebox.jukebox.models.SongTask;
import com.vibejukebox.jukebox.webservice.SongBackendApi;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MusicParameterActivty extends AppCompatActivity {

    private static final String TAG = MusicParameterActivty.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final int VIBE_GET_GENERATED_PLAYLIST = 1;

    private static final int VIBE_USER_SAVED_TRACKS = 2;

    private static final int VIBE_SERVER_ERROR = -1;

    private static final String VIBE_JUKEBOX_PREFERENCES = "JukeboxPreferences";

    private static final String VIBE_JUKEBOX_ACCESS_TOKEN_PREF = "AccessToken";

    /** Parameter Sliders */
    private double mEnergyValue;
    private double mAcousticValue;
    private double mDanceValue;

    private boolean mEnergyTouched = false;
    private boolean mAcousticnessTouched = false;
    private boolean mDanceTouched = false;

    @Bind(R.id.EnergyValueView) TextView mEnergyValueText;
    @Bind(R.id.AcousticValueView) TextView mAcousticValueText;
    @Bind(R.id.DanceValueView) TextView mDanceValueText;

    private String mPlaylistName;
    private String mArtistName;

    /**
     * Lists holding the list of saved songs when creating playlists from favorites
     */
    List<String> mTrackUris;
    List<Track> mTracks;

    /**
     * Boolean to create playlist based on favorites or an artist
     */
    private boolean mIsArtistRadio = false;

    private ProgressDialog mProgressDialog;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case VIBE_GET_GENERATED_PLAYLIST:
                    startCreatedPlaylist();
                    return true;
                case VIBE_USER_SAVED_TRACKS:
                    requestVibedPlaylist();
                    return true;
                case VIBE_SERVER_ERROR:
                    showServerError();
                    return true;
            }
            return false;
        }
    });

    /**
     * Retrieves the stored Access token from Spotify Api
     * @return: Access token string
     */
    private String getAccessToken() {
        if(DEBUG) {
            Log.d(TAG, "getAccessToken -- ");
        }

        SharedPreferences preferences = getSharedPreferences(VIBE_JUKEBOX_PREFERENCES, MODE_PRIVATE);
        return preferences.getString(VIBE_JUKEBOX_ACCESS_TOKEN_PREF, null);
    }

    private void showServerError() {
        Toast.makeText(this, R.string.VIBE_APP_SERVER_ERROR, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_parameter_activty);

        Toolbar toolbar = (Toolbar)findViewById(R.id.mytoolbarParam);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        ButterKnife.bind(this);

        //Initialize Parameter Sliders
        initUi();

        //Initialize all needed values for playlist creation
        initValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mIsArtistRadio) {
            mTrackUris = new ArrayList<>();
            getSavedTracks();
        }
    }

    private void initValues() {
        Intent intent = getIntent();
        mIsArtistRadio = intent.getBooleanExtra(Vibe.VIBE_JUKEBOX_ARTIST_RADIO, false);
        //mAuthResponse = intent.getParcelableExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE);
        mPlaylistName = intent.getStringExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME);
        mArtistName = intent.getStringExtra(Vibe.VIBE_JUKEBOX_START_WITH_ARTIST);
    }

    private void initUi() {
        SeekBar energySlider = (SeekBar)findViewById(R.id.EnergyBar);
        if(energySlider != null) {
            energySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mEnergyValue = progress*0.01;
                    mEnergyValueText.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mEnergyTouched = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mEnergyValue = utilFormatDecimal(mEnergyValue);
                    Log.d(TAG, "--> " + mEnergyValue);
                }
            });
        }

        SeekBar acousticnessSlider = (SeekBar)findViewById(R.id.AcousticnessBar);
        if(acousticnessSlider != null) {
            acousticnessSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mAcousticValue = progress*0.01;
                    mAcousticValueText.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mAcousticnessTouched = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mAcousticValue = utilFormatDecimal(mAcousticValue);
                    Log.d(TAG, "--> " + mAcousticValue);
                }
            });
        }

        SeekBar danceabilitySlider = (SeekBar)findViewById(R.id.DanceabilityBar);
        if(danceabilitySlider != null) {
            danceabilitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mDanceValue = progress * 0.01;
                    mDanceValueText.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mDanceTouched = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mDanceValue = utilFormatDecimal(mDanceValue);
                    Log.d(TAG, "--> " + mDanceValue);
                }
            });
        }
    }

    private double utilFormatDecimal(double value) {
        DecimalFormat df = new DecimalFormat(("#.##"));
        String val = df.format(value);
        return Double.valueOf(val);
    }

    /**
     * Called when the user presses the Launch button after setting parameters
     * @param view: View
     */
    public void launch(View view) {
        if(mIsArtistRadio) {
            requestArtistRadio();
        } else {
            //getSavedTracks();
            requestVibedPlaylist();
        }
    }

    private void startCreatedPlaylist() {
        if(DEBUG) {
            Log.e(TAG, "startCreatedPlaylist (starting service)");
        }

        if(mPlaylistName == null || mPlaylistName.equals("")) {
            mPlaylistName = "Vibed Playlist";
        }

        Intent intent = new Intent(this, VibeService.class);
        //intent.putExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE, mAuthResponse);
        intent.putExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME, mPlaylistName);
        intent.putStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE, (ArrayList<String>) mTrackUris);
        intent.putParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE, (ArrayList<Track>) mTracks);

        startService(intent);
    }

    /**
     * Launch playlist generation based on a user chosen artist.
     */
    private void requestArtistRadio() {
        if(DEBUG) {
            Log.d(TAG, "requestArtistRadio with Uri -- " + mArtistName);
        }

        // If the user did not touch a parameter slider that parameter is not taken into
        //account for the playlist generation
        Params params = getParametersIfZero();

        // Artist sent as a list
        List<String> artistRadio = new ArrayList<>();
        artistRadio.add(mArtistName);

        //Launch a dialog to show user backend process is ongoing
        launchProcessingDialog();

        //Track Uris collected in getSavedTracks() function
        SongTask songTask = new SongTask(true, params, artistRadio);
        SongBackendApi.getInstance().getBackendService().requestTracksFromFavorites(songTask, new Callback<SongResponse>() {
            @Override
            public void success(SongResponse songResponse, Response response) {
                if(DEBUG) {
                    Log.d(TAG, "Successful call to Artist radio backend service. ");
                }

                mProgressDialog.cancel();
                parseBackendResponse(songResponse);
            }

            @Override
            public void failure(RetrofitError error) {
                mProgressDialog.cancel();
                Log.e(TAG, "Failed call to Artist radio backend playlist service.");
                Log.e(TAG, "Message -- > " + error.getMessage());
                error.printStackTrace();

                //Sets all parameters back to "nothing"
                resetSliderParameters();

                //Displays toast to user that a server error has occured
                mHandler.sendEmptyMessage(VIBE_SERVER_ERROR);
            }
        });
    }

    /** TODO: Backend dev not finished */
    private void requestVibedPlaylist() {
        if(DEBUG) {
            Log.d(TAG, "requestRetroPlaylist --> " + mAcousticValue + " " + mEnergyValue + " " + mDanceValue);
        }

        if(mTrackUris.size() == 0) {
            Toast.makeText(MusicParameterActivty.this, R.string.VIBE_APP_NO_SAVED_SONGS, Toast.LENGTH_LONG).show();
            return;
        }

        //Get Correct parameters from sliders
        Params params = getParametersIfZero();

        //Launch a dialog to show user backend process is ongoing
        launchProcessingDialog();

        //Track Uris collected in getSavedTracks() function
        SongTask songTask = new SongTask(false, false, params, mTrackUris);
        SongBackendApi.getInstance().getBackendService().requestTracksFromFavorites(songTask, new Callback<SongResponse>() {
            @Override
            public void success(SongResponse songResponse, Response response) {
                if(DEBUG)
                    Log.d(TAG, "Successful call to backend service (playlist generated based on saved tracks)");
                mProgressDialog.cancel();
                parseBackendResponse(songResponse);
            }

            @Override
            public void failure(RetrofitError error) {
                mProgressDialog.cancel();
                Log.e(TAG, "Failed generating playlist from user's saved tracks");
                Log.e(TAG, "Message -- > " + error.getMessage());
                error.printStackTrace();

                //Sets all parameters back to "nothing"
                resetSliderParameters();

                //Displays toast to user that a server error has occured
                mHandler.sendEmptyMessage(VIBE_SERVER_ERROR);
            }
        });
    }

    /**
     * Function sets each parameter to null if the slider has not been touched by the user
     * @return: Params object
     */
    private Params getParametersIfZero() {
        Params params = new Params();
        if(!mEnergyTouched) {
            params.setEnergy(null);
        } else {
            params.setEnergy(mEnergyValue);
        }

        if(!mAcousticnessTouched) {
            params.setAcousticness(null);
        } else {
            params.setAcousticness(mAcousticValue);
        }

        if(!mDanceTouched) {
            params.setDanceability(null);
        } else {
            params.setDanceability(mDanceValue);
        }

        return params;
    }

    private void resetSliderParameters() {
        SeekBar energySlider = (SeekBar)findViewById(R.id.EnergyBar);
        energySlider.setProgress(0);

        SeekBar acousticSlider = (SeekBar)findViewById(R.id.AcousticnessBar);
        acousticSlider.setProgress(0);

        SeekBar danceSlider = (SeekBar)findViewById(R.id.DanceabilityBar);
        danceSlider.setProgress(0);

        mEnergyTouched = false;
        mAcousticnessTouched = false;
        mDanceTouched = false;
    }

    /**
     * Function parses the json response from the backend
     * @param response: SongResponse object
     */
    private void parseBackendResponse(SongResponse response) {
        if(DEBUG) {
            Log.d(TAG, "parseBackendResponse");
        }

        mTrackUris = new ArrayList<>(response.getPlaylist());

        if(!mTrackUris.isEmpty()) {
            getTrackObjects(mTrackUris);
        } else {
            Toast.makeText(this, "No matches with those parameters, please try again", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Get Spotify user saved tracks to generate playlist
     */
    private void getSavedTracks() {
        if(DEBUG){
            Log.d(TAG, "getSavedTracks");
        }

        //final List<Track> savedTracks = new ArrayList<>();
        SpotifyApi api = new SpotifyApi();

        if(getAccessToken() == null) {
            Log.e(TAG, "Error: Authentication is null.");
            return;
        }

        //Sets the limit on the number of returned songs
        Map<String, Object> options = new HashMap<>();
        options.put("limit",50);

        api.setAccessToken(getAccessToken());
        SpotifyService spotify = api.getService();

        //Get a User’s Saved Tracks Api endpoint
        spotify.getMySavedTracks(options, new Callback<Pager<SavedTrack>>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                if(DEBUG) {
                    Log.d(TAG, "Success getting saved user's tracks");
                }

                String trackName;
                String artist;

                for (SavedTrack savedTrack : savedTrackPager.items) {
                    trackName = savedTrack.track.name;
                    artist = savedTrack.track.artists.get(0).name;
                    Track track = new Track();
                    track.setArtistName(artist);
                    track.setTrackName(trackName);

                    Log.d(TAG, "Artist: " + artist + "   Track: " + trackName);
                    //savedTracks.add(track);
                    mTrackUris.add(savedTrack.track.uri);
                    //mHandler.sendMessage(mHandler.obtainMessage(VIBE_USER_SAVED_TRACKS, mTrackUris));
                }

                //mTracks = new ArrayList<>(savedTracks);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed getting user's saved tracks. " + error.getMessage());
                error.printStackTrace();
            }
        });
    }

    /**
     * Function gets a list of Vibe Track objects from a list of Spotify URIs
     * @param trackURIs: List of Spotify uris
     */
    private void getTrackObjects(List<String> trackURIs) {
        String trackUriString = SpotifyClient.getTrackIds(trackURIs);
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        final List<Track> trackList = new ArrayList<>();

        //Get Several tracks API point
        spotify.getTracks(trackUriString, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                if(DEBUG) {
                    Log.d(TAG, "Successful call to get Several tracks from Uri list");
                }

                for (kaaes.spotify.webapi.android.models.Track track : tracks.tracks) {
                    Log.d(TAG, "Track name:  " + track.name);
                    Track vibeTrack = new Track(track.artists.get(0).name, track.name);
                    vibeTrack.setTrackName(track.name);
                    vibeTrack.setArtistName(track.artists.get(0).name);
                    trackList.add(vibeTrack);
                }

                mTracks = new ArrayList<>(trackList);
                mHandler.sendEmptyMessage(VIBE_GET_GENERATED_PLAYLIST);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occurred getting the list of track Ids from Uri list:  " + error.getMessage());
            }
        });
    }

    /**
     * Progress dialog to give user a visual feedback that the music playlist
     * is being created in the backend
     */
    private void launchProcessingDialog() {
        CharSequence Progress_title = "Creating playlist ...";
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(Progress_title);
        mProgressDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_music_parameter_activty, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*if(id == android.R.id.home){
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }*/

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}