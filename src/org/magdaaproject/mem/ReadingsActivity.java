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
package org.magdaaproject.mem;

import org.magdaaproject.utils.TimeUtils;
import org.magdaaproject.utils.UnitConversionUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The readings activity for the MEM software, this is where the readings
 * and system status are displayed
 */
public class ReadingsActivity extends Activity implements OnClickListener {
	
	/*
	 * private class level constants
	 */
	//private static final String sTag = "ReadingsActivity";
	
	/*
	 * private class level variables
	 */
	private TextView sensorStatusView;
	private TextView temperatureValueView;
	private ImageView temperatureImgView;
	private TextView humidityValueView;
	private ImageView humidityImgView;
	private TextView readingTimeView;
	
	private Drawable coldTempDrawable = null;
	private Drawable warmTempDrawable = null;
	private Drawable hotTempDrawable = null;
	
	private Drawable humidity25Drawable = null;
	private Drawable humidity50Drawable = null;
	private Drawable humidity75Drawable = null;
	private Drawable humidity100Drawable = null;
	
	private int maxColdTemp;
	private int minHotTemp;
	
	private String celsiusFormat;
	private String fahrenheitFormat;
	private String kelvinFormat;
	private String readingTimeFormat;
	
	private String temperatureFormat;
	private String humidityFormat;
	
