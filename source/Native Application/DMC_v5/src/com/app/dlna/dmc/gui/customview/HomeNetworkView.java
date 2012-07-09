package com.app.dlna.dmc.gui.customview;

import java.util.List;
import java.util.Map;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.gui.activity.LibraryActivity;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.gui.dialog.DeviceDetailsDialog;
import com.app.dlna.dmc.gui.dialog.DeviceDetailsDialog.DeviceDetailsListener;
import com.app.dlna.dmc.processor.impl.PlaylistManager;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor.DMSProcessorListner;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.DevicesListener;
import com.app.dlna.dmc.processor.model.Playlist;
import com.app.dlna.dmc.processor.model.Playlist.ViewMode;
import com.app.dlna.dmc.processor.model.PlaylistItem;

public class HomeNetworkView extends DMRListenerView {
	private ProgressDialog m_progressDlg;
	private boolean m_loadMore;
	private boolean m_isRoot;
	private boolean m_isBrowsing;
	private HomeNetworkToolbar m_toolbar;

	@SuppressWarnings("rawtypes")
	public HomeNetworkView(Context context) {
		super(context);
		m_isBrowsing = false;
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cv_homenetwork,
				this);
		m_listView = (ListView) findViewById(R.id.lv_mediasource_browsing);
		m_adapter = new CustomArrayAdapter(context, 0);
		m_listView.setAdapter(m_adapter);
		m_listView.setOnItemClickListener(m_itemClick);
		m_listView.setOnItemLongClickListener(m_itemLongClick);
		m_listView.setOnScrollListener(m_scrollListener);
		m_progressDlg = new ProgressDialog(MainActivity.INSTANCE);
		m_progressDlg.setTitle("Loading");
		m_progressDlg.setMessage("Waiting for loading items");
		m_progressDlg.setCancelable(true);
		m_progressDlg.setCanceledOnTouchOutside(false);
		MainActivity.UPNP_PROCESSOR.addDevicesListener(m_upnpListener);

		m_toolbar = (HomeNetworkToolbar) findViewById(R.id.botToolbar);
		m_toolbar.setLocalNetworkView(this);
		m_toolbar.setVisibility(View.GONE);

