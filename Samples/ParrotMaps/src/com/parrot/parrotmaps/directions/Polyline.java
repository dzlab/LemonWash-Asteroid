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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

import com.parrot.parrotmaps.IdGenerator;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.LatLngBounds;
import com.parrot.parrotmaps.log.PLog;

public class Polyline implements Parcelable

{
	/** Attributes */
	private final String TAG = this.getClass().getSimpleName();
	private static final int NB_SUB_PARTS = 10;
	private final long   mId;
	private ArrayList<LatLng> mPath;
	private ArrayList<Integer> mLevels;
	private String mColor = "#0000FF";
	private double mOpacity = 0.70;
	private double mWeight = 4;
	private int mZindex = 1;
	private LatLngBounds mBounds = null;
	private ArrayList<Polyline> mParts = null;

	/**
	 * Constructor
	 * 
	 * @param obj
	 * @return
	 * @throws JSONException
	 */
	public Polyline(JSONObject obj) throws JSONException, Exception {
		mId = IdGenerator.nextPolylineId();
		String encodedPoints = obj.getString("points");
		String encodedLevels = obj.getString("levels");
		decodePoints(encodedPoints);
		decodeLevels(encodedLevels);
		initParts();
	}
		
	public Polyline(Polyline polyline, int startIndex, int endIndex) throws Exception {
		mId = IdGenerator.nextMarkerId();
		if (startIndex >= 0 && endIndex >= 0 && startIndex <= endIndex ||
				polyline.getPath().size()-1 <= Math.max(startIndex, endIndex)) {	
			mPath   = new ArrayList<LatLng>(polyline.getPath().subList(startIndex, endIndex+1));
			mLevels = new ArrayList<Integer>(polyline.getLevels().subList(startIndex, endIndex+1));
			mColor = polyline.getColor();
			mOpacity = polyline.getOpacity();
			mWeight = polyline.getWeight();
			mZindex = polyline.getZindex();
			Iterator<LatLng> it = mPath.iterator();
			boolean first = true;
			while (it.hasNext()) {
				LatLng latlng = it.next();
				if (first) {
					mBounds = new LatLngBounds(latlng, latlng);
					first = false;
				}
				else {
					mBounds.extend(latlng);
				}
			}
		}
		else {
			throw new Exception("Bad indexes in Polyline Constructor : start = "+startIndex +
					", end = "+endIndex+", polyline size = "+polyline.getPath().size()+".");
		}
	}
	/**
	 * Operation
	 * 
	 * @return ArrayList<Marker>
	 */
	public long getId() {
		return mId;
	}

	
	/**
	 * Operation
	 * 
	 * @return ArrayList<LatLng>
	 */
	public ArrayList<LatLng> getPath() {
		return mPath;
	}
	/**
	 * Operation
	 * 
	 * @return ArrayList<Integer>
	 */
	public ArrayList<Integer> getLevels() {
		return mLevels;
	}
	
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getColor() {
		return mColor;
	}
	/**
	 * Operation
	 * 
	 * @param color
	 */
	public void setColor(String color) {
		if (checkColor(color)) {
			mColor = color;
		}
	}
	/**
	 * Operation
	 * 
	 * @return double
	 */
	public double getOpacity() {
		return mOpacity;
	}
	/**
	 * Operation
	 * 
	 * @param opacity
	 */
	public void setOpacity(double opacity) {
		mOpacity = opacity;
	}
	/**
	 * Operation
	 * 
	 * @return double
	 */
	public double getWeight() {
		return mWeight;
	}
	/**
	 * Operation
	 * 
	 * @param weight
	 */
	public void setWeight(double weight) {
		mWeight = weight;
	}
	/**
	 * Operation
	 * 
	 * @return int
	 */
	public int getZindex() {
		return mZindex;
	}
	/**
	 * Operation
	 * 
	 * @param zindex
	 */
	public void setZindex(int zindex) {
		if (zindex >= 0 && zindex < 256) {
			mZindex = zindex;
		} else {
			PLog.e(TAG, "Invalid new Zindex for polyline : ", zindex);
		}
	}
	/**
	 * Operation
	 * 
	 * @return LatLngBounds
	 */
	public LatLngBounds getBounds() {
		return mBounds.clone();
	}
	public LatLngBounds getBoundsRef() {
		return mBounds;
	}
	/**
	 * Operation
	 * 
	 * @param encodedPoints
	 * @return
	 */
	private void decodePoints(String encodedPoints) {
		if (encodedPoints != null) {
//			encodedPoints = encodedPoints.replace("\\\\", "\\");

			int len = encodedPoints.length();
			int index = 0;
			mPath = new ArrayList<LatLng>();
			double lat = 0;
			double lng = 0;
			boolean first = true;

			while (index < len) {
				int b;
				int shift = 0;
				int result = 0;
				do {
					b = encodedPoints.codePointAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while ((b >= 0x20) && (index < len) );
				int dlat = (((result & 1) > 0) ? ~(result >> 1) : (result >> 1));
				lat += dlat;

				shift = 0;
				result = 0;
				if( index < len ) {
					do {
						b = encodedPoints.codePointAt(index++) - 63;
						result |= (b & 0x1f) << shift;
						shift += 5;
					} while ((b >= 0x20) && (index < len) );
				}
				int dlng = (((result & 1) > 0) ? ~(result >> 1) : (result >> 1));
				lng += dlng;

				LatLng newPoint = new LatLng(lat * 1e-5, lng * 1e-5);
				mPath.add(newPoint);
				if (first) {
					mBounds = new LatLngBounds(newPoint, newPoint);
					first = false;
				}
				else {
					mBounds.extend(newPoint);
				}
			}
		}
	}

	private void decodeLevels(String encodedLevels) {
		/*
		 * http://facstaff.unca.edu/mcmcclur/googlemaps/encodepolyline/description.html
		character  	P  	O  	N  	M  	L  	K  	J  	I  	H  	G  	F  	E  	D  	C  	B  	A  	@  	?
		map level 	0 	1 	2 	3 	4 	5 	6 	7 	8 	9 	10 	11 	12 	13 	14 	15 	16 	17
		line level 	17 	16 	15 	14 	13 	12 	11 	10 	9 	8 	7 	6 	5 	4 	3 	2 	1 	0
		*/
		mLevels = new ArrayList<Integer>();
		
		for (int pointIndex = 0; pointIndex < encodedLevels.length(); ++pointIndex) {
			int pointLevel = encodedLevels.codePointAt(pointIndex) - '?';
			//int mapLevel = 17 - 3 - pointLevel;
			//int mapLevel = (17-pointLevel)-7;
			int mapLevel;
			if( pointLevel == 0 )
			{
				mapLevel = 15;
			}
			else if( pointLevel == 1 )
			{
				mapLevel = 11;
			}
			else if( pointLevel == 2 )
			{
				mapLevel = 8;
			}
			else
			{
				mapLevel = 0;
			}
			mLevels.add(mapLevel);
		}
	}
	
	/**
	 * Checks whether the given string matches as a color.
	 * @param color String to test
	 * @return true if color is correct, false otherwise
	 */
	private boolean checkColor(String color) {
		Pattern p = Pattern.compile("^#[0-9a-f]{6}$");
		Matcher m = p.matcher(color);
		return m.matches();
	}
	
	
	/**
	 * @return List of small polylines of partSize size
	 */
	public ArrayList<Polyline> getParts() {
		if( null == mParts ) {
			initParts();
		}
		return mParts;
	}
	
	/**
	 * Creates one or more polylines with partSize size.
	 * Useful to separate one long polyline into several small others.
	 * The last part will be sized with the number of points left.
	 */
	public void initParts() {
		if (NB_SUB_PARTS < 1) {
			PLog.e(TAG, "Impossible to divide a polyline into ",NB_SUB_PARTS," parts.");
		}
		else {
			try {
				mParts = new ArrayList<Polyline>();
				int nbParts = mPath.size()/NB_SUB_PARTS;
				int rest    = mPath.size()%NB_SUB_PARTS;
				int i=0;
				int startIndex;
				int endIndex;
				while (i < nbParts) {
					startIndex = i*NB_SUB_PARTS;
					endIndex   = startIndex + NB_SUB_PARTS - 1;
					mParts.add(new Polyline(this, startIndex, endIndex));
					i++;
				}
				if (rest > 0) {
					startIndex = nbParts*NB_SUB_PARTS;
					endIndex   = startIndex + rest - 1;
					mParts.add(new Polyline(this, startIndex, endIndex));
				}
			} catch (Exception e) {
				PLog.e(TAG, "Error while attempting to get parts of polyline.");
			}
		}
	}
	
	/**
	 * Returns a JavaScript JSONObject of this polyline in order
	 * to draw it on webView.
	 * Used in webView.drawPolyline() function.
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getJSONObject() throws JSONException {
		JSONObject jPolyline = new JSONObject();
		jPolyline.put("id", mId);
		jPolyline.put("color", mColor);
		jPolyline.put("opacity", mOpacity);
		jPolyline.put("weight", mWeight);
		jPolyline.put("zindex", mZindex);
		
		/* Creating list of LatLng */
		JSONArray jArrayLatLng = new JSONArray();
		Iterator<LatLng> itPath = mPath.iterator();
		while (itPath.hasNext()) {
			LatLng     latlng  = itPath.next();
			JSONObject jlatlng = new JSONObject();
			jlatlng.put("lat", latlng.getLat());
			jlatlng.put("lng", latlng.getLng());
			jArrayLatLng.put(jlatlng);
		}
		jPolyline.put("path", jArrayLatLng);
		
		/* Creating list of Levels */
		JSONArray jArrayLevels = new JSONArray();
		Iterator<Integer> itLevel = mLevels.iterator();
		while (itLevel.hasNext()) {
			int level = itLevel.next();
			jArrayLevels.put(level);
		}
		jPolyline.put("levels", jArrayLevels);
		
		return jPolyline;
	}
	
