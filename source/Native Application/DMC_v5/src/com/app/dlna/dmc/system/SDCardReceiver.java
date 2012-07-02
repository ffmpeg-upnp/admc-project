package com.app.dlna.dmc.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.app.dlna.dmc.processor.upnp.LocalContentDirectoryService;

public class SDCardReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
			LocalContentDirectoryService.removeAllContent();
		} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
			LocalContentDirectoryService.scanMedia(context);
		}
	}

}
