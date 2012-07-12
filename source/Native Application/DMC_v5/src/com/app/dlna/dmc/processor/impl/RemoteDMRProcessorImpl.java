package com.app.dlna.dmc.processor.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.support.avtransport.callback.GetMediaInfo;
import org.teleal.cling.support.avtransport.callback.GetPositionInfo;
import org.teleal.cling.support.avtransport.callback.GetTransportInfo;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.model.MediaInfo;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.SeekMode;
import org.teleal.cling.support.model.TransportInfo;
import org.teleal.cling.support.renderingcontrol.callback.GetVolume;
import org.teleal.cling.support.renderingcontrol.callback.SetVolume;

import android.util.Log;
import android.widget.Toast;

import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.interfaces.YoutubeProcessor.IYoutubeProcessorListener;
import com.app.dlna.dmc.processor.model.PlaylistItem;
import com.app.dlna.dmc.processor.model.YoutubeItem;
import com.app.dlna.dmc.processor.model.PlaylistItem.Type;
import com.app.dlna.dmc.utility.Utility;
import com.app.dlna.dmc.utility.Utility.CheckResult;

public class RemoteDMRProcessorImpl implements DMRProcessor {
	private static final int UPDATE_INTERVAL = 1000;
	private static final int MAX_VOLUME = 100;
	private static final long SEEK_DELAY_INTERVAL = 200;
	private static final int AUTO_NEXT_DELAY = 4; // second
	private static final String TAG = RemoteDMRProcessorImpl.class.getName();
	@SuppressWarnings("rawtypes")
	private Device m_device;
	private ControlPoint m_controlPoint;
	@SuppressWarnings("rawtypes")
	private Service m_avtransportService = null;
	@SuppressWarnings("rawtypes")
	private Service m_renderingControl = null;
	private List<DMRProcessorListener> m_listeners;
	private PlaylistProcessor m_playlistProcessor;
	private int m_currentVolume;
	private boolean m_isBusy = false;
	private boolean m_checkGetPositionInfo = false;
	private boolean m_checkGetTransportInfo = false;
	private boolean m_checkGetVolumeInfo = false;
	private int m_autoNextPending = AUTO_NEXT_DELAY;
	private PlaylistItem m_currentItem;
	private UpdateThread m_updateThread = null;

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
			while (running) {
				Log.d("RemoteDMRProcessorImpl", "Upate thread is running, [REMOTE] + " + getId());
				if (m_avtransportService == null)
					return;
				if (!m_checkGetPositionInfo) {
					m_checkGetPositionInfo = true;
					m_controlPoint.execute(new GetPositionInfo(m_avtransportService) {

						@SuppressWarnings("rawtypes")
						@Override
						public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
							fireOnFailEvent(invocation.getAction(), response, defaultMsg);
							m_checkGetPositionInfo = false;
						}

						@SuppressWarnings("rawtypes")
						@Override
						public void received(ActionInvocation invocation, PositionInfo positionInfo) {
							fireUpdatePositionEvent(positionInfo.getTrackElapsedSeconds(), positionInfo.getTrackDurationSeconds());
							m_checkGetPositionInfo = false;
						}
					});

				}

