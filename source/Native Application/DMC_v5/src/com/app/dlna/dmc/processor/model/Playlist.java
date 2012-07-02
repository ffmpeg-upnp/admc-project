package com.app.dlna.dmc.processor.model;

import com.app.dlna.dmc.processor.model.PlaylistItem.Type;

public class Playlist {
	private long m_id;
	private String m_name;
	private int m_currentIdx;
	private String m_metadata;

	public Playlist() {
		m_id = -1;
		m_name = "";
		m_currentIdx = 0;
	}

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

	public String getMetaData() {
		return m_metadata;
	}

	public void setMetaData(String metaData) {
		this.m_metadata = metaData;
	}

	public enum ViewMode {
		ALL("All items", "All"), AUDIO_ONLY("Audio only", "Audio"), VIDEO_ONLY("Video only", "Video"), IMAGE_ONLY(
				"Image only", "Image");
		String viewMode = "";
		String compactString;

		ViewMode(String viewMode, String compactString) {
			this.viewMode = viewMode;
			this.compactString = compactString;
		}

		public String getString() {
			return viewMode;
		}

		public String getCompactString() {
			return compactString;
		}

		public boolean compatibleWith(Type type) {
			if (this.equals(ALL))
				return true;
			switch (type) {
			case AUDIO_LOCAL:
			case AUDIO_REMOTE:
				return this.equals(AUDIO_ONLY);
			case VIDEO_LOCAL:
			case VIDEO_REMOTE:
			case YOUTUBE:
				return this.equals(VIDEO_ONLY);
			case IMAGE_LOCAL:
			case IMAGE_REMOTE:
				return this.equals(IMAGE_ONLY);
			default:
				return false;
			}
		}
	}
}
