package com.app.dlna.dmc.gui.playlist;

import java.net.URL;

import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.types.UDN;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.gui.abstractactivity.UpnpListenerActivity;
import com.app.dlna.dmc.nativeui.R;
import com.app.dlna.dmc.processor.http.HTTPServerData;
import com.app.dlna.dmc.processor.impl.LocalDMRProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor.DMRProcessorListner;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.utility.Utility;

public class PlaylistActivity extends UpnpListenerActivity implements DMRProcessorListner {

	private static final String TAG = PlaylistActivity.class.getName();

	private DMRProcessor m_dmrProcessor;
	private PlaylistProcessor m_playlistProcessor;
	private static final int STATE_PAUSE = 1;
	private static final int STATE_PLAYING = 2;
	private static final int STATE_STOP = 3;
	private int m_currentState;

	private RelativeLayout m_rl_dmrController;
	private boolean m_isSeeking = false;
	private PlaylistItemArrayAdapter m_adapter;
	private ListView m_listView;
	private SeekBar m_sb_volume;
	protected boolean m_isFailed = false;
	private TextView m_tv_rendererName;
	private Button m_btn_PlayPause;
	private Button m_btn_Stop;
	private SeekBar m_sb_playingProgress;
	private TextView m_tv_progressTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "Playlist onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist_activity);
		m_listView = (ListView) findViewById(R.id.playList);
		m_adapter = new PlaylistItemArrayAdapter(PlaylistActivity.this, 0);
		m_listView.setAdapter(m_adapter);
		m_rl_dmrController = (RelativeLayout) findViewById(R.id.rl_dmrController);

		m_btn_PlayPause = (Button) findViewById(R.id.playPause);
		m_btn_Stop = (Button) findViewById(R.id.stop);
		m_btn_Stop.setEnabled(false);
		m_sb_playingProgress = (SeekBar) findViewById(R.id.playingProgress);
		m_tv_progressTime = (TextView) findViewById(R.id.progressTime);
		m_sb_volume = (SeekBar) findViewById(R.id.volumeControl);

