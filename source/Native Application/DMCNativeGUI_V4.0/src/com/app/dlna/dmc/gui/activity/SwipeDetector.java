package com.app.dlna.dmc.gui.activity;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SwipeDetector implements View.OnTouchListener {
	private NowPlayingActivity m_activity;
	private static final int MIN_DISTANCE = 100;
	private static final int DOWN_TIME = 500;
	private static final String TAG = SwipeDetector.class.getName();
	private float m_downX, m_upX;
	private boolean m_enabled = true;

	public SwipeDetector(NowPlayingActivity activity) {
		this.m_activity = activity;
	}

	public void onRightToLeftSwipe() {
		if (m_enabled && m_activity != null)
			m_activity.doNext();
	}

	public void onLeftToRightSwipe() {
		if (m_enabled && m_activity != null)
			m_activity.doPrev();
	}

	public void onTap() {
		if (m_enabled && m_activity != null)
			m_activity.toggleItemInfo();
	}

	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			m_downX = event.getX();
			return true;
		}
		case MotionEvent.ACTION_UP: {
			m_upX = event.getX();

			float deltaX = m_downX - m_upX;
			if (event.getEventTime() - event.getDownTime() < DOWN_TIME)
				if (Math.abs(deltaX) > MIN_DISTANCE) {
					if (deltaX < 0) {
						this.onLeftToRightSwipe();
					} else if (deltaX > 0) {
						this.onRightToLeftSwipe();
					}
				} else {
					this.onTap();
				}
			return true;
		}
		}
		return false;
	}

	public void setEnable(boolean enabled) {
		m_enabled = enabled;
	}

	public boolean isEnabled() {
		return m_enabled;
	}
}