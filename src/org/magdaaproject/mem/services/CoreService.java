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

import org.magdaaproject.utils.SensorUtils;
import org.magdaaproject.utils.UnitConversionUtils;

import android.content.Intent;
import android.os.IBinder;
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
	 * private class level variables
	 */
	// logging variables
	private static final boolean sVerboseLog = true;
	private static final String sLogTag = "CoreService";
	
	// pins to listen on for input
	private static final int sTempInputPin = 43;
	private static final int sHumidityInputPin = 44;
	
	// sleep time between reading the input pins values
	private static final int sSleepTime = 30000;
	
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
		
		// return the start sticky flag
		return android.app.Service.START_STICKY;
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
			try {
				
				// get the temp and humidity voltages from the sensors
				mTempVoltage = tempInput.getVoltage();
				mHumidityVoltage = humidityInput.getVoltage();
				
				// convert the temperature sensor voltage
				mTemperature = SensorUtils.convertVoltageToTemp(mTempVoltage, SensorUtils.TMP36, UnitConversionUtils.CELSIUS);
				
				// convert the humidity sensor voltage
				mRelativeHumidity = SensorUtils.convertVoltageToRelativeHumidity(mHumidityVoltage, SensorUtils.HIH5031);
				
				// debug code
				Log.v(sLogTag, "temperature voltage: '" + mTempVoltage + "'");
				Log.v(sLogTag, "temperature celsius: '" + mTemperature + "'");
				Log.v(sLogTag, "humidity voltage: '" + mHumidityVoltage + "'");
				Log.v(sLogTag, "humidity value: '" + mRelativeHumidity + "'");
				
				// adjust the relative humidity value
				mRelativeHumidity = SensorUtils.adjustRelativeHumidity(mRelativeHumidity, mTemperature, SensorUtils.HIH5031);
				
				Log.v(sLogTag, "adjusted humidity: '" + mRelativeHumidity + "'");
				
				
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
