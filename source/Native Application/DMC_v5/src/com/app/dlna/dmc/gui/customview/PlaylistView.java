package com.app.dlna.dmc.gui.customview;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import app.dlna.controller.v5.R;

import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.async.AsyncTaskWithProgressDialog;
import com.app.dlna.dmc.processor.impl.PlaylistManager;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor.ChangeMode;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor.PlaylistListener;
import com.app.dlna.dmc.processor.model.Playlist;
import com.app.dlna.dmc.processor.model.Playlist.ViewMode;
import com.app.dlna.dmc.processor.model.PlaylistItem;

public class PlaylistView extends DMRListenerView {

	private PlaylistToolbar m_playlistToolbar;
	public static final int VM_LIST = 0;
	public static final int VM_DETAILS = 1;
	protected static final String TAG = PlaylistView.class.getName();
	private int m_viewMode = -1;
	private PlaylistProcessor m_currentPlaylist = null;

	private PlaylistListener m_playlistListener = new PlaylistListener() {
		@Override
		public void onItemChanged(PlaylistItem item, ChangeMode changeMode) {
			if (MainActivity.UPNP_PROCESSOR == null || MainActivity.UPNP_PROCESSOR.getPlaylistProcessor() == null)
				return;
			if (item != null) {
				DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
				if (dmrProcessor != null)
					dmrProcessor.setURIandPlay(item);
			}
		}
	};

