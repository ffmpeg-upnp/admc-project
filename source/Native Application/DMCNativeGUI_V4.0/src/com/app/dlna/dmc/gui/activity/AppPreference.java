package com.app.dlna.dmc.gui.activity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.app.dlna.dmc.processor.playlist.Playlist.ViewMode;

public class AppPreference {
	public static SharedPreferences PREF = null;

	public static int getMaxItemPerLoad() {
		return PREF != null ? Integer.valueOf(PREF.getString("max_item_count", "50")) : 50;
	}

	public static boolean getVideoQuality() {
		return PREF != null ? PREF.getBoolean("video_quality", false) : false;
	}

	public static int getImageDimension() {
		return Integer.valueOf(PREF != null ? PREF.getString("image_qualitiy", "384") : "384");
	}

	public static void setPlaylistViewMode(ViewMode viewMode) {
		if (PREF != null) {
			Editor editor = PREF.edit();
			editor.putString("playlist_viewmode", viewMode.toString());
			editor.commit();
		}

	}

	public static ViewMode getPlaylistViewMode() {
		return ViewMode.valueOf(PREF != null ? PREF.getString("playlist_viewmode", "ALL") : "ALL");
	}

	public static boolean getDMSExported() {
		return PREF != null ? PREF.getBoolean("dms_exported", true) : true;
	}

	public static boolean getImageZoomable() {
		return PREF != null ? PREF.getBoolean("image_zoomable", false) : false;
	}
}
