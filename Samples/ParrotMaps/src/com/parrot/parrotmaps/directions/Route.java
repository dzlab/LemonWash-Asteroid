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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.parrot.parrotmaps.LatLngBounds;

public class Route

{
	/** Attributes */
	private ArrayList<Leg> mLegs;
	private String mSummary;
	private Polyline mOverviewPath;
	private ArrayList<String> mWarnings;
	private ArrayList<Integer> mWaypointOrder;
	private ArrayList<Integer> mOptimizedWaypointOrder;
	private String mCopyrights;
	private LatLngBounds mBounds;
	/**
	 * Constructor
	 * 
	 * @param res
	 * @return
	 * @throws JSONException 
	 */
	public Route(JSONObject obj) throws JSONException, Exception {
		boolean first = true;
		
		mSummary = obj.getString("summary");
		
		JSONArray array = obj.getJSONArray("legs");
		mLegs = new ArrayList<Leg>();
		for (int i=0 ; i<array.length() ; i++) {
			Leg current = new Leg(array.getJSONObject(i));
			mLegs.add(current);
			if (first) {
				mBounds = current.getBounds();
				first = false;
			}
			else {
				mBounds.union(current.getBounds());
			}
		}
		mCopyrights = obj.getString("copyrights");
		mOverviewPath = new Polyline(obj.getJSONObject("overview_polyline"));
		
		array = obj.getJSONArray("warnings");
		mWarnings = new ArrayList<String>();
		for (int i=0 ; i<array.length() ; i++) {
			mWarnings.add(array.getString(i));
		}
		array = obj.getJSONArray("waypoint_order");
		mWaypointOrder = new ArrayList<Integer>();
		for (int i=0 ; i<array.length() ; i++) {
			mWaypointOrder.add(array.getInt(i));
		}
		/* API V3 */
		if (obj.has("optimized_waypoint_order")) {
			array = obj.getJSONArray("optimized_waypoint_order");
			mOptimizedWaypointOrder = new ArrayList<Integer>();
			for (int i=0 ; i<array.length() ; i++) {
				mOptimizedWaypointOrder.add(array.getInt(i));
			}
		}
		/* Directions API : doesn't exist */
		else {
			mOptimizedWaypointOrder = mWaypointOrder;
		}
	
	}
	/**
	 * Operation
	 * 
	 * @return ArrayList<Leg>
	 */
	public ArrayList<Leg> getLegs() {
		return mLegs;
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getSummary() {
		return mSummary;
	}
	
	/**
	 * Operation
	 * 
	 * @return Polyline
	 */
	public Polyline getOverviewPath() {
		return mOverviewPath;
	}
	/**
	 * Operation
	 * 
	 * @return ArrayList<String>
	 */
	public ArrayList<String> getWarnings() {
		return mWarnings;
	}
	/**
	 * Operation
	 * 
	 * @return ArrayList<Integer>
	 */
	public ArrayList<Integer> getWaypointOrder() {
		return mWaypointOrder;
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getCopyrights() {
		return mCopyrights;
	}

}