	private OnItemLongClickListener m_playlistItemLongClick = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {

			if (m_adapter.getItem(position).getData() instanceof PlaylistItem) {
				new AlertDialog.Builder(getContext())
						.setTitle(R.string.select_action)
						.setItems(getResources().getStringArray(R.array.playlistitem_contextmenu),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										switch (which) {
										case 0:
											PlaylistItem playlistItem = (PlaylistItem) m_adapter.getItem(position).getData();
											MainActivity.UPNP_PROCESSOR.getDownloadProcessor().startDownload(playlistItem);
											break;
										default:
											break;
										}
										dialog.dismiss();
									}
								}).create().show();
				return true;
			} else {
				String[] actions = null;
				if (position == 0) {
					actions = getResources().getStringArray(R.array.playlist_unsaved_contextmenu);
				} else {
					actions = getResources().getStringArray(R.array.playlist_contextmenu);
				}
				new AlertDialog.Builder(getContext()).setTitle(R.string.select_action)
						.setItems(actions, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
								case 0:
									cleanPlaylist((Playlist) m_adapter.getItem(position).getData());
									break;
								case 1:
									renamePlaylist((Playlist) m_adapter.getItem(position).getData());
									break;
								case 2:
									removePlaylist((Playlist) m_adapter.getItem(position).getData());
								default:
									break;
								}
								dialog.dismiss();
							}

						}).create().show();
				return true;
			}
		}
	};

	private void removePlaylist(final Playlist playlist) {
		new AlertDialog.Builder(getContext()).setTitle(R.string.comfirm)
				.setMessage(R.string.are_you_sure_to_remove_this_playlist_)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						new Thread(new Runnable() {

							@Override
							public void run() {
								MainActivity.INSTANCE.showLoadingMessage(getContext().getString(R.string.cleaning_playlist));
								PlaylistManager.deletePlaylist(playlist);
								MainActivity.INSTANCE.runOnUiThread(new Runnable() {

									@Override
									public void run() {
										m_adapter.clear();
										preparePlaylist();
									}
								});
								PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
								if (playlistProcessor != null && playlistProcessor.getData().getId() == playlist.getId()) {
									MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(null);
									DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
									if (dmrProcessor != null) {
										dmrProcessor.stop();
										dmrProcessor.setPlaylistProcessor(MainActivity.UPNP_PROCESSOR.getPlaylistProcessor());
									}

								}

								MainActivity.INSTANCE.dismissLoadingDialog();
								MainActivity.INSTANCE.showToast(getContext().getString(R.string.remove_playlist_sucessfully));
							}
						}).start();

					}
				}).setNegativeButton(R.string.cancel, null).create().show();
	}

	private void renamePlaylist(final Playlist playlist) {
		AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

		alert.setTitle(R.string.change_playlist_name);
		alert.setMessage(R.string.insert_playlist_name_);

		final EditText input = new EditText(getContext());
		alert.setView(input);

		alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value != null && value.trim().length() != 0) {
					PlaylistManager.renamePlaylist(playlist.getId(), value);
					m_adapter.clear();
					preparePlaylist();
				}
			}
		});

		alert.setNegativeButton(R.string.cancel, null);

		alert.show();
	}

	private void cleanPlaylist(final Playlist playlist) {
		new AlertDialog.Builder(getContext()).setTitle(R.string.comfirm)
				.setMessage(R.string.are_you_sure_to_clean_this_playlist_)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						new Thread(new Runnable() {

							@Override
							public void run() {
								MainActivity.INSTANCE.showLoadingMessage(getContext().getString(R.string.cleaning_playlist));
								PlaylistManager.clearPlaylist(playlist.getId());
								MainActivity.INSTANCE.dismissLoadingDialog();
								MainActivity.INSTANCE.showToast(getContext().getString(R.string.clean_playlist_sucessfully));
								PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
								if (playlistProcessor != null && playlistProcessor.getData().getId() == playlist.getId()) {
									MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(PlaylistManager
											.getPlaylistProcessor(playlist));
									DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
									if (dmrProcessor != null) {
										dmrProcessor.stop();
										dmrProcessor.setPlaylistProcessor(MainActivity.UPNP_PROCESSOR.getPlaylistProcessor());
									}

								}
							}
						}).start();

					}
				}).setNegativeButton(R.string.cancel, null).create().show();
	}

	public PlaylistView(Context context) {
		super(context);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cv_playlist_allitem, this);
		m_listView = (ListView) findViewById(R.id.lv_playlist);
		m_adapter = new CustomArrayAdapter(getContext(), 0);
		m_listView.setAdapter(m_adapter);
		m_listView.setOnItemClickListener(m_playlistItemClick);
		m_listView.setOnItemLongClickListener(m_playlistItemLongClick);

		m_playlistToolbar = (PlaylistToolbar) findViewById(R.id.botToolbar);
		m_playlistToolbar.setPlaylistView(this);
		super.updateListView();
		m_viewMode = VM_LIST;
		preparePlaylist();
	}

	public void preparePlaylist() {
		if (MainActivity.UPNP_PROCESSOR != null && MainActivity.UPNP_PROCESSOR.getPlaylistProcessor() != null)
			MainActivity.UPNP_PROCESSOR.getPlaylistProcessor().addListener(m_playlistListener);
		switch (m_viewMode) {
		case VM_DETAILS:
			if (m_currentPlaylist == null || m_currentPlaylist.getData() == null || m_currentPlaylist.getData().getName() == null) {
				m_viewMode = VM_LIST;
				preparePlaylist();
				return;
			}
			m_currentPlaylist = PlaylistManager.getPlaylistProcessor(m_currentPlaylist.getData());
			m_adapter.clear();
			for (PlaylistItem item : m_currentPlaylist.getAllItems()) {
				m_adapter.add(new AdapterItem(item));
			}
			m_playlistToolbar.setVisibility(View.VISIBLE);
			m_playlistToolbar.updateToolbar(m_viewMode);
			break;
		case VM_LIST:
			new AsyncTaskWithProgressDialog<Void, Void, List<Playlist>>(getContext().getString(R.string.loading_all_playlist)) {

				@Override
				protected void onPreExecute() {
				}

				@Override
				protected List<Playlist> doInBackground(Void... params) {
					return PlaylistManager.getAllPlaylist();
				}

				@Override
				protected void onPostExecute(List<Playlist> result) {
					if (m_adapter.getCount() > 0) {
						if (!(m_adapter.getItem(0).getData() instanceof Playlist))
							m_adapter.clear();
					}
					for (Playlist playlist : result)
						if (m_adapter.getPosition(new AdapterItem(playlist)) < 0)
							m_adapter.add(new AdapterItem(playlist));
					m_playlistToolbar.setVisibility(View.GONE);
				}
			}.execute(new Void[] {});
			break;
		default:
			break;
		}

	}

	private OnItemClickListener m_playlistItemClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			final Object object = m_adapter.getItem(position).getData();
			if (object instanceof Playlist) {

				new AsyncTaskWithProgressDialog<Void, Void, PlaylistProcessor>(getContext().getString(
						R.string.loading_playlist_items)) {

					@Override
					protected PlaylistProcessor doInBackground(Void... params) {
						return PlaylistManager.getPlaylistProcessor((Playlist) object);
					}

					protected void onPostExecute(PlaylistProcessor playlistProcessor) {
						m_currentPlaylist = playlistProcessor;
						m_viewMode = VM_DETAILS;
						preparePlaylist();
						super.onPostExecute(playlistProcessor);
					};
				}.execute(new Void[] {});
			} else if (object instanceof PlaylistItem) {
				if (m_currentPlaylist == null) {
					Toast.makeText(getContext(), R.string.cannot_get_playlist, Toast.LENGTH_SHORT).show();
					return;
				}
				DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
				if (dmrProcessor == null) {
					Toast.makeText(getContext(), R.string.cannot_connect_to_renderer, Toast.LENGTH_SHORT).show();
					return;
				}
				MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(m_currentPlaylist);
				m_currentPlaylist.addListener(m_playlistListener);
				dmrProcessor.setPlaylistProcessor(m_currentPlaylist);
				PlaylistItem playlistItem = (PlaylistItem) object;
				m_currentPlaylist.setCurrentItem(playlistItem);
				switch (playlistItem.getType()) {
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
			}

		}
	};

	public void backToListPlaylist() {
		super.updateListView();
		m_viewMode = VM_LIST;
		preparePlaylist();
	}

	public PlaylistProcessor getCurrentPlaylistProcessor() {
		if (m_currentPlaylist == null) {
			Playlist unsaved = new Playlist();
			unsaved.setId(1);
			m_currentPlaylist = PlaylistManager.getPlaylistProcessor(unsaved);
		}
		return m_currentPlaylist;
	}

	public void udpateCurrentPlaylistProcessor() {
		if (m_currentPlaylist == null) {
			Playlist unsaved = new Playlist();
			unsaved.setId(1);
			m_currentPlaylist = PlaylistManager.getPlaylistProcessor(unsaved);
		} else {
			m_currentPlaylist = PlaylistManager.getPlaylistProcessor(m_currentPlaylist.getData());
		}
	}

}
