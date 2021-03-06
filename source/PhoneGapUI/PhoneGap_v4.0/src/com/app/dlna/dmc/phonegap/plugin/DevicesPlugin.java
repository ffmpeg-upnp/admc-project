package com.app.dlna.dmc.phonegap.plugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.types.UDN;

import android.util.Log;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor.DMRProcessorListner;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.DevicesListener;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.utility.Utility;
import com.phonegap.api.PhonegapActivity;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class DevicesPlugin extends Plugin implements DevicesListener {
	private static final String TAG = DevicesPlugin.class.getSimpleName();
	public static final String ACTION_REFRESH_DMS = "refreshDMS";
	public static final String ACTION_REFRESH_DMR = "refreshDMR";
	public static final String ACTION_SET_DMS = "setDMS";
	public static final String ACTION_SET_DMR = "setDMR";

	@SuppressWarnings("rawtypes")
	private List<Device> m_dms_list = new ArrayList<Device>();
	@SuppressWarnings("rawtypes")
	private List<Device> m_dmr_list = new ArrayList<Device>();
	PhonegapActivity m_activity;

	public DevicesPlugin() {
	}

	@SuppressWarnings("rawtypes")
	public DevicesPlugin(PhonegapActivity ctx) {
		super.setContext(ctx);
		m_activity = ctx;
		Log.e(TAG, "Set context");
		for (Device device : MainActivity.UPNP_PROCESSOR.getDMSList()) {
			addDMS(device);
		}
		for (Device device : MainActivity.UPNP_PROCESSOR.getDMRList()) {
			addDMR(device);
			if (device instanceof LocalDevice)
				MainActivity.UPNP_PROCESSOR.setCurrentDMR(device.getIdentity().getUdn());
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public PluginResult execute(String action, JSONArray data, String callID) {

		PluginResult result = new PluginResult(Status.OK);
		if (ACTION_REFRESH_DMS.equals(action)) {
			Log.i(TAG, "Call refresh DMS");
			sendJavascript("clearDMSList();");
			m_dms_list.clear();
			for (Device device : MainActivity.UPNP_PROCESSOR.getDMSList()) {
				addDMS(device);
			}
		} else if (ACTION_REFRESH_DMR.equals(action)) {
			Log.i(TAG, "Call refresh DMR");
			sendJavascript("clearDMRList();");
			m_dmr_list.clear();
			for (Device device : MainActivity.UPNP_PROCESSOR.getDMRList()) {
				addDMR(device);
			}
		} else if (ACTION_SET_DMS.equals(action)) {
			Log.e(TAG, "Call SetDMS");
			try {
				setDMS(data.getString(0));
			} catch (Exception ex) {
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else if (ACTION_SET_DMR.equals(action)) {
			Log.e(TAG, "Call SetDMR");
			try {
				setDMR(data.getString(0));
			} catch (Exception ex) {
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	private void setDMR(String udn) {
		MainActivity.UPNP_PROCESSOR.setCurrentDMR(new UDN(udn));
		Device device = MainActivity.UPNP_PROCESSOR.getCurrentDMR();
		String jsCommand = "";
		if (device != null) {
			MainActivity.UPNP_PROCESSOR.getDMRProcessor().addListener(dMRListner);
			jsCommand += "setCurrentDMR('" + device.getIdentity().getUdn().getIdentifierString() + "');";
			jsCommand += "playlist_updateDMRName('" + device.getDetails().getFriendlyName() + "');";
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			PlaylistItem currentItem = playlistProcessor.getCurrentItem();
			if (currentItem != null) {
				MainActivity.UPNP_PROCESSOR.getDMRProcessor().setURIandPlay(currentItem);
			}
		} else {
			Log.i(TAG, "Selected device is null");
			jsCommand += "playlist_updateDMRName('Please chose a DMR to play');";
		}
		sendJavascript(jsCommand);
	}

	DMRProcessorListner dMRListner = new DMRProcessorListner() {
		PlaylistPlugin playlistPlugin = new PlaylistPlugin(MainActivity.INSTANCE);

		@Override
		public void onUpdatePosition(long current, long max) {
			String jsCommand = "";
			jsCommand += "playlist_updateDurationSeekbar(" + current + ", " + max + ");";
			jsCommand += "playlist_updateDurationString('" + Utility.getTimeString(current) + " / " + Utility.getTimeString(max)
					+ "');";
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (dmrProcessor != null)
				jsCommand += "playlist_updateVolumeSeekbar(" + dmrProcessor.getVolume() + ", " + dmrProcessor.getMaxVolume()
						+ ");";
			playlistPlugin.sendJavascript(jsCommand);
		}

		@Override
		public void onStoped() {
			playlistPlugin.sendJavascript("playlist_onStop();");
		}

		@Override
		public void onPlaying() {
			playlistPlugin.sendJavascript("playlist_onPlaying();");
		}

		@Override
		public void onPaused() {
			playlistPlugin.sendJavascript("playlist_onPause();");
		}

		@Override
		public void onErrorEvent(String error) {

		}

		@Override
		public void onCheckURLStart() {

		}

		@Override
		public void onCheckURLEnd() {

		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onActionFail(Action actionCallback, UpnpResponse response, String cause) {

		}
	};

	private void setDMS(String udn) {
		MainActivity.UPNP_PROCESSOR.setCurrentDMS(new UDN(udn));
		sendJavascript("clearDMSList();");
		try {
			new LibraryPlugin(DevicesPlugin.this.ctx).execute(LibraryPlugin.ACTION_BROWSE, new JSONArray("['0']"), "0");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onDeviceAdded(Device device) {
		if (device.getType().getNamespace().equals("schemas-upnp-org")) {
			if (device.getType().getType().equals("MediaServer")) {
				addDMS(device);
			} else if (device.getType().getType().equals("MediaRenderer")) {
				addDMR(device);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void addDMR(Device device) {
		if (!m_dmr_list.contains(device)) {
			m_dmr_list.add(device);
			String jsonString = createDeviceElement(device, "dmr");
			sendJavascript("add_device(" + jsonString + ",'dmr');");
		}
	}

	@SuppressWarnings("rawtypes")
	private void addDMS(Device device) {
		if (!m_dms_list.contains(device)) {
			m_dms_list.add(device);
			String jsonString = createDeviceElement(device, "dms");
			Log.e(TAG, "JsonString = " + jsonString);
			sendJavascript("add_device(" + jsonString + ",'dms');");
		}
	}

	@SuppressWarnings("rawtypes")
	private String createDeviceElement(Device device, String type) {
		JSONObject jsonDevice = new JSONObject();
		final String udn = device.getIdentity().getUdn().getIdentifierString();
		String deviceImage = "";
		String deviceAddress = "";
		String deviceName = "";
		deviceName = device.getDetails().getFriendlyName();
		if (device instanceof RemoteDevice) {
			deviceAddress = ((RemoteDevice) device).getIdentity().getDescriptorURL().getAuthority();
			final Icon[] icons = device.getIcons();
			if (icons != null && icons.length > 0 && icons[0] != null && icons[0].getUri() != null) {
				final RemoteDevice remoteDevice = (RemoteDevice) device;
				deviceImage = remoteDevice.getIdentity().getDescriptorURL().getProtocol() + "://"
						+ remoteDevice.getIdentity().getDescriptorURL().getAuthority() + icons[0].getUri().toString();
				try {
					URI uri = URI.create(deviceImage);
					if (uri == null || uri.getPath() == null || uri.getPath().trim().isEmpty())
						deviceImage = "img/ic_device_unknow_player.png";
				} catch (Exception e) {
					deviceImage = "img/ic_device_unknow_player.png";
				}
			} else {
				deviceImage = "img/ic_device_unknow_player.png";
			}
		} else {
			deviceAddress = "Local Device";
			if (type.equals("dms"))
				deviceImage = "img/ic_device_unknow_server.png";
			else
				deviceImage = "img/ic_device_unknow_player.png";
		}
		try {
			jsonDevice.put("name", deviceName);
			jsonDevice.put("type", type);
			jsonDevice.put("udn", udn);
			jsonDevice.put("icon", deviceImage);
			jsonDevice.put("address", deviceAddress);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonDevice.toString();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onDeviceRemoved(Device device) {
		Log.e(TAG, "Device removed: " + device);
		if (device.getType().getNamespace().equals("schemas-upnp-org")) {
			if (device.getType().getType().equals("MediaServer")) {
				removeDMS(device);
			} else if (device.getType().getType().equals("MediaRenderer")) {
				removeDMR(device);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void removeDMR(Device device) {
		sendJavascript("remove_dmr('" + device.getIdentity().getUdn().getIdentifierString() + "');");
		m_dmr_list.remove(device);
	}

	@SuppressWarnings("rawtypes")
	private void removeDMS(Device device) {
		sendJavascript("remove_dms('" + device.getIdentity().getUdn().getIdentifierString() + "');");
		m_dms_list.remove(device);
	}

	@Override
	public void onDMSChanged() {

	}

	@Override
	public void onDMRChanged() {

	}
}
