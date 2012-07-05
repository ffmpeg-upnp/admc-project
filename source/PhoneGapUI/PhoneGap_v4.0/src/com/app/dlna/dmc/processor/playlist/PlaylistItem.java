package com.app.dlna.dmc.processor.playlist;

import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.item.AudioItem;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.VideoItem;

import com.app.dlna.dmc.utility.Utility;

public class PlaylistItem {
	public enum Type {
		VIDEO, AUDIO, IMAGE, UNKNOW
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
	}

	public void setId(long id) {
		m_id = id;
	}

	public long getId() {
		return m_id;
	}

	public String getUrl() {
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
		return other.getUrl().equals(this.getUrl()) && other.m_type.equals(this.m_type) && other.m_title.equals(this.m_title);
	}

	public static PlaylistItem createFromDLDIObject(DIDLObject object) {
		PlaylistItem item = new PlaylistItem();
		item.setTitle(object.getTitle());
		item.setUrl(object.getResources().get(0).getValue());
		if (object instanceof AudioItem) {
			item.setType(Type.AUDIO);
		} else if (object instanceof VideoItem) {
			item.setType(Type.VIDEO);
		} else {
			item.setType(Type.IMAGE);
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

	public String getMetaData() {
		return m_metadata;
	}

	public void setMetaData(String metadata) {
		this.m_metadata = metadata;
	}

}