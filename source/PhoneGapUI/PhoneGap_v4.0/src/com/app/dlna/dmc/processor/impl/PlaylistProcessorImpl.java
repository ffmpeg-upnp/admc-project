package com.app.dlna.dmc.processor.impl;

import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.model.DIDLObject;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistItem.Type;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;

public class PlaylistProcessorImpl implements PlaylistProcessor {
	private List<PlaylistItem> m_playlistItems;
	private int m_currentItemIdx;
	private List<PlaylistListener> m_listeners;
	private String m_containerId = "";

	public PlaylistProcessorImpl() {
		m_playlistItems = new ArrayList<PlaylistItem>();
		m_listeners = new ArrayList<PlaylistProcessor.PlaylistListener>();
	}

	@Override
	public boolean isFull() {
		return false;
	}

	@Override
	public void next() {
		if (m_playlistItems.size() == 0)
			return;
		++m_currentItemIdx;
		if (m_currentItemIdx >= m_playlistItems.size())
			m_currentItemIdx = 0;
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
		if (m_currentItemIdx < 0)
			m_currentItemIdx = m_playlistItems.size() - 1;
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
		if (m_currentItemIdx == -1) {
			return null;
		}

		if (m_playlistItems.size() > 0 && m_currentItemIdx < m_playlistItems.size()) {
			return m_playlistItems.get(m_currentItemIdx);
		}
		return null;
	}

	@Override
	public int setCurrentItem(int idx) {
		if (0 <= idx && idx < m_playlistItems.size())
			return m_currentItemIdx = idx;
		return -1;
	}

	@Override
	public int setCurrentItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			return m_currentItemIdx = m_playlistItems.indexOf(item);
		}
	}

	@Override
	public PlaylistItem addItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			if (m_playlistItems.contains(item))
				return item;
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
		return addItem(PlaylistItem.createFromDLDIObject(object));
	}

	@Override
	public PlaylistItem removeDIDLObject(DIDLObject object) {
		return removeItem(PlaylistItem.createFromDLDIObject(object));
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
	public String getContainerId() {
		return m_containerId;
	}

	@Override
	public void setContainerId(String id) {
		m_containerId = id;
	}

}