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

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class LatLng implements Parcelable

{
	/** Attributes */
	private double mLat;
	private double mLng;
	public int mLatE6;
	public int mLngE6;
	GeoPoint geoPoint;
	
	/**
	 * Operation
	 * 
	 * @param lat
	 * @param lng
	 * @return
	 */
	public LatLng(double lat, double lng) {
		mLat = lat;
		mLng = lng;
		mLatE6 = (int) (mLat * 1000000);
		mLngE6 = (int) (mLng * 1000000);
		geoPoint = new GeoPoint(mLatE6, mLngE6);
	}
	/**
	 * Operation
	 * 
	 * @param geopoint
	 * @return
	 */
	public LatLng(GeoPoint geopoint) {
		mLat = ((double)geopoint.getLatitudeE6()) / 1000000;
		mLng = ((double)geopoint.getLongitudeE6()) / 1000000;
		mLatE6 = geopoint.getLatitudeE6();
		mLngE6 = geopoint.getLongitudeE6();
		geoPoint = new GeoPoint(mLatE6, mLngE6);
	}
	/**
	 * Operation
	 * 
	 * @param location
	 * @return
	 */
	public LatLng(Location location) {
		mLat = location.getLatitude();
		mLng = location.getLongitude();
		mLatE6 = (int) (mLat * 1000000);
		mLngE6 = (int) (mLng * 1000000);
		geoPoint = new GeoPoint(mLatE6, mLngE6);
	}
	/**
	 * Operation
	 * 
	 * @param latE6
	 * @param lngE6
	 * @return
	 */
	public LatLng(int latE6, int lngE6) {
		mLat = ((double)latE6) / 1000000;
		mLng = ((double)lngE6) / 1000000;
		mLatE6 = latE6;
		mLngE6 = lngE6;
		geoPoint = new GeoPoint(mLatE6, mLngE6);
	}
	/**
	 * Operation
	 * 
	 * @param obj
	 * @return
	 * @throws JSONException 
	 */
	public LatLng(JSONObject obj) throws JSONException {
		if (obj.has("lat")) {
			mLat = obj.getDouble("lat");
			mLng = obj.getDouble("lng");
		}
		/* API V3 */
		else {
			mLat = obj.getDouble("b");
			mLng = obj.getDouble("c");
		}
		mLatE6 = (int) (mLat * 1000000);
		mLngE6 = (int) (mLng * 1000000);
		geoPoint = new GeoPoint(mLatE6, mLngE6);
	}
	
	public LatLng(Bundle bundle) {
		mLat = bundle.getDouble("lat", 0);
		mLng = bundle.getDouble("lng", 0);
		mLatE6 = (int) (mLat * 1000000);
		mLngE6 = (int) (mLng * 1000000);
		geoPoint = new GeoPoint(mLatE6, mLngE6);
	}
	
	/**
	 * Operation
	 * 
	 * @return double
	 */
	public double getLat() {
		return mLat;
	}
	/**
	 * Operation
	 * 
	 * @return double
	 */
	public double getLng() {
		return mLng;
	}
	/**
	 * Operation
	 * 
	 * @return GeoPoint
	 */
	public GeoPoint getGeoPoint() {
//		return new GeoPoint((int) (mLat * 1000000), (int) (mLng * 1000000));
		return geoPoint;
	}
	
	public boolean equals (LatLng other) {
		return (other.mLat == mLat) && (other.mLng == mLng);
	}
	
	@Override
	public LatLng clone () {
		return new LatLng(mLat, mLng);
	}
	
	@Override
	public String toString() {
		return new StringBuffer("(").append(getLat() + "," + getLng()).append(")").toString();
	}
	
	/* Parcelable implementation. */
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    	out.writeDouble(mLat);
    	out.writeDouble(mLng);
    	out.writeInt(mLatE6);
    	out.writeInt(mLngE6);
    }

    public static final Parcelable.Creator<LatLng> CREATOR
            = new Parcelable.Creator<LatLng>() {
        public LatLng createFromParcel(Parcel in) {
            return new LatLng(in);
        }

        public LatLng[] newArray(int size) {
            return new LatLng[size];
        }
    };
    
    
    private LatLng(Parcel in) {
        mLat = in.readDouble();
        mLng = in.readDouble();
		mLatE6 = in.readInt();
		mLngE6 = in.readInt();
		geoPoint = new GeoPoint(mLatE6, mLngE6);
    }
    /* End of parcelable implementation. */

}
