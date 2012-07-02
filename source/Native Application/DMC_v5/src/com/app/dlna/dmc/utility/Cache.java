package com.app.dlna.dmc.utility;

import java.util.HashMap;

import android.graphics.Bitmap;

public class Cache {
	private static HashMap<String, Bitmap> BITMAP_CACHE = new HashMap<String, Bitmap>();

	public static HashMap<String, Bitmap> getBitmapCache() {
		return BITMAP_CACHE;
	}

	public static void clear() {
		synchronized (BITMAP_CACHE) {
			BITMAP_CACHE.clear();
		}
	}
}
