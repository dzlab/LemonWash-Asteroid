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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.parrot.parrotmaps.Marker.TYPE;
import com.parrot.parrotmaps.directions.Polyline;
import com.parrot.parrotmaps.localsearch.LocalSearchLayerInterface.SEARCH_RESULT;
import com.parrot.parrotmaps.log.PLog;

/** 
 *
 */
public class Layer extends ItemizedOverlay<OverlayItem> implements LayerInterface, Parcelable

{
	/** Attributes */
	public  final String TAG = this.getClass().getSimpleName();
	private final long   mId;
	protected LinkedList<Marker> mMarkersList;
	protected LinkedList<Polyline> mPolylinesList;
	protected LatLngBounds mBounds = null;
	protected List<List<Path>> mPathList = null;
	
	/** Attributes */
	private DisplayAbstract   mContext   = null;
	protected Controller      mController = null;

	private Marker mInfoWindowMarker = null;
	private InfoWindowDrawer mInfoWindowDrawer = null;
	
	private ArrayList<Marker> mMarkersTapped = null;
	
	public Layer(DisplayAbstract context, Controller controller) throws Exception {
		/* Default marker, if no other marker is associated to an item. */
        super(boundCenterBottom(Drawable.createFromStream(context.getAssets().open("marker_other.png"), "")));
		mContext = context;
		mController = controller;
		mId = IdGenerator.nextLayerId();
		mMarkersList = new LinkedList<Marker>();
		mPolylinesList = new LinkedList<Polyline>();
	    
		populate();
	}

	public long getId() {
		return mId;
	}

	@SuppressWarnings("unchecked")
	public synchronized LinkedList<Marker> getMarkersList() {
		LinkedList<Marker> copy = null;
		if (mMarkersList != null) {
			copy = (LinkedList<Marker>) mMarkersList.clone();
		}
		return copy;
	}

	public synchronized void setMarkersList(LinkedList<Marker> list) {
		mMarkersList = list;
		setLastFocusedIndex(-1);
		remakeBounds();
		populate();
	}

	@SuppressWarnings("unchecked")
	public synchronized LinkedList<Polyline> getPolylinesList() {
		LinkedList<Polyline> copy = null;
		if (mPolylinesList != null) {
			copy = (LinkedList<Polyline>) mPolylinesList.clone();
		}
		return copy;
	}

	public synchronized void addMarker(Marker marker) {
		if (mMarkersList == null) {
			mMarkersList = new LinkedList<Marker>();
		}
		if (mBounds == null) {
			mBounds = new LatLngBounds(marker.getLatLng(), marker.getLatLng());
		}
		else {
			mBounds.extend(marker.getLatLng());
		}
		mMarkersList.add(marker);
	}

	public synchronized void removeMarker(Marker marker) {
		if (mMarkersList == null) {
			PLog.e(TAG, "Impossible to remove a marker from an empty list in this layer.");
		}
		else {
			/* Removing marker from the list */
			mMarkersList.remove(marker);
	
			/* And test if this marker was on a border of the rectangle bounds.
			 * If yes, we have to reduce bounds. */
			if (marker.getLatLng().getLat() == mBounds.getSw().getLat() ||
					marker.getLatLng().getLat() == mBounds.getNe().getLat() ||
					marker.getLatLng().getLng() == mBounds.getSw().getLng() ||
					marker.getLatLng().getLng() == mBounds.getNe().getLng()) {
				remakeBounds();
			}
			populate();
		}
	}

	public synchronized void addPolyline(Polyline polyline) {
		if (mPolylinesList == null) {
			mPolylinesList = new LinkedList<Polyline>();
		}
		if (mBounds == null) {
			mBounds = polyline.getBounds();
		}
		else {
			mBounds.union(polyline.getBounds());
		}
		mPolylinesList.add(polyline);
	}
	
	public synchronized void removePolyline(Polyline polyline) {
		if (mPolylinesList == null) {
			PLog.e(TAG, "Impossible to remove a polyline from an empty list in this layer.");
		}
		else {
			/* Removing polyline from the list */
			mPolylinesList.remove(polyline);
			
			/* Too much test to check if bounds need to be recalculated.
			 * Forcing doing it.
			 */
			remakeBounds();
		}
	}

