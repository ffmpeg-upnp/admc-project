package com.app.dlna.dmc.gui.customview;

import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import app.dlna.controller.v5.R;

import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor.DMRProcessorListner;

public class DMRListenerView extends LinearLayout {
	protected ListView m_listView;
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

	DMRProcessorListner m_dmrListner = new DMRProcessorListner() {

		private String TAG = DMRListenerView.class.getName();

		@Override
		public void onUpdatePosition(long current, long max) {
			if (MainActivity.UPNP_PROCESSOR == null || MainActivity.UPNP_PROCESSOR.getDMRProcessor() == null)
				return;
			if (!m_currentURI.equals(MainActivity.UPNP_PROCESSOR.getDMRProcessor().getCurrentTrackURI())) {
				int start = m_listView.getFirstVisiblePosition();
				int end = m_listView.getLastVisiblePosition();
				for (int i = start, j = end; i <= j; i++) {
					final int position = i;
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							try {
								m_adapter.updateSingleView(m_listView, position);
							} catch (Exception e) {

							}
						}
					});
				}
			}
			m_currentURI = MainActivity.UPNP_PROCESSOR.getDMRProcessor().getCurrentTrackURI();

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
					// TODO Auto-generated method stub
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

	public void updateListView() {
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (dmrProcessor != null)
			dmrProcessor.addListener(m_dmrListner);
	}

}
