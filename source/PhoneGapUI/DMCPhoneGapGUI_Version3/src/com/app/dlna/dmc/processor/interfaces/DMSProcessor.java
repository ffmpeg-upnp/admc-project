package com.app.dlna.dmc.processor.interfaces;

import java.util.List;
import java.util.Map;

import org.teleal.cling.support.model.DIDLObject;

public interface DMSProcessor {
	void browse(String objectID, int pageIndex, DMSProcessorListner listener);

	void nextPage(DMSProcessorListner listener);

	void previousPage(DMSProcessorListner listener);

	void back(DMSProcessorListner listener);

	void dispose();

	DIDLObject getDIDLObject(String objectID);

	void addCurrentItemsToPlaylist(PlaylistProcessor playlistProcessor, DMSAddRemoveContainerListener actionListener);

	void addAllToPlaylist(PlaylistProcessor playlistProcessor, DMSAddRemoveContainerListener actionListener);

	void removeCurrentItemsFromPlaylist(PlaylistProcessor playlistProcessor,
			DMSAddRemoveContainerListener actionListener);

	void removeAllFromPlaylist(PlaylistProcessor playlistProcessor, DMSAddRemoveContainerListener actionListener);

	List<DIDLObject> getAllObjects();

	public interface DMSProcessorListner {
		void onBrowseComplete(String objectID, boolean haveNext, boolean havePrev,
				Map<String, List<? extends DIDLObject>> result);

		void onBrowseFail(String message);
	}

	public interface DMSAddRemoveContainerListener {
		void onActionComplete();

		void onActionFail(Exception ex);

		void onActionStart();
	}
}
