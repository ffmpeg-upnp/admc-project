package com.app.dlna.dmc.processor.impl;

import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.item.AudioItem;
import org.teleal.cling.support.model.item.VideoItem;

import android.net.Uri;
import android.util.Log;

import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.http.HTTPServerData;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.Playlist;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistItem.Type;
import com.app.dlna.dmc.processor.playlist.PlaylistManager;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;

public class PlaylistProcessorImpl implements PlaylistProcessor {
	private static final String TAG = PlaylistProcessorImpl.class.getName();
	private List<PlaylistItem> m_playlistItems;
	private int m_currentItemIdx;
	private int m_maxSize;
	private Playlist m_data;
	private List<PlaylistListener> m_listeners;

	public PlaylistProcessorImpl(Playlist data, int maxItem) {
		m_playlistItems = new ArrayList<PlaylistItem>();
		m_currentItemIdx = data.getCurrentIdx();
		m_maxSize = maxItem;
		m_data = data;
		m_listeners = new ArrayList<PlaylistProcessor.PlaylistListener>();
	}

	@Override
	public Playlist getData() {
		m_data.setCurrentIdx(m_currentItemIdx);
		return m_data;
	}

	@Override
	public void setData(Playlist data) {
		m_data = data;
	}

	@Override
	public int getMaxSize() {
		return m_maxSize;
	}

	@Override
	public boolean isFull() {
		return m_playlistItems.size() >= m_maxSize;
	}

	@Override
	public void next() {
		if (m_playlistItems.size() == 0)
			return;
		m_currentItemIdx = (m_currentItemIdx + 1) % m_playlistItems.size();
		if (m_currentItemIdx >= m_playlistItems.size()) {
			m_currentItemIdx = 0;
		}
		fireOnNextEvent();
	}

	private void fireOnNextEvent() {
		synchronized (m_listeners) {
			for (PlaylistListener listener : m_listeners) {
				listener.onNext();
			}
		}
	}

	@Override
	public void previous() {
		if (m_playlistItems.size() == 0)
			return;
		m_currentItemIdx = (m_currentItemIdx - 1) % m_playlistItems.size();
		if (m_currentItemIdx < 0) {
			m_currentItemIdx = m_playlistItems.size() - 1;
		}
		fireOnPrevEvent();
	}

	private void fireOnPrevEvent() {
		synchronized (m_listeners) {
			for (PlaylistListener listener : m_listeners) {
				listener.onPrev();
			}
		}
	}

	@Override
	public PlaylistItem getCurrentItem() {
		if (m_currentItemIdx == -1 && m_playlistItems.size() > 0) {
			return m_playlistItems.get(m_currentItemIdx = 0);
		}
		if (m_playlistItems.size() > 0 && m_currentItemIdx < m_playlistItems.size()) {
			return m_playlistItems.get(m_currentItemIdx);
		}
		return null;
	}

	@Override
	public int setCurrentItem(int idx) {
		if (0 <= idx && idx < m_playlistItems.size()) {
			m_currentItemIdx = idx;
			return m_currentItemIdx;
		}
		return -1;
	}

	@Override
	public int setCurrentItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			if (m_playlistItems.contains(item)) {
				m_currentItemIdx = m_playlistItems.indexOf(item);
				return m_currentItemIdx;
			}
			return -1;
		}
	}

	@Override
	public PlaylistItem addItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			if (m_playlistItems.contains(item))
				return item;
			if (m_playlistItems.size() >= m_maxSize) {
				// remove last item
				PlaylistItem lastItem = m_playlistItems.get(m_playlistItems.size() - 1);
				PlaylistManager.deletePlaylistItem(lastItem.getId());
				m_playlistItems.remove(lastItem);
			}
			PlaylistManager.createPlaylistItem(item, m_data.getId());
			m_playlistItems.add(item);
			if (m_playlistItems.size() == 1) {
				m_currentItemIdx = 0;
			}
			return item;
		}
	}

	@Override
	public PlaylistItem removeItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			int itemIdx = -1;
			if ((itemIdx = m_playlistItems.indexOf(item)) >= 0) {
				long id = m_playlistItems.get(itemIdx).getId();
				PlaylistManager.deletePlaylistItem(id);
				m_playlistItems.remove(item);
				DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
				if (dmrProcessor != null && dmrProcessor.getCurrentTrackURI().equals(item.getUrl())) {
					dmrProcessor.stop();
				}
				if (itemIdx == m_currentItemIdx)
					m_currentItemIdx = 0;
				return item;
			}
			return null;
		}
	}

	@Override
	public List<PlaylistItem> getAllItems() {
		return m_playlistItems;
	}

	@Override
	public boolean containsUrl(String url) {
		List<String> listUrl = new ArrayList<String>();
		for (PlaylistItem item : m_playlistItems) {
			listUrl.add(item.getUrl());
		}
		return listUrl.contains(url);
	}

	@Override
	public PlaylistItem addDIDLObject(DIDLObject object) {
		return addItem(createPlaylistItem(object));
	}

	@Override
	public PlaylistItem removeDIDLObject(DIDLObject object) {
		return removeItem(createPlaylistItem(object));
	}

	private PlaylistItem createPlaylistItem(DIDLObject object) {
		PlaylistItem item = new PlaylistItem();
		item.setTitle(object.getTitle());
		Uri uri = Uri.parse(object.getResources().get(0).getValue());
		boolean isLocal = uri.getHost().equals(HTTPServerData.HOST) && uri.getPort() == HTTPServerData.PORT;
		item.setUrl(object.getResources().get(0).getValue());
		Log.i(TAG, "PlaylistItem url = " + object.getResources().get(0).getValue());
		if (object instanceof AudioItem) {
			if (isLocal) {
				item.setType(Type.AUDIO_LOCAL);
			} else
				item.setType(Type.AUDIO_REMOTE);
		} else if (object instanceof VideoItem) {
			if (isLocal) {
				item.setType(Type.VIDEO_LOCAL);
			} else
				item.setType(Type.VIDEO_REMOTE);
		} else {
			if (isLocal) {
				item.setType(Type.IMAGE_LOCAL);
			} else
				item.setType(Type.IMAGE_REMOTE);
		}
		return item;
	}

	@Override
	public PlaylistItem getItemAt(int idx) {
		return m_playlistItems.get(idx);
	}

	@Override
	public void addListener(PlaylistListener listener) {
		synchronized (m_listeners) {
			if (!m_listeners.contains(listener))
				m_listeners.add(listener);
		}
	}

	@Override
	public void removeListener(PlaylistListener listener) {
		synchronized (m_listeners) {
			if (!m_listeners.contains(listener))
				m_listeners.add(listener);
		}
	}

	@Override
	public void saveState() {
		PlaylistManager.savePlaylistState(getData());
	}

	@Override
	public int getCurrentItemIndex() {
		return m_currentItemIdx;
	}

	@Override
	public PlaylistItem addYoutubeItem(YoutubeItem result) {
		return addItem(createPlaylistItem(result));
	}

	private PlaylistItem createPlaylistItem(YoutubeItem object) {
		PlaylistItem item = new PlaylistItem();
		item.setTitle(object.getTitle());
		item.setUrl(object.getId());
		item.setType(Type.YOUTUBE);
		return item;
	}

	@Override
	public void updateItemList() {
		synchronized (m_playlistItems) {
			m_playlistItems = PlaylistManager.getAllPlaylistItem(getData().getId());
		}
	}

}