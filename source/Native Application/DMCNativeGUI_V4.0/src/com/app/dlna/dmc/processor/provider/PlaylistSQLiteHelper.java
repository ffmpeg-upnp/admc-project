package com.app.dlna.dmc.processor.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PlaylistSQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_PLAYLIST_ITEMS = "PlaylistItems";
	public static final String COL_ID = "_id";
	public static final String COL_TITLE = "title";
	public static final String COL_URL = "url";
	public static final String COL_TYPE = "type";
	public static final String COL_PLAYLIST_ID = "playlist_id";
	public static final String COL_METADATA = "metadata";

	public static final String TABLE_PLAYLISTS = "playlists";
	public static final String COL_NAME = "name";
	public static final String COL_CURRENT_POSITION = "current_item";

	private static final String DATABASE_NAME = "playlists.db";
	private static final int DATABASE_VERSION = 11;

	private static final String UNSAVED = "Unsaved Playlist";
	private static final String DEFAULT_POS = "-1";

	private static final String DATABASE_CREATE_PLAYLIST = "create table " + TABLE_PLAYLISTS + "( " + COL_ID
			+ " integer primary key autoincrement, " + COL_NAME + " text not null," + COL_CURRENT_POSITION
			+ " integer not null);";

	private static final String DATABASE_CREATE_PLAYLIST_ITEM = "create table " + TABLE_PLAYLIST_ITEMS + "( " + COL_ID
			+ " integer primary key autoincrement, " + COL_TITLE + " text not null, " + COL_URL + " text not null, "
			+ COL_TYPE + " text not null, " + COL_PLAYLIST_ID + " text not null, " + COL_METADATA + " text not null);";

	private static final String DATABASE_CREATE_UNSAVEDLIST = "insert into " + TABLE_PLAYLISTS + " (" + COL_NAME + ","
			+ COL_CURRENT_POSITION + ") values ('" + UNSAVED + "','" + DEFAULT_POS + "');";

	public static String[] PLAYLIST_ALLCOLUMNS = { COL_ID, COL_NAME, COL_CURRENT_POSITION };
	public static String[] PLALYLISTITEM_ALLCOLUMNS = { COL_ID, COL_TITLE, COL_URL, COL_TYPE, COL_PLAYLIST_ID, COL_METADATA };

	public PlaylistSQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE_PLAYLIST);
		db.execSQL(DATABASE_CREATE_UNSAVEDLIST);
		db.execSQL(DATABASE_CREATE_PLAYLIST_ITEM);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_ITEMS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
		onCreate(db);
	}

}