		m_tv_rendererName = (TextView) findViewById(R.id.rendererName);

	}

	@Override
	protected void onResume() {
		Log.i(TAG, "Playlist onResume");
		super.onResume();
		prepareView();
	}

	private void prepareView() {
		if (MainActivity.UPNP_PROCESSOR != null) {
			if (MainActivity.UPNP_PROCESSOR.getPlaylistProcessor() != null) {
				m_playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
				m_listView.setOnItemClickListener(onPlaylistItemClick);
				m_listView.setOnItemLongClickListener(onPlaylistItemLongClick);
			}

			if (MainActivity.UPNP_PROCESSOR.getDMRProcessor() != null) {
				m_dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
				if (m_dmrProcessor instanceof LocalDMRProcessorImpl) {
					m_rl_dmrController.setVisibility(View.GONE);
					m_dmrProcessor.removeListener(PlaylistActivity.this);
				} else {
					m_rl_dmrController.setVisibility(View.VISIBLE);
					m_dmrProcessor.setPlaylistProcessor(m_playlistProcessor);
					m_dmrProcessor.addListener(PlaylistActivity.this);
					m_tv_rendererName.setText(m_dmrProcessor.getName());
					m_sb_playingProgress.setOnSeekBarChangeListener(playbackSeekListener);
					m_sb_volume.setOnSeekBarChangeListener(volumeSeekListener);
				}
			}

			refreshPlaylist();
		}
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "Playlist onPause");
		m_adapter.clear();
		if (m_dmrProcessor != null) {
			m_dmrProcessor.removeListener(PlaylistActivity.this);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "Playlist onDestroy");
		if (m_dmrProcessor != null)
			m_dmrProcessor.dispose();
		super.onDestroy();
	}

	private OnSeekBarChangeListener playbackSeekListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			m_isSeeking = false;
			Log.e(TAG, "Progress = " + seekBar.getProgress());
			m_dmrProcessor.seek(Utility.getTimeString(seekBar.getProgress()));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			m_isSeeking = true;

		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			m_tv_progressTime.setText(Utility.getTimeString(m_sb_playingProgress.getProgress()) + " / "
					+ Utility.getTimeString(m_sb_playingProgress.getMax()));
		}
	};

	public void onPlayPauseClick(View view) {
		switch (m_currentState) {
		case STATE_PAUSE:
		case STATE_STOP:
			m_dmrProcessor.play();
			break;
		case STATE_PLAYING:
			m_dmrProcessor.pause();
			break;
		default:
			break;
		}
	}

	public void onStopClick(View view) {
		m_dmrProcessor.stop();
	}

	public void onChangeClick(View view) {
		showDMRList();
	}

	private void showDMRList() {

	}

	public void onNextClick(View view) {
		doNext();
	}

	private void doNext() {
		if (m_playlistProcessor != null && m_dmrProcessor != null) {
			m_playlistProcessor.next();
			updateCurrentPlaylistItem();
		}
	}

	private void updateCurrentPlaylistItem() {
		final PlaylistItem item = m_playlistProcessor.getCurrentItem();
		if (item != null) {
			m_adapter.setCurrentItem(item);
			validateListView(item);
			if (m_dmrProcessor != null)
				m_dmrProcessor.setURIandPlay(item.getUri());
		}
	}

	public void onPreviousClick(View view) {
		doPrevious();
	}

	private void doPrevious() {
		if (m_playlistProcessor != null && m_dmrProcessor != null) {
			m_playlistProcessor.previous();
			updateCurrentPlaylistItem();
		}
	}

	private void validateListView(final PlaylistItem item) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_adapter.notifyDataSetChanged();
				m_listView.smoothScrollToPosition(m_adapter.getPosition(item));
			}
		});
	}

	public void onSoundClick(View view) {
		if (m_sb_volume.getVisibility() == View.VISIBLE) {
			m_sb_volume.setVisibility(View.GONE);
		} else {
			m_sb_volume.setVisibility(View.VISIBLE);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onActionFail(final Action actionCallback, final UpnpResponse operation, final String cause) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Log.e(TAG, "Action fail: " + actionCallback.toString() + "; response = " + operation.toString() + "; cause = "
						+ cause);
				m_dmrProcessor.dispose();
				MainActivity.UPNP_PROCESSOR.setCurrentDMR(new UDN("null"));
				if (!m_isFailed) {
					m_isFailed = true;
					try {
						new AlertDialog.Builder(PlaylistActivity.this).setTitle("Error")
								.setMessage("Remote Device Error: " + cause)
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										PlaylistActivity.this.finish();
									}
								}).setCancelable(false).show();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

			}
		});

	}

	@Override
	public void onErrorEvent(final String error) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Log.e(TAG, "onErrorEvent: " + error);
				m_dmrProcessor.dispose();
				MainActivity.UPNP_PROCESSOR.setCurrentDMR(new UDN("null"));
				if (!m_isFailed) {
					m_isFailed = true;
					try {
						new AlertDialog.Builder(PlaylistActivity.this).setTitle("Error")
								.setMessage("Remote Device Error: " + error)
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										PlaylistActivity.this.finish();
									}
								}).setCancelable(false).show();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

			}
		});
	}

	@Override
	public void onUpdatePosition(final long current, final long max) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (!m_isSeeking) {
					m_sb_playingProgress.setMax((int) max);
					m_sb_playingProgress.setProgress((int) current);
					m_sb_playingProgress.invalidate();
					m_tv_progressTime.setText(Utility.getTimeString(current) + " / " + Utility.getTimeString(max));
				}
				m_sb_volume.setProgress(m_dmrProcessor.getVolume());
			}
		});
	}

	@Override
	public void onPaused() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_btn_PlayPause.setBackgroundResource(R.drawable.ic_media_play);
				m_btn_Stop.setEnabled(true);
				m_currentState = STATE_PAUSE;
			}
		});

	}

	@Override
	public void onStoped() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_btn_Stop.setEnabled(false);
				m_currentState = STATE_STOP;
				m_btn_PlayPause.setBackgroundResource(R.drawable.ic_media_play);
			}
		});

	}

	@Override
	public void onPlaying() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				m_btn_PlayPause.setBackgroundResource(R.drawable.ic_media_pause);
				m_btn_Stop.setEnabled(true);
				m_currentState = STATE_PLAYING;
			}
		});

	}

	@Override
	public void onEndTrack() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Next", Toast.LENGTH_SHORT);
			}
		});
		doNext();
	}

	private OnSeekBarChangeListener volumeSeekListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			m_dmrProcessor.setVolume(seekBar.getProgress());
			Log.e(TAG, "Stop tracking");
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			Log.e(TAG, "Start tracking");
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		}
	};

	private OnItemClickListener onPlaylistItemClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adaper, View view, int position, long arg3) {
			playItem(position);
		}
	};

	private void playItem(int position) {
		PlaylistItem item = m_adapter.getItem(position);
		String url = item.getUri();
		if (m_dmrProcessor == null) {
			// Toast.makeText(PlaylistActivity.this, "Cannot get DMRProcessor",
			// Toast.LENGTH_SHORT).show();
			new AlertDialog.Builder(PlaylistActivity.this).setTitle("Error")
					.setMessage("Cannot get DMRProcessor. Please select another one")
					.setPositiveButton("Ok", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							PlaylistActivity.this.finish();
						}
					}).create().show();
		} else {
			m_dmrProcessor.setURIandPlay(url);
			m_playlistProcessor.setCurrentItem(item);
			m_adapter.setCurrentItem(item);
			validateListView(item);
		}
	}

	private OnItemLongClickListener onPlaylistItemLongClick = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View view, final int position, long arg3) {
			String actionList[] = null;
			final PlaylistItem playlistItem = m_adapter.getItem(position);
			try {
				URL url = new URL(playlistItem.getUri());
				if (url.getHost().equals(HTTPServerData.HOST)) {
					actionList = new String[2];
					actionList[0] = "Play";
					actionList[1] = "Remove";
				} else {
					actionList = new String[3];
					actionList[0] = "Play";
					actionList[1] = "Remove";
					actionList[2] = "Download";
				}

			} catch (Exception ex) {
				Toast.makeText(PlaylistActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
			}

			new AlertDialog.Builder(PlaylistActivity.this).setTitle("Select action").setCancelable(false)
					.setItems(actionList, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								playItem(position);
								break;
							case 1:
								PlaylistItem item = m_adapter.getItem(position);
								m_playlistProcessor.removeItem(item);
								m_adapter.remove(item);
								m_adapter.notifyDataSetChanged();
								break;
							case 2:
								if (MainActivity.UPNP_PROCESSOR != null) {
									MainActivity.UPNP_PROCESSOR.getDownloadProcessor().startDownload(playlistItem.getTitle(),
											playlistItem.getUri());
								}
								break;
							default:
								break;
							}
						}
					}).setNegativeButton("Cancel", null).create().show();

			return true;

		}
	};

	private void refreshPlaylist() {
		synchronized (m_adapter) {
			if (m_playlistProcessor != null) {
				m_adapter.clear();
				for (PlaylistItem item : m_playlistProcessor.getAllItems()) {
					m_adapter.add(item);
				}
			}
		}
	}

}
