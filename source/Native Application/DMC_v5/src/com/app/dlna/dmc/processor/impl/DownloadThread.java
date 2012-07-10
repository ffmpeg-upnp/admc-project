package com.app.dlna.dmc.processor.impl;

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
import android.util.Log;
import android.widget.RemoteViews;
import app.dlna.controller.v5.R;

import com.app.dlna.dmc.gui.activity.MainActivity;

public class DownloadThread extends Thread {
	private File m_parent;
	private DownloadListener m_listener;
	private int m_downloadID;
	private Context m_context;
	private static NotificationManager NOTIFICATION_MANAGER;
	private Notification m_notification;
	private boolean m_isRunning;
	private String m_name;
	private String m_url;
	private long m_maxsize;
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
		contentTypeMap.put("audio/wma", "wma");

		contentTypeMap.put("video/mpeg", "mpg");
		contentTypeMap.put("video/vnd.mpegurl", "m4u");
		contentTypeMap.put("video/x-msvideo", "avi");
		contentTypeMap.put("video/x-ms-wmv", "wmv");
		contentTypeMap.put("video/x-sgi-movie", "movie");
		contentTypeMap.put("video/x-flv", "flv");
		contentTypeMap.put("video/flv", "flv");
		contentTypeMap.put("video/mp4", "mp4");
		contentTypeMap.put("video/MP4V-ES", "mp4");

		contentTypeMap.put("image/bmp", "bmp");
		contentTypeMap.put("image/gif", "gif");
		contentTypeMap.put("image/jpeg", "jpeg");
		contentTypeMap.put("image/png", "png");
		contentTypeMap.put("image/x-icon", "ico");
		contentTypeMap.put("image/x-rgb", "rgb");
	}

	public DownloadThread(String name, String url, File parrent, DownloadListener listener, int downloadID, Context context) {
		m_name = name;
		m_url = url;
		m_context = context;
		m_maxsize = -1;
		m_parent = parrent;
		m_listener = listener;
		m_downloadID = downloadID;
		m_context = context;
		if (NOTIFICATION_MANAGER == null)
			NOTIFICATION_MANAGER = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void startDownload() {
		if (m_name == null) {
			m_notification = new Notification(android.R.drawable.ic_dialog_alert,
					m_context.getString(R.string.sorry_this_item_cannot_be_downloaded), System.currentTimeMillis());
			PendingIntent contentIntent = PendingIntent.getActivity(m_context, 0, new Intent(), 0);
			m_notification.setLatestEventInfo(m_context, m_context.getString(R.string.download_fail),
					m_context.getString(R.string.sorry_this_item_cannot_be_downloaded), contentIntent);
			NOTIFICATION_MANAGER.notify(0, m_notification);
			if (m_listener != null)
				m_listener.onDownloadFail(this, new RuntimeException(m_context.getString(R.string.item_cannot_be_downloaded)));
		} else {
			m_isRunning = true;
			m_notification = new Notification(android.R.drawable.ic_menu_save, m_context.getString(R.string.download_file), System.currentTimeMillis());
			PendingIntent contentIntent = PendingIntent.getActivity(m_context, 0, new Intent(), 0);
			m_notification.setLatestEventInfo(m_context, m_context.getString(R.string.download_content), m_context.getString(R.string.downloading_content), contentIntent);
			m_notification.flags = Notification.FLAG_NO_CLEAR;
			RemoteViews contentView = new RemoteViews(m_context.getPackageName(), R.layout.download_notification);
			contentView.setTextViewText(R.id.contentName, m_name);
			m_notification.contentView = contentView;
			Intent cancelIntent = new Intent(m_context, MainActivity.class);
			cancelIntent.setAction(MainActivity.ACTION_CANCEL_DOWNLOAD + "_" + m_downloadID);
			Log.i("DownloadThread", "new download id = " + m_downloadID);
			PendingIntent cancelPendingIntent = PendingIntent.getActivity(m_context, 0, cancelIntent, 0);
			contentView.setOnClickPendingIntent(R.id.cancel, cancelPendingIntent);
			if (m_maxsize > 0) {
				contentView.setProgressBar(R.id.downloadProgress, 0, 0, false);
			} else {
				contentView.setProgressBar(R.id.downloadProgress, 0, 0, true);
			}
			NOTIFICATION_MANAGER.notify(m_downloadID, m_notification);
			this.start();
		}
	}

	public void stopDownload() {
		Log.i("DownloadThread", "Stop requested");
		m_isRunning = false;
		this.interrupt();
		NOTIFICATION_MANAGER.cancel(m_downloadID);
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
						NOTIFICATION_MANAGER.notify(m_downloadID, m_notification);
					}
				} else {
					m_notification.contentView.setProgressBar(R.id.downloadProgress, 0, 0, true);
				}

			}
			if (size == m_maxsize) {
				if (m_listener != null && size == m_maxsize) {
					m_listener.onDownloadComplete(this);
				}
				Notification notification = new Notification(android.R.drawable.ic_menu_save, m_context.getString(R.string.download_complete),
						System.currentTimeMillis());
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				PendingIntent contentIntent = PendingIntent.getActivity(m_context, 0, new Intent(), 0);
				notification.setLatestEventInfo(m_context, m_context.getString(R.string.download_complete), newFile.getAbsolutePath(), contentIntent);
				NOTIFICATION_MANAGER.cancel(m_downloadID);
				NOTIFICATION_MANAGER.notify(m_downloadID + 5000, notification);
			} else {
				NOTIFICATION_MANAGER.cancel(m_downloadID);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			if (m_listener != null)
				m_listener.onDownloadFail(this, ex);
			Notification notification = new Notification(android.R.drawable.ic_menu_save,
					m_context.getString(R.string.download_fail), System.currentTimeMillis());
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			PendingIntent contentIntent = PendingIntent.getActivity(m_context, 0, new Intent(), 0);
			notification.setLatestEventInfo(m_context, m_context.getString(R.string.download_fail), m_context.getString(R.string.cannot_download_item_)
					+ m_name, contentIntent);
			NOTIFICATION_MANAGER.cancel(m_downloadID);
			NOTIFICATION_MANAGER.notify(m_downloadID + 5000, notification);
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
	}

	public String getItemName() {
		return m_name;
	}

	public int getDownloadId() {
		return m_downloadID;
	}
}
