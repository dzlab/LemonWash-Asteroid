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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.OverlayItem;
import com.parrot.parrotmaps.directions.DirectionMarker;
import com.parrot.parrotmaps.localsearch.ResultMarker;
import com.parrot.parrotmaps.log.PLog;
import com.parrot.parrotmaps.panoramio.PanoramioMarker;
import com.parrot.parrotmaps.wikipedia.WikipediaMarker;

public class Marker implements Parcelable

{
	public final String TAG = getClass().getSimpleName();
	
	public static enum TYPE {
		RESULT,
		WIKIPEDIA,
		PANORAMIO,
		POSITION,
		DIRECTIONS_START,
		DIRECTIONS_STEP,
		DIRECTIONS_END,
	}
	
	public static class MARKER_ICON {
		public BitmapDrawable drawable;
		public String   fileName;
		public boolean  middleAnchor;
		
		public MARKER_ICON(BitmapDrawable drawable, String fileName, boolean middleAnchor) {
			this.drawable = drawable;
			this.fileName = fileName;
			this.middleAnchor = middleAnchor;
		}
	}

	static private HashMap<TYPE, MARKER_ICON> sHashMapTypes = new HashMap<TYPE, MARKER_ICON>();
	static private AssetManager sAssetManager = null;
	
	
	/** Attributes */
	private final long mId;
	private final TYPE mType;
	private LatLng mLatLng;
	private String mTitle = "";
	private int mZindex = 2;
	private BitmapDrawable mDrawable;
	private String mDrawableFileName;
	private boolean mDrawableIsAsset;
	private boolean mMiddleAnchor;
	private InfoWindow mInfoWindow;

	/**
	 * Initialize markers types.
	 * Shall be called at process init.
	 * @param context
	 */
	static public void initMarkerTypes( Context context ) {
		if( 0 == sHashMapTypes.size() ) {
			addMarkerType(Marker.TYPE.RESULT,           "marker_result.png",           false, context);
			addMarkerType(Marker.TYPE.WIKIPEDIA,        "marker_wikipedia.png",        true,  context);
			addMarkerType(Marker.TYPE.PANORAMIO,        "marker_photos.png",           true,  context);
			addMarkerType(Marker.TYPE.POSITION,         "marker_position.png",         true,  context);
			addMarkerType(Marker.TYPE.DIRECTIONS_START, "marker_directions_start.png", false, context);
			addMarkerType(Marker.TYPE.DIRECTIONS_STEP,  "marker_directions_step.png",  true,  context);
			addMarkerType(Marker.TYPE.DIRECTIONS_END,   "marker_directions_end.png",   false, context);
		}
	}
	
