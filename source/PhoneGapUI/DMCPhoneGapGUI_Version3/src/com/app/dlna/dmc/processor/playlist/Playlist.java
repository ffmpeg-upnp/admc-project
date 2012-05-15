package com.app.dlna.dmc.processor.playlist;

public class Playlist {
	private long m_id;
	private String m_name;
	private int m_currentIdx;

	public long getId() {
		return m_id;
	}

	public void setId(long id) {
		this.m_id = id;
	}

	public String getName() {
		return m_name;
	}

	public void setName(String name) {
		this.m_name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof Playlist) {
			Playlist other = (Playlist) o;
			return m_name != null && other.m_name != null && m_name.equals(other.m_name) && m_id == other.m_id;
		}
		return false;
	}

	public int getCurrentIdx() {
		return m_currentIdx;
	}

	public void setCurrentIdx(int currentIdx) {
		this.m_currentIdx = currentIdx;
	}
}
