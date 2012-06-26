package com.app.dlna.dmc.processor.interfaces;

import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;

import com.app.dlna.dmc.processor.playlist.PlaylistItem;

public interface DMRProcessor {

	void setURIandPlay(PlaylistItem item);

	void play();

	void pause();

	void stop();

	void seek(String position);

	void setVolume(int newVolume);

	int getVolume();

	int getMaxVolume();

	void addListener(DMRProcessorListner listener);

	void removeListener(DMRProcessorListner listener);

	void dispose();

	String getName();

	void setPlaylistProcessor(PlaylistProcessor playlistProcessor);

	String getCurrentTrackURI();

	void setRunning(boolean running);
	
	PlaylistItem getCurrentItem();

	public interface DMRProcessorListner {
		void onUpdatePosition(long current, long max);

		void onPaused();

		void onStoped();

		void onPlaying();

		void onErrorEvent(String error);
		
		void onCheckURLStart();
		
		void onCheckURLEnd();

		@SuppressWarnings("rawtypes")
		void onActionFail(Action actionCallback, UpnpResponse response, final String cause);
	}

}
