package com.app.dlna.dmc.gui.devices;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.types.UDN;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.app.dlna.controller.nativegui.R;
import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.gui.abstractactivity.UpnpListenerActivity;

public class DevicesActivity extends UpnpListenerActivity {
	private static final String TAG = DevicesActivity.class.getName();

	private ListView m_dmrList;
	private ListView m_dmsList;
	private DeviceArrayAdapter m_dmrAdapter;
	private DeviceArrayAdapter m_dmsAdapter;
	private ViewPager m_pager;
	private View m_dms_page;
	private View m_dmr_page;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Devices onCreate");
		setContentView(R.layout.devices_activity);

		m_dms_page = getLayoutInflater().inflate(R.layout.dms_page, null);
		m_dmsList = (ListView) m_dms_page.findViewById(R.id.dmsList);
		m_dmsAdapter = new DeviceArrayAdapter(DevicesActivity.this, 0);
		m_dmsList.setOnItemClickListener(onDMSClick);
		m_dmsList.setAdapter(m_dmsAdapter);

		m_dmr_page = getLayoutInflater().inflate(R.layout.dmr_page, null);
		m_dmrList = (ListView) m_dmr_page.findViewById(R.id.dmrList);
		m_dmrAdapter = new DeviceArrayAdapter(DevicesActivity.this, 0);
		m_dmrList.setOnItemClickListener(onDMRClick);
		m_dmrList.setAdapter(m_dmrAdapter);

		m_pager = (ViewPager) findViewById(R.id.viewPager);