	public LatLngBounds getBoundsCenteredOnMarker (Marker centralMarker) {
		LatLngBounds bounds = null;
		if( (null != centralMarker)
				&& (null != mMarkersList)
				&& (mMarkersList.size() > 0)) {
			LatLng center = centralMarker.getLatLng();
			double maxLatSpan = 0.0;
			double maxLngSpan = 0.0;
			for( Marker marker : mMarkersList ) {
				double latDelta = Math.abs(marker.getLatLng().getLat()-center.getLat());
				double lngDelta = Math.abs(marker.getLatLng().getLng()-center.getLng());
				if( latDelta > maxLatSpan ) {
					maxLatSpan = latDelta;
				}
				if( lngDelta > maxLngSpan ) {
					maxLngSpan = lngDelta;
				}
			}
			bounds = new LatLngBounds(center,maxLatSpan,maxLngSpan);
		}
		return bounds;
	}
	
	public synchronized void removeAllMarkers() {
		if (mMarkersList != null) {
			mMarkersList.clear();
		}
		populate();
		remakeBounds();
	}

	public synchronized void removeAllPolylines() {
		if (mPolylinesList != null) {
			mPolylinesList.clear();
		}
		remakeBounds();
	}
	
	public synchronized void clearLayer() {
		if (mMarkersList != null) {
			mMarkersList.clear();
		}
		if (mPolylinesList != null) {
			mPolylinesList.clear();
		}
		mMarkersList   = null;
		mPolylinesList = null;
		mBounds        = null;
		populate();
	}

	/**
	 * Returns a JavaScript JSONObject of this marker in order
	 * to draw it on webView.
	 * Used in webView.drawMarker() function.
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getJSONObject() throws JSONException {
		JSONObject jLayer = new JSONObject();
		JSONArray  jMarkersArray   = new JSONArray();
		JSONArray  jPolylinesArray = new JSONArray();
		
		/* Building all JSON markers... */
		if (mMarkersList != null) {
			Iterator<Marker> itMarker = mMarkersList.iterator();
			while (itMarker.hasNext()) {
				jMarkersArray.put(itMarker.next().getJSONObject());
			}
		}
		
		/* Building all JSON polylines... */
		if (mPolylinesList != null) {
			Iterator<Polyline> itPolyline = mPolylinesList.iterator();
			while (itPolyline.hasNext()) {
				jPolylinesArray.put(itPolyline.next().getJSONObject());
			}
		}
		
		jLayer.put("id",        mId);
		jLayer.put("markers",   jMarkersArray);
		jLayer.put("polylines", jPolylinesArray);
		return jLayer;
	}

	/**
	 * Prepare bitmap for info window of a marker.
	 * @param marker The marker.
	 * @return The height of the bitmap in pixels.
	 */
	public int openInfoWindow (Marker marker) {
//		PLog.i(TAG,"openInfoWindow");
		mInfoWindowDrawer = new InfoWindowDrawer();
		// Find and give focus to the associated overlay item
		// this item will by raised over others items
		for( int i=0; i<mMarkersList.size(); i++ ) {
			Marker m = mMarkersList.get(i);
			if( marker == m ) {
				setFocus( getItem(i) );
				break;
			}
		}
		// Prepare info window drawing
		// and return its height
		return mInfoWindowDrawer.openInfoWindow(marker);
	}
	
	public void closeInfoWindow() {
		mInfoWindowDrawer = null;
	}

	/**
	 * This method remakes bounds of a layer, useful when a marker or polyline is just deleted.
	 * This will update the new smaller bounds.
	 */
	protected synchronized void remakeBounds() {
		mBounds = null;
		if (mMarkersList != null) {
			Iterator<Marker> itMarkers = mMarkersList.iterator();
			while (itMarkers.hasNext()) {
				Marker marker = itMarkers.next();
				if (mBounds == null) {
					mBounds = new LatLngBounds(marker.getLatLng(), marker.getLatLng());
				}
				else {
					mBounds.extend(marker.getLatLng());
				}
			}
		}
		if (mPolylinesList != null) {
			Iterator<Polyline> itPolylines = mPolylinesList.iterator();
			while (itPolylines.hasNext()) {
				Polyline polyline = itPolylines.next();
				if (mBounds == null) {
					mBounds = polyline.getBounds();
				}
				else {
					mBounds.union(polyline.getBounds());
				}
			}
		}
	}

	@Override
	protected synchronized OverlayItem createItem(int i) {
		OverlayItem item = null;
		if( (null != mMarkersList)
				&& (mMarkersList.size() > i) ) {
			Marker marker = mMarkersList.get(i);
			item = marker.getOverlayItem();
			
			/* Getting OverlayItem */
			BitmapDrawable drawable = marker.getDrawable();
			if (drawable != null) {
				BitmapDrawable mapDrawable = new BitmapDrawable(drawable.getBitmap());
				/* Anchor at the bottom or at the middle. */
				if (marker.getMiddleAnchor()) {
					item.setMarker(boundCenter(mapDrawable));
				}
				else {
					item.setMarker(boundCenterBottom(mapDrawable));
				}
			}
		}
		
		return item;
	}

	@Override
	public synchronized int size() {
		int size = 0;
		if (mMarkersList != null) {
			size = mMarkersList.size();
		}
		return size;
	}
	
	private long mWhenLastDraw = 0;
	
	@Override
	public synchronized boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
