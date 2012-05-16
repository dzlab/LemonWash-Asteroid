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
package com.parrot.parrotmaps.localsearch;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Location;

import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.geocoding.Geocoder;
import com.parrot.parrotmaps.log.PLog;

public class QuickSearchCategories
{
	private static final String TAG = "QuickSearchCategories";
	// Search categories
	// do not change this without changing
	// strings order in arrays.xml
	public static final int ENTRY_CAT_GAS_STATIONS = 0;
	public static final int ENTRY_CAT_BARS = 1;
	public static final int ENTRY_CAT_RESTAURANTS = 2;
	public static final int ENTRY_CAT_HOTELS = 3;
	public static final int ENTRY_CAT_MOVIE_THEATERS = 4;
	public static final int ENTRY_CAT_COFFEE = 5;
	public static final int ENTRY_CAT_MEDICAL = 6;
	public static final int ENTRY_CAT_PARKING = 7;
	public static final int ENTRY_CAT_SPORT = 8;
	public static final int ENTRY_CAT_LEISURE = 9;
	public static final int ENTRY_CAT_TRANSPORT = 10;
	public static final int ENTRY_CAT_SHOPPING = 11;
	public static final int ENTRY_CAT_FINANCE = 12;
	public static final int ENTRY_CAT_COMMUNITY = 13;
	
	private static final int LANG_EN = 0;   //!< English
	private static final int LANG_FR = 1;   //!< French
	private static final int LANG_DE = 2;	//!< German
	private static final int LANG_IT = 3;	//!< Italian
	private static final int LANG_SP = 4;	//!< Spanish
	private static final int LANG_DU = 7;	//!< Dutch

	private static final int LANG_PT = 17;	//!< Portuguese

	private static final int LANG_BE = 19;	//!< For Belgium
	private static final int LANG_ZH = 20;	//!< For China
	
	
	private static final int LANG_DEFAULT = LANG_EN;	//!< Default language is english
	
	/**
	 * Queries to Geocoder are performed we are 30km far from
	 * the position of the last query.
	 */
	private static final float DISTANCE_TO_LAST_QUERY_THRESHOLD = 30000;

	//! Reference to Geocoder used for current query
	private static Geocoder sGeocoder = null;
	
	//! Languages returned by the last query to Geocoder
	private static List<Integer> sLastLanguages = null;
	//! Latitude of the last query to Geocoder
	private static double sLastLat = Double.NaN;
	//! Longitude of the last query to Geocoder
	private static double sLastLon = Double.NaN;

	/**
	 * Return the keywords to search for the given category
	 * @param ctx The current application context
	 * @param category The category id
	 * @param lat The latitude to determine the search location
	 * @param lon The longitude to determine the search location
	 * @return A string containing the keywords
	 */
	public static String getKeywordsForCategory( Context ctx, int category, double lat, double lon )
	{
		StringBuffer keywords = new StringBuffer();
		
		List<Integer> languages = getLanguagesByLocation( ctx, lat, lon );
		if( null != languages )
		{
			for( int i=0; i<languages.size(); i++ )
			{
				String subKeywords = getKeywordsByLanguage( ctx, languages.get(i), category );
				if( (keywords.length() > 0)
						&& (subKeywords.length() > 0) )
				{
					keywords.append(" | ");
				}
				keywords.append(subKeywords);
			}
		}
		else
		{
			keywords.append(getKeywordsByLanguage( ctx, LANG_DEFAULT, category ));
		}
		PLog.i(TAG,"getKeywordsForCategory("+category+") ",keywords);
        return keywords.toString();
	}

