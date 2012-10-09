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
package org.magdaaproject.mem.xforms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.magdaaproject.mem.R;
import org.magdaaproject.mem.provider.ReadingsContract;
import org.magdaaproject.utils.DeviceUtils;
import org.magdaaproject.utils.FileUtils;
import org.magdaaproject.utils.OpenDataKitUtils;
import org.magdaaproject.utils.serval.RhizomeUtils;
import org.magdaaproject.utils.xforms.XFormsException;
import org.magdaaproject.utils.xforms.XFormsUtils;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * receive the new reading broadcast and write an XForms instance file 
 */
public class InstanceWriter extends BroadcastReceiver {
	
	/*
	 * public class level constants
	 */
	public static final String INSTANCE_ID = "magdaa_mem_v1";
	
	/*
	 * private class level constants
	 */
	private static final boolean sVerboseLog = true;
	private static final String sLogTag = "InstanceWriter";

	/*
	 * (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		
		// check to make sure this is the right intent
		if(intent.getAction().equals(context.getString(R.string.system_broadcast_intent_new_reading_action)) == false) {
			// called with the wrong intent
			Log.w(sLogTag, "receiver was called with the wrong intent");
			return;
		}
		
		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "InstanceWriter called");
			Log.v(sLogTag, "new reading uri '" + intent.getDataString() + "'");
		}

		// get a content resolver
		ContentResolver mContentResolver = context.getContentResolver();

		// get the URI from the intent
		Uri mNewRecord = intent.getData();

		// get the data
		Cursor mCursor = mContentResolver.query(
				mNewRecord, 
				null, 
				null, 
				null, 
				null);

		if(mCursor != null && mCursor.getCount() > 0) {
			mCursor.moveToFirst();
			
			// build a hashmap of entries
			HashMap<String, String> mElements = new HashMap<String,String>();
			
			// add the elements
			mElements.put("DeviceId", DeviceUtils.getDeviceId(context));
			mElements.put("Temperature", Float.toString(mCursor.getFloat(mCursor.getColumnIndex(ReadingsContract.Table.TEMPERATURE))));
			mElements.put("Humidity", Float.toString(mCursor.getFloat(mCursor.getColumnIndex(ReadingsContract.Table.HUMIDITY))));
			
			// build the location string
			String mLocation = OpenDataKitUtils.getLocationString(
					mCursor.getString(mCursor.getColumnIndex(ReadingsContract.Table.LATITUDE)),
					mCursor.getString(mCursor.getColumnIndex(ReadingsContract.Table.LONGITUDE)),
					mCursor.getString(mCursor.getColumnIndex(ReadingsContract.Table.ALTITUDE)),
					mCursor.getString(mCursor.getColumnIndex(ReadingsContract.Table.GPS_ACCURACY)));
			
			mElements.put("Location", mLocation);
			
			// store the timestamp
			Long mTimeStamp = mCursor.getLong(mCursor.getColumnIndex(ReadingsContract.Table.TIMESTAMP));
			
			// play nice and tidy up
			mCursor.close();
			
			String mXmlString = null;
			String mFileName = null;
			String mResult = null;

			// build the xml and write it to the file
			try {
				XFormsUtils mXForm = new XFormsUtils();
				
				// set debug output option
				mXForm.setDebugOutput(sVerboseLog);
				
				mXmlString = mXForm.buildXFormsData(mElements, INSTANCE_ID);
				
				// build the file path
				String mOutputPath = Environment.getExternalStorageDirectory().getPath();
				mOutputPath += context.getString(R.string.system_file_path_odk_instances);
				
				// get the odk specific directory and file name
				mFileName = context.getString(R.string.system_xforms_form_name);
				mFileName += "_" + OpenDataKitUtils.getInstanceFileName(mTimeStamp);
				
				//finalise the output path
				if(mOutputPath.endsWith(File.separator) == false) {
					mOutputPath += File.separator + mFileName + ".xml";
				} else {
					mOutputPath += mFileName + ".xml";
				}
				
				// write the file
				mResult = FileUtils.writeNewFile(mXmlString, mFileName, mOutputPath);
				
				// output some debug logging
				if(sVerboseLog) {
					Log.v(sLogTag, "new file written: '" + mResult + "'");
				}
			} catch (XFormsException e) {
				Log.e(sLogTag, "an error occured while making the XForms XML", e);
			} catch (IOException e) {
				Log.e(sLogTag, "an error occrued while writing the XForms XML file", e);
			}
			
			// write a compressed version of the data
			if(mResult != null) {
				try {
					
					// reset the result
					mResult = null;
					
					// build the file path
					String mOutputPath = Environment.getExternalStorageDirectory().getPath();
					mOutputPath += context.getString(R.string.system_file_path_rhizome_data);
					
					//finalise the output path
					if(mOutputPath.endsWith(File.separator) == false) {
						mOutputPath += File.separator + mFileName + ".xml";
					} else {
						mOutputPath += mFileName + ".xml";
					}
					
					// write the file
					mResult = FileUtils.writeNewGzipFile(mXmlString, mFileName, mOutputPath);
					
					// output some debug logging
					if(sVerboseLog) {
						Log.v(sLogTag, "new gzip file written: '" + mResult + "'");
					}
				} catch (IOException e) {
					Log.e(sLogTag, "an error occrued while writing the compressed form of the XForms XML file", e);
				}
			}
			
			// share the file via rhizome
			if(mResult != null) {
				
				boolean mShareResult = false;
				
				try {
					mShareResult = RhizomeUtils.shareFile(context, mResult);
				} catch (IOException e) {
					Log.e(sLogTag, "unable to share the file via rhizome");
				}
				
				if(mShareResult == false) {
					Toast.makeText(
							context.getApplicationContext(), 
							String.format(context.getString(R.string.instance_writer_rhizome_share_failed), mResult), 
							Toast.LENGTH_LONG).show();
				}
				
			}
		} else {
			Log.w(sLogTag, "unable to retrieve reading data");
		}
	}
}
