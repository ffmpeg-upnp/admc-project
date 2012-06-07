package com.app.dlna.dmc.gui.customview.nowplaying;

import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.gui.activity.NowPlayingActivity;
import com.app.dlna.dmc.gui.customview.adapter.AdapterItem;
import com.app.dlna.dmc.gui.customview.adapter.CustomArrayAdapter;
import com.app.dlna.dmc.processor.async.AsyncTaskWithProgressDialog;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor.DMRProcessorListner;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.Playlist;
import com.app.dlna.dmc.processor.playlist.Playlist.ViewMode;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistItem.Type;
import com.app.dlna.dmc.processor.playlist.PlaylistManager;
import com.app.dlna.dmc.utility.Utility;

public class RendererControlView extends LinearLayout {

	private int m_playingState;
	private static final int STATE_PLAYING = 0;
	private static final int STATE__PAUSE = 1;
	private static final int STATE_STOP = 2;
	private TextView m_tv_current;
	private TextView m_tv_max;
	private ImageView m_btn_playPause, m_btn_next, m_btn_prev;
	private SeekBar m_sb_duration;
	private SeekBar m_sb_volume;
	private boolean m_isSeeking = false;
	private AlertDialog m_alertDialog;
	private CustomArrayAdapter m_playlistAdapter;
	private CustomArrayAdapter m_playlistItemAdapter;
	private LinearLayout m_ll_seekControl;
	private ImageView m_btn_fakeDropdow;
	private ImageView m_btn_playlistName;
	private Button m_btn_viewMode;

