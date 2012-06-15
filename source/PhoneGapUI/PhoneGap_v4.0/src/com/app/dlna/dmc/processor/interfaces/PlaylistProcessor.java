package com.app.dlna.dmc.processor.interfaces;

import java.util.List;

import org.teleal.cling.support.model.DIDLObject;

import com.app.dlna.dmc.processor.playlist.PlaylistItem;

public interface PlaylistProcessor {
	void next();

	void previous();

	void addListener(PlaylistListener listener);

	void removeListener(PlaylistListener listener);

	PlaylistItem getCurrentItem();

	PlaylistItem addItem(PlaylistItem item);

	PlaylistItem addDIDLObject(DIDLObject object);

	PlaylistItem removeItem(PlaylistItem item);

	PlaylistItem removeDIDLObject(DIDLObject object);

	PlaylistItem getItemAt(int idx);

	boolean containsUrl(String url);

	String getContainerId();

	void setContainerId(String id);

	boolean isFull();

	List<PlaylistItem> getAllItems();

	int setCurrentItem(int idx);

	int setCurrentItem(PlaylistItem item);

	public interface PlaylistListener {
		void onNext();

		void onPrev();

	}

	int getCurrentItemIndex();

}
