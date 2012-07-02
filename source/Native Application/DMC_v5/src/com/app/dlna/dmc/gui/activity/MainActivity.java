package com.app.dlna.dmc.gui.activity;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.types.UDN;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.customview.RendererCompactView;
import com.app.dlna.dmc.gui.customview.RendererCompactView.OnDMRChangeListener;
import com.app.dlna.dmc.processor.impl.PlaylistManager;
import com.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.SystemListener;
import com.app.dlna.dmc.processor.upnp.CoreUpnpService;
import com.app.dlna.dmc.processor.upnp.LocalContentDirectoryService;
import com.app.dlna.dmc.system.SDCardReceiver;
import com.app.dlna.dmc.utility.NFCUtils;

@SuppressWarnings("deprecation")
public class MainActivity extends TabActivity implements SystemListener {
	private static final int NOWPLAYING = 1;
	private static final int LIBRARY = 0;
	private TabHost m_tabHost;
	public static UpnpProcessor UPNP_PROCESSOR = null;
	private static final int DEFAULT_TAB_INDEX = 0;
	private ProgressDialog m_routerProgressDialog;
	private ProgressDialog m_nfcProgressDialog;
	private ProgressDialog m_loadingDialog;
	private Toast m_infoToast;
	public static MainActivity INSTANCE;
	private BroadcastReceiver m_mountedReceiver = new SDCardReceiver();
	// NFC
	private PendingIntent m_pendingIntent;
	private IntentFilter[] m_filters;
	private String[][] m_techLists;
	private boolean m_waitToWriteTAG = false;
	private String m_messageToWrite;
	// Renderer compactview
	private RendererCompactView m_rendererCompactView;
	private ImageView m_btn_toggleRendererView;
	private LinearLayout m_ll_tabwidgets;

