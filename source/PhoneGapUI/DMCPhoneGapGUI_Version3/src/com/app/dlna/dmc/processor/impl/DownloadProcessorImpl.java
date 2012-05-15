package com.app.dlna.dmc.processor.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.model.DIDLObject;

import android.app.Activity;
import android.widget.Toast;

import com.app.dlna.dmc.processor.download.DownloadItemThread;
import com.app.dlna.dmc.processor.download.DownloadThread;
import com.app.dlna.dmc.processor.download.DownloadThread.DownloadListener;
import com.app.dlna.dmc.processor.download.DownloadURLThread;
import com.app.dlna.dmc.processor.interfaces.DownloadProcessor;

public class DownloadProcessorImpl implements DownloadProcessor {

	private Activity m_activity;
	private File m_sdRoot;
	private List<DownloadThread> m_listDownloads;
	private DownloadListener m_downloadListener = new DownloadListener() {

		@Override
		public void onDownloadFail(final DownloadThread downloadThread, final Exception ex) {
			m_activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(m_activity,
							"Download failed, file: " + downloadThread.getItemName() + ", error = " + ex.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onDownloadComplete(final DownloadThread downloadThread) {

			m_activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(m_activity, "Download complete, file: " + downloadThread.getItemName(), Toast.LENGTH_SHORT)
							.show();
				}
			});
		}
	};

	// private NotificationManager m_notificationManager;

	public DownloadProcessorImpl(Activity activity) {
		m_activity = activity;
		// TODO: must change app_name here
		m_sdRoot = new File("/mnt/sdcard/DMCProject");
		m_sdRoot.mkdir();
		m_listDownloads = new ArrayList<DownloadThread>();
		// m_notificationManager = (NotificationManager)
		// activity.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public int startDownload(DIDLObject item) {
		int size;
		synchronized (m_listDownloads) {
			size = m_listDownloads.size();
		}

		DownloadItemThread downloadThread = new DownloadItemThread(item, m_sdRoot, m_downloadListener, size + 1, m_activity);
		synchronized (m_listDownloads) {
			m_listDownloads.add(downloadThread);
			downloadThread.startDownload();
		}
		return 0;
	}

	@Override
	public void stopDownload(int id) {

	}

	@Override
	public void stopAllDownloads() {
		synchronized (m_listDownloads) {
			for (DownloadThread downloadThread : m_listDownloads) {
				if (downloadThread != null && !downloadThread.isInterrupted())
					downloadThread.stopDownload();
			}
		}
	}

	@Override
	public int startDownload(String name, String url) {
		int size;
		synchronized (m_listDownloads) {
			size = m_listDownloads.size();
		}

		DownloadURLThread downloadThread = new DownloadURLThread(name, url, m_sdRoot, m_downloadListener, size + 1, m_activity);
		synchronized (m_listDownloads) {
			m_listDownloads.add(downloadThread);
			downloadThread.startDownload();
		}
		return 0;
	}
}