package com.app.dlna.dmc.processor.http;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teleal.cling.support.messagebox.model.DateTime;

public class HTTPHelper {

	private static final int BUFFERSIZE = 131702;
	public static String NEWLINE = "\r\n";

	public static String makeGETrequest(String url) throws MalformedURLException {
		String result = "";
		URL requestURL = new URL(url);
		result += "GET " + requestURL.getFile() + " HTTP/1.1";
		result += NEWLINE;
		result += "Host: " + requestURL.getHost() + ":" + (requestURL.getPort() > 0 ? requestURL.getPort() : 80);
		result += NEWLINE;
		result += "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";
		result += NEWLINE;
		result += "Connection: close";
		result += NEWLINE;
		result += NEWLINE;

		return result;
	}

	public static String makeHttp200Reponse(String filename) {
		String result = "";

		File f = new File(filename);
		String mimeType = URLConnection.getFileNameMap().getContentTypeFor(filename);
		result += "HTTP/1.1 200 OK";
		result += NEWLINE;
		result += "Content-Type: " + mimeType;
		result += NEWLINE;
		result += "Content-Length: " + f.length();
		result += NEWLINE;
		result += "Accept-Ranges: bytes";
		result += NEWLINE;
		result += createDLNAHeaderField();
		result += NEWLINE;
		result += "TransferMode.DLNA.ORG: Streaming";
		result += NEWLINE;
		result += "Date: " + DateTime.getCurrentDate().toString();
		result += NEWLINE;
		result += "Last-Modified: " + new Date(f.lastModified()).toString();
		result += NEWLINE;
		result += "Connection: close";
		result += NEWLINE;
		result += "Server: SimpleDMS";
		result += NEWLINE;
		result += NEWLINE;

		return result;
	}

	// private static String getDLNAType(String mimeType) {
	// String DLNAMimeType = "*";
	// // String mimeTypePart[] = mimeType.split("/");
	// // if (mimeTypePart[0].equals("video")) {
	// // if (mimeTypePart[1].equals("mp4")){
	// // }
	// // } else if (mimeTypePart[0].equals("image")) {
	// // DLNAMimeType = IMAGE;
	// // } else if (mimeTypePart[0].equals("audio")) {
	// // DLNAMimeType = AUDIO;
	// // }
	// return DLNAMimeType;
	// }

	public static String createDLNAHeaderField() {
		return "contentFeatures.dlna.org: DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
	}

	public static String makeHttp206Reponse(String filename, long range) {
		String result = "";
		File f = new File(filename);
		String mimeType = URLConnection.getFileNameMap().getContentTypeFor(filename);
		result += "HTTP/1.1 206 Partial Content";
		result += NEWLINE;
		result += "Content-Type: " + mimeType;
		result += NEWLINE;
		result += "Content-Length: " + (f.length() - range);
		result += NEWLINE;
		result += "Content-Range: bytes " + (range + "-" + (f.length() - 1 + "-" + f.length()));
		result += NEWLINE;
		result += "Accept-Ranges: bytes";
		result += NEWLINE;
		result += createDLNAHeaderField();
		result += NEWLINE;
		result += "TransferMode.DLNA.ORG: Streaming";
		result += NEWLINE;
		result += "Date: " + DateTime.getCurrentDate().toString();
		result += NEWLINE;
		result += "Last-Modified: " + new Date(f.lastModified()).toString();
		result += NEWLINE;
		result += "Connection: close";
		result += NEWLINE;
		result += "Server: SimpleDMS";
		result += NEWLINE;
		result += NEWLINE;

		return result;
	}

	public static void handleClientRequest(final Socket client, String requesttype, long range, String filename) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			if (range == 0) {
				bw.write(HTTPHelper.makeHttp200Reponse(filename));
				bw.flush();
			} else {
				bw.write(HTTPHelper.makeHttp206Reponse(filename, range));
				bw.flush();
			}

			if (requesttype.equals("GET")) {

				DataInputStream dis = new DataInputStream(new FileInputStream(filename));
				dis.skip(range);
				DataOutputStream dos = new DataOutputStream(client.getOutputStream());

				byte[] b = new byte[BUFFERSIZE];
				long count = range;
				int bytesread = 0;
				while (HTTPServerData.RUNNING) {
					bytesread = dis.read(b);
					count += bytesread;
					dos.write(b, 0, bytesread);
					if (count == new File(filename).length())
						break;
				}
				dos.flush();

			}

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Client close connection");
		}
	}

	public static void handleProxyDataRequest(Socket client, List<String> rawrequest, String directLink) {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(directLink).openConnection();
			connection.setConnectTimeout(5000);
			for (String str : rawrequest) {
				if (str.contains("HEAD")) {
					connection.setRequestMethod("HEAD");
				} else if (str.contains("GET")) {
					connection.setRequestMethod("GET");
				} else if (str.contains("Range")) {
					connection.addRequestProperty("Range", str.split(":")[1]);
				}

			}
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
				Map<String, List<String>> resultHeaders = new HashMap<String, List<String>>();
				resultHeaders = connection.getHeaderFields();
				for (String key : resultHeaders.keySet()) {
					if (key == null)
						for (String value : resultHeaders.get(key)) {
							bw.write(value);
							bw.write(NEWLINE);
						}
					else
						for (String value : resultHeaders.get(key)) {
							bw.write(key + " : " + value);
							bw.write(NEWLINE);
						}
				}
				bw.write(createDLNAHeaderField());
				bw.write(NEWLINE);
				bw.write("TransferMode.DLNA.ORG: Streaming");
				bw.write(NEWLINE);
				bw.write(NEWLINE);
				bw.flush();
				if (connection.getRequestMethod().equals("GET")) {
					DataInputStream dis = new DataInputStream(connection.getInputStream());
					DataOutputStream dos = new DataOutputStream(client.getOutputStream());

					byte[] buffer = new byte[65536];
					int read = -1;
					while ((read = dis.read(buffer)) > 0 && HTTPServerData.RUNNING) {
						dos.write(buffer, 0, read);
						dos.flush();
					}

					try {
						dis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						dos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					try {
						bw.close();
					} catch (Exception e) {
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