		for (Device device : MainActivity.UPNP_PROCESSOR.getDMSList()) {
			m_adapter.add(new AdapterItem(device));
		}

	}

	private OnScrollListener m_scrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			try {
				if (MainActivity.UPNP_PROCESSOR.getDMSProcessor() != null
						&& m_progressDlg != null
						&& !m_progressDlg.isShowing()
						&& firstVisibleItem + visibleItemCount == totalItemCount
						&& m_adapter.getItem(firstVisibleItem + visibleItemCount - 1).getData() instanceof DIDLObject
						&& ((DIDLObject) m_adapter.getItem(firstVisibleItem + visibleItemCount - 1).getData()).getId()
								.equals("-1")) {
					doLoadMoreItems();
				}
			} catch (Exception ex) {

			}
		}
	};

	private OnItemClickListener m_itemClick = new OnItemClickListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
			AdapterItem item = m_adapter.getItem(position);
			if (item.getData() instanceof Device) {
				UDN udn = ((Device) item.getData()).getIdentity().getUdn();
				MainActivity.UPNP_PROCESSOR.setCurrentDMS(udn);
				m_progressDlg.show();
				browse("0", 0);
				m_isBrowsing = true;
				m_toolbar.setVisibility(View.VISIBLE);
			} else {
				final DIDLObject object = (DIDLObject) item.getData();
				if (object instanceof Container) {
					if (((Container) object).getChildCount() != null && ((Container) object).getChildCount() == 0)
						Toast.makeText(getContext(), "Folder is empty", Toast.LENGTH_SHORT).show();
					else
						browse(object.getId(), 0);
				} else if (object instanceof Item) {
					if (object.getId().equals("-1")) {
						doLoadMoreItems();
					} else {
						addToPlaylistAndPlay(object);
					}
				}
			}
		}

	};

	private OnItemLongClickListener m_itemLongClick = new OnItemLongClickListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, final View view, final int position, final long id) {
			final Object object = m_adapter.getItem(position).getData();
			if (object instanceof Device) {
				new DeviceDetailsDialog(getContext(), (Device) object, new DeviceDetailsListener() {

					@Override
					public void onWriteTAGClick(Device device) {
						MainActivity.INSTANCE.waitToWriteTAG(device.getIdentity().getUdn().getIdentifierString());
					}

					@Override
					public void onSelectClick(Device device) {
						m_listView.performItemClick(view, position, id);
					}
				}).show();
			} else if (object instanceof Item) {
				new AlertDialog.Builder(getContext())
						.setTitle("Select Action")
						.setItems(getResources().getStringArray(R.array.didlobject_contextmenu),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										switch (which) {
										case 0:
											addToOtherPlaylist((Item) object);
											break;
										case 1:
											MainActivity.UPNP_PROCESSOR.getDownloadProcessor().startDownload(
													((Item) object));
											break;
										default:
											break;
										}
										dialog.dismiss();
									}
								}).create().show();
			}

			return true;
		}

	};

	private void addToOtherPlaylist(final Item item) {
		final List<Playlist> allPlaylist = PlaylistManager.getAllPlaylist();
		String[] listPlaylistName = new String[allPlaylist.size()];
		for (int i = 0; i < allPlaylist.size(); ++i) {
			listPlaylistName[i] = allPlaylist.get(i).getName();
		}
		new AlertDialog.Builder(getContext()).setTitle("Select Playlist To Add")
				.setItems(listPlaylistName, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, final int which) {
						new Thread(new Runnable() {

							@Override
							public void run() {
								MainActivity.INSTANCE.showLoadingMessage("Adding item to playlist \""
										+ allPlaylist.get(which).getName() + "\"");
								PlaylistItem playlistItem = PlaylistManager
										.getPlaylistProcessor(allPlaylist.get(which)).addDIDLObject(item);
								MainActivity.INSTANCE.dismissLoadingDialog();
								if (playlistItem != null) {
									MainActivity.INSTANCE.showToast("Item added to playlist sucessfully");
								}
							}
						}).start();
					}
				}).create().show();
	}

	private void doLoadMoreItems() {
		m_loadMore = true;
		m_progressDlg.show();
		MainActivity.UPNP_PROCESSOR.getDMSProcessor().nextPage(m_browseListener);
	}

	private void browse(String id, int pageIndex) {
		DMSProcessor dmsProcessor = MainActivity.UPNP_PROCESSOR.getDMSProcessor();
		if (dmsProcessor != null) {
			m_progressDlg.show();
			// Log.e(TAG, "Browse id = " + id);
			m_progressDlg.show();
			m_loadMore = false;
			dmsProcessor.browse(id, pageIndex, m_browseListener);
		}
	}

	private void addToPlaylistAndPlay(DIDLObject object) {
		if (getContext() instanceof LibraryActivity) {
			LibraryActivity activity = (LibraryActivity) getContext();
			PlaylistProcessor playlistProcessor = activity.getPlaylistView().getCurrentPlaylistProcessor();
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (playlistProcessor == null || dmrProcessor == null)
				return;
			MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(playlistProcessor);
			playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			PlaylistItem added = playlistProcessor.addDIDLObject(object);
			if (added != null) {
				m_adapter.notifyVisibleItemChanged(m_listView);
				switch (added.getType()) {
				case AUDIO_LOCAL:
				case AUDIO_REMOTE:
					AppPreference.setPlaylistViewMode(ViewMode.AUDIO_ONLY);
					break;
				case VIDEO_LOCAL:
				case VIDEO_REMOTE:
				case YOUTUBE:
					AppPreference.setPlaylistViewMode(ViewMode.VIDEO_ONLY);
					break;
				case IMAGE_LOCAL:
				case IMAGE_REMOTE:
					AppPreference.setPlaylistViewMode(ViewMode.IMAGE_ONLY);
					break;
				default:
					AppPreference.setPlaylistViewMode(ViewMode.ALL);
					break;
				}
				playlistProcessor.setCurrentItem(added);
				dmrProcessor.setURIandPlay(playlistProcessor.getCurrentItem());
			} else {
				if (playlistProcessor.isFull()) {
					Toast.makeText(getContext(), "Current playlist is full", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getContext(), "An error occurs, try again later", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	private DMSProcessorListner m_browseListener = new DMSProcessorListner() {

		@Override
		public void onBrowseFail(final String message) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_progressDlg.dismiss();
					new AlertDialog.Builder(MainActivity.INSTANCE).setTitle("Error occurs").setMessage(message)
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									m_isBrowsing = false;
									m_isRoot = false;
									backToPlaylist();
								}
							}).show();

				}
			});
			// Log.e(TAG, "On browse fail, message = " + message);
		}

		@Override
		public void onBrowseComplete(final String objectID, final boolean haveNext, boolean havePrev,
				final Map<String, List<? extends DIDLObject>> result) {
			// Log.i(TAG, "browse complete: object id = " + objectID +
			// " haveNext = " + haveNext + "; havePrev ="
			// + havePrev + "; result size = " + result.size());
			m_isRoot = objectID.equals("0");
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (m_progressDlg.isShowing()) {
						if (m_loadMore) {
							m_adapter.remove(m_adapter.getItem(m_adapter.getCount() - 1));
						} else
							m_adapter.clear();
						for (DIDLObject container : result.get("Containers"))
							m_adapter.add(new AdapterItem(container));

						for (DIDLObject item : result.get("Items"))
							m_adapter.add(new AdapterItem(item));

						prepareImageCache(result);

						if (haveNext) {
							Item item = new Item();
							item.setTitle("Load more result");
							item.setId("-1");
							m_adapter.add(new AdapterItem(item));
						}

						m_progressDlg.dismiss();
						if (!m_loadMore)
							m_listView.smoothScrollToPosition(0);
						m_adapter.notifyVisibleItemChanged(m_listView);
					}
				}
			});

		}
	};

	private void prepareImageCache(final Map<String, List<? extends DIDLObject>> result) {
		m_adapter.cancelPrepareImageCache();
		m_adapter.prepareImageItemCache(result.get("Items"));
	}

	public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (m_isRoot) {
				m_adapter.clear();
			}
			return true;
		}
		return false;
	};

	private DevicesListener m_upnpListener = new DevicesListener() {

		@SuppressWarnings("rawtypes")
		@Override
		public void onDeviceAdded(Device device) {
			if (device.getType().getNamespace().equals("schemas-upnp-org")) {
				if (device.getType().getType().equals("MediaServer")) {
					addDMS(device);
				}
			}
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onDeviceRemoved(Device device) {
			if (device.getType().getNamespace().equals("schemas-upnp-org")) {
				if (device.getType().getType().equals("MediaServer")) {
					removeDMS(device);
				}
			}
		}

		@Override
		public void onDMSChanged() {
			browse("0", 0);
			m_isBrowsing = true;
			m_toolbar.setVisibility(View.VISIBLE);
		}

		@Override
		public void onDMRChanged() {
			// Not implemented here
		}
	};

	@SuppressWarnings("rawtypes")
	private void addDMS(final Device device) {
		MainActivity.INSTANCE.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (m_adapter) {
					if (m_isBrowsing)
						return;
					if (device instanceof LocalDevice)
						m_adapter.insert(new AdapterItem(device), 0);
					else
						m_adapter.add(new AdapterItem(device));
				}
			}
		});

	}

	@SuppressWarnings("rawtypes")
	private void removeDMS(final Device device) {
		MainActivity.INSTANCE.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (m_adapter) {
					if (m_isBrowsing)
						return;
					m_adapter.remove(new AdapterItem(device));
				}
			}
		});
	}

	public boolean isRoot() {
		return m_isRoot;
	}

	public boolean isBrowsing() {
		return m_isBrowsing;
	}

	public CustomArrayAdapter getListAdapter() {
		return m_adapter;
	}

	public void setBrowsing(boolean browsing) {
		m_isBrowsing = browsing;
	}

	public ProgressDialog getProgressDlg() {
		return m_progressDlg;
	}

	public void setLoadMore(boolean loadMore) {
		m_loadMore = loadMore;
	}

	public DMSProcessorListner getBrowseListener() {
		return m_browseListener;
	}

	public void updateListView() {
		super.updateListView();
		m_adapter.notifyVisibleItemChanged(m_listView);
	}

	public ListView getListView() {
		return m_listView;
	}

	@SuppressWarnings("rawtypes")
	public void backToPlaylist() {
		m_adapter.clear();
		for (Device dms : MainActivity.UPNP_PROCESSOR.getDMSList()) {
			if (dms instanceof LocalDevice)
				m_adapter.insert(new AdapterItem(dms), 0);
			else
				m_adapter.add(new AdapterItem(dms));
		}
		m_isBrowsing = false;
		m_adapter.cancelPrepareImageCache();
		m_toolbar.setVisibility(View.GONE);
	}

}
