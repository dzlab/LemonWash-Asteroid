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
package com.parrot.parrotmaps.wikipedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.parrot.parrotmaps.BufferedReaderFactory;
import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.DisplayAbstract;
import com.parrot.parrotmaps.InfoWindow;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.LatLngBounds;
import com.parrot.parrotmaps.Layer;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.log.PLog;

public class GeonamesLayer extends Layer implements WikipediaLayerInterface {
	static private final String BASE_URL           = "api.geonames.org/wikipediaBoundingBoxJSON?";
	static private int          MAX_SEARCH_RESULTS = 10;
	
	public final String TAG = this.getClass().getSimpleName();

	private static final int MSG_UPDATE = 0;
	private static final String DATA_BOUNDS = "bounds";

	//! Maximum number of Geonames items handled by the layer
	private static final int MAX_GEONAMES_ITEMS = 40;

	private String          mLang;
	private HandlerThread   mThread = null;
	private Looper          mLooper = null;
	private GeonamesHandler mHandler = null;

	public GeonamesLayer(DisplayAbstract context, Controller controller, String lang) throws Exception {
		super(context, controller);
		mLang = lang;
		// Start update thread
		mThread = new HandlerThread("Geonames Thread", Process.THREAD_PRIORITY_BACKGROUND);
	    mThread.start();
	    mLooper = mThread.getLooper();
	    mHandler = new GeonamesHandler(mLooper);
	}

	public void update(LatLngBounds bounds) {
		// Remove pending update messages
		mHandler.removeMessages(MSG_UPDATE);
		// Prepare update message 
		Message msg = Message.obtain();
		msg.what = MSG_UPDATE;
		Bundle data = new Bundle();
		data.putParcelable(DATA_BOUNDS, bounds);
		msg.setData(data);
		// Send update message
		mHandler.sendMessage(msg);
	}

	/**
	 * Update markers.
	 * Called in the geonames background thread.
	 */
	private void update_private(LatLngBounds bounds) {
		JSONObject response = null;

		try {
			StringBuffer result = new StringBuffer();

			URL url = new URL(BASE_URL +
					"lang=" + mLang +
					"&north=" + bounds.getNe().getLat() +
					"&south=" + bounds.getSw().getLat() +
					"&east=" + bounds.getNe().getLng() +
					"&west=" + bounds.getSw().getLng() +
					"&maxRows=" + MAX_SEARCH_RESULTS);
			PLog.d(TAG, "Sending geonames URL... ", url);
			BufferedReader buf = BufferedReaderFactory.openBufferedReader(url);
			for (String line; (line = buf.readLine()) != null;) {
				result.append(line).append("\n");
			}
			buf.close();
			PLog.d(TAG, "Geonames result received !");
			response = new JSONObject(result.toString());

			/*
			 * Once the first response is received, we adjust the real
			 * amount of possible responses for next sends.
			 */

			if (response.has("status")) {
				/* No results found on first request */
				throw (new Exception("Bad response status : " + response.getJSONObject("status").getString("message")));
			}

			updateMarkers(bounds,response);

			// Trigger a refresh of map display
			mController.getHandler().sendEmptyMessage(Controller.MSG_INVALIDATE_DISPLAY);
			
		} catch (MalformedURLException e) {
			PLog.e(TAG, "Incorrect URL in search method : ", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			PLog.e(TAG, "Error while manipulating stream response : ",
					e.getMessage());
			e.printStackTrace();
		} catch (JSONException e) {
			PLog.e(TAG, "Error JSON while building geonames result : ",
					e.getMessage());
			response = null;
			e.printStackTrace();
		} catch (Exception e) {
			PLog.e(TAG, "Error while building geonames result : ",
					e.getMessage());
			e.printStackTrace();
		}
	}

	private synchronized void updateMarkers(LatLngBounds bounds,JSONObject response) throws Exception {
		LinkedList<Marker> newList = new LinkedList<Marker>();
		LinkedList<Marker> oldList = getMarkersList();
		
		// Get Geonames elements in response
		JSONArray jArray = response.getJSONArray("geonames");
		PLog.d(TAG, "", jArray.length(), " elements in geonames response.");

		// Keep previous markers
		Iterator<Marker> itMarkers = oldList.iterator();
		while ( itMarkers.hasNext() ) {
			Marker oldMarker = itMarkers.next();
			newList.add(oldMarker);
		}
		
		// If have too much markers, remove invisible ones
		itMarkers = newList.iterator();
		while( (itMarkers.hasNext())
				&& ((jArray.length()+newList.size() > MAX_GEONAMES_ITEMS)) ) {
			Marker marker = itMarkers.next();
			if (!bounds.contains(marker.getLatLng())) {
				itMarkers.remove();
			}
		}
		
		// If still have too much markers, remove first ones
		itMarkers = newList.iterator();
		while( (itMarkers.hasNext())
				&& ((jArray.length()+newList.size() > MAX_GEONAMES_ITEMS)) ) {
			itMarkers.next();
			itMarkers.remove();
		}
		
		// Adding Markers in new range
		for (int i=0 ; i<jArray.length(); i++) {
			JSONObject jObject = jArray.getJSONObject(i);
			LatLng latlng = new LatLng(jObject.getDouble("lat"), jObject.getDouble("lng"));
			/* We add only a marker if it didn't exist in old list. */
			boolean found = false;
			itMarkers = getMarkersList().iterator();
			while (!found && itMarkers.hasNext()) {
				Marker existingMarker = itMarkers.next();
				if (latlng.equals(existingMarker.getLatLng())) {
					found = true;
				}
			}
			if (!found) {
				InfoWindow infoWindow = new InfoWindow(jObject.getString("title"),
						InfoWindow.TYPE.NORMAL,
						jObject.optString("thumbnailImg", null),
						jObject.getString("summary"));
				WikipediaMarker marker = new WikipediaMarker(latlng,
						true,
						jObject.getString("title"),
						1,
						infoWindow);
				marker.setArticleURL(jObject.optString("wikipediaUrl",""));
				marker.setThumbnailURL(jObject.optString("thumbnailImg",""));
				newList.add(marker);
			}
		}
		setMarkersList(newList);
	}

	/**
	 * Stop the markers update thread. 
	 */
	@Override
	public void destroy() {
		if( null != mLooper ) {
			mLooper.quit();
			mLooper = null;
			mThread = null;
		}
	}
	
    private final class GeonamesHandler extends Handler
    {
        public GeonamesHandler(Looper looper)
        {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	switch(msg.what) {
        	case MSG_UPDATE:
        		LatLngBounds bounds = msg.getData().getParcelable(DATA_BOUNDS);
				update_private(bounds);
        		break;
        	default:
        		break;
        	}
         }
    }
}
