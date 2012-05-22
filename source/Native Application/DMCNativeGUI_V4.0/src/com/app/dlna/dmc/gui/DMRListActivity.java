package com.app.dlna.dmc.gui;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.support.model.DIDLObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.customview.adapter.AdapterItem;
import com.app.dlna.dmc.gui.customview.adapter.CustomArrayAdapter;
import com.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.DevicesListener;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.SystemListener;
import com.app.dlna.dmc.processor.localdevice.service.LocalContentDirectoryService;
import com.app.dlna.dmc.processor.playlist.Playlist;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistManager;

public class DMRListActivity extends Activity implements SystemListener {
	private static final String TAG = DMRListActivity.class.getName();
	private UpnpProcessorImpl m_upnpProcessor;
	@SuppressWarnings("rawtypes")
	private DevicesListener m_devicesListener = new DevicesListener() {

		@Override
		public void onDeviceRemoved(final Device device) {
			DMRListActivity.this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_adapter.remove(new AdapterItem(device));
				}
			});
		}

		@Override
		public void onDeviceAdded(final Device device) {
			if (device instanceof RemoteDevice && device.getType().getType().equals("MediaRenderer"))
				DMRListActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						m_adapter.add(new AdapterItem(device));
					}
				});
		}

		@Override
		public void onDMSChanged() {

		}

		@Override
		public void onDMRChanged() {

		}
	};
	private CustomArrayAdapter m_adapter;
	private ListView m_listView;
	private String m_playToURI = "";
	private OnItemClickListener m_dmrClickListener = new OnItemClickListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Device remoteDMR = (Device) m_adapter.getItem(position).getData();
			m_upnpProcessor.setCurrentDMR(remoteDMR.getIdentity().getUdn());
			DIDLObject object = LocalContentDirectoryService.getDIDLObjectFromPath(m_playToURI);
			PlaylistProcessor processor = m_upnpProcessor.getPlaylistProcessor();
			if (null == processor) {
				Playlist unsaved = new Playlist();
				unsaved.setId(1);
				m_upnpProcessor.setPlaylistProcessor(PlaylistManager.getPlaylistProcessor(unsaved));
			}
			PlaylistItem item = m_upnpProcessor.getPlaylistProcessor().addDIDLObject(object);
			m_upnpProcessor.getPlaylistProcessor().setCurrentItem(item);
			m_upnpProcessor.getDMRProcessor().setURIandPlay(item);
			Intent intent = new Intent(DMRListActivity.this, MainActivity.class);
			intent.setAction(MainActivity.ACTION_PLAYTO);
			DMRListActivity.this.startActivity(intent);
			DMRListActivity.this.finish();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dmrlist);
		m_upnpProcessor = new UpnpProcessorImpl(DMRListActivity.this);
		m_upnpProcessor.bindUpnpService();

		m_listView = (ListView) findViewById(R.id.listView);
		m_adapter = new CustomArrayAdapter(DMRListActivity.this, 0);
		m_listView.setAdapter(m_adapter);
		m_listView.setOnItemClickListener(m_dmrClickListener);
		Intent intent = getIntent();
		if (intent != null) {
			Log.e(TAG, "intent = " + intent);
			Log.e(TAG, "extra = " + intent.getExtras().get(Intent.EXTRA_STREAM));
			Uri uri = ((Uri) intent.getExtras().get(Intent.EXTRA_STREAM));
			if (uri.toString().startsWith("content://")) {
				// Link from content provider
				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor cursor = managedQuery(uri, proj, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				m_playToURI = cursor.getString(column_index);
			} else {
				// Absolute path
				m_playToURI = ((Uri) intent.getExtras().get(Intent.EXTRA_STREAM)).getPath();
			}
		}
		Log.e(TAG, "Remote playtoURI = " + m_playToURI);
		PlaylistManager.RESOLVER = getContentResolver();
	}

	public void onCloseClick(View view) {
		this.finish();
	}

	public void onRefreshClick(View view) {
		if (m_upnpProcessor != null)
			m_upnpProcessor.refreshDevicesList();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onStartComplete() {
		m_upnpProcessor.addDevicesListener(m_devicesListener);
		for (Device device : m_upnpProcessor.getDMRList())
			if (device instanceof RemoteDevice)
				m_adapter.add(new AdapterItem(device));
	}

	@Override
	public void onStartFailed() {
		new AlertDialog.Builder(DMRListActivity.this).setMessage("Cannot start Upnp Service, please try again later")
				.setTitle("Upnp Service Info").setPositiveButton("OK", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						DMRListActivity.this.finish();
					}
				}).setCancelable(false).create().show();
	}

	@Override
	public void onNetworkChanged() {
		this.finish();
	}

	@Override
	public void onRouterError(String cause) {
		this.finish();
	}

	@Override
	public void onRouterEnabledEvent() {
		this.finish();
	}

	@Override
	public void onRouterDisabledEvent() {
		this.finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		m_upnpProcessor.removeDevicesListener(m_devicesListener);
		m_upnpProcessor.removeSystemListener(this);
		m_upnpProcessor.unbindUpnpService();
	}
}
