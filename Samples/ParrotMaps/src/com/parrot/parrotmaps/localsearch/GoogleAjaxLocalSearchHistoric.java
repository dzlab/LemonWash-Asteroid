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
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.SparseArray;
import android.util.SparseIntArray;

import com.parrot.parrotmaps.BufferedReaderFactory;
import com.parrot.parrotmaps.LatLngBounds;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.localsearch.LocalSearchLayerInterface.SEARCH_RESULT;
import com.parrot.parrotmaps.log.PLog;

public class GoogleAjaxLocalSearchHistoric{

	private static final String TAG = "GoogleAjaxLocalSearchHistoric";
	
	// Attributes for Google Local Search
	static private String BASE_URL = "http://ajax.googleapis.com/ajax/services/search/local?";
	private static int GOOGLE_PAGE_SIZE = 8;
	private static int GOOGLE_START_OFFSET_MAX = 24;
	private String mQuery;
	private LatLngBounds mBounds;
	private String mLang;
	private BufferedReader   mResultsReader  = null;
	private LinkedList<ResultMarker> mMarkersList = new LinkedList<ResultMarker>();
	
	private boolean mIsCanceled = false;
	
	//private 
	
	/**
	 * Number of results by page (attribute set by Parrot)
	 */
	static private int PARROT_PAGE_SIZE = 10;
	
	
	/**
	 * Current page index of the historic. By default, it is set to 0
	 */
	private int mCurrentPage = 0;
	
	
	/**
	 * Represent the historic. It contains all the markers found for the current POI request
	 */
	private SparseArray<ResultMarker> mMarkersArray = new SparseArray<ResultMarker>();
	
	/**
	 * Use to store a temporary historic
	 */
	private SparseArray<ResultMarker> mTempMarkersArray = new SparseArray<ResultMarker>();
	
	
	private SparseIntArray mPageTable = new SparseIntArray();
	
	private HistoricPageStates mPageStates = new HistoricPageStates();

	public static boolean updateMap = false;
	
	
	
	private static GoogleAjaxLocalSearchHistoric instance = null;
	
	
	/**
	 * Get the unique instance of the Google Local Search Historic.
	 * @return 
	 */
	public final synchronized static GoogleAjaxLocalSearchHistoric getInstance()
	{
		if (instance == null)
		{
			instance = new GoogleAjaxLocalSearchHistoric();
		}
		return instance;
	}
	
	/**
	 * Release unique instance.
	 * Called on application close.
	 */
	public synchronized static void deleteInstance() {
		instance = null;
	}
	
	private GoogleAjaxLocalSearchHistoric()	{}
	
	
	/**
	 * Clean the historic and prepare it for a new request. 
	 * To perform the request, you have to call getPage(index)  (index = page number)
	 * @param query The POI keywords to search
	 * @param bounds Bounds of the local search
	 * @param lang Language code for results
	 */
	public void newSearch(String query, LatLngBounds bounds, String lang)
	{
		mTempMarkersArray.clear();
	}
	
	
	/**
	 * Add a list of markers to the historic at the given offset 
	 * @param markers The list of markers to add
	 * @param offset The offset
	 */
	public void addMarkers(List<ResultMarker> markers, int offset)
	{
		for (int i = 0; i < markers.size(); i++)
		{
			mPageTable.put((i + offset) / PARROT_PAGE_SIZE, mPageTable.get((i + offset) / PARROT_PAGE_SIZE) + 1);
			mMarkersArray.put(i + offset, markers.get(i));
		}
	}
	
	
	/**
	 * Get the next page of the Google search historic.
	 * You have to be sure that you can make this request (i.e you're not asking for page > 2)
	 * @return A list of markers or null if the request is canceled
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException 
	 */
	public LinkedList<Marker> getNextPage() throws SocketTimeoutException, UnknownHostException
	{
		int nextPage = mCurrentPage + 1;
		return getPage(nextPage);
	}
	
