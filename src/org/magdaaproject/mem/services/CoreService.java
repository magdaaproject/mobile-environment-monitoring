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
package org.magdaaproject.mem.services;

import java.io.IOException;

import org.magdaaproject.mem.R;
import org.magdaaproject.mem.provider.ReadingsContract;
import org.magdaaproject.utils.SensorUtils;
import org.magdaaproject.utils.UnitConversionUtils;
import org.magdaaproject.utils.readings.ReadingsList;
import org.magdaaproject.utils.readings.TempHumidityReading;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

/**
 * 
 * the CoreService class controls access to the IOIO hardware, retrieving new sensor values, 
 * storing new values in the database and sending the relevant broadcasts
 */
public class CoreService extends IOIOService {
	
	/*
	 * private class level constants
	 */
	// logging variables
	private static final boolean sVerboseLog = true;
	private static final String sLogTag = "CoreService";
	
	// pins to listen on for input
	private static final int sTempInputPin = 43;
	private static final int sHumidityInputPin = 44;
	
	// sleep time between reading the input pins values
	private static final int sSleepTime = 1000;
	
	// identification for the notification
	private static final int sNotificationId = 1;
	
	/*
	 * private class level variables
	 */
	private ReadingsList listOfReadings;
	private long readingInterval;
	private long nextReadingTime = System.currentTimeMillis() + readingInterval;
	
	private boolean collectLocationInfo = false;
	private boolean collectGpsLocationInfo = false;
	private boolean collectManualLocationInfo = false;
	
	private float manualLatitude;
	private float manualLongitude;
	
	/*
	 * (non-Javadoc)
	 * @see ioio.lib.util.android.IOIOService#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		
		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "service onCreate() called");
		}
		
		// initialise variables
		listOfReadings = new ReadingsList();
		
		// get the required preferences
		SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		// get the reading interval
		try {
			readingInterval = Long.parseLong(
					mPreferences.getString("preferences_collection_interval", getString(R.string.preferences_collection_interval_default)));
		} catch (NumberFormatException e) {
			readingInterval = Long.parseLong(getString(R.string.preferences_collection_interval_default));
		}
		
		// determine if we need to collect location information
		collectLocationInfo = mPreferences.getBoolean("preferences_collection_location", true);
		
		if(collectLocationInfo) {
			
			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "need to collect location information");
			}
			
			// use GPS for location info?
			collectGpsLocationInfo = mPreferences.getBoolean("preferences_collection_location_gps", true);
			
			if(collectGpsLocationInfo) {
				// output verbose debug log info
				if(sVerboseLog) {
					Log.v(sLogTag, "need to collect location information from GPS");
				}
				
				//TODO initiate the collection of location information
				
			} else {
				
				// use manual location info?
				collectManualLocationInfo = mPreferences.getBoolean("preferences_collection_location_manual", false);
				
				if(collectManualLocationInfo) {
					
					// output verbose debug log info
					if(sVerboseLog) {
						Log.v(sLogTag, "need to collect location information from manually entered valuse");
					}
				
					// get the manually entered coordinates
					manualLatitude = Float.parseFloat(mPreferences.getString("preferences_collection_location_manual_lat", "0"));
					manualLongitude = Float.parseFloat(mPreferences.getString("preferences_collection_location_manual_lng", "0"));
					
					// output verbose debug log info
					if(sVerboseLog) {
						Log.v(sLogTag, "manual latitude: '" + manualLatitude + "'");
						Log.v(sLogTag, "manual longitude: '" + manualLongitude + "'");
					}
					
				}
			}
		} else {
			
			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "do not need to collect location information");
			}
			
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		super.onStartCommand(intent,  flags, startId);
		
		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "service onStartCommand() called");
		}
		
		// add the notification 
		addNotification();
		
		// return the start sticky flag
		return android.app.Service.START_STICKY;
	}
	
	
	/*
	 * private method to add a notification icon
	 */
	@SuppressWarnings("deprecation")
	private void addNotification() {
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		int mNotificationIcon = R.drawable.ic_stat_magdaa;
		CharSequence mTickerText = getString(R.string.system_notification_ticker_text);
		long mWhen = System.currentTimeMillis();

		// create the notification and set the flag so that it stays up
		Notification mNotification = new Notification(mNotificationIcon, mTickerText, mWhen);
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

		// get the content of the notification
		CharSequence mNotificationTitle = getString(R.string.system_notification_title);
		CharSequence mNotificationContent = getString(R.string.system_notification_content);

		// create the intent for the notification
		// set flags so that the user returns to this activity and not a new one
		Intent mNotificationIntent = new Intent(this, org.magdaaproject.mem.ReadingsActivity.class);
		mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		// create a pending intent so that the system can use the above intent at a later time.
		PendingIntent mPendingIntent = PendingIntent.getActivity(this, 0, mNotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		// complete the setup of the notification
		mNotification.setLatestEventInfo(getApplicationContext(), mNotificationTitle, mNotificationContent, mPendingIntent);

		// add the notification
		mNotificationManager.notify(sNotificationId, mNotification);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see ioio.lib.util.android.IOIOService#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "service onDestroy() called");
		}
		
		// clear the notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(sNotificationId);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		// return null as we don't expect anyone will bind to this service
		return null;
	}
	
	/*
	 * ioio related code
	 */
	
	/*
	 * a private looper inner class for responding to button events
	 */
	private class Looper extends BaseIOIOLooper {
		
		/*
		 * define class level variables
		 */
		private AnalogInput tempInput;
		private AnalogInput humidityInput;
		
