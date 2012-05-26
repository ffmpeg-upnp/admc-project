package com.app.dlna.dmc.gui.activity;

import java.io.IOException;
import java.net.MalformedURLException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.customview.nowplaying.LocalMediaPlayer;
import com.app.dlna.dmc.gui.customview.nowplaying.RendererControlView;
import com.app.dlna.dmc.processor.async.AsyncTaskWithProgressDialog;
import com.app.dlna.dmc.processor.impl.LocalDMRProcessorImpl;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor.PlaylistListener;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.utility.Utility;

public class NowPlayingActivity extends Activity {
	protected String TAG = NowPlayingActivity.class.getName();
	private RendererControlView m_rendererControl;
	private ViewFlipper m_viewFlipper;
	private ProgressDialog m_progressDialog;
	private Animation m_animFlipInNext;
	private Animation m_animFlipOutNext;
	private Animation m_animFlipInPrevious;
	private Animation m_animFlipOutPrevious;
	private SurfaceView m_surface;
	private ImageView m_image;
	private boolean m_isPausing;
	private LinearLayout m_content;

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

		m_content = (LinearLayout) findViewById(R.id.content);
		m_content.setOnClickListener(m_contentClickListener);
		m_content.setOnTouchListener(m_swipeDetector);
		m_image = (ImageView) findViewById(R.id.image);
		m_image.setOnClickListener(m_contentClickListener);
		m_image.setOnTouchListener(m_swipeDetector);

		m_surface = (SurfaceView) findViewById(R.id.surface);
		m_surface.setOnTouchListener(m_swipeDetector);
		m_surface.setOnClickListener(m_contentClickListener);
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
		m_viewFlipper.setOnClickListener(m_contentClickListener);
		
	}

	private void initPortrait() {
		initializeComponents();
		m_viewFlipper.setVisibility(View.VISIBLE);
		m_rendererControl.setVisibility(View.VISIBLE);
		ViewGroup vg = (ViewGroup) findViewById(R.id.rl_nowplaying_root);
		vg.invalidate();
	}

	private void initLandscape() {
		initializeComponents();
		m_viewFlipper.setVisibility(View.GONE);
		m_rendererControl.setVisibility(View.GONE);

	}

	private PlaylistListener m_playlistListener = new PlaylistListener() {

		@Override
		public void onPrev() {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					int displayMode = getWindowManager().getDefaultDisplay().getRotation();
					if (displayMode == Surface.ROTATION_0 || displayMode == Surface.ROTATION_180) {
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
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					int displayMode = getWindowManager().getDefaultDisplay().getRotation();
					if (displayMode == Surface.ROTATION_0 || displayMode == Surface.ROTATION_180) {
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
	private SwipeDetector m_swipeDetector;

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
		PlaylistItem item = playlistProcessor.getCurrentItem();
		if (item == null)
			return;
		((TextView) m_viewFlipper.getCurrentView()).setText(item.getTitle());
		switch (item.getType()) {
		case AUDIO_LOCAL:
		case AUDIO_REMOTE: {
			m_image.setVisibility(View.VISIBLE);
			m_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_didlobject_audio_large));
			m_surface.setVisibility(View.GONE);
			break;
		}
		case YOUTUBE:
		case VIDEO_LOCAL:
		case VIDEO_REMOTE: {
			if (dmrProcessor instanceof LocalDMRProcessorImpl) {
				m_surface.setVisibility(View.VISIBLE);
				m_image.setVisibility(View.GONE);
			} else {
				m_image.setVisibility(View.VISIBLE);
				m_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_didlobject_video_large));
				m_surface.setVisibility(View.GONE);
			}
			break;
		}
		case IMAGE_LOCAL:
		case IMAGE_REMOTE: {
			new AsyncTaskWithProgressDialog<String, Void, Bitmap>("Loading image") {

				protected void onPreExecute() {
					m_image.setVisibility(View.VISIBLE);
					m_surface.setVisibility(View.GONE);
				};

				@Override
				protected Bitmap doInBackground(String... params) {
					Log.i(TAG, "Load in background");
					String url = params[0];
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
					}
				}

				@Override
				protected void onPostExecute(Bitmap result) {
					if (result == null) {
						m_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_didlobject_image_large));
					} else {
						m_image.setImageBitmap(result);
					}
				}
			}.execute(new String[] { item.getUrl(), "512", "512" });
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
		Log.e(TAG, "nowplaying ressume");
		super.onResume();
		m_isPausing = false;
		updatePlaylist();
		updateItemInfo();
	}

	@Override
	protected void onPause() {
		Log.e(TAG, "nowplaying pause");
		super.onPause();
		m_isPausing = true;
		m_surface.setVisibility(View.GONE);
		updateSurfaceView();
		m_rendererControl.disconnectToDMR();
		PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
		if (playlistProcessor != null)
			playlistProcessor.removeListener(m_playlistListener);
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
					Log.i(TAG, "suface state = " + m_surface.isShown());
					// if (m_surface.isShown()) {
					LocalMediaPlayer.surface_width = m_content.getWidth();
					LocalMediaPlayer.surface_height = m_content.getHeight();
					localDMR.setHolder(m_surface.getHolder());
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
			Log.e(TAG, "surface destroyed");
			DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
			if (dmrProcessor instanceof LocalDMRProcessorImpl) {
				LocalDMRProcessorImpl localDMR = (LocalDMRProcessorImpl) dmrProcessor;
				localDMR.setHolder(null);
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.e(TAG, "surface created");
			updateSurfaceView();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.e(TAG, "surface changed");
			updateSurfaceView();
		}
	};
	private View.OnClickListener m_contentClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			if (m_viewFlipper != null) {
				if (m_viewFlipper.isShown())
					m_viewFlipper.setVisibility(View.GONE);
				else
					m_viewFlipper.setVisibility(View.VISIBLE);
			}
			if (m_rendererControl != null)
				if (m_rendererControl.isShown())
					m_rendererControl.setVisibility(View.GONE);
				else
					m_rendererControl.setVisibility(View.VISIBLE);
		}
	};;

	public void switchToPortrait() {
		Log.e(TAG, "switch to portrait");
		updateItemInfo();
	}

	public void switchToLandscape() {
		Log.e(TAG, "switch to landscape");
		updateItemInfo();
	};

}
