package com.app.dlna.dmc.processor.upnp;

import java.net.NetworkInterface;

import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceConfiguration;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.android.AndroidWifiSwitchableRouter;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.ModelUtil;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.ManufacturerDetails;
import org.teleal.cling.model.meta.ModelDetails;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.transport.Router;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.cache.Cache;
import com.app.dlna.dmc.processor.http.HTTPServerData;
import com.app.dlna.dmc.processor.http.MainHttpProcessor;
import com.app.dlna.dmc.processor.impl.DMSProcessorImpl;
import com.app.dlna.dmc.processor.impl.LocalDMRProcessorImpl;
import com.app.dlna.dmc.processor.impl.RemoteDMRProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.localdevice.service.LocalContentDirectoryService;
import com.app.dlna.dmc.processor.receiver.NetworkStateReceiver;
import com.app.dlna.dmc.processor.receiver.NetworkStateReceiver.RouterStateListener;
import com.app.dlna.dmc.utility.Utility;

public class CoreUpnpService extends Service {
	public static final int NOTIFICATION = 1500;
	private static final String TAG = CoreUpnpService.class.getName();
	private MainHttpProcessor m_httpThread;
	private UpnpService m_upnpService;
	private CoreUpnpServiceBinder binder = new CoreUpnpServiceBinder();
	@SuppressWarnings("rawtypes")
	private Device m_currentDMS;
	@SuppressWarnings("rawtypes")
	private Device m_currentDMR;
	private PlaylistProcessor m_playlistProcessor;
	private NotificationManager m_notificationManager;
	private DMSProcessor m_dmsProcessor;
	private DMRProcessor m_dmrProcessor;
	private CoreUpnpServiceListener m_upnpProcessor;
	private WifiLock m_wifiLock;
	private WifiManager m_wifiManager;
	private ConnectivityManager m_connectivityManager;
	private boolean m_isInitialized;
	private NetworkStateReceiver m_networkReceiver;
	private UDN m_localDMS_UDN = null;
	private UDN m_localDMR_UDN = null;
	private RegistryListener m_registryListener;