	private Intent coreServiceIntent = null;

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings);
        
        // find all of the necessary views
        sensorStatusView = (TextView) findViewById(R.id.readings_ui_lbl_sensor_status);
        temperatureValueView = (TextView) findViewById(R.id.readings_ui_lbl_temperature_value);
        temperatureImgView = (ImageView) findViewById(R.id.readings_ui_lbl_temperature_img);
        humidityValueView = (TextView) findViewById(R.id.readings_ui_lbl_humidity_value);
        humidityImgView = (ImageView) findViewById(R.id.readings_ui_lbl_humidity_img);
        readingTimeView = (TextView) findViewById(R.id.readings_ui_lbl_reading_time);
        
        // get formating strings
        celsiusFormat = getString(R.string.readings_ui_lbl_temperature_value_celsius);
        fahrenheitFormat = getString(R.string.readings_ui_lbl_temperature_value_fahrenheit);
        kelvinFormat = getString(R.string.readings_ui_lbl_temperature_value_kelvin);
        humidityFormat = getString(R.string.readings_ui_lbl_humidity_value);
        readingTimeFormat = getString(R.string.readings_ui_lbl_reading_time);
        
        
        // get the cold and hot temperature preferences
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
        try {
	        maxColdTemp = Integer.parseInt(mPreferences.getString(
	        		"preferences_display_max_cold_temp", 
	        		getString(R.string.preferences_display_max_cold_temp_default)));
        } catch (NumberFormatException e) {
        	maxColdTemp = Integer.parseInt(getString(R.string.preferences_display_max_cold_temp_default));
        }
        
        try {
	        minHotTemp = Integer.parseInt(mPreferences.getString(
	        		"preferences_display_min_hot_temp", 
	        		getString(R.string.preferences_display_min_hot_temp_default)));
        } catch (NumberFormatException e) {
        	minHotTemp = Integer.parseInt(getString(R.string.preferences_display_max_cold_temp_default));
        }
        
        // get the temperature format preference
        temperatureFormat = mPreferences.getString("preferences_display_temperature", "c");
        
        mPreferences = null;
        
        // populate the views with test data
        updateSensorStatus(true);
        
        updateTemperatureValue(21.2f);
        
        updateTemperatureImage(34);
        
        updateHumidityValue(47f);
        updateHumidityImage(47);
        
        updateReadingTime(System.currentTimeMillis());
        
        // start the core service
        coreServiceIntent = new Intent(this, org.magdaaproject.mem.services.CoreService.class);
        startService(coreServiceIntent);
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		
		// stop the service
		if(coreServiceIntent != null) {
			stopService(coreServiceIntent);
		}
		
		super.onDestroy();
	}
	
	/**
	 * a private method to update the sensor status label
	 * 
	 * @param isConnected true if the sensor is connected
	 */
	private void updateSensorStatus(boolean isConnected) {
		
		// get the parts of the text to use
		CharSequence mStartText = getText(R.string.readings_ui_lbl_sensor_status);
		SpannableString mConnectedValue;
		ForegroundColorSpan mColourSpan;
		
		// get the right words and colours for the connected status
		if(isConnected) {
			mConnectedValue = new SpannableString(getString(R.string.readings_ui_lbl_sensor_status_connected));
			mColourSpan = new ForegroundColorSpan(Color.GREEN);
		} else {
			mConnectedValue = new SpannableString(getString(R.string.readings_ui_lbl_sensor_status_disconnected));
			mColourSpan = new ForegroundColorSpan(Color.RED);
		}
		
		// apply the colour
		mConnectedValue.setSpan(mColourSpan, 0, mConnectedValue.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		
		// combine the two parts and set the text of the view
		sensorStatusView.setText(TextUtils.concat(mStartText, " ", mConnectedValue));
	}
	
	
	/**
	 * a private method to update the temperature value label
	 * @param temperature the recorded temperature
	 */
	private void updateTemperatureValue(float temperature) {
		
		String mValue = null;
		
		if(temperatureFormat.equals("c")) {
			mValue = String.format(celsiusFormat, temperature);
		} else if(temperatureFormat.equals("f")) {
			mValue = String.format(fahrenheitFormat, 
					UnitConversionUtils.comvertTemperature(temperature, UnitConversionUtils.CELSIUS, UnitConversionUtils.FAHRENHEIT));
		} else {
			mValue = String.format(kelvinFormat, 
					UnitConversionUtils.comvertTemperature(temperature, UnitConversionUtils.CELSIUS, UnitConversionUtils.KELVIN));
		}
		
        temperatureValueView.setText(mValue);
	}
	
	/**
	 * a private method to update the humidity value label
	 * @param humidity the recorded humidity
	 */
	private void updateHumidityValue(float humidity) {
		humidityValueView.setText(String.format(humidityFormat, humidity));
	}
	
	/**
	 * update the temperature image based on the recorded temperature
	 * @param temperature the recorded temperature
	 */
	private void updateTemperatureImage(float temperature) {
		
		//determine which drawable to use
		if(temperature < maxColdTemp) {
			if(coldTempDrawable == null) {
				coldTempDrawable = getResources().getDrawable(R.drawable.temperature_cold);
			}
			temperatureImgView.setImageDrawable(coldTempDrawable);
		} else if(temperature > minHotTemp) {
			if(hotTempDrawable == null) {
				hotTempDrawable = getResources().getDrawable(R.drawable.temperature_hot);
			}
			temperatureImgView.setImageDrawable(hotTempDrawable);
		} else {
			if(warmTempDrawable == null) {
				warmTempDrawable = getResources().getDrawable(R.drawable.temperature_warm);
			}
			temperatureImgView.setImageDrawable(warmTempDrawable);
		}
	}
	
	/**
	 * update the humidity image based on the recorded humidity
	 * @param humidity the recorded humidity
	 */
	private void updateHumidityImage(float humidity) {
		
		// determine which drawable to use
		if(humidity < 25) {
			if(humidity25Drawable == null) {
				humidity25Drawable = getResources().getDrawable(R.drawable.humidity_25);
			}
			humidityImgView.setImageDrawable(humidity25Drawable);
		} else if(humidity < 50) {
			if(humidity50Drawable == null) {
				humidity50Drawable = getResources().getDrawable(R.drawable.humidity_50);
			}
			humidityImgView.setImageDrawable(humidity50Drawable);
		} else if(humidity < 75) {
			if(humidity75Drawable == null) {
				humidity75Drawable = getResources().getDrawable(R.drawable.humidity_75);
			}
			humidityImgView.setImageDrawable(humidity75Drawable);
		} else {
			if(humidity100Drawable == null) {
				humidity100Drawable = getResources().getDrawable(R.drawable.humidity_100);
			}
			humidityImgView.setImageDrawable(humidity100Drawable);
		}
	}
	
	/**
	 * update the sensor reading time based on the recorded time
	 * @param time the time that the reading occurred
	 */
	private void updateReadingTime(long time) {
		readingTimeView.setText(String.format(readingTimeFormat, TimeUtils.formatTime(time)));
	}
}
