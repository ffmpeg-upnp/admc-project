package com.app.dlna.dmc.processor.interfaces;

import java.util.List;
import java.util.Map;

import org.teleal.cling.support.model.DIDLObject;

import com.app.dlna.dmc.processor.model.Playlist;

public interface DMSProcessor {
	static final String ACTION_REMOVE = "Remove";
	static final String ACTION_ADD = "Add";

	void browse(String objectID, int pageIndex, DMSProcessorListner listener);

	void nextPage(DMSProcessorListner listener);

	void previousPage(DMSProcessorListner listener);

	void back(DMSProcessorListner listener);

	void dispose();

	DIDLObject getDIDLObject(String objectID);

	void addCurrentItemsToPlaylist(PlaylistProcessor playlistProcessor, DMSAddRemoveContainerListener actionListener);

	void addAllToPlaylist(Playlist playlist, DMSAddRemoveContainerListener actionListener);

	void addAllToPlaylist(Playlist playlist, String containerID, DMSAddRemoveContainerListener actionListener);

	void removeCurrentItemsFromPlaylist(PlaylistProcessor playlistProcessor,
			DMSAddRemoveContainerListener actionListener);

	void removeAllFromPlaylist(Playlist playlist, DMSAddRemoveContainerListener actionListener);

	void removeAllFromPlaylist(Playlist playlist, String containerID, DMSAddRemoveContainerListener actionListener);

	List<DIDLObject> getAllObjects();

	public interface DMSProcessorListner {
		void onBrowseComplete(String objectID, boolean haveNext, boolean havePrev,
				Map<String, List<? extends DIDLObject>> result);

		void onBrowseFail(String message);
	}

	public interface DMSAddRemoveContainerListener {
		void onActionComplete(String message);

		void onActionFail(Exception ex);

		void onActionStart(String action);
	}
}
