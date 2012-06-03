package com.app.dlna.dmc.gui.customview.library;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.activity.LibraryActivity;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.gui.customview.adapter.CustomArrayAdapter;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.DMSProcessor.DMSAddRemoveContainerListener;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;

public class HomeNetworkToolbar extends LinearLayout {
	private static final String ACTION_DESELECT_ALL = "Deselect All";
	private static final String ACTION_SELECT_ALL = "Select All";

	private HomeNetworkView m_homeNetworkView;
	private ImageView m_btn_back;
	private CustomArrayAdapter m_homeNetworkAdapter;

	public HomeNetworkToolbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cv_toolbar_homenetwork,
				this);
		m_btn_back = ((ImageView) findViewById(R.id.btn_back));
		m_btn_back.setOnClickListener(onBackClick);
		m_btn_back.setOnLongClickListener(onBackLongclick);
		((ImageView) findViewById(R.id.btn_containerSelectAll)).setOnClickListener(onSelectAll);
		((ImageView) findViewById(R.id.btn_containerDeselectAll)).setOnClickListener(onDeselectAll);
	}

	public void setLocalNetworkView(HomeNetworkView localNetworkView) {
		m_homeNetworkView = localNetworkView;
		m_homeNetworkAdapter = localNetworkView.getListAdapter();
	}

	private OnClickListener onBackClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (m_homeNetworkView.isBrowsing()) {
				if (m_homeNetworkView.isRoot()) {
					// return to device list
					backToDeviceList();
				} else {
					upOneLevel();
				}
			}
		}
	};

	private OnLongClickListener onBackLongclick = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			backToDeviceList();
			return true;
		}
	};

	private void backToDeviceList() {
		m_homeNetworkView.backToPlaylist();
	}

	private void upOneLevel() {
		if (m_homeNetworkView.isRoot())
			Toast.makeText(getContext(), "You are in root of this data source", Toast.LENGTH_SHORT).show();
		else if (MainActivity.UPNP_PROCESSOR.getDMSProcessor() != null) {
			m_homeNetworkView.getProgressDlg().show();
			m_homeNetworkView.setLoadMore(false);
			MainActivity.UPNP_PROCESSOR.getDMSProcessor().back(m_homeNetworkView.getBrowseListener());
		}
	}

	private void modifyPlaylist(String type) {
		LibraryActivity activity = (LibraryActivity) getContext();
		final PlaylistProcessor playlistProcessor = activity.getPlaylistView().getCurrentPlaylistProcessor();
		if (playlistProcessor == null) {
			Toast.makeText(MainActivity.INSTANCE, "Cannot process playlist", Toast.LENGTH_SHORT).show();
			return;
		}
		final DMSProcessor dmsProcessor = MainActivity.UPNP_PROCESSOR.getDMSProcessor();
		if (dmsProcessor == null) {
			Toast.makeText(MainActivity.INSTANCE, "Cannot contact to dms server", Toast.LENGTH_SHORT).show();
			return;
		}

		if (type.equals(ACTION_SELECT_ALL)) {
			dmsProcessor.addAllToPlaylist(playlistProcessor, m_playlistModifyListener);
		} else if (type.equals(ACTION_DESELECT_ALL)) {
			dmsProcessor.removeAllFromPlaylist(playlistProcessor, m_playlistModifyListener);
		}

	}

	private DMSAddRemoveContainerListener m_playlistModifyListener = new DMSAddRemoveContainerListener() {
		ProgressDialog m_progreProgressDialog;

		@Override
		public void onActionStart(final String actionType) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					String message = actionType.equals(DMSProcessor.ACTION_ADD) ? "Waiting for add all items"
							: "Waiting for remove all items";

					m_progreProgressDialog = ProgressDialog.show(getContext(), "Processing", message);
					m_progreProgressDialog.setCancelable(false);
				}
			});

		}

		@Override
		public void onActionFail(final Exception ex) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_progreProgressDialog.dismiss();
					Toast.makeText(getContext(), "Error occur: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
					m_homeNetworkAdapter.notifyVisibleItemChanged(m_homeNetworkView.getListView());
				}
			});

		}

		@Override
		public void onActionComplete(final String message) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_progreProgressDialog.dismiss();
					Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
					LibraryActivity activity = (LibraryActivity) getContext();
					activity.getPlaylistView().udpateCurrentPlaylistProcessor();
					m_homeNetworkAdapter.notifyVisibleItemChanged(m_homeNetworkView.getListView());
				}
			});

		}
	};

	private OnClickListener onSelectAll = new OnClickListener() {

		@Override
		public void onClick(View v) {
			modifyPlaylist(ACTION_SELECT_ALL);
		}
	};
	private OnClickListener onDeselectAll = new OnClickListener() {

		@Override
		public void onClick(View v) {
			modifyPlaylist(ACTION_DESELECT_ALL);
		}
	};
}
