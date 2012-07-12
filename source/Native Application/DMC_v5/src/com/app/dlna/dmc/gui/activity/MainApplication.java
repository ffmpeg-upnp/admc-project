package com.app.dlna.dmc.gui.activity;

import java.util.Locale;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class MainApplication extends Application {

	private static MainApplication INSTANCE;

	@Override
	public void onCreate() {
		updateLanguage(this);
		super.onCreate();
		INSTANCE = this;
	}

	public static Context getContext() {
		return INSTANCE.getApplicationContext();
	}

	public static void updateLanguage(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String lang = prefs.getString("locale_override", "en");
		updateLanguage(ctx, lang);
	}

	public static void updateLanguage(Context ctx, String lang) {
		Configuration cfg = new Configuration();
		if (!TextUtils.isEmpty(lang))
			cfg.locale = new Locale(lang);
		else
			cfg.locale = Locale.getDefault();

		ctx.getResources().updateConfiguration(cfg, null);
	}

	public static void updateLanguage(Context ctx, Configuration cfg) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String lang = prefs.getString("locale_override", "en");
		if (!TextUtils.isEmpty(lang))
			cfg.locale = new Locale(lang);
		else
			cfg.locale = Locale.getDefault();

		ctx.getResources().updateConfiguration(cfg, null);
	}
}