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
package org.magdaaproject.mem.charts;

import java.util.Arrays;

import org.magdaaproject.mem.R;
import org.magdaaproject.mem.provider.ReadingsContract;
import org.magdaaproject.utils.ChartUtils;
import org.magdaaproject.utils.TimeUtils;

import com.androidplot.series.XYSeries;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * The temperature chart activity for the MEM software, this is where the
 * temperature chart is displayed
 */
public class TemperatureActivity extends Activity {
	
	/*
	 * private class level constants
	 */
//	private static final boolean sVerboseLog = false;
//	private static final String sLogTag = "TemperatureActivity";
	
	private static final String sTemperatureUnits = "preferences_display_temperature";
	
	private static int sDefaultRangeBuffer = 5;
	
	/*
	 * private class level variables
	 */
	private XYPlot temperatureChart;
	
	private String temperatureScale;
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chart_temperature);
		
		// get a reference to the chart
		temperatureChart = (XYPlot) findViewById(R.id.chart_temperature_ui_chart);
		
		// get the temperature display preference
		SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		temperatureScale = mPreferences.getString(sTemperatureUnits, getString(R.string.preferences_display_temperature_default));
		temperatureScale = temperatureScale.toUpperCase();
		
		// get the data
		XYSeries mSeries = getData();
		
		// deal with the situation where no data is available
		if(mSeries == null) {
			Toast.makeText(getApplicationContext(), R.string.chart_ui_toast_no_data, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// get the boundaries for the range values
		Float[] mRange = ChartUtils.getRangeBoundaries(mSeries);
		
		// format the chart
		temperatureChart = ChartUtils.formatChart(temperatureChart);
		
		// get the line style
		LineAndPointFormatter mLineStyle = ChartUtils.getLineStyle();
		
		// add the data
		temperatureChart.addSeries(mSeries, mLineStyle);
		
		// setup the timestamp tick marks
		temperatureChart.setDomainStep(XYStepMode.SUBDIVIDE, mSeries.size());
		
		// customise the chart labels
		temperatureChart.setTitle(getString(R.string.chart_temperature_ui_chart_title));
		temperatureChart.setDomainLabel(getString(R.string.chart_temperature_ui_chart_domain_label));
		temperatureChart.setRangeLabel("¼" + temperatureScale);
		temperatureChart.setDomainValueFormat(ChartUtils.getBlankDomainFormatter());
		
		temperatureChart.setRangeLowerBoundary(mRange[0] - sDefaultRangeBuffer, BoundaryMode.FIXED);
		temperatureChart.setRangeUpperBoundary(mRange[1] + sDefaultRangeBuffer, BoundaryMode.FIXED);
		
		// TODO hide developer helper markup
		temperatureChart.disableAllMarkup();
		
	}
	
	/*
	 * private method to get the required data
	 */
	private SimpleXYSeries getData() {
		
		/*
		 *  get content related variables
		 */
		ContentResolver mContentResolver = getContentResolver();
		
		// get only the required fields
		String[] mProjection = new String[2];
		mProjection[0] = ReadingsContract.Table.TIMESTAMP;
		mProjection[1] = ReadingsContract.Table.TEMPERATURE;
		
		String mSelection = ReadingsContract.Table.TIMESTAMP + " > ?";
		
		String[] mSelectionArgs = new String[1];
		mSelectionArgs[0] = Long.toString(System.currentTimeMillis() - TimeUtils.ONE_HOUR_IN_MILLISECONDS);
		
		String mSortOrder = ReadingsContract.Table.TIMESTAMP + " ASC"; 
		
		// get the data
		Cursor mCursor = mContentResolver.query(
				ReadingsContract.CONTENT_URI,
				mProjection, 
				mSelection, mSelectionArgs, mSortOrder);
		
		// check on what was returned
		if(mCursor == null || mCursor.getCount() == 0) {
			if(mCursor != null) {
				mCursor.close();
			}
			return null;
		}
		
		// build a list of data points
		Number[] mTemperatures = new Number[mCursor.getCount()];
		Number[] mTimestamps = new Number[mCursor.getCount()];
		
		int mFakeTimestamp = 0;
		
		while(mCursor.moveToNext()) {
			mTemperatures[mCursor.getPosition()] = mCursor.getFloat(mCursor.getColumnIndex(ReadingsContract.Table.TEMPERATURE));
			//mTimestamps[mCursor.getPosition()] = mCursor.getLong(mCursor.getColumnIndex(ReadingsContract.Table.TIMESTAMP)) / sTimeDivisor;
			mTimestamps[mCursor.getPosition()] = mFakeTimestamp++;
		}
		
		// play nice and tidy up
		mCursor.close();
		
		// return the series
		return new SimpleXYSeries(
				Arrays.asList(mTimestamps),
				Arrays.asList(mTemperatures),
				String.format(getString(R.string.chart_temperature_ui_chart_series_title), temperatureScale));
	}
}
