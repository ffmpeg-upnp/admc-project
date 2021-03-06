package com.app.dlna.dmc.processor.impl;

import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.item.AudioItem;
import org.teleal.cling.support.model.item.VideoItem;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.Playlist;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistItem.Type;
import com.app.dlna.dmc.processor.playlist.PlaylistManager;

public class PlaylistProcessorImpl implements PlaylistProcessor {
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
		return m_playlistItems.size() >= m_maxSize ? true : false;
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
			if (m_playlistItems.size() >= m_maxSize)
				return null;
			if (m_playlistItems.contains(item))
				return item;
			m_playlistItems.add(item);
			PlaylistManager.createPlaylistItem(item, m_data.getId());
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
				m_playlistItems.remove(item);
				long id = item.getId();
				PlaylistManager.deletePlaylistItem(id);
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
		item.setUrl(object.getResources().get(0).getValue());
		if (object instanceof AudioItem) {
			item.setType(Type.AUDIO);
		} else if (object instanceof VideoItem) {
			item.setType(Type.VIDEO);
		} else {
			item.setType(Type.IMAGE);
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

}