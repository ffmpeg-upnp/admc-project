package com.app.dlna.dmc.gui.customview;

import android.media.MediaPlayer;
import android.view.SurfaceHolder;

public class LocalMediaPlayer extends MediaPlayer {
	private SurfaceHolder m_surfaceHolder = null;
	public static int surface_width;
	public static int surface_height;

	@Override
	public void setDisplay(SurfaceHolder sh) {
		try {
			super.setDisplay(sh);
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				super.setDisplay(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		m_surfaceHolder = sh;
	}

	@Override
	public void start() throws IllegalStateException {
		super.start();
		scaleContent();
	}

	public void scaleContent() {
		try {
			int video_width = getVideoWidth();
			int video_height = getVideoHeight();
			if (surface_width != 0 && surface_height != 0 && video_width != 0 && video_height != 0 && m_surfaceHolder != null) {
				float scale_width = (float) video_width / surface_width;
				float scale_height = (float) video_height / surface_height;
				float max_scale = scale_width > scale_height ? scale_width : scale_height;
				int target_width = (int) (video_width / max_scale);
				int target_height = (int) (video_height / max_scale);
				m_surfaceHolder.setFixedSize(target_width, target_height);
			}
		} catch (Exception ex) {

		}
	}
}
