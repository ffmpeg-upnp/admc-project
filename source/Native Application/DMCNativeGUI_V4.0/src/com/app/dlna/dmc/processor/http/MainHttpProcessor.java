package com.app.dlna.dmc.processor.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;

public class MainHttpProcessor extends Thread {
	private static final String TAG = "HTTPServerLOG";
	private ServerSocket m_server;
	private static final int SIZE = 12;
	private ThreadPoolExecutor m_executor = new ThreadPoolExecutor(SIZE, SIZE, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler() {

				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

				}
			});

	public MainHttpProcessor() {
		HTTPServerData.RUNNING = true;
	}

	public void stopHttpThread() {
		Log.d(TAG, "Stop HTTP Thread");
		HTTPServerData.RUNNING = false;
		try {
			m_server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			m_executor.shutdownNow();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void run() {
		Log.e(TAG, "Start HTTP Thread");
		try {
			m_server = new ServerSocket(HTTPServerData.PORT);
			while (HTTPServerData.RUNNING) {
				final Socket client = m_server.accept();
				m_executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							final BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
							List<String> rawrequest = new ArrayList<String>();
							String line = null;
							String request = null;
							String requesttype = null;
							long range = 0;
							Log.e(TAG, "<--START HEADER-->");
							while ((line = br.readLine()) != null && (line.length() != 0)) {
								Log.e(TAG, line);
								rawrequest.add(line);
								if (line.contains("GET")) {
									requesttype = "GET";
									request = getRequestFilePath(line);
								} else if (line.contains("HEAD")) {
									requesttype = "HEAD";
									request = getRequestFilePath(line);
								} else if (line.contains("Range")) {
									Log.e(TAG, "Have range");
									String strrange = line.substring(13, line.lastIndexOf('-'));
									range = Long.valueOf(strrange);
								}
							}
							Log.e(TAG, "<--END HEADER-->");
							String filename = null;
							if (request != null) {
								Log.e(TAG, "Request = " + request);
								if (HTTPServerData.LINK_MAP.containsKey(request)) {
									Log.e(TAG, "Youtube Proxy mode");
									HTTPRequestHandler.handleProxyDataRequest(client, rawrequest, HTTPServerData.LINK_MAP.get(request));
								} else {
									filename = URLDecoder.decode(request, "ASCII");
									Log.e(TAG, "Play-to mode, file name = " + filename);
									if (filename != null && filename.length() != 0) {
										HTTPRequestHandler.handleClientRequest(client, requesttype, range, filename);
									}
								}

							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

					private String getRequestFilePath(String line) {
						String request;
						String[] linepatthern = line.split(" ");
						request = linepatthern[1];
						return request;
					}

				});
			}
			m_server.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