	/**
	 * Get the previous page of the Google search historic.
	 * You have to be sure that you can make this request (i.e you're not asking for page < 0)
	 * @return A list of markers or null if the request is canceled
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException 
	 */
	public LinkedList<Marker> getPreviousPage() throws SocketTimeoutException, UnknownHostException
	{
		int previousPage = mCurrentPage - 1;
		return getPage(previousPage);
	}
	
	
	/**
	 * Get the markers from the requested page. 
	 * If some markers are not loaded yet, it makes a Google local search request to fill the blank.
	 * @param index The index of the page to get
	 * @return A list of markers or null if the request is canceled
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException 
	 */
	public LinkedList<Marker> getPage(int index, String query, LatLngBounds bounds, String lang) throws SocketTimeoutException, UnknownHostException
	{
		int oldPage = mCurrentPage;
		mCurrentPage = index;
		LinkedList<Marker> list = new LinkedList<Marker>();
		
		// Get the indexes needed to fill the list
		int firstIndex = index * PARROT_PAGE_SIZE;
		int firstIndexOfNextPage = firstIndex + PARROT_PAGE_SIZE;
		
		//It will be used for giving a drawable assets for the marker
		int j = 0;
		if (query != null || bounds != null || lang != null)
		{
			try
			{
				// It is a new search
				SEARCH_RESULT result = loadPage(0, query, bounds, lang);
				
				if (result == SEARCH_RESULT.OK || result == SEARCH_RESULT.OK_NO_NEXT)
				{
					mPageStates.put(0, HistoricPageStates.LOADED_VALUE, true);
					if (result == SEARCH_RESULT.OK_NO_NEXT)
					{
						mPageStates.put(0, HistoricPageStates.HAS_NEXT_VALUE, false);
					}
					for (int i = 0 ; i < PARROT_PAGE_SIZE; i++)
					{
						if (mMarkersArray.get(i) != null)
						{
							mMarkersArray.get(i).setAsset(j);
							list.add(mMarkersArray.get(i));
							j++;
						}
					}		
				}
				else
				{
					// The search has been canceled. 
					// Get back to the previous search there is one.
					mCurrentPage = oldPage;
					return null;
				}
			}
			catch (SocketTimeoutException e) {
				mCurrentPage = oldPage;
				throw e;
			}
			catch (UnknownHostException e) {
				mCurrentPage = oldPage;
				throw e;
			}
		}
		else
		{
			try
			{
				for (int i = firstIndex ; i < firstIndexOfNextPage; i++)
				{
					if (mPageStates.isPageLoaded(index))
					{
						// The page is already loaded
						if (mMarkersArray.get(i) != null)
						{
							mMarkersArray.get(i).setAsset(j);
							list.add(mMarkersArray.get(i));
							j++;
						}
					}
					else
					{
						// If the marker at the current indice doesn't exist, a request is sent to Google
						if (mMarkersArray.get(i) == null)
						{
							int google_page_index = i / GOOGLE_PAGE_SIZE;
							
							SEARCH_RESULT result = loadPage(google_page_index * GOOGLE_PAGE_SIZE);
							//On recharge tout à partir de cette page google
							if (result == SEARCH_RESULT.OK_NO_NEXT)
							{
								mPageStates.put(index, HistoricPageStates.HAS_NEXT_VALUE, false);
							}
							else if (result != SEARCH_RESULT.OK)
							{
								// The search has been canceled. 
								// Get back to the previous search there is one.
								mCurrentPage = oldPage;
								return null;
							}
						}
						if (mMarkersArray.get(i) != null)
						{
							mMarkersArray.get(i).setAsset(j);
							list.add(mMarkersArray.get(i));
							j++;
						}
					}
				}
			} catch (SocketTimeoutException e) {
				mCurrentPage = oldPage;
				throw e;
			} catch (UnknownHostException e) {
				mCurrentPage = oldPage;
				throw e;
			}
		}
		mPageStates.put(index, HistoricPageStates.LOADED_VALUE, true);
		return list;
	}
	
	public LinkedList<Marker> getPage(int index) throws SocketTimeoutException, UnknownHostException
	{
		return getPage(index, null, null, null);
	}
	
	
	
