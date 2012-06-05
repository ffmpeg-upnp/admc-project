package com.app.dlna.dmc.processor.interfaces;

import org.teleal.cling.support.model.DIDLObject;

import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;

public interface DownloadProcessor {
	int startDownload(DIDLObject item);

	int startDownload(String name, String url);

	int startDownload(YoutubeItem item);

	int startDownload(PlaylistItem item);

	void stopDownload(int id);

	void stopAllDownloads();
}
