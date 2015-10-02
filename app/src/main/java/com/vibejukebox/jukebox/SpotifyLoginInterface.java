package com.vibejukebox.jukebox;

/**
 * Created by Sergex on 10/2/15.
 */
public interface SpotifyLoginInterface {

    void loginSpotify();

    void checkSpotifyProduct();

    void storeAccessToken(String accessToken);
}