		m_pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});
		PagerAdapter adapter = new PagerAdapter() {
			@Override
			public void destroyItem(ViewGroup container, int position, Object view) {
				((ViewPager) container).removeView((View) view);
			}

			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				if (position == 0) {
					((ViewPager) container).addView(m_dms_page);
					return m_dms_page;
				} else if (position == 1) {
					((ViewPager) container).addView(m_dmr_page);
					return m_dmr_page;
				} else {
					return null;
				}

			}

			@Override
			public boolean isViewFromObject(View view, Object key) {
				return view == key;
			}

			@Override
			public CharSequence getPageTitle(int position) {
				return new String[] { "Media Server", "Media Renderer" }[position];
			}

			@Override
			public int getCount() {
				return 2;
			}
		};
		m_pager.setAdapter(adapter);

		// restoreState();

	}

	private OnItemClickListener onDMRClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
			synchronized (m_dmrAdapter) {
				UDN udn = m_dmrAdapter.getItem(position).getIdentity().getUdn();
				MainActivity.UPNP_PROCESSOR.setCurrentDMR(udn);
				synchronized (m_dmrAdapter) {
					m_dmrAdapter.setCurrentDeviceUDN(udn.getIdentifierString());
					m_dmrAdapter.notifyDataSetChanged();
				}
			}
		}

	};

	private OnItemClickListener onDMSClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
			synchronized (m_dmsAdapter) {
				UDN udn = m_dmsAdapter.getItem(position).getIdentity().getUdn();
				MainActivity.UPNP_PROCESSOR.setCurrentDMS(udn);
				synchronized (m_dmsAdapter) {
					m_dmsAdapter.setCurrentDeviceUDN(udn.getIdentifierString());
					m_dmsAdapter.notifyDataSetChanged();
				}
			}
		}

	};

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "Devices onResume");
		MainActivity.UPNP_PROCESSOR.addListener(DevicesActivity.this);
		refresh();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "Devices onPause");
		MainActivity.UPNP_PROCESSOR.removeListener(DevicesActivity.this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Devices onDestroy");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onDeviceAdded(Device device) {
		super.onDeviceAdded(device);
		if (device.getType().getNamespace().equals("schemas-upnp-org")) {
			if (device.getType().getType().equals("MediaServer")) {
				addDMS(device);
			} else if (device.getType().getType().equals("MediaRenderer")) {
				addDMR(device);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onDeviceRemoved(Device device) {
		super.onDeviceRemoved(device);
		if (device.getType().getNamespace().equals("schemas-upnp-org")) {
			if (device.getType().getType().equals("MediaServer")) {
				removeDMS(device);
			} else if (device.getType().getType().equals("MediaRenderer")) {
				removeDMR(device);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void refresh() {
		synchronized (m_dmsAdapter) {
			m_dmsAdapter.clear();
			if (MainActivity.UPNP_PROCESSOR.getCurrentDMS() != null) {
				m_dmsAdapter.setCurrentDeviceUDN(MainActivity.UPNP_PROCESSOR.getCurrentDMS().getIdentity().getUdn()
						.getIdentifierString());
			} else {
				m_dmsAdapter.setCurrentDeviceUDN("");
			}
		}

		synchronized (m_dmrAdapter) {
			m_dmrAdapter.clear();
			if (MainActivity.UPNP_PROCESSOR.getCurrentDMR() != null) {
				m_dmrAdapter.setCurrentDeviceUDN(MainActivity.UPNP_PROCESSOR.getCurrentDMR().getIdentity().getUdn()
						.getIdentifierString());
			} else {
				m_dmrAdapter.setCurrentDeviceUDN("");
			}
		}

		for (Device device : MainActivity.UPNP_PROCESSOR.getDMSList()) {
			addDMS(device);
		}

		for (Device device : MainActivity.UPNP_PROCESSOR.getDMRList()) {
			addDMR(device);
		}
	}

	@SuppressWarnings("rawtypes")
	private void addDMR(final Device device) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (m_dmrAdapter) {
					if (device instanceof LocalDevice)
						m_dmrAdapter.insert(device, 0);
					else
						m_dmrAdapter.add(device);
				}
			}
		});

	}

	@SuppressWarnings("rawtypes")
	private void removeDMR(final Device device) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (m_dmrAdapter) {
					m_dmrAdapter.remove(device);
				}
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private void addDMS(final Device device) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (m_dmsAdapter) {
					if (device instanceof LocalDevice)
						m_dmsAdapter.insert(device, 0);
					else
						m_dmsAdapter.add(device);
				}
			}
		});

	}

	@SuppressWarnings("rawtypes")
	private void removeDMS(final Device device) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (m_dmsAdapter) {
					m_dmsAdapter.remove(device);
				}
			}
		});
	}

	// public void onDMSButtonClick(View view) {
	// if (m_ll_dms.getVisibility() == View.VISIBLE)
	// return;
	// AlphaAnimation dmsAnimation = new AlphaAnimation(0f, 1f);
	// dmsAnimation.setDuration(500);
	// dmsAnimation.setAnimationListener(new AnimationListener() {
	//
	// @Override
	// public void onAnimationStart(Animation animation) {
	// m_ll_dms.setVisibility(View.VISIBLE);
	// }
	//
	// @Override
	// public void onAnimationRepeat(Animation animation) {
	//
	// }
	//
	// @Override
	// public void onAnimationEnd(Animation animation) {
	// m_ll_dms.setVisibility(View.VISIBLE);
	// }
	// });
	// m_ll_dms.startAnimation(dmsAnimation);
	//
	// AlphaAnimation dmrAnimation = new AlphaAnimation(1f, 0f);
	// dmrAnimation.setDuration(500);
	// dmrAnimation.setAnimationListener(new AnimationListener() {
	//
	// @Override
	// public void onAnimationStart(Animation animation) {
	// m_ll_dmr.setVisibility(View.VISIBLE);
	// }
	//
	// @Override
	// public void onAnimationRepeat(Animation animation) {
	//
	// }
	//
	// @Override
	// public void onAnimationEnd(Animation animation) {
	// m_ll_dmr.setVisibility(View.GONE);
	// }
	// });
	// m_ll_dmr.startAnimation(dmrAnimation);
	//
	// }
	//
	// public void onDMRButtonClick(View view) {
	// if (m_ll_dmr.getVisibility() == View.VISIBLE)
	// return;
	// AlphaAnimation dmsAnimation = new AlphaAnimation(1f, 0f);
	// dmsAnimation.setDuration(500);
	// dmsAnimation.setAnimationListener(new AnimationListener() {
	//
	// @Override
	// public void onAnimationStart(Animation animation) {
	// m_ll_dms.setVisibility(View.VISIBLE);
	// }
	//
	// @Override
	// public void onAnimationRepeat(Animation animation) {
	//
	// }
	//
	// @Override
	// public void onAnimationEnd(Animation animation) {
	// m_ll_dms.setVisibility(View.GONE);
	// }
	// });
	// m_ll_dms.startAnimation(dmsAnimation);
	//
	// AlphaAnimation dmrAnimation = new AlphaAnimation(0f, 1f);
	// dmrAnimation.setDuration(500);
	// dmrAnimation.setAnimationListener(new AnimationListener() {
	//
	// @Override
	// public void onAnimationStart(Animation animation) {
	// m_ll_dmr.setVisibility(View.VISIBLE);
	// }
	//
	// @Override
	// public void onAnimationRepeat(Animation animation) {
	//
	// }
	//
	// @Override
	// public void onAnimationEnd(Animation animation) {
	// m_ll_dmr.setVisibility(View.VISIBLE);
	// }
	// });
	// m_ll_dmr.startAnimation(dmrAnimation);
	// }

	// @SuppressWarnings("rawtypes")
	// private void saveState() {
	// ObjectOutputStream outputStream = null;
	//
	// try {
	// outputStream = new ObjectOutputStream(openFileOutput("devices_cache",
	// Context.MODE_PRIVATE));
	// if (m_dmsAdapter != null) {
	// synchronized (m_dmsAdapter) {
	// int dmsCount = m_dmsAdapter.getCount();
	// for (int i = 0; i < dmsCount; ++i) {
	// Device device = m_dmsAdapter.getItem(i);
	// if (device instanceof RemoteDevice) {
	// RemoteDevice remote = (RemoteDevice) device;
	// outputStream.writeObject(remote);
	// }
	// }
	// }
	// }
	//
	// if (m_dmrAdapter != null) {
	// synchronized (m_dmsAdapter) {
	// int dmrCount = m_dmrAdapter.getCount();
	// for (int i = 0; i < dmrCount; ++i) {
	// Device device = m_dmrAdapter.getItem(i);
	// if (device instanceof RemoteDevice) {
	// RemoteDevice remote = (RemoteDevice) device;
	// outputStream.writeObject((RemoteDevice) remote);
	// }
	// }
	// }
	// }
	//
	// } catch (FileNotFoundException ex) {
	// ex.printStackTrace();
	// } catch (IOException ex) {
	// ex.printStackTrace();
	// } finally {
	// try {
	// if (outputStream != null) {
	// outputStream.flush();
	// outputStream.close();
	// }
	// } catch (IOException ex) {
	// ex.printStackTrace();
	// }
	// }
	//
	// }

	// @SuppressWarnings("rawtypes")
	// private void restoreState() {
	// ObjectInputStream inputStream = null;
	// m_dmsAdapter.clear();
	// m_dmrAdapter.clear();
	// try {
	// inputStream = new ObjectInputStream(openFileInput("devices_cache"));
	//
	// while (true) {
	// Object object = inputStream.readObject();
	// if (object instanceof RemoteDevice) {
	// RemoteDevice device = (RemoteDevice) object;
	// if (device.getType().getNamespace().equals("schemas-upnp-org")) {
	// if (device.getType().getType().equals("MediaServer")) {
	// addDMS(device);
	// } else if (device.getType().getType().equals("MediaRenderer")) {
	// addDMR(device);
	// }
	// }
	// }
	// }
	// } catch (FileNotFoundException ex) {
	// ex.printStackTrace();
	// } catch (IOException ex) {
	// ex.printStackTrace();
	// } catch (ClassNotFoundException e) {
	// e.printStackTrace();
	// } finally {
	// try {
	// if (inputStream != null) {
	// inputStream.close();
	// }
	// } catch (IOException ex) {
	// ex.printStackTrace();
	// }
	// }
	// }
}
