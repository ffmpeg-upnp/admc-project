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

import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;

public class DMRProcessorImpl implements DMRProcessor {
	private static final int UPDATE_INTERVAL = 1000;
	private static final String TAG = DMRProcessorImpl.class.getName();
	private static final int MAX_VOLUME = 100;
	protected static final long SEEK_DELAY_INTERVAL = 200;
	@SuppressWarnings("rawtypes")
	private Device m_device;
	private ControlPoint m_controlPoint;
	@SuppressWarnings("rawtypes")
	private Service m_avtransportService = null;
	@SuppressWarnings("rawtypes")
	private Service m_renderingControl = null;
	private List<DMRProcessorListner> m_listeners;
	private PlaylistProcessor m_playlistProcessor;
	private boolean m_isRunning = true;
	private int m_currentVolume;
	private boolean m_isBusy = false;
	private int m_state = -1;
	private static final int PLAYING = 0;
	private static final int PAUSE = 1;
	private static final int STOP = 2;
	private boolean m_checkGetPositionInfo = false;
	private boolean m_checkGetTransportInfo = false;
	private boolean m_checkGetVolumeInfo = false;
	private boolean m_user_stop = false;
	private boolean m_seftAutoNext = true;
	private int m_autoNextPending = 0;
	private String m_currentTrackURI;

	private class UpdateThread extends Thread {
		@Override
		public void run() {
			while (m_isRunning) {
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
							// Log.v(TAG, positionInfo.toString());
							// Log.v(TAG, "Track uri = " +
							// positionInfo.getTrackURI());
							m_currentTrackURI = positionInfo.getTrackURI();
							fireUpdatePositionEvent(positionInfo.getTrackElapsedSeconds(),
									positionInfo.getTrackDurationSeconds());

							if (positionInfo.getTrackDurationSeconds() == 0) {
								// Log.v(TAG, "auto next");
								m_state = STOP;
								new Thread(new Runnable() {

									@Override
									public void run() {
										try {
											Thread.sleep(2000);
											if (m_state == STOP && m_user_stop == false) {
												fireOnEndTrackEvent();
											}
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
								}).start();
							}
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
								m_state = PLAYING;
								break;
							case PAUSED_PLAYBACK:
								fireOnPausedEvent();
								m_state = PAUSE;
								break;
							case STOPPED:
								fireOnStopedEvent();
								m_state = STOP;
								break;
							default:
								break;
							}
							m_checkGetTransportInfo = false;
						}

					});
				}

