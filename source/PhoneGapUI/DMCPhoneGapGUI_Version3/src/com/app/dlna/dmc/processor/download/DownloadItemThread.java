package com.app.dlna.dmc.processor.download;

import java.io.File;

import org.teleal.cling.support.model.DIDLObject;

import android.app.NotificationManager;
import android.content.Context;

public class DownloadItemThread extends DownloadThread {
	public DownloadItemThread(DIDLObject item, File parrent, DownloadListener listener, int downloadID, Context context) {
		m_name = item.getTitle();
		m_url = item.getResources().get(0).getValue();
		m_maxsize = item.getResources().get(0).getSize();
		m_parent = parrent;
		m_listener = listener;
		m_downloadID = downloadID;
		m_context = context;
		m_notificationManager = (NotificationManager) m_context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
}
