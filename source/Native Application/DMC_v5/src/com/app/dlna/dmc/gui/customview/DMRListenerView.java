package com.app.dlna.dmc.gui.customview;

import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.Window;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;
import app.dlna.controller.v5.R;

import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor.DMRProcessorListener;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor.ChangeMode;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor.PlaylistListener;
import com.app.dlna.dmc.processor.model.PlaylistItem;

public class DMRListenerView extends LinearLayout {
	protected GridView m_gridView;
	protected CustomArrayAdapter m_adapter;
	protected String m_currentURI = "";
	protected ProgressDialog m_pdlg;

	public DMRListenerView(Context context) {
		super(context);
		m_pdlg = new ProgressDialog(getContext());
		m_pdlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
		m_pdlg.setMessage(context.getString(R.string.check_item_url_));
		m_pdlg.setCancelable(false);
	}

	DMRProcessorListener m_dmrListner = new DMRProcessorListener() {

		private String TAG = DMRListenerView.class.getName();

		@Override
		public void onUpdatePosition(long current, long max) {
			// if (MainActivity.UPNP_PROCESSOR == null ||
			// MainActivity.UPNP_PROCESSOR.getDMRProcessor() == null)
			// return;
			// if
			// (!m_currentURI.equals(MainActivity.UPNP_PROCESSOR.getDMRProcessor().getCurrentTrackURI()))
			// {
			// int start = m_gridView.getFirstVisiblePosition();
			// int end = m_gridView.getLastVisiblePosition();
			// for (int i = start, j = end; i <= j; i++) {
			// final int position = i;
			// MainActivity.INSTANCE.runOnUiThread(new Runnable() {
			//
			// @Override
			// public void run() {
			// try {
			// m_adapter.updateSingleView(m_gridView, position);
			// } catch (Exception e) {
			//
			// }
			// }
			// });
			// }
			// }
			// m_currentURI =
			// MainActivity.UPNP_PROCESSOR.getDMRProcessor().getCurrentTrackURI();

		}

		@Override
		public void onStoped() {

		}

		@Override
		public void onPlaying() {

		}

		@Override
		public void onPaused() {

		}

		@Override
		public void onErrorEvent(final String error) {
			// MainActivity.UPNP_PROCESSOR.refreshDevicesList();
			Log.e(TAG, "error: " + error);
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
				}
			});

		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onActionFail(Action actionCallback, UpnpResponse response, String cause) {
			// MainActivity.UPNP_PROCESSOR.refreshDevicesList();
		}

		@Override
		public void onCheckURLStart() {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_pdlg.show();
				}
			});
		}

		@Override
		public void onCheckURLEnd() {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_pdlg.dismiss();
				}
			});
		}
	};

	protected PlaylistListener m_playlistListener = new PlaylistListener() {

		@Override
		public void onItemChanged(PlaylistItem item, ChangeMode changeMode) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					((CustomArrayAdapter) m_gridView.getAdapter()).notifyDataSetChanged();
				}
			});
		}
	};

	public void updateGridView() {
		if (MainActivity.UPNP_PROCESSOR != null) {
			MainActivity.UPNP_PROCESSOR.addDMRListener(m_dmrListner);
			MainActivity.UPNP_PROCESSOR.addPlaylistListener(m_playlistListener);
		}

	}

}
