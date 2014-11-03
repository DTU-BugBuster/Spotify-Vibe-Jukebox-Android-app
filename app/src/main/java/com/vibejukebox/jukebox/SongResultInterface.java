package com.vibejukebox.jukebox;

public interface SongResultInterface
{
	String getDisplayText();
	String getArtistName();
	
	boolean hasSeparator();
	boolean isPlayed();
}
