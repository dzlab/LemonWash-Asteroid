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

import android.util.SparseArray;

/**
 * Class representing the states of the historic's pages.
 * The state of a page is defined by the following attributes:<p>
 * <li><b>loaded</b> (the page contains elements that have been loaded from a Google Search request)
 * <li><b>has next</b> (it is possible (or not) to search for a page with a higher index)
 * <li><b>has previous</b> (it is possible (or not) to search for a page with a lower index)
 * 
 * These attributes are generally set after the first Google Search.<p>
 * 
 * Historic pages are referenced through their index. The historic is limited to 3 pages of 10 results.
 * So index 0 means it is the first page and index 2 means it is the last page 
 * 
 *
 */
public class HistoricPageStates {
	
	
	public static final int LOADED_VALUE = 0;
	public static final int HAS_PREVIOUS_VALUE = 1;
	public static final int HAS_NEXT_VALUE = 2;

	/**
	 * Class representing the state of 1 page
	 *
	 */
	private class PageState
	{
		boolean mHasPrevious;
		boolean mHasNext;
		boolean mLoaded;
		
		/**
		 * Create a new page state. 
		 * It is preferred to assume that the page has previous and next pages if it is not known when it is creating.
		 * Logically, the page must be considered as not loaded when it is created 
		 * @param hasPrevious Set to true if it has a previous page, false otherwise
		 * @param hasNext Set to true if it has a next page, false otherwise
		 * @param loaded Set to true if the page is loaded, false otherwise
		 */
		protected PageState(boolean hasPrevious, boolean hasNext, boolean loaded)
		{
			mHasPrevious = hasPrevious;
			mHasNext = hasNext;
			mLoaded = loaded; 
		}
		
	}
	
	/**
	 * Contains the page states
	 */
	private SparseArray<PageState> mPageStates = new SparseArray<PageState>();
	
	/**
	 * Get the loaded state of the given page
	 * @param page_index The page index
	 * @return True if the page is already loaded, false otherwise
	 */
	public boolean isPageLoaded(int page_index)
	{
		PageState state = mPageStates.get(page_index);
		if (state == null)
		{
			return false;
		}
		else
		{
			return state.mLoaded;
		}
	}
	
	/**
	 * Request if the given page has a previous page
	 * @param page_index The page index
	 * @return True if there is a previous page, false otherwise
	 */
	public boolean hasPagePrevious(int page_index)
	{
		PageState state = mPageStates.get(page_index);
		if (state == null)
		{
			return false;
		}
		else
		{
			return state.mHasPrevious;
		}
	}
	
	/**
	 * Request if the given page has a next page
	 * @param page_index the page index
	 * @return True if there is a next page, false otherwise
	 */
	public boolean hasPageNext(int page_index)
	{
		PageState state = mPageStates.get(page_index);
		if (state == null)
		{
			return false;
		}
		else
		{
			return state.mHasNext;
		}
	}
	
	/**
	 * Set the given state for the given page
	 * @param page_index The page index
	 * @param state The state to modify
	 * @param value The new value of the state to modify
	 */
	public void put(int page_index, int state, boolean value)
	{
		PageState page_state = mPageStates.get(page_index);
		if (page_state == null)
		{
			// The page does not exist yet, it is a new page so create it
			// and put it in the sparse array
			page_state = new PageState(true, true, false);
			mPageStates.put(page_index, page_state);
		}
		if (page_index == 0)
		{
			// It is the first page
			page_state.mHasPrevious = false;
		}
		else if (page_index == 2)
		{
			// It is the last page
			page_state.mHasNext = false;
		}
		switch (state)
		{
			case LOADED_VALUE:
				page_state.mLoaded = value;
				break;
			
			case HAS_PREVIOUS_VALUE:
				page_state.mHasPrevious = value;
				break;
				
			case HAS_NEXT_VALUE:
				page_state.mHasNext = value;
				break;
		}

	}
	
	/**
	 * Clear all states that have been previously store
	 * This method should be used when cleaning the historic
	 */
	public void clear()
	{
		mPageStates.clear();
	}

}
