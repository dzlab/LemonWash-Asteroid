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
package com.parrot.parrotmaps.gps;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.log.PLog;

/**
 * Check the gps status.
 * It is used at application launch to display
 * or not a warning message indicating
 * that this application requires a GPS location.
 * 
 * Gps check is launched by Controller with the static
 * method GpsChecker.checkGpsStatus.
 * Once the Gps check is finished, it sends the message
 * Controller.MSG_GPS_CHECK_RESULT to the Controller with
 * a value indicating if the Gps is available or not.
 * 
 * @author FL
 *
 */
public class GpsChecker implements Listener, LocationListener {
	private static final String TAG = "GpsChecker";
	
	private Handler mControllerHandler;
	private Handler mHandler;
	private HandlerThread mGpsCheckTthread;
	private Boolean mGPSAvailable = false;
	private LocationManager mLocMng;

	private static GpsChecker mInstance;
	
	public synchronized static void checkGpsStatus(Context ctx, Handler handler) {
		if( null == mInstance ) {
			mInstance = new GpsChecker(ctx,handler);
		}
	}
	
	public synchronized static void stopGpsCheck() {
		if( null != mInstance ) {
			mInstance.destroy();
			releaseChecker();
		}
	}
	
	private synchronized static void releaseChecker() {
		mInstance = null;
	}
	
	private GpsChecker( Context ctx, Handler controllerHandler ) {
		PLog.w(TAG,"GpsChecker");
		mControllerHandler = controllerHandler;
		
		mLocMng = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		mLocMng.addGpsStatusListener(this);
		mLocMng.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

		// Check if a location has been received in 4 seconds
		mGpsCheckTthread = new HandlerThread(TAG);
		mGpsCheckTthread.start();
		mHandler = new GpsCheckerHandler(mGpsCheckTthread.getLooper());
		mHandler.sendEmptyMessageDelayed(0, 4*1000);
	}

	private void destroy() {
		mGpsCheckTthread.getLooper().quit();
		mControllerHandler = null;
	}
	
	/**
	 * If the STOPPED event is received,
	 * we consider that the GPS is not available.
	 */
	public void onGpsStatusChanged(int event) {
		if( event == GpsStatus.GPS_EVENT_STARTED ) {
			//             PLog.w(TAG,"onGpsStatusChanged - GPS_EVENT_STARTED");
			// Do nothing
		}
		else if( event == GpsStatus.GPS_EVENT_STOPPED ) {
			//             PLog.w(TAG,"onGpsStatusChanged - GPS_EVENT_STOPPED");
			synchronized(mGPSAvailable) {
				mGPSAvailable = false;
			}
		}
		else if( event == GpsStatus.GPS_EVENT_FIRST_FIX ) {
			//             PLog.w(TAG,"onGpsStatusChanged - GPS_EVENT_FIRST_FIX");
			// Do nothing
		}
		else if( event == GpsStatus.GPS_EVENT_SATELLITE_STATUS ) {
			//             PLog.w(TAG,"onGpsStatusChanged - GPS_EVENT_SATELLITE_STATUS");
			// Do nothing
		}
	}
	
	/**
	 */
    private final class GpsCheckerHandler extends Handler {
        public GpsCheckerHandler(Looper looper) {
            super(looper);
        }
    	
        @Override
        public void handleMessage(Message msg) {
        	if( null != mControllerHandler ) {
        		Message gpsMsg = Message.obtain(mControllerHandler, Controller.MSG_GPS_CHECK_RESULT);
        		synchronized(mGPSAvailable) {
        			gpsMsg.arg1 = mGPSAvailable?1:0;
        		}
        		gpsMsg.sendToTarget();
        		
        		 // Unregister GPS status listener
        		mLocMng.removeGpsStatusListener(GpsChecker.this);
        		// Unregister GPS updates listener
        		mLocMng.removeUpdates(GpsChecker.this);
        		        		
        		// Release GPS checker single instance
        		GpsChecker.releaseChecker();
        	}
         }
    }

    /**
     * This function is called when a location has been found.
     * When called, we consider that the GPS is available.
     */
	public void onLocationChanged(Location location) {
		synchronized(mGPSAvailable) {
			mGPSAvailable = true;
		}
	}

	public void onProviderDisabled(String provider) {
		// Do nothing
	}

	public void onProviderEnabled(String provider) {
		// Do nothing
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Do nothing
	}
}
