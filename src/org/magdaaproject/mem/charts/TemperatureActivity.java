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

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

import org.magdaaproject.mem.R;
import org.magdaaproject.mem.provider.ReadingsContract;

import android.graphics.Shader;
import com.androidplot.Plot;
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
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The temperature chart activity for the MEM software, this is where the
 * temperature chart is displayed
 */
public class TemperatureActivity extends Activity {
	
	/*
	 * private class level constants
	 */
	private static final boolean sVerboseLog = false;
	private static final String sLogTag = "TemperatureActivity";

	// debug code
	//private static final long sOneHour = 3600000 * 4;

	private static final long sOneHour = 3600000;

	//	private static final long sTimeDivisor = 1000;
	
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
		
		// get the boundaries for the range values
		Float[] mRange = getRangeBoundaries(mSeries);
		
		// format the chart
		formatChart(temperatureChart);
		
		// get the line style
		LineAndPointFormatter mLineStyle = getLineStyle();
		
		// add the data
		temperatureChart.addSeries(mSeries, mLineStyle);
		
		// setup the timestamp tick marks
		temperatureChart.setDomainStep(XYStepMode.SUBDIVIDE, mSeries.size());
		
		// customise the chart labels
		temperatureChart.setTitle(getString(R.string.chart_temperature_ui_chart_title));
		temperatureChart.setDomainLabel(getString(R.string.chart_temperature_ui_chart_domain_label));
		temperatureChart.setRangeLabel("¼" + temperatureScale);
		temperatureChart.setDomainValueFormat(getDomainValueFormat());
		
		temperatureChart.setRangeLowerBoundary(mRange[0] - sDefaultRangeBuffer, BoundaryMode.FIXED);
		temperatureChart.setRangeUpperBoundary(mRange[1] + sDefaultRangeBuffer, BoundaryMode.FIXED);
		
		// debug messages 
		if(sVerboseLog) {
			Log.v(sLogTag, "RangeMin '" + mRange[0] + "'");
			Log.v(sLogTag, "RnageMax '" + mRange[1] + "'");
		}
		
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
		mSelectionArgs[0] = Long.toString(System.currentTimeMillis() - sOneHour);
		
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
	
	// private method to format the chart
	// TODO break this into the magdaa library for consistent look and feel of charts
	private void formatChart(XYPlot xyPlot) {
		xyPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
		xyPlot.getGraphWidget().getGridLinePaint().setColor(Color.BLACK);
		xyPlot.getGraphWidget().getGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1,1}, 1));
		xyPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
		xyPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
 
		xyPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
		xyPlot.getBorderPaint().setStrokeWidth(1);
		xyPlot.getBorderPaint().setAntiAlias(false);
		xyPlot.getBorderPaint().setColor(Color.WHITE);
	}
	
	// private method to return a line style
	// TODO break this into the magdaa library for consistent look and feel of charts
	private LineAndPointFormatter getLineStyle() {
		// Create a formatter to use for drawing a series using LineAndPointRenderer:
		LineAndPointFormatter mFormatter = new LineAndPointFormatter(Color.BLUE, Color.CYAN, Color.BLUE);
		
		// create a better fill paint
		Paint mLineFill = new Paint();
        mLineFill.setAlpha(200);
        mLineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.BLUE, Shader.TileMode.MIRROR));
        
        mFormatter.setFillPaint(mLineFill);
        
        return mFormatter;
	}
	
	// private method to return a formatter for the time labels
	// TODO break this into the magdaa library for consistent look and feel of charts
	private Format getDomainValueFormat() {
		
		Format mFormat = new Format() {
	
			private static final long serialVersionUID = -6090275787464042111L;
	
			// private class level variables
			//private SimpleDateFormat dateFormat = new SimpleDateFormat(TimeUtils.DEFAULT_SHORT_TIME_FORMAT);

			/*
			 * (non-Javadoc)
			 * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
			 */
			@Override
			public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
				
				// adjust the times
//				long timestamp = ((Number) object).longValue() * sTimeDivisor;
//                Date date = new Date(timestamp);
//                //return dateFormat.format(date, buffer, field);
                // return an empty string buffer to disable the display of domain labels
                return new StringBuffer();

			}

			/*
			 * (non-Javadoc)
			 * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
			 */
			@Override
			public Object parseObject(String string, ParsePosition position) {
				return null;
			}
		};
		
		return mFormat;
	}
	
	// get range boundaries for the y axis
	private Float[] getRangeBoundaries(XYSeries series) {
		
		Float[] mRange = new Float[2];
		
		mRange[1] = Float.MIN_NORMAL;
		mRange[0] = Float.MAX_VALUE;
		
		float mYValue;
		
		for(int i = 0; i < series.size(); i++) {
			
			mYValue = (Float) series.getY(i);
			
			if(mYValue < mRange[0]) {
				mRange[0] = mYValue;
			}
			
			if(mYValue > mRange[1]) {
				mRange[1] = mYValue;
			}
		}
		
		return mRange;
	}
}
