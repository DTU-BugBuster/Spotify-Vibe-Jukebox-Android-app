package com.vibejukebox.jukebox.models;

import retrofit.mime.TypedString;

/**
 * Created by Sergex on 6/29/15.
 */
public class SongTyped extends TypedString {
    public SongTyped(String string) {
        super(string);
    }

    @Override
    public String mimeType() {
        return "application/json";
    }
}
