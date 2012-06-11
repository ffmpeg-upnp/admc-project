package com.app.dlna.dmc.processor.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.SurfaceHolder;

import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.gui.customview.nowplaying.LocalMediaPlayer;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.YoutubeProcessor.IYoutubeProcessorListener;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistItem.Type;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;
import com.app.dlna.dmc.utility.Utility;
import com.app.dlna.dmc.utility.Utility.CheckResult;

public class LocalDMRProcessorImpl implements DMRProcessor {
	private static final int SLEEP_INTERVAL = 1000;
	private List<DMRProcessorListner> m_listeners;
	private LocalMediaPlayer m_player;
	private PlaylistItem m_currentItem;
	private PlaylistProcessor m_playlistProcessor;
	private AudioManager m_audioManager;
	private int m_maxVolume;
	private boolean m_selfAutoNext;
	private static final int STATE_PLAYING = 0;
	private static final int STATE_STOPED = 1;
	private static final int STATE_PAUSED = 2;
	private int m_currentState;
	private UpdateThread m_updateThread;

	private class UpdateThread extends Thread {

		private boolean running = false;

		public UpdateThread() {
			running = true;
		}

		public void stopThread() {
			running = false;
			this.interrupt();
		}

		@Override
		public void run() {
			while (running && m_player != null) {
				Log.v("LocalDMRProcessorImpl", "Upate thread is running, [LOCAL] + " + getId());
				try {
					if (m_player != null && m_player.isPlaying()) {
						int currentPosition = (int) (m_player.getCurrentPosition() / 1000);
						fireUpdatePositionEvent(currentPosition, m_player.getDuration() / 1000);
						m_currentState = STATE_PLAYING;
					}
					switch (m_currentState) {
					case STATE_PLAYING:
						fireOnPlayingEvent();
						break;
					case STATE_PAUSED:
						fireOnPausedEvent();
						break;
					case STATE_STOPED:
						fireOnStopedEvent();
						break;
					}
					try {
						Thread.sleep(SLEEP_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!running)
						break;
				} catch (Exception ex) {
					ex.printStackTrace();
					running = false;
					if (m_player != null) {
						try {
							m_player.reset();
							m_player.release();
						} catch (Exception e) {

						} finally {
							m_player = null;
						}
					}
					fireOnStopedEvent();
					if (m_playlistProcessor != null)
						m_playlistProcessor.next();
					break;
				}
			}
		}
	}

	public LocalDMRProcessorImpl(Context context) {
		m_listeners = new ArrayList<DMRProcessor.DMRProcessorListner>();
		m_currentItem = new PlaylistItem();
		m_audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		m_maxVolume = m_audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		m_selfAutoNext = true;
		m_updateThread = new UpdateThread();
		m_updateThread.start();
	}

	@Override
	public void setURIandPlay(final PlaylistItem item) {
		if (item == null) {
			m_currentItem = null;
			stop();
			return;
		}
		if (m_currentItem != null && m_currentItem.equals(item))
			return;
		m_currentItem = item;
		setRunning(true);
		switch (m_currentItem.getType()) {
		case YOUTUBE:
			new YoutubeProcessorImpl().getDirectLinkAsync(new YoutubeItem(item.getUrl()),
					new IYoutubeProcessorListener() {

						@Override
						public void onStartPorcess() {
						}

						@Override
						public void onSearchComplete(List<YoutubeItem> result) {
						}

						@Override
						public void onGetLinkComplete(YoutubeItem result) {
							stop();
							m_player = new LocalMediaPlayer();
							m_player.setDisplay(m_holder);
							m_player.setOnPreparedListener(m_preparedListener);
							m_player.setOnCompletionListener(m_completeListener);
							m_player.setOnErrorListener(m_onErrorListener);
							m_player.setScreenOnWhilePlaying(true);
							if (result.getId().equals(m_currentItem.getUrl()))
								synchronized (m_currentItem) {
									try {
										m_player.setDataSource(result.getDirectLink());
										m_player.prepareAsync();
									} catch (IllegalArgumentException e) {
										e.printStackTrace();
									} catch (IllegalStateException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
						}

						@Override
						public void onFail(Exception ex) {
							ex.printStackTrace();
						}
					});
			break;
		case IMAGE_LOCAL:
		case IMAGE_REMOTE:
		case UNKNOW:
			stop();
			break;
		default:
			new Thread(new Runnable() {

				@Override
				public void run() {
					CheckResult result = Utility.checkItemURL(item);
					stop();
					m_player = new LocalMediaPlayer();
					m_player.setDisplay(m_holder);
					m_player.setOnPreparedListener(m_preparedListener);
					m_player.setOnCompletionListener(m_completeListener);
					m_player.setOnErrorListener(m_onErrorListener);
					m_player.setScreenOnWhilePlaying(true);
					if (result.getItem().equals(m_currentItem)) {
						if (result.isReachable())
							synchronized (m_currentItem) {
								Type itemType = m_currentItem.getType();
								if (itemType != Type.IMAGE_LOCAL && itemType != Type.IMAGE_REMOTE)
									try {
										if (m_player != null) {
											m_player.setDataSource(m_currentItem.getUrl());
											m_player.prepareAsync();
										}

									} catch (IllegalArgumentException e) {
										e.printStackTrace();
									} catch (IllegalStateException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
							}
						else {
							autoNext();
						}
					}
				}
			}).start();
			break;
		}
	}

	private OnPreparedListener m_preparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			mp.start();
			setRunning(true);
			m_currentState = STATE_PLAYING;
		}

	};

	private OnCompletionListener m_completeListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			mp.reset();
			fireOnStopedEvent();
			if (m_playlistProcessor != null && m_selfAutoNext)
				m_playlistProcessor.next();
		}
	};