		/*
		 * (non-Javadoc)
		 * @see ioio.lib.util.BaseIOIOLooper#setup()
		 */
		@Override
        protected void setup() throws ConnectionLostException {
			
			try {
				tempInput = ioio_.openAnalogInput(sTempInputPin);
				humidityInput = ioio_.openAnalogInput(sHumidityInputPin);
			} catch (ConnectionLostException e) {
				Log.e(sLogTag, "connection to ioio lost during setup", e);
				throw e;
			}
			
			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "ioio looper setup() called");
			}
		}
		
		/*
		 * (non-Javadoc)
		 * @see ioio.lib.util.BaseIOIOLooper#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException {
			
			float mTempVoltage;
			float mHumidityVoltage;
			
			float mTemperature;
			float mRelativeHumidity;
			
			TempHumidityReading mSensorReading;
			
			try {
				
				// get the temp and humidity voltages from the sensors
				mTempVoltage = tempInput.getVoltage();
				mHumidityVoltage = humidityInput.getVoltage();
				
				// convert the temperature sensor voltage
				mTemperature = SensorUtils.convertVoltageToTemp(mTempVoltage, SensorUtils.TMP36, UnitConversionUtils.CELSIUS);
				
				// convert the humidity sensor voltage
				mRelativeHumidity = SensorUtils.convertVoltageToRelativeHumidity(mHumidityVoltage, SensorUtils.HIH5031);
				
				// adjust the relative humidity value
				mRelativeHumidity = SensorUtils.adjustRelativeHumidity(mRelativeHumidity, mTemperature, SensorUtils.HIH5031);
				
				// add the readings to the list
				mSensorReading = new TempHumidityReading(mTemperature, mRelativeHumidity);
				listOfReadings.add(mSensorReading);
				
				// debug code
				if(sVerboseLog) {
					Log.v(sLogTag, "temperature voltage: '" + mTempVoltage + "'");
					Log.v(sLogTag, "temperature celsius: '" + mTemperature + "'");
					Log.v(sLogTag, "humidity voltage: '" + mHumidityVoltage + "'");
					Log.v(sLogTag, "humidity value: '" + mRelativeHumidity + "'");
					Log.v(sLogTag, "adjusted humidity: '" + mRelativeHumidity + "'");
					Log.v(sLogTag, "sensor readings list size: '" + listOfReadings.size() + "'");
					Log.v(sLogTag, "next reading time: '" + nextReadingTime + "'");
					Log.v(sLogTag, "current reading time: '" + System.currentTimeMillis() + "'");
				}
				
				// determine if it is time to save a reading
				if(nextReadingTime <= (System.currentTimeMillis() - readingInterval)) {
					
					if(sVerboseLog) {
						Log.v(sLogTag, "need to save a reading");
					}
					
					// calculate the average temperature and humidity
					float mAvgTemp = 0;
					float mAvgHumidity = 0;
					
					// add up the readings
					// TODO take into account the age of the readings?
					for(int i = 0; i < listOfReadings.size(); i++) {
						mSensorReading = (TempHumidityReading) listOfReadings.get(i);
						
						mAvgTemp += mSensorReading.getTemp();
						mAvgHumidity += mSensorReading.getHumidity();
					}
					
					// divide by the number of readings
					mAvgTemp     = mAvgTemp / listOfReadings.size();
					mAvgHumidity = mAvgHumidity / listOfReadings.size();
					
					if(sVerboseLog) {
						Log.v(sLogTag, "average temperature: '" + mAvgTemp + "'");
						Log.v(sLogTag, "average humidity: '" + mAvgHumidity + "'");
					}
					
					// round to a single decimal point precision
					mAvgTemp = Math.round(mAvgTemp * 10) / 10;
					mAvgHumidity = Math.round(mAvgHumidity * 10) / 10;
					
					if(sVerboseLog) {
						Log.v(sLogTag, "rounded average temperature: '" + mAvgTemp + "'");
						Log.v(sLogTag, "rounded average humidity: '" + mAvgHumidity + "'");
					}
					
					// add additional debug output if necessary
					if(sVerboseLog) {
						String mOutputPath = Environment.getExternalStorageDirectory().getPath();
						mOutputPath += getString(R.string.system_file_Path_debug_output);
						
						try {
							Log.v(sLogTag, "list debug file: " + listOfReadings.dumpData(mOutputPath));
						} catch (IOException e) {
							Log.v(sLogTag, "unable to write list debug file", e);
						}
					}
					
					// write a new sensor reading entry
					ContentValues mValues = new ContentValues();
					
					// populate the list of new values
					mValues.put(ReadingsContract.Table.TIMESTAMP, System.currentTimeMillis());
					mValues.put(ReadingsContract.Table.TEMPERATURE, mAvgTemp);
					mValues.put(ReadingsContract.Table.HUMIDITY, mAvgHumidity);
					
					// add the values to the database
					try {
						Uri newRecord = getContentResolver().insert(ReadingsContract.CONTENT_URI, mValues);
						
						if(sVerboseLog) {
							Log.v(sLogTag, String.format("new database entry %s: %s, %s", newRecord.getLastPathSegment(), mAvgTemp, mAvgHumidity));
						}
					} catch (SQLException e) {
						Log.e(sLogTag, "unable to write new sensor reading to the database", e);
					}
					
					// increment the reading interval
					nextReadingTime = System.currentTimeMillis() + readingInterval;
				}
				
				// sleep for the desired amount of time
				Thread.sleep(sSleepTime);
				
			} catch (InterruptedException e) {
				Log.e(sLogTag, "inerrupted exception thrown while reading values", e);
				ioio_.disconnect();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see ioio.lib.util.android.IOIOService#createIOIOLooper()
	 */
	@Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }
}
