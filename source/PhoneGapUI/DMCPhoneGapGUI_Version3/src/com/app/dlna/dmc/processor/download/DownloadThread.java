package com.app.dlna.dmc.processor.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.app.dlna.dmc.R;

public class DownloadThread extends Thread {
	protected File m_parent;
	protected DownloadListener m_listener;
	protected int m_downloadID;
	protected Context m_context;
	protected NotificationManager m_notificationManager;
	protected Notification m_notification;
	protected boolean m_isRunning;
	protected String m_name;
	protected String m_url;
	protected long m_maxsize;

	protected DownloadThread() {

	}

	public void startDownload() {
		m_isRunning = true;
		m_notification = new Notification(R.drawable.ic_download, "Downloading content", System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(m_context, 0, new Intent(), 0);
		m_notification.setLatestEventInfo(m_context, "Download content", "Downloading content", contentIntent);
		m_notification.flags = Notification.FLAG_NO_CLEAR;
		RemoteViews contentView = new RemoteViews(m_context.getPackageName(), R.layout.download_notification);
		contentView.setTextViewText(R.id.contentName, m_name);
		m_notification.contentView = contentView;
		if (m_maxsize > 0) {
			contentView.setProgressBar(R.id.downloadProgress, 0, 0, false);
		} else {
			contentView.setProgressBar(R.id.downloadProgress, 0, 0, true);
		}
		m_notificationManager.notify(m_downloadID, m_notification);
		this.start();
	}

	public void stopDownload() {
		m_isRunning = false;
		this.interrupt();
		m_notificationManager.cancel(m_downloadID);
	}

	public interface DownloadListener {
		public void onDownloadComplete(DownloadThread downloadThread);

		public void onDownloadFail(DownloadThread downloadThread, Exception ex);
	}

	@Override
	public void run() {
		super.run();
		OutputStream os = null;
		InputStream is = null;
		HttpURLConnection connection = null;
		try {
			URL u = new URL(m_url);
			connection = (HttpURLConnection) u.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.connect();
			String contentType = "";
			String filename = m_name;
			if (contentType != null) {
				if (contentType.equals("audio/mpeg")) {
					filename += ".mp3";
				}
			}
			is = connection.getInputStream();
			File newFile = new File(m_parent, filename);
			os = new FileOutputStream(newFile);
			byte[] buffer = new byte[204800];
			int read = 0;
			long size = 0;
			long second = System.currentTimeMillis();
			m_maxsize = connection.getContentLength();
			while ((read = is.read(buffer)) > 0 && m_isRunning) {
				size += read;
				os.write(buffer, 0, read);
				os.flush();
				if (m_maxsize > 0) {
					long newSecond = System.currentTimeMillis();
					if (Math.abs(newSecond - second) > 1000) {
						int percent = (int) (size * 100 / m_maxsize);
						second = newSecond;
						m_notification.contentView.setProgressBar(R.id.downloadProgress, 100, percent, false);
						m_notification.contentView.setTextViewText(R.id.downloaded, size + "/" + m_maxsize);
						m_notificationManager.notify(m_downloadID, m_notification);
					}
				} else {
					m_notification.contentView.setProgressBar(R.id.downloadProgress, 0, 0, true);
				}

			}
			if (size == m_maxsize) {
				m_listener.onDownloadComplete(this);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			if (m_listener != null)
				m_listener.onDownloadFail(this, ex);
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (connection != null)
				connection.disconnect();
		}
		m_notificationManager.cancel(m_downloadID);
	}

	public String getItemName() {
		return m_name;
	}
}