	/**
	 * Return the markers from the current historic page.
	 * You can see what page is on by calling getCurrentPage
	 * @return
	 */
	public synchronized LinkedList<Marker> getMarkerFromCurrentPage()
	{
		try {
			if (mMarkersArray.size() > 0)
			{
				LinkedList<Marker> resultmarkers =  getPage(mCurrentPage);
				return resultmarkers;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e)
		{
			PLog.e(TAG, "Exception ", e.getMessage());
			return null;
		}
		return null;
	}
	
	
	
	
	
	/**
	 * Send requests to the Google Ajax service for searching POIs. The request starts at the given offset
	 * @param offset 
	 * @throws SocketTimeoutException
	 * @throws UnknownHostException 
	 */
	private SEARCH_RESULT loadPage(int offset, String query, LatLngBounds bounds, String lang) throws SocketTimeoutException, UnknownHostException
	{
		boolean reset = true;
		boolean no_previous = false;
		List<ResultMarker> markersList = new LinkedList<ResultMarker>();
		JSONObject response = null;

		try {
			int nbResultsARequest = 8;
			int nbParts           = PARROT_PAGE_SIZE/nbResultsARequest;
			int startOffset       = offset;
			
			if ((PARROT_PAGE_SIZE%nbResultsARequest) > 0) {
				nbParts++;
			}
			/*
			 * Sending requests until MAX_SEARCH_RESULTS or nbSearchResults is reached.
			 */
			while (nbParts > 0 && startOffset <= GOOGLE_START_OFFSET_MAX && !no_previous) {
				if (mIsCanceled)
				{
					return SEARCH_RESULT.FAILED_CANCELED;
				}
				StringBuffer result = new StringBuffer();
				
				URL url = null;
				if (query == null && bounds == null && lang == null)
				{
					url = buildURL(startOffset, mQuery, mBounds, mLang);
				}
				else
				{
					url = buildURL(startOffset, query, bounds, lang);
				}
				PLog.d(TAG, "Sending google ajax local search URL... ", url);
				mResultsReader = BufferedReaderFactory.openBufferedReader(url);
				for (String line; (line = mResultsReader.readLine()) != null;) {
					result.append(line).append("\n");
				}
				mResultsReader.close();
				if (mIsCanceled)
				{
					return SEARCH_RESULT.FAILED_CANCELED;
				}
				PLog.d(TAG, "Local search result received !");
				response = new JSONObject(result.toString());

				try {
					int currentPageIndex = response.getJSONObject("responseData").getJSONObject("cursor").getInt("currentPageIndex");
					if (currentPageIndex != (startOffset / GOOGLE_PAGE_SIZE))
					{
						// Response current page and requested page are not the same. 
						// It happens when we request a google page that should not exist because of the lack of result
						// In this case, the request resent the exact same result as the previous request.
						no_previous = true;
						break;
					}
				} catch (JSONException e) {
					//Cannot find 'currentPageIndex' -> single result (city)
					no_previous = true;
				}
				
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
				GoogleAjaxLocalSearchClient.setMarkers(response,markersList);
				startOffset += nbResultsARequest;
				nbParts--;
			}
			
		} catch (SocketTimeoutException e) {
			PLog.e(TAG, "Network timeout : " + e.getMessage());
			throw e;				
		} catch (MalformedURLException e) {
			PLog.e(TAG, "Incorrect URL in search method : ", e.getMessage());
			e.printStackTrace();
		}  catch (UnknownHostException e)
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
			response = null;
			e.printStackTrace();
		} catch (Exception e) {
			PLog.e(TAG, "Error while building local search result : ",
					e.getMessage());
			e.printStackTrace();
		}
		if (mIsCanceled)
		{
			return SEARCH_RESULT.FAILED_CANCELED;
		}
		if (query != null || bounds != null || lang != null)
		{
			//It is a new search. At this point, the longest part is done. 
			//Clean historic and set new lang, bounds, and query
			clean();
			mQuery = query;
			mBounds = bounds;
			mLang = lang;
		}
		addMarkers(markersList, offset);
		if (no_previous)
		{
			return SEARCH_RESULT.OK_NO_NEXT;
		}
		return SEARCH_RESULT.OK;
	}
	
	
	private SEARCH_RESULT loadPage(int offset) throws SocketTimeoutException, UnknownHostException
	{
		 return loadPage(offset, null, null, null);
	}
	
	
	
	
	/**
	 * Build an URL according to the query, the language and the bounds set when calling newSearch(...) 
	 * @param startOffset
	 * @return
	 */
	private URL buildURL(int startOffset, String query, LatLngBounds bounds, String lang)
	{
		URL url = null;
		if (query != null)
		{
			query = URLEncoder.encode(query);
		}
		else
		{
			query = "";
		}
		try
		{
			if( null != bounds ) 
			{
				url = new URL(BASE_URL +
					"v=1.0" +
					"&q=" + query +
					"&hl=" + lang +
					"&rsz=" + GOOGLE_PAGE_SIZE +
					"&start=" + startOffset +
					"&sll=" + bounds.getCenter().getLat() + "," + bounds.getCenter().getLng() +
					"&sspn=" + bounds.toSpan());
			}
			else 
			{
				url = new URL(BASE_URL +
						"v=1.0" +
						"&q=" + query +
						"&hl=" + lang +
						"&rsz=" + GOOGLE_PAGE_SIZE +
						"&start=" + startOffset);
			}
		}
		catch (MalformedURLException e) 
		{
			e.printStackTrace();
		}
		return url;
	}
	

	/**
	 * Get the current page index
	 * @return
	 */
	public int getCurrentPage()
	{
		return mCurrentPage;
	}
	
	/**
	 * Set the current page index
	 * @param currentPage The new index
	 */
	public void setCurrentPage(int currentPage)
	{
		mCurrentPage = currentPage; 
	}
	
	
	/**
	 * Clean the current historic so a new search can be perform normally
	 */
	public void clean()
	{
		mCurrentPage = 0;
		mMarkersArray.clear();
		mMarkersList.clear();
		mPageTable.clear();
		mPageStates.clear();
		mResultsReader  = null;
	}
	

	/**
	 * Interrupt the current search query.
	 */
	public void interruptSearchQuery() {
		PLog.i(TAG,"interruptSearchQuery");
		try {
			mIsCanceled = true;
			if( null != mResultsReader ) {
				mResultsReader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void setCanceled(boolean canceled)
	{
		mIsCanceled = canceled;
	}
	
	
	public HistoricPageStates getHistoricPageStates()
	{
		return mPageStates;
	}
	
}
