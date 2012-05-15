package com.app.dlna.dmc.gui;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.types.UDN;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.app.dlna.dmc.R;
import com.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor;
import com.app.dlna.dmc.processor.nfc.NFCUtils;
import com.app.dlna.dmc.processor.receiver.SDCardReceiver;
import com.app.dlna.dmc.processor.systemservice.RestartService;
import com.app.dlna.dmc.processor.upnp.CoreUpnpService;

public class MainActivity extends UpnpListenerDroidGapActivity {
	private static final String TAG = MainActivity.class.getName();
	public static UpnpProcessor UPNP_PROCESSOR = null;
	private ProgressDialog m_routerProgressDialog;
	private ProgressDialog m_nfcProgressDialog;
	public static MainActivity INSTANCE;
	// SD Card
	private BroadcastReceiver m_mountedReceiver = new SDCardReceiver();
	// NFC
	private NfcAdapter m_nfcAdapter;
	private PendingIntent m_pendingIntent;
	private IntentFilter[] m_filters;
	private String[][] m_techLists;
	private boolean m_waitToWriteTAG = false;
	private String m_messageToWrite;

	private static final int SIZE = 2;
	public ThreadPoolExecutor EXEC = new ThreadPoolExecutor(SIZE, SIZE, 8, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler() {

				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

				}
			});

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

		m_nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		m_pendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		// for NfcTAG that have data
		IntentFilter ndefDiscovered = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndefDiscovered.addDataType("text/plain");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
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

		super.loadUrl("file:///android_asset/www/index.html");

	}

	protected void onResume() {
		Log.e(TAG, "onResume");
		super.onResume();
		if (m_nfcAdapter != null)
			m_nfcAdapter.enableForegroundDispatch(this, m_pendingIntent, m_filters, m_techLists);
	};

	@Override
	protected void onPause() {
		Log.e(TAG, "onPaused");
		super.onPause();
		if (m_nfcAdapter != null)
			m_nfcAdapter.disableForegroundDispatch(MainActivity.this);
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
	}

	@Override
	public void onStartFailed() {
		super.onStartFailed();
		new AlertDialog.Builder(MainActivity.this).setTitle("Start Fail")
				.setMessage("Cannot found a connection for UpnpService. Start service fail")
				.setPositiveButton("OK", new OnClickListener() {

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
				new AlertDialog.Builder(MainActivity.this).setTitle("Network error").setMessage(cause)
						.setCancelable(false).setPositiveButton("OK", new OnClickListener() {

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

	@SuppressWarnings("rawtypes")
	protected void onNewIntent(Intent intent) {
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
						Log.e(TAG, "NFC Tag receive");
						byte[] buffer = msgs[i].getRecords()[0].getPayload();
						String textEncoding = (buffer[0] & 0200) == 0 ? "UTF-8" : "UTF-16";
						int languageCodeLength = buffer[0] & 0077;
						try {
							String text = new String(buffer, languageCodeLength + 1, buffer.length - languageCodeLength
									- 1, textEncoding);
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
							Log.e(TAG, "NFC TAG parse text error");
						}

					}
				}
			}
		} else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			if (m_waitToWriteTAG && m_messageToWrite != null) {
				Log.e(TAG, "New Empty TAG detected, trying to write data");
				writeDataToTAG(intent);
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
		if (m_nfcAdapter != null && m_nfcAdapter.isEnabled()) {
			m_messageToWrite = text;
			m_waitToWriteTAG = true;
			m_nfcProgressDialog.show();
		} else {
			new AlertDialog.Builder(MainActivity.this).setTitle("NFC")
					.setMessage("Please enable NFC on you device first").setPositiveButton("OK", null).create().show();
		}
	}

}
