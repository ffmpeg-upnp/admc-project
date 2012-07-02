package com.app.dlna.dmc.processor.interfaces;

import java.util.List;

import com.app.dlna.dmc.processor.model.YoutubeItem;

public interface YoutubeProcessor {

	void executeQueryAsync(String query, IYoutubeProcessorListener callback);

	void getDirectLinkAsync(YoutubeItem item, IYoutubeProcessorListener callback);

	void registURLAsync(YoutubeItem item, IYoutubeProcessorListener callback);
	
	String getDirectLink(String id);

	public interface IYoutubeProcessorListener {
		void onStartPorcess();

		void onGetLinkComplete(YoutubeItem result);

		void onFail(Exception ex);

		void onSearchComplete(List<YoutubeItem> result);
	}
}
