package com.app.dlna.dmc.gui.customview.adapter;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.ImageItem;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.cling.support.model.item.VideoItem;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.activity.LibraryActivity;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.cache.Cache;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.Playlist;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.utility.Utility;

public class CustomArrayAdapter extends ArrayAdapter<AdapterItem> {
	private LayoutInflater m_inflater = null;
	private Map<String, Bitmap> m_cacheDMSIcon;
	public static final int MAX_SIZE = 48;
	private static Bitmap BM_IMAGE;
	private static Bitmap BM_VIDEO;
	private static Bitmap BM_AUDIO;
	private static Bitmap BM_YOUTUBE;
	private static Bitmap BM_UNKNOW;
	private boolean m_isDropDown;
	private Object m_tag;
	private int m_currentPosition = 0;

	public CustomArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		m_inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		m_cacheDMSIcon = new HashMap<String, Bitmap>();

		if (BM_IMAGE == null)
			BM_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_didlobject_image);
		if (BM_VIDEO == null)
			BM_VIDEO = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_didlobject_video);
		if (BM_AUDIO == null)
			BM_AUDIO = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_didlobject_audio);
		if (BM_UNKNOW == null)
			BM_UNKNOW = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_didlobject_unknow);
		if (BM_YOUTUBE == null)
			BM_YOUTUBE = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_playlist_youtube);

		m_isDropDown = false;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (convertView == null || convertView instanceof ProgressBar) {
			convertView = m_inflater.inflate(R.layout.lvitem_generic_item, null, false);
		}
		if (convertView.getTag() == null) {
			setViewHolder(convertView);
		}
		final AdapterItem object = getItem(position);

		final ViewHolder holder = (ViewHolder) convertView.getTag();
		holder.desc.setVisibility(View.GONE);
		holder.action.setVisibility(View.GONE);
		if (object.getData() instanceof Playlist) {
			initPlaylist((Playlist) object.getData(), holder, position);
		} else if (object.getData() instanceof PlaylistItem) {
			PlaylistItem _object = (PlaylistItem) object.getData();
			holder.name.setText(_object.getTitle());
			switch (_object.getType()) {
			case AUDIO_REMOTE:
			case AUDIO_LOCAL:
				holder.icon.setImageBitmap(BM_AUDIO);
				break;
			case VIDEO_REMOTE:
			case VIDEO_LOCAL:
				holder.icon.setImageBitmap(BM_VIDEO);
				break;
			case IMAGE_REMOTE:
			case IMAGE_LOCAL:
				holder.icon.setImageBitmap(BM_IMAGE);
				break;
			case YOUTUBE:
				holder.icon.setImageBitmap(BM_YOUTUBE);
				break;
			default:
				holder.icon.setImageBitmap(BM_UNKNOW);
				break;
			}
			if (_object.getUrl().equals(MainActivity.UPNP_PROCESSOR.getDMRProcessor().getCurrentTrackURI())) {
				m_currentPosition = position;
				holder.playing.setVisibility(View.VISIBLE);
			} else {
				holder.playing.setVisibility(View.GONE);
			}
		}

		convertView.setBackgroundDrawable(null);
		return convertView;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (m_isDropDown)
			return getDropDownView(position, convertView, parent);
		if (convertView == null || convertView instanceof ProgressBar) {
			convertView = m_inflater.inflate(R.layout.lvitem_generic_item, null, false);
		}
		if (convertView.getTag() == null) {
			setViewHolder(convertView);
		}
		final AdapterItem object = getItem(position);

		final ViewHolder holder = (ViewHolder) convertView.getTag();
		holder.action.setTag(new Integer(position));
		if (object.getData() instanceof Device)
			initDeviceItem((Device) object.getData(), holder);
		else if (object.getData() instanceof DIDLObject) {
			DIDLObject didlObject = (DIDLObject) object.getData();
			if (didlObject.getId().equals("-1")) {
				return m_inflater.inflate(R.layout.lvitem_loadmoreitem, null);
			} else {
				initDIDLObject(didlObject, holder);
			}
		} else if (object.getData() instanceof PlaylistItem) {
			initPlaylistItem((PlaylistItem) object.getData(), holder, position, true);
		} else if (object.getData() instanceof Playlist) {
			initPlaylist((Playlist) object.getData(), holder, position);
		}
		// } else if (object.getData() instanceof YoutubeItem) {
		// initYoutubeItem((YoutubeItem) object.getData(), holder);
		// }

		return convertView;
	}

	public void setDropDownMode(boolean value) {
		m_isDropDown = value;
	}

	// private void initYoutubeItem(YoutubeItem data, ViewHolder holder) {
	// holder.name.setText(data.getTitle());
	// holder.name.setTextSize(15);
	// holder.name.setSingleLine(false);
	// holder.name.setMaxLines(2);
	// holder.desc.setText(Utility.getTimeString(data.getDuration()));
	// holder.desc.setTextSize(11);
	// holder.playing.setVisibility(View.GONE);
	// holder.action.setVisibility(View.GONE);
	// HashMap<String, Bitmap> cache = Cache.getBitmapCache();
	// String imageUrl = data.getThumbnail();
	// holder.icon.setTag(imageUrl);
	// if (cache.containsKey(imageUrl) && cache.get(imageUrl) != null) {
	// holder.icon.setImageBitmap(cache.get(imageUrl));
	// } else {
	// holder.icon.setImageBitmap(BM_VIDEO);
	// cache.put(imageUrl, BM_VIDEO);
	// Utility.loadImageItemThumbnail(holder.icon, imageUrl,
	// Cache.getBitmapCache(), MAX_SIZE);
	// }
	// }

	private void initPlaylist(Playlist data, ViewHolder holder, int position) {
		holder.action.setVisibility(View.GONE);
		holder.name.setText(data.getName());
		holder.desc.setText("");
		holder.icon.setImageResource(R.drawable.ic_playlist);
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor != null && playlistProcessor.getData() != null && playlistProcessor.getData().equals(data)) {
			holder.playing.setVisibility(View.VISIBLE);
		} else {
			holder.playing.setVisibility(View.GONE);
		}
	}

	@SuppressWarnings("rawtypes")
	private void initDeviceItem(final Device device, final ViewHolder holder) {
		holder.action.setVisibility(View.GONE);
		holder.playing.setVisibility(View.GONE);
		if (device instanceof LocalDevice)
			holder.name.setText("Local Device");
		else
			holder.name.setText(device.getDetails().getFriendlyName());
		final String udn = device.getIdentity().getUdn().getIdentifierString();

		if (device instanceof RemoteDevice) {
			holder.desc.setText(((RemoteDevice) device).getIdentity().getDescriptorURL().getAuthority());
		} else {
			holder.desc.setText("Local device");
		}

		if (m_cacheDMSIcon.containsKey(udn)) {
			holder.icon.setImageBitmap(m_cacheDMSIcon.get(udn));
		} else {
			if (device instanceof RemoteDevice) {
				try {
					final Icon[] icons = device.getIcons();
					if (icons != null && icons.length > 0 && icons[0] != null && icons[0].getUri() != null) {
						loadDeviceIcon(device, holder, udn, icons);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				holder.icon.setImageResource(R.drawable.ic_launcher);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	private void loadDeviceIcon(final Device device, final ViewHolder holder, final String udn, final Icon[] icons) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final RemoteDevice remoteDevice = (RemoteDevice) device;

					String urlString = remoteDevice.getIdentity().getDescriptorURL().getProtocol() + "://"
							+ remoteDevice.getIdentity().getDescriptorURL().getAuthority() + icons[0].getUri().toString();
					URL url = new URL(urlString);
					final Bitmap icon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
					m_cacheDMSIcon.put(udn, icon);
					((Activity) getContext()).runOnUiThread(new Runnable() {

						@Override
						public void run() {
							CustomArrayAdapter.this.notifyDataSetChanged();
						}
					});

				} catch (Exception ex) {
					((Activity) getContext()).runOnUiThread(new Runnable() {

						@Override
						public void run() {
							holder.icon.setImageBitmap(null);
						}
					});
					ex.printStackTrace();
				}
			}
		}).start();
	}

	private void initDIDLObject(DIDLObject object, ViewHolder holder) {
		holder.name.setText(object.getTitle());
		if (object.getId().equals("-1")) {
			holder.icon.setVisibility(View.GONE);
			holder.desc.setText("");
			holder.action.setVisibility(View.GONE);
			holder.playing.setVisibility(View.GONE);
			return;
		}
		holder.icon.setVisibility(View.VISIBLE);
		if (object instanceof Container) {
			holder.icon.setImageResource(R.drawable.ic_didlobject_container);
			holder.action.setVisibility(View.GONE);
			holder.playing.setVisibility(View.GONE);
			int childCount = ((Container) object).getChildCount() != null ? ((Container) object).getChildCount() : 1;
			String childCountStr = "";
			if (childCount == 0)
				childCountStr = "empty";
			else if (childCount == 1)
				childCountStr = "1 child";
			else
				childCountStr = childCount + " items";
			holder.desc.setText(childCountStr);
		} else {
			String objectUrl = object.getResources().get(0).getValue();
			if (object instanceof MusicTrack) {
				holder.icon.setImageBitmap(BM_AUDIO);
			} else if (object instanceof VideoItem) {
				holder.icon.setImageBitmap(BM_VIDEO);
			} else if (object instanceof ImageItem) {
				// holder.icon.setImageResource(R.drawable.ic_didlobject_image);
				// Utility.loadImageItemThumbnail(holder.icon,
				// object.getResources().get(0).getValue(),
				// Cache.getBitmapCache(),
				// MAX_SIZE);
				HashMap<String, Bitmap> cache = Cache.getBitmapCache();
				holder.icon.setTag(objectUrl);
				if (cache.containsKey(objectUrl) && cache.get(objectUrl) != null) {
					holder.icon.setImageBitmap(cache.get(objectUrl));
				} else {
					holder.icon.setImageBitmap(BM_IMAGE);
					cache.put(objectUrl, BM_IMAGE);
					Utility.loadImageItemThumbnail(holder.icon, objectUrl, Cache.getBitmapCache(), MAX_SIZE);
				}
			} else {
				holder.icon.setImageResource(R.drawable.ic_didlobject_unknow);
			}
			if (object.getResources().size() > 0) {
				if (object.getResources().get(0) != null && object.getResources().get(0).getSize() != null)
					holder.desc.setText(Utility.convertSizeToString(object.getResources().get(0).getSize()));
			}
			if (object instanceof Item) {
				holder.action.setVisibility(View.VISIBLE);
				if (getContext() instanceof LibraryActivity) {
					LibraryActivity activity = (LibraryActivity) getContext();
					PlaylistProcessor playlistProcessor = activity.getPlaylistView().getCurrentPlaylistProcessor();
					if (playlistProcessor.containsUrl(objectUrl)) {
						holder.action.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_btn_remove));
					} else {
						holder.action.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_btn_add));
					}
				}

				if (objectUrl.equals(MainActivity.UPNP_PROCESSOR.getDMRProcessor().getCurrentTrackURI())) {
					holder.playing.setVisibility(View.VISIBLE);
				} else {
					holder.playing.setVisibility(View.GONE);
				}
			} else {
				holder.action.setVisibility(View.GONE);
			}
		}
	}

	private void initPlaylistItem(PlaylistItem object, ViewHolder holder, int position, boolean editable) {
		holder.name.setText(object.getTitle());
		String imageUrl = object.getUrl();
		switch (object.getType()) {
		case AUDIO_LOCAL:
		case AUDIO_REMOTE:
			holder.icon.setImageBitmap(BM_AUDIO);
			break;
		case VIDEO_LOCAL:
		case VIDEO_REMOTE:
			holder.icon.setImageBitmap(BM_VIDEO);
			break;
		case IMAGE_LOCAL:
		case IMAGE_REMOTE: {
			HashMap<String, Bitmap> cache = Cache.getBitmapCache();
			holder.icon.setTag(imageUrl);
			if (cache.containsKey(imageUrl) && cache.get(imageUrl) != null) {
				holder.icon.setImageBitmap(cache.get(imageUrl));
			} else {
				holder.icon.setImageBitmap(BM_IMAGE);
				cache.put(imageUrl, BM_IMAGE);
				Utility.loadImageItemThumbnail(holder.icon, imageUrl, Cache.getBitmapCache(), MAX_SIZE);
			}
			break;
		}
		case YOUTUBE:
			holder.icon.setImageBitmap(BM_YOUTUBE);
			break;
		default:
			holder.icon.setImageBitmap(BM_UNKNOW);
			break;
		}
		if (editable) {
			holder.action.setVisibility(View.VISIBLE);
			holder.action.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_btn_remove));
		} else
			holder.action.setVisibility(View.GONE);
		if (imageUrl.equals(MainActivity.UPNP_PROCESSOR.getDMRProcessor().getCurrentTrackURI())) {
			holder.playing.setVisibility(View.VISIBLE);
		} else {
			holder.playing.setVisibility(View.GONE);
		}
	}

	public void setViewHolder(View view) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.name = (TextView) view.findViewById(R.id.name);
		viewHolder.desc = (TextView) view.findViewById(R.id.desc);
		viewHolder.icon = (ImageView) view.findViewById(R.id.icon);
		viewHolder.action = (ImageView) view.findViewById(R.id.action);
		viewHolder.action.setOnClickListener(m_actionClick);
		viewHolder.playing = (ImageView) view.findViewById(R.id.playing);
		view.setTag(viewHolder);
	}

	private class ViewHolder {
		TextView desc;
		TextView name;
		ImageView icon;
		ImageView action;
		ImageView playing;
	}

	@Override
	public void clear() {
		super.clear();
	}

	public void prepareImageItemCache(final List<? extends DIDLObject> objects) {
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// m_cancelPreparing = false;
		// for (DIDLObject didlObject : objects) {
		//
		// if (m_cancelPreparing) {
		// Log.e(TAG, "Cancel preparing");
		// break;
		// }
		//
		// if (didlObject instanceof ImageItem) {
		// synchronized (Cache.getBitmapCache()) {
		// String imageUrl = didlObject.getResources().get(0).getValue();
		// try {
		// Cache.getBitmapCache().put(imageUrl,
		// Utility.getBitmapFromURL(imageUrl, MAX_SIZE));
		// } catch (MalformedURLException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// }
		// }
		// }).start();
	}

	public void cancelPrepareImageCache() {
	}

	private OnClickListener m_actionClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getTag() == null)
				return;
			int position = (Integer) v.getTag();
			Object item = getItem(position).getData();
			if (getContext() instanceof LibraryActivity) {
				LibraryActivity activity = (LibraryActivity) getContext();
				PlaylistProcessor playlistProcessor = activity.getPlaylistView().getCurrentPlaylistProcessor();
				if (playlistProcessor != null) {
					if (item instanceof PlaylistItem) {
						playlistProcessor.removeItem((PlaylistItem) item);
						remove(getItem(position));
					} else if (item instanceof DIDLObject) {
						final DIDLObject object = (DIDLObject) item;
						if (playlistProcessor.containsUrl(object.getResources().get(0).getValue())) {
							if (playlistProcessor.removeDIDLObject(object) != null)
								updateSingleView(v, position);
						} else {
							if (playlistProcessor.addDIDLObject(object) != null)
								updateSingleView(v, position);
							else {
								Toast.makeText(getContext(), "Current playlist is full", Toast.LENGTH_SHORT).show();
							}
						}
					}
				} else {
					Toast.makeText(getContext(), "Please chose a playlist to add", Toast.LENGTH_SHORT).show();
				}
			}

		}
	};

	public void updateSingleView(View v, int position) {
		if (v.getParent().getParent().getParent() instanceof ListView) {
			ListView list = (ListView) v.getParent().getParent().getParent();
			int start = list.getFirstVisiblePosition();
			int end = list.getLastVisiblePosition();
			for (int i = start, j = end; i <= j; i++)
				if (position == i) {
					View view = list.getChildAt(i - start);
					list.getAdapter().getView(i, view, list);
					break;
				}
		}
	}

	public void updateSingleView(ListView list, int position) {
		int start = list.getFirstVisiblePosition();
		int end = list.getLastVisiblePosition();
		for (int i = start, j = end; i <= j; i++)
			if (position == i) {
				View view = list.getChildAt(i - start);
				list.getAdapter().getView(i, view, list);
				break;
			}
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

	public void notifyVisibleItemChanged(ListView list) {
		int start = list.getFirstVisiblePosition() < getCount() ? list.getFirstVisiblePosition() : getCount() - 1;
		int end = list.getLastVisiblePosition() < getCount() ? list.getLastVisiblePosition() : getCount() - 1;
		for (int i = start, j = end; i <= j; i++) {
			View view = list.getChildAt(i - start);
			if (0 <= i && i < getCount())
				list.getAdapter().getView(i, view, list);
		}
	}

	public Object getTag() {
		return m_tag;
	}

	public void setTag(Object tag) {
		m_tag = tag;
	}

	public int getCurrentPostition() {
		return m_currentPosition;
	}

}
