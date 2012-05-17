package com.app.dlna.dmc.phonegap.plugin;

import org.json.JSONArray;

import com.app.dlna.dmc.gui.MainActivity;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;

public class ApplicationPlugin extends Plugin {
	public static final String ACTION_SHOWLOADSTART = "showLoadStart";
	public static final String ACTION_SHOWLOADCOMPLETE = "showLoadComplete";

	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		if (ACTION_SHOWLOADSTART.equals(action)) {
			MainActivity.INSTANCE.showLoadingDialog();
		} else if (ACTION_SHOWLOADCOMPLETE.equals(action)) {
			MainActivity.INSTANCE.hideLoadingDialog();
		}
		return null;
	}

}
