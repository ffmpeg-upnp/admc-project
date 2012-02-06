package com.app.dlna.dmc.gui.devices;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.RemoteDevice;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.dlna.dmc.R;

public class RemoteDMRArrayAdapter extends ArrayAdapter<RemoteDevice> {

	public RemoteDMRArrayAdapter(Context context, int resource, int textViewResourceId, List<RemoteDevice> objects) {
		super(context, resource, textViewResourceId, objects);
		m_inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private static final String TAG = RemoteDMRArrayAdapter.class.getName();
	private LayoutInflater m_inflater;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = m_inflater.inflate(R.layout.dmr_listview_item, null, false);
		}
		if (convertView.getTag() == null) {
			setViewHolder(convertView);
		}
		RemoteDevice device = getItem(position);
		Log.e(TAG, "adding device to listview :" + device.getDetails().getFriendlyName());

		ViewHolder holder = (ViewHolder) convertView.getTag();
		holder.deviceName.setText(device.getDetails().getFriendlyName());
		holder.deviceType.setText("Digital Media Renderer");

		Icon[] icons = device.getIcons();
		if (icons != null && icons[0] != null && icons[0].getUri() != null) {
			try {
				String urlString = device.getIdentity().getDescriptorURL().getProtocol() + "://" + device.getIdentity().getDescriptorURL().getAuthority();
				String iconURI = icons[0].getUri().toString();
				if (iconURI.startsWith("/")) {
					urlString += iconURI;
				} else {
					urlString += ("/" + iconURI);
				}

				Log.e(TAG, "ICON URL = " + urlString);
				URL url = new URL(urlString);
				holder.deviceIcon.setImageBitmap(BitmapFactory.decodeStream(url.openConnection().getInputStream()));

			} catch (MalformedURLException e) {
				Log.e(TAG, "Can't get Icon of device: " + device.getDisplayString());
			} catch (IOException e) {
				Log.e(TAG, "Can't get Icon of device: " + device.getDisplayString());
			}
		}

		return convertView;
	}

	@Override
	public void add(RemoteDevice object) {
		if (object.getType().getNamespace().compareTo("schemas-upnp-org") == 0 && object.getType().getType().compareTo("MediaRenderer") == 0)
			super.add(object);
	}

	public void setViewHolder(View view) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.deviceName = (TextView) view.findViewById(R.id.deviceName);
		viewHolder.deviceType = (TextView) view.findViewById(R.id.deviceType);
		viewHolder.deviceIcon = (ImageView) view.findViewById(R.id.deviceIcon);
		view.setTag(viewHolder);
	}

	private class ViewHolder {
		TextView deviceType;
		TextView deviceName;
		ImageView deviceIcon;
	}
}
