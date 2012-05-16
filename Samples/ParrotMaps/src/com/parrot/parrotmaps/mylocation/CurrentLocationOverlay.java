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
package com.parrot.parrotmaps.mylocation;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.parrot.parrotmaps.log.PLog;

/**
 * 
 * Override the MyLocationOverlay class to draw custom
 * current location marker.
 * 
 * @author FL
 *
 */
public class CurrentLocationOverlay extends MyLocationOverlay {
    private static final double SPEED_MIN_LIMIT = 1.4;
	private static final String TAG = "CurrentLocationOverlay";
	
	/** Current orientation of the bitmaps */
    private float mOrientation = Float.NaN;
    /** Matrix used for rotation of bitmaps */
    private Matrix mRotationMatrix = null;
    
    /** Bitmaps for location marker without orientation */
    private MyLocationBitmap[] mStaticLocationBitmaps;
    /** Bitmaps for location marker with orientation */
    private MyLocationBitmap[] mOrientatedLocationBitmaps;

    private Bitmap mMyLocationBitmap = null;
    private boolean mGPSAvailable = false;
    
    /** Last animation step time stamp */
    private long mAnimationStepTimestamp = 0;
    /** Animation step */
    int mAnimationStep = 0;
    
    public CurrentLocationOverlay(Context context, MapView mapView) {
        super(context, mapView);
        mRotationMatrix = new Matrix();
        
        mStaticLocationBitmaps = new MyLocationBitmap[2];
        mStaticLocationBitmaps[0] = new MyLocationBitmap(context,"marker_position.png");
        mStaticLocationBitmaps[1] = mStaticLocationBitmaps[0];
        
        mOrientatedLocationBitmaps = new MyLocationBitmap[2];
        mOrientatedLocationBitmaps[0] = new MyLocationBitmap(context,"marker_position.png");
        mOrientatedLocationBitmaps[1] = new MyLocationBitmap(context,"marker_position_direction.png");
    }
    
    @Override 
    protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLocation, long when) {
        // Translate the GeoPoint to screen pixels
        Point screenPts = mapView.getProjection().toPixels(myLocation, null);
    	
    	// Manage marker animation steps
    	if( (when - mAnimationStepTimestamp) >= 900 ) {
    		mAnimationStep++;
    		mAnimationStepTimestamp = when;

    		if( (Float.isNaN(mOrientation))
    				&& (lastFix.getSpeed() <= SPEED_MIN_LIMIT) ) {
    			// If the speed is low and no current orientation is defined,
    			// draw the static location marker
    			mMyLocationBitmap = mStaticLocationBitmaps[mAnimationStep%mStaticLocationBitmaps.length].mOriginalBitmap;
    		}
    		else {
    			if( (lastFix.getSpeed() > SPEED_MIN_LIMIT)
    					|| (Float.isNaN(mOrientation)) ) {
    				// If speed is not low
    				// update current orientation
    				mOrientation = lastFix.getBearing();
    			}
    			int animationIndex = 0;
    			if( mGPSAvailable ) {
    				animationIndex = mAnimationStep%mOrientatedLocationBitmaps.length;
    			}
    			else {
    				animationIndex = 0;
    			}
    			mMyLocationBitmap = mOrientatedLocationBitmaps[animationIndex].getRotatedBitmap(mOrientation);
    		}
    	}
    	
        if( null != mMyLocationBitmap ) {
        	// Draw the marker on the canvas
        	canvas.drawBitmap(
        			mMyLocationBitmap, 
        			screenPts.x - (mMyLocationBitmap.getWidth()  / 2), 
        			screenPts.y - (mMyLocationBitmap.getHeight() / 2), 
        			null
        	);
        }
    }
    
    /**
     * 
     */
    private class MyLocationBitmap {
    	Bitmap mOriginalBitmap = null;
    	private Bitmap mRotatedBitmap = null;
    	private float mRotatedBitmapOrientation = Float.NaN;
    	
    	MyLocationBitmap( Context context, String assetName ) {
    		try {
    			mOriginalBitmap = BitmapFactory.decodeStream(context.getAssets().open(assetName));
			} catch (IOException e) {
				e.printStackTrace();
				mOriginalBitmap = null;
			}
    	}
    	
    	Bitmap getRotatedBitmap( final float orientation ) {
    		if( (null == mRotatedBitmap)
    				|| (mRotatedBitmapOrientation != orientation) ) {
	        	mRotationMatrix.reset();
	        	mRotationMatrix = new Matrix(); 
		        mRotationMatrix.postRotate(orientation);
		        mRotatedBitmap = Bitmap.createBitmap(
		        		mOriginalBitmap,
		        		0, 0,
		        		mOriginalBitmap.getWidth(),
		        		mOriginalBitmap.getHeight(),
		        		mRotationMatrix,
		        		true
		        );
		        mRotatedBitmapOrientation = orientation;
    		}
    		return mRotatedBitmap;
    	}
    }
    
    @Override
    public void onLocationChanged(android.location.Location location) {
    	super.onLocationChanged(location);
    	mGPSAvailable = true;
    }
    
    @Override
    public void onStatusChanged(java.lang.String provider,
            int status,
            android.os.Bundle extras) {
    	PLog.i(TAG,"onStatusChanged - ",status);
    	super.onStatusChanged(provider, status, extras);
    	// Currently GPS status is not correct on RnB4
    	// therefore we don't test it 
    	mGPSAvailable = false;
    }
}