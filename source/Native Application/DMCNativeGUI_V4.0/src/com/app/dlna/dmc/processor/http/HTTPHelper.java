package com.app.dlna.dmc.processor.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;
import java.util.List;

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

	public static String createDLNAHeaderField() {
		return "contentFeatures.dlna.org: *";
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
		result += "transferMode.dlna.org: Streaming";
		result += NEWLINE;
		result += "Cache-Control: no-cache";
		result += NEWLINE;
		result += "Server: Android, UPnP/1.0 DLNADOC/1.50, Simple DMS";
		result += NEWLINE;
		result += NEWLINE;

		return result;
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
		result += "transferMode.dlna.org: Streaming";
		result += NEWLINE;
		result += "Cache-Control: no-cache";
		result += NEWLINE;
		result += "Server: Android, UPnP/1.0 DLNADOC/1.50, Simple DMS";
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
				try {
					dos.close();
				} catch (Exception ex) {

				}
				try {
					dis.close();
				} catch (Exception ex) {

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Client close connection");
		} finally {
			try {
				client.close();
			} catch (Exception ex) {

			}
		}
	}

	public static void handleProxyDataRequest(Socket client, List<String> rawrequest, String directLink) {
		try {
			URL youtube = new URL(directLink);
			Socket socket = new Socket(youtube.getHost(), 80);
			PrintStream ps = new PrintStream(socket.getOutputStream());

			for (String requestLine : rawrequest) {
				String[] token = requestLine.split(" ");
				if (requestLine.contains("HEAD") || requestLine.contains("GET")) {
					ps.println(token[0] + " " + youtube.getFile() + " HTTP/1.1");
				} else if (requestLine.contains("Host")) {
					ps.println("Host: " + youtube.getAuthority());
				} else if (requestLine.contains("Cache-Control:")) {
					ps.println("Cache-Control: no-cache");
				} else {
					ps.println(requestLine);
				}
			}
			ps.println();
			ps.flush();

			// Handle HTTP Header

			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintStream clientPs = new PrintStream(client.getOutputStream());
			String line = null;
			while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
				clientPs.println(line);
			}
			clientPs.println(createDLNAHeaderField());
			clientPs.println("transferMode.dlna.org: Streaming");
			clientPs.println();
			clientPs.flush();

			// Handle Data

			DataOutputStream dos = new DataOutputStream(client.getOutputStream());
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			byte[] buffer = new byte[65536];
			int read = -1;
			while ((read = dis.read(buffer)) > 0 && HTTPServerData.RUNNING) {
				dos.write(buffer, 0, read);
			}
			try {
				clientPs.close();
			} catch (Exception e) {
			}
			try {
				br.close();
			} catch (Exception e) {
			}
			try {
				dis.close();
			} catch (Exception e) {
			}
			try {
				dos.close();
			} catch (Exception ex) {

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (Exception ex) {

			}
		}
	}
}
