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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

import com.parrot.parrotmaps.BufferedReaderFactory;
import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.DisplayAbstract;
import com.parrot.parrotmaps.InfoWindow;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.Layer;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.InfoWindow.TYPE;
import com.parrot.parrotmaps.log.PLog;

public class DirectionsResultLayer extends Layer
		implements
			DirectionsResultLayerInterface

{
	/** Attributes */
	private        DirectionsResult mDirectionsResult = null;
	static private String           mBaseURL          = "http://maps.google.com/maps/api/directions/json?";
	private        BufferedReader   directionsReader  = null;
	
	/*
	 * Available langages at
	 * http://spreadsheets.google.com/pub?key=p9pdwsai2hDMsLkXsoM05KQ&gid=1
	 */

	/**
	 * Operation
	 * @param context 
	 */
	public DirectionsResultLayer(DisplayAbstract context, Controller controller) throws Exception {
		super(context, controller);
	}
	/**
	 * Operation
	 * 
	 * @param start
	 * @param end
	 */
	public SEARCH_RESULT route(String start, String end, String language) {
		URL url;
		StringBuffer result = new StringBuffer("");
		SEARCH_RESULT returnCode = SEARCH_RESULT.OK;
		setInterrupted(false);
		if ((start != null)
				&& !start.equals("")
				&& (end != null)
				&& !end.equals("") ) {
			start = normalizeRouteQuery(start);
			end = normalizeRouteQuery(end);
			try {
				url = new URL(mBaseURL + "origin=" + start + "&destination=" + end
						+ "&language=" + language + "&sensor=true");
				PLog.d(TAG, "Sending direction URL... ", url);
				directionsReader = BufferedReaderFactory.openBufferedReader(url);
				for (String line; !isInterrupted() && (line = directionsReader.readLine()) != null;) {
					result.append(line).append("\n");
				}
				directionsReader.close();
				PLog.d(TAG, "Direction result received !");
				JSONObject root = new JSONObject(result.toString());
	
				if (root.getString("status").equals("OK")) {
					mDirectionsResult = new DirectionsResult(root);
				}
				setMarkersAndPolylines();
			} catch (SocketTimeoutException e) {
				PLog.e(TAG, "Network timeout");
				returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
			} catch (MalformedURLException e) {
				PLog.e(TAG, "Incorrect URL in route method : ", e.getMessage());
				returnCode = SEARCH_RESULT.FAILED;
				e.printStackTrace();
			} catch (UnknownHostException e) {
				PLog.e(TAG, "Unknown Host Exception");
				returnCode = SEARCH_RESULT.FAILED_NETWORK_TIMEOUT;
			}catch (IOException e) {
				PLog.e(TAG, "Error while manipulating stream response : ",
						e.getMessage());
				returnCode = SEARCH_RESULT.FAILED;
				e.printStackTrace();
			} catch (JSONException e) {
				PLog.e(TAG, "Error while building directions result : ",
						e.getMessage());
				returnCode = SEARCH_RESULT.FAILED;
				e.printStackTrace();
			} catch (Exception e) {
				PLog.e(TAG, "Error while building directions result : ",
						e.getMessage());
				returnCode = SEARCH_RESULT.FAILED;
				e.printStackTrace();
			}
		}
		else {
			PLog.e(TAG, "Route aborded, invalid arguments : start=", start, ", end=", end);
			returnCode = SEARCH_RESULT.FAILED;
		}
		if( isInterrupted() ) {
			PLog.e(TAG, "Route interrupted");
			returnCode = SEARCH_RESULT.FAILED_CANCELED;
		}
		return returnCode;
	}

	private void setMarkersAndPolylines() throws Exception {
		clearLayer();
		InfoWindow infoWindow;

		if (mDirectionsResult != null) {
			Iterator<Route> itRoute = mDirectionsResult.getRoutes().iterator();
			while (itRoute.hasNext()) {
				Route route = itRoute.next();
				Iterator<Leg> itLeg = route.getLegs().iterator();
				while (itLeg.hasNext()) {
					Leg leg = itLeg.next();
					Iterator<Step> itStep = leg.getSteps().iterator();
					boolean firstStep = true;
					while (itStep.hasNext()) {
						Step step = itStep.next();
						infoWindow = new InfoWindow(step.getInstructionsShort(), TYPE.NORMAL, null, null);
						
						/* Drawing a step marker on beginning of each step, except the first (start marker to draw). */
						DirectionMarker marker;
						if (firstStep) {
							marker = new DirectionMarker(leg.getStartLocation(), Marker.TYPE.DIRECTIONS_START, false, infoWindow.getTitle(), 3, infoWindow);
							firstStep = false;
						}
						else if (!itStep.hasNext()) {
							marker = new DirectionMarker(leg.getEndLocation(), Marker.TYPE.DIRECTIONS_END, false, infoWindow.getTitle(), 3, infoWindow);
						}
						else {
							marker = new DirectionMarker(step.getStartLocation(), Marker.TYPE.DIRECTIONS_STEP, true, infoWindow.getTitle(), 2, infoWindow);							
						}
						marker.setInstructionDistance(step.getDirectionsDistance().getText());
						addMarker(marker);
						Polyline polyline = step.getPolyline();
						addPolyline(polyline);
					}
				}
			}
		}
	}

	private String normalizeRouteQuery(String query) {
		query = query.replace(" ", "+");
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return query;
	}
	
	public DirectionsResult getDirectionsResult() {
		return mDirectionsResult;
	}
	
	/**
	 * Creates the layer from existing DirectionsResult object.
	 * @param directionsResult Object to import
	 * @return true if import was successfully done, false otherwise.
	 */
	public boolean importDirectionsResult (DirectionsResult directionsResult) {
		boolean returnCode = true;
		mDirectionsResult = directionsResult;
		try {
			setMarkersAndPolylines();
		} catch (Exception e) {
			PLog.e(TAG, "Error while building directions result : ",
					e.getMessage());
			e.printStackTrace();
			returnCode = false;
		}
		return returnCode;
	}
	
	public String getRouteSummary() {
		String routeSummary = "";
		if( (null != mDirectionsResult)
				&& (null != mDirectionsResult.getRoutes())
				&& (mDirectionsResult.getRoutes().size() > 0)) {
			Route route = mDirectionsResult.getRoutes().get(0);
			if( (null != route)
					&& (null != route.getLegs())
					&& (route.getLegs().size() > 0)) {
				Leg leg = route.getLegs().get(0);
				routeSummary = leg.getDirectionsDuration().getText()
								+"    "
								+leg.getDirectionsDistance().getText();
			}
		}
		return routeSummary;
	}

	/**
     * Get the index of the next direction instruction
     * according to a given position.
     * @param pos The position
     * @return The index of the next direction instruction.
     */
	public int getNextInstruction(LatLng pos) {
		float[] dist = new float[1];
		float closestDist = Float.MAX_VALUE;
		int closestStep = 0;
		int curStep = 0;
		int pointAtFirstStep = 0;
		if( (null != mDirectionsResult)
			&& (null != mDirectionsResult.getRoutes())
			&& (mDirectionsResult.getRoutes().size() > 0)) {
			Route route = mDirectionsResult.getRoutes().get(0);
			if( (null != route)
					&& (null != route.getLegs())
					&& (route.getLegs().size() > 0)) {
				for( Leg leg : route.getLegs() ) {
					for( Step s : leg.getSteps() ) {
						for( LatLng latLng : s.getPolyline().getPath() ) {
							Location.distanceBetween(pos.getLat(), pos.getLng(), latLng.getLat(), latLng.getLng(), dist);
							if( dist[0] < closestDist ) {
								closestDist = dist[0];
								if( 0 == curStep ) {
									if( pointAtFirstStep <= (s.getPolyline().getPath().size()/10) )
									{
										closestStep = curStep;
									}
									else
									{
										closestStep = curStep+1;
									}
								}
								else {
									closestStep = curStep+1;
								}
							}
							if( 0 == curStep ) {
								pointAtFirstStep++;
							}
						}
						curStep++;
					}
				}
			}
		}
		int markerListLen = 0;
		if( null != mMarkersList ) {
			markerListLen = mMarkersList.size();
		}
		return Math.min(closestStep,markerListLen-1);
	}

	private boolean mInterrupted = false;
	/**
	 * Interrupt the current route query.
	 */
	public void interruptRouteQuery() {
		setInterrupted(true);
	}
	private synchronized void setInterrupted( boolean interrupted ) {
		mInterrupted = interrupted;
	}
	public synchronized boolean isInterrupted() {
		return mInterrupted;
	}

	/**
	 * Number of times the draw method of this layer to be called
	 * each time the map view is refreshed.
	 * First, draw method has to be called to draw the polyline.
	 * Then, draw method hes to be called to draw markers.
	 * @return 2.
	 */
	@Override
	public int getNbRequiredDrawPass() {
		return 2;
	}
}
