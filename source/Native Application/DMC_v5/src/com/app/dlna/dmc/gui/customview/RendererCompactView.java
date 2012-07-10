package com.app.dlna.dmc.gui.customview;

import java.net.URL;

import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.dlna.controller.v5.R;

import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.gui.dialog.DeviceDetailsDialog;
import com.app.dlna.dmc.gui.dialog.DeviceDetailsDialog.DeviceDetailsListener;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor.DMRProcessorListener;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.DevicesListener;

public class RendererCompactView extends LinearLayout {

	protected static final String TAG = RendererCompactView.class.getName();
	private LinearLayout m_ll_renderers;
	private LayoutInflater m_inflater;
	private ImageView m_btn_quickPlayPause;
	private OnDMRChangeListener m_onDMRChangeListener;

	public RendererCompactView(Context context, AttributeSet attrs) {
		super(context, attrs);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cv_renderer_compact, this);
		m_ll_renderers = (LinearLayout) ((HorizontalScrollView) findViewById(R.id.gridView_renderer)).getChildAt(0);
		m_inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		m_btn_quickPlayPause = (ImageView) findViewById(R.id.btn_quickPlayPause);
		m_btn_quickPlayPause.setOnClickListener(m_quickPlayPauseClick);
		m_btn_quickPlayPause.setTag(R.string.play);
	}

	@SuppressWarnings("rawtypes")
	public void initComponent() {
		for (Device device : MainActivity.UPNP_PROCESSOR.getDMRList()) {
			m_ll_renderers.addView(getView(device));
			if (device instanceof LocalDevice && MainActivity.UPNP_PROCESSOR.getCurrentDMR() == null) {
				MainActivity.UPNP_PROCESSOR.setCurrentDMR(device.getIdentity().getUdn());
			}
		}
		MainActivity.UPNP_PROCESSOR.addDevicesListener(m_deviceListener);
		updateListRenderer();
		setDMRListener();
	}

	@SuppressWarnings("rawtypes")
	private View getView(final Device device) {
		View ret = m_inflater.inflate(R.layout.gvitem_renderer_compact, null, false);
		TextView name = (TextView) ret.findViewById(R.id.name);
		if (device instanceof RemoteDevice)
			name.setText(device.getDetails().getFriendlyName());
		else
			name.setText("Local Player");
		ret.setTag(device);
		final ImageView icon = (ImageView) ret.findViewById(R.id.icon);
		final Icon[] icons = device.getIcons();
		if (device instanceof RemoteDevice && icons != null && icons.length > 0 && icons[0] != null && icons[0].getUri() != null
				&& icons[0].getUri().getPath() != null && !icons[0].getUri().getPath().isEmpty()) {
			loadRendererIcon(device, icon, icons);
		} else {
			icon.setImageResource(R.drawable.ic_device_unknow_player);
		}
		ret.setOnClickListener(m_rendererClick);
		ret.setOnLongClickListener(m_rendererLongClick);
		return ret;
	}

	@SuppressWarnings("rawtypes")
	private void loadRendererIcon(final Device device, final ImageView icon, final Icon[] icons) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final RemoteDevice remoteDevice = (RemoteDevice) device;

					String urlString = remoteDevice.getIdentity().getDescriptorURL().getProtocol() + "://"
							+ remoteDevice.getIdentity().getDescriptorURL().getAuthority() + icons[0].getUri().toString();
					URL url = new URL(urlString);
					final Bitmap bm = BitmapFactory.decodeStream(url.openConnection().getInputStream());
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							icon.setImageBitmap(bm);
							icon.invalidate();
						}
					});

				} catch (Exception ex) {
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							icon.setImageResource(R.drawable.ic_device_unknow_player);
							icon.invalidate();
						}
					});
					ex.printStackTrace();
				}
			}
		}).start();
	}

	private OnClickListener m_rendererClick = new OnClickListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public void onClick(View v) {
			Device device = (Device) v.getTag();
			UDN udn = device.getIdentity().getUdn();
			MainActivity.UPNP_PROCESSOR.setCurrentDMR(udn);
			updateListRenderer();
			setDMRListener();
			if (m_onDMRChangeListener != null) {
				m_onDMRChangeListener.onDMRChange();
			}
		}

	};
	private OnLongClickListener m_rendererLongClick = new OnLongClickListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean onLongClick(final View v) {
			new DeviceDetailsDialog(getContext(), (Device) v.getTag(), new DeviceDetailsListener() {

				@Override
				public void onWriteTAGClick(Device device) {
					MainActivity.INSTANCE.waitToWriteTAG(device.getIdentity().getUdn().getIdentifierString());
				}

				@Override
				public void onSelectClick(Device device) {
					v.performClick();
				}
			}).show();
			return true;
		}
	};

	// Quick playpause

	private OnClickListener m_quickPlayPauseClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			try {
				if (MainActivity.UPNP_PROCESSOR.getDMRProcessor() != null) {
					if (m_btn_quickPlayPause.getTag().equals(R.string.play)) {
						MainActivity.UPNP_PROCESSOR.getDMRProcessor().play();
					} else if (m_btn_quickPlayPause.getTag().equals(R.string.pause)) {
						MainActivity.UPNP_PROCESSOR.getDMRProcessor().pause();
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	};

	private void setDMRListener() {
		if (MainActivity.UPNP_PROCESSOR != null)
			MainActivity.UPNP_PROCESSOR.addDMRListener(m_dmrListener);
	}

	private DMRProcessorListener m_dmrListener = new DMRProcessorListener() {

		@Override
		public void onUpdatePosition(long current, long max) {
		}

		@Override
		public void onStoped() {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_btn_quickPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_btn_media_quickplay));
					m_btn_quickPlayPause.setTag(R.string.play);
					m_btn_quickPlayPause.invalidate();
				}
			});
		}

		@Override
		public void onPlaying() {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_btn_quickPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_btn_media_quickpause));
					m_btn_quickPlayPause.setTag(R.string.pause);
					m_btn_quickPlayPause.invalidate();
				}
			});
		}

		@Override
		public void onPaused() {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_btn_quickPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_btn_media_quickplay));
					m_btn_quickPlayPause.setTag(R.string.play);
					m_btn_quickPlayPause.invalidate();
				}
			});
		}

		@Override
		public void onErrorEvent(String error) {
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onActionFail(final Action actionCallback, UpnpResponse response, String cause) {
			// MainActivity.INSTANCE.runOnUiThread(new Runnable() {
			//
			// @Override
			// public void run() {
			// MainActivity.UPNP_PROCESSOR.refreshDevicesList();
			// }
			// });
			// Log.e(TAG, "Action fail: actionName = " +
			// actionCallback.getName() + " ; cause = " + cause);
			// MainActivity.INSTANCE.runOnUiThread(new Runnable() {
			//
			// @Override
			// public void run() {
			// Toast.makeText(getContext(), "Action fail: " +
			// actionCallback.getName(), Toast.LENGTH_LONG).show();
			// }
			// });
		}

		@Override
		public void onCheckURLStart() {

		}

		@Override
		public void onCheckURLEnd() {

		}
	};

	@SuppressWarnings("rawtypes")
	public void updateListRenderer() {
		Device currentDMR = MainActivity.UPNP_PROCESSOR.getCurrentDMR();
		if (currentDMR == null)
			return;
		for (int i = 0; i < m_ll_renderers.getChildCount(); ++i) {
			if (currentDMR.equals(m_ll_renderers.getChildAt(i).getTag())) {
				m_ll_renderers.getChildAt(i).setSelected(true);
				m_ll_renderers.getChildAt(i).invalidate();
			} else {
				m_ll_renderers.getChildAt(i).setSelected(false);
				m_ll_renderers.getChildAt(i).invalidate();
			}
		}
		m_ll_renderers.invalidate();
	}

	// Devices listener
	private DevicesListener m_deviceListener = new DevicesListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public void onDeviceRemoved(Device device) {
			if (device.getType().getNamespace().equals("schemas-upnp-org")) {
				if (device.getType().getType().equals("MediaRenderer")) {
					removeDMR(device);
				}
			}
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onDeviceAdded(Device device) {

			if (device.getType().getNamespace().equals("schemas-upnp-org")) {
				if (device.getType().getType().equals("MediaRenderer") && device instanceof RemoteDevice
						&& device.getServices().length != 0
						&& device.findServices(new ServiceType("schemas-upnp-org", "AVTransport")) != null) {
					addDMR(device);
				}
			}
		}

		@Override
		public void onDMSChanged() {
			// Not implemented here

		}

		@Override
		public void onDMRChanged() {
			updateListRenderer();
			setDMRListener();
		}

	};

	@SuppressWarnings("rawtypes")
	private void addDMR(final Device device) {
		MainActivity.INSTANCE.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_ll_renderers.addView(getView(device));
				m_ll_renderers.invalidate();
				updateListRenderer();
				setDMRListener();
				if (m_onDMRChangeListener != null) {
					m_onDMRChangeListener.onDMRUpdate();
				}
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private void removeDMR(final Device device) {
		MainActivity.INSTANCE.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				for (int idx = 0; idx < m_ll_renderers.getChildCount(); ++idx) {
					Object viewTag = m_ll_renderers.getChildAt(idx).getTag();
					if (viewTag != null && viewTag.equals(device)) {
						m_ll_renderers.removeViewAt(idx);
						break;
					}
				}
				m_ll_renderers.invalidate();
				updateListRenderer();
				setDMRListener();
				if (m_onDMRChangeListener != null) {
					m_onDMRChangeListener.onDMRUpdate();
				}
			}
		});
	}

	public interface OnDMRChangeListener {
		void onDMRChange();

		void onDMRUpdate();
	}

	public void setOnDMRChangeListener(OnDMRChangeListener listener) {
		m_onDMRChangeListener = listener;
	}

}
