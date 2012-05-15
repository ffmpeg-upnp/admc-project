package com.app.dlna.dmc.processor.interfaces;

import org.teleal.cling.support.model.DIDLObject;

public interface DownloadProcessor {
	int startDownload(DIDLObject item);

	int startDownload(String name, String url);

	void stopDownload(int id);

	void stopAllDownloads();
}