	private OnErrorListener m_onErrorListener = new OnErrorListener() {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			if (m_player != null) {
				m_player.reset();
				m_player.release();
				m_player = null;
				setRunning(false);
			}

			if (m_playlistProcessor != null)
				m_playlistProcessor.next();

			return true;
		}
	};
	private SurfaceHolder m_holder = null;

	@Override
	public void play() {
		try {
			if (m_player != null)
				m_player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pause() {
		if (m_player != null && m_player.isPlaying())
			try {
				m_player.pause();
				m_currentState = STATE_PAUSED;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
	}

	@Override
	public void stop() {
		if (m_player != null)
			try {
				m_player.reset();
				m_player.release();
				m_player = null;
				m_currentState = STATE_STOPED;
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	@Override
	public void seek(String position) {
		try {
			String[] elements = position.split(":");
			long miliSec = new Integer(elements[0]) * 3600 + new Integer(elements[1]) * 60 + new Integer(elements[2]);
			miliSec *= 1000;
			m_player.seekTo((int) miliSec);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void setVolume(int newVolume) {
		try {
			m_audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_VIBRATE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public int getVolume() {
		return m_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	@Override
	public int getMaxVolume() {
		return m_maxVolume;
	}

	@Override
	public void addListener(DMRProcessorListner listener) {
		synchronized (m_listeners) {
			if (m_listeners.contains(listener)) {
				m_listeners.remove(listener);
			}
			m_listeners.add(listener);
			setRunning(true);
		}
	}

	@Override
	public void removeListener(DMRProcessorListner listener) {
		synchronized (m_listeners) {
			m_listeners.remove(listener);
		}

	}

	@Override
	public void dispose() {
		m_listeners.clear();
		if (AppPreference.stopDMR())
			stop();
		if (m_updateThread != null) {
			m_updateThread.stopThread();
		}
	}

	@Override
	public String getName() {
		return "Local Player";
	}

	@Override
	public void setPlaylistProcessor(PlaylistProcessor playlistProcessor) {
		m_playlistProcessor = playlistProcessor;
	}

	@Override
	public void setSeftAutoNext(boolean autoNext) {
		m_selfAutoNext = autoNext;
	}

	@Override
	public String getCurrentTrackURI() {
		return m_currentItem != null ? m_currentItem.getUrl() : "";
	}

	@Override
	public void setRunning(boolean running) {
		if (running) {
			if (m_updateThread != null) {
				m_updateThread.stopThread();
				m_updateThread = null;
			}
			m_updateThread = new UpdateThread();
			m_updateThread.start();
		} else {
			if (m_updateThread != null)
				m_updateThread.stopThread();
			m_updateThread = null;
		}
	}

	private void fireUpdatePositionEvent(long current, long max) {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onUpdatePosition(current, max);
			}
		}
	}

	private void fireOnStopedEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onStoped();
			}
		}
	}

	private void fireOnPausedEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onPaused();
			}
		}
	}

	private void fireOnPlayingEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onPlaying();
			}
		}
	}

	private void autoNext() {
		m_player.reset();
		fireOnStopedEvent();
		if (m_playlistProcessor != null && m_selfAutoNext)
			m_playlistProcessor.next();
	}

	public void setHolder(SurfaceHolder holder) {
		m_holder = holder;
		if (m_player != null) {
			m_player.setDisplay(holder);
			m_player.scaleContent();
			// m_player.setSufaceDimension(width, height);
		}
	}

	@Override
	public PlaylistItem getCurrentItem() {
		return m_currentItem;
	}
}
