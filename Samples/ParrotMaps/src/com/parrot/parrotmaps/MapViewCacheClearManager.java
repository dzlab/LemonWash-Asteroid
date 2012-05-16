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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.maps.MapView;
import com.parrot.parrotmaps.log.PLog;

/**
 * MapViewCacheClearManager implements a workaround for an issue in cache management
 * in Google Maps tools.
 * See issue #64264.
 * 
 * Each time network is turned to ready, it forces a cache flush in maps tools.
 * 
 * To force a cache flush, the MapView is draw with a 0 witdh.
 * 
 * @author FL
 *
 */
public class MapViewCacheClearManager {
	
	public static final String TAG = "MapViewCacheClearManager";
	
	//! Map display activity handle
	private MapViewDisplay mMapViewDisplay;
	//! Map view handle
	private MapView mMapView;

	//! Network status listener
	NetworkBroadcastReceiver mNetworkListener;
	//! Internal looper for internal messages
	private Looper mLooper;
	//! Internal messages handle
	MapViewCacheClearHandler mMapViewCacheClearHandler;

	//! Network states
	public enum State {
		UNKNOWN,      //!< Network state unknown
		CONNECTED,    //!< Network available
		NOT_CONNECTED //!< Network not available
	}
	
	//! Network state
	private State mState;
	
	/**
	 * Create a map
	 * @param mapViewDisplay
	 * @param mapView
	 */
	public MapViewCacheClearManager( MapViewDisplay mapViewDisplay,
									 MapView mapView ) {
		mMapViewDisplay = mapViewDisplay;
		mMapView = mapView;
		
		mState = State.UNKNOWN;
		
		IntentFilter networkIntentFilter = new IntentFilter();
		networkIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mNetworkListener = new NetworkBroadcastReceiver();
		mapViewDisplay.registerReceiver(mNetworkListener, networkIntentFilter);
		// Start update thread
		HandlerThread thread = new HandlerThread("Erase Thread", Process.THREAD_PRIORITY_BACKGROUND);
	    thread.start();
	    Looper mLooper = thread.getLooper();
	    mMapViewCacheClearHandler = new MapViewCacheClearHandler(mLooper);
	}

	/**
	 * Network state broadcast listener
	 */
	private class NetworkBroadcastReceiver extends BroadcastReceiver {

		/**
		 * When network state changes from NotConnected to Connected,
		 * the maps cache is cleared.
		 */
		@Override		
		public void onReceive(Context context, Intent intent) {
            if( ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()) ) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if( !noConnectivity ) {
                	// Now connected
                	// Clear map view cache
                	if( State.NOT_CONNECTED == mState ) {
                		mMapViewCacheClearHandler.sendEmptyMessage(MapViewCacheClearHandler.CLEAR_CACHE);
                	}
            		mState = State.CONNECTED; 
                } else {
                	// Not connected
                	mState = State.NOT_CONNECTED;
                }
            }
		}
		
	}
	
	/**
	 * Internal messages handler.
	 * The cache clear is done in 2 steps :
	 * 1. change layout to force MapView width at 0
	 * 2. draw MapView in a canvas
	 * When, the MapView is drawn with a 0 width, it triggers an
	 * exception in Maps tools. When this exception occurs, it flushes
	 * the map cache.
	 */
    private final class MapViewCacheClearHandler extends Handler
    {
    	//! Message for first step of cache clear
    	protected static final int CLEAR_CACHE = 0;
    	//! Message for second step of cache clear
    	private static final int CLEAR_CACHE_STEP_2 = 1;
    	
    	/**
    	 * Init the messages handler
    	 */
        public MapViewCacheClearHandler(Looper looper)
        {
            super(looper);
        }
        
        /**
         * Messages handling.
         */
        @Override
        public void handleMessage(Message msg) {        	
        	switch(msg.what) {
        	// First step of cache clear
        	case CLEAR_CACHE:
        		// Run this in the UI thread
        		// in order to be able to manipulate layout and views
        		mMapViewDisplay.runOnUiThread(new Runnable() {
        			public void run() {
        				if( null != mMapView ) {
        					mMapView.invalidate();
        					// Get a bitmap copy of current map view
        					// It will be used to avoid a screen blink, during this operation
        					Bitmap mapCopy = Bitmap.createBitmap(mMapView.getWidth(), mMapView.getHeight(), Bitmap.Config.RGB_565);
        					Canvas canvas = new Canvas(mapCopy);
        					mMapView.draw(canvas);
        					// Show the image view, to force map view width at 0
        					ImageView dummy = (ImageView)mMapViewDisplay.findViewById(R.id.dummy_view);
        					// Draw the copy of map view in the image view
        					dummy.setImageBitmap(mapCopy);
        					RelativeLayout layout = (RelativeLayout)mMapViewDisplay.findViewById(R.id.maplayout);
        					layout.setMinimumWidth(0);
        					dummy.setVisibility(View.VISIBLE);
        					mMapView.invalidate();
        				}
        			}
        		});
        		mMapViewCacheClearHandler.sendEmptyMessageDelayed(CLEAR_CACHE_STEP_2,500);
        		break;
        		
        	// Second step of cache clear
        	case CLEAR_CACHE_STEP_2:
        		// Run this in the UI thread
        		// in order to be able to manipulate layout and views
        		mMapViewDisplay.runOnUiThread(new Runnable() {
        			public void run() {
        				// Restore the correct map view size
        				// and hide the image view
        				if( null != mMapView ) {
        					mMapView.invalidate();
        					View dummy = (View)mMapViewDisplay.findViewById(R.id.dummy_view);
        					dummy.setVisibility(View.GONE);
        					mMapView.invalidate();
        	                mMapView.draw(new Canvas());
        				}
        			}
        		});
        		break;
        		
        	default:
                PLog.e(TAG,"MapViewCacheClearHandler.handleMessage - invalid message: ",msg.what);
        		break;
        	}
         }
    }

    /**
     * Stop the cache clear manager.
     */
	public void stop() {
		if( null != mLooper ) {
			mLooper.quit();
			mLooper = null;
		}
		if( null != mNetworkListener ) {
			mMapViewDisplay.unregisterReceiver(mNetworkListener);
			mNetworkListener = null;
		}
	}

}
