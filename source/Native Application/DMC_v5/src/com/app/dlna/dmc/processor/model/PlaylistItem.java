package com.app.dlna.dmc.processor.model;

import java.net.URI;
import java.net.URISyntaxException;

import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.item.AudioItem;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.VideoItem;

import android.net.Uri;

import com.app.dlna.dmc.http.HTTPServerData;
import com.app.dlna.dmc.utility.Utility;

public class PlaylistItem {
	public enum Type {
		VIDEO_LOCAL, AUDIO_LOCAL, IMAGE_LOCAL, YOUTUBE, AUDIO_REMOTE, VIDEO_REMOTE, IMAGE_REMOTE, UNKNOW,
	}

	private long m_id;
	private String m_url;
	private String m_title;
	private Type m_type;
	private String m_metadata;

	public PlaylistItem() {
		m_id = -1;
		m_url = "";
		m_title = "";
		m_type = Type.UNKNOW;
		m_metadata = "";
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
				current = new URI("http", HTTPServerData.HOST + ":" + HTTPServerData.PORT, uri.getPath(), null, null).toString();
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

	public String getMetaData() {
		if (m_metadata.isEmpty())
			return Utility.createMetaData(m_title, m_type, m_url);
		return m_metadata;
	}

	public void setMetaData(String metadata) {
		m_metadata = metadata;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PlaylistItem))
			return false;
		PlaylistItem other = (PlaylistItem) o;
		return other.getUrl().equals(this.getUrl()) && other.m_type.equals(this.m_type) && other.m_title.equals(this.m_title);
	}

	public static PlaylistItem createFromDLDIObject(DIDLObject object) {
		PlaylistItem item = new PlaylistItem();
		item.setTitle(object.getTitle());
		Uri uri = Uri.parse(object.getResources().get(0).getValue());
		boolean isLocal = uri.getHost().equals(HTTPServerData.HOST) && uri.getPort() == HTTPServerData.PORT;
		item.setUrl(object.getResources().get(0).getValue());
		if (object instanceof AudioItem) {
			if (isLocal) {
				item.setType(Type.AUDIO_LOCAL);
			} else
				item.setType(Type.AUDIO_REMOTE);
		} else if (object instanceof VideoItem) {
			if (isLocal) {
				item.setType(Type.VIDEO_LOCAL);
			} else
				item.setType(Type.VIDEO_REMOTE);
		} else {
			if (isLocal) {
				item.setType(Type.IMAGE_LOCAL);
			} else
				item.setType(Type.IMAGE_REMOTE);
		}
		DIDLParser ps = new DIDLParser();
		DIDLContent ct = new DIDLContent();
		ct.addItem((Item) object);
		try {
			item.setMetaData(ps.generate(ct));
		} catch (Exception e) {
			item.setMetaData("");
		}
		return item;
	}

}