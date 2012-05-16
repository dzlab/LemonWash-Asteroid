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

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.DisplayAbstract;
import com.parrot.parrotmaps.LatLngBounds;
import com.parrot.parrotmaps.Layer;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.log.PLog;

public class GoogleAjaxLocalSearchLayer extends Layer
		implements
			LocalSearchLayerInterface

{
	/** Attributes */
	public final String TAG = this.getClass().getSimpleName();
	
	private GoogleAjaxLocalSearchClient mClient = null;
	
	private int mCurrentPage = 0;
	
	private boolean mIsCanceled = false;
	
	/**
	 * Constructor.
	 * @param mContext 
	 */
	public GoogleAjaxLocalSearchLayer(DisplayAbstract context, Controller controller) throws Exception {
		super(context, controller);
		mClient = new GoogleAjaxLocalSearchClient();
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
	 */
	public SEARCH_RESULT search(String query, LatLngBounds bounds, String lang) {
		SEARCH_RESULT returnCode = SEARCH_RESULT.OK;
		
		

		LinkedList<Marker> responseMarkers;
		
		try {
			responseMarkers = mClient.search(query, bounds, lang, 0, true);
			if( (null == responseMarkers)
					|| (responseMarkers.size() == 0) ) {
				if (mIsCanceled)
				{
					PLog.w(TAG, "search - canceled for: ",query);
					GoogleAjaxLocalSearchHistoric.getInstance().setCanceled(false);
					mIsCanceled = false;
					returnCode = SEARCH_RESULT.FAILED_CANCELED;
				}
				else
				{
					PLog.w(TAG, "search - Nothing found for: ",query);
					returnCode = SEARCH_RESULT.FAILED;
				}
			}
			else {
				if (!mIsCanceled)
				{
					removeAllMarkers();
					//clean();
					setMarkersList(responseMarkers);
				}
				else
				{
					GoogleAjaxLocalSearchHistoric.getInstance().setCanceled(false);
					mIsCanceled = false;
					returnCode = SEARCH_RESULT.FAILED_CANCELED;
				}
			}
		} catch (SocketTimeoutException e) {
			PLog.w(TAG, "search - A network timeout occured for: ",query);
			returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
		} catch (UnknownHostException e)
		{
			PLog.w(TAG, "search - Unkown Host exception occured for: ",query);
			returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
		}
		mCurrentPage = GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage();
		return returnCode;
	}

	/**
	 * Interrupt the current search query.
	 */
	public void interruptSearchQuery() {
		if( null != mClient ) {
			mClient.interruptSearchQuery();
		}
	}
	
	@Override
	public SEARCH_RESULT searchNext()
	{
		SEARCH_RESULT returnCode = SEARCH_RESULT.OK;
		
		LinkedList<Marker> responseMarkers;
		try {
			responseMarkers = mClient.searchNext();
			if( (null == responseMarkers)
					|| (responseMarkers.size() == 0) ) {
				if (mIsCanceled)
				{
					PLog.w(TAG, "search - canceled for: next");
					GoogleAjaxLocalSearchHistoric.getInstance().setCanceled(false);
					mIsCanceled = false;
					returnCode = SEARCH_RESULT.FAILED_CANCELED;
				}
				else
				{
					PLog.w(TAG, "search - Nothing found for next query");
					returnCode = SEARCH_RESULT.FAILED;
				}
			}
			else {
				if (!mIsCanceled)
				{
					removeAllMarkers();
					setMarkersList(responseMarkers);
				}
				else
				{
					GoogleAjaxLocalSearchHistoric.getInstance().setCanceled(false);
					mIsCanceled = false;
					returnCode = SEARCH_RESULT.FAILED_CANCELED;
				}
			}
		} catch (SocketTimeoutException e) {
			PLog.w(TAG, "search - A network timeout occured for 'searchNext' ");
			returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
		} catch (UnknownHostException e)
		{
			PLog.w(TAG, "search - Unkown Host exception occured for 'searchNext' ");
			returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
		}
		mCurrentPage = GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage();
		return returnCode;
	}
	
	
	@Override
	public SEARCH_RESULT searchPrevious()
	{
		SEARCH_RESULT returnCode = SEARCH_RESULT.OK;
		
		LinkedList<Marker> responseMarkers;
		try {
			responseMarkers = mClient.searchPrevious();
			if( (null == responseMarkers)
					|| (responseMarkers.size() == 0) ) {
				if (mIsCanceled)
				{
					PLog.w(TAG, "search - canceled for: next");
					GoogleAjaxLocalSearchHistoric.getInstance().setCanceled(false);
					mIsCanceled = false;
					returnCode = SEARCH_RESULT.FAILED_CANCELED;
				}
				else
				{
					PLog.w(TAG, "search - Nothing found for next query");
					returnCode = SEARCH_RESULT.FAILED;
				}
			}
			else {
				if (!mIsCanceled)
				{
					removeAllMarkers();
					setMarkersList(responseMarkers);
				}
				else
				{
					GoogleAjaxLocalSearchHistoric.getInstance().setCanceled(false);
					mIsCanceled = false;
					returnCode = SEARCH_RESULT.FAILED_CANCELED;
				}
			}
		} catch (SocketTimeoutException e) {
			PLog.w(TAG, "search - A network timeout occured for 'searchPrevious' ");
			returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
		} catch (UnknownHostException e)
		{
			PLog.w(TAG, "search - Unkown Host exception occured for 'searchPrevious' ");
			returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
		}
		mCurrentPage = GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage();
		return returnCode;
	}
	
	public void clean()
	{
		mClient.clean();
	}
	
	
	public int getCurrentPage()
	{
		return mCurrentPage;
	}
	
	

	public void setCurrentPage(int currentPage) {
		mCurrentPage = currentPage;
	}
	
	public void cancelTask()
	{
		mIsCanceled = true;
	}
	
}
