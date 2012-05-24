package com.app.dlna.dmc.gui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import app.dlna.controller.v4.R;

public class SettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
	}
}