	/**
	 * Adds a marker type with its key (type) and its value (Drawable and its file name).
	 * @param key Marker type
	 * @param fileName fileName
	 * @param middleAnchor true if the middle of the marker will match to the anchor, false for center-bottom matching 
	 * @param context Used to access assets
	 */
	static private void addMarkerType(TYPE key, String fileName, boolean middleAnchor, Context context) {
		try {
			MARKER_ICON value = null;
			sAssetManager = context.getAssets();
			value = new MARKER_ICON(new BitmapDrawable(sAssetManager.open(fileName)), fileName, middleAnchor);
			sHashMapTypes.put(key, value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Constructor
	 * 
	 * @param latlng Position
	 * @param type Marker type (standart icon)
	 * @param filePath if not null, will try to get drawable from this path
	 * @param middleAnchor specifies if marker will point at its middle (true) or bottom (false), only when filePath not null
	 * @param title Title
	 * @param zIndex zIndex
	 * @param infoWindow info window
	 * @return
	 * @throws Exception 
	 */
	public Marker(LatLng latlng, TYPE type, boolean middleAnchor, String title, int zIndex, InfoWindow infoWindow) throws Exception {
		mId = IdGenerator.nextMarkerId();
		mType = type;
		mLatLng = latlng;
		mTitle = title;
		mZindex = zIndex;
		mDrawableIsAsset  = true;
		if( null != sHashMapTypes.get(type) ) {
			mDrawable         = sHashMapTypes.get(type).drawable;
			mDrawableFileName = sHashMapTypes.get(type).fileName;
			mMiddleAnchor     = sHashMapTypes.get(type).middleAnchor;
		}
		else {
			mDrawable         = null;
			mDrawableFileName = "";
			mMiddleAnchor     = false;
		}
		mInfoWindow = infoWindow;
	}
	
	/**
	 * Constructor
	 * 
	 * @param latlng Position
	 * @param type Marker type (standart icon)
	 * @param filePath if not null, will try to get drawable from this path
	 * @param middleAnchor specifies if marker will point at its middle (true) or bottom (false), only when filePath not null
	 * @param title Title
	 * @param zIndex zIndex
	 * @param infoWindow info window
	 * @return
	 * @throws Exception 
	 */
	public Marker( LatLng latlng,
				   TYPE type,
				   String assetName,
				   String title,
				   int zIndex,
				   InfoWindow infoWindow) throws Exception {
		mId = IdGenerator.nextMarkerId();
		mType = type;
		mLatLng = latlng;
		mTitle = title;
		mZindex = zIndex;
		mDrawableIsAsset  = true;
		if( null != sHashMapTypes.get(type) ) {
			mDrawable         = sHashMapTypes.get(type).drawable;
			mDrawableFileName = sHashMapTypes.get(type).fileName;
			mMiddleAnchor     = sHashMapTypes.get(type).middleAnchor;
		}
		else {
			mDrawable         = null;
			mDrawableFileName = "";
			mMiddleAnchor     = false;
		}
		if( null != assetName ) {
			mDrawable         = new BitmapDrawable(sAssetManager.open(assetName));
			mDrawableFileName = assetName;
		}
		mInfoWindow = infoWindow;
	}
	
	public Marker(LatLng latlng, TYPE type, URL url, boolean middleAnchor, String title, int zIndex, InfoWindow infoWindow) throws Exception {
		mId = IdGenerator.nextMarkerId();
		mType = type;
		mLatLng = latlng;
		mTitle = title;
		mZindex = zIndex;
		mDrawableIsAsset = false;
		mDrawable = new BitmapDrawable(url.openStream());
		if (mDrawable == null) {
			mDrawable = sHashMapTypes.get(type).drawable;
		}
		mDrawableFileName = url.toString();
		mMiddleAnchor = middleAnchor;
		mInfoWindow = infoWindow;
	}
	
    /**
     * Create a Marker object from a parcel.
     * The type has to be already extracted from the Parcel.	
     * @param in Parcel to read.
     * @param type Type of marker.
     */
    protected Marker(Parcel in, TYPE type) {
    	mType = type;
    	mId = in.readLong();
    	mLatLng = in.readParcelable(getClass().getClassLoader());
    	mTitle = in.readString();
    	mZindex = in.readInt();
    	mDrawableFileName = in.readString();
    	if( in.readInt() == 0 ) {
    		mDrawableIsAsset = false;
    	} else {
    		mDrawableIsAsset = true;
    	}
    	mMiddleAnchor = Boolean.getBoolean(in.readString());
    	mInfoWindow = in.readParcelable(getClass().getClassLoader());
    	
    	if (mDrawableFileName == null) {
			mDrawable = null;
		}
    	else if( mDrawableIsAsset ) {
    		if( null != sAssetManager ) {
    			try {
    				mDrawable = new BitmapDrawable(sAssetManager.open(mDrawableFileName));
    			} catch (IOException e) {
    				PLog.e(TAG, "Failed to open stream from asset drawable file");
    				e.printStackTrace();
    			}
    		}
    	} else {
            ByteArrayInputStream bais = new  ByteArrayInputStream(in.createByteArray());
            mDrawable = new BitmapDrawable(bais); 
		}
    }
	
	
	/**
	 * Gets Marker unique ID.
	 * @return Marker ID
	 */
	public long getId() {
		return mId;
	}
	/**
	 * Gets Marker position.
	 * @return Marker position
	 */
	public LatLng getLatLng() {
		return mLatLng.clone();
	}
	/**
	 * Gets Marker title, if exists, null otherwise.
	 * @return Title or null
	 */
	public String getTitle() {
		return mTitle;
	}
	/**
	 * Gets zIndex, specifying level on map, when multiple
	 * points are drawn.
	 * @return zIndex
	 */
	public int getZindex() {
		return mZindex;
	}
	/**
	 * Gets Drawable linked to this Marker.
	 * @return Drawable
	 */
	public BitmapDrawable getDrawable() {
		return mDrawable;
	}
	
	/**
	 * Gets file name of Drawable linked to this Marker.
	 * @return String
	 */
	public String getFileName() {
		return mDrawableFileName;
	}
	
	/**
	 * Returns true if this marker must be anchored at the middle,
	 * or at the middle bottom, on a map.
	 * @return true for a middle anchored Marker, false for a middle anchored Marker
	 */
	public boolean getMiddleAnchor() {
		return mMiddleAnchor;
	}
	
	/**
	 * Gets InfoWindow linked to this Marker.
	 * If doesn't exist, returns null.
	 * @return InfoWindow or null if doesn't exist
	 */
	public InfoWindow getInfoWindow() {
		return mInfoWindow;
	}
	/**
	 * Specifies what to do, when user just selected this marker
	 * linked InfoWindow.
	 */
	public void onClick() {
	}
	
	/**
	 * Returns a JavaScript JSONObject of this marker in order
	 * to draw it on webView.
	 * Used in webView.drawMarker() function.
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getJSONObject() throws JSONException {
		JSONObject jMarker = new JSONObject();
		JSONObject jlatlng = new JSONObject();
		jlatlng.put("lat", mLatLng.getLat());
		jlatlng.put("lng", mLatLng.getLng());
		
		jMarker.put("middleAnchor", mMiddleAnchor);
		jMarker.put("width", mDrawable.getIntrinsicWidth());
		jMarker.put("height", mDrawable.getIntrinsicHeight());
		jMarker.put("id", mId);
		jMarker.put("latlng", jlatlng);
		jMarker.put("title", mTitle);
		jMarker.put("zIndex", mZindex);
		jMarker.put("fileName", mDrawableFileName);
		
		return jMarker;
	}
	
	/**
	 * Gets OverlayItem object, used by a MapView.
	 * @return new item to be displayed on MapView
	 */
	public OverlayItem getOverlayItem () {
		return new OverlayItem(getLatLng().getGeoPoint(), getTitle(), String.valueOf(getId()));
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer("");
		buf.append("-- Begin Marker --\n");
		buf.append("mId=").append(mId);
		buf.append("mTitle=").append(mTitle);
		buf.append("mZindex=").append(mZindex);
		buf.append("mDrawable=").append(mDrawable);
		buf.append("mFileName=").append(mDrawableFileName);
		buf.append("mMiddleAnchor=").append(mMiddleAnchor);
		buf.append("mInfoWindow=").append(mInfoWindow);
		buf.append("-- End Marker --\n");
		return buf.toString();
	}
	
	
	/* Parcelable implementation. */
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    	out.writeString(mType.name());
    	out.writeLong(mId);
    	out.writeParcelable(mLatLng, flags);
    	out.writeString(mTitle);
    	out.writeInt(mZindex);
    	out.writeString(mDrawableFileName);
    	out.writeInt( mDrawableIsAsset ? 1 : 0 );
    	out.writeString(Boolean.toString(mMiddleAnchor));
    	out.writeParcelable(mInfoWindow, flags);
    	if( !mDrawableIsAsset ) {
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		getDrawable().getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, baos);
    		out.writeByteArray(baos.toByteArray());
    	}
    }

    public static final Parcelable.Creator<Marker> CREATOR
            = new Parcelable.Creator<Marker>() {
        public Marker createFromParcel(Parcel in) {
        	final Marker marker;
        	TYPE type = Enum.valueOf(TYPE.class,in.readString());
        	if( type == TYPE.PANORAMIO ) {
        		marker = new PanoramioMarker( in );
        	}
        	else if( type == TYPE.WIKIPEDIA ) {
        		marker = new WikipediaMarker( in );
        	}
        	else if( type == TYPE.RESULT ) {
        		marker = new ResultMarker( in );
        	}
        	else if( (type == TYPE.DIRECTIONS_END)
        			|| (type == TYPE.DIRECTIONS_START)
        			|| (type == TYPE.DIRECTIONS_STEP) ) {
        		marker = new DirectionMarker( in, type );
        	}
        	else {
        		marker = new Marker( in, type );
        	}
            return marker;
        }

        public Marker[] newArray(int size) {
            return new Marker[size];
        }
    };
	
	public TYPE getType() {
		return mType;
	}

	public void processAction(Context context,Controller controller) {
		PLog.i(TAG,"processAction - do nothing");
	}

	private int mInfoWindowHeight = 0;
	public void setInfoWindowHeight(int height) {
		mInfoWindowHeight = height;		
	}
	public int getInfoWindowHeight() {
		return mInfoWindowHeight;
	}
	
	
	public void setAsset(String assetName)
	{
		if( null != assetName ) {
			if (mDrawable != null)
			{
				mDrawable.setCallback(null);
				mDrawable = null;
			}
			try {
				mDrawable         = new BitmapDrawable(sAssetManager.open(assetName));
				mDrawableFileName = assetName;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}