	/**
	 * Gets orientation in degrees between the 
	 * first and the second point of the polyline.
	 * Useful to param StreetView panorama function.
	 * Value returned is between 0 and 359°
	 * @return orientation in degrees
	 */
	public int getFirstOrientation () {
		int orientation = 0;
		/* Calculating angle to use for StreetView */
		if (mPath != null && mPath.size()>=2) {
			LatLng A = mPath.get(0);
			LatLng B = mPath.get(1);
			double oppose   = B.getLat()-A.getLat();
			double adjacent = B.getLng()-A.getLng();
			orientation = (int)(Math.toDegrees(Math.atan2(adjacent, oppose)));
			orientation = (orientation + 360)%360;
		}
		return orientation;
	}
	
	/* Parcelable implementation. */
	public int describeContents() {
        return 0;
    }

	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(mId);
		out.writeTypedList(mPath);
		
		int[] tab = new int[mLevels.size()];
		for (int i=0 ; i<mLevels.size() ; i++) {
			tab[i] = mLevels.get(i);
		}		
		out.writeIntArray(tab);
		
		out.writeString(mColor);
		out.writeDouble(mOpacity);
		out.writeDouble(mWeight);
		out.writeInt(mZindex);
		out.writeParcelable(mBounds, flags);
    }

    public static final Parcelable.Creator<Polyline> CREATOR
            = new Parcelable.Creator<Polyline>() {
        public Polyline createFromParcel(Parcel in) {
            return new Polyline(in);
        }

        public Polyline[] newArray(int size) {
            return new Polyline[size];
        }
    };
    
    
    private Polyline(Parcel in) {
    	mId = in.readLong();
    	mPath = new ArrayList<LatLng>();
    	in.readList(mPath, getClass().getClassLoader());
		
		int[] tab = new int[1];
		in.readIntArray(tab);
		mLevels = new ArrayList<Integer>();
		for (int i=0 ; i<tab.length ; i++) {
			mLevels.add(tab[i]);
		}
		
		mColor = in.readString();
		mOpacity = in.readDouble();
		mWeight = in.readDouble();
		mZindex = in.readInt();
		mBounds = in.readParcelable(getClass().getClassLoader());
    }
    /* End of parcelable implementation. */
}
