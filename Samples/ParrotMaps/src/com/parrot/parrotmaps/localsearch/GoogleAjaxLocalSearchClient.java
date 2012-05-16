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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.parrot.parrotmaps.BufferedReaderFactory;
import com.parrot.parrotmaps.InfoWindow;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.LatLngBounds;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.log.PLog;

public class GoogleAjaxLocalSearchClient
{
	/** Attributes */
	public final String TAG = this.getClass().getSimpleName();

	static private String BASE_URL           = "http://ajax.googleapis.com/ajax/services/search/local?";
	static private int    MAX_SEARCH_RESULTS = 10;
	
	private BufferedReader   mResultsReader  = null;
	private GoogleAjaxLocalSearchHistoric mHistoric = null;
	
	/**
	 * Constructor.
	 */
	public GoogleAjaxLocalSearchClient() {
	}
	
	
	/**
	 * Search with historic
	 * @param query
	 * @param bounds
	 * @param lang
	 * @param page
	 * @param reset
	 * @return
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException 
	 */
	public LinkedList<Marker> search(String query, LatLngBounds bounds, String lang, int page, boolean reset) throws SocketTimeoutException, UnknownHostException
	{
		mHistoric = GoogleAjaxLocalSearchHistoric.getInstance();
		if (reset)
		{
			mHistoric.newSearch(query, bounds, lang);
			return mHistoric.getPage(page, query, bounds, lang);
		}
		else
		{
			return mHistoric.getPage(page);
		}
	}
	
	/**
	 * Search for next results
	 * @return A list of marker if next results exits, null otherwise 
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException
	 */
	public LinkedList<Marker> searchNext() throws SocketTimeoutException, UnknownHostException
	{
		if (mHistoric == null)
		{
			return null;
		}
		return mHistoric.getNextPage();
	}
	
	/**
	 * Search for previous results
	 * @return A list of marker if previous results exist, null otherwise
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException
	 */
	public LinkedList<Marker> searchPrevious() throws SocketTimeoutException, UnknownHostException
	{
		if (mHistoric == null)
		{
			return null;
		}
		return mHistoric.getPreviousPage();
	}

