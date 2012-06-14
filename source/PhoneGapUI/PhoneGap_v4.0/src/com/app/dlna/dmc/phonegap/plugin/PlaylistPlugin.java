package com.app.dlna.dmc.phonegap.plugin;

import org.json.JSONArray;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.phonegap.api.PhonegapActivity;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;

//updateCurrentItem
public class PlaylistPlugin extends Plugin {
	public static final String ACTION_UPDATE_CURRENT_ITEM = "updateCurrentItem";

	public PlaylistPlugin() {
	}

	public PlaylistPlugin(PhonegapActivity ctx) {
		super.setContext(ctx);
	}

	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		if (ACTION_UPDATE_CURRENT_ITEM.equals(action)) {
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			String currentPlaylistName = "";
			if (null == playlistProcessor) {
				currentPlaylistName = "";
			} else {
				PlaylistItem item = playlistProcessor.getCurrentItem();
				if (null == item) {
					currentPlaylistName = "";
				} else {
					currentPlaylistName = item.getTitle();
				}
			}
			sendJavascript("setCurrentPlaylistItemTitle('" + currentPlaylistName + "');");
		}
		return null;
	}

}
