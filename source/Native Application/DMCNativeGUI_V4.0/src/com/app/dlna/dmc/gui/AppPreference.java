package com.app.dlna.dmc.gui.activity;

import android.content.SharedPreferences;

public class AppPreference {
	public static SharedPreferences PREF = null;

	public static int getMaxItemPerLoad() {
		return PREF != null ? Integer.valueOf(PREF.getString("max_item_count", "50")) : 50;
	}

	public static boolean getVideoQuality() {
		return PREF != null ? PREF.getBoolean("video_quality", false) : false;
	}
}
