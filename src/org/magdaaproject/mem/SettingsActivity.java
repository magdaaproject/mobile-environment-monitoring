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

import org.magdaaproject.utils.GeoCoordUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * the settings activity allows the user to change the various settings / preferences
 * related to the application
 *
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	/*
	 * private class level constants
	 */
	private static final boolean sVerboseLog = true;
	private static final String sTag = "SettingsActivity";

	private static final String sLocationInfo = "preferences_collection_location";
	private static final String sLocationInfoGps = "preferences_collection_location_gps";
	private static final String sLocationInfoManual = "preferences_collection_location_manual";

	private static final String sManualLatitude = "preferences_collection_location_manual_lat";
	private static final String sManualLongitude = "preferences_collection_location_manual_lng";

	private static final int sMissingCoordsDialog = 0;
	private static final int sGpsNotEnabledDialog = 1;
	private static final int sInvalidTempValueDialog = 2;
	
	private static final String sTemperatureUnits = "preferences_display_temperature";
	private static final String sMaxColdTemp = "preferences_display_max_cold_temp";
	private static final String sMinHotTemp = "preferences_display_min_hot_temp";
	private static final String sCollectionInterval = "preferences_collection_interval";

	/*
	 * (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// listen for changes to the preferences
		SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		
		// adjust the description of the list preferences
		adjustTemperatureDisplaySummary(mPreferences);
		adjustMaxColdTempSummary(mPreferences);
		adjustMinHotTempSummary(mPreferences);
		adjustCollectionIntervalSummary(mPreferences);
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sTag, "preference key: '" + key + "'");
		}
		
		/*
		 * adjust the summaries of some of the preferences
		 */
		if(key.equals(sTemperatureUnits)) {
			adjustTemperatureDisplaySummary(sharedPreferences);
			return;
		}
		
		if(key.equals(sMaxColdTemp)) {
			adjustMaxColdTempSummary(sharedPreferences);
			return;
		}
		
		if(key.equals(sMinHotTemp)) {
			adjustMinHotTempSummary(sharedPreferences);
			return;
		}
		
		if(key.equals(sCollectionInterval)) {
			adjustCollectionIntervalSummary(sharedPreferences);
			return;
		}

		/*
		 *  ensure the appropriate coordinate checkbox is ticked
		 */
		if(key.equals(sLocationInfoGps)) {

			Boolean mValue = sharedPreferences.getBoolean(key, false);

			// uncheck the manual location check box
			SharedPreferences.Editor mEditor = sharedPreferences.edit();
			mEditor.putBoolean(sLocationInfoManual, !mValue);
			mEditor.apply();

			CheckBoxPreference mPreference = (CheckBoxPreference) findPreference(sLocationInfoManual);
			mPreference.setChecked(!mValue);
			return;

		}

		if(key.equals(sLocationInfoManual)) {

			Boolean mValue = sharedPreferences.getBoolean(key, false);

			// uncheck the gps location check box
			SharedPreferences.Editor mEditor = sharedPreferences.edit();
			mEditor.putBoolean(sLocationInfoGps, !mValue);
			mEditor.apply();

			CheckBoxPreference mPreference = (CheckBoxPreference) findPreference(sLocationInfoGps);
			mPreference.setChecked(!mValue);
			return;
		}

		/*
		 * ensure the manual coordinates are valid
		 */
		if(key.equals(sManualLatitude)) {

			String mValue = sharedPreferences.getString(sManualLatitude, null);

			if(mValue != null && mValue.equals("") == false) {
				if(GeoCoordUtils.isValidLatitude(Float.parseFloat(mValue)) == false) {

					// remove the erroneous data
					SharedPreferences.Editor mEditor = sharedPreferences.edit();
					mEditor.putString(sManualLatitude, null);
					mEditor.apply();

					EditTextPreference mPreference = (EditTextPreference) findPreference(sManualLatitude);
					mPreference.setText("");

					// show a toast
					Toast.makeText(getApplicationContext(), R.string.preferences_collection_location_manual_invalid, Toast.LENGTH_LONG).show();

				}
			}
			
			return;
		}

		if(key.equals(sManualLongitude)) {

			String mValue = sharedPreferences.getString(sManualLongitude, null);

			if(mValue != null) {
				if(GeoCoordUtils.isValidLongitude(Float.parseFloat(mValue)) == false) {

					// remove the erroneous data
					SharedPreferences.Editor mEditor = sharedPreferences.edit();
					mEditor.putString(sManualLongitude, null);
					mEditor.apply();

					EditTextPreference mPreference = (EditTextPreference) findPreference(sManualLongitude);
					mPreference.setText("");

					// show a toast
					Toast.makeText(getApplicationContext(), R.string.preferences_collection_location_manual_invalid, Toast.LENGTH_LONG).show();

				}
			}
		}
		
		return;
	}
	
	/*
	 * private methods to adjust the summary of some of the preferences
	 */
	@SuppressWarnings("deprecation")
	private void adjustTemperatureDisplaySummary(SharedPreferences preferences) {
		String mTemperatureUnits = preferences.getString(sTemperatureUnits, getString(R.string.preferences_display_temperature_default));
		Preference mPreference = (Preference) findPreference(sTemperatureUnits);
		if(mTemperatureUnits.equals("c")) {
			mPreference.setSummary(String.format(getString(R.string.preferences_display_temperature_description), "¼C"));
		} else if(mTemperatureUnits.equals("f")) {
			mPreference.setSummary(String.format(getString(R.string.preferences_display_temperature_description), "¼F"));
		} else {
			mPreference.setSummary(String.format(getString(R.string.preferences_display_temperature_description), "¼K"));
		}
	}

	@SuppressWarnings("deprecation")
	private void adjustMaxColdTempSummary(SharedPreferences preferences) {
		String mValue = preferences.getString(sMaxColdTemp, getString(R.string.preferences_display_max_cold_temp_default));
		int mIntValue;
		
		// validate the value
		try {
			mIntValue = Integer.parseInt(mValue);
		} catch (NumberFormatException e) {
			mIntValue = -100;
		}
		
		if(mIntValue > -20 && mIntValue < 100) {
			Preference mPreference = (Preference) findPreference(sMaxColdTemp);
			mPreference.setSummary(String.format(getString(R.string.preferences_display_max_cold_temp_description), mValue));
		} else {
			// remove the erroneous data
			EditTextPreference mPreference = (EditTextPreference) findPreference(sMaxColdTemp);
			mPreference.setText(getString(R.string.preferences_display_max_cold_temp_default));
			
			SharedPreferences.Editor mEditor = preferences.edit();
			mEditor.putString(sMaxColdTemp, getString(R.string.preferences_display_max_cold_temp_default));
			mEditor.apply();
			
			showDialog(sInvalidTempValueDialog);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void adjustMinHotTempSummary(SharedPreferences preferences) {
		String mValue = preferences.getString(sMinHotTemp, getString(R.string.preferences_display_min_hot_temp_default));
		int mIntValue;
		
		// validate the value
		try {
			mIntValue = Integer.parseInt(mValue);
		} catch (NumberFormatException e) {
			mIntValue = -100;
		}
		
		if(mIntValue > 0 && mIntValue < 100) {
			Preference mPreference = (Preference) findPreference(sMinHotTemp);
			mPreference.setSummary(String.format(getString(R.string.preferences_display_min_hot_temp_description), mValue));
		} else {
			// remove the erroneous data
			EditTextPreference mPreference = (EditTextPreference) findPreference(sMinHotTemp);
			mPreference.setText(getString(R.string.preferences_display_min_hot_temp_default));
			
			SharedPreferences.Editor mEditor = preferences.edit();
			mEditor.putString(sMinHotTemp, getString(R.string.preferences_display_min_hot_temp_default));
			mEditor.apply();
			
			showDialog(sInvalidTempValueDialog);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void adjustCollectionIntervalSummary(SharedPreferences preferences) {
		String mValue = preferences.getString(sCollectionInterval, getString(R.string.preferences_collection_interval_default));
		long mLngValue = Long.parseLong(mValue) / 60000;
		Preference mPreference = (Preference) findPreference(sCollectionInterval);
		
		if(mLngValue < 1) {
			mPreference.setSummary(getString(R.string.preferences_collection_interval_summary_a));
		}else if(mLngValue == 1) {
			mPreference.setSummary(getString(R.string.preferences_collection_interval_summary_b));
		} else {
			mPreference.setSummary(String.format(getString(R.string.preferences_collection_interval_summary_c), mLngValue));
		}
	}

	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	@SuppressWarnings("deprecation")
	public void onBackPressed(){

		// validate some settings
		SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		if(mPreferences.getBoolean(sLocationInfo, false) == false) {
			// finish as normal
			finish();
		} else {

			// validate the manual location information if required
			if(mPreferences.getBoolean(sLocationInfoManual, false)) {
				// manual location is enabled
	
				String mLatitude = mPreferences.getString(sManualLatitude, null);
				String mLongitude = mPreferences.getString(sManualLongitude, null);
	
				if(mLatitude == null || mLongitude == null) {
					// show an error dialog
					showDialog(sMissingCoordsDialog);
				} else {
					// finish as normal
					finish();
				}
			} else if(mPreferences.getBoolean(sLocationInfoGps, false)) {
				// gps location information is enabled
	
				// output verbose debug log info
				if(sVerboseLog) {
					Log.v(sTag, "gps location information is enabled");
				}
	
				LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	
				boolean mEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	
				if(mEnabled == false) {
	
					if(sVerboseLog) {
						Log.v(sTag, "gps location service is not enabled");
					}
	
					showDialog(sGpsNotEnabledDialog);
				} else {
					// finish as normal
					finish();
				}
			}
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
		case sMissingCoordsDialog:
			mBuilder.setMessage(R.string.preferences_dialog_invalid_manual_location_coords_message)
			.setCancelable(false)
			.setTitle(R.string.preferences_dialog_invalid_manual_location_coords_title)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			return mBuilder.create();
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
		case sInvalidTempValueDialog:
			mBuilder.setMessage(R.string.preferences_dialog_invalid_temperature_message)
			.setCancelable(false)
			.setTitle(R.string.preferences_dialog_invalid_temperature_title)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			return mBuilder.create();
		default:
			return super.onCreateDialog(id);
		}
	}
}
