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
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.UnitsTools;
import com.parrot.parrotmaps.log.PLog;

public class ResultsListAdapter extends BaseAdapter
{
	
	private static final String TAG = "ResultListAdapter";
	
	private LayoutInflater mInflater;

	private List<Marker>       mMarkersList = null;
	private int                mCount       = 0;
	private LatLng             mCenter      = null;
	private GoogleAjaxLocalSearchHistoric mHistoric;
	
	private boolean mRequestCanceled = false;
	
	private Handler mHandler = new Handler();
	
	class ViewHolder
	{
		ImageView icon;
		TextView name;
		TextView address;
		TextView distance;
	}
	

	public ResultsListAdapter(Context context, LatLng center) {
		// Cache the LayoutInflate to avoid asking for a new one each time.
		mInflater = LayoutInflater.from(context);
		mCenter = center;
		mHistoric = GoogleAjaxLocalSearchHistoric.getInstance();
		try {
			mMarkersList = mHistoric.getPage(mHistoric.getCurrentPage());
			mCount = mMarkersList.size();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public int getCount() {
		return mCount;
	}

	public Object getItem(int position) {
		String result;
		
		if (mMarkersList != null && mCount > position) {
			result = mMarkersList.get(position).getTitle();
		}
		else {
			result = null;
		}
		
		return result;
	}

	public long getItemId(int position) {
		long result;
		
		if ((mMarkersList != null)
				&& (mCount > position)) {
			result = mMarkersList.get(position).getId();
		}
		else {
			result = -1;
		}
		return result;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		// A ViewHolder keeps references to children views to avoid
		// Unnecessary calls
		// to findViewById() on each row.
		ViewHolder holder;

		// When convertView is not null, we can reuse it directly, there is
		// no need
		// to reinflate it. We only inflate a new View when the convertView
		// supplied
		// by ListView is null.
		if (convertView == null)
		{
			convertView = mInflater.inflate(R.layout.results_list_row,null);

			// Creates a ViewHolder and store references to the two children
			// views
			// we want to bind data to.
			holder = new ViewHolder();
			holder.icon = (ImageView) convertView.findViewById(R.id.poi_icon);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.address = (TextView) convertView.findViewById(R.id.address);
			holder.distance = (TextView) convertView.findViewById(R.id.distance);

			convertView.setTag(holder);
		}
		else
		{
			// Get the ViewHolder back to get fast access to the TextView
			// and the ImageView.
			holder = (ViewHolder) convertView.getTag();
		}

		// Get poi handle
		//ResultMarker poi = (ResultMarker)mLayer.getMarkersList().get(position);
		ResultMarker poi = (ResultMarker)mMarkersList.get(position);

		// Set entry name
		holder.name.setText(poi.getTitle());

		// Set entry address
		if (poi.getAddress() != null) {
			holder.address.setText(poi.getAddress());
		}
		else if (poi.getInfoWindow() != null && poi.getInfoWindow().getContent() != null) {
			holder.address.setText(poi.getInfoWindow().getContent());
		}
		else {
			holder.address.setText("");
		}
		
		// Set entry distance
		if (mCenter != null) {
			float[] distance = new float[1];
			Location.distanceBetween(mCenter.getLat(), mCenter.getLng(), poi.getLatLng().getLat(), poi.getLatLng().getLng(), distance);
			holder.distance.setText(UnitsTools.distanceToLocalString(distance[0]));
			holder.distance.setVisibility(View.VISIBLE);
		}
		else {
			holder.distance.setText(String.format(""));
			holder.distance.setVisibility(View.GONE);
		}

		// Set entry icon
		holder.icon.setImageDrawable(poi.getDrawable());
		
		return convertView;
	}

	/**
	 * Update the current location with the given position
	 * @param lastPositionKnown The new position
	 */
	public void udpateLocation(LatLng lastPositionKnown) {
		mCenter = lastPositionKnown;
	}
	
	/**
	 * Request for the next page of the request list. 
	 * You have to be sure you can do that (i.e you are not asking for page > 2).
	 * The method checks multiple times if the request is canceled (cancel is called from an other thread).
	 */
	public void getNext()
	{
		mRequestCanceled = false;
		int oldPage = mHistoric.getCurrentPage();
		LinkedList<Marker> mNewMarkersList = null;
		try {
			mNewMarkersList = mHistoric.getNextPage();
			if (mNewMarkersList == null)
			{
				// The request is canceled or has failed
				if (mRequestCanceled)
				{
					PLog.w(TAG, "search - canceled for: next");
					mHistoric.setCanceled(false);
					mRequestCanceled = false;
				}
			}
			else
			{
				if (!mRequestCanceled)
				{
					mMarkersList = mNewMarkersList;
				}
				else
				{
					// The request is canceled.
					// Reset state to original page
					mHistoric.setCurrentPage(oldPage);
					mHistoric.setCanceled(false);
					mRequestCanceled = false;
				}
				mHandler.post(new Runnable() {
					
					public void run() {
						notifyDataSetChanged();
					}
				});
			}
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	/**
	 * Request for the previous page of the request list. 
	 * You have to be sure you can do that (i.e you are not asking for page < 0).
	 * The method checks multiple times if the request is canceled (cancel is called from an other thread).
	 */
	public void getPrevious()
	{
		mRequestCanceled = false;
		int oldPage = mHistoric.getCurrentPage();
		LinkedList<Marker> mNewMarkersList = null;
		try {
			mNewMarkersList = mHistoric.getPreviousPage();
			if (mNewMarkersList == null)
			{
				// The request is canceled or has failed
				if (mRequestCanceled)
				{
					PLog.w(TAG, "search - canceled for: next");
					mHistoric.setCanceled(false);
					mRequestCanceled = false;
				}
			}
			else
			{
				if (!mRequestCanceled)
				{
					mMarkersList = mNewMarkersList;
				}
				else
				{
					// The request is canceled.
					// Reset state to original page
					mHistoric.setCurrentPage(oldPage);
					mHistoric.setCanceled(false);
					mRequestCanceled = false;
				}
				mHandler.post(new Runnable() {
					
					public void run() {
						notifyDataSetChanged();
					}
				});
			}
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Get the marker at the given position
	 * @param position The marker's position
	 * @return The marker at the given position
	 */
	public Marker getMarker(int position)
	{
		return mMarkersList.get(position);
	}
	
	/**
	 * Check if the request is canceled
	 * @return True if it is canceled, false otherwise
	 */
	public boolean isRequestCanceled()
	{
		return mRequestCanceled;
	}
	
	/**
	 * Cancel the request
	 */
	public void cancelRequest()
	{
		mRequestCanceled = true;
	}
	
}
