package com.app.dlna.dmc.utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.processor.http.HTTPServerData;

public class Utility {
	public static final String TAG = Utility.class.getName();

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

	public static void loadImageItemThumbnail(final ImageView image, final String imageUrl,
			final Map<String, Bitmap> m_cacheImageItem, final int size) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (m_cacheImageItem == null) {
						final Bitmap bm = getBitmapFromURL(imageUrl, size);
						MainActivity.INSTANCE.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									image.setImageBitmap(bm);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						});
					} else {
						if (m_cacheImageItem.containsKey(imageUrl)) {
							MainActivity.INSTANCE.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									try {
										synchronized (m_cacheImageItem) {
											image.setImageBitmap(m_cacheImageItem.get(imageUrl));
										}
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							});
						} else {
							final Bitmap bm = getBitmapFromURL(imageUrl, size);
							synchronized (m_cacheImageItem) {
								m_cacheImageItem.put(imageUrl, bm);
							}
							MainActivity.INSTANCE.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									try {
										image.setImageBitmap(bm);
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							});
						}
					}

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
				}
			}
		}).start();
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


		return BitmapFactory.decodeByteArray(buffer, 0, buffer.length, o2);
	}
}
