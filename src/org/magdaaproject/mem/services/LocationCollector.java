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

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

/**
 * collect location information and store it for use by the CoreService class
 */
public class LocationCollector implements LocationListener {
	
	/*
	 * private class level constants
	 */
	private static final int sTwoMinutes = 1000 * 60 * 2;
	
	private static final boolean sVerboseLog = false;
	private static final String sLogTag = "LocationCollector";
	
	/*
	 * private class level variables
	 */
	private Location currentLocation = null;
	

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
	public void onLocationChanged(Location location) {
		
		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "updated location received");
		}
		
		// determine if the new location is better than the current location
		if(isBetterLocation(location, currentLocation) == true) {
			currentLocation = location;
			
			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "new location is better than existing location");
				Log.v(sLogTag, "new details lat: '" + currentLocation.getLatitude() + "', lng: '" + currentLocation.getLongitude() + "'");
			}
		}

	}
	
	/**
	 * get details of the current location
	 * 
	 * @return the current location, may be null if no location is available
	 */
	public Location getCurrentLocation() {
		return currentLocation;
	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		
		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "location provider has been disabled '" + provider + "'");
		}
		
		// location provider has been disabled so reset the current location
		currentLocation = null;

	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "location provider has been enabled '" + provider + "'");
		}

	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
		// check to see if the provider has been disabled
		if(status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			// reset the current location
			currentLocation = null;
			
			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "location provider is not available '" + provider + "'");
			}
		}

	}
	
	/*
	 * the following two functions are derived from code released here:
	 * http://developer.android.com/guide/topics/location/strategies.html
	 * 
	 * Which is made available under the terms of the Apache 2.0 license (http://www.apache.org/licenses/LICENSE-2.0)
	 */

	/** 
	 * Determines whether one Location reading is better than the current Location fix
	 * 
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 * 
	 * @rturn true if the location is better than the current location
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > sTwoMinutes;
	    boolean isSignificantlyOlder = timeDelta < -sTwoMinutes;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/**
	 * checks to see if two providers are the same
	 * @param provider1 the first provider
	 * @param provider2 the second provider
	 * @return true if the provider is the same
	 */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}

}
