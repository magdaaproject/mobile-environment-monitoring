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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * main class for managing the creation of, and access to, the database
 */
public class MainDatabaseHelper extends SQLiteOpenHelper {

	/*
	 * public class level constants
	 */

	/**
	 * name of the actual database file
	 */
	public static final String DB_NAME = "magdaa-mem.db";

	/**
	 * current version of the database file
	 */
	public static final int DB_VERSION = 1;

	/*
	 * private class level constants
	 */
	private static final String sReadingsTableCreate = "CREATE TABLE " + 
			ReadingsContract.Table.TABLE_NAME + "( " +
			ReadingsContract.Table._ID + " INTEGER PRIMARY KEY, " +
			ReadingsContract.Table.TIMESTAMP + " INTEGER, " + 
			ReadingsContract.Table.TEMPERATURE + " REAL, " +
			ReadingsContract.Table.HUMIDITY + " REAL, " +
			ReadingsContract.Table.LATITUDE + " REAL, " +
			ReadingsContract.Table.LONGITUDE + " REAL, " + 
			ReadingsContract.Table.ALTITUDE + " REAL, " +
			ReadingsContract.Table.GPS_ACCURACY + " REAL)";
	
	private static final String sReadingsIndexCreate = "CREATE INDEX readings_timestamp_index ON " +
			ReadingsContract.Table.TABLE_NAME + "( " +
			ReadingsContract.Table.TIMESTAMP  + " DESC)";

	/**
	 * constructs a new MainDatabaseHelper object
	 * 
	 * @param context the context in which the database should be constructed
	 */
	MainDatabaseHelper(Context context) {
		// context, database name, factory, db version
		super(context, DB_NAME, null, DB_VERSION);
	}

	/*
	 * (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		// create the table and index
		db.execSQL(sReadingsTableCreate);
		db.execSQL(sReadingsIndexCreate);
	}

	/*
	 * (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// add code to update the database if necessary
	}
}
