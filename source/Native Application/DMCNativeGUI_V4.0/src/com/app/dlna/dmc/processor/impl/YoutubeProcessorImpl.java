package com.app.dlna.dmc.processor.impl;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.processor.http.HTTPServerData;
import com.app.dlna.dmc.processor.interfaces.YoutubeProcessor;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;
import com.app.dlna.dmc.utility.Utility;

public class YoutubeProcessorImpl implements YoutubeProcessor {

	private static final String REGEX = "yt.preload.start\\([^\\)]{1,}\\)";
	private static final String REQUEST_COUNT = "50";
	private static final String QUERY_VIDEO = "http://gdata.youtube.com/feeds/api/videos?v=2&q={query}&start-index=1&max-results={count}&v=2&alt=json";
	private static final String QUERY_VIDEO_INFO = "http://www.youtube.com/get_video_info?video_id={id}";
	private static final String QUERY_HTML = "http://www.youtube.com/watch?v={id}";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.202 Safari/535.1";

	// private static final String TAG = YoutubeProcessorImpl.class.getName();

	@Override
	public void getDirectLinkAsync(final YoutubeItem item, final IYoutubeProcessorListener callback) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					callback.onStartPorcess();
					String directLink = getDirectLink(item.getId());
					if (directLink.trim().isEmpty()) {
						callback.onFail(new RuntimeException("Cannot get Youtube Video"));
					} else {
						item.setDirectLink(directLink);
						callback.onGetLinkComplete(item);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					callback.onFail(ex);
				}
			}

		}).start();

	}

	@Override
	public void registURLAsync(final YoutubeItem item, final IYoutubeProcessorListener callback) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					callback.onStartPorcess();
					String directlink = getDirectLink(item.getId());
					if (directlink == null || directlink.trim().isEmpty())
						callback.onFail(new RuntimeException("Cannot get directlink from Youtube"));
					String disget = Utility.getMD5(directlink);
					String disgetLink = "/" + disget.substring(0, disget.length() - 1) + ".mp4";
					HTTPServerData.LINK_MAP.put(disgetLink, directlink);
					// Log.e(TAG, "DirectLink = " + directlink);
					// Log.e(TAG, "DisgetLink = " + disgetLink);
					item.setDirectLink("http://" + HTTPServerData.HOST + ":" + HTTPServerData.PORT + disgetLink);
					callback.onGetLinkComplete(item);
				} catch (Exception ex) {
					ex.printStackTrace();
					callback.onFail(ex);
				}
			}

		}).start();
		//
		// @Override
		// public void onStartPorcess() {
		//
		// }
		//
		// @Override
		// public void onFail(Exception ex) {
		//
		// }
		//
		// @Override
		// public void onGetDirectLinkComplete(String result) {
		// String disget = Utility.getMD5(result);
		// String disgetLink = "/" + disget.substring(0, disget.length() - 1) +
		// ".mp4";
		// try {
		// HTTPLinkManager.LINK_MAP.put(disgetLink, result);
		// Log.d(TAG, "DirectLink = " + result);
		// Log.d(TAG, "DisgetLink = " + disgetLink);
		// callback.onGetDirectLinkComplete(disgetLink);
		// } catch (Exception ex) {
		// ex.printStackTrace();
		// callback.onFail(ex);
		// }
		//
		// }
		//
		// @Override
		// public void onSearchComplete(List<YoutubeItem> result) {
		//
		// }
		// });

	}

	@Override
	public void executeQueryAsync(final String query, final IYoutubeProcessorListener callback) {
		callback.onStartPorcess();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String _query = query.replace(" ", "%20");
					URL jsonURL = new URL(QUERY_VIDEO.replace("{query}", _query).replace("{count}", REQUEST_COUNT));
					String jsonTxt = IOUtils.toString(jsonURL, "utf-8");
					JSONObject jj = new JSONObject(jsonTxt);
					JSONObject feeds = jj.getJSONObject("feed");
					JSONArray entries = feeds.getJSONArray("entry");
					List<YoutubeItem> youtubeItems = new ArrayList<YoutubeItem>();
					for (int i = 0; i < entries.length(); ++i) {
						JSONObject mediaGroup = entries.getJSONObject(i).getJSONObject("media$group");
						YoutubeItem youtubeItem = new YoutubeItem();
						youtubeItem.setId(mediaGroup.getJSONObject("yt$videoid").getString("$t"));
						youtubeItem.setTitle(mediaGroup.getJSONObject("media$title").getString("$t"));
						youtubeItem.setDuration(Long.valueOf(mediaGroup.getJSONObject("yt$duration").getString("seconds")));
						JSONArray thumbs = mediaGroup.getJSONArray("media$thumbnail");
						youtubeItem.setHTMLLink(mediaGroup.getJSONObject("media$player").getString("url"));
						if (thumbs.length() > 0)
							youtubeItem.setThumbnail(thumbs.getJSONObject(0).getString("url"));
						youtubeItem.setAuthor(entries.getJSONObject(i).getJSONArray("author").getJSONObject(0)
								.getJSONObject("name").getString("$t"));
						youtubeItems.add(youtubeItem);
					}
					callback.onSearchComplete(youtubeItems);
				} catch (Exception ex) {
					callback.onFail(ex);
				}

			}
		}).start();

	}

	@Override
	public String getDirectLink(String id) {
		String directLink = "";
		try {
			URL url = new URL(QUERY_VIDEO_INFO.replace("{id}", id));
			String toDecode = IOUtils.toString(url);
			String[] splitToParam = toDecode.split("&");
			String urls = "";
			for (String str : splitToParam) {
				if (str.contains("url_encoded_fmt_stream_map")) {
					urls = str;
				}
			}
			if (urls.isEmpty()) {
				URL htmlUrl = new URL(QUERY_HTML.replace("{id}", id));
				HttpGet httpGet = new HttpGet(htmlUrl.toURI());
				httpGet.setHeader("User-Agent", USER_AGENT);
				HttpClient client = new DefaultHttpClient();
				HttpResponse response = client.execute(httpGet);
				String html = IOUtils.toString(response.getEntity().getContent());
				Pattern pattern = Pattern.compile(REGEX);
				Matcher matcher = pattern.matcher(html);
				while (matcher.find()) {
					String str = matcher.group();
					if (!str.contains("crossdomain.xml")) {
						directLink = str.replace("\\u0026", "&").replace("\\", "").replace("generate_204", "videoplayback")
								.replace("yt.preload.start(\"", "").replace("\")", "");
						directLink = Utility.decodeYoutubeUrl(directLink);
						break;
					}
				}
			} else {
				String hd720 = "";
				String medium = "";
				String other = "";
				String decoded = URLDecoder.decode(urls, "UTF-8");
				for (String str : decoded.split("url=")) {
					if (str.contains("mp4")) {
						if (str.contains("hd720")) {
							hd720 = str;
						} else if (str.contains("medium")) {
							medium = str;
						} else {
							other = str;
						}
					}
				}
				hd720 = Utility.decodeYoutubeUrl(hd720);
				medium = Utility.decodeYoutubeUrl(medium);
				other = Utility.decodeYoutubeUrl(other);
				if (AppPreference.isVideoHQ()) {
					directLink = hd720;
					if (directLink.isEmpty())
						directLink = medium;
					if (directLink.isEmpty())
						directLink = other;
				} else {
					directLink = medium;
					if (directLink.isEmpty())
						directLink = other;
					if (directLink.isEmpty())
						directLink = hd720;
				}
			}
		} catch (Exception e) {
			directLink = "";
		}
		return directLink;
	}
}
