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
package com.parrot.parrotmaps;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import com.parrot.parrotmaps.directions.Polyline;

public interface LayerInterface


{
	/**
	 * Returns ID of this layer.
	 * @return ID of this layer
	 */
	abstract public long getId (  );

	/**
     * Returns list of markers of this layer.
     * @return list of markers
     */
    abstract public LinkedList<Marker> getMarkersList (  );
    
    /**
     * Sets a new list of markers.
     * @param new list
     */
    abstract public void setMarkersList(LinkedList<Marker> list);
    /**
	 * Returns list of polylines of this layer.
     * @return list of polylines
     */
    abstract public LinkedList<Polyline> getPolylinesList (  );
    
    /**
     * Adds a marker in the layer.
     * Updates built-in list of markers and bounds.
     * @param marker
     */
    abstract public void addMarker ( Marker marker );
    
    /**
     * Removes a marker of the layer.
     * Updates built-in list of markers and bounds.
     * @param marker
     */
    abstract public void removeMarker ( Marker marker );
    
    /**
     * Adds a polyline in the layer.
     * Updates built-in list of polylines and bounds.
     * @param polyline
     */
    abstract public void addPolyline ( Polyline polyline );
    
    /**
     * Removes a polyline of the layer.
     * Updates built-in list of polylines and bounds.
     * @param polyline
     */
    abstract public void removePolyline ( Polyline polyline );
    
    /**
	 * Returns bounds to display all the markers of the layer on a map centered
	 * on a specific marker.
	 * @param centralMarker The marker at the center of the map 
     * @return bounds of the layer
     */
    public LatLngBounds getBoundsCenteredOnMarker (Marker centralMarker);
    
    /**
     * Removes all markers of this layer.
     */
    public void removeAllMarkers();

    /**
     * Removes all polylines of this layer.
     */
    public void removeAllPolylines();

    /**
     * Removes all markers and polylines of this layer.
     */
    public void clearLayer();
    
	/**
	 * Returns a JavaScript JSONObject of this layer in order
	 * to draw it on webView.
	 * Used in webView.drawLayer() function.
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject getJSONObject() throws JSONException;

	/**
	 * Only for MapView overlay managing.
	 * Prepares InfoWindow linked to marker given in parameters.
	 * This method permits that at next map refresh,
	 * the InfoWindow will appear.
	 * @param marker
	 * @return The height of the bitmap in pixels.
	 */
	public int openInfoWindow (Marker marker);

	/**
	 * Only for MapView overlay managing.
	 * Close the actual opened InfoWindow.
	 */
	public void closeInfoWindow();
	
	/**
	 * Called when the activity is destroyed.
	 */
	public void destroy();
}