	@Override
	public void onCreate() {
		super.onCreate();
		m_isInitialized = false;
		m_wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		m_connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			m_upnpService = new UpnpServiceImpl(createConfiguration(m_wifiManager)) {
				@Override
				protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
					AndroidWifiSwitchableRouter router = CoreUpnpService.this.createRouter(getConfiguration(),
							protocolFactory, m_wifiManager, m_connectivityManager);
					m_networkReceiver = new NetworkStateReceiver(router, new RouterStateListener() {

						@Override
						public void onRouterError(String cause) {
							Log.e(TAG, "Router error: " + cause);
							if (m_upnpProcessor != null)
								m_upnpProcessor.onRouterError("No network found");
						}

						@Override
						public void onNetworkChanged(NetworkInterface ni) {
							Log.w(TAG, "Network interface changed");
							if (m_upnpProcessor != null) {
								m_upnpProcessor.onNetworkChanged(ni);
							}
						}

						@Override
						public void onRouterEnabled() {
							if (m_upnpProcessor != null)
								m_upnpProcessor.onRouterEnabled();
						}

						@Override
						public void onRouterDisabled() {
							if (m_upnpProcessor != null)
								m_upnpProcessor.onRouterDisabled();
						}
					});
					if (!ModelUtil.ANDROID_EMULATOR) {
						IntentFilter filter = new IntentFilter();
						filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
						filter.addAction("	");
						registerReceiver(m_networkReceiver, filter);
					}
					return router;
				}
			};
			m_isInitialized = true;
		} catch (Exception ex) {
			m_isInitialized = false;
		}

		if (m_isInitialized) {
			// prevent wifi sleep when screen
			m_wifiLock = m_wifiManager.createWifiLock(3, "UpnpWifiLock");
			m_wifiLock.acquire();

			if (m_wifiManager.isWifiEnabled()
					&& m_connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
				HTTPServerData.HOST = Utility.intToIp(m_wifiManager.getDhcpInfo().ipAddress);
				Log.i(TAG, "Host = " + HTTPServerData.HOST);
			} else {
				HTTPServerData.HOST = null;
				Log.i(TAG, "Host = null");
			}

			m_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			m_httpThread = new MainHttpProcessor();
			m_httpThread.start();
			m_playlistProcessor = null;
			LocalContentDirectoryService.scanMedia(CoreUpnpService.this);
			showNotification();
			startLocalDMS();
			startLocalDMR();
		}
	}

	protected AndroidUpnpServiceConfiguration createConfiguration(WifiManager wifiManager) {
		return new AndroidUpnpServiceConfiguration(wifiManager, m_connectivityManager) {
			@Override
			public ServiceType[] getExclusiveServiceTypes() {
				return new ServiceType[] { new UDAServiceType("AVTransport"), new UDAServiceType("ContentDirectory"),
						new UDAServiceType("RenderingControl") };
			}
		};
	}

	protected AndroidWifiSwitchableRouter createRouter(UpnpServiceConfiguration configuration,
			ProtocolFactory protocolFactory, WifiManager wifiManager, ConnectivityManager connectivityManager) {
		return new AndroidWifiSwitchableRouter(configuration, protocolFactory, wifiManager, connectivityManager);
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(m_networkReceiver);
		} catch (Exception ex) {

		}

		if (m_dmsProcessor != null)
			m_dmsProcessor.dispose();

		if (m_dmrProcessor != null)
			m_dmrProcessor.dispose();
		if (m_wifiLock != null)
			try {
				m_wifiLock.release();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		if (m_httpThread != null)
			m_httpThread.stopHttpThread();

		try {
			MainActivity.INSTANCE.EXEC.shutdownNow();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Cache.clear();
		if (m_upnpService != null) {
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					try {
						m_upnpService.getRegistry().removeAllLocalDevices();
						m_upnpService.getRegistry().removeAllRemoteDevices();
						m_upnpService.getRegistry().removeListener(m_registryListener);
						m_upnpService.shutdown();
						m_upnpService = null;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					return null;
				}

				protected void onPostExecute(Void result) {
					if (m_notificationManager != null)
						m_notificationManager.cancel(NOTIFICATION);
					// android.os.Process.killProcess(android.os.Process.myPid());
				};
			}.execute(new Void[] {});
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class CoreUpnpServiceBinder extends android.os.Binder implements AndroidUpnpService {

		public boolean isInitialized() {
			return m_isInitialized;
		}

		public PlaylistProcessor getPlaylistProcessor() {
			return m_playlistProcessor;
		}

		public DMSProcessor getDMSProcessor() {
			return m_dmsProcessor;
		}

		public DMRProcessor getDMRProcessor() {
			return m_dmrProcessor;
		}

		public void setCurrentDMS(UDN uDN) {
			m_dmsProcessor = null;
			m_currentDMS = m_upnpService.getRegistry().getDevice(uDN, true);
			if (m_currentDMS != null) {
				Log.d(TAG, "CURRENT DMS:" + m_currentDMS.toString());
				m_dmsProcessor = new DMSProcessorImpl(m_currentDMS, getControlPoint());
			} else {
				Log.e(TAG, "GET DMS FAIL:" + uDN.toString());
				Toast.makeText(getApplicationContext(), "Set DMS fail. Cannot get DMS info; UDN = " + uDN.toString(),
						Toast.LENGTH_SHORT).show();
				m_dmsProcessor = null;
			}
		}

		public void setCurrentDMR(UDN uDN) {
			Log.e(TAG, "Set current DMR = " + uDN);
			if (m_dmrProcessor != null)
				m_dmrProcessor.dispose();
			m_dmrProcessor = null;
			m_currentDMR = m_upnpService.getRegistry().getDevice(uDN, true);
			if (m_currentDMR != null) {
				Log.d(TAG, "CURRENT DMR:" + m_currentDMR.toString());
				if (m_currentDMR instanceof LocalDevice)
					m_dmrProcessor = new LocalDMRProcessorImpl(CoreUpnpService.this);
				else
					m_dmrProcessor = new RemoteDMRProcessorImpl(m_currentDMR, getControlPoint());
			} else {
				Log.e(TAG, "GET DMR FAIL:" + uDN.toString());
				Toast.makeText(getApplicationContext(), "Set DMR fail. Cannot get DMR info; UDN = " + uDN.toString(),
						Toast.LENGTH_SHORT).show();
				m_dmrProcessor = null;
			}
		}

		@SuppressWarnings("rawtypes")
		public Device getCurrentDMS() {
			return m_currentDMS;
		}

		@SuppressWarnings("rawtypes")
		public Device getCurrentDMR() {
			return m_currentDMR;
		}

		public UpnpService get() {
			return m_upnpService;
		}

		public UpnpServiceConfiguration getConfiguration() {
			return m_upnpService.getConfiguration();
		}

		public Registry getRegistry() {
			return m_upnpService.getRegistry();
		}

		public ControlPoint getControlPoint() {
			return m_upnpService.getControlPoint();
		}

		public void setProcessor(CoreUpnpServiceListener upnpProcessor) {
			m_upnpProcessor = upnpProcessor;
		}

		public void setPlaylistProcessor(PlaylistProcessor playlistProcessor) {
			if (m_playlistProcessor != null)
				m_playlistProcessor.saveState();
			m_playlistProcessor = playlistProcessor;
		}

		public void addRegistryListener(RegistryListener listener) {
			m_registryListener = listener;
			m_upnpService.getRegistry().addListener(listener);
		}
	}

	@SuppressWarnings("unchecked")
	private void startLocalDMS() {
		try {
			String deviceName = Build.MODEL.toUpperCase() + " " + Build.DEVICE.toUpperCase() + " - DMS";
			String MACAddress = m_wifiManager.getConnectionInfo().getMacAddress();
			Log.i(TAG, "Local DMS: Device name = " + deviceName + ";MAC = " + MACAddress);
			String hashUDN = Utility.getMD5(deviceName + "-" + MACAddress + "-LocalDMS");
			Log.i(TAG, "Hash UDN = " + hashUDN);
			String uDNString = hashUDN.substring(0, 8) + "-" + hashUDN.substring(8, 12) + "-"
					+ hashUDN.substring(12, 16) + "-" + hashUDN.substring(16, 20) + "-" + hashUDN.substring(20);
			LocalService<LocalContentDirectoryService> localService = new AnnotationLocalServiceBinder()
					.read(LocalContentDirectoryService.class);
			localService.setManager(new DefaultServiceManager<LocalContentDirectoryService>(localService,
					LocalContentDirectoryService.class));
			m_localDMS_UDN = new UDN(uDNString);
			DeviceIdentity identity = new DeviceIdentity(m_localDMS_UDN);
			DeviceType type = new DeviceType("schemas-upnp-org", "MediaServer");
			DeviceDetails details = new DeviceDetails(deviceName,
					new ManufacturerDetails("Android Digital Controller"), new ModelDetails("v1.0"), "", "");

			LocalDevice localDevice = new LocalDevice(identity, type, details, localService);

			m_upnpService.getRegistry().addDevice(localDevice);
			Log.d(TAG, "Create Local Device complete");
		} catch (Exception ex) {
			Log.d(TAG, "Cannot create Local Device");
			ex.printStackTrace();
		}
	}

	private void startLocalDMR() {
		try {
			String deviceName = Build.MODEL.toUpperCase() + " " + Build.DEVICE.toUpperCase() + " - DMR";
			String MACAddress = m_wifiManager.getConnectionInfo().getMacAddress();
			Log.i(TAG, "Local DMR: Device name = " + deviceName + ";MAC = " + MACAddress);
			String hashUDN = Utility.getMD5(deviceName + "-" + MACAddress + "-LocalDMR");
			Log.i(TAG, "Hash UDN = " + hashUDN);
			String uDNString = hashUDN.substring(0, 8) + "-" + hashUDN.substring(8, 12) + "-"
					+ hashUDN.substring(12, 16) + "-" + hashUDN.substring(16, 20) + "-" + hashUDN.substring(20);
			m_localDMR_UDN = new UDN(uDNString);
			DeviceIdentity identity = new DeviceIdentity(m_localDMR_UDN);
			DeviceType type = new DeviceType("schemas-upnp-org", "MediaRenderer");
			DeviceDetails details = new DeviceDetails(deviceName,
					new ManufacturerDetails("Android Digital Controller"), new ModelDetails("v1.0"), "", "");

			LocalDevice localDevice = new LocalDevice(identity, type, details, new LocalService[0]);

			m_upnpService.getRegistry().addDevice(localDevice);
			Log.d(TAG, "Create Local Device complete");
		} catch (Exception ex) {
			Log.d(TAG, "Cannot create Local Device");
			ex.printStackTrace();
		}
	}

	private void showNotification() {
		Notification notification = new Notification(R.drawable.ic_launcher, "CoreUpnpService started",
				System.currentTimeMillis());

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(CoreUpnpService.this,
				MainActivity.class), 0);

		notification.setLatestEventInfo(this, "CoreUpnpService", "Service is running", contentIntent);
		notification.flags = Notification.FLAG_ONGOING_EVENT;

		m_notificationManager.notify(NOTIFICATION, notification);
	}

	public interface CoreUpnpServiceListener {
		void onNetworkChanged(NetworkInterface ni);

		void onRouterError(String message);

		void onRouterDisabled();

		void onRouterEnabled();
	}
}
