package com.app.dlna.dmc.gui;

import android.content.SharedPreferences;

public class AppPreference {
	public static SharedPreferences PREF = null;

	public static int getMaxItemPerLoad() {
		return 15;
	}

	public static boolean isVideoHQ() {
		return false;
	}

	public static int getImageDimension() {
		return 0;
	}

	public static boolean getDMSExported() {
		return false;
	}

	public static boolean getImageZoomable() {
		return false;
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
		return true;
	}

	public static boolean getProxyMode() {
		return true;
	}

	public static boolean stopDMR() {
		return true;
	}

	public static boolean getAutoNext() {
		return true;
	}

}
