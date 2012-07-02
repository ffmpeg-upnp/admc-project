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

	}

	@Override
	public void onRouterError(String cause) {

	}

	@Override
	public void onRouterEnabledEvent() {

	}

	@Override
	public void onRouterDisabledEvent() {

	}

}
