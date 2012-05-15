package com.app.dlna.dmc.processor.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class PlaylistProvider extends ContentProvider {
	private static final String AUTHORITY = "com.app.dlna.dmc.native.processor.provider.playlistprovider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");
	public static final Uri PLAYLIST_URI = Uri.parse("content://" + AUTHORITY + "/playlist");
	public static final Uri PLAYLIST_ITEM_URI = Uri.parse("content://" + AUTHORITY + "/playlist_item");
	private static final int PLAYLIST = 1;
	private static final int PLAYLIST_ITEM = 2;

	private PlaylistSQLiteHelper dbPlaylistHelper;

	private static final UriMatcher uriMatcher;
	private static final String TAG = PlaylistProvider.class.getName();
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "playlist", PLAYLIST);
		uriMatcher.addURI(AUTHORITY, "playlist_item", PLAYLIST_ITEM);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		switch (uriMatcher.match(uri)) {
		case PLAYLIST: {
			SQLiteDatabase database = dbPlaylistHelper.getWritableDatabase();
			int deleted = database.delete(PlaylistSQLiteHelper.TABLE_PLAYLISTS, selection, selectionArgs);
			database.close();
			return deleted;
		}
		case PLAYLIST_ITEM: {
			Log.i(TAG,"Delete item, id = " + selectionArgs[0]);
			SQLiteDatabase database = dbPlaylistHelper.getWritableDatabase();
			int deleted = database.delete(PlaylistSQLiteHelper.TABLE_PLAYLIST_ITEMS, selection, selectionArgs);
			Log.i(TAG, "Delete item count = " + deleted);
			database.close();
			return deleted;
		}
		}
		return -1;
	}

	@Override
	public String getType(Uri uri) {
		return String.valueOf(uriMatcher.match(uri));
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		switch (uriMatcher.match(uri)) {
		case PLAYLIST: {
			SQLiteDatabase database = dbPlaylistHelper.getWritableDatabase();
			long newId = database.insert(PlaylistSQLiteHelper.TABLE_PLAYLISTS, null, values);
			database.close();
			return Uri.parse(PLAYLIST_URI.toString() + "/?newid=" + newId);
		}
		case PLAYLIST_ITEM: {
			Log.i(TAG, "Insert playlist item " + values.toString());
			SQLiteDatabase database = dbPlaylistHelper.getWritableDatabase();
			long newId = database.insert(PlaylistSQLiteHelper.TABLE_PLAYLIST_ITEMS, null, values);
			database.close();
			return Uri.parse(PLAYLIST_ITEM_URI.toString() + "/?newid=" + newId);
		}
		default:
			return null;
		}
	}

	@Override
	public boolean onCreate() {
		dbPlaylistHelper = new PlaylistSQLiteHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor result = null;
		switch (uriMatcher.match(uri)) {
		case PLAYLIST: {
			Log.i(TAG, "query all playlist");
			SQLiteDatabase database = dbPlaylistHelper.getReadableDatabase();
			result = database.query(PlaylistSQLiteHelper.TABLE_PLAYLISTS, projection, null, null, null, null, null);
			break;
		}
		case PLAYLIST_ITEM: {
			Log.i(TAG, "query all playlist item, selection = " + selection);
			SQLiteDatabase database = dbPlaylistHelper.getReadableDatabase();
			result = database.query(PlaylistSQLiteHelper.TABLE_PLAYLIST_ITEMS, projection, selection, selectionArgs,
					null, null, null);
			break;
		}
		default:
			break;
		}
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = -1;
		switch (uriMatcher.match(uri)) {
		case PLAYLIST: {
			Log.i(TAG, "Update Playlist = " + values.toString());
			SQLiteDatabase database = dbPlaylistHelper.getWritableDatabase();
			count = database.update(PlaylistSQLiteHelper.TABLE_PLAYLISTS, values, selection, selectionArgs);
			database.close();
			break;
		}
		case PLAYLIST_ITEM: {
			Log.i(TAG, "UpdateItem = " + values.toString());
			SQLiteDatabase database = dbPlaylistHelper.getWritableDatabase();
			count = database.update(PlaylistSQLiteHelper.TABLE_PLAYLIST_ITEMS, values, selection, selectionArgs);
			database.close();
			break;
		}
		default:
			break;
		}
		return count;
	}

}