	private static final int SIZE = 2;
	protected static final String ACTION_PLAYTO = "com.app.dlna.dmc.gui.MainActivity.ACTION_PLAYTO";
	public static final String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
	public static final String EXTRA_DOWNLOAD_ID = "download_id";
	public ThreadPoolExecutor EXEC = new ThreadPoolExecutor(SIZE, SIZE, 8, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
			new RejectedExecutionHandler() {

				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

				}
			});

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		m_tabHost = getTabHost();
		m_tabHost.setup();

		UPNP_PROCESSOR = new UpnpProcessorImpl(MainActivity.this);
		UPNP_PROCESSOR.bindUpnpService();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addDataScheme("file");

		registerReceiver(m_mountedReceiver, filter);
		INSTANCE = this;

		// m_ll_menu = (LinearLayout) findViewById(R.id.ll_floatMenu);

		m_pendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		// for NfcTAG that have data
		IntentFilter ndefDiscovered = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndefDiscovered.addDataType("text/plain");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Init failed");
		}
		// for Empty NfcTAG
		IntentFilter techDiscovered = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

		m_filters = new IntentFilter[] { ndefDiscovered, techDiscovered };
		m_techLists = new String[][] { new String[] { Ndef.class.getName() } };

		m_nfcProgressDialog = new ProgressDialog(MainActivity.this);
		m_nfcProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		m_nfcProgressDialog.setMessage("Tap for a NFC TAG to write");
		m_nfcProgressDialog.setCancelable(true);
		m_nfcProgressDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				m_waitToWriteTAG = false;
			}
		});
		PlaylistManager.RESOLVER = getContentResolver();
		AppPreference.PREF = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

		m_loadingDialog = new ProgressDialog(MainActivity.this);
		m_loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		m_loadingDialog.setCancelable(true);
		m_loadingDialog.setCanceledOnTouchOutside(false);

		m_infoToast = Toast.makeText(MainActivity.this, "Toast", Toast.LENGTH_LONG);
	}

	public void showToast(final String message) {
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_infoToast.setText(message);
				m_infoToast.show();
			}
		});

	}

	public boolean showLoadingMessage(final String message) {
		if (!m_loadingDialog.isShowing()) {
			MainActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					m_loadingDialog.setMessage(message);
					m_loadingDialog.show();
				}
			});
			return true;
		} else
			return false;
	}

	public boolean dismissLoadingDialog() {
		boolean result = m_loadingDialog.isShowing();
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_loadingDialog.dismiss();
			}
		});
		return result;
	}

	private OnTabChangeListener changeListener = new OnTabChangeListener() {

		@Override
		public void onTabChanged(String tabId) {
			updateTab();
			// if (m_rendererCompactView.isShown())
			// hideRendererCompactView();
		}
	};

	protected void onResume() {
		super.onResume();
		NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(this);
		if (defaultAdapter != null)
			defaultAdapter.enableForegroundDispatch(this, m_pendingIntent, m_filters, m_techLists);
		if (UPNP_PROCESSOR != null)
			UPNP_PROCESSOR.setDMSExproted(AppPreference.getDMSExported());
	};

	@Override
	protected void onPause() {
		super.onPause();
		NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(this);
		if (defaultAdapter != null)
			defaultAdapter.disableForegroundDispatch(MainActivity.this);
	}

	protected void onDestroy() {
		super.onDestroy();
		UPNP_PROCESSOR.unbindUpnpService();
		unregisterReceiver(m_mountedReceiver);
	};

	@Override
	public void onStartComplete() {
		createTabs();
		m_ll_tabwidgets = (LinearLayout) findViewById(R.id.ll_tabwidgets);
		m_rendererCompactView = (RendererCompactView) findViewById(R.id.cv_compact_dmr);
		m_rendererCompactView.initComponent();
		m_rendererCompactView.setOnDMRChangeListener(m_onDMRChanged);
		m_btn_toggleRendererView = (ImageView) findViewById(R.id.btn_toggleShowHide);
		Intent intent = getIntent();
		if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_PLAYTO)) {
			switchToNowPlaying();
		}
	}

	private OnDMRChangeListener m_onDMRChanged = new OnDMRChangeListener() {

		@Override
		public void onDMRChange() {
			String tabTag = getTabHost().getCurrentTabTag();
			Activity activity = getLocalActivityManager().getActivity(tabTag);
			if (activity instanceof NowPlayingActivity) {
				NowPlayingActivity nowPlayingActivity = (NowPlayingActivity) activity;
				nowPlayingActivity.updateDMRControlView();
				nowPlayingActivity.updateItemInfo();
			} else if (activity instanceof LibraryActivity) {
				LibraryActivity libraryActivity = (LibraryActivity) activity;
				libraryActivity.getHomeNetworkView().updateListView();
				libraryActivity.getPlaylistView().updateListView();
			}
			MainActivity.UPNP_PROCESSOR.getDMRProcessor()
					.setPlaylistProcessor(MainActivity.UPNP_PROCESSOR.getPlaylistProcessor());
		}

		@Override
		public void onDMRUpdate() {
			String tabTag = getTabHost().getCurrentTabTag();
			Activity activity = getLocalActivityManager().getActivity(tabTag);
			if (activity instanceof NowPlayingActivity) {
				((NowPlayingActivity) activity).updateDMRControlView();
			}
		}
	};

	private void createTabs() {
		Intent intent = null;

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		TabSpec library = m_tabHost.newTabSpec(getString(R.string.library));
		TextView tvMediaSource = (TextView) inflater.inflate(R.layout.cv_tabwidget, null);
		tvMediaSource.setText(R.string.library);
		library.setIndicator(tvMediaSource);
		intent = new Intent(this, LibraryActivity.class);
		library.setContent(intent);

		TabSpec nowPlayling = m_tabHost.newTabSpec(getString(R.string.now_playing));
		TextView tvPlaylist = (TextView) inflater.inflate(R.layout.cv_tabwidget, null);
		tvPlaylist.setText(R.string.now_playing);
		nowPlayling.setIndicator(tvPlaylist);
		intent = new Intent(this, NowPlayingActivity.class);
		nowPlayling.setContent(intent);

		m_tabHost.addTab(library);
		m_tabHost.addTab(nowPlayling);
		m_tabHost.setCurrentTab(DEFAULT_TAB_INDEX);
		m_tabHost.setOnTabChangedListener(changeListener);
		m_tabHost.refreshDrawableState();
		updateTab();
	}

	private void updateTab() {
		for (int i = 0; i < m_tabHost.getTabWidget().getChildCount(); ++i) {
			View view = m_tabHost.getTabWidget().getChildAt(i);
			if (m_tabHost.getCurrentTab() == i)
				((TextView) view).setTextColor(getResources().getColor(R.color.blue));
			else
				((TextView) view).setTextColor(getResources().getColor(R.color.blue_transparent_less));
		}
		if (m_ll_tabwidgets != null) {
			Activity activity = getLocalActivityManager().getActivity(m_tabHost.getCurrentTabTag());
			if (activity instanceof LibraryActivity) {
				m_ll_tabwidgets.setVisibility(View.VISIBLE);
				if (m_btn_toggleRendererView != null)
					m_btn_toggleRendererView.setVisibility(View.VISIBLE);
			} else {

				int displayMode = getWindowManager().getDefaultDisplay().getRotation();
				if (displayMode == Surface.ROTATION_0 || displayMode == Surface.ROTATION_180) {
					if (m_btn_toggleRendererView != null)
						m_btn_toggleRendererView.setVisibility(View.VISIBLE);
					if (m_ll_tabwidgets != null)
						m_ll_tabwidgets.setVisibility(View.VISIBLE);
				} else {
					if (m_btn_toggleRendererView != null)
						m_btn_toggleRendererView.setVisibility(View.GONE);
					if (m_ll_tabwidgets != null)
						m_ll_tabwidgets.setVisibility(View.GONE);
				}

			}
		}
	}

	@Override
	public void onStartFailed() {
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
	public void finishFromChild(Activity child) {
		if (m_tabHost.getCurrentTabTag().equals(getString(R.string.library))) {
			confirmExit();
		} else {
			m_tabHost.setCurrentTab(DEFAULT_TAB_INDEX);
		}
	}

	@Override
	public void onNetworkChanged() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					new AlertDialog.Builder(MainActivity.this).setTitle("Network changed")
							.setMessage("Network interface changed. Application must restart.").setCancelable(false)
							.setPositiveButton("OK", new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							}).create().show();
				} catch (Exception e) {
				}

			}
		});
	}

	@Override
	public void onRouterError(final String cause) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (m_routerProgressDialog != null)
					m_routerProgressDialog.dismiss();
				try {
					new AlertDialog.Builder(MainActivity.this).setTitle("Network error").setMessage(cause).setCancelable(false)
							.setPositiveButton("OK", new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									MainActivity.this.finish();
								}
							}).create().show();
				} catch (Exception ex) {

				}
			}
		});

	}

	@Override
	public void onRouterDisabledEvent() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (m_routerProgressDialog != null)
					m_routerProgressDialog.dismiss();
				try {
					m_routerProgressDialog = ProgressDialog.show(MainActivity.this, "Router disabled",
							"Router disabled, try to enabled router");
				} catch (Exception ex) {

				}
			}
		});

	}

	@Override
	public void onRouterEnabledEvent() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				if (m_routerProgressDialog != null)
					m_routerProgressDialog.dismiss();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_rescan_sdcard:
			Toast.makeText(MainActivity.this, "Rescan sdcard", Toast.LENGTH_SHORT).show();
			if (!LocalContentDirectoryService.isScanning())
				LocalContentDirectoryService.scanMedia(MainActivity.this);
			break;
		case R.id.menu_refresh_devices:
			Toast.makeText(MainActivity.this, "Refresh", Toast.LENGTH_SHORT).show();
			UPNP_PROCESSOR.refreshDevicesList();
			break;
		case R.id.menu_settings:
			Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
			MainActivity.this.startActivity(intent);
			break;
		}
		return super.onOptionsItemSelected(item);
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
						moveTaskToBack(true);
					}
				}).setNegativeButton(R.string.close, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).create().show();
	}

	@SuppressWarnings("rawtypes")
	protected void onNewIntent(Intent intent) {
		if (intent.getAction() == null)
			return;
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			if (m_waitToWriteTAG && m_messageToWrite != null) {
				// write message to TAG
				writeDataToTAG(intent);
			} else {
				Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
				if (rawMsgs != null) {
					NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
					for (int i = 0; i < rawMsgs.length; i++) {
						msgs[i] = (NdefMessage) rawMsgs[i];
						byte[] buffer = msgs[i].getRecords()[0].getPayload();
						String textEncoding = (buffer[0] & 0200) == 0 ? "UTF-8" : "UTF-16";
						int languageCodeLength = buffer[0] & 0077;
						try {
							String text = new String(buffer, languageCodeLength + 1, buffer.length - languageCodeLength - 1,
									textEncoding);
							String deviceUDN = "";
							if (text.startsWith("uuid:"))
								deviceUDN = text.substring(5);
							else
								deviceUDN = text;
							if (UPNP_PROCESSOR != null) {
								Device newDevice = null;
								if ((newDevice = UPNP_PROCESSOR.getRegistry().getDevice(new UDN(deviceUDN), true)) != null) {
									String alert = null;
									if (newDevice.getType().getType().equals("MediaServer")) {
										alert = "Detect Media Server From TAG";
										UPNP_PROCESSOR.setCurrentDMS(newDevice.getIdentity().getUdn());
									} else if (newDevice.getType().getType().equals("MediaRenderer")) {
										alert = "Detect Media Renderer From TAG";
										UPNP_PROCESSOR.setCurrentDMR(newDevice.getIdentity().getUdn());
									}
									if (alert != null)
										Toast.makeText(MainActivity.this, alert, Toast.LENGTH_SHORT).show();
								} else {
									Toast.makeText(
											MainActivity.this,
											"Cannot find specified device from Nfc TAG, please check if device is connected to the network or the data on your TAG",
											Toast.LENGTH_LONG).show();
								}
							}
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}

					}
				}
			}
		} else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			if (m_waitToWriteTAG && m_messageToWrite != null) {
				writeDataToTAG(intent);
			}
		} else if (MainActivity.ACTION_PLAYTO.equals(intent.getAction())) {
			m_tabHost.setCurrentTab(NOWPLAYING);
		} else if (intent.getAction().startsWith(ACTION_CANCEL_DOWNLOAD)) {
			int download_id = Integer.valueOf(intent.getAction().split("_")[2]);
			Log.i("MainActivity", "download_id = " + download_id);
			Toast.makeText(MainActivity.this, "Cancel download request " + download_id, Toast.LENGTH_SHORT).show();
			if (UPNP_PROCESSOR != null) {
				UPNP_PROCESSOR.getDownloadProcessor().stopDownload(download_id);
			}
		}
	}

	private void writeDataToTAG(Intent intent) {
		try {
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			if (NFCUtils.writeTag(NFCUtils.getNdefMessageFromString(m_messageToWrite), detectedTag)) {
				m_nfcProgressDialog.dismiss();
				Toast.makeText(this, "Write device info to NFC Tag complete.", Toast.LENGTH_LONG).show();
			} else {
				m_nfcProgressDialog.dismiss();
				Toast.makeText(this, "Write failed. Try again later.", Toast.LENGTH_LONG).show();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Toast.makeText(this, "Write failed; cause = " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	};

	public void waitToWriteTAG(String text) {
		NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(this);
		if (defaultAdapter != null && defaultAdapter.isEnabled()) {
			m_messageToWrite = text;
			m_waitToWriteTAG = true;
			m_nfcProgressDialog.show();
		} else {
			new AlertDialog.Builder(MainActivity.this).setTitle("NFC").setMessage("Please enable NFC on you device first")
					.setPositiveButton("OK", null).create().show();
		}
	}

	public void showRendererCompactView() {
		if (m_rendererCompactView == null)
			return;
		Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.compactrenderer_slidein);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				m_rendererCompactView.setVisibility(View.VISIBLE);
				m_rendererCompactView.updateListRenderer();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (m_btn_toggleRendererView != null)
					m_btn_toggleRendererView.setImageDrawable(getResources().getDrawable(R.drawable.ic_btn_navigate_down));
			}
		});
		m_rendererCompactView.startAnimation(animation);

	}

	public void hideRendererCompactView() {
		if (m_rendererCompactView == null)
			return;
		Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.compactrenderer_slideout);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				m_rendererCompactView.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				m_rendererCompactView.setVisibility(View.GONE);
				if (m_btn_toggleRendererView != null)
					m_btn_toggleRendererView.setImageDrawable(getResources().getDrawable(R.drawable.ic_btn_navigate_up));
			}
		});
		m_rendererCompactView.startAnimation(animation);
	}

	public boolean isCompactRendererShowing() {
		return m_rendererCompactView.getVisibility() == View.VISIBLE;
	}

	public void onShowHideClick(View view) {
		if (isCompactRendererShowing())
			hideRendererCompactView();
		else
			showRendererCompactView();
	}

	public void switchToLibrary() {
		m_tabHost.setCurrentTab(LIBRARY);
	}

	public void switchToNowPlaying() {
		m_tabHost.setCurrentTab(NOWPLAYING);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (m_ll_tabwidgets == null || m_btn_toggleRendererView == null)
			return;
		Activity activity = getLocalActivityManager().getActivity(m_tabHost.getCurrentTabTag());
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			m_ll_tabwidgets.setVisibility(View.VISIBLE);
			m_btn_toggleRendererView.setVisibility(View.VISIBLE);
			if (activity instanceof NowPlayingActivity)
				((NowPlayingActivity) activity).switchToPortrait();

		} else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (activity instanceof NowPlayingActivity) {
				m_ll_tabwidgets.setVisibility(View.GONE);
				m_btn_toggleRendererView.setVisibility(View.GONE);
				((NowPlayingActivity) activity).switchToLandscape();
			} else {
				m_ll_tabwidgets.setVisibility(View.VISIBLE);
				m_btn_toggleRendererView.setVisibility(View.VISIBLE);
			}
		}

		m_rendererCompactView.setVisibility(View.GONE);
		if (m_btn_toggleRendererView != null)
			m_btn_toggleRendererView.setImageDrawable(getResources().getDrawable(R.drawable.ic_btn_navigate_up));
	}
}
