package com.vibejukebox.jukebox.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
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

import com.spotify.sdk.android.authentication.AuthenticationResponse;
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

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MusicParameterActivty extends AppCompatActivity
{
    private static final String TAG = MusicParameterActivty.class.getSimpleName();
    private static final boolean DEBUG = DebugLog.DEBUG;

    private static final int VIBE_GET_GENERATED_PLAYLIST = 1;

    /** Parameter Sliders */
    private double mEnergyValue;
    private double mAcousticValue;
    private double mDanceValue;

    private TextView mEnergyValueText;
    private TextView mAcousticValueText;
    private TextView mDanceValueText;

    private String mPlaylistName;

    /**
     * Lists holding the list of saved songs when creating playlists from favorites
     */
    List<String> mTrackUris;
    List<Track> mTracks;

    /**
     * Spotify Api Auth response
     */
    private AuthenticationResponse mAuthResponse;

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
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_parameter_activty);

        Toolbar toolbar = (Toolbar)findViewById(R.id.mytoolbarParam);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        //Initialize Parameter Sliders
        initUi();

        //Initialize all needed values for playlist creation
        initValues();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(!mIsArtistRadio){
            mTrackUris = new ArrayList<>();
            getSavedTracks();
        }
    }

    private void initValues()
    {
        Intent intent = getIntent();
        mIsArtistRadio = intent.getBooleanExtra(Vibe.VIBE_JUKEBOX_ARTIST_RADIO, false);
        mAuthResponse = intent.getParcelableExtra(Vibe.VIBE_JUKEBOX_SPOTIFY_AUTHRESPONSE);
        mPlaylistName = getIntent().getStringExtra(Vibe.VIBE_JUKEBOX_PLAYLIST_NAME);
    }

    private void initUi()
    {
        mEnergyValueText = (TextView)findViewById(R.id.EnergyValueView);
        mAcousticValueText = (TextView)findViewById(R.id.AcousticValueView);
        mDanceValueText = (TextView)findViewById(R.id.DanceValueView);

        SeekBar energySlider = (SeekBar)findViewById(R.id.EnergyBar);
        energySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEnergyValue = progress*0.01;
                mEnergyValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mEnergyValue = utilFormatDecimal(mEnergyValue);
                Log.d(TAG, "--> " + mEnergyValue);
            }
        });

        SeekBar acousticnessSlider = (SeekBar)findViewById(R.id.AcousticnessBar);
        acousticnessSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mAcousticValue = progress*0.01;
                mAcousticValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mAcousticValue = utilFormatDecimal(mAcousticValue);
                Log.d(TAG, "--> " + mAcousticValue);
            }
        });

        SeekBar danceabilitySlider = (SeekBar)findViewById(R.id.DanceabilityBar);
        danceabilitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mDanceValue = progress*0.01;
                mDanceValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDanceValue = utilFormatDecimal(mDanceValue);
                Log.d(TAG, "--> " + mDanceValue);
            }
        });

    }

    private double utilFormatDecimal(double value)
    {
        DecimalFormat df = new DecimalFormat(("#.##"));
        String val = df.format(value);
        return Double.valueOf(val);
    }

    /**
     * Called when the user presses the Launch button after setting parameters
     * @param view
     */
    public void launch(View view)
    {
        Log.d(TAG, " Start Playlist Launch ");
        if(mIsArtistRadio)
            requestArtistRadio();
        else
            requestVibedPlaylist();
    }

    private void startCreatedPlaylist()
    {
        if(mPlaylistName != null && !mPlaylistName.equals(""))
            mPlaylistName = "Vibed Playlist";

        Intent intent = new Intent(this, VibeService.class);
        intent.putExtra("authresponse", mAuthResponse);
        intent.putExtra("playlistname", mPlaylistName);
        intent.putStringArrayListExtra(Vibe.VIBE_JUKEBOX_TRACK_URI_QUEUE, (ArrayList<String>) mTrackUris);
        intent.putParcelableArrayListExtra(Vibe.VIBE_JUKEBOX_TRACKS_IN_QUEUE, (ArrayList<Track>) mTracks);

        startService(intent);
    }

    private void requestArtistRadio()
    {
        if(DEBUG)
            Log.d(TAG, "requestArtistRadio -- ");

        Params params = new Params();
        params.setAcousticness(mAcousticValue);
        params.setEnergy(mEnergyValue);
        params.setDanceability(mDanceValue);

        //TODO: Input an Artist with Spotify URI
        List<String> artistRadio = new ArrayList<>();
        artistRadio.add("ARTOA5I1187B9A4CEF");

        //Launch a dialog to show user backend process is ongoing
        launchProcessingDialog();

        //Track Uris collected in getSavedTracks() function
        SongTask songTask = new SongTask(true, params, artistRadio);
        SongBackendApi.getInstance().getBackendService().requestTracksFromfavorites(songTask, new Callback<SongResponse>() {
            @Override
            public void success(SongResponse songResponse, Response response) {
                Log.d(TAG, "Successful call to Artist radio backend service. ");
                mProgressDialog.cancel();
                parseBackendResponse(songResponse);
            }

            @Override
            public void failure(RetrofitError error) {
                mProgressDialog.cancel();
                Log.e(TAG, "Failed call to Artist radio backend playlist service.");
                Log.e(TAG, "Message -- > " + error.getMessage());
                error.printStackTrace();
            }
        });
    }

    private void requestVibedPlaylist()
    {
        Log.d(TAG, "requestRetroPlaylist -- " + mAcousticValue + " " + mEnergyValue + " " + mDanceValue);

        //TODO: TEST TRACKS
        /*String[] testSongs = {
                "spotify:track:0NtO8R2saehkOjdlPrRq0V",
                "spotify:track:6747MQ9ojYX37enpeHN7DG",
                "spotify:track:7lpIOIdQzePKF8m4EM0BMR"
        };

        List<String> songArr = new ArrayList<>();
        for(String song : testSongs){
            songArr.add(song);
        }*/

        Params params = new Params();
        params.setAcousticness(mAcousticValue);
        params.setEnergy(mEnergyValue);
        params.setDanceability(mDanceValue);

        //Launch a dialog to show user backend process is ongoing
        launchProcessingDialog();

        //Track Uris collected in getSavedTracks() function
        SongTask songTask = new SongTask(false, params, mTrackUris);
        SongBackendApi.getInstance().getBackendService().requestTracksFromfavorites(songTask, new Callback<SongResponse>() {
            @Override
            public void success(SongResponse songResponse, Response response) {
                Log.d(TAG, "Successful call to backend service. ");
                mProgressDialog.cancel();
                parseBackendResponse(songResponse);
            }

            @Override
            public void failure(RetrofitError error) {
                mProgressDialog.cancel();
                Log.e(TAG, "Failed call to backend playlist service.");
                Log.e(TAG, "Message -- > " + error.getMessage());
                error.printStackTrace();
            }
        });
    }

    private void parseBackendResponse(SongResponse response) {
        Log.d(TAG, "parseBackendResponse");
        mTrackUris = new ArrayList<>(response.getPlaylist());
        getTrackObjects(mTrackUris);
    }

    /**
     * Get Spotify user saved tracks to generate playlist
     */
    private void getSavedTracks()
    {
        Log.d(TAG, "getSavedTracks");

        final List<Track> savedTracks = new ArrayList<>();
        SpotifyApi api = new SpotifyApi();

        if(mAuthResponse == null){
            Log.e(TAG, "Error: Authentication is null.");
            return;
        }

        //Sets the limit on the number of returned songs
        Map<String, Object> options = new HashMap<>();
        options.put("limit",50);

        api.setAccessToken(mAuthResponse.getAccessToken());
        SpotifyService spotify = api.getService();

        //Get a User’s Saved Tracks Api endpoint
        spotify.getMySavedTracks(options, new Callback<Pager<SavedTrack>>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                Log.d(TAG, "SUCCESS");
                String trackName;
                String artist;

                for(SavedTrack savedTrack : savedTrackPager.items){
                    trackName = savedTrack.track.name;
                    artist = savedTrack.track.artists.get(0).name;
                    Track track = new Track();
                    track.setArtistName(artist);
                    track.setTrackName(trackName);

                    Log.d(TAG, "Artist: " + artist + "   Track: " + trackName);
                    savedTracks.add(track);
                    mTrackUris.add(savedTrack.track.uri);
                }

                //mTracks = new ArrayList<>(savedTracks);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "FAILURE -- > " + error.getMessage());
                error.printStackTrace();
            }
        });
    }

    /**
     * TODO: CHECK IF WORKING
     * @param trackURIs
     */
    private void getTrackObjects(List<String> trackURIs)
    {
        String trackUriString = SpotifyClient.getTrackIds(trackURIs);

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        final List<Track> trackList = new ArrayList<>();

        //Get Several tracks API point
        spotify.getTracks(trackUriString, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                Log.d(TAG, "Successful call to get Several tracks from Uri list");

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
    private void launchProcessingDialog()
    {
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
