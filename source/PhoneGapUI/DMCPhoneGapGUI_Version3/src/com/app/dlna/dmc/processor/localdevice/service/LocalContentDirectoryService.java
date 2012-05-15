package com.app.dlna.dmc.processor.localdevice.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teleal.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.teleal.cling.support.contentdirectory.ContentDirectoryException;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.model.container.StorageFolder;
import org.teleal.cling.support.model.item.ImageItem;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.cling.support.model.item.VideoItem;
import org.teleal.common.util.MimeType;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.app.dlna.dmc.gui.ResourceManager;
import com.app.dlna.dmc.processor.http.HTTPServerData;
import com.app.dlna.dmc.utility.Utility;

public class LocalContentDirectoryService extends AbstractContentDirectoryService {
	private static final String TAG = "LocalContentDirectoryService";
	private static final int SCANNING_NOTIFICATION = 37000;
	private static NotificationManager m_notificationManager;
	private static List<MusicTrack> m_listMusic = null;
	private static List<VideoItem> m_listVideo = null;
	private static List<ImageItem> m_listPhoto = null;
	private static List<String> m_musicMap = null;
	private static List<String> m_videoMap = null;
	private static List<String> m_photoMap = null;
	private static Map<String, String> m_mineMap = null;
	private static boolean IS_SCANNING;

	public static void scanMedia(final Context context) {
		IS_SCANNING = true;
		m_notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		final Notification notification = new Notification(ResourceManager.getScanningIcon(),
				"Scanning content on sdcard", System.currentTimeMillis());

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);

		notification.setLatestEventInfo(context, "Scan content on sdcard", "Scanning", contentIntent);
		notification.flags = Notification.FLAG_NO_CLEAR;

		// TODO: more MIME-Type adding here
		m_mineMap = new HashMap<String, String>();
		m_mineMap.put("flv", "video/x-flv");

		m_listMusic = new ArrayList<MusicTrack>();
		m_listVideo = new ArrayList<VideoItem>();
		m_listPhoto = new ArrayList<ImageItem>();

		m_musicMap = new ArrayList<String>();
		m_videoMap = new ArrayList<String>();
		m_photoMap = new ArrayList<String>();

		m_musicMap.add("mp3");
		m_musicMap.add("wma");
		m_musicMap.add("wav");
		m_musicMap.add("mid");
		m_musicMap.add("midi");

		m_videoMap.add("mp4");
		m_videoMap.add("flv");
		m_videoMap.add("mpg");
		m_videoMap.add("avi");
		m_videoMap.add("mkv");
		m_videoMap.add("m4v");

		m_photoMap.add("jpg");
		m_photoMap.add("jpeg");
		m_photoMap.add("png");
		m_photoMap.add("bmp");
		m_photoMap.add("gif");

		Log.e(TAG, "Start media scanning");
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (HTTPServerData.HOST == null) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try {
					m_notificationManager.notify(SCANNING_NOTIFICATION, notification);
					scanFile(Environment.getExternalStorageDirectory().getAbsolutePath());
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					m_notificationManager.cancel(SCANNING_NOTIFICATION);
					IS_SCANNING = false;
				}
			}
		}).start();
	}

	public static boolean isScanning() {
		return IS_SCANNING;
	}

	public static void removeAllContent() {
		synchronized (m_listMusic) {
			m_listMusic.clear();
		}
		synchronized (m_listPhoto) {
			m_listPhoto.clear();
		}
		synchronized (m_listVideo) {
			m_listVideo.clear();
		}
	}

	// private Thread m_scaningThread = new Thread(new Runnable() {
	//
	// @Override
	// public void run() {
	// try {
	// scanFile(Environment.getExternalStorageDirectory().getAbsolutePath());
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// }
	// }
	// });

	private static void scanFile(String path) {
		try {
			File file = new File(path);
			for (File subFile : file.listFiles()) {
				if (subFile.isDirectory()) {
					scanFile(subFile.getAbsolutePath());
				} else if (subFile.length() >= 51200) {
					String fileName = subFile.getName();
					int dotPos = subFile.getName().lastIndexOf(".");
					String fileExtension = dotPos != -1 ? fileName.substring(dotPos) : null;
					if (fileExtension != null) {
						fileExtension = fileExtension.toLowerCase().replace(".", "");
					}
					String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
					if (mimeType == null) {
						mimeType = m_mineMap.get(fileExtension);
					}

					if (mimeType != null) {
						Res res = new Res(new MimeType(mimeType.split("/")[0], mimeType.split("/")[1]),
								subFile.length(), Utility.createLink(subFile));
						if (fileExtension != null) {
							if (m_musicMap.contains(fileExtension)) {
								MusicTrack musicTrack = new MusicTrack("0/1/" + subFile.getName(), "0/1",
										subFile.getName(), "local dms", "", "", res);
								m_listMusic.add(musicTrack);
							}

							if (m_videoMap.contains(fileExtension)) {
								VideoItem videoItem = new VideoItem("0/2/" + subFile.getName(), "0/2",
										subFile.getName(), "local dms", res);
								m_listVideo.add(new VideoItem(videoItem));
							}
							if (m_photoMap.contains(fileExtension)) {
								ImageItem imageItem = new ImageItem("0/3/" + subFile.getName(), "0/3",
										subFile.getName(), "local dms", res);
								m_listPhoto.add(new ImageItem(imageItem));
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult,
			long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {
		Log.d(TAG, "Browse " + objectID);
		BrowseResult br = null;
		int count = 0;
		try {
			final DIDLContent content = new DIDLContent();
			if (objectID.equals("0")) {
				content.addContainer(new StorageFolder("0/1", "0", "Music", "Android Local DMS", m_listMusic.size(), 0l));
				content.addContainer(new StorageFolder("0/2", "0", "Video", "Android Local DMS", m_listVideo.size(), 0l));
				content.addContainer(new StorageFolder("0/3", "0", "Photo", "Android Local DMS", m_listPhoto.size(), 0l));
				count = 3;
			}
			if (objectID.equals("0/1"))
				count = getResultContent(firstResult, maxResults, content, m_listMusic);
			if (objectID.equals("0/2"))
				count = getResultContent(firstResult, maxResults, content, m_listVideo);
			if (objectID.equals("0/3"))
				count = getResultContent(firstResult, maxResults, content, m_listPhoto);
			DIDLParser parser = new DIDLParser();
			br = new BrowseResult(parser.generate(content), count, count);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return br;
	}

	private int getResultContent(long firstResult, long maxResults, final DIDLContent content,
			List<? extends DIDLObject> sourceList) {
		int count;
		synchronized (sourceList) {
			int toIndex;
			if (maxResults == 0)
				toIndex = sourceList.size();
			else
				toIndex = (firstResult + maxResults) < (sourceList.size() - 1) ? (int) (firstResult + maxResults)
						: (sourceList.size() - 1);
			for (DIDLObject didlObject : sourceList.subList((int) firstResult, toIndex)) {
				if (didlObject != null) {
					content.addItem((Item) didlObject);
				}
			}
			count = sourceList.size();
		}
		return count;
	}
}