	/**
	 * Get an address from a location.
	 * @param ctx The current application context
	 * @param lat The latitude of the location
	 * @param lon The longitude of the location
	 * @return The address or null it fails
	 */
	private static Address getAddressByLocation( Context ctx, double lat, double lon )
	{
		sGeocoder = new Geocoder(ctx);
		Address address = null;
		try {
			List<Address> adds = sGeocoder.getFromLocation(lat, lon, 1);
			sGeocoder = null;
			if( (null != adds)
					&& (adds.size() > 0) )
			{
				PLog.i(TAG,"getAddressByLocation - add[0] : ",adds.get(0).getCountryCode());
				address = adds.get(0);
			}
			else
			{
				PLog.i(TAG,"getAddressByLocation - No result from geocoder");
				address = null;
			}
		} catch (Exception e)
		{
			PLog.i(TAG,"getAddressByLocation - An exception occured ",e);
			e.printStackTrace();
		}
		return address;
	}
	
	
	/**
	 * See country codes at http://www.unc.edu/~rowlett/units/codes/country.htm
	 * @param ctx The current application context
	 * @param add The address from which the language has to be determined
	 * @return
	 */
	private static ArrayList<Integer> getLanguagesByAddress( Context ctx, Address add )
	{
		ArrayList<Integer> languages = new ArrayList<Integer>();
		languages.add(LANG_EN);
		// France and French Guiana
		if( add.getCountryCode().equalsIgnoreCase("FR")
				|| add.getCountryCode().equalsIgnoreCase("GF") )
		{
			languages.remove(LANG_EN);
			languages.add(LANG_FR);
		}
		// Italia
		else if( add.getCountryCode().equalsIgnoreCase("IT") )
		{
			languages.add(LANG_IT);
		}
		// Germany
		else if( add.getCountryCode().equalsIgnoreCase("DE") )
		{
			languages.add(LANG_DE);
		}
		// Netherlands
		else if( add.getCountryCode().equalsIgnoreCase("NL") )
		{
			languages.add(LANG_DU);
		}
		// Austria
		else if( add.getCountryCode().equalsIgnoreCase("AT") )
		{
			languages.add(LANG_DE);
		}
		// Canada
		else if( add.getCountryCode().equalsIgnoreCase("CA") )
		{
			languages.add(LANG_FR);
		}
		// Belgium
		else if( add.getCountryCode().equalsIgnoreCase("BE") )
		{
			languages.remove(LANG_EN);
			languages.add(LANG_BE);
		}
		else if( add.getCountryCode().equalsIgnoreCase("ES")       // Spain
				 || add.getCountryCode().equalsIgnoreCase("CL")    // Chile
 				 || add.getCountryCode().equalsIgnoreCase("AR")    // Argentina
 			     || add.getCountryCode().equalsIgnoreCase("UY")    // Uruguay
				 || add.getCountryCode().equalsIgnoreCase("PY")    // Paraguay
				 || add.getCountryCode().equalsIgnoreCase("BO")    // Bolivia
			     || add.getCountryCode().equalsIgnoreCase("PE")    // Peru
				 || add.getCountryCode().equalsIgnoreCase("EC")    // Ecuador
				 || add.getCountryCode().equalsIgnoreCase("CO")    // Colombia
				 || add.getCountryCode().equalsIgnoreCase("VE")    // Venezuela
			     || add.getCountryCode().equalsIgnoreCase("GT")    // Guatemala
				 || add.getCountryCode().equalsIgnoreCase("BZ")    // Belize
				 || add.getCountryCode().equalsIgnoreCase("HN")    // Honduras
				 || add.getCountryCode().equalsIgnoreCase("SV")    // El Salvador
				 || add.getCountryCode().equalsIgnoreCase("CR")    // Costa Rica
				 || add.getCountryCode().equalsIgnoreCase("MX") )  // Mexico
		{
			languages.add(LANG_SP);
		}
		// Portugal, Brasil
		else if( add.getCountryCode().equalsIgnoreCase("PT")
				|| add.getCountryCode().equalsIgnoreCase("BR") )
		{
			languages.add(LANG_PT);
		}
		// China
		else if( add.getCountryCode().equalsIgnoreCase("CN") )
		{
			languages.add(LANG_ZH);
		}
		else
		{
			// No other language for other countries
		}
			     
		return languages;
	}
	
	/**
	 * Get the list of languages spoken at a specific location.
	 * @param ctx Application context
	 * @param lat The latitude of the location
	 * @param lon The longitude of the location
	 * @return List of languages codes.
	 */
	private static List<Integer> getLanguagesByLocation( Context ctx, double lat, double lon )
	{
		List<Integer> languages = null;

		if( !Double.isNaN(sLastLat)
				&& !Double.isNaN(sLastLon)
				&& (null != sLastLanguages) ) {
			float[] distance = new float[1];
			Location.distanceBetween(lat, lon, sLastLat, sLastLon, distance);
			if( distance[0] < DISTANCE_TO_LAST_QUERY_THRESHOLD ) {
				languages = sLastLanguages;
				PLog.i(TAG,"getLanguagesByLocation - Get from cache");
			}
		}
		if( null == languages ) {
			Address add = getAddressByLocation( ctx, lat, lon );
			if( null != add )
			{
				languages = getLanguagesByAddress( ctx, add );
				sLastLanguages = languages;
				sLastLat = lat;
				sLastLon = lon;
			}
		}
		return languages;
	}
	
	/**
	 * Return the keywords to search for the given category for the given language
	 * @param ctx The current application context
	 * @param language The keyword language
	 * @param category The category to search
	 * @return A string with the keywords
	 */
	private static String getKeywordsByLanguage( Context ctx, int language, int category )
	{
		String keywords = "";
		int kwArrayId;
		switch( language )
		{
		case LANG_EN:
			kwArrayId = R.array.keywords_en;
			break;
		case LANG_FR:
			kwArrayId = R.array.keywords_fr;
			break;
		case LANG_DE:
			kwArrayId = R.array.keywords_de;
			break;
		case LANG_IT:
			kwArrayId = R.array.keywords_it;
			break;
		case LANG_SP:
			kwArrayId = R.array.keywords_sp;
			break;
		case LANG_DU:
			kwArrayId = R.array.keywords_du;
			break;
		case LANG_PT:
			kwArrayId = R.array.keywords_pt;
			break;
		case LANG_BE:
			kwArrayId = R.array.keywords_be;
			break;
		case LANG_ZH:
			kwArrayId = R.array.keywords_zh;
			break;
		default:
			kwArrayId = R.array.keywords_en;
			break;
		}
		// Get keywords for local language
		String[] kwArray = ctx.getResources().getStringArray(kwArrayId);
		if( null != kwArray )
		{
			keywords = kwArray[category];
		}
		return keywords;
	}

	/**
	 * Interrupt the current geocoding query.
	 */
	public static void interruptQuery() {
		PLog.i(TAG,"interruptQuery");
		if( null != sGeocoder ) {
			sGeocoder.interruptQuery();
		}
	}

}
