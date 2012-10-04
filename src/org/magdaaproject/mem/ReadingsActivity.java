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

import android.app.Activity;
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
	
	private Drawable coldTempDrawable = null;
	private Drawable warmTempDrawable = null;
	private Drawable hotTempDrawable = null;
	
	private int maxColdTemp;
	private int minHotTemp;

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings);
        
        // find all of the necessary text
        sensorStatusView = (TextView) findViewById(R.id.readings_ui_lbl_sensor_status);
        temperatureValueView = (TextView) findViewById(R.id.readings_ui_lbl_temperature_value);
        temperatureImgView = (ImageView) findViewById(R.id.readings_ui_lbl_temperature_img);
        
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
        
        mPreferences = null;
        
        // populate the views with test data
        updateSensorStatus(true);
        
        temperatureValueView.setText("21¼C");
        
        updateTemperatureImage(34);
        // TODO populate with live data
        
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

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
	 * update the temperature image based on the recorded temperature
	 * @param temperature the temperature to the nearest degree
	 */
	private void updateTemperatureImage(int temperature) {
		
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
}
