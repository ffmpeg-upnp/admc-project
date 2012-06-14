package com.app.dlna.dmc.gui;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.widget.Toast;

import com.app.dlna.dmc.phonegap.R;
import com.app.dlna.dmc.phonegap.plugin.DevicesPlugin;
import com.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor;
import com.app.dlna.dmc.processor.receiver.SDCardReceiver;
import com.app.dlna.dmc.processor.systemservice.RestartService;
import com.app.dlna.dmc.processor.upnp.CoreUpnpService;

public class MainActivity extends UpnpListenerDroidGapActivity {
	private static final String TAG = MainActivity.class.getName();
	public static UpnpProcessor UPNP_PROCESSOR = null;
	private ProgressDialog m_routerProgressDialog;
	private ProgressDialog m_loadingDialog;
	public static MainActivity INSTANCE;
	// SD Card
	private BroadcastReceiver m_mountedReceiver = new SDCardReceiver();

	private static final int SIZE = 2;
	public ThreadPoolExecutor EXEC = new ThreadPoolExecutor(SIZE, SIZE, 8, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
			new RejectedExecutionHandler() {

				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

				}
			});
	private DevicesPlugin devicesPlugin;
	private Toast m_longToast;
	private Toast m_shortToast;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		UPNP_PROCESSOR = new UpnpProcessorImpl(MainActivity.this);
		UPNP_PROCESSOR.bindUpnpService();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addDataScheme("file");

		registerReceiver(m_mountedReceiver, filter);
		INSTANCE = this;

		super.loadUrl("file:///android_asset/www/index.html");
		m_loadingDialog = new ProgressDialog(MainActivity.this);
		m_loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		m_loadingDialog.setMessage("Loading...");
		m_loadingDialog.setCancelable(true);

		m_longToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_LONG);
		m_longToast.setGravity(Gravity.CENTER, 0, 0);

		m_shortToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);
		m_shortToast.setGravity(Gravity.CENTER, 0, 0);
	}

	protected void onResume() {
		Log.e(TAG, "onResume");
		super.onResume();
	};

	@Override
	protected void onPause() {
		Log.e(TAG, "onPaused");
		super.onPause();
	}

	public void onDestroy() {
		Log.e(TAG, "onDestroy");
		super.onDestroy();
		UPNP_PROCESSOR.unbindUpnpService();
		unregisterReceiver(m_mountedReceiver);
	};

	@Override
	public void onStartComplete() {
		super.onStartComplete();
		devicesPlugin = new DevicesPlugin(MainActivity.this);
		UPNP_PROCESSOR.addDevicesListener(devicesPlugin);
	}

	public void refreshDMSList() {
		if (devicesPlugin != null)
			try {
				devicesPlugin.execute(DevicesPlugin.ACTION_REFRESH_DMS, new JSONArray("[]"), "");
			} catch (JSONException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void onStartFailed() {
		super.onStartFailed();
		new AlertDialog.Builder(MainActivity.this)
				.setTitle("Network Info")
				.setMessage(
						"Cannot find a wifi connection for UpnpService. Please connect to a wifi network or run wifi hotspot to use the app.")
				.setPositiveButton("Wifi settings", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
						MainActivity.this.finish();
						((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
								.cancel(CoreUpnpService.NOTIFICATION);
					}
				}).setNegativeButton("Exit", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.this.finish();
						((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
								.cancel(CoreUpnpService.NOTIFICATION);
					}
				}).setCancelable(false).create().show();
	}

	@Override
	public void onNetworkChanged() {
		super.onNetworkChanged();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				new AlertDialog.Builder(MainActivity.this).setTitle("Network changed")
						.setMessage("Network interface changed. Application must restart.").setCancelable(false)
						.setPositiveButton("OK", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								restartActivity();
							}
						}).create().show();

			}
		});
	}

	@Override
	public void onRouterError(final String cause) {
		super.onRouterError(cause);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (m_routerProgressDialog != null)
					m_routerProgressDialog.dismiss();
				new AlertDialog.Builder(MainActivity.this).setTitle("Network error").setMessage(cause).setCancelable(false)
						.setPositiveButton("OK", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								MainActivity.this.finish();
							}
						}).create().show();
			}
		});

	}

	@Override
	public void onRouterDisabledEvent() {
		super.onRouterDisabledEvent();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (m_routerProgressDialog != null)
					m_routerProgressDialog.dismiss();
				m_routerProgressDialog = ProgressDialog.show(MainActivity.this, "Router disabled",
						"Router disabled, try to enabled router");
			}
		});

	}

	@Override
	public void onRouterEnabledEvent() {
		super.onRouterEnabledEvent();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (m_routerProgressDialog != null)
					m_routerProgressDialog.dismiss();
			}
		});
	}

	private void restartActivity() {
		MainActivity.this.startService(new Intent(MainActivity.this, RestartService.class));
		MainActivity.this.finish();
	}

	@Override
	public void onBackPressed() {
		confirmExit();
	}

	private void confirmExit() {
		new AlertDialog.Builder(MainActivity.this).setTitle("Confirm exit").setMessage("Are you sure want to exit?")
				.setPositiveButton(R.string.minimize, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						moveTaskToBack(false);
					}
				}).setNegativeButton(R.string.close, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).create().show();
	}

	protected void onNewIntent(Intent intent) {
	}

	public void showLoadingDialog() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_loadingDialog.show();
			}
		});
	}

	public void hideLoadingDialog() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_loadingDialog.dismiss();
			}
		});
	}

	public boolean isLoading() {
		return m_loadingDialog.isShowing();
	}

	public void showShortToast(final String message) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_shortToast.setText(message);
				m_shortToast.show();
			}
		});
	}

	public void showLongToast(final String message) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_longToast.setText(message);
				m_longToast.show();
			}
		});
	}
}
