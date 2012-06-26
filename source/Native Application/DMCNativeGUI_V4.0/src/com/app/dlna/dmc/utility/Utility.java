package com.app.dlna.dmc.utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.item.AudioItem;
import org.teleal.cling.support.model.item.ImageItem;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.VideoItem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.http.HTTPServerData;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;

public class Utility {

	private static final String TAG = Utility.class.getName();

	public static String intToIp(int i) {
		String result = "";
		result = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
		return result;
	}

	public static String getMD5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			md.update(input.getBytes());
			StringBuffer hexString = new StringBuffer();
			byte[] mdbytes = md.digest();
			for (int i = 0; i < mdbytes.length; i++) {
				String hex = Integer.toHexString(0xff & mdbytes[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception ex) {
			return null;
		}
	}

	public static String createLink(File file) {
		try {
			return new URI("http", HTTPServerData.HOST + ":" + HTTPServerData.PORT, file.getAbsolutePath(), null, null)
					.toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String createLink(String path) {
		try {
			return new URI("http", HTTPServerData.HOST + ":" + HTTPServerData.PORT, path, null, null).toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getTimeString(long seconds) {
		StringBuilder sb = new StringBuilder();

		long hour = seconds / 3600;
		long minute = (seconds - hour * 3600) / 60;
		long second = seconds - hour * 3600 - minute * 60;
		sb.append(String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second));

		return sb.toString();
	}

	public static String convertSizeToString(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	public static void loadImageItemThumbnail(final ImageView image, final String imageUrl, final Map<String, Bitmap> cache,
			final int size) {
		MainActivity.INSTANCE.EXEC.execute(new Runnable() {

			@Override
			public void run() {
				try {
					final Bitmap bm = getBitmapFromURL(imageUrl, size);
					cache.put(imageUrl, bm);
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@SuppressWarnings("rawtypes")
						@Override
						public void run() {
							if (image.getTag() instanceof String && ((String) image.getTag()).equals(imageUrl))
								try {
									image.setImageBitmap(bm);
									if (image.getParent().getParent() instanceof ListView) {
										ListView list = (ListView) image.getParent().getParent();
										((ArrayAdapter) list.getAdapter()).notifyDataSetChanged();
									}
								} catch (Exception ex) {
									image.setImageResource(R.drawable.ic_didlobject_image);
								}
						}
					});
				} catch (MalformedURLException e) {
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							image.setImageResource(R.drawable.ic_didlobject_image);
						}

					});
					e.printStackTrace();
				} catch (IOException e) {
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							image.setImageResource(R.drawable.ic_didlobject_image);
						}

					});
					e.printStackTrace();
				} catch (OutOfMemoryError e) {
					MainActivity.INSTANCE.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							System.gc();
							image.setImageResource(R.drawable.ic_didlobject_image);
						}

					});
					e.printStackTrace();
				}
			}
		});
	}

	public static Bitmap getBitmapFromURL(final String imageUrl, int size) throws IOException, MalformedURLException {
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		byte[] buffer = IOUtils.toByteArray((InputStream) new URL(imageUrl).getContent());
		BitmapFactory.decodeByteArray(buffer, 0, buffer.length, o);

		int scale = 1;
		if (o.outHeight > size || o.outWidth > size) {
			scale = (int) Math.pow(2,
					(int) Math.round(Math.log(size / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
		}

		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;

		Bitmap result = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, o2);
		buffer = null;
		System.gc();
		return result;
	}

	public static String decodeYoutubeUrl(String directLink) throws UnsupportedEncodingException {
		String tmp = URLDecoder.decode(directLink, "UTF-8");
		directLink = tmp.split(" ")[0];
		return directLink;
	}

	public static CheckResult checkItemURL(PlaylistItem item) {
		CheckResult result = new CheckResult(item, false);
//		try {
//			HttpURLConnection connection = (HttpURLConnection) new URL(item.getUrl()).openConnection();
//			connection.setConnectTimeout(3000);
//			connection.setRequestMethod("HEAD");
//			result.setReachable(connection.getResponseCode() == HttpURLConnection.HTTP_OK);
//		} catch (Exception ex) {
//			Log.w(TAG, "check fail, url = " + item.getUrl());
//		}
		result.setReachable(true);

		return result;
	}

	public static String createMetaData(String title, PlaylistItem.Type type, String url) {
		Item item = null;
		Res res = new Res(new ProtocolInfo("*:*:*:*"), 0l, url);
		switch (type) {
		case AUDIO_LOCAL:
		case AUDIO_REMOTE:
			item = new AudioItem("0", "0", title, "", res);
			break;
		case YOUTUBE:
		case VIDEO_LOCAL:
		case VIDEO_REMOTE:
			item = new VideoItem("0", "0", title, "", res);
			break;
		case IMAGE_LOCAL:
		case IMAGE_REMOTE:
			item = new ImageItem("0", "0", title, "", res);
			break;
		default:
			break;
		}
		if (item != null) {
			DIDLParser ps = new DIDLParser();
			DIDLContent ct = new DIDLContent();
			ct.addItem(item);
			try {
				return ps.generate(ct);
			} catch (Exception e) {
				return "";
			}
		} else {
			return "";
		}
	}

	public static class CheckResult {
		private PlaylistItem item;
		private boolean reachable;

		public CheckResult(PlaylistItem item, boolean reachable) {
			this.item = item;
			this.reachable = reachable;
		}

		public PlaylistItem getItem() {
			return item;
		}

		public void setItem(PlaylistItem item) {
			this.item = item;
		}

		public boolean isReachable() {
			return reachable;
		}

		public void setReachable(boolean reachable) {
			this.reachable = reachable;
		}

	}
}
