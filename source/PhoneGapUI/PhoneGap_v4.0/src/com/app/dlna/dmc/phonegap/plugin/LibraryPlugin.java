package com.app.dlna.dmc.phonegap.plugin;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.ImageItem;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.cling.support.model.item.VideoItem;

import android.util.Log;
import android.widget.Toast;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor.DMSAddRemoveContainerListener;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor.DMSProcessorListner;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.phonegap.api.PhonegapActivity;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class LibraryPlugin extends Plugin {
	private static final String TAG = LibraryPlugin.class.getName();
	public static final String ACTION_BROWSE = "browse";
	public static final String ACTION_BACK = "back";
	public static final String ACTION_LOADMORE = "loadMore";
	public static final String ACTION_ADDTOPLAYLIST = "addToPlaylist";
	private static boolean LOADMORE = false;
	private static boolean HAVENEXT = false;

	public LibraryPlugin() {
	}

	public LibraryPlugin(PhonegapActivity ctx) {
		super.setContext(ctx);
	}

	@Override
	public PluginResult execute(String action, final JSONArray data, String callbackId) {
		if (ACTION_BROWSE.equals(action)) {
			if (MainActivity.UPNP_PROCESSOR == null) {
				return new PluginResult(Status.ERROR, "Cannot get UPNP Processor");
			} else {
				try {
					DMSProcessor dmsProcessor = MainActivity.UPNP_PROCESSOR.getDMSProcessor();
					LOADMORE = false;
					String objectID = data.getString(0);
					Log.i(TAG, "Object id = " + objectID);
					MainActivity.INSTANCE.showLoadingDialog();
					dmsProcessor.browse(objectID, 0, m_lisListner);
				} catch (JSONException e) {
					return new PluginResult(Status.JSON_EXCEPTION);
				}
			}
		} else if (ACTION_BACK.equals(action)) {
			if (MainActivity.UPNP_PROCESSOR == null) {
				return new PluginResult(Status.ERROR, "Cannot get UPNP Processor");
			} else {
				LOADMORE = false;
				MainActivity.INSTANCE.hideLoadingDialog();
				MainActivity.UPNP_PROCESSOR.getDMSProcessor().back(m_lisListner);
			}
		} else if (ACTION_LOADMORE.equals(action)) {
			if (!HAVENEXT)
				return null;
			LOADMORE = true;
			MainActivity.INSTANCE.showLoadingDialog();
			MainActivity.UPNP_PROCESSOR.getDMSProcessor().nextPage(m_lisListner);
		} else if (ACTION_ADDTOPLAYLIST.equals(action)) {
			final PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			if (playlistProcessor == null) {
				MainActivity.INSTANCE.showLongToast("Error: cannot modify playlist");
				return null;
			}
			// TODO: check current container ID here
			MainActivity.UPNP_PROCESSOR.getDMSProcessor().addAllToPlaylist(playlistProcessor,
					new DMSAddRemoveContainerListener() {

						@Override
						public void onActionFail(Exception ex) {
							MainActivity.INSTANCE.hideLoadingDialog();
						}

						@Override
						public void onActionComplete(String message) {
							MainActivity.INSTANCE.hideLoadingDialog();
							String objectID = "";
							try {
								objectID = data.getString(0);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							if (!objectID.isEmpty()) {
								DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
								playlistProcessor.setCurrentItem(Integer.valueOf(objectID));
								if (dmrProcessor == null) {
									MainActivity.INSTANCE
											.showLongToast("You must select a Renderer to play this content");
								} else {
									dmrProcessor.setURIandPlay(playlistProcessor.getCurrentItem());
									sendJavascript("setSelectedDMR('"
											+ MainActivity.UPNP_PROCESSOR.getCurrentDMS().getIdentity().getUdn()
													.getIdentifierString() + "');");
								}
							}
						}

						@Override
						public void onActionStart(String action) {
							MainActivity.INSTANCE.showLoadingDialog();
						}
					});

		}
		return null;
	}

	protected void addToPlaylist(DIDLObject object) {
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor == null) {
			Toast.makeText(MainActivity.INSTANCE, "Cannot get playlist processor", Toast.LENGTH_SHORT).show();
			return;
		}
		PlaylistItem item = null;
		if ((item = playlistProcessor.addDIDLObject(object)) != null) {
			sendJavascript("addItemToPlaylist('" + object.getResources().get(0).getValue() + "');");
			if (item != null) {
				Log.e(TAG, "set URI and Play");
				MainActivity.UPNP_PROCESSOR.getDMRProcessor().setURIandPlay(item);
			}
		} else {
			if (playlistProcessor.isFull()) {
				Toast.makeText(MainActivity.INSTANCE, "Current playlist is full", Toast.LENGTH_SHORT).show();
			} else {
				playlistProcessor.removeDIDLObject(object);
				sendJavascript("removeItemFromPlaylist('" + object.getResources().get(0).getValue() + "');");
			}
		}
	}

	private JSONObject createJsonFromDIDLObject(DIDLObject object) {
		JSONObject result = new JSONObject();

		try {
			result.put("name", object.getTitle().trim().replace("\"", "\\\""));
			result.put("id", object.getId());
			if (object instanceof Container) {
				result.put("childCount", String.valueOf(((Container) object).getChildCount()));
			} else {
				result.put("url", object.getResources().get(0).getValue());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

	private DMSProcessorListner m_lisListner = new DMSProcessorListner() {

		public void onBrowseFail(String message) {
			Log.e(TAG, "Call browse fail. Error: " + message);
			if (message.equals("Root")) {
				sendJavascript("upToDMSList();");
			}
		}

		public void onBrowseComplete(String objectID, boolean haveNext, boolean havePrev,
				Map<String, List<? extends DIDLObject>> result) {
			HAVENEXT = haveNext;
			if (!LOADMORE)
				sendJavascript("clearLibraryList();");
			JSONArray response = new JSONArray();
			for (DIDLObject container : result.get("Containers")) {
				JSONObject object = createJsonFromDIDLObject(container);
				try {
					object.put("icon", "img/ic_didlobject_container.png");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				response.put(object);
			}
			for (DIDLObject item : result.get("Items")) {
				JSONObject object = createJsonFromDIDLObject(item);
				String icon = "";
				if (item instanceof MusicTrack) {
					icon = "img/ic_didlobject_audio.png";
				} else if (item instanceof VideoItem) {
					icon = "img/ic_didlobject_video.png";
				} else if (item instanceof ImageItem) {
					icon = "img/ic_didlobject_image.png";
				}
				try {
					object.put("icon", icon);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				response.put(object);
			}

			sendJavascript("loadBrowseResult('" + response.toString().replace("'", "\\'") + "');");
		}
	};

}
