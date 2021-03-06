package com.app.dlna.dmc.gui.subactivity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.gui.customview.localnetwork.HomeNetworkView;
import com.app.dlna.dmc.gui.customview.playlist.PlaylistView;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;

public class LibraryActivity extends Activity {
	protected static final String TAG = LibraryActivity.class.getName();
	private ViewPager m_pager;
	private HomeNetworkView m_homeNetworkView;
	private View m_internet;
	private PlaylistView m_playlistView;
	private String[] m_pagerTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m_homeNetworkView = new HomeNetworkView(this);
		m_internet = new LinearLayout(this);
		m_playlistView = new PlaylistView(this);

		setContentView(R.layout.activity_library);
		m_pagerTitle = getResources().getStringArray(R.array.libray_pager_list);

		m_pager = (ViewPager) findViewById(R.id.viewPager);
		m_pager.setOnPageChangeListener(m_onPageChangeListener);
		m_pager.setAdapter(m_pagerAdapter);

	}

	@Override
	protected void onResume() {
		super.onResume();
		m_playlistView.preparePlaylist();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (MainActivity.UPNP_PROCESSOR != null && MainActivity.UPNP_PROCESSOR.getPlaylistProcessor() != null) {
			PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
			playlistProcessor.saveState();
		}
	}

	private OnPageChangeListener m_onPageChangeListener = new OnPageChangeListener() {
		@Override
		public void onPageSelected(int position) {

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageScrollStateChanged(int state) {
			if (state == 0)
				switch (m_pager.getCurrentItem()) {
				case 0:
					m_homeNetworkView.updateListView();
					break;
				case 1:
					m_playlistView.preparePlaylist();
					break;
				case 2:
					break;

				default:
					break;
				}
		}
	};

	PagerAdapter m_pagerAdapter = new PagerAdapter() {
		@Override
		public void destroyItem(ViewGroup container, int position, Object view) {
			((ViewPager) container).removeView((View) view);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (position == 0) {
				((ViewPager) container).addView(m_homeNetworkView);
				m_homeNetworkView.updateListView();
				return m_homeNetworkView;
			} else if (position == 1) {
				((ViewPager) container).addView(m_playlistView);
				m_playlistView.backToListPlaylist();
				return m_playlistView;
			} else if (position == 2) {
				((ViewPager) container).addView(m_internet);
				return m_internet;
			}
			return null;
		}

		@Override
		public boolean isViewFromObject(View view, Object key) {
			return view == key;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return m_pagerTitle[position];
		}

		@Override
		public int getCount() {
			return m_pagerTitle.length;
		}
	};

}
