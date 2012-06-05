package com.app.dlna.dmc.gui.abstractactivity;

import android.app.TabActivity;

import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.SystemListener;

@SuppressWarnings("deprecation")
public class UpnpListenerTabActivity extends TabActivity implements SystemListener {

	@Override
	public void onStartComplete() {

	}

	@Override
	public void onStartFailed() {

	}

	@Override
	public void onNetworkChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRouterError(String cause) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRouterEnabledEvent() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRouterDisabledEvent() {
		// TODO Auto-generated method stub

	}

}
