/*
 * Copyright (c) 2011, Parrot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   * Neither the name of "Parrot SA" nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.parrot.parrotmaps;

import java.util.Locale;

/**
 * Tools for units conversions.
 * 
 * @author FL
 *
 */
public class UnitsTools {
	//private static final String TAG = UnitsTools.class.getSimpleName();
	private final static double	KM_TO_MI_FACTOR	= 1609.344;
	private final static String[] IMPERIAL_SYSTEM_COUNTRIES = {"GB", "US"};
	
	public enum DISTANCE_UNITS_TYPES
	{
		METERS_LIKE,
		KILOMETERS_LIKE
	}

	/**
	 * Converts a distance in meters to a string for display.
	 * The result will include a string for the unit.
	 * The unit will depend on the configured language.
	 * @param distance Distance un meters.
	 * @return The string to display.
	 */
	public static String distanceToLocalString(double distance)
	{
		String disp;
		if(isImperialSystem())
		{
			double miles = getDistanceInMi(distance);
			if( miles < 0.1 )
			{
				double yards = distance / 0.9114;
				disp = String.format("%.0f", yards) + getLocalUnit(DISTANCE_UNITS_TYPES.METERS_LIKE);
			}
			else if( miles < 10 )
			{
				disp = String.format("%.1f", miles) + getLocalUnit(DISTANCE_UNITS_TYPES.KILOMETERS_LIKE);
			}
			else if( miles < 1000 )
			{
				disp = String.format("%.0f", miles) + getLocalUnit(DISTANCE_UNITS_TYPES.KILOMETERS_LIKE);
			}
			else
			{
				disp = String.format("%d", ((int)miles)/1000) + "," + String.format("%.0f", miles - ((((int)miles)/1000))*1000) + getLocalUnit(DISTANCE_UNITS_TYPES.KILOMETERS_LIKE);
			}
		}
		else
		{
			if( distance < 1000 )
			{
				disp = String.format("%.0f", distance) + getLocalUnit(DISTANCE_UNITS_TYPES.METERS_LIKE);
			}
			else if( distance < 10000 )
			{
				disp = String.format("%.1f", distance/1000) + getLocalUnit(DISTANCE_UNITS_TYPES.KILOMETERS_LIKE);
			}
			else if( distance < 1000000 )
			{
				disp = String.format("%.0f", distance/1000) + getLocalUnit(DISTANCE_UNITS_TYPES.KILOMETERS_LIKE);
			}
			else
			{
				double km = distance/1000;
				disp = String.format("%d", ((int)km)/1000) + " " + String.format("%.0f", km - ((((int)km)/1000))*1000) + getLocalUnit(DISTANCE_UNITS_TYPES.KILOMETERS_LIKE);
			}
		}
		return disp;
	}
	
	private static boolean isImperialSystem()
	{		
		String countryCode = Locale.getDefault().getCountry();
		for(int i = 0 ; i < IMPERIAL_SYSTEM_COUNTRIES.length ; i++)
			if(countryCode.equals(IMPERIAL_SYSTEM_COUNTRIES[i])) return true;
		return false;		
	}

	public static String getLocalUnit(DISTANCE_UNITS_TYPES type)
	{		
		switch(type)
		{
			case METERS_LIKE:
				return isImperialSystem() ? " yd" : " m";
			case KILOMETERS_LIKE:
			default:
				return isImperialSystem() ? " mi" : " km";
		}
	}
	
	public static double getDistanceInMi(double distanceInKm)
	{
		return distanceInKm / KM_TO_MI_FACTOR;
	}
			
	public static double getDistanceInKm(double distanceLocalUnitKm)
	{
		return isImperialSystem() ? distanceLocalUnitKm * KM_TO_MI_FACTOR / 1000 : distanceLocalUnitKm;
	}
}
