package com.app.dlna.dmc.phonegapui.plugin;

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

import com.app.dlna.dmc.phonegapui.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor.DMSProcessorListner;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class LibraryPlugin extends Plugin {
	private static final String TAG = LibraryPlugin.class.getName();
	private static final String ACTION_BROWSE = "browse";
	private static final String ACTION_BACK = "back";
	private static final String ACTION_FILTER = "filter";
	private static final String ACTION_GETPAGE = "getCurrentPage";
	private static final String ACTION_NEXTPAGE = "nextPage";
	private static final String ACTION_PREVIOUSPAGE = "previousPage";

	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		if (ACTION_BROWSE.equals(action)) {
			if (MainActivity.UPNP_PROCESSOR == null) {
				return new PluginResult(Status.ERROR, "Cannot get UPNP Processor");
			} else {
				try {
					DMSProcessor dmsProcessor = MainActivity.UPNP_PROCESSOR.getDMSProcessor();
					String objectID = data.getString(0);
					Log.i(TAG, "Object id = " + objectID);
					dmsProcessor.browse(objectID, 0, m_lisListner);
				} catch (JSONException e) {
					return new PluginResult(Status.JSON_EXCEPTION);
				}
			}
		} else if (ACTION_FILTER.equals(action)) {

		} else if (ACTION_BACK.equals(action)) {
			if (MainActivity.UPNP_PROCESSOR == null) {
				return new PluginResult(Status.ERROR, "Cannot get UPNP Processor");
			} else {
				DMSProcessor dmsProcessor = MainActivity.UPNP_PROCESSOR.getDMSProcessor();
				dmsProcessor.back(m_lisListner);
			}
		} else if (ACTION_GETPAGE.equals(action)) {
			Log.i(TAG, "Get current Page");

		} else if (ACTION_PREVIOUSPAGE.equals(action)) {
			Log.i(TAG, "Previous page");
			MainActivity.UPNP_PROCESSOR.getDMSProcessor().previousPage(m_lisListner);
		} else if (ACTION_NEXTPAGE.equals(action)) {
			Log.i(TAG, "Next page");
			MainActivity.UPNP_PROCESSOR.getDMSProcessor().nextPage(m_lisListner);
		}

		return null;
	}

	private JSONObject createJsonFromDIDLObject(DIDLObject object) {
		JSONObject result = new JSONObject();

		try {
			result.put("name", object.getTitle().trim().replace("\"", "\\\"").replace("'", " "));
			result.put("id", object.getId());
			result.put("state", "false");
			if (object instanceof Container) {
				result.put("childCount", String.valueOf(((Container) object).getChildCount()));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

	private DMSProcessorListner m_lisListner = new DMSProcessorListner() {

		public void onBrowseFail(String message) {
			Log.e(TAG, "Call browse fail. Error: " + message);
		}

		// public void onBrowseComplete(Map<String, List<? extends DIDLObject>>
		// result) {
		// sendJavascript("clearLibraryList();");
		// JSONArray response = new JSONArray();
		// for (DIDLObject container : result.get("Containers")) {
		// JSONObject object = createJsonFromDIDLObject(container);
		// try {
		// object.put("icon", "img/folder_icon.png");
		// } catch (JSONException e) {
		// e.printStackTrace();
		// }
		// response.put(object);
		// if (response.length() == 10) {
		// Log.i(TAG, "Browse result = " + response.toString());
		// sendJavascript("loadBrowseResult('" + response.toString() + "');");
		// response = new JSONArray();
		// }
		// }
		// Log.i(TAG, "Browse result = " + response.toString());
		// if (response.length() != 0)
		// sendJavascript("loadBrowseResult('" + response.toString() + "');");
		// response = new JSONArray();
		// for (DIDLObject item : result.get("Items")) {
		// JSONObject object = createJsonFromDIDLObject(item);
		// String icon = "";
		// if (item instanceof MusicTrack) {
		// icon = "img/ic_didlobject_audio.png";
		// } else if (item instanceof VideoItem) {
		// icon = "img/ic_didlobject_video.png";
		// } else if (item instanceof ImageItem) {
		// icon = "img/ic_didlobject_image.png";
		// }
		// try {
		// object.put("icon", icon);
		// } catch (JSONException e) {
		// e.printStackTrace();
		// }
		// response.put(object);
		// if (response.length() >= 10) {
		// Log.i(TAG, "Browse result = " + response.toString());
		// sendJavascript("loadBrowseResult('" + response.toString() + "');");
		// response = new JSONArray();
		// }
		// }
		// Log.i(TAG, "Browse result = " + response.toString());
		// if (response.length() != 0)
		// sendJavascript("loadBrowseResult('" + response.toString() + "');");
		// }
		// public void onBrowseComplete(Map<String, List<? extends DIDLObject>>
		// result) {
		// sendJavascript("notifyBrowseComplete();");
		// };

		public void onBrowseComplete(String objectID, boolean haveNext, boolean havePrev,
				Map<String, List<? extends DIDLObject>> result) {
			sendJavascript("clearLibraryList();");
			JSONArray response = new JSONArray();
			for (DIDLObject container : result.get("Containers")) {
				JSONObject object = createJsonFromDIDLObject(container);
				try {
					object.put("icon", "img/folder_icon.png");
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
			if (response.length() != 0)
				sendJavascript("loadBrowseResult('" + response.toString() + "');");
			if (objectID.equals("0")) {
				sendJavascript("disableBackButton();");
			} else {
				sendJavascript("enableBackButton();");
			}

			if (haveNext) {
				sendJavascript("enableNextPageButton();");
			} else {
				sendJavascript("disableNextPageButton();");
			}

			if (havePrev) {
				sendJavascript("enablePrevPageButton();");
			} else {
				sendJavascript("disablePrevPageButton();");
			}
		}
	};
}