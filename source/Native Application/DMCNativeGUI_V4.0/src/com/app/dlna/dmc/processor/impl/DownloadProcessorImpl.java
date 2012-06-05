package com.app.dlna.dmc.processor.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.model.DIDLObject;

import android.app.Activity;
import android.widget.Toast;

import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.download.DownloadThread;
import com.app.dlna.dmc.processor.download.DownloadThread.DownloadListener;
import com.app.dlna.dmc.processor.interfaces.DownloadProcessor;
import com.app.dlna.dmc.processor.interfaces.YoutubeProcessor.IYoutubeProcessorListener;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;

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
					Toast.makeText(m_activity, "Download complete, file: " + downloadThread.getItemName(),
							Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

	public DownloadProcessorImpl(Activity activity) {
		m_activity = activity;
		// TODO: must change app_name here
		m_sdRoot = new File("/mnt/sdcard/Media2Share");
		m_sdRoot.mkdir();
		m_listDownloads = new ArrayList<DownloadThread>();
	}

	@Override
	public int startDownload(DIDLObject item) {
		int size;
		synchronized (m_listDownloads) {
			size = m_listDownloads.size();
		}
		DownloadThread downloadThread = new DownloadThread(item.getTitle(), item.getResources().get(0).getValue(),
				m_sdRoot, m_downloadListener, ++size, m_activity);

		synchronized (m_listDownloads) {
			m_listDownloads.add(downloadThread);
			downloadThread.startDownload();
		}
		return size;
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

		DownloadThread downloadThread = new DownloadThread(name, url, m_sdRoot, m_downloadListener, size + 1,
				m_activity);
		synchronized (m_listDownloads) {
			m_listDownloads.add(downloadThread);
			downloadThread.startDownload();
		}
		return size;
	}

	@Override
	public int startDownload(final YoutubeItem item) {
		new YoutubeProcessorImpl().getDirectLinkAsync(item, new IYoutubeProcessorListener() {
			@Override
			public void onStartPorcess() {
				MainActivity.INSTANCE.showLoadingMessage("Contacting to youtube server");
			}

			@Override
			public void onSearchComplete(List<YoutubeItem> result) {

			}

			@Override
			public void onGetLinkComplete(YoutubeItem result) {
				if (MainActivity.INSTANCE.dissmissLoadingMessage()) {
					startDownload(item.getTitle(), result.getDirectLink());
					MainActivity.INSTANCE.showToast("Starting download...");
				} else {
					MainActivity.INSTANCE.showToast("Action canceled...");
				}
			}

			@Override
			public void onFail(Exception ex) {
				MainActivity.INSTANCE.showToast("Download file failed. Try again later.");
			}
		});
		return 0;
	}

	@Override
	public int startDownload(PlaylistItem item) {
		switch (item.getType()) {
		case YOUTUBE:
			new YoutubeProcessorImpl().getDirectLinkAsync(new YoutubeItem(item.getUrl()),
					new IYoutubeProcessorListener() {

						@Override
						public void onStartPorcess() {
							MainActivity.INSTANCE.showLoadingMessage("Contacting to youtube server");
						}

						@Override
						public void onSearchComplete(List<YoutubeItem> result) {
						}

						@Override
						public void onGetLinkComplete(YoutubeItem result) {
							if (MainActivity.INSTANCE.dissmissLoadingMessage()) {
								startDownload(result.getTitle(), result.getDirectLink());
								MainActivity.INSTANCE.showToast("Starting download...");
							} else {
								MainActivity.INSTANCE.showToast("Action canceled...");
							}
						}

						@Override
						public void onFail(Exception ex) {
							MainActivity.INSTANCE.showToast("Download file failed. Try again later.");
						}
					});
			return 0;
		default:
			return startDownload(item.getTitle(), item.getUrl());
		}
	}
}