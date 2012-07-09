package com.app.dlna.dmc.gui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.webkit.WebView;
import app.dlna.controller.v4.R;

public class HelpDialog extends Dialog {

	public HelpDialog(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dl_help);
		((WebView) findViewById(R.id.webView)).loadUrl("file:///android_asset/howtouse.html");
	}
}
