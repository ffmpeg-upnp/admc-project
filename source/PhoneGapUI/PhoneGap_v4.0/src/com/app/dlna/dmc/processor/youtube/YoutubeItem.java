package com.app.dlna.dmc.processor.youtube;

public class YoutubeItem {
	private String id;
	private String title;
	private long duration;
	private String thumbnail;
	private String author;
	private String htmlLink;
	private String directLink;

	public YoutubeItem() {

	}

	public YoutubeItem(String id) {
		this.id = id;
	}

	public YoutubeItem(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setHTMLLink(String htmlLink) {
		this.htmlLink = htmlLink;
	}

	public String getHTMLLink() {
		return htmlLink;
	}

	public void setDirectLink(String directLink) {
		this.directLink = directLink;
	}

	public String getDirectLink() {
		return this.directLink;
	}
}
