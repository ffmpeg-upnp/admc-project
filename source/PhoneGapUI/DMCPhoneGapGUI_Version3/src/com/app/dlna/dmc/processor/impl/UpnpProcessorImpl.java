package com.app.dlna.dmc.processor.impl;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.app.dlna.dmc.gui.UpnpListenerDroidGapActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.DownloadProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor;
import com.app.dlna.dmc.processor.upnp.CoreUpnpService;
import com.app.dlna.dmc.processor.upnp.CoreUpnpService.CoreUpnpServiceBinder;
import com.app.dlna.dmc.processor.upnp.CoreUpnpService.CoreUpnpServiceListener;

public class UpnpProcessorImpl implements UpnpProcessor, RegistryListener, CoreUpnpServiceListener {
	private static String TAG = UpnpProcessorImpl.class.getName();

	private Activity m_activity;

	private CoreUpnpServiceBinder m_upnpService;

	private ServiceConnection m_serviceConnection;

	private List<DevicesListener> m_devicesListeners;

	private List<SystemListener> m_systemListeners;

	private DownloadProcessor m_downloadProcessor;

	public UpnpProcessorImpl(UpnpListenerDroidGapActivity activity) {
		m_activity = activity;
		m_systemListeners = new ArrayList<SystemListener>();
		m_systemListeners.add(activity);
		m_devicesListeners = new ArrayList<DevicesListener>();
		m_downloadProcessor = new DownloadProcessorImpl(activity);
	}

