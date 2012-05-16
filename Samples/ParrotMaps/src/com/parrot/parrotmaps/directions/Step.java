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

import org.json.JSONException;
import org.json.JSONObject;

import android.text.Html;

import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.LatLngBounds;

public class Step

{
	/** Attributes */
	public final String TAG = this.getClass().getSimpleName();
	
	private String mTravelMode;
	private DirectionsDistance mDistance;
	private DirectionsDuration mDuration;
	private LatLng mStartLocation;
	private LatLng mEndLocation;
	private String mHTMLInstructions; /* With protected HTML symbols : <b> */
	private String mInstructions;     /* With normal HTML symbols : \u003cb\u003e */
	private Polyline mPolyline;
	/**
	 * Constructor
	 * 
	 * @param step
	 * @return
	 */
	public Step(JSONObject obj) throws JSONException, Exception {
		mTravelMode = obj.getString("travel_mode");
		mDistance = new DirectionsDistance(obj.getJSONObject("distance"));
		mDuration = new DirectionsDuration(obj.getJSONObject("duration"));
		mStartLocation = new LatLng(obj.getJSONObject("start_location"));
		mEndLocation = new LatLng(obj.getJSONObject("end_location"));
		mHTMLInstructions = obj.optString("html_instructions", null);
		if (mHTMLInstructions != null) {
			mHTMLInstructions = Html.fromHtml(mHTMLInstructions).toString();
		}
		mInstructions = obj.optString("instructions", null);
		if (mInstructions == null && mHTMLInstructions == null) {
			throw (new JSONException("No member html_instructions or instructions in the current step."));
		}
		mPolyline = new Polyline(obj.getJSONObject("polyline"));
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
	 * @return StartLocation
	 */
	public LatLng getStartLocation() {
		return mStartLocation;
	}
	/**
	 * Operation
	 * 
	 * @return EndLocation
	 */
	public LatLng getEndLocation() {
		return mEndLocation;
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getInstructions() {
		if (mHTMLInstructions != null) {
			return mHTMLInstructions;
		}
		else {
			return mInstructions;
		}
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getInstructionsShort() {
		String instructions = null;
		if (mHTMLInstructions != null) {
			instructions = mHTMLInstructions;
		}
		else {
			instructions = mInstructions;
		}
		if( (null != instructions)
				&& (instructions.indexOf("\n")!=-1)) {
			instructions = instructions.substring(0, instructions.indexOf("\n"));
		}
		return instructions;
	}
	/**
	 * Operation
	 * 
	 * @return Polyline
	 */
	public Polyline getPolyline() {
		return mPolyline;
	}
	/**
	 * Operation
	 * 
	 * @return LatLngBounds
	 */
	public LatLngBounds getBounds() {
		return mPolyline.getBounds();
	}
	
}
