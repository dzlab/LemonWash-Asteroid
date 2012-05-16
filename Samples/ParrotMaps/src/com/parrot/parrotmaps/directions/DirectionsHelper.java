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
package com.parrot.parrotmaps.directions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Address;

import com.parrot.parrotmaps.LatLngBounds;
import com.parrot.parrotmaps.MapAddress;
import com.parrot.parrotmaps.geocoding.Geocoder;
import com.parrot.parrotmaps.localsearch.GoogleAjaxLocalSearchClient;
import com.parrot.parrotmaps.localsearch.ResultMarker;
import com.parrot.parrotmaps.log.PLog;

/**
 *
 */
public class DirectionsHelper
{
	private static final String TAG = "RouteHelper";

	private static int ADDRESS_NOT_FOUND = 0;
	private static int ADDRESS_FOUND = 1;
	
	/**
	 * Use the Geocoder to get an address from a query.
	 * @param ctx Context
	 * @param query Location query
	 * @param retAdd If an address is found, it is stored in retAdd[0]
	 * @return ADDRESS_FOUND if an address if found, ADDRESS_FOUND otherwise
	 */
	private static ArrayList<MapAddress> getAddsFromQueryOnGeocoder( Context ctx, String query )
	{
		int found = ADDRESS_NOT_FOUND;
		ArrayList<MapAddress> adds = null;
		Geocoder geocoder = new Geocoder(ctx);
		try
		{
			List<Address> geoAdds = geocoder.getFromLocationName(query, 10);
			// Check if at least one address given by the geocoder matches
			for( int i=0; (i<geoAdds.size())&&(ADDRESS_NOT_FOUND==found); i++ )
			{
				PLog.i(TAG,"getAddressFromQueryOnGeocoder query ",query," ret ",geoAdds.get(i));
				Address add = geoAdds.get(0);
				query = query.toLowerCase();
				if( ((null!=add.getCountryName()) && add.getCountryName().toLowerCase().contains(query))
						|| ((null!=add.getAdminArea()) && add.getAdminArea().toLowerCase().contains(query))
						|| ((null!=add.getLocality()) && add.getLocality().toLowerCase().contains(query)) )
				{
					found = ADDRESS_FOUND;
				}
			}
			if( ADDRESS_FOUND == found )
			{
				adds = new ArrayList<MapAddress>();
				for( int i=0; i<geoAdds.size(); i++ )
				{
					Address add = geoAdds.get(i);
					String address = "";
					int j = 0;
					String line = add.getAddressLine(j);
					while( null != line )
					{
						address += line;
						j++;
						line = add.getAddressLine(j);
						if( null != line )
						{
							address += " ";
						}
					}
					MapAddress mapAddress = new MapAddress();
					mapAddress.address = address;
					mapAddress.dispAddress = address;
					adds.add(mapAddress);
				}
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		PLog.i(TAG,"getAddsFromQueryOnGeocoder ",query," address ",adds);
		return adds;
	}

	/**
	 * Use Google local search service to get an address from a query.
	 * The returned address is the address of the first result returned
	 * by the Google server.
	 * @param loc Location
	 * @param query Query string
	 * @param lang language code for results
	 * @return The address found or an empty string
	 * @throws SocketTimeoutException 
	 * @throws UnknownHostException 
	 */
	private static ArrayList<MapAddress> getAddsFromQueryOnLocal( LatLngBounds bounds,
                                                                  String query,
                                                                  String lang ) throws SocketTimeoutException, UnknownHostException
	{
		ArrayList<MapAddress> adds = null;
		// Do a local search with this query on the Google servers
		GoogleAjaxLocalSearchClient client = new GoogleAjaxLocalSearchClient();
		List<ResultMarker> queryResults = null;
		//queryResults = client.search(query, bounds, lang);
		queryResults = client.search(query, bounds, lang);
		if( (null != queryResults)
				&& (queryResults.size() > 0) )
		{
			adds = new ArrayList<MapAddress>();
			for( int i=0; i<queryResults.size(); i++ )
			{
				MapAddress address = new MapAddress();
				address.address = queryResults.get(i).getAddressOneLine();
				address.dispAddress = queryResults.get(i).getTitle()+", "+address.address;
				adds.add(address);
			}
		}
		PLog.w(TAG,"getAddsFromQueryOnLocal ",query," address ",adds);
		return adds;
	}

	/**
	 * 
	 * @param ctx
	 * @param loc
	 * @param query
	 * @return
	 */
	public static ArrayList<MapAddress> getAddsFromQuery( Context ctx,
                                                          LatLngBounds bounds,
                                                          String query )
	{
		ArrayList<MapAddress> adds = null;
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try
		{
			adds = getAddsFromQueryOnGeocoder( ctx, query );
			if( null == adds )
			{
				adds = getAddsFromQueryOnLocal( bounds,
												query,
												ctx.getResources().getConfiguration().locale.getLanguage() );
			}
		}
		catch( Exception ex )
		{
			PLog.i(TAG,"getAddsFromQuery: Catched exception ",ex);
			ex.printStackTrace();
		}
		return adds;
	}
}
