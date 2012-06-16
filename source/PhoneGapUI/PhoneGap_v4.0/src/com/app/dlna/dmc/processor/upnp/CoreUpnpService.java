package com.app.dlna.dmc.processor.upnp;

import java.net.NetworkInterface;

import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceConfiguration;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.android.AndroidWifiSwitchableRouter;
import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.model.ModelUtil;
import org.teleal.cling.model.meta.Device;
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
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import com.app.dlna.dmc.gui.AppPreference;
import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.phonegap.R;
import com.app.dlna.dmc.processor.impl.DMSProcessorImpl;
import com.app.dlna.dmc.processor.impl.PlaylistProcessorImpl;
import com.app.dlna.dmc.processor.impl.RemoteDMRProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.receiver.NetworkStateReceiver;
import com.app.dlna.dmc.processor.receiver.NetworkStateReceiver.RouterStateListener;

public class CoreUpnpService extends Service {
	@SuppressWarnings("rawtypes")
	private Device m_currentDMS;
	@SuppressWarnings("rawtypes")
	private Device m_currentDMR;
	public static final int NOTIFICATION = 1500;
	private UpnpService m_upnpService;
	private CoreUpnpServiceBinder binder = new CoreUpnpServiceBinder();
	private PlaylistProcessor m_playlistProcessor;
	private NotificationManager m_notificationManager;
	private DMSProcessor m_dmsProcessor;
	private DMRProcessor m_dmrProcessor;
	private CoreUpnpServiceListener m_upnpServiceListener;
	private WifiLock m_wifiLock;
	private WifiManager m_wifiManager;
	private ConnectivityManager m_connectivityManager;
	private boolean m_isInitialized;
	private NetworkStateReceiver m_networkReceiver;
	private RegistryListener m_registryListener;
	private WakeLock m_serviceWakeLock;

	@Override
	public void onCreate() {
		super.onCreate();
		m_isInitialized = false;
		PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
		m_serviceWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Media2Share WakeLock");
		m_serviceWakeLock.acquire();

		m_wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		m_connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			m_upnpService = new UpnpServiceImpl(createConfiguration(m_wifiManager)) {
				@Override
				protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
					AndroidWifiSwitchableRouter router = CoreUpnpService.this.createRouter(getConfiguration(), protocolFactory,
							m_wifiManager, m_connectivityManager);
					m_networkReceiver = new NetworkStateReceiver(router, new RouterStateListener() {

						@Override
						public void onRouterError(String cause) {
							if (m_upnpServiceListener != null)
								m_upnpServiceListener.onRouterError("No network found");
						}

						@Override
						public void onNetworkChanged(NetworkInterface ni) {
							if (m_upnpServiceListener != null) {
								m_upnpServiceListener.onNetworkChanged(ni);
							}
						}

						@Override
						public void onRouterEnabled() {
							if (m_upnpServiceListener != null)
								m_upnpServiceListener.onRouterEnabled();
						}

						@Override
						public void onRouterDisabled() {
							if (m_upnpServiceListener != null)
								m_upnpServiceListener.onRouterDisabled();
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

			m_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			m_playlistProcessor = new PlaylistProcessorImpl();
			showNotification();
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

	protected AndroidWifiSwitchableRouter createRouter(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory,
			WifiManager wifiManager, ConnectivityManager connectivityManager) {
		return new AndroidWifiSwitchableRouter(configuration, protocolFactory, wifiManager, connectivityManager);
	}

	@Override
	public void onDestroy() {
		m_serviceWakeLock.release();
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

		try {
			MainActivity.INSTANCE.EXEC.shutdownNow();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

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
					if (AppPreference.getKillProcessStatus())
						android.os.Process.killProcess(android.os.Process.myPid());
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
				m_dmsProcessor = new DMSProcessorImpl(m_currentDMS, getControlPoint());
			} else {
				Toast.makeText(getApplicationContext(), "Set DMS fail. Cannot get DMS info; UDN = " + uDN.toString(),
						Toast.LENGTH_SHORT).show();
				m_dmsProcessor = null;
			}
		}

		public void setCurrentDMR(UDN uDN) {
			if (m_dmrProcessor != null)
				m_dmrProcessor.dispose();
			m_dmrProcessor = null;
			m_currentDMR = m_upnpService.getRegistry().getDevice(uDN, true);
			if (m_currentDMR != null) {
				m_dmrProcessor = new RemoteDMRProcessorImpl(m_currentDMR, getControlPoint());
			} else {
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
			return m_upnpService != null ? m_upnpService.getConfiguration() : null;
		}

		public Registry getRegistry() {
			return m_upnpService != null ? m_upnpService.getRegistry() : null;
		}

		public ControlPoint getControlPoint() {
			return m_upnpService != null ? m_upnpService.getControlPoint() : null;
		}

		public void setProcessor(CoreUpnpServiceListener upnpServiceListener) {
			m_upnpServiceListener = upnpServiceListener;
		}

		public void addRegistryListener(RegistryListener listener) {
			m_registryListener = listener;
			m_upnpService.getRegistry().addListener(listener);
		}

		public void setDMSExported(boolean value) {
			if (m_upnpService != null)
				m_upnpService.getConfiguration().getStreamServerConfiguration().setExported(value);
		}
	}

	private void showNotification() {
		Notification notification = new Notification(R.drawable.ic_launcher, "CoreUpnpService started",
				System.currentTimeMillis());

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(CoreUpnpService.this, MainActivity.class), 0);

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