	public void bindUpnpService() {

		m_serviceConnection = new ServiceConnection() {

			public void onServiceDisconnected(ComponentName name) {
				m_upnpService = null;
			}

			public void onServiceConnected(ComponentName name, IBinder service) {
				m_upnpService = (CoreUpnpServiceBinder) service;
				if (m_upnpService.isInitialized()) {
					m_upnpService.addRegistryListener(UpnpProcessorImpl.this);
					Log.i(TAG, "Upnp Service Ready");
					fireOnStartCompleteEvent();
					m_upnpService.setProcessor(UpnpProcessorImpl.this);
					m_upnpService.getControlPoint().search();
				} else {
					m_upnpService = null;
					fireOnStartFailedEvent();
				}
			}
		};

		Intent intent = new Intent(m_activity, CoreUpnpService.class);
		m_activity.getApplicationContext().bindService(intent, m_serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public void unbindUpnpService() {
		try {
			Log.i(TAG, "Unbind to service");
			if (m_serviceConnection != null) {
				try {
					m_activity.getApplicationContext().unbindService(m_serviceConnection);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			if (m_downloadProcessor != null) {
				m_downloadProcessor.stopAllDownloads();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void searchAll() {
		if (m_upnpService != null) {
			// Log.e(TAG, "Search invoke");
			m_upnpService.getRegistry().removeAllRemoteDevices();
			m_upnpService.getControlPoint().search();
		}
	}

	public void addDevicesListener(DevicesListener listener) {
		synchronized (m_devicesListeners) {
			if (!m_devicesListeners.contains(listener)) {
				m_devicesListeners.add(listener);
			}
		}
	}

	public void removeDevicesListener(DevicesListener listener) {
		synchronized (m_devicesListeners) {
			if (m_devicesListeners.contains(listener)) {
				m_devicesListeners.remove(listener);
			}
		}
	}

	public Registry getRegistry() {
		return m_upnpService.getRegistry();
	}

	public ControlPoint getControlPoint() {
		return m_upnpService != null ? m_upnpService.getControlPoint() : null;
	}

	private void fireOnStartCompleteEvent() {
		synchronized (m_systemListeners) {
			for (SystemListener listener : m_systemListeners) {
				listener.onStartComplete();
			}
		}
	}

	private void fireOnStartFailedEvent() {
		synchronized (m_systemListeners) {
			for (SystemListener listener : m_systemListeners) {
				listener.onStartFailed();
			}
		}
	}

	private void fireOnRouterErrorEvent(String cause) {
		synchronized (m_systemListeners) {
			for (SystemListener listener : m_systemListeners) {
				listener.onRouterError(cause);
			}
		}
	}

	private void fireOnNetworkChangedEvent() {
		synchronized (m_systemListeners) {
			for (SystemListener listener : m_systemListeners) {
				listener.onNetworkChanged();
			}
		}
	}

	private void fireOnRouterDisabledEvent() {
		synchronized (m_systemListeners) {
			for (SystemListener listener : m_systemListeners) {
				listener.onRouterDisabledEvent();
			}
		}
	}

	private void fireOnRouterEnabledEvent() {
		synchronized (m_systemListeners) {
			for (SystemListener listener : m_systemListeners) {
				listener.onRouterEnabledEvent();
			}
		}
	}

	@Override
	public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
	}

	@Override
	public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
	}

	@Override
	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		fireDeviceAddedEvent(device);
	}

	@Override
	public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
	}

	@Override
	public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
		fireDeviceRemovedEvent(device);
	}

	@Override
	public void localDeviceAdded(Registry registry, LocalDevice device) {
		// Log.e(TAG, "Local Device Add:" + device.toString());
		fireDeviceAddedEvent(device);
	}

	@Override
	public void localDeviceRemoved(Registry registry, LocalDevice device) {
		// Log.e(TAG, "Local Device Removed:" + device.toString());
		fireDeviceRemovedEvent(device);
	}

	@Override
	public void beforeShutdown(Registry registry) {
	}

	@Override
	public void afterShutdown() {
	}

	@SuppressWarnings("rawtypes")
	private void fireDeviceAddedEvent(Device device) {
		synchronized (m_devicesListeners) {
			for (DevicesListener listener : m_devicesListeners) {
				listener.onDeviceAdded(device);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void fireDeviceRemovedEvent(Device device) {
		synchronized (m_devicesListeners) {
			for (DevicesListener listener : m_devicesListeners) {
				listener.onDeviceRemoved(device);
			}
		}
	}

	private void fireOnDMSChangedEvent() {
		synchronized (m_devicesListeners) {
			for (DevicesListener listener : m_devicesListeners) {
				listener.onDMSChanged();
			}
		}
	}

	private void fireOnDMRChangedEvent() {
		synchronized (m_devicesListeners) {
			for (DevicesListener listener : m_devicesListeners) {
				listener.onDMRChanged();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection<Device> getDMSList() {
		if (m_upnpService != null)
			return m_upnpService.getRegistry().getDevices(new DeviceType("schemas-upnp-org", "MediaServer"));
		return new ArrayList<Device>();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection<Device> getDMRList() {
		if (m_upnpService != null)
			return m_upnpService.getRegistry().getDevices(new DeviceType("schemas-upnp-org", "MediaRenderer"));
		return new ArrayList<Device>();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setCurrentDMS(UDN uDN) {
		m_upnpService.setCurrentDMS(uDN);
		fireOnDMSChangedEvent();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setCurrentDMR(UDN uDN) {
		// Device currentDMR = m_upnpService.getCurrentDMR();
		// m_upnpService.setCurrentDMR(uDN);
		// if (currentDMR == null
		// || (m_upnpService.getCurrentDMR() != null &&
		// !currentDMR.getIdentity().equals(
		// m_upnpService.getCurrentDMR().getIdentity()))) {
		// fireOnDMRChangedEvent();
		// }
		// Device currentDMR = m_upnpService.getCurrentDMR();
		// Device newDMR = m_upnpService.getRegistry().getDevice(uDN, true);
		//
		// if (newDMR != null) {
		// if (currentDMR == null) {
		// m_upnpService.setCurrentDMR(uDN);
		// fireOnDMRChangedEvent();
		// } else {
		// if (!currentDMR.getIdentity().equals(newDMR)) {
		// fireOnDMRChangedEvent();
		// }
		// }
		// }
		Device currentDMR = m_upnpService.getCurrentDMR();
		Device newDMR = m_upnpService.getRegistry().getDevice(uDN, true);

		if (currentDMR == null || !newDMR.getIdentity().equals(currentDMR.getIdentity())) {
			m_upnpService.setCurrentDMR(uDN);
		}
		fireOnDMRChangedEvent();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Device getCurrentDMS() {
		return m_upnpService != null ? m_upnpService.getCurrentDMS() : null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Device getCurrentDMR() {
		return m_upnpService != null ? m_upnpService.getCurrentDMR() : null;
	}

	@Override
	public PlaylistProcessor getPlaylistProcessor() {
		return m_upnpService != null ? m_upnpService.getPlaylistProcessor() : null;
	}

	@Override
	public DMSProcessor getDMSProcessor() {
		return m_upnpService != null ? m_upnpService.getDMSProcessor() : null;
	}

	@Override
	public DMRProcessor getDMRProcessor() {
		return m_upnpService != null ? m_upnpService.getDMRProcessor() : null;
	}

	@Override
	public void onNetworkChanged(NetworkInterface ni) {
		Log.w(TAG, "NetworkInterface changed to: " + ni.getDisplayName());
		fireOnNetworkChangedEvent();
	}

	@Override
	public void onRouterError(String message) {
		Log.e(TAG, "Router error " + message);
		fireOnRouterErrorEvent(message);
	}

	@Override
	public void onRouterDisabled() {
		fireOnRouterDisabledEvent();
	}

	@Override
	public void onRouterEnabled() {
		fireOnRouterEnabledEvent();
	}

	@Override
	public DownloadProcessor getDownloadProcessor() {
		return m_downloadProcessor;
	}

	@Override
	public void setPlaylistProcessor(PlaylistProcessor playlistProcessor) {
		m_upnpService.setPlaylistProcessor(playlistProcessor);
	}

	@Override
	public void addSystemListener(SystemListener listener) {
		synchronized (m_systemListeners) {
			if (!m_systemListeners.contains(listener))
				m_systemListeners.add(listener);
		}
	}

	@Override
	public void removeSystemListener(SystemListener listener) {
		synchronized (m_systemListeners) {
			if (m_systemListeners.contains(listener))
				m_systemListeners.add(listener);
		}

	}

	@Override
	public void refreshDevicesList() {
		m_upnpService.getRegistry().removeAllRemoteDevices();
		m_upnpService.getControlPoint().search();
	}

}
