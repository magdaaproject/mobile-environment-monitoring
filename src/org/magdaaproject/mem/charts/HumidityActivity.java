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
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

/**
 * The humidity chart activity for the MEM software, this is where the
 * humidity chart is displayed
 */
public class HumidityActivity extends Activity {
	
	/*
	 * private class level constants
	 */
//	private static final boolean sVerboseLog = false;
//	private static final String sLogTag = "TemperatureActivity";
	
	private static int sDefaultRangeBuffer = 5;
	
	/*
	 * private class level variables
	 */
	private XYPlot humidityChart;
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chart_humidity);
		
		// get a reference to the chart
		humidityChart = (XYPlot) findViewById(R.id.chart_humidity_ui_chart);
		
		// get the data
		XYSeries mSeries = getData();
		
		// deal with the situation where no data is available
		if(mSeries == null) {
			Toast.makeText(getApplicationContext(), R.string.chart_ui_toast_no_data, Toast.LENGTH_LONG).show();
			finish();
		}
		
		// get the boundaries for the range values
		Float[] mRange = ChartUtils.getRangeBoundaries(mSeries);
		
		// format the chart
		humidityChart = ChartUtils.formatChart(humidityChart);
		
		// get the line style
		LineAndPointFormatter mLineStyle = ChartUtils.getLineStyle();
		
		// add the data
		humidityChart.addSeries(mSeries, mLineStyle);
		
		// setup the timestamp tick marks
		humidityChart.setDomainStep(XYStepMode.SUBDIVIDE, mSeries.size());
		
		// customise the chart labels
		humidityChart.setTitle(getString(R.string.chart_humidity_ui_chart_title));
		humidityChart.setDomainLabel(getString(R.string.chart_humidity_ui_chart_domain_label));
		humidityChart.setRangeLabel(getString(R.string.chart_humidity_ui_chart_series_label));
		humidityChart.setDomainValueFormat(ChartUtils.getBlankDomainFormatter());
		
		humidityChart.setRangeLowerBoundary(mRange[0] - sDefaultRangeBuffer, BoundaryMode.FIXED);
		humidityChart.setRangeUpperBoundary(mRange[1] + sDefaultRangeBuffer, BoundaryMode.FIXED);
		
		// TODO hide developer helper markup
		humidityChart.disableAllMarkup();
		
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
		mProjection[1] = ReadingsContract.Table.HUMIDITY;
		
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
		Number[] mSeries = new Number[mCursor.getCount()];
		Number[] mDomains = new Number[mCursor.getCount()];
		
		int mFakeTimestamp = 0;
		
		while(mCursor.moveToNext()) {
			mSeries[mCursor.getPosition()] = mCursor.getFloat(mCursor.getColumnIndex(ReadingsContract.Table.HUMIDITY));
			//mTimestamps[mCursor.getPosition()] = mCursor.getLong(mCursor.getColumnIndex(ReadingsContract.Table.TIMESTAMP)) / sTimeDivisor;
			mDomains[mCursor.getPosition()] = mFakeTimestamp++;
		}
		
		// play nice and tidy up
		mCursor.close();
		
		// return the series
		return new SimpleXYSeries(
				Arrays.asList(mDomains),
				Arrays.asList(mSeries),
				getString(R.string.chart_humidity_ui_chart_series_label));
	}
}
