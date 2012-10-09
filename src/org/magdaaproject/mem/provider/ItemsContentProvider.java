/*
 * Copyright (C) 2012 The MaGDAA Project
 *
 * This file is part of the MaGDAA MEM Software
 *
 * MaGDAA MEM Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.magdaaproject.mem.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * a class which provides access to the sensor readings content
 */
public class ItemsContentProvider extends ContentProvider {

	/*
	 * public class level constants
	 */
	/**
	 * authority string for the content provider
	 */
	public static final String AUTHORITY = "org.magdaaproject.mem.provider.items";

	/*
	 * private class level constants
	 */
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int sReadingsListUri = 0;
	private static final int sReadingsItemUri = 1;

	private static final String sTag = "ItemsContentProvider";

	/*
	 * private class level variables
	 */
	private MainDatabaseHelper databaseHelper;
	private SQLiteDatabase database;

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {

		//define which URIs to match
		sUriMatcher.addURI(AUTHORITY, ReadingsContract.CONTENT_URI_PATH, sReadingsListUri);
		sUriMatcher.addURI(AUTHORITY, ReadingsContract.CONTENT_URI_PATH + "/#", sReadingsItemUri);

		// create the database if necessary
		databaseHelper = new MainDatabaseHelper(getContext());

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		// choose the uri and table name to match against
		switch(sUriMatcher.match(uri)) {
		case sReadingsListUri:
			// uri matches the entire table
			if(TextUtils.isEmpty(sortOrder) == true) {
				sortOrder = ReadingsContract.Table.TIMESTAMP + " DESC";
			}
			break;
		case sReadingsItemUri:
			// uri matches a single item
			if(TextUtils.isEmpty(selection) == true) {
				selection = ReadingsContract.Table._ID + " = " + uri.getLastPathSegment();
			} else {
				selection += " AND " + ReadingsContract.Table._ID + " = " + uri.getLastPathSegment();
			}
			break;
		default:
			// unknown uri found
			Log.e(sTag, "unknown URI detected on query: " + uri.toString());
			throw new IllegalArgumentException("unknwon URI detected");
		}

		// get a connection to the database
		database = databaseHelper.getReadableDatabase();

		// return the results of the query
		return database.query(ReadingsContract.Table.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 */
	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {

		// define helper variables
		Uri mResultUri = null;
		String mTable = null;
		Uri mContentUri = null;

		// identify the appropriate uri
		switch(sUriMatcher.match(uri)) {
		case sReadingsListUri:
			mTable = ReadingsContract.Table.TABLE_NAME;
			mContentUri = ReadingsContract.CONTENT_URI;
			break;
		default:
			// unknown uri found
			Log.e(sTag, "unknown URI detected on insert: " + uri.toString());
			throw new IllegalArgumentException("unknwon URI detected");
		}

		// get a connection to the database
		database = databaseHelper.getWritableDatabase();

		long mId = database.insertOrThrow(mTable, null, values);

		// play nice and tidy up
		database.close();

		mResultUri = ContentUris.withAppendedId(mContentUri, mId);

		//notify any component interested about this change
		getContext().getContentResolver().notifyChange(mResultUri, null);

		return mResultUri;
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {

		// get a connection to the database
		database = databaseHelper.getWritableDatabase();
		int mCount;

		// determine what type of delete is required
		switch(sUriMatcher.match(uri)) {
		case sReadingsListUri:
			mCount = database.delete(ReadingsContract.Table.TABLE_NAME, selection, selectionArgs);
			break;
		case sReadingsItemUri:
			if(TextUtils.isEmpty(selection) == true) {
				selection = ReadingsContract.Table._ID + " = ?";
				selectionArgs = new String[0];
				selectionArgs[0] = uri.getLastPathSegment();
			}
			mCount = database.delete(ReadingsContract.Table.TABLE_NAME, selection, selectionArgs);
			break;
		default:
			// unknown uri found
			Log.e(sTag, "unknown URI detected on delete: " + uri.toString());
			throw new IllegalArgumentException("unknwon URI detected");
		}

		//notify any component interested about this change
		getContext().getContentResolver().notifyChange(uri, null);
		return mCount;
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public synchronized String getType(Uri uri) {

		// choose the mime type
		switch(sUriMatcher.match(uri)) {
		case sReadingsListUri:
			return ReadingsContract.CONTENT_TYPE_LIST;
		case sReadingsItemUri:
			return ReadingsContract.CONTENT_TYPE_ITEM;
		default:
			// unknown uri found
			Log.e(sTag, "unknown URI detected on get type: " + uri.toString());
			throw new IllegalArgumentException("unknwon URI detected");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("Method not implemented");
	}

}