	/**
	 * Builds request's URL, send it to the Google Ajax service, and analysis
	 * response.
	 * If several requests are needed because MAX_SEARCH_RESULTS > 8 (ceiled
	 * by Google), the method manage itself to do the operation
	 * http://code.google.com/intl/fr-FR/apis/ajaxsearch/documentation/#fonje
	 * @param query searched address or POI
	 * @param bounds bounds of the local search
	 * @param lang language code for results
	 * @return true if all operations were successfully done, false otherwise.
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException 
	 * @see
	 */
	public List<ResultMarker> search(String query, LatLngBounds bounds, String lang) throws SocketTimeoutException, UnknownHostException {
		boolean reset = true;
		
		List<ResultMarker> markersList = new LinkedList<ResultMarker>();
		JSONObject response = null;

		try {
			int nbResultsARequest = 8;
			int nbParts           = MAX_SEARCH_RESULTS/nbResultsARequest;
			int startOffset       = 0;
			
			if ((MAX_SEARCH_RESULTS%nbResultsARequest) > 0) {
				nbParts++;
			}
			/*
			 * Sending requests until MAX_SEARCH_RESULTS or nbSearchResults is reached.
			 */
			while (nbParts > 0) {

				StringBuffer result = new StringBuffer();
				query = normalizeRouteQuery(query);
				//query = URLEncoder.encode(query);
				//query = encodeRouteQuery(query);
				
				URL url;
				if( null != bounds ) {
					url = new URL(BASE_URL +
						"v=1.0" +
						"&q=" + query +
						"&hl=" + lang +
						"&rsz=" + nbResultsARequest +
						"&start=" + startOffset +
						"&sll=" + bounds.getCenter().getLat() + "," + bounds.getCenter().getLng() +
						"&sspn=" + bounds.toSpan());
				}
				else {
					url = new URL(BASE_URL +
							"v=1.0" +
							"&q=" + query +
							"&hl=" + lang +
							"&rsz=" + nbResultsARequest +
							"&start=" + startOffset);
				}
				PLog.d(TAG, "Sending google ajax local search URL... ", url);
				mResultsReader = BufferedReaderFactory.openBufferedReader(url);
				for (String line; (line = mResultsReader.readLine()) != null;) {
					result.append(line).append("\n");
				}
				mResultsReader.close();
				PLog.d(TAG, "Local search result received !");
				response = new JSONObject(result.toString());

				/*
				 * Once the first response is received, we adjust the real
				 * amount of possible responses for next sends.
				 */
				if (reset) {
					if (response.getInt("responseStatus") != 200) {
						/* No results found on first request */
						throw (new Exception("Bad response status : " + response.getString("responseDetails")));
					}
					/* Getting number of parts to download.
					 * If it is lower than number of parts calculated with MAX_SEARCH_RESULTS,
					 * we reduce the number of parts to download to this new number.
					 */
					JSONArray pages = response.getJSONObject("responseData").getJSONObject("cursor").optJSONArray("pages");
					if (pages != null) {
						int nbRealParts = pages.getJSONObject(pages.length()-1).getInt("label");
						nbParts = Math.min(nbParts, nbRealParts);
					}
					else {
						nbParts = 1;
					}
					reset = false;
				}
				setMarkers(response,markersList);
				startOffset += nbResultsARequest;
				nbParts--;
			}
		} catch (SocketTimeoutException e) {
			PLog.e(TAG, "Network timeout : ", e.getMessage());
			throw e;
		} catch (MalformedURLException e) {
			PLog.e(TAG, "Incorrect URL in search method : ", e.getMessage());
			e.printStackTrace();
		} catch (UnknownHostException e)
		{
			PLog.e(TAG, "Unknown Host Exception ", e.getMessage());
			throw e;
		}catch (IOException e) {
			PLog.e(TAG, "Error while manipulating stream response : ",
					e.getMessage());
			e.printStackTrace();
		} catch (JSONException e) {
			PLog.e(TAG, "Error JSON while building local search result : ",
					e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			PLog.e(TAG, "Error while building local search result : ",
					e.getMessage());
			e.printStackTrace();
		} 
		
		return markersList;
	}
	
	/**
	 * Adds to markers list all results of a response.
	 * @throws Exception
	 */
	static protected void setMarkers(JSONObject response, List<ResultMarker> markersList) throws Exception {
		if (response != null) {
			JSONArray jArray = response.getJSONObject("responseData").getJSONArray("results");
			
			for (int i=0 ; i<jArray.length() /*&& markersList.size()<MAX_SEARCH_RESULTS*/ ; i++) {
				JSONObject jObject = jArray.getJSONObject(i);
				
				// Get latitude and longitude
				LatLng latlng = new LatLng(jObject.getDouble("lat"), jObject.getDouble("lng"));
				ArrayList<String> phoneNumbers = new ArrayList<String>();
				ArrayList<String> addressLines = new ArrayList<String>();
				
				StringBuffer content = new StringBuffer("");
				if (jObject.has("content")) {
					content.append(jObject.optString("content", ""));
				}
				if (content.length() != 0) {
					content.append("\n");
				}


				// Address lines
				String separator = "";
				boolean first = true;
				JSONArray jAddressArray = jObject.getJSONArray("addressLines");
				for (int j=0 ; j<jAddressArray.length() ; j++) {
					if (first) {
						first = false;
					}
					else {
						separator = "\n";
					}
					addressLines.add(jAddressArray.getString(j));
					content.append(separator).append(jAddressArray.getString(j));
				}
				
				// Phone numbers 
				JSONArray jPhoneArray = null;
				if (jObject.has("phoneNumbers")) {
					jPhoneArray = jObject.getJSONArray("phoneNumbers");
					for (int j=0 ; j<jPhoneArray.length() ; j++) {
						// Type, not used at present
//						String type = jPhoneArray.getJSONObject(j).optString("type", "");
						String number = jPhoneArray.getJSONObject(j).getString("number");
						if (first) {
							first = false;
						}
						else {
							separator = "\n";
						}

						phoneNumbers.add(number);
						content.append(separator).append(number);
					}
				}
				
				InfoWindow infoWindow = new InfoWindow(jObject.getString("titleNoFormatting"),
						InfoWindow.TYPE.NORMAL,
						null,
						null);
				ResultMarker marker = new ResultMarker(latlng,
						markersList.size(),                        // Marker index
						jObject.getString("titleNoFormatting"),
						1,
						infoWindow);
				marker.setPhoneNumbersList(phoneNumbers);
				marker.setAddressLinesList(addressLines);
				marker.setCountry(jObject.getString("country"));
				marker.setState(jObject.getString("region"));
				marker.setCity(jObject.getString("city"));
				marker.setStreet(jObject.getString("streetAddress"));
				markersList.add(marker);
			}
		}
		
	}

	/**
	 * Normalize the given query by replacing spaces
	 * @param query
	 * @return
	 */
	private String normalizeRouteQuery(String query) {
		return query.replace(" ", "+");
	}

	/**
	 * Interrupt the current search query.
	 */
	public void interruptSearchQuery() {
		PLog.i(TAG,"interruptSearchQuery");
		try {
			if( null != mHistoric ) {
				mHistoric.setCanceled(true);
			}
			if( null != mResultsReader ) {
				mResultsReader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the current page index 
	 */
	public int getCurrentPage()
	{
		return mHistoric.getCurrentPage();
	}
	
	/**
	 * Clean the current historic so a new search can be perform normally
	 */
	public void clean()
	{
		if (mHistoric != null)
		{
			mHistoric.clean();
		}
	}
}
