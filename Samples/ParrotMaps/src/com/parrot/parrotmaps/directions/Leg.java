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

import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.LatLngBounds;

public class Leg

{
	/** Attributes */
	private DirectionsDistance mDistance;
	private DirectionsDuration mDuration;
	private String mStartAddress;
	private LatLng mStartLocation;
	private String mEndAddress;
	private LatLng mEndLocation;
	private ArrayList<Step> mSteps;
	private LatLngBounds mBounds;

	/**
	 * Constructor
	 * 
	 * @param leg
	 * @return
	 * @throws JSONException 
	 */
	public Leg(JSONObject obj) throws JSONException, Exception {
		boolean first = true;
		
		mDistance = new DirectionsDistance(obj.getJSONObject("distance"));
		mDuration = new DirectionsDuration(obj.getJSONObject("duration"));
		mStartLocation = new LatLng(obj.getJSONObject("start_location"));
		mStartAddress = obj.getString("start_address");
		mEndLocation = new LatLng(obj.getJSONObject("end_location"));
		mEndAddress = obj.getString("end_address");
		
		JSONArray array = obj.getJSONArray("steps");
		mSteps = new ArrayList<Step>();
		for (int i=0 ; i<array.length() ; i++) {
			Step current = new Step(array.getJSONObject(i));
			mSteps.add(current);
			if (first) {
				mBounds = current.getBounds();
				first = false;
			}
			else {
				mBounds.union(current.getBounds());
			}
		}
	}
	/**
	 * Operation
	 * 
	 * @return DirectionsDistance
	 */
	public DirectionsDistance getDirectionsDistance() {
		return mDistance;
	}
	/**
	 * Operation
	 * 
	 * @return DirectionsDuration
	 */
	public DirectionsDuration getDirectionsDuration() {
		return mDuration;
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getStartAddress() {
		return mStartAddress;
	}
	/**
	 * Operation
	 * 
	 * @return LatLng
	 */
	public LatLng getStartLocation() {
		return mStartLocation;
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getEndAddress() {
		return mEndAddress;
	}
	/**
	 * Operation
	 * 
	 * @return LatLng
	 */
	public LatLng getEndLocation() {
		return mEndLocation;
	}
	/**
	 * Operation
	 * 
	 * @return ArrayList<Step>
	 */
	public ArrayList<Step> getSteps() {
		return mSteps;
	}
	
	/**
	 * Returns a copy of bounds, for avoid modifications.
	 * @return LatLngBounds
	 */
	public LatLngBounds getBounds() {
		return mBounds.clone();
	}
	
}