	public RendererControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initComponents();
	}

	public void initComponents() {
		if (getContext() instanceof Activity) {
			Activity activity = (Activity) getContext();
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			switch (rotation) {
			case Surface.ROTATION_0:
			case Surface.ROTATION_180:
				((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
						R.layout.cv_toolbar_nowplayling_renderer_control, this);
				break;
			default:
				((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
						R.layout.cv_toolbar_nowplayling_renderer_control_land, this);
				break;
			}
		}

		m_tv_current = (TextView) findViewById(R.id.tv_current);
		m_tv_max = (TextView) findViewById(R.id.tv_max);

		m_btn_prev = (ImageView) findViewById(R.id.btn_prev);
		m_btn_prev.setOnClickListener(onPrevClick);
		m_btn_playPause = (ImageView) findViewById(R.id.btn_playPause);
		m_btn_playPause.setOnClickListener(onPlayPauseClick);
		m_btn_next = (ImageView) findViewById(R.id.btn_next);
		m_btn_next.setOnClickListener(onNextClick);

		m_sb_duration = (SeekBar) findViewById(R.id.sb_duration);
		m_sb_volume = (SeekBar) findViewById(R.id.sb_volume);
		m_sb_duration.setOnSeekBarChangeListener(onSeekListener);
		m_sb_volume.setOnSeekBarChangeListener(onSeekListener);

		m_playlistAdapter = new CustomArrayAdapter(MainActivity.INSTANCE, 0);
		m_playlistAdapter.setDropDownMode(true);

		m_playlistItemAdapter = new CustomArrayAdapter(MainActivity.INSTANCE, 0);
		m_playlistItemAdapter.setDropDownMode(true);

		m_btn_fakeDropdow = (ImageView) findViewById(R.id.btn_fakeDropdown);
		m_btn_fakeDropdow.setOnClickListener(onShowPlaylistItems);
		m_btn_playlistName = (ImageView) findViewById(R.id.tv_playlistName);
		m_btn_playlistName.setOnClickListener(onShowPlaylists);
		m_ll_seekControl = (LinearLayout) findViewById(R.id.ll_seekControl);

		m_btn_viewMode = (Button) findViewById(R.id.btn_viewmode);
		m_btn_viewMode.setOnClickListener(m_viewModeClick);
		m_btn_viewMode.setText(AppPreference.getPlaylistViewMode().getCompactString());
	}

	OnClickListener onShowPlaylists = new OnClickListener() {

		@Override
		public void onClick(View v) {
			new AsyncTaskWithProgressDialog<Void, Void, List<Playlist>>("Loading all playlists...") {

				@Override
				protected List<Playlist> doInBackground(Void... params) {
					return PlaylistManager.getAllPlaylist();
				}

				protected void onPostExecute(List<Playlist> result) {
					super.onPostExecute(result);
					ListView listView = new ListView(getContext());
					listView.setBackgroundColor(android.R.color.white);
					listView.setOnItemClickListener(onPlaylistClick);
					listView.setAdapter(m_playlistAdapter);
					m_playlistAdapter.clear();

					for (Playlist playlist : result) {
						m_playlistAdapter.add(new AdapterItem(playlist));
					}

					m_alertDialog = new AlertDialog.Builder(getContext()).setView(listView)
							.setNegativeButton("Close", null).create();
					m_alertDialog.show();
				};

			}.execute(new Void[] {});

		}
	};

	OnItemClickListener onPlaylistClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, final int position, long arg3) {
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			if (playlistProcessor != null
					&& m_playlistAdapter.getItem(position).getData().equals(playlistProcessor.getData()))
				dismissSelectDialog();
			else
				new AsyncTaskWithProgressDialog<Void, Void, PlaylistProcessor>("Loading playlist") {

					@Override
					protected PlaylistProcessor doInBackground(Void... params) {
						return PlaylistManager.getPlaylistProcessor((Playlist) m_playlistAdapter.getItem(position)
								.getData());
					}

					protected void onPostExecute(PlaylistProcessor result) {
						super.onPostExecute(result);
						if (result.getAllItems().size() > 0) {
							MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(result);
							DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
							NowPlayingActivity activity = (NowPlayingActivity) getContext();
							activity.updateItemInfo();
							activity.updatePlaylist();
							if (dmrProcessor != null) {
								dmrProcessor.setPlaylistProcessor(result);
							}
							dismissSelectDialog();
						} else {
							Toast.makeText(getContext(), "Playlist is empty", Toast.LENGTH_SHORT).show();
						}
					}

				}.execute(new Void[] {});
		}
	};

	private void dismissSelectDialog() {
		if (m_alertDialog != null)
			m_alertDialog.dismiss();
	};

	OnClickListener onShowPlaylistItems = new OnClickListener() {

		@Override
		public void onClick(View v) {
			new AsyncTaskWithProgressDialog<Void, Void, List<PlaylistItem>>("Loading all playlists...") {

				@Override
				protected List<PlaylistItem> doInBackground(Void... params) {
					PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
					if (playlistProcessor == null)
						return new ArrayList<PlaylistItem>();
					return PlaylistManager.getPlaylistProcessor(playlistProcessor.getData()).getAllItems();
				}

				protected void onPostExecute(java.util.List<PlaylistItem> result) {
					super.onPostExecute(result);
					PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
					if (result.size() == 0) {
						Toast.makeText(getContext(), "Current playlist is empty", Toast.LENGTH_SHORT).show();
						return;
					}
					ListView listView = new ListView(getContext());
					listView.setBackgroundColor(android.R.color.white);
					listView.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
							PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
							PlaylistItem item = (PlaylistItem) m_playlistItemAdapter.getItem(position).getData();
							playlistProcessor.setCurrentItem(item);
							NowPlayingActivity activity = (NowPlayingActivity) getContext();
							activity.updateItemInfo();
							if (m_alertDialog != null)
								m_alertDialog.dismiss();
						}

					});
					listView.setAdapter(m_playlistItemAdapter);
					m_playlistItemAdapter.clear();

					for (PlaylistItem playlistItem : playlistProcessor.getAllItemsByViewMode()) {
						m_playlistItemAdapter.add(new AdapterItem(playlistItem));
					}

					m_alertDialog = new AlertDialog.Builder(getContext()).setView(listView)
							.setNegativeButton("Close", null).create();
					m_alertDialog.show();
				};

			}.execute(new Void[] {});
		}
	};

	public void connectToDMR() {
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (dmrProcessor != null)
			dmrProcessor.addListener(m_dmrListener);
	}

	public void disconnectToDMR() {
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (dmrProcessor == null)
			return;
		dmrProcessor.removeListener(m_dmrListener);
	}

	private OnClickListener onPlayPauseClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			switch (m_playingState) {
			case STATE__PAUSE:
			case STATE_STOP:
				if (dmrProcessor != null)
					dmrProcessor.play();
				break;
			case STATE_PLAYING:
				if (dmrProcessor != null)
					dmrProcessor.pause();
				break;
			default:
				break;
			}
		}
	};
	// private OnClickListener onStopClick = new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// DMRProcessor dmrProcessor =
	// MainActivity.UPNP_PROCESSOR.getDMRProcessor();
	// if (dmrProcessor == null)
	// return;
	// dmrProcessor.stop();
	// }
	// };

	private OnClickListener onNextClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			NowPlayingActivity nowplaying = (NowPlayingActivity) RendererControlView.this.getContext();
			nowplaying.doNext();
		}
	};

	private OnClickListener onPrevClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			NowPlayingActivity nowplaying = (NowPlayingActivity) RendererControlView.this.getContext();
			nowplaying.doPrev();
		}
	};
	private OnSeekBarChangeListener onSeekListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			m_isSeeking = false;
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (dmrProcessor == null)
				return;
			if (seekBar.equals(m_sb_duration)) {
				dmrProcessor.seek(Utility.getTimeString(seekBar.getProgress()));
			} else {
				dmrProcessor.setVolume(seekBar.getProgress());
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			m_isSeeking = true;

		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (seekBar.equals(m_sb_duration))
				m_tv_current.setText(Utility.getTimeString(m_sb_duration.getProgress()));
		}
	};

	private DMRProcessorListner m_dmrListener = new DMRProcessorListner() {

		@Override
		public void onUpdatePosition(final long current, final long max) {
			if (m_isSeeking)
				return;
			final String currentText = Utility.getTimeString(current);
			final String maxText = Utility.getTimeString(max);
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_tv_current.setText(currentText);
					m_tv_max.setText(maxText);
					m_sb_duration.setMax((int) max);
					m_sb_duration.setProgress((int) current);
					m_sb_duration.invalidate();
					DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
					m_sb_volume.setMax(dmrProcessor.getMaxVolume());
					m_sb_volume.setProgress(dmrProcessor.getVolume());
				}
			});
		}

		@Override
		public void onStoped() {
			m_playingState = STATE_STOP;
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_btn_playPause.setImageDrawable(getContext().getResources().getDrawable(
							R.drawable.ic_btn_media_play));
					m_sb_duration.setProgress(0);
				}
			});
		}

		@Override
		public void onPlaying() {
			m_playingState = STATE_PLAYING;
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_btn_playPause.setImageDrawable(getContext().getResources().getDrawable(
							R.drawable.ic_btn_media_pause));
				}
			});

		}

		@Override
		public void onPaused() {
			m_playingState = STATE__PAUSE;
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					m_btn_playPause.setImageDrawable(getContext().getResources().getDrawable(
							R.drawable.ic_btn_media_play));
				}
			});

		}

		@Override
		public void onErrorEvent(final String error) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					MainActivity.UPNP_PROCESSOR.refreshDevicesList();
				}
			});
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onActionFail(Action actionCallback, final UpnpResponse response, final String cause) {
			MainActivity.INSTANCE.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					MainActivity.UPNP_PROCESSOR.refreshDevicesList();
				}
			});
		}

		@Override
		public void onCheckURLStart() {

		}

		@Override
		public void onCheckURLEnd() {

		}

	};

	public void updateViewForContentType(Type type) {
		switch (type) {
		case AUDIO_LOCAL:
		case AUDIO_REMOTE:
		case VIDEO_LOCAL:
		case VIDEO_REMOTE:
			m_ll_seekControl.setVisibility(View.VISIBLE);
			m_btn_playPause.setVisibility(View.VISIBLE);
			break;
		case IMAGE_LOCAL:
		case IMAGE_REMOTE:
			m_ll_seekControl.setVisibility(View.GONE);
			m_btn_playPause.setVisibility(View.GONE);
			break;
		default:
			break;
		}
	}

	private OnClickListener m_viewModeClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final ViewMode[] viewModes = ViewMode.values();
			int len = viewModes.length;
			String items[] = new String[len];
			for (int i = 0; i < len; ++i) {
				items[i] = viewModes[i].getString();
			}
			new AlertDialog.Builder(getContext()).setTitle("Select filter mode")
					.setItems(items, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							AppPreference.setPlaylistViewMode(viewModes[which]);
							((NowPlayingActivity) getContext()).updateItemInfo();
						}
					}).create().show();
		}
	};
}
