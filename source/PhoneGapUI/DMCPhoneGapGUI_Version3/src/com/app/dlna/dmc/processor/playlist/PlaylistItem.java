package com.app.dlna.dmc.processor.playlist;

import java.net.URI;
import java.net.URISyntaxException;

import com.app.dlna.dmc.processor.http.HTTPServerData;

public class PlaylistItem {
	public enum Type {
		VIDEO_LOCAL, AUDIO_LOCAL, IMAGE_LOCAL, YOUTUBE, AUDIO_REMOTE, VIDEO_REMOTE, IMAGE_REMOTE, UNKNOW,
	}

	private long m_id;
	private String m_url;
	private String m_title;
	private Type m_type;

	public PlaylistItem() {
		m_id = -1;
		m_url = "";
		m_title = "";
		m_type = Type.UNKNOW;
	}

	public void setId(long id) {
		m_id = id;
	}

	public long getId() {
		return m_id;
	}

	public String getUrl() {
		if (m_type == Type.IMAGE_LOCAL || m_type == Type.VIDEO_LOCAL || m_type == Type.AUDIO_LOCAL) {
			String current;
			try {
				URI uri = URI.create(m_url);
				current = new URI("http", HTTPServerData.HOST + ":" + HTTPServerData.PORT, uri.getPath(), null, null)
						.toString();
				return current;
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return m_url;
	}

	public void setUrl(String url) {
		this.m_url = url;
	}

	public String getTitle() {
		return m_title;
	}

	public void setTitle(String title) {
		this.m_title = title;
	}

	public Type getType() {
		return m_type;
	}

	public void setType(Type type) {
		this.m_type = type;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PlaylistItem))
			return false;
		PlaylistItem other = (PlaylistItem) o;
		return other.getUrl().equals(this.getUrl()) && other.m_type.equals(this.m_type)
				&& other.m_title.equals(this.m_title);
	}

}