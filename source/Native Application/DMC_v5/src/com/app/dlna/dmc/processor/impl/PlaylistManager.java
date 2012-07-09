package com.app.dlna.dmc.processor.impl;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.app.dlna.dmc.data.PlaylistProvider;
import com.app.dlna.dmc.data.PlaylistSQLiteHelper;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.model.Playlist;
import com.app.dlna.dmc.processor.model.PlaylistItem;
import com.app.dlna.dmc.processor.model.PlaylistItem.Type;

public class PlaylistManager {
	private static final int MAX_ITEM = 64;
	public static ContentResolver RESOLVER = null;

	public static List<Playlist> getAllPlaylist() {
		try {
			List<Playlist> result = new ArrayList<Playlist>();
			Cursor cursor = RESOLVER.query(PlaylistProvider.PLAYLIST_URI, PlaylistSQLiteHelper.PLAYLIST_ALLCOLUMNS,
					null, null, null);
			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(cursor.getColumnIndex(PlaylistSQLiteHelper.COL_ID));
					String name = cursor.getString(cursor.getColumnIndex(PlaylistSQLiteHelper.COL_NAME));
					Playlist playlist = new Playlist();
					playlist.setId(id);
					playlist.setName(name);
					playlist.setCurrentIdx(cursor.getInt(cursor
							.getColumnIndex(PlaylistSQLiteHelper.COL_CURRENT_POSITION)));
					result.add(playlist);

				} while (cursor.moveToNext());
			}
			cursor.close();
			return result;
		} catch (Exception ex) {
			ex.printStackTrace();
			return new ArrayList<Playlist>();
		}
	}

	public static List<PlaylistItem> getAllPlaylistItem(long id) {
		try {
			List<PlaylistItem> result = new ArrayList<PlaylistItem>();
			Cursor itemsCursor = RESOLVER.query(PlaylistProvider.PLAYLIST_ITEM_URI,
					PlaylistSQLiteHelper.PLALYLISTITEM_ALLCOLUMNS, PlaylistSQLiteHelper.COL_PLAYLIST_ID + " = ? ",
					new String[] { String.valueOf(id) }, null);
			if (itemsCursor != null && itemsCursor.moveToFirst()) {
				do {
					result.add(cursorToPlaylistItem(itemsCursor));
				} while (itemsCursor.moveToNext());
			}
			itemsCursor.close();
			return result;
		} catch (Exception ex) {
			return null;
		}
	}

	public static PlaylistItem cursorToPlaylistItem(Cursor cursor) {
		try {
			long id = cursor.getLong(cursor.getColumnIndex(PlaylistSQLiteHelper.COL_ID));
			String title = cursor.getString(cursor.getColumnIndex(PlaylistSQLiteHelper.COL_TITLE));
			String url = cursor.getString(cursor.getColumnIndex(PlaylistSQLiteHelper.COL_URL));
			String type = cursor.getString(cursor.getColumnIndex(PlaylistSQLiteHelper.COL_TYPE));
			String metadata = cursor.getString(cursor.getColumnIndex(PlaylistSQLiteHelper.COL_METADATA));
			PlaylistItem res = new PlaylistItem();
			res.setId(id);
			res.setTitle(title);
			res.setType(Type.valueOf(type));
			res.setUrl(url);
			res.setMetaData(metadata);
			return res;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static boolean createPlaylistItem(PlaylistItem playlistItem, long playlist_id) {
		try {
			ContentValues values = new ContentValues();
			values.put(PlaylistSQLiteHelper.COL_TITLE, playlistItem.getTitle());
			values.put(PlaylistSQLiteHelper.COL_URL, playlistItem.getUrl());
			values.put(PlaylistSQLiteHelper.COL_TYPE, playlistItem.getType().toString());
			values.put(PlaylistSQLiteHelper.COL_PLAYLIST_ID, playlist_id);
			values.put(PlaylistSQLiteHelper.COL_METADATA, playlistItem.getMetaData());
			Uri uri = RESOLVER.insert(PlaylistProvider.PLAYLIST_ITEM_URI, values);
			if (uri == null)
				return false;
			String newId = uri.getQueryParameter("newid");
			playlistItem.setId(Long.valueOf(newId));
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public static boolean createPlaylist(Playlist playlist) {
		try {
			ContentValues values = new ContentValues();
			values.put(PlaylistSQLiteHelper.COL_NAME, playlist.getName());
			values.put(PlaylistSQLiteHelper.COL_CURRENT_POSITION, playlist.getCurrentIdx());
			Uri uri = RESOLVER.insert(PlaylistProvider.PLAYLIST_URI, values);
			if (uri != null) {
				String newId = uri.getQueryParameter("newid");
				playlist.setId(Long.valueOf(newId));
				ContentValues updateValues = new ContentValues();
				updateValues.put(PlaylistSQLiteHelper.COL_PLAYLIST_ID, newId);
				RESOLVER.update(PlaylistProvider.PLAYLIST_ITEM_URI, updateValues, PlaylistSQLiteHelper.COL_PLAYLIST_ID
						+ " = ?", new String[] { "1" });
				return true;
			}
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public static boolean deletePlaylist(PlaylistProcessor playlistProcessor) {
		return deletePlaylist(playlistProcessor.getData());
	}

	public static boolean deletePlaylist(Playlist playlist) {
		long playlistId = playlist.getId();
		RESOLVER.delete(PlaylistProvider.PLAYLIST_ITEM_URI, PlaylistSQLiteHelper.COL_PLAYLIST_ID + " = ?",
				new String[] { String.valueOf(playlistId) });
		int playlistCount = RESOLVER.delete(PlaylistProvider.PLAYLIST_URI, PlaylistSQLiteHelper.COL_ID + " = ?",
				new String[] { String.valueOf(playlistId) });
		return playlistCount == 1;
	}

	public static PlaylistProcessor getPlaylistProcessor(Playlist playlist) {
		PlaylistProcessor processor = new PlaylistProcessorImpl(playlist, MAX_ITEM);
		for (PlaylistItem item : getAllPlaylistItem(playlist.getId())) {
			processor.getAllItems().add(item);
		}

		return processor;
	}

	public static boolean deletePlaylistItem(long id) {
		return 1 == RESOLVER.delete(PlaylistProvider.PLAYLIST_ITEM_URI, PlaylistSQLiteHelper.COL_ID + " = ?",
				new String[] { String.valueOf(id) });
	}

	public static void clearPlaylist(long id) {
		RESOLVER.delete(PlaylistProvider.PLAYLIST_ITEM_URI, PlaylistSQLiteHelper.COL_PLAYLIST_ID + " = ?",
				new String[] { String.valueOf(id) });
	}

	public static void savePlaylistState(Playlist playlist) {
		ContentValues values = new ContentValues();
		values.put(PlaylistSQLiteHelper.COL_CURRENT_POSITION, playlist.getCurrentIdx());
		RESOLVER.update(PlaylistProvider.PLAYLIST_URI, values, PlaylistSQLiteHelper.COL_ID + " = ?",
				new String[] { String.valueOf(playlist.getId()) });
	}

	public static int insertAllItem(List<PlaylistItem> items, long playlist_id) {
		ContentValues[] valuesList = new ContentValues[items.size()];
		for (int i = 0; i < items.size(); ++i) {
			valuesList[i] = new ContentValues();
			PlaylistItem playlistItem = items.get(i);
			valuesList[i].put(PlaylistSQLiteHelper.COL_TITLE, playlistItem.getTitle());
			valuesList[i].put(PlaylistSQLiteHelper.COL_URL, playlistItem.getUrl());
			valuesList[i].put(PlaylistSQLiteHelper.COL_TYPE, playlistItem.getType().toString());
			valuesList[i].put(PlaylistSQLiteHelper.COL_PLAYLIST_ID, playlist_id);
			valuesList[i].put(PlaylistSQLiteHelper.COL_METADATA, playlistItem.getMetaData());
		}
		return RESOLVER.bulkInsert(PlaylistProvider.PLAYLIST_ITEM_URI, valuesList);
	}

	public static void renamePlaylist(long id, String value) {
		ContentValues values = new ContentValues();
		values.put(PlaylistSQLiteHelper.COL_NAME, value);
		RESOLVER.update(PlaylistProvider.PLAYLIST_URI, values, PlaylistSQLiteHelper.COL_ID + " = ?",
				new String[] { String.valueOf(id) });
	}
}
