package com.app.dlna.dmc.phonegap.plugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.utility.Utility;
import com.phonegap.api.PhonegapActivity;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

//updateCurrentItem
public class PlaylistPlugin extends Plugin {
	public static final String ACTION_UPDATE_CURRENT_ITEM = "updateCurrentItem";
	public static final String ACTION_ITEM_CLICK = "itemClick";
	private static final String ACTION_NEXT = "next";
	private static final String ACTION_PREV = "prev";
	private static final String ACTION_PLAY = "play";
	private static final String ACTION_PAUSE = "pause";
	private static final String ACTION_SET_VOLUME = "setVolume";
	private static final String ACTION_SEEK = "seek";
	private static final String TAG = PlaylistPlugin.class.getName();

	public PlaylistPlugin() {
	}

	public PlaylistPlugin(PhonegapActivity ctx) {
		super.setContext(ctx);
	}

	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		if (ACTION_UPDATE_CURRENT_ITEM.equals(action)) {
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			sendJavascript("setCurrentPlaylistItemTitle('" + playlistProcessor.getCurrentItem().getTitle() + "');");
		} else if (ACTION_ITEM_CLICK.equals(action)) {
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			int objectID = 0;
			try {
				objectID = data.getInt(0);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			playlistProcessor.setCurrentItem(objectID);
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (null == dmrProcessor) {
				MainActivity.INSTANCE.showLongToast("You must select a Renderer to play this content");
			} else {
				PlaylistItem item = playlistProcessor.getCurrentItem();
				sendJavascript("setCurrentPlaylistItemTitle('" + item.getTitle() + "');");
				dmrProcessor.setURIandPlay(item);
			}
		} else if (ACTION_NEXT.equals(action)) {
			doNext();
		} else if (ACTION_PREV.equals(action)) {
			doPrev();
		} else if (ACTION_PLAY.equals(action)) {
			doPlay();
		} else if (ACTION_PAUSE.equals(action)) {
			doPause();
		} else if (ACTION_SEEK.equals(action)) {
			try {
				doSeek(data.getInt(0));
			} catch (JSONException e) {
				return new PluginResult(Status.JSON_EXCEPTION);
			}
		} else if (ACTION_SET_VOLUME.equals(action)) {
			try {
				doSetVolume(data.getInt(0));
			} catch (JSONException e) {
				return new PluginResult(Status.JSON_EXCEPTION);
			}
		}
		return null;
	}

	private void doSetVolume(int value) {
		Log.e(TAG, "SetVolume: value = " + value);
		if (MainActivity.UPNP_PROCESSOR.getDMRProcessor() != null) {
			MainActivity.UPNP_PROCESSOR.getDMRProcessor().setVolume(value);
		}
	}

	private void doSeek(int seekTo) {
		Log.i(TAG, "Seek");
		if (MainActivity.UPNP_PROCESSOR.getDMRProcessor() != null) {
			MainActivity.UPNP_PROCESSOR.getDMRProcessor().seek(Utility.getTimeString(seekTo));
		}
	}

	private void doPause() {
		Log.i(TAG, "Pause");
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (dmrProcessor != null) {
			dmrProcessor.pause();
		}
	}

	private void doPlay() {
		Log.i(TAG, "Play");
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (dmrProcessor != null) {
			dmrProcessor.play();
		}
	}

	private void doPrev() {
		Log.i(TAG, "Prev");
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (playlistProcessor != null && dmrProcessor != null) {
			playlistProcessor.previous();
			PlaylistItem item = playlistProcessor.getCurrentItem();
			dmrProcessor.setURIandPlay(item);
			sendJavascript("setCurrentPlaylistItemTitle('" + item.getTitle() + "');");
			dmrProcessor.setURIandPlay(item);
		}
	}

	private void doNext() {
		Log.i(TAG, "Next");
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (playlistProcessor != null && dmrProcessor != null) {
			playlistProcessor.next();
			PlaylistItem item = playlistProcessor.getCurrentItem();
			dmrProcessor.setURIandPlay(item);
			sendJavascript("setCurrentPlaylistItemTitle('" + item.getTitle() + "');");
			dmrProcessor.setURIandPlay(item);
		}
	}

	public void updateCurrentItem(PlaylistItem item) {
		if (item != null)
			sendJavascript("setCurrentPlaylistItemTitle('" + item.getTitle() + "');");
	}

}