				if (!m_checkGetTransportInfo) {
					m_checkGetTransportInfo = true;
					m_controlPoint.execute(new GetTransportInfo(m_avtransportService) {
						@SuppressWarnings("rawtypes")
						@Override
						public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
							fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
							m_checkGetTransportInfo = false;
						}

						@SuppressWarnings("rawtypes")
						@Override
						public void received(ActionInvocation invocation, TransportInfo transportInfo) {
							switch (transportInfo.getCurrentTransportState()) {
							case PLAYING:
								fireOnPlayingEvent();
								break;
							case PAUSED_PLAYBACK:
								fireOnPausedEvent();
								break;
							case STOPPED:
								fireOnStopedEvent();
								fireOnEndTrackEvent();
								break;
							default:
								break;
							}
							m_checkGetTransportInfo = false;
						}

					});
				}
				if (m_renderingControl != null && !m_checkGetVolumeInfo) {
					m_checkGetVolumeInfo = true;
					m_controlPoint.execute(new GetVolume(m_renderingControl) {
						@SuppressWarnings("rawtypes")
						@Override
						public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
							fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
							m_checkGetVolumeInfo = false;
						}

						@SuppressWarnings("rawtypes")
						@Override
						public void received(ActionInvocation actionInvocation, int currentVolume) {
							m_currentVolume = currentVolume;
							m_checkGetVolumeInfo = false;
						}
					});
				}

				try {
					Thread.sleep(UPDATE_INTERVAL);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public RemoteDMRProcessorImpl(Device dmr, ControlPoint controlPoint) {
		m_device = dmr;
		m_controlPoint = controlPoint;
		m_avtransportService = m_device.findService(new ServiceType("schemas-upnp-org", "AVTransport"));
		m_renderingControl = m_device.findService(new ServiceType("schemas-upnp-org", "RenderingControl"));
		if (m_renderingControl.getAction("SetVolume") == null || m_renderingControl.getAction("GetVolume") == null)
			m_renderingControl = null;
		m_listeners = new ArrayList<DMRProcessor.DMRProcessorListener>();
		m_currentItem = new PlaylistItem();
		m_updateThread = new UpdateThread();
		m_updateThread.start();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void play() {
		if (m_controlPoint == null || m_avtransportService == null)
			return;
		m_isBusy = true;
		Play play = new Play(m_avtransportService) {

			@Override
			public void success(ActionInvocation invocation) {
				super.success(invocation);
				m_isBusy = false;
				// m_state = PLAYING;
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
				fireOnFailEvent(invocation.getAction(), response, defaultMsg);
				m_isBusy = false;
			}

		};

		m_controlPoint.execute(play);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void pause() {
		if (m_controlPoint == null || m_avtransportService == null)
			return;
		m_isBusy = true;
		Pause pause = new Pause(m_avtransportService) {

			@Override
			public void success(ActionInvocation invocation) {
				super.success(invocation);
				m_isBusy = false;
				// m_state = PAUSE;
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
				fireOnFailEvent(invocation.getAction(), response, defaultMsg);
				m_isBusy = false;
			}

		};
		m_controlPoint.execute(pause);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void stop() {
		if (m_controlPoint == null || m_avtransportService == null)
			return;
		m_isBusy = true;

		Stop stop = new Stop(m_avtransportService) {
			@Override
			public void success(ActionInvocation invocation) {
				super.success(invocation);
				fireUpdatePositionEvent(0, 0);
				m_isBusy = false;
				// m_state = STOP;
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
				fireOnFailEvent(invocation.getAction(), response, defaultMsg);
				m_isBusy = false;
			}
		};

		m_controlPoint.execute(stop);
	}

	@Override
	public void addListener(DMRProcessorListener listener) {
		synchronized (m_listeners) {
			if (!m_listeners.contains(listener))
				m_listeners.add(listener);
			if (m_avtransportService == null)
				fireOnErrorEvent("Cannot get service on this device");
		}
	}

	@Override
	public void removeListener(DMRProcessorListener listener) {
		synchronized (m_listeners) {
			m_listeners.remove(listener);
		}
	}

	@SuppressWarnings("rawtypes")
	private void fireOnFailEvent(Action action, UpnpResponse response, String message) {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onActionFail(action, response, message);
			}
		}
	}

	private void fireUpdatePositionEvent(long current, long max) {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onUpdatePosition(current, max);
			}
		}
	}

	private void fireOnStopedEvent() {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onStoped();
			}
		}
	}

	private void fireOnPausedEvent() {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onPaused();
			}
		}
	}

	private void fireOnPlayingEvent() {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onPlaying();
			}
		}
	}

	private void fireOnEndTrackEvent() {
		if (m_isBusy || m_playlistProcessor == null)
			return;
		PlaylistItem currentItem = m_playlistProcessor.getCurrentItem();
		if (AppPreference.getAutoNext()) {
			if (currentItem != null && (currentItem.getType() == Type.IMAGE_LOCAL || currentItem.getType() == Type.IMAGE_REMOTE)
					&& !AppPreference.getAutoNextImage())
				return;
			Log.e(TAG, "onEndTrack, m_autoNextPending = " + m_autoNextPending);
			if (m_autoNextPending <= 0) {
				Log.e(TAG, "Call playlist next");
				m_playlistProcessor.next();
				m_autoNextPending = AUTO_NEXT_DELAY;
			} else {
				--m_autoNextPending;
			}
		}
	}

	private void fireOnErrorEvent(String error) {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onErrorEvent(error);
			}
		}
	}

	@Override
	public void dispose() {
		if (AppPreference.stopDMR())
			stop();
		if (m_updateThread != null)
			m_updateThread.stopThread();
		synchronized (m_listeners) {
			m_listeners.clear();
		}
	}

	public void seek(String position) {
		m_isBusy = true;
		// Log.e(TAG, "Call seek");
		@SuppressWarnings("rawtypes")
		Seek seek = new Seek(m_avtransportService, SeekMode.REL_TIME, position) {
			@Override
			public void success(ActionInvocation invocation) {
				super.success(invocation);
				m_isBusy = false;
				// Log.e(TAG, "Call seek complete");
				try {
					Thread.sleep(SEEK_DELAY_INTERVAL);
					m_isBusy = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse reponse, String defaultMsg) {
				// Log.e(TAG, "Seek fail: " + defaultMsg);
				m_isBusy = false;
			}
		};
		m_controlPoint.execute(seek);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setVolume(int newVolume) {
		if (m_renderingControl != null) {
			m_isBusy = true;
			m_controlPoint.execute(new SetVolume(m_renderingControl, newVolume) {
				@Override
				public void success(ActionInvocation invocation) {
					super.success(invocation);
					m_isBusy = false;
				}

				@Override
				public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
					fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
					m_isBusy = false;
				}
			});
		}
	}

	@Override
	public int getVolume() {
		return m_currentVolume;
	}

	@Override
	public int getMaxVolume() {
		return MAX_VOLUME;
	}

	@Override
	public String getName() {
		return m_device != null ? m_device.getDetails().getFriendlyName() : "NULL";
	}

	@Override
	public void setPlaylistProcessor(PlaylistProcessor playlistProcessor) {
		m_playlistProcessor = playlistProcessor;

	}

	@Override
	public String getCurrentTrackURI() {
		return null == m_currentItem ? "" : m_currentItem.getUrl();
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

	@Override
	public void setURIandPlay(final PlaylistItem item) {
		m_autoNextPending = AUTO_NEXT_DELAY;
		if (item == null) {
			m_currentItem = null;
			stop();
			return;
		}
		final String url = item.getUrl();
		m_autoNextPending = AUTO_NEXT_DELAY;
		if (m_controlPoint == null || m_avtransportService == null)
			return;
		if (m_currentItem != null && m_currentItem.equals(item))
			return;
		m_isBusy = true;
		m_currentItem = item;
		stop();
		switch (item.getType()) {
		case YOUTUBE:
			IYoutubeProcessorListener youtubeCallback = new IYoutubeProcessorListener() {

				@Override
				public void onStartPorcess() {
				}

				@Override
				public void onSearchComplete(List<YoutubeItem> result) {
				}

				@Override
				public void onGetLinkComplete(YoutubeItem result) {
					if (result.getId().equals(m_currentItem.getUrl()))
						synchronized (m_currentItem) {
							setUriAndPlay(result.getDirectLink(),
									Utility.createMetaData(result.getTitle(), Type.YOUTUBE, result.getDirectLink()));
						}
				}

				@Override
				public void onFail(final Exception ex) {
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(MainActivity.INSTANCE, ex.getMessage(), Toast.LENGTH_SHORT).show();
						}
					});

				}
			};
			if (AppPreference.getProxyMode()) {
				new YoutubeProcessorImpl().registURLAsync(new YoutubeItem(item.getUrl()), youtubeCallback);
			} else {
				new YoutubeProcessorImpl().getDirectLinkAsync(new YoutubeItem(item.getUrl()), youtubeCallback);
			}
			break;
		default:
			new Thread(new Runnable() {

				@Override
				public void run() {
					CheckResult result = Utility.checkItemURL(item);
					if (result.getItem().equals(m_currentItem)) {
						if (result.isReachable())
							setUriAndPlay(url, item.getMetaData());
						else {
						}
					}
				}

			}).start();
			break;
		}
	}

	@SuppressWarnings("rawtypes")
	private void setUriAndPlay(final String url, final String metaData) {
		synchronized (m_currentItem) {
			m_controlPoint.execute(new GetMediaInfo(m_avtransportService) {

				@Override
				public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
					fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
					m_isBusy = false;
				}

				@Override
				public void received(ActionInvocation invocation, MediaInfo mediaInfo) {
					String current_uri = null;
					String currentPath = null;
					String newPath = null;
					String currentQuery = null;
					String newQuery = null;

					try {
						current_uri = mediaInfo.getCurrentURI();
						if (current_uri != null) {
							URI _uri = new URI(current_uri);
							currentPath = _uri.getPath();
							currentQuery = _uri.getQuery();
						}
						URI _uri = new URI(url);
						newPath = _uri.getPath();
						newQuery = _uri.getQuery();
					} catch (URISyntaxException e) {
						current_uri = null;
					}
					if (currentPath != null
							&& newPath != null
							&& currentPath.equals(newPath)
							&& (currentQuery == newQuery || (currentQuery != null && newQuery != null && currentQuery
									.equals(newQuery)))) {
						play();
					} else {
						Stop stop = new Stop(m_avtransportService) {
							@Override
							public void success(ActionInvocation invocation) {
								super.success(invocation);
								fireUpdatePositionEvent(0, 0);
								m_isBusy = false;
								// m_state = STOP;
								m_controlPoint.execute(new SetAVTransportURI(m_avtransportService, url, metaData) {
									@Override
									public void success(ActionInvocation invocation) {
										super.success(invocation);
										m_controlPoint.execute(new Play(m_avtransportService) {

											@Override
											public void failure(ActionInvocation invocation, UpnpResponse operation,
													String defaultMsg) {
												fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
												m_isBusy = false;
											}

											public void success(ActionInvocation invocation) {
												m_isBusy = false;
											};
										});
									}

									@Override
									public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {

										fireUpdatePositionEvent(0, 0);
										m_isBusy = false;
										// m_state = STOP;
										m_controlPoint.execute(new SetAVTransportURI(m_avtransportService, url, metaData) {
											@Override
											public void success(ActionInvocation invocation) {
												super.success(invocation);
												m_controlPoint.execute(new Play(m_avtransportService) {

													@Override
													public void failure(ActionInvocation invocation, UpnpResponse operation,
															String defaultMsg) {
														fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
														m_isBusy = false;
													}

													public void success(ActionInvocation invocation) {
														m_isBusy = false;
													};
												});
											}

											@Override
											public void failure(ActionInvocation invocation, UpnpResponse response,
													String defaultMsg) {
												m_controlPoint.execute(new Play(m_avtransportService) {

													@Override
													public void failure(ActionInvocation invocation, UpnpResponse operation,
															String defaultMsg) {
														fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
														m_isBusy = false;
													}

													public void success(ActionInvocation invocation) {
														m_isBusy = false;
													};
												});
												fireOnFailEvent(invocation.getAction(), response, defaultMsg);
												m_isBusy = false;
											}
										});
									}
								});
							}

							@Override
							public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
								fireOnFailEvent(invocation.getAction(), response, defaultMsg);
								// m_isBusy = false;
								// m_user_stop = false;
							}
						};
						m_controlPoint.execute(stop);
					}
				}
			});
		}
	}

	@Override
	public PlaylistItem getCurrentItem() {
		return m_currentItem;
	}
}
