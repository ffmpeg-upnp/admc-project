package com.app.dlna.dmc.gui.activity;

import java.io.IOException;
import java.net.MalformedURLException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.customview.adapter.AdapterItem;
import com.app.dlna.dmc.gui.customview.adapter.CustomArrayAdapter;
import com.app.dlna.dmc.gui.customview.nowplaying.LocalMediaPlayer;
import com.app.dlna.dmc.gui.customview.nowplaying.RendererControlView;
import com.app.dlna.dmc.gui.customview.nowplaying.TouchImageView;
import com.app.dlna.dmc.processor.async.AsyncTaskWithProgressDialog;
import com.app.dlna.dmc.processor.impl.LocalDMRProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor.PlaylistListener;
import com.app.dlna.dmc.processor.playlist.Playlist.ViewMode;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.utility.Utility;

public class NowPlayingActivity extends Activity {
	private RendererControlView m_rendererControl;
	private ViewFlipper m_viewFlipper;
	private ProgressDialog m_progressDialog;
	private Animation m_animFlipInNext;
	private Animation m_animFlipOutNext;
	private Animation m_animFlipInPrevious;
	private Animation m_animFlipOutPrevious;
	private SurfaceView m_surface;
	private TouchImageView m_image;
	private boolean m_isPausing;
	private RelativeLayout m_content;
	private Bitmap m_previousBitmap;
	private boolean m_lastInfoState = true;
	private SwipeDetector m_swipeDetector;
	private ListView m_playlistView;
	private CustomArrayAdapter m_adapter;
	private AsyncTask<String, Void, Bitmap> m_loadingImageTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeComponents();
	}

	public void initializeComponents() {
		setContentView(R.layout.activity_nowplaying);
		m_rendererControl = (RendererControlView) findViewById(R.id.rendererControlView);
		m_rendererControl.setVisibility(View.VISIBLE);
		m_progressDialog = new ProgressDialog(NowPlayingActivity.this);
		m_progressDialog.setTitle("Loading image");
		m_progressDialog.setCancelable(true);

		m_swipeDetector = new SwipeDetector(this);

		m_content = (RelativeLayout) findViewById(R.id.content);
		m_content.setOnTouchListener(m_swipeDetector);
		m_image = (TouchImageView) findViewById(R.id.image);
		m_image.setDrawingCacheEnabled(false);
		m_image.setMaxZoom(4f);
		if (!AppPreference.getImageZoomable()) {
			m_image.setOnTouchListener(m_swipeDetector);
		}
		m_image.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				NowPlayingActivity.this.toggleItemInfo();
			}
		});

		m_surface = (SurfaceView) findViewById(R.id.surface);
		m_surface.setOnTouchListener(m_swipeDetector);
		m_surface.getHolder().addCallback(m_surfaceCallback);

		m_animFlipInNext = AnimationUtils.loadAnimation(this, R.anim.flipinnext);
		m_animFlipInNext.setAnimationListener(m_animationListner);
		m_animFlipOutNext = AnimationUtils.loadAnimation(this, R.anim.flipoutnext);
		m_animFlipOutNext.setAnimationListener(m_animationListner);
		m_animFlipInPrevious = AnimationUtils.loadAnimation(this, R.anim.flipinprevious);
		m_animFlipInPrevious.setAnimationListener(m_animationListner);
		m_animFlipOutPrevious = AnimationUtils.loadAnimation(this, R.anim.flipoutprevious);
		m_animFlipOutPrevious.setAnimationListener(m_animationListner);

		m_viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
		m_viewFlipper.setOnTouchListener(m_swipeDetector);
		m_viewFlipper.addView(getLayoutInflater().inflate(R.layout.cv_tv_title, null));
		m_viewFlipper.addView(getLayoutInflater().inflate(R.layout.cv_tv_title, null));
		m_playlistView = (ListView) findViewById(R.id.playlist);
		m_playlistView.setTag(AppPreference.getPlaylistViewMode());
		m_adapter = new CustomArrayAdapter(NowPlayingActivity.this, 0);
		m_adapter.setTag(AppPreference.getPlaylistViewMode());
		m_adapter.setDropDownMode(true);
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor != null) {
			// List<PlaylistItem> items =
			// playlistProcessor.getAllItemsByViewMode();
			// for (int i = 0; i < items.size(); ++i) {
			// PlaylistItem item = items.get(i);
			// m_adapter.add(new AdapterItem(item));
			// }
			for (PlaylistItem item : playlistProcessor.getAllItemsByViewMode())
				m_adapter.add(new AdapterItem(item));
			PlaylistItem current = playlistProcessor.getCurrentItem();
			if (current == null) {
				m_playlistView.smoothScrollToPosition(0);
			} else {
				int idx = 0;
				if (AppPreference.getPlaylistViewMode().equals(ViewMode.ALL)) {
					idx = playlistProcessor.getAllItems().indexOf(current);
				} else {
					idx = playlistProcessor.getAllItemsByViewMode().indexOf(current);
				}

				m_playlistView.smoothScrollToPosition(idx < 3 ? 0 : idx + 3);
			}
		}
		m_playlistView.setAdapter(m_adapter);
		m_playlistView.setOnItemClickListener(m_playlistItemClick);
		m_playlistView.setVisibility(View.VISIBLE);
		if (m_lastInfoState) {
			m_viewFlipper.setVisibility(View.VISIBLE);
			m_rendererControl.setVisibility(View.VISIBLE);
		} else {
			m_viewFlipper.setVisibility(View.GONE);
			m_rendererControl.setVisibility(View.GONE);
		}
	}

	private OnItemClickListener m_playlistItemClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			m_lastInfoState = true;
			final Object object = m_adapter.getItem(position).getData();
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			if (playlistProcessor == null)
				return;
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (dmrProcessor == null) {
				Toast.makeText(NowPlayingActivity.this, "Cannot connect to renderer", Toast.LENGTH_SHORT).show();
				return;
			}
			playlistProcessor.setCurrentItem((PlaylistItem) object);
			updateItemInfo();
		}
	};

	private void initPortrait() {
		initializeComponents();
		m_rendererControl.initComponents();
	}

	private void initLandscape() {
		initializeComponents();
		m_rendererControl.initComponents();
	}

	private PlaylistListener m_playlistListener = new PlaylistListener() {

		@Override
		public void onPrev() {
			cancelLoadingTask();
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (m_viewFlipper == null)
						return;
					if (m_viewFlipper.isShown()) {
						m_viewFlipper.setInAnimation(m_animFlipInPrevious);
						m_viewFlipper.setOutAnimation(m_animFlipOutPrevious);
						m_viewFlipper.showPrevious();
					} else {
						updateItemInfo();
						m_swipeDetector.setEnable(true);
					}
				}
			});
		}

		@Override
		public void onNext() {
			cancelLoadingTask();
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (m_viewFlipper == null)
						return;
					if (m_viewFlipper.isShown()) {
						m_viewFlipper.setInAnimation(m_animFlipInNext);
						m_viewFlipper.setOutAnimation(m_animFlipOutNext);
						m_viewFlipper.showNext();
					} else {
						updateItemInfo();
						m_swipeDetector.setEnable(true);
					}

				}
			});

		}

	};

	private void cancelLoadingTask() {
		try {
			if (m_loadingImageTask != null && !m_loadingImageTask.isCancelled())
				m_loadingImageTask.cancel(true);
			m_loadingImageTask = null;
		} catch (Exception ex) {

		}
	}

	private AnimationListener m_animationListner = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
			m_swipeDetector.setEnable(false);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			m_swipeDetector.setEnable(false);
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			PlaylistItem item = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor().getCurrentItem();
			if (item != null)
				((TextView) m_viewFlipper.getCurrentView()).setText(item.getTitle());
			updateItemInfo();
			m_swipeDetector.setEnable(true);
		}
	};

	public void doNext() {
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor != null)
			playlistProcessor.next();
	}

	public void doPrev() {
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor != null)
			playlistProcessor.previous();
	}

	public void updateItemInfo() {
		updateRotation();
		m_rendererControl.connectToDMR();
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
		if (dmrProcessor == null || playlistProcessor == null)
			return;
		PlaylistItem playing = dmrProcessor.getCurrentItem();
		if (playing != null && !AppPreference.getPlaylistViewMode().compatibleWith(playing.getType())) {
			dmrProcessor.stop();
			dmrProcessor.setURIandPlay(null);
			playlistProcessor.updateForViewMode();
		}
		PlaylistItem item = playlistProcessor.getCurrentItem();
		if (item == null) {
			dmrProcessor.stop();
			return;
		}
		m_rendererControl.updateViewForContentType(item.getType());
		((TextView) m_viewFlipper.getCurrentView()).setText(item.getTitle());
		switch (item.getType()) {
		case AUDIO_LOCAL:
		case AUDIO_REMOTE: {
			// m_image.setVisibility(View.VISIBLE);
			// m_image.setImageBitmap(BitmapFactory.decodeResource(getResources(),
			// R.drawable.ic_didlobject_audio_large));
			// m_image.setMaxZoom(1f);
			// m_image.setOnTouchListener(m_swipeDetector);
			m_image.setVisibility(View.GONE);
			m_surface.setVisibility(View.GONE);
			m_playlistView.setVisibility(View.VISIBLE);
			m_lastInfoState = true;
			break;
		}
		case YOUTUBE:
		case VIDEO_LOCAL:
		case VIDEO_REMOTE: {
			m_image.setVisibility(View.GONE);
			if (dmrProcessor instanceof LocalDMRProcessorImpl) {
				m_surface.setVisibility(View.VISIBLE);
				m_playlistView.setVisibility(View.GONE);

			} else {
				// m_image.setVisibility(View.VISIBLE);
				// m_image.setImageBitmap(BitmapFactory.decodeResource(getResources(),
				// R.drawable.ic_didlobject_video_large));
				// m_image.setMaxZoom(1f);
				// m_image.setOnTouchListener(m_swipeDetector);
				m_playlistView.setVisibility(View.VISIBLE);
				m_surface.setVisibility(View.GONE);
				m_lastInfoState = true;
			}
			break;
		}
		case IMAGE_LOCAL:
		case IMAGE_REMOTE: {
			m_playlistView.setVisibility(View.GONE);
			cancelLoadingTask();
			m_loadingImageTask = new AsyncTaskWithProgressDialog<String, Void, Bitmap>("Loading image") {

				protected void onPreExecute() {
					if (m_previousBitmap != null)
						m_previousBitmap.recycle();
					System.gc();
					m_image.setVisibility(View.GONE);
					m_content.findViewById(R.id.loading_icon).setVisibility(View.VISIBLE);
					m_surface.setVisibility(View.GONE);
				};

				@Override
				protected Bitmap doInBackground(String... params) {
					String url = params[0];
					PlaylistItem item = new PlaylistItem();
					item.setUrl(url);
					Utility.checkItemURL(item);
					int width = Integer.parseInt(params[1]);
					int height = Integer.parseInt(params[2]);
					try {
						return Utility.getBitmapFromURL(url, width < height ? width : height);
					} catch (MalformedURLException e) {
						e.printStackTrace();
						return null;
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					} catch (OutOfMemoryError e) {
						e.printStackTrace();
						return null;
					}
				}

				@Override
				protected void onPostExecute(Bitmap result) {
					m_image.setVisibility(View.VISIBLE);
					m_content.findViewById(R.id.loading_icon).setVisibility(View.GONE);
					if (m_previousBitmap != null)
						m_previousBitmap.recycle();
					System.gc();
					m_previousBitmap = result;
					if (result == null) {
						Toast.makeText(NowPlayingActivity.this,
								"Image loading error. Reduce image quality in Settings maybe fix this problem",
								Toast.LENGTH_SHORT).show();
						m_image.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_didlobject_image_large));
						m_image.setMaxZoom(1f);
						m_image.setOnTouchListener(m_swipeDetector);
					} else {
						m_image.setImageBitmap(result);
						m_image.setMaxZoom(4f);
					}
				}
			};
			m_loadingImageTask.execute(new String[] { item.getUrl(), String.valueOf(AppPreference.getImageDimension()),
					String.valueOf(AppPreference.getImageDimension()) });
			break;
		}
		default: {
			break;
		}
		}
		updateSurfaceView();
		dmrProcessor.setURIandPlay(item);
	}

	public void updateRotation() {
		int displayMode = getWindowManager().getDefaultDisplay().getRotation();
		if (displayMode == Surface.ROTATION_0 || displayMode == Surface.ROTATION_180) {
			initPortrait();
		} else {
			initLandscape();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		m_lastInfoState = true;
		m_isPausing = false;
		updatePlaylist();
		updateItemInfo();
	}

	@Override
	protected void onPause() {
		super.onPause();
		m_isPausing = true;
		m_surface.setVisibility(View.GONE);
		updateSurfaceView();
		m_rendererControl.disconnectToDMR();
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor != null)
			playlistProcessor.removeListener(m_playlistListener);
		m_adapter = null;
	}

	public void updatePlaylist() {
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor != null)
			playlistProcessor.addListener(m_playlistListener);
	}

	public void updateDMRControlView() {
		m_rendererControl.connectToDMR();
	}

	@Override
	public void onBackPressed() {
		MainActivity.INSTANCE.switchToLibrary();
	}

	private void updateSurfaceView() {
		try {
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (dmrProcessor == null)
				return;
			if (dmrProcessor instanceof LocalDMRProcessorImpl) {
				LocalDMRProcessorImpl localDMR = (LocalDMRProcessorImpl) dmrProcessor;
				if (m_isPausing) {
					localDMR.setHolder(null);
				} else {
					if (m_surface.getVisibility() == View.GONE) {
						localDMR.setHolder(null);
					} else {
						// if (m_surface.isShown()) {
						LocalMediaPlayer.surface_width = m_content.getWidth();
						LocalMediaPlayer.surface_height = m_content.getHeight();
						localDMR.setHolder(m_surface.getHolder());
					}
					// }
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private SurfaceHolder.Callback m_surfaceCallback = new Callback() {

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (dmrProcessor instanceof LocalDMRProcessorImpl) {
				LocalDMRProcessorImpl localDMR = (LocalDMRProcessorImpl) dmrProcessor;
				localDMR.setHolder(null);
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			updateSurfaceView();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			updateSurfaceView();
		}
	};

	public void switchToPortrait() {
		updateItemInfo();
	}

	public void switchToLandscape() {
		updateItemInfo();
	};

	public void toggleItemInfo() {
		if (m_viewFlipper != null && m_rendererControl != null)
			if (m_viewFlipper.isShown()) {
				m_viewFlipper.setVisibility(View.GONE);
				m_rendererControl.setVisibility(View.GONE);
				m_lastInfoState = false;
			} else {
				m_viewFlipper.setVisibility(View.VISIBLE);
				m_rendererControl.setVisibility(View.VISIBLE);
				m_lastInfoState = true;
			}
	}
}
