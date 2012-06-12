package com.app.dlna.dmc.processor.systemservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.app.dlna.dmc.gui.MainActivity;

public class RestartService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(4000);
					Intent intent = new Intent(RestartService.this, MainActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startActivity(intent);
				} catch (Exception ex) {

				}
			}
		}).start();
		return super.onStartCommand(intent, flags, startId);
	}
}
