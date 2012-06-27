package com.app.dlna.dmc.processor.localdevice.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teleal.cling.model.types.csv.CSV;
import org.teleal.cling.model.types.csv.CSVString;
import org.teleal.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.teleal.cling.support.contentdirectory.ContentDirectoryException;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.ProtocolInfo;
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
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.gui.resource.ResourceManager;
import com.app.dlna.dmc.processor.http.HTTPRequestHandler;
import com.app.dlna.dmc.processor.http.HTTPServerData;
import com.app.dlna.dmc.utility.Utility;

public class LocalContentDirectoryService extends AbstractContentDirectoryService {
	private static final int SCANNING_NOTIFICATION = 37000;
	private static final String TAG = LocalContentDirectoryService.class.getName();
	private static NotificationManager m_notificationManager;
	private static List<MusicTrack> m_listMusic = null;
	private static List<VideoItem> m_listVideo = null;
	private static List<ImageItem> m_listPhoto = null;
	private static List<String> m_musicMap = null;
	private static List<String> m_videoMap = null;
	private static List<String> m_imageMap = null;
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

		m_mineMap = new HashMap<String, String>();
		m_mineMap.put("flv", "video/x-flv");

		m_listMusic = new ArrayList<MusicTrack>();
		m_listVideo = new ArrayList<VideoItem>();
		m_listPhoto = new ArrayList<ImageItem>();

		m_musicMap = new ArrayList<String>();
		m_videoMap = new ArrayList<String>();
		m_imageMap = new ArrayList<String>();

		for (String music_ext : AppPreference.getMusicExtension())
			if (!music_ext.trim().isEmpty())
				m_musicMap.add(music_ext);
		for (String video_ext : AppPreference.getVideoExtension())
			if (!video_ext.trim().isEmpty())
				m_videoMap.add(video_ext);
		for (String image_ext : AppPreference.getImageExtension())
			if (!image_ext.trim().isEmpty())
				m_imageMap.add(image_ext);

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

	private static void scanFile(String path) {
		try {
			File file = new File(path);
			for (File subFile : file.listFiles()) {
				if (subFile.isDirectory()) {
					scanFile(subFile.getAbsolutePath());
				} else if (subFile.length() >= AppPreference.getMinSize()) {
					insertFileToLibrary(subFile);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static DIDLObject insertFileToLibrary(File subFile) {
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
			if (fileExtension != null) {
				if (m_musicMap.contains(fileExtension)) {
					MusicTrack musicTrack = new MusicTrack("0/1/" + subFile.getName(), "0/1", subFile.getName(),
							AppPreference.getLocalServerName(), "", "", new Res(new ProtocolInfo("http-get:*:"
									+ mimeType + ":" + HTTPRequestHandler.getDLNAHeaderValue(mimeType)),
									subFile.length(), Utility.createLink(subFile)));
					m_listMusic.add(musicTrack);
					return musicTrack;
				} else if (m_videoMap.contains(fileExtension)) {
					VideoItem videoItem = new VideoItem("0/2/" + subFile.getName(), "0/2", subFile.getName(),
							AppPreference.getLocalServerName(), new Res(new ProtocolInfo("http-get:*:" + mimeType + ":"
									+ HTTPRequestHandler.getDLNAHeaderValue(mimeType)), subFile.length(),
									Utility.createLink(subFile)));
					m_listVideo.add(new VideoItem(videoItem));
					return videoItem;
				} else if (m_imageMap.contains(fileExtension)) {
					ImageItem imageItem = new ImageItem("0/3/" + subFile.getName(), "0/3", subFile.getName(),
							AppPreference.getLocalServerName(), new Res(new ProtocolInfo("http-get:*:" + mimeType + ":"
									+ HTTPRequestHandler.getDLNAHeaderValue(mimeType)), subFile.length(),
									Utility.createLink(subFile)));
					m_listPhoto.add(new ImageItem(imageItem));
					return imageItem;
				}
			}
		}
		return null;
	}

	@Override
	public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult,
			long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {
		BrowseResult br = null;
		int[] resCount = null;
		try {
			final DIDLContent content = new DIDLContent();
			if (objectID.equals("0")) {
				content.addContainer(new StorageFolder("0/1", "0", "Music", AppPreference.getLocalServerName(),
						m_listMusic.size(), 0l));
				content.addContainer(new StorageFolder("0/2", "0", "Video", AppPreference.getLocalServerName(),
						m_listVideo.size(), 0l));
				content.addContainer(new StorageFolder("0/3", "0", "Photo", AppPreference.getLocalServerName(),
						m_listPhoto.size(), 0l));
				resCount = new int[] { 3, 3 };
			} else if (objectID.equals("0/1"))
				resCount = getResultContent(firstResult, maxResults, content, m_listMusic);
			else if (objectID.equals("0/2"))
				resCount = getResultContent(firstResult, maxResults, content, m_listVideo);
			else if (objectID.equals("0/3"))
				resCount = getResultContent(firstResult, maxResults, content, m_listPhoto);
			DIDLParser parser = new DIDLParser();
			br = new BrowseResult(parser.generate(content), resCount[0], resCount[1]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return br;
	}


	@Override
	public CSV<String> getSearchCapabilities() {
		CSVString result = new CSVString("*");
		return result;
	}

	@Override
	public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult, long maxResults,
			SortCriterion[] orderBy) throws ContentDirectoryException {
		Log.e(TAG, "containerID = " + containerId + ";search = " + searchCriteria + "; filter = " + filter + "; firstResult = "
				+ firstResult + "; maxresult = " + maxResults);
		return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
	}

	private int[] getResultContent(long firstResult, long maxResults, final DIDLContent content,
			List<? extends DIDLObject> sourceList) {
		int[] res = new int[2];

		Log.e(TAG, "first result =" + firstResult + "; max result = " + maxResults);
		synchronized (sourceList) {
			int toIndex;
			if (maxResults == 0)
				toIndex = sourceList.size();
			else
				toIndex = ((firstResult + maxResults) < (sourceList.size()) ? (int) (firstResult + maxResults)
						: (sourceList.size()));
			for (DIDLObject didlObject : sourceList.subList((int) firstResult, toIndex)) {
				if (didlObject != null) {
					content.addItem((Item) didlObject);
					res[0]++;
				}
			}
			res[1] = sourceList.size();
		}
		return res;
	}

	public static DIDLObject getDIDLObjectFromPath(String path) {
		DIDLObject result = null;
		if (path.startsWith("/sdcard"))
			path = "/mnt/sdcard" + path.substring(7);
		String uriToPlay = Utility.createLink(path);
		for (MusicTrack music : m_listMusic) {
			if (music.getResources().get(0).getValue().equals(uriToPlay)) {
				result = music;
			}
		}
		if (null == result)
			for (ImageItem image : m_listPhoto) {
				if (image.getResources().get(0).getValue().equals(uriToPlay)) {
					result = image;
				}
			}
		if (null == result)
			for (VideoItem video : m_listVideo) {
				if (video.getResources().get(0).getValue().equals(uriToPlay)) {
					result = video;
				}
			}
		if (null == result) {
			result = insertFileToLibrary(new File(path));
		}
		return result;
	}
}
