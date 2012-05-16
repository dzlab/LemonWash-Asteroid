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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class LatLngBounds implements Parcelable

{
	/** Attributes */
	private LatLng mSw;
	private LatLng mNe;
	private LatLng mCenter;
	
	/**
	 * Constructor with center position, and lat and lng span.
	 * Constructor used with MapView informations.
	 * @param center Center of bounds
	 * @param latSpanE6 Span between SW and NE latitude, * 100.000
	 * @param lngSpanE6 Span between SW and NE longitude, * 100.000
	 */
	public LatLngBounds(GeoPoint center, int latSpanE6, int lngSpanE6) {
		mCenter = new LatLng(center);
		calculateBounds(latSpanE6, lngSpanE6);
	}
	/**
	 * Constructor with center position, and lat and lng span.
	 * Constructor used with MapView informations.
	 * @param center Center of bounds
	 * @param latSpanE6 Span between SW and NE latitude, * 100.000
	 * @param lngSpanE6 Span between SW and NE longitude, * 100.000
	 */
	public LatLngBounds(LatLng center, int latSpanE6, int lngSpanE6) {
		mCenter = center;
		calculateBounds(latSpanE6, lngSpanE6);
	}
	/**
	 * Constructor with center position, and lat and lng span.
	 * Constructor used with MapView informations.
	 * @param center Center of bounds
	 * @param latSpan Span between SW and NE latitude
	 * @param lngSpan Span between SW and NE longitude
	 */
	public LatLngBounds(LatLng center, double latSpan, double lngSpan) {
		mCenter = center;
		calculateBounds(latSpan, lngSpan);
	}
	/**
	 * Constructor with SW and NE in parameters.
	 * Center will be automatically calculated.
	 * @param sw South East LatLng
	 * @param ne North East LatLng
	 */
	public LatLngBounds(LatLng sw, LatLng ne) {
		mSw = sw;
		mNe = ne;
		calculateCenter();
	}
	/**
	 * Constructor with all attributes of LatLngBounds.
	 * @param sw South West LatLng
	 * @param ne North East LatLng
	 * @param center Center LatLng
	 * @return
	 */
	public LatLngBounds(LatLng sw, LatLng ne, LatLng center) {
		mSw = sw;
		mNe = ne;
		mCenter = center;
	}
	
	/**
	 * Constructor with each South West and North East latitude and longitude.
	 * Center will be automatically calculated.
	 * @param lat_sw latitude of South West
	 * @param lng_sw longitude of South West
	 * @param lat_ne latitude of North East
	 * @param lng_ne longitude of North East
	 */
	public LatLngBounds(double lat_sw, double lng_sw, double lat_ne, double lng_ne) {
		mSw = new LatLng(lat_sw, lng_sw);
		mNe = new LatLng(lat_ne, lng_ne);
		calculateCenter();
	}
	/**
	 * This JSON object is sent from JavaScript (WebView) in method
	 * getLatLngBounds().
	 * 
	 * @param obj
	 * @return
	 * @throws JSONException 
	 */
	public LatLngBounds(JSONObject obj) throws JSONException {
		if (obj.has("sw")) {
			mSw     = new LatLng(obj.getJSONObject("sw"));
			mNe     = new LatLng(obj.getJSONObject("ne"));
			mCenter = new LatLng(obj.getJSONObject("center"));
		}
		/* API V3 bounds */
		else {
			mSw     = new LatLng(obj.getJSONObject("fa").getDouble("b"),
					obj.getJSONObject("U").getDouble("c"));
			mNe     = new LatLng(obj.getJSONObject("fa").getDouble("c"),
					obj.getJSONObject("U").getDouble("b"));
			calculateCenter();
			
		}
	}

	/**
	 * Copy constructor
	 * @param displayBounds
	 */
	public LatLngBounds(LatLngBounds bounds) {
		mCenter = bounds.mCenter.clone();
		mSw = bounds.mSw.clone();
		mNe = bounds.mNe.clone();
	}

	/**
	 * Returns South West of theses bounds. 
	 * @return South West LatLng
	 */
	public LatLng getSw() {
		return mSw.clone();
	}
	
	/**
	 * Returns North East of theses bounds. 
	 * @return North East LatLng
	 */
	public LatLng getNe() {
		return mNe.clone();
	}

	/**
	 * Returns center of theses bounds. 
	 * @return Center LatLng
	 */
	public LatLng getCenter() {
		return mCenter.clone();
	}
	
	public int getLatSpanE6() {
		return (int)(Math.abs( mNe.getLat()-mSw.getLat())*1000000);
	}
	
	public int getLngSpanE6() {
		return (int)(Math.abs( mNe.getLng()-mSw.getLng())*1000000);
	}
	
	/**
	 * Returns if yes or not the given LatLng is contained
	 * in bounds.
	 * @param latlng LatLng to test
	 * @return true if LatLng is contained, false otherwise
	 */
	public boolean contains(LatLng latlng) {
		double lat = latlng.getLat();
		double lng = latlng.getLng();
		return (lat >= mSw.getLat() && lng >= mSw.getLng() &&
				lat <= mNe.getLat() && lng <= mNe.getLng());
	}
	
	public boolean contains(LatLngBounds bounds ) {
		return contains(bounds.mNe) && contains(bounds.mSw);
	}
	
	/**
	 * Extends bounds to the LatLng passed in parameter, if this
	 * one is not contained in bounds.
	 * @param point LatLng to include in bounds
	 * @return LatLng
	 */
	public void extend (LatLng point) {
		if (!contains(point)) {
			mSw = new LatLng(Math.min(mSw.getLat(), point.getLat()),
					Math.min(mSw.getLng(), point.getLng()));
			mNe = new LatLng(Math.max(mNe.getLat(), point.getLat()),
					Math.max(mNe.getLng(), point.getLng()));
			calculateCenter();
		}
	}
	
	/**
	 * Returns true or false, if these bounds intersect another.
	 * @param other Other bounds to test
	 * @return true if two bounds intersects, false otherwise
	 */
	public boolean intersects(LatLngBounds other) {
		int bottom1 = mSw.mLatE6, bottom2 = other.getSw().mLatE6;
		int left1   = mSw.mLngE6, left2   = other.getSw().mLngE6;
		int top1    = mNe.mLatE6, top2    = other.getNe().mLatE6;
		int right1  = mNe.mLngE6, right2  = other.getNe().mLngE6;

		return !((bottom1>top2) || (left1>right2) || (top1<bottom2) || (right1<left2));
	}
	
	/**
	 * Creates new SW and NE to create union with another entire
	 * LatLngBounds.
	 * @param other Other bounds to make union
	 */
	public void union (LatLngBounds other) {
		mSw = new LatLng(Math.min(mSw.getLat(), other.getSw().getLat()),
				Math.min(mSw.getLng(), other.getSw().getLng()));
		mNe = new LatLng(Math.max(mNe.getLat(), other.getNe().getLat()),
				Math.max(mNe.getLng(), other.getNe().getLng()));
		calculateCenter();
	}

	/**
	 * Calculates LatLng center when SW and NE are known.
	 */
	private void calculateCenter() {
		double latCenter = (mSw.getLat() + mNe.getLat()) / 2;
		double lngCenter = (mSw.getLng() + mNe.getLng()) / 2;;
		mCenter = new LatLng(latCenter, lngCenter);
	}
	
	/**
	 * Calculates LatLng SW and NE when center and latSpanE6 and
	 * lngSpanE6 are known.
	 * @param latSpanE6 Latitude span between SW lat and NE lat
	 * @param latSpanE6 Latitude span between SW lng and NE lng
	 */
	private void calculateBounds(int latSpanE6, int lngSpanE6) {
		double deltaLat = (((double)latSpanE6)/1000000)/2;
		double deltaLng = (((double)lngSpanE6)/1000000)/2;
		calculateBounds(deltaLat,deltaLng);
	}
	
	/**
	 * Calculates LatLng SW and NE when center and latSpanE6 and
	 * lngSpanE6 are known.
	 * @param latSpan Latitude span between SW lat and NE lat
	 * @param latSpan Latitude span between SW lng and NE lng
	 */
	private void calculateBounds(double latSpan, double lngSpan) {
		mSw = new LatLng(mCenter.getLat()-latSpan, mCenter.getLng()-lngSpan);
		mNe = new LatLng(mCenter.getLat()+latSpan, mCenter.getLng()+lngSpan);
	}
	
	/**
	 * Returns clone to avoid pointer errors.
	 * @return new LatLngBounds, clone of this
	 */
	@Override
	public LatLngBounds clone() {
		return new LatLngBounds(getSw().clone(), getNe().clone(), getCenter().clone());
	}
	
	@Override
	public boolean equals( Object o ) {
	     // Return true if the objects are identical.
	     if (this == o) {
//		     PLog.i("equals","Same object");
	       return true;
	     }
	     // Return false if the other object has the wrong type.
	     if (!(o instanceof LatLngBounds)) {
	       return false;
	     }
	     // Cast to the appropriate type
	     LatLngBounds lhs = (LatLngBounds) o;
	     // Check each field. Primitive fields, reference fields, and nullable reference
	     // fields are all treated differently.
	     return mSw.equals(lhs.mSw) && mNe.equals(lhs.mNe) && mCenter.equals(lhs.mCenter);
    }
	
	@Override
	public String toString() {
		return new StringBuffer("").append("(sw=" + getSw().toString() + ",ne=" + getNe().toString() + ")").toString();
	}
	
	/**
	 * Calculates the span of bounds and returns it in a String.
	 * A span is two values which are latitude and longitude
	 * length between each parts of bounds.
	 * Example :
	 * -SW.lat = 1       -SW.lng = 10
	 * -NE.lat = 5       -NE.lng = 50
	 * -> lat span = NE.lat - SW.lat = 5  - 1  = 4
	 * -> lng span = NE.lng - SW.lng = 50 - 10 = 40
	 * @return span formated in a string
	 */
	public String toSpan() {
		return new StringBuffer("")
			.append(mNe.getLat() - mSw.getLat())
			.append(",")
			.append(mNe.getLng() - mSw.getLng())
			.toString();
	}
	
	/* Parcelable implementation. */
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    	out.writeParcelable(mSw, flags);
    	out.writeParcelable(mNe, flags);
    	out.writeParcelable(mCenter, flags);
    }

    public static final Parcelable.Creator<LatLngBounds> CREATOR
            = new Parcelable.Creator<LatLngBounds>() {
        public LatLngBounds createFromParcel(Parcel in) {
            return new LatLngBounds(in);
        }

        public LatLngBounds[] newArray(int size) {
            return new LatLngBounds[size];
        }
    };
    
    
    private LatLngBounds(Parcel in) {
        mSw = in.readParcelable(getClass().getClassLoader());
        mNe = in.readParcelable(getClass().getClassLoader());
        mCenter = in.readParcelable(getClass().getClassLoader());
    }
    /* End of parcelable implementation. */
	
}
