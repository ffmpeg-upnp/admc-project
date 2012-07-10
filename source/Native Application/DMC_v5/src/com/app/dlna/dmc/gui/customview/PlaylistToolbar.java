package com.app.dlna.dmc.gui.customview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import app.dlna.controller.v5.R;

import com.app.dlna.dmc.gui.activity.LibraryActivity;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.async.AsyncTaskWithProgressDialog;
import com.app.dlna.dmc.processor.impl.PlaylistManager;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.model.Playlist;

public class PlaylistToolbar extends LinearLayout {

	private ImageView m_btn_back;
	private ImageView m_btn_save;
	private ImageView m_btn_clear;
	private ImageView m_btn_remove;
	private PlaylistView m_playlistView;

	public PlaylistToolbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.cv_toolbar_playlist, this);
		m_btn_back = (ImageView) findViewById(R.id.btn_back);
		m_btn_back.setOnClickListener(m_backClick);
		m_btn_save = (ImageView) findViewById(R.id.btn_save);
		m_btn_save.setOnClickListener(m_saveClick);
		m_btn_clear = (ImageView) findViewById(R.id.btn_clear);
		m_btn_clear.setOnClickListener(m_clearClick);
		m_btn_remove = (ImageView) findViewById(R.id.btn_remove);
		m_btn_remove.setOnClickListener(m_removeClick);
	}

	public void setLocalNetworkView(HomeNetworkView localNetworkView) {
	}

	public void setBackButtonEnabled(boolean enabled) {
		m_btn_back.setEnabled(enabled);
	}

	public void setPlaylistView(PlaylistView playlistView) {
		m_playlistView = playlistView;
	}

	private OnClickListener m_backClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			m_playlistView.backToListPlaylist();
		}
	};

	private OnClickListener m_saveClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			PlaylistProcessor currentPlaylistProcessor = ((LibraryActivity) getContext()).getPlaylistView()
					.getCurrentPlaylistProcessor();
			if (currentPlaylistProcessor.getData().getId() == 1 && currentPlaylistProcessor.getAllItems().size() > 0)
				createSaveDialog();
			else
				Toast.makeText(getContext(), getContext().getResources().getString(R.string.playlist_is_empty),
						Toast.LENGTH_SHORT).show();
		}
	};
	private OnClickListener m_clearClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			new AlertDialog.Builder(getContext()).setMessage(R.string.warning).setTitle(R.string.confirm_delete)
					.setMessage(R.string.this_will_clear_all_item_in_this_playlist)
					.setPositiveButton(getContext().getString(R.string.ok), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							new AsyncTaskWithProgressDialog<Void, Void, Void>(getContext().getResources().getString(
									R.string.clear_playlist)) {
								@Override
								protected Void doInBackground(Void... params) {
									long playlistId = m_playlistView.getCurrentPlaylistProcessor().getData().getId();
									PlaylistManager.clearPlaylist(playlistId);
									PlaylistProcessor processor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
									if (processor != null && processor.getData().getId() == playlistId) {
										processor.updateItemList();
										DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
										if (dmrProcessor != null)
											dmrProcessor.stop();
									}
									LibraryActivity activity = (LibraryActivity) getContext();
									activity.getPlaylistView().udpateCurrentPlaylistProcessor();
									return null;
								}

								protected void onPostExecute(Void result) {
									super.onPostExecute(result);
									m_playlistView.backToListPlaylist();
								};

							}.execute(new Void[] {});
						}
					}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

						}
					}).create().show();
		}
	};
	private OnClickListener m_removeClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			PlaylistProcessor processor = m_playlistView.getCurrentPlaylistProcessor();
			if (processor.getData().getId() != 1) {
				confirmDelete();
			}
		}
	};

	private void createSaveDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

		alert.setTitle(R.string.save_playlist);
		alert.setMessage(R.string.insert_playlist_name_);

		final EditText input = new EditText(getContext());
		alert.setView(input);

		alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value != null && value.trim().length() != 0) {
					createPlaylist(value);
				}
			}
		});

		alert.setNegativeButton(R.string.cancel, null);

		alert.show();
	}

	private void createPlaylist(String name) {
		final Playlist playlist = new Playlist();
		playlist.setName(name);

		new AsyncTaskWithProgressDialog<Void, Void, Boolean>(getContext().getResources().getString(
				R.string.create_playlist)) {

			@Override
			protected Boolean doInBackground(Void... params) {
				return Boolean.valueOf(PlaylistManager.createPlaylist(playlist));
			}

			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				if (result) {
					new AsyncTaskWithProgressDialog<Void, Void, PlaylistProcessor>("") {

						@Override
						protected PlaylistProcessor doInBackground(Void... params) {
							return PlaylistManager.getPlaylistProcessor(playlist);
						}

						protected void onPostExecute(PlaylistProcessor result) {
							super.onPostExecute(result);
							PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
							if (playlistProcessor != null && playlistProcessor.getData().getId() == 1) {
								int currentPost = playlistProcessor.getCurrentItemIndex();
								result.setCurrentItem(currentPost);
								MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(result);
							}
							m_playlistView.backToListPlaylist();
						};

					}.execute(new Void[] {});

				} else {
					Toast.makeText(getContext(), getContext().getResources().getString(R.string.create_playlist_fail),
							Toast.LENGTH_SHORT).show();
				}
			};
		}.execute(new Void[] {});
	}

	private void confirmDelete() {
		final PlaylistProcessor processor = m_playlistView.getCurrentPlaylistProcessor();
		AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
		alert.setTitle(R.string.confirm_delete);
		alert.setMessage(getContext().getString(R.string.are_you_sure_to_delete_) + processor.getData().getName()
				+ "\"?");
		alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				new AsyncTaskWithProgressDialog<Void, Void, Boolean>(getContext().getString(R.string.delete_playlist)) {

					@Override
					protected Boolean doInBackground(Void... params) {
						return PlaylistManager.deletePlaylist(processor);
					}

					@Override
					protected void onPostExecute(Boolean result) {
						super.onPostExecute(result);
						String resultText = "";
						if (result) {
							resultText = getContext().getString(R.string.delete_playlist_success);
							if (MainActivity.UPNP_PROCESSOR.getPlaylistProcessor() != null
									&& MainActivity.UPNP_PROCESSOR.getPlaylistProcessor().getData().getId() == processor
											.getData().getId()) {
								MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(null);
								DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
								if (dmrProcessor != null)
									dmrProcessor.stop();

							}
						} else {
							resultText = getContext().getString(R.string.delete_playlist_failed_try_again_later);
						}
						Toast.makeText(getContext(), resultText, Toast.LENGTH_SHORT).show();
						m_playlistView.backToListPlaylist();
					}

				}.execute(new Void[] {});
			}
		});

		alert.setNegativeButton(R.string.cancel, null);

		alert.show();
	}

	public void updateToolbar(int state) {
		switch (state) {
		case PlaylistView.VM_LIST:
			this.setVisibility(View.GONE);
			break;
		case PlaylistView.VM_DETAILS:
			this.setVisibility(View.VISIBLE);
			PlaylistProcessor processor = m_playlistView.getCurrentPlaylistProcessor();
			if (processor.getData().getId() == 1) {
				// Unsaved playlist
				m_btn_remove.setVisibility(View.GONE);
				m_btn_save.setVisibility(View.VISIBLE);
			} else {
				m_btn_remove.setVisibility(View.VISIBLE);
				m_btn_save.setVisibility(View.GONE);
			}
			break;
		}
	}
}