//		PLog.e(TAG,"draw - when "+when+" shadow "+shadow);

		if( (null!=mPolylinesList)
				&& (mPolylinesList.size() > 0) ) {
			// In the case of directions layer, we have a polyline
			// add markers to draw, at each map view refresh
			// Layer.draw is called 2 times
			// the first time, the polyline is drawn
			// the second time, the markers are drawn
			if( !shadow
					&& (mWhenLastDraw != when) ) {
				// Draw path
				drawPolyline( canvas, mapView, shadow );
				mWhenLastDraw = when;
			}
			else {
				// Draw markers
				drawMarkers( canvas, mapView, shadow, when );
			}
		}
		else {
			// Draw markers
			drawMarkers( canvas, mapView, shadow, when );
		}
		
		boolean animatedInfoWindow = false;
		if( null != mInfoWindowDrawer ) {
			animatedInfoWindow = mInfoWindowDrawer.mAnimatedInfoWindow;
		}
		return animatedInfoWindow;
	}
	
	/**
	 * Draw markers.
	 * @param canvas
	 * @param mapView
	 * @param shadow
	 * @param when
	 */
	private synchronized void drawMarkers(Canvas canvas, MapView mapView, boolean shadow, long when) {
//		PLog.e(TAG,"drawMarkers - when "+when+" shadow "+shadow);
		// Let the ItemizedOverlay superclass draw each marker, after polylines.
		// Don't draw shadow for item that are centered on the position
		if (!(shadow
				&& (mMarkersList!=null)
				&& (mMarkersList.size()>0)
				&& (mMarkersList.get(0).getMiddleAnchor())) ) {
			super.draw(canvas, mapView, shadow);
		}

		if (!shadow) {
			Projection projection = mapView.getProjection();
			// We can draw InfoWindow if it has been prepared.
			if (mInfoWindowDrawer != null) {
				mInfoWindowDrawer.updateInfoWindow(when);
				mInfoWindowDrawer.drawInfoWindow(canvas, projection);
			}
		}
	}
	
	/**
	 * Draw polyline
	 * @param canvas
	 * @param mapView
	 * @param shadow
	 */
	private synchronized void drawPolyline(Canvas canvas, MapView mapView, boolean shadow) {
//		PLog.e(TAG,"drawPolyline - shadow "+shadow);
		if( shadow ) {
			// No shadow for polyline
			return;
		}
		if( (null==mPolylinesList)
				|| (mPolylinesList.size() <= 0) ) {
			// No polyline to draw
			return;
		}
		
		Projection projection = mapView.getProjection();
		LatLngBounds bounds = new LatLngBounds(mapView.getMapCenter(),
				mapView.getLatitudeSpan(), mapView.getLongitudeSpan());
		
		Paint paint = new Paint();
		paint.setARGB(200, 100, 100, 255);
		paint.setStrokeWidth((float) (5));
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		GeoPoint tl = projection.fromPixels(0, 0);
		GeoPoint br = projection.fromPixels(mapView.getWidth(), mapView.getHeight());
		GeoPoint center = projection.fromPixels(mapView.getWidth()/2, mapView.getHeight()/2);
		int latSpan = tl.getLatitudeE6()-br.getLatitudeE6();

		PolylineStoreValue polylineStoreValue = null;
		Point curCenter = new Point();
		curCenter.x = mapView.getWidth()/2;
		curCenter.y = mapView.getHeight()/2;
		Point bitmapOriginalCenter = null;

		List<Path> polylinePath = null;

		Set s = mPolylineStore.entrySet();
		Iterator it = s.iterator();
		while( (null == polylinePath)
				&& it.hasNext() ) {
			Map.Entry m =(Map.Entry)it.next();
			PolylineStoreKey key = (PolylineStoreKey)m.getKey();
			if(	(latSpan<=(key.latSpan+5*key.latSpan/1000))
					&& (latSpan>=(key.latSpan-5*key.latSpan/1000)) ) {
				polylineStoreValue = (PolylineStoreValue)m.getValue();
				bitmapOriginalCenter = projection.toPixels(polylineStoreValue.originalCenter, null);
				if( (curCenter.x == bitmapOriginalCenter.x)
						&& (curCenter.y == bitmapOriginalCenter.y) ) {
					polylinePath = polylineStoreValue.path;
				}
			}
		}

		if( null == polylinePath ) {
			polylinePath = new ArrayList<Path>();

			drawPolyline(mapView,bounds,polylinePath);

			PolylineStoreValue val = new PolylineStoreValue();
			val.path = polylinePath;
			val.originalCenter = center;
			PolylineStoreKey key = new PolylineStoreKey();
			key.bounds = bounds;
			key.latSpan = latSpan;
			mPolylineStore.put(key,val);
		}
		for (Path path : polylinePath) {
			canvas.drawPath(path, paint);
		}
	}
	
	private void drawPolyline( MapView mapView, LatLngBounds bounds, List<Path> paths ) {
		Projection projection = mapView.getProjection();

		Iterator<Polyline> itPolyline;

		// Compute path
		LinkedList<Polyline> polylinesList = getPolylinesList();
		if (polylinesList != null) {
			itPolyline = polylinesList.iterator();
			while( itPolyline.hasNext() ) {
				Polyline polyline = itPolyline.next();
				drawPolyline( polyline, projection, mapView.getZoomLevel(), bounds, paths);
			}
		}
	}
	
	/**
	 * Draw the directions path.
	 * @param polylineBitmap 
	 * @param polyline
	 * @param canvas
	 * @param projection
	 * @param mapView
	 * @param bounds
	 */
	private void drawPolyline( Polyline polyline,
			Projection projection,
			int zoomLevel,
			LatLngBounds bounds,
			List<Path> paths) {
		// We consider drawing polyline only if it intersects displayed map.
		if (bounds.intersects(polyline.getBoundsRef())) {
			boolean first;

			ArrayList<Polyline> parts = polyline.getParts();
			Point newPoint = null;
			Point lastPoint = null;
			int nbParts = parts.size();
			Polyline polylinePart = null;
			for( int p=0; (p<nbParts) /*&& !isCurrentPolylineComputationInterrupted()*/; p++ ) {
				polylinePart = parts.get(p);
				
				ArrayList<LatLng> partPath = polylinePart.getPath();
				ArrayList<Integer> partLevels = polylinePart.getLevels();

				// Check if polyline part is on the screen or if the previous or the next one is on the screen
				if ( partPath.size() == partLevels.size() ) {
//					&& polylinePart.getBounds().intersects(bounds) ) { 
					Path path = new Path();
					first = true;
					int size = partPath.size();
					for( int i=0; i<size /*&& !isCurrentPolylineComputationInterrupted()*/; i++ ) {
						LatLng latlng = partPath.get(i);
						int level = partLevels.get(i);
						if ( zoomLevel >= level ) {
							newPoint = new Point();
							projection.toPixels(latlng.getGeoPoint(), newPoint);
							if (first) {
								// We always draw the first point of a polyline's part.
								if (lastPoint == null)
									path.moveTo(newPoint.x, newPoint.y);
								else {
									path.moveTo(lastPoint.x, lastPoint.y);
									path.lineTo(newPoint.x, newPoint.y);
								}
								lastPoint = newPoint;
								first = false;
							} else {
								// We draw only not duplicated generated XY point
								if( (lastPoint.x != newPoint.x)
										|| (lastPoint.y != newPoint.y) ) {
									path.lineTo(newPoint.x, newPoint.y);
									lastPoint = newPoint;
								}
							}
						}
					}
					paths.add(path);
				}
			}
		}
	}

	/**
	 * Updates overlay to make it match to associated layer.
	 * Only for DynamicLayerInterface and to update infoWindows.
	 */
	public synchronized void populateMarkers() {
		// Populate the overlay
		populate();
	}
	
	/**
	 * Sends a message directly to controller when user just
	 * tapped on a marker. Controller will be able to to actions
	 * directly on Marker matching index given in parameter.
	 * This method is called only if the onTap(GeoPoint, MapView)
	 * method calls its parent onTap(GeoPoint, MapView) method.
	 * @param index Index of OverlayItem tapped
	 * @return true if the tap was handled by this overlay
	 */
	@Override
	protected boolean onTap(int index) {
		PLog.d(TAG, "onTap - index ", index);
		return true;
	}
	
	/**
	 * First method called when this layer has been tapped.
	 * We check if an opened InfoWindow has been tapped.
	 * Otherwise, we call parent method, which we call our
	 * onTap(int) method if a marker has been tapped.
	 * @param p GeoPoint which has been tapped
	 * @param mapView MapView which has been tapped
	 * @return true if the tap was handled by this overlay
	 */
	@Override
	public synchronized boolean onTap(GeoPoint p, MapView mapView) {
		mMarkersTapped = new ArrayList<Marker>();
		
		if( (null != mInfoWindowMarker)
				&& (null != mInfoWindowDrawer)
				&& (null != mInfoWindowDrawer.mInfoWindowBitmap) ) {
			Point point = new Point(0,0);
			/* Checking if InfoWindow not tapped... */
			Projection projection = mapView.getProjection();
			projection.toPixels(p, point);
			Point markerAnchor = new Point();
			int infoWinWidth  = mInfoWindowDrawer.mInfoWindowBitmap.getWidth();
			int infoWinHeight = mInfoWindowDrawer.mInfoWindowBitmap.getHeight();
			int markerHeight = mInfoWindowMarker.getDrawable().getIntrinsicHeight();
			projection.toPixels(mInfoWindowMarker.getLatLng().getGeoPoint(), markerAnchor);
			int x0;
			int y0;
			if (mInfoWindowMarker.getMiddleAnchor()) {
				x0 = markerAnchor.x-infoWinWidth/2;
				y0 = markerAnchor.y-infoWinHeight-markerHeight/2;
			}
			else {
				x0 = markerAnchor.x-infoWinWidth/2;
				y0 = markerAnchor.y-infoWinHeight-markerHeight;
			}
			
			/* If tapped, we send the event to controller. */
			if ((point.x > x0)
					&& (point.x < (x0+infoWinWidth))
					&& (point.y > y0)
					&& (point.y < (y0+infoWinHeight))) {
				mMarkersTapped.add(mInfoWindowMarker);
			}
		}
		try {
			super.onTap( p, mapView );
		} catch( Exception e ) {
			PLog.e(TAG,"onTap - An error occured ",e);
			e.printStackTrace();
		}
		mController.overlayReportItemTap(this, mMarkersTapped);			
		mMarkersTapped = null;
		
		return false;
	}
	
	@Override
	protected boolean hitTest(OverlayItem item,
			android.graphics.drawable.Drawable drawableMarker,
			int hitX, int hitY) 
	{
		boolean hit = super.hitTest(item, drawableMarker, hitX, hitY);
		if( (true == hit)
				&& (null != mMarkersTapped) )
		{
			PLog.d(TAG, "hitTest - hit " , item.getSnippet());
			long markerID = Long.parseLong(item.getSnippet());
			Iterator<Marker> it = mMarkersList.iterator();
			Marker marker = null;
			boolean found = false;
			while (!found && it.hasNext()) {
				marker = it.next();
				if (marker.getId() == markerID) {
					found = true;
				}
			}
			if (found) {
				mMarkersTapped.add(marker);
			}
			else {
				PLog.e(TAG, "Item tapped but not found in Markers list : ID=" ,
						item.getSnippet() , ", Title=" , item.getTitle());
			}
		}
		return hit;
	}
	
	/* Parcelable implementation. */
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
		PLog.d(TAG, "writeToParcel " , toString());
    	out.writeLong(mId);
    	out.writeTypedList(mMarkersList);
    	out.writeTypedList(mPolylinesList);
    	out.writeParcelable(mBounds, flags);
    	if( (null != mInfoWindowDrawer)
    			&& (null != mInfoWindowDrawer.mInfoWindowBitmap) ) {
    		out.writeParcelable(mInfoWindowDrawer.mInfoWindowBitmap, flags);
    	}
    }

    public static final Parcelable.Creator<Layer> CREATOR
            = new Parcelable.Creator<Layer>() {
        public Layer createFromParcel(Parcel in) {
            return new Layer(in);
        }

        public Layer[] newArray(int size) {
            return new Layer[size];
        }
    };
    
    
    private Layer(Parcel in) {
    	super(null);
		PLog.d(TAG, "readFromParcel " , toString());
        mId = in.readLong();
        mMarkersList = new LinkedList<Marker>();
        in.readTypedList(mMarkersList, Marker.CREATOR);
        mPolylinesList = new LinkedList<Polyline>();
        in.readTypedList(mPolylinesList, Polyline.CREATOR);
        mBounds = in.readParcelable(getClass().getClassLoader());
        mContext = null;
        mController = null;
        mInfoWindowMarker = null;
        mInfoWindowDrawer = new InfoWindowDrawer();
        mInfoWindowDrawer.mInfoWindowBitmap = in.readParcelable(getClass().getClassLoader());
    }
    /* End of parcelable implementation. */

    /**
     * Called when the activity is destroyed
     */
	public void destroy() {
		// Do nothing
	}
	
	
	/**
	 * Dedicated to draw information windows
	 */
	class InfoWindowDrawer {

		static final long SLIDE_TIME = 2000; // 2s
		static final long PAUSE_TIME = 2000; // 2s
		static final int INFOWINDOW_MAX_LINE_LARGE = 3;
		static final int INFOWINDOW_MAX_LINE_MEDIUM = 2;
				
		InfoWindow mWin;
		LinearLayout mLayout;
		TextView mTitle;
		int mMaxLines = INFOWINDOW_MAX_LINE_MEDIUM;
		int mCurrentFirstLine = 0;
		long mLastPauseTimeStamp = -1;
		Canvas mCanvas;
		boolean mAnimatedInfoWindow = false;
		private Bitmap mInfoWindowBitmap = null;

		/**
		 * Prepare bitmap for info window of a marker.
		 * @param marker The marker.
		 * @return The height of the bitmap in pixels.
		 */
		public int openInfoWindow (Marker marker) {
			mInfoWindowMarker = marker;
			if( marker.getType() == TYPE.DIRECTIONS_STEP ) {
				mMaxLines = INFOWINDOW_MAX_LINE_LARGE;
			}
			else {
				mMaxLines = INFOWINDOW_MAX_LINE_MEDIUM;
			}
			mWin = marker.getInfoWindow();
			mLayout = (LinearLayout)View.inflate(mContext, R.layout.infowindowview, null);
			mTitle = (TextView)mLayout.findViewById(R.id.InfoWinTitle);
			TextView content = (TextView)mLayout.findViewById(R.id.InfoWinContent);
			if (mWin.getTitle() != null) {
				mTitle.setMaxLines(mMaxLines);
				mTitle.setVisibility(View.VISIBLE);
				mTitle.setText(mWin.getTitle());
			}
			else {
				mTitle.setVisibility(View.GONE);
			}
			content.setText(mWin.getContent());
			mLayout.measure(MeasureSpec.makeMeasureSpec(300, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(200, MeasureSpec.AT_MOST));
			mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());

			if( mTitle.getLineCount() > mMaxLines ) {
				mAnimatedInfoWindow = true;
			}
			else {
				mAnimatedInfoWindow = false;
			}

			int bitmapHeight = 0;
			try {
				mInfoWindowBitmap = Bitmap.createBitmap(mLayout.getWidth(), mLayout.getHeight(), Bitmap.Config.ARGB_8888);
				mCanvas = new Canvas(mInfoWindowBitmap);
				mLayout.draw(mCanvas);
				mCurrentFirstLine = 0;
				mLastPauseTimeStamp = -1;
				bitmapHeight = mInfoWindowBitmap.getHeight();
				marker.setInfoWindowHeight(bitmapHeight);
			}
			catch( OutOfMemoryError e ) {
				// Prevent from crash when creating the bitmap failed due to an out of memory error 
				PLog.e(TAG,"openInfoWindow - An exception occured when creating bitmap : ",e);
				bitmapHeight = 0;
				marker.setInfoWindowHeight(0);
			}
			return bitmapHeight;
		}
		
		public void updateInfoWindow( long when ) {
			if( !mAnimatedInfoWindow )
				return;
			if( (when % 2) != 0  )
				return;
			if (mWin.getTitle() != null) {
				int lineHeight = mTitle.getLineHeight();
				int nbLines = mTitle.getLineCount();
				if( -1 == mLastPauseTimeStamp ) {
					mLastPauseTimeStamp = when;
					mCurrentFirstLine = 0;
				}
				else if( (when-mLastPauseTimeStamp) >= (PAUSE_TIME+SLIDE_TIME) ) {
					mLastPauseTimeStamp = when;
					if( nbLines > 0 )
						mCurrentFirstLine = (mCurrentFirstLine+1)%nbLines;
				} else if( ((when-mLastPauseTimeStamp) >= PAUSE_TIME)
						&& ((mCurrentFirstLine+mMaxLines)>=nbLines) ) {
					mLastPauseTimeStamp = when;
					mCurrentFirstLine = 0;
				}
				
				if( (when-mLastPauseTimeStamp) < PAUSE_TIME ) {
					mTitle.scrollTo(0, mCurrentFirstLine*lineHeight);
				}
				else {
					mTitle.scrollTo(0, mCurrentFirstLine*lineHeight + (int)((when-mLastPauseTimeStamp-PAUSE_TIME)*lineHeight/SLIDE_TIME) );
				}
			}
			mLayout.draw(mCanvas);
		}
		
		private void drawInfoWindow(Canvas canvas, Projection projection) {
			if( null == mInfoWindowBitmap ) {
				PLog.e(TAG, "drawInfoWindow - Invalid conditions");
				return;
			}
			Point markerAnchor = new Point();
			int infoWinWidth  = mInfoWindowBitmap.getWidth();
			int infoWinHeight = mInfoWindowBitmap.getHeight();
			int markerHeight = mInfoWindowMarker.getDrawable().getIntrinsicHeight();
			projection.toPixels(mInfoWindowMarker.getLatLng().getGeoPoint(), markerAnchor);
			int x0;
			int y0;
			if (mInfoWindowMarker.getMiddleAnchor()) {
				x0 = markerAnchor.x-infoWinWidth/2;
				y0 = markerAnchor.y-infoWinHeight-markerHeight/2;
			}
			else {
				x0 = markerAnchor.x-infoWinWidth/2;
				y0 = markerAnchor.y-infoWinHeight-markerHeight;
			}
		    canvas.drawBitmap(mInfoWindowBitmap, x0, y0, null);
		}
	}
	


	class PolylineStoreKey {
    	LatLngBounds bounds;
    	int latSpan;
    }
    
    class PolylineStoreValue {
    	List<Path> path;
    	GeoPoint originalCenter;
    }

    Map mPolylineStore = new LinkedHashMap<PolylineStoreKey,Bitmap>() {
        public boolean removeEldestEntry (Map.Entry<PolylineStoreKey,Bitmap> eldest){
            return size() > 4;
          }
    };

	public SEARCH_RESULT searchNext()
	{
		return SEARCH_RESULT.FAILED;
	}
	
	public SEARCH_RESULT searchPrevious()
	{
		return SEARCH_RESULT.FAILED;
	}

	/**
	 * Number of times the draw method of this layer has to be called
	 * each time the map view is refreshed.
	 * @return By default, it returns 1.
	 */
	public int getNbRequiredDrawPass() {
		return 1;
	}
}
