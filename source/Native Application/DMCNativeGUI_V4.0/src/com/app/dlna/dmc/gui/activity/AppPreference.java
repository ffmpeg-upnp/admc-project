package com.app.dlna.dmc.gui.activity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.app.dlna.dmc.processor.playlist.Playlist.ViewMode;

public class AppPreference {
	public static SharedPreferences PREF = null;

	public static int getMaxItemPerLoad() {
		return PREF != null ? Integer.valueOf(PREF.getString("max_item_count", "50")) : 50;
	}

	public static boolean isVideoHQ() {
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

	public static String[] getMusicExtension() {
		String musicExtension = PREF != null ? PREF.getString("music_ext", "") : "";
		if (PREF != null && musicExtension.trim().isEmpty()) {
			musicExtension = "mp3;wma;midi;wav;mid;midi;";
			PREF.edit().putString("music_ext", musicExtension).commit();
		}
		return musicExtension.split(";");
	}

	public static String[] getVideoExtension() {
		String videoExtension = PREF != null ? PREF.getString("video_ext", "") : "";
		if (PREF != null && videoExtension.trim().isEmpty()) {
			videoExtension = "mp4;flv;mpg;avi;mkv;m4v;";
			PREF.edit().putString("video_ext", videoExtension).commit();
		}
		return videoExtension.split(";");
	}

	public static String[] getImageExtension() {
		String imageExtension = PREF != null ? PREF.getString("image_ext", "") : "";
		if (PREF != null && imageExtension.trim().isEmpty()) {
			imageExtension = "jpeg;jpg;png;gif;bmp;gif;";
			PREF.edit().putString("image_ext", imageExtension).commit();
		}
		return imageExtension.split(";");
	}

	public static boolean getKillProcessStatus() {
		return PREF != null ? PREF.getBoolean("kill_process", false) : false;
	}

	public static boolean getProxyMode() {
		return PREF != null ? PREF.getBoolean("proxy_mode", false) : false;
	}

	public static boolean stopDMR() {
		return PREF != null ? PREF.getBoolean("stop_dmr", true) : true;
	}

	public static String getLocalServerName() {
		return "Media2Share Server";
	}

	public static boolean getAutoNext() {
		return PREF != null ? PREF.getBoolean("auto_next", false) : false;
	}
}
