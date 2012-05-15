package com.app.dlna.dmc.processor.interfaces;

import java.util.List;

import com.app.dlna.dmc.processor.youtube.YoutubeItem;

public interface YoutubeProcessor {

	void executeQueryAsync(String query, IYoutubeProcessorListener callback);

	void getDirectLinkAsync(YoutubeItem item, IYoutubeProcessorListener callback);

	void registURLAsync(String link, IYoutubeProcessorListener callback);
	
	String getDirectLink(String id);

	public interface IYoutubeProcessorListener {
		void onStartPorcess();

		void onGetDirectLinkComplete(YoutubeItem result);

		void onFail(Exception ex);

		void onSearchComplete(List<YoutubeItem> result);
	}
}