				if (!m_checkGetVolumeInfo) {
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
					e.printStackTrace();
				}
			}
			super.run();
		}
	}

	@SuppressWarnings("rawtypes")
	public DMRProcessorImpl(Device dmr, ControlPoint controlPoint) {
		m_device = dmr;
		m_controlPoint = controlPoint;
		m_avtransportService = m_device.findService(new ServiceType("schemas-upnp-org", "AVTransport"));
		m_renderingControl = m_device.findService(new ServiceType("schemas-upnp-org", "RenderingControl"));
		m_listeners = new ArrayList<DMRProcessor.DMRProcessorListner>();
		new UpdateThread().start();
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
				m_state = PLAYING;
				m_user_stop = false;
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
				m_state = PAUSE;
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
				m_state = STOP;
				m_user_stop = true;
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
				fireOnFailEvent(invocation.getAction(), response, defaultMsg);
				m_isBusy = false;
				m_user_stop = false;
			}
		};

		m_controlPoint.execute(stop);
	}

	@Override
	public void addListener(DMRProcessorListner listener) {
		synchronized (m_listeners) {
			if (!m_listeners.contains(listener))
				m_listeners.add(listener);
			if (m_avtransportService == null || m_renderingControl == null)
				fireOnErrorEvent("Cannot get service on this device");
		}
	}

	@Override
	public void removeListener(DMRProcessorListner listener) {
		synchronized (m_listeners) {
			m_listeners.remove(listener);
		}
	}

	@SuppressWarnings("rawtypes")
	private void fireOnFailEvent(Action action, UpnpResponse response, String message) {
		if (!m_isRunning)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onActionFail(action, response, message);
			}
			m_isRunning = false;
		}
	}

	private void fireUpdatePositionEvent(long current, long max) {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onUpdatePosition(current, max);
			}
		}
	}

	private void fireOnStopedEvent() {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onStoped();
			}
		}
	}

	private void fireOnPausedEvent() {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onPaused();
			}
		}
	}

	private void fireOnPlayingEvent() {
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onPlaying();
			}
		}
	}

	private void fireOnEndTrackEvent() {
		Log.i(TAG, "fireOnEndTrackEvent");
		if (m_isBusy)
			return;
		synchronized (m_listeners) {
			if (m_listeners.size() > 0) {
				// Log.i(TAG, "fire auto next with ");
				for (DMRProcessorListner listener : m_listeners) {
					listener.onEndTrack();
				}
			}
			if (m_seftAutoNext) {
				// Log.i(TAG, "seft next");
				if (m_autoNextPending == 0) {
					if (m_playlistProcessor != null) {
						m_playlistProcessor.next();
						final PlaylistItem item = m_playlistProcessor.getCurrentItem();
						if (item != null) {
							setURIandPlay(item);
						}
					}
					m_autoNextPending = 7;
				} else {
					--m_autoNextPending;
				}
			}
		}
	}

	private void fireOnErrorEvent(String error) {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onErrorEvent(error);
			}
		}
	}

	@Override
	public void dispose() {
		m_isRunning = false;
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
	public void setSeftAutoNext(boolean autoNext) {
		m_seftAutoNext = autoNext;
	}

	@Override
	public String getCurrentTrackURI() {
		return null == m_currentTrackURI ? "" : m_currentTrackURI;
	}

	@Override
	public void setRunning(boolean running) {
		m_isRunning = running;
		if (m_isRunning)
			new UpdateThread().start();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setURIandPlay(PlaylistItem item) {
		final String url = item.getUrl();
		m_autoNextPending = 5;
		if (m_controlPoint == null || m_avtransportService == null)
			return;
		m_isBusy = true;
		m_controlPoint.execute(new GetMediaInfo(m_avtransportService) {

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
				fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
				m_isBusy = false;
			}

			@Override
			public void received(ActionInvocation invocation, MediaInfo mediaInfo) {
				// if (mediaInfo != null && mediaInfo.getCurrentURIMetaData() !=
				// null)
				// Log.e(TAG, mediaInfo.getCurrentURIMetaData());
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
					// Log.e(TAG, "set AV uri = " + uri);
					Stop stop = new Stop(m_avtransportService) {
						@Override
						public void success(ActionInvocation invocation) {
							super.success(invocation);
							fireUpdatePositionEvent(0, 0);
							m_isBusy = false;
							m_state = STOP;
							m_controlPoint.execute(new SetAVTransportURI(m_avtransportService, url, null) {
								@Override
								public void success(ActionInvocation invocation) {
									super.success(invocation);
									m_controlPoint.execute(new Play(m_avtransportService) {

										@Override
										public void failure(ActionInvocation invocation, UpnpResponse operation,
												String defaultMsg) {
											// Log.e(TAG, "Call fail");
											fireOnFailEvent(invocation.getAction(), operation, defaultMsg);
											m_isBusy = false;
										}

										public void success(ActionInvocation invocation) {
											m_isBusy = false;
											m_state = PLAYING;
										};
									});
								}

								@Override
								public void failure(ActionInvocation invocation, UpnpResponse response,
										String defaultMsg) {
									fireOnFailEvent(invocation.getAction(), response, defaultMsg);
									m_isBusy = false;
								}
							});
						}

						@Override
						public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
							fireOnFailEvent(invocation.getAction(), response, defaultMsg);
							m_isBusy = false;
							m_user_stop = false;
						}
					};
					m_controlPoint.execute(stop);
				}
			}
		});
	}
}
