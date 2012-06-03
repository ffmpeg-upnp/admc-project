package com.app.dlna.dmc.gui.customview.youtube;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.activity.LibraryActivity;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.impl.YoutubeProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.YoutubeProcessor;
import com.app.dlna.dmc.processor.interfaces.YoutubeProcessor.IYoutubeProcessorListener;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;

public class YoutubeView extends LinearLayout {

	protected static final String TAG = YoutubeView.class.getName();
	private ListView m_listView;
	private EditText m_ed_query;
	private ImageView m_btn_search;
	private YoutubeItemArrayAdapter m_adapter;
	private YoutubeProcessor m_youtubeProcessor;
	private ProgressDialog m_progress;

	public YoutubeView(Context context) {
		super(context);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cv_youtube, this);
		m_listView = (ListView) findViewById(R.id.listView);
		m_adapter = new YoutubeItemArrayAdapter(context, 0);
		m_listView.setAdapter(m_adapter);
		m_listView.setOnItemClickListener(m_itemClick);
		m_listView.setOnItemLongClickListener(m_itemLongClick);
		m_ed_query = (EditText) findViewById(R.id.ed_query);
		m_btn_search = (ImageView) findViewById(R.id.btn_search);
		m_btn_search.setOnClickListener(m_onSearchClick);
		m_youtubeProcessor = new YoutubeProcessorImpl();
		m_progress = new ProgressDialog(context);
		m_progress.requestWindowFeature(Window.FEATURE_NO_TITLE);
		m_progress.setMessage("Contacting to Youtube...");
		m_progress.setCancelable(true);
		m_progress.setCanceledOnTouchOutside(false);
	}

	private OnClickListener m_onSearchClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String query = m_ed_query.getText().toString();
			m_adapter.clear();
			m_youtubeProcessor.executeQueryAsync(query, m_youtubeListener);
		}
	};

	private OnItemClickListener m_itemClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
			MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(((LibraryActivity) getContext()).getPlaylistView()
					.getCurrentPlaylistProcessor());
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			PlaylistItem added = playlistProcessor.addYoutubeItem(m_adapter.getItem(position));
			if (added != null) {
				Toast.makeText(getContext(), "Added item to playlist", Toast.LENGTH_SHORT).show();
				Log.i(TAG, "new idx = 	" + playlistProcessor.setCurrentItem(added));
				DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
				if (dmrProcessor != null)
					dmrProcessor.setURIandPlay(playlistProcessor.getCurrentItem());

				// m_youtubeProcessor.getDirectLinkAsync(m_adapter.getItem(position),
				// m_youtubeListener);
			} else {
				if (playlistProcessor.isFull()) {
					Toast.makeText(getContext(), "Current playlist is full", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getContext(), "An error occurs, try again later", Toast.LENGTH_SHORT).show();
				}
			}
		}
	};
	private OnItemLongClickListener m_itemLongClick = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long arg3) {
			return false;
		}
	};

	private IYoutubeProcessorListener m_youtubeListener = new IYoutubeProcessorListener() {

		@Override
		public void onStartPorcess() {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_progress.show();
				}
			});

		}

		@Override
		public void onSearchComplete(final List<YoutubeItem> result) {
			if (m_progress.isShowing())
				MainActivity.INSTANCE.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						m_progress.dismiss();
						m_adapter.clear();
						for (YoutubeItem item : result)
							m_adapter.add(item);
						m_listView.smoothScrollToPosition(0);
					}
				});

		}

		@Override
		public void onGetLinkComplete(final YoutubeItem result) {
			// if (m_progress.isShowing())
			// MainActivity.INSTANCE.runOnUiThread(new Runnable() {
			//
			// @Override
			// public void run() {
			// m_progress.dismiss();
			// DMRProcessor dmrProcessor =
			// MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			// if (dmrProcessor != null)
			// dmrProcessor.setURIandPlay(playlistProcessor.getCurrentItem());
			// }
			//
			// });
		}

		@Override
		public void onFail(Exception ex) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_progress.dismiss();
				}

			});

		}
	};

}
