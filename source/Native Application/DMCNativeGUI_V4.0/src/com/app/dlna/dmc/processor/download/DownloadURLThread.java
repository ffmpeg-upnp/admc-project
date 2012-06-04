package com.app.dlna.dmc.processor.download;

import java.io.File;

import android.app.NotificationManager;
import android.content.Context;

public class DownloadURLThread extends DownloadThread {
	public DownloadURLThread(String name, String url, File parrent, DownloadListener listener, int downloadID, Context context) {
		super(context);
		m_name = name;
		m_url = url;
		m_maxsize = -1;
		m_parent = parrent;
		m_listener = listener;
		m_downloadID = downloadID;
		m_context = context;
	}

}
