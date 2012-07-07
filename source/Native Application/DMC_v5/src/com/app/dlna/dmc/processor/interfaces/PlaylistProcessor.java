package com.app.dlna.dmc.processor.interfaces;

import java.util.List;

import org.teleal.cling.support.model.DIDLObject;

import com.app.dlna.dmc.processor.model.Playlist;
import com.app.dlna.dmc.processor.model.PlaylistItem;
import com.app.dlna.dmc.processor.model.YoutubeItem;

public interface PlaylistProcessor {
	void next();

	void previous();

	void addListener(PlaylistListener listener);

	void removeListener(PlaylistListener listener);

	PlaylistItem getCurrentItem();

	PlaylistItem addItem(PlaylistItem item);

	PlaylistItem addDIDLObject(DIDLObject object);

	PlaylistItem addYoutubeItem(YoutubeItem result);

	PlaylistItem removeItem(PlaylistItem item);

	PlaylistItem removeDIDLObject(DIDLObject object);

	PlaylistItem getItemAt(int idx);

	boolean containsUrl(String url);

	int getMaxSize();

	boolean isFull();

	List<PlaylistItem> getAllItems();

	int setCurrentItem(int idx);

	int setCurrentItem(PlaylistItem item);

	Playlist getData();

	void setData(Playlist data);

	public interface PlaylistListener {
		void onItemChanged(PlaylistItem item, ChangeMode changeMode);
	}

	void saveState();

	int getCurrentItemIndex();

	void updateItemList();

	List<PlaylistItem> getAllItemsByViewMode();

	void updateForViewMode();

	public enum ChangeMode {
		NEXT, PREV, UNKNOW
	}

}
