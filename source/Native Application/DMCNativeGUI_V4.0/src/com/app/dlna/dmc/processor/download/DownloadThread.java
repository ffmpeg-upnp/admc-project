package com.app.dlna.dmc.processor.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import app.dlna.controller.v4.R;

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
	public static HashMap<String, String> contentTypeMap;
	static {
		contentTypeMap = new HashMap<String, String>();
		contentTypeMap.put("audio/mpeg", "mp3");
		contentTypeMap.put("audio/basic", "au");
		contentTypeMap.put("audio/midi", "mid");
		contentTypeMap.put("audio/x-aiff", "aif");
		contentTypeMap.put("audio/x-mpegurl", "m3u");
		contentTypeMap.put("audio/x-pn-realaudio", "ra");
		contentTypeMap.put("audio/x-wav", "wav");
		contentTypeMap.put("audio/x-ms-wma", "wma");

		contentTypeMap.put("video/mpeg", "mpg");
		contentTypeMap.put("video/vnd.mpegurl", "m4u");
		contentTypeMap.put("video/x-msvideo", "avi");
		contentTypeMap.put("video/x-ms-wmv", "wmv");
		contentTypeMap.put("video/x-sgi-movie", "movie");
		contentTypeMap.put("video/x-flv", "flv");
		contentTypeMap.put("video/mp4", "mp4");
		contentTypeMap.put("video/MP4V-ES", "mp4");

		contentTypeMap.put("image/bmp", "bmp");
		contentTypeMap.put("image/gif", "gif");
		contentTypeMap.put("image/jpeg", "jpeg");
		contentTypeMap.put("image/png", "png");
		contentTypeMap.put("image/x-icon", "ico");
		contentTypeMap.put("image/x-rgb", "rgb");
	}

	protected DownloadThread(Context context) {
		m_context = context;
		m_notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void startDownload() {
		if (m_name == null) {
			m_notification = new Notification(android.R.drawable.ic_dialog_alert, "Sorry, this item cannot be downloaded",
					System.currentTimeMillis());
			PendingIntent contentIntent = PendingIntent.getActivity(m_context, 0, new Intent(), 0);
			m_notification.setLatestEventInfo(m_context, "Download fail", "Sorry, this item cannot be downloaded", contentIntent);
			m_notificationManager.notify(0, m_notification);
			if (m_listener != null)
				m_listener.onDownloadFail(this, new RuntimeException("Item cannot be downloaded"));
		} else {
			m_isRunning = true;
			m_notification = new Notification(android.R.drawable.ic_menu_save, "Download file", System.currentTimeMillis());
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
			connection = (HttpURLConnection) new URL(m_url).openConnection();
			connection.setConnectTimeout(3000);
			connection.setRequestMethod("GET");
			connection.connect();
			String filename = m_name;
			String contentType = connection.getContentType();
			String ext = contentTypeMap.get(contentType);
			if (ext != null)
				filename += "." + ext;
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
						m_notification.contentView.setTextViewText(R.id.downloaded, percent + " % ");
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
