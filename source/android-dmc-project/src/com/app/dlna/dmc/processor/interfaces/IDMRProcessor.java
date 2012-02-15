package com.app.dlna.dmc.processor.interfaces;

import org.teleal.cling.model.meta.Action;

public interface IDMRProcessor {
	void setURI(String uri);

	void play();

	void pause();

	void stop();

	void seek(String position);

	void setVolume(int newVolume);
	
	int getVolume();

	void addListener(DMRProcessorListner listener);

	void removeListener(DMRProcessorListner listener);

	void dispose();

	public interface DMRProcessorListner {
		void onUpdatePosition(long current, long max);

		void onPaused();

		void onStoped();

		void onPlaying();

		@SuppressWarnings("rawtypes")
		void onActionFail(Action actionCallback, final String cause);
	}

}