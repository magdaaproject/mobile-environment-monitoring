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

import org.magdaaproject.mem.provider.ReadingsContract;
import org.magdaaproject.mem.services.CoreService;
import org.magdaaproject.utils.serval.ServalStatusReceiver;
import org.magdaaproject.utils.TimeUtils;
import org.magdaaproject.utils.UnitConversionUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The readings activity for the MEM software, this is where the readings
 * and system status are displayed
 */
public class ReadingsActivity extends Activity implements OnClickListener {

	/*
	 * private class level constants
	 */
	private static final boolean sVerboseLog = true;
	private static final String sLogTag = "ReadingsActivity";
	private static final int sGpsNotEnabledDialog = 0;
	private static final int sSelectChartDialog = 1;

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

	private ServalStatusReceiver servalMeshStatusReceiver = null;

	private Button stopCollectionButton = null;
	private Button viewChartsButton = null;
	
	private ReadingsActivity parentActivity;
	
	private float previousTemperature;
	private float previousHumidity;
	private long  previousTimestamp = -1;

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_readings);
		
		// store a reference to this activity
		parentActivity = this;

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

		// ensure GPS is enabled if necessary
		if(mPreferences.getBoolean("preferences_collection_location", true)) {
			if(mPreferences.getBoolean("preferences_collection_location_gps", true)) {
				// check to ensure GPS is enabled
				LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

				boolean mEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

				if(mEnabled == false) {
					showDialog(sGpsNotEnabledDialog);
				}
			}
		}

		mPreferences = null;

		// start the core service
		coreServiceIntent = new Intent(this, org.magdaaproject.mem.services.CoreService.class);
		startService(coreServiceIntent);

		// setup the buttons
		stopCollectionButton = (Button) findViewById(R.id.readings_ui_btn_stop);
		stopCollectionButton.setOnClickListener(this);

		viewChartsButton = (Button) findViewById(R.id.readings_ui_btn_charts);
		viewChartsButton.setOnClickListener(this);

		// register the various broadcast receivers
		registerReceivers();

		// reset the UI
		resetUI();		
	}

	/*
	 * (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {

		// determine which view fired the event
		switch(v.getId()) {
		case R.id.readings_ui_btn_stop:
			if(CoreService.isRunning()) {
				// stop the service and finish
				if(coreServiceIntent != null) {
					stopService(coreServiceIntent);
				}
				finish();
			} else {
				// restart the service
				if(coreServiceIntent != null) {
					startService(coreServiceIntent);
				}

				// reset the UI
				resetUI();
			}
			break;
		case R.id.readings_ui_btn_charts:
			// show the chart functionality
			showDialog(sSelectChartDialog);
		default:
			Log.w(sLogTag, "an unknown view fired an onClick event");
		}

	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {

		// unregister the receivers
		unregisterReceivers();

		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {

		// unregister the receivers
		unregisterReceivers();

		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {

		// register the receivers
		registerReceivers();

		// reset the UI
		resetUI();

		super.onResume();
	}

	/*
	 * method to reset the UI on start or after a pause
	 */
	private void resetUI() {

		// use default place holder or last reading values
		if(previousTimestamp == -1) {
			// use placeholder
			String mValue = null;
	
			// reset the temperature UI component
			if(temperatureFormat.equals("c")) {
				mValue = getString(R.string.readings_ui_lbl_temperature_default_celsius);
			} else if(temperatureFormat.equals("f")) {
				mValue = getString(R.string.readings_ui_lbl_temperature_default_fahrenheit);
			} else {
				mValue = getString(R.string.readings_ui_lbl_temperature_default_kelvin);
			}
	
			temperatureValueView.setText(mValue);
	
			// reset the humidity ui component
			mValue = getString(R.string.readings_ui_lbl_humidity_default);
			humidityValueView.setText(mValue);
	
			// reset the time ui component
			readingTimeView.setText(R.string.readings_ui_lbl_reading_time_default);
		} else {
			// use previous values
			
			// update the temperature
			updateTemperatureValue(previousTemperature);
			updateTemperatureImage(previousTemperature);

			// update the humidity
			updateHumidityValue(previousHumidity);
			updateHumidityImage(previousHumidity);

			// update the reading time
			updateReadingTime(previousTimestamp);
		}

		//send off an intent to inquire about the status of the sensor
		Intent mIntent = new Intent(getString(R.string.system_broadcast_intent_sensor_status_inquiry_action));
		sendBroadcast(mIntent);

		// check to see if collection is running
		if(CoreService.isRunning() == false) {
			stopCollectionButton.setText(R.string.readings_ui_btn_stop_restart);
		} else {
			stopCollectionButton.setText(R.string.readings_ui_btn_stop);
		}
	}


	/*
	 * method to register the various broadcast receivers
	 */
	private void registerReceivers() {

		// register for readings updates
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(getString(R.string.system_broadcast_intent_new_reading_action));
		try {
			mIntentFilter.addDataType(ReadingsContract.CONTENT_TYPE_ITEM);
		} catch (MalformedMimeTypeException e) {
			Log.e(sLogTag, "unable to set type for new reading receiver", e);
		}

		registerReceiver(newReadingsReceiver, mIntentFilter);

		// register for sensor status updates
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(getString(R.string.system_broadcast_intent_sensor_status_action));

		registerReceiver(sensorStatusReceiver, mIntentFilter);

		// register for changes in the status of Serval Mesh
		mIntentFilter = new IntentFilter();

		for(int i = 0; i < ServalStatusReceiver.SERVAL_STATUS_ACTIONS.length; i++) {
			mIntentFilter.addAction(ServalStatusReceiver.SERVAL_STATUS_ACTIONS[i]);
		}

		if(servalMeshStatusReceiver == null) {
			servalMeshStatusReceiver = new ServalStatusReceiver();
		}

		registerReceiver(servalMeshStatusReceiver, mIntentFilter);

		// send off an intent to poll for the current state of the serval mesh
		Intent mIntent = new Intent(ServalStatusReceiver.SERVAL_STATE_CHECK_ACTION);
		startService(mIntent);

		//send off an intent to inquire about the status of the sensor
		mIntent = new Intent(getString(R.string.system_broadcast_intent_sensor_status_inquiry_action));
		sendBroadcast(mIntent);
	}

	/*
	 * method to unregister the various broadcast receivers
	 */
	private void unregisterReceivers() {

		/*
		 * workaround for a crash bug, no idea why I can't unregister what is
		 * clearly a registered receiver as it responds to intents
		 */
		try {
			unregisterReceiver(newReadingsReceiver);
			unregisterReceiver(sensorStatusReceiver);
			unregisterReceiver(servalMeshStatusReceiver);
		} catch (IllegalArgumentException e) {
			Log.w(sLogTag, "IllegalArgumentException thrown when unregistering receivers");
		}
	}

	/*
	 * callback method used to construct the required dialog
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {

		AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);

		// determine which dialog to show
		switch(id) {
		case sGpsNotEnabledDialog:
			mBuilder.setMessage(R.string.preferences_dialog_invalid_gps_status_message)
			.setCancelable(false)
			.setTitle(R.string.preferences_dialog_invalid_gps_status_title)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Intent mIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(mIntent);
				}
			})
			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			return mBuilder.create();
		case sSelectChartDialog:
			mBuilder.setTitle(R.string.readings_ui_dialog_charts_title)
			.setItems(R.array.readings_ui_dialog_charts_list, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// show one of the chart activities
					Intent mIntent;
					
					// determine which chart activity to show
					switch(which) {
					case 0: // temperature chart
						mIntent = new Intent(parentActivity, org.magdaaproject.mem.charts.TemperatureActivity.class);
						break;
					case 1: // humidity chart
						mIntent = new Intent(parentActivity, org.magdaaproject.mem.charts.HumidityActivity.class);
						break;
					default:
						Log.e(sLogTag, "unknown chart selected");
						return;
					}
					startActivity(mIntent);
				}
			});
			return mBuilder.create();
		default:
			return super.onCreateDialog(id);
		}
	}


	/**
	 * a broadcast receiver used to receive notifications of new readings
	 */
	private BroadcastReceiver newReadingsReceiver = new BroadcastReceiver() {

		/*
		 * private inner class level variables
		 */
		private String[] projection = null; 
		private ContentResolver contentResolver = null;

		private volatile String lastRecordId = "";

		@Override
		public void onReceive(Context context, Intent intent) {

			// get the URI from the intent
			Uri mNewRecord = intent.getData();

			// filter out multiple calls with the same id
			// workaround, no idea what is causing the issue
			if(lastRecordId.equals(mNewRecord.getLastPathSegment())) {
				// return, no need to continue
				return;
			}

			// store this record id to filter out duplicate calls
			lastRecordId = mNewRecord.getLastPathSegment();

			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "newReadingsReceiver called");
				Log.v(sLogTag, "new reading uri '" + intent.getDataString() + "'");
			}

			// define the projection
			if(projection == null) {
				projection = new String[3];
				projection[0] = ReadingsContract.Table.TEMPERATURE;
				projection[1] = ReadingsContract.Table.HUMIDITY;
				projection[2] = ReadingsContract.Table.TIMESTAMP;
			}

			// get a content resolver if required
			if(contentResolver == null) {
				contentResolver = context.getContentResolver();
			}

			// get the data
			Cursor mCursor = contentResolver.query(
					mNewRecord, 
					projection, 
					null, 
					null, 
					null);

			if(mCursor != null && mCursor.getCount() > 0) {
				mCursor.moveToFirst();

				// update the temperature
				updateTemperatureValue(mCursor.getFloat(mCursor.getColumnIndex(projection[0])));
				updateTemperatureImage(mCursor.getFloat(mCursor.getColumnIndex(projection[0])));
				previousTemperature = mCursor.getFloat(mCursor.getColumnIndex(projection[0]));

				// update the humidity
				updateHumidityValue(mCursor.getFloat(mCursor.getColumnIndex(projection[1])));
				updateHumidityImage(mCursor.getFloat(mCursor.getColumnIndex(projection[1])));
				previousHumidity = mCursor.getFloat(mCursor.getColumnIndex(projection[1]));

				// update the reading time
				updateReadingTime(mCursor.getLong(mCursor.getColumnIndex(projection[2])));
				previousTimestamp = mCursor.getLong(mCursor.getColumnIndex(projection[2]));

				mCursor.close();
			}

			// check on the status of the serval mesh
			if(servalMeshStatusReceiver.getStatus() != ServalStatusReceiver.SERVAL_STATUS_ON) {
				Toast.makeText(getApplicationContext(), R.string.readings_ui_toast_serval_not_running, Toast.LENGTH_LONG).show();
			}
		}
	};

	/**
	 * a broadcast receiver used to receive notifications of new readings
	 */
	private BroadcastReceiver sensorStatusReceiver = new BroadcastReceiver() {

		/*
		 * (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {

			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "sensorStatusReceiver called");
			}

			// update the sensor status
			updateSensorStatus(intent.getBooleanExtra("connected", false));
			
			// ensure the stop service button has the appropriate text
			if(stopCollectionButton.getText().equals(getString(R.string.readings_ui_btn_stop)) == false ) {
				stopCollectionButton.setText(R.string.readings_ui_btn_stop);
			}
		}

	};

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
