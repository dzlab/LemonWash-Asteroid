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
import java.util.List;

import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.parrot.parrotmaps.directions.DirectionsResultLayer;
import com.parrot.parrotmaps.directions.DirectionsResultLayerInterface;
import com.parrot.parrotmaps.log.PLog;
import com.parrot.parrotmaps.mylocation.CurrentLocationOverlay;

public class MapViewDisplay extends DisplayAbstract

{
	/** Attributes */
	private final String                   TAG                = getClass().getSimpleName();
	private       List<Overlay>            mLayersList        = null;
	private       MapView                  mMapView           = null;
	private       CurrentLocationOverlay   mMyLocationOverlay = null;
	private       MapController            mMapController     = null;
	private       DirectionsResultLayer    mDirectionsLayer   = null;
	private       MapViewCacheClearManager mMapViewCacheClearManager = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maplayout);
		mMapView = (MapView)findViewById(R.id.mapview);
		mMapView.setFocusable(false);
		mLayersList = mMapView.getOverlays();
		mMyLocationOverlay = new CurrentLocationOverlay(this, mMapView);
		mMapController = mMapView.getController();
		mMapModesList = new ArrayList<MAP_MODE>();
		mMapModesList.add(MAP_MODE.MAP);
		mMapModesList.add(MAP_MODE.SATELLITE);
		mAPILayersList = new ArrayList<API_LAYER>();
		mAPILayersList.add(API_LAYER.MY_LOCATION);
		mAPILayersList.add(API_LAYER.TRAFFIC);
		mMapView.setOnClickListener(this);
		mMapView.setOnTouchListener(this);
		mMapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_NEVER);
		mButtonMap_mylocation = (ImageButton)findViewById(R.id.map_mylocation);
		mButtonMap_zoom_in    = (ImageButton)findViewById(R.id.map_zoom_in);
		mButtonMap_zoom_out   = (ImageButton)findViewById(R.id.map_zoom_out);
		mButtonMap_prev_arrow = (ImageButton)findViewById(R.id.map_prev);
		mButtonMap_next_arrow = (ImageButton)findViewById(R.id.map_next);
		mPoiOrResultsList = (ListView)findViewById(R.id.poi_or_results_list);
		mButtonMap_mylocation.setOnClickListener(this);
		mButtonMap_zoom_in.setOnClickListener(this);
		mButtonMap_zoom_out.setOnClickListener(this);
		mButtonMap_prev_arrow.setOnClickListener(this);
		mButtonMap_next_arrow.setOnClickListener(this);
		mPoiOrResultsList.setOnItemClickListener(this);
		
		// Map cache clear management
		mMapViewCacheClearManager = new MapViewCacheClearManager( this, mMapView );
		
		super.onCreateEnd();
	}
	
	@Override
	protected void onCreateBackground() {
		super.onCreateBackground();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		try {
			mController = new Controller(this);
			runOnUiThread(new Runnable() {
				public void run() {
					mController.manageIntent();
				}
			});
		} catch (Exception e) {
			PLog.e(TAG, "FATAL error : ", e.getMessage());
			e.printStackTrace();
			finish();
		}
	}

	/**
	 * 
	 */
	@Override
	protected void onDestroy() {
        try {
       		// Indicate to controller that the activity is destroyed
        	mController.getHandler().sendEmptyMessage(Controller.MSG_DESTROYED);
        	mMapViewCacheClearManager.stop();
        } catch (Exception e) {
			PLog.e(TAG, "onDestroyBackground - failed to send on destroy msg to controller: " , e);
			e.printStackTrace();
        }
		super.onDestroy();
	}
	
	/**
	 * Operation
	 * 
	 * @param
	 */
	@Override
	protected void onResumeBackground() {
		super.onResumeBackground();
        try {
        	mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (Exception e) {
			PLog.e(TAG, "Failed to init location manager : " , e.getMessage());
			e.printStackTrace();
        }
        try {
        	mMyLocationOverlay.enableMyLocation();
        } catch (Exception e) {
			PLog.e(TAG, "Failed to resume current location tracking : " , e.getMessage());
			e.printStackTrace();
        }
		runOnUiThread(new Runnable() {
			public void run() {
				mMapView.invalidate();
			}
		});
	}
	
	/**
	 * Operation
	 * 
	 * @param
	 */
	@Override
	protected void onPauseBackground() {
		super.onPauseBackground();
        try {
        	mLocationManager.removeUpdates(this);
        } catch (Exception e) {
			PLog.e(TAG, "Failed to init location manager : " , e.getMessage());
			e.printStackTrace();
        }
        try {
        	mMyLocationOverlay.disableMyLocation();
        } catch (Exception e) {
			PLog.e(TAG, "Failed to disable current location tracking : " , e.getMessage());
			e.printStackTrace();
        }
		//mMyLocationOverlay.disableCompass();
	}
	
	
	/**
	 * Operation
	 * 
	 * @param latlng
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	public void setCenter(final LatLng latlng) {
//		PLog.v(TAG, "setCenter "+latlng);
		
		runOnUiThread(new Runnable() {
			public void run() {
				LatLng sw = getLatLngBounds().getSw();
				LatLng ne = getLatLngBounds().getNe();
				double latSpan = (ne.getLat() - sw.getLat())*2;
				double lngSpan = (ne.getLng() - sw.getLng())*2;
				
				LatLngBounds tolerance = new LatLngBounds(
						new LatLng(sw.getLat()-latSpan, sw.getLng()-lngSpan),
						new LatLng(ne.getLat()+latSpan, ne.getLng()+lngSpan));
				if (tolerance.contains(latlng)) {
					mMapController.animateTo(latlng.getGeoPoint());
				}
				else {
					mMapController.stopAnimation(false);
					mMapController.setCenter(latlng.getGeoPoint());
				}
//				PLog.v(TAG, "Animating map to " + latlng);
				positionChanged();
			}
		});
		
	}
	
	@Override
	public void panToBounds(final LatLngBounds bounds) {
		PLog.e(TAG, "panToBounds ",bounds);
		runOnUiThread(new Runnable() {
			public void run() {
				GeoPoint center = bounds.getCenter().getGeoPoint();
				
				/* Getting pan difference between center and SW and NE. */
				int latSpanE6_1 = bounds.getSw().getGeoPoint().getLatitudeE6()  - center.getLatitudeE6();
				int lonSpanE6_1 = bounds.getSw().getGeoPoint().getLongitudeE6() - center.getLongitudeE6();
				int latSpanE6_2 = bounds.getNe().getGeoPoint().getLatitudeE6()  - center.getLatitudeE6();
				int lonSpanE6_2 = bounds.getNe().getGeoPoint().getLongitudeE6() - center.getLongitudeE6();
				
				
				/* Go to the center of bounds and zoom out to make
				 * appear angles of rectangle bounds.
				 */
				try {
					mMapController.setCenter(center);
					mMapController.zoomToSpan(latSpanE6_1, lonSpanE6_1);
					mMapController.zoomToSpan(latSpanE6_2, lonSpanE6_2);
				}
				catch( OutOfMemoryError e ) {
					// These exception can occures sometimes with MapController
					PLog.e(TAG,"panToBounds - An exception occured: ",e);
				}
				
				/* zoomToSpan() method of MapView is not precises and often zoom in too much. */
				mMapController.setZoom(mMapView.getZoomLevel()-1);
				positionChanged();
			}
		});
	}

	/**
	 * Operation
	 * 
	 * @param zoom
	 */
	@Override
	public LatLng getCenter() {
		return new LatLng(mMapView.getMapCenter());
	}

	/**
	 * Operation
	 * 
	 * @param zoom
	 */
	@Override
	public LatLngBounds getLatLngBounds() {
		LatLngBounds bounds = new LatLngBounds(mMapView.getMapCenter(), mMapView.getLatitudeSpan(), mMapView.getLongitudeSpan());
		return bounds;
	}
	
	/**
	 * Operation
	 * 
	 * @param zoomLevel
	 */
	@Override
	public void setZoomLevel(final int zoomLevel) {
		PLog.e(TAG, "setZoomLevel ",zoomLevel);
		runOnUiThread(new Runnable() {
			public void run() {
				if (zoomLevel != getZoomLevel()) {
					try {
						mMapController.setZoom(zoomLevel);
					}
					catch( OutOfMemoryError e ) {
						// These exception can occures sometimes with MapController
						PLog.e(TAG,"setZoomLevel - An exception occured: ",e);
					}
					positionChanged();
				}
			}
		});
	}
	
	/**
	 * Set zoom level to specific spans.
	 * 
	 */
	@Override
	public void setZoomToSpan(final LatLngBounds bounds) {
//		PLog.v(TAG, "setZoomToSpan ",bounds);
		if( null != bounds ) {
			runOnUiThread(new Runnable() {
				public void run() {
					try {
						mMapController.zoomToSpan(bounds.getLatSpanE6(), bounds.getLngSpanE6());
					}
					catch( OutOfMemoryError e ) {
						// These exception can occures sometimes with MapController
						PLog.e(TAG,"setZoomToSpan - An exception occured: ",e);
					}
					positionChanged();
				}
			});
		}
	}
	
	/**
	 * Operation
	 * 
	 */
	@Override
	public void zoomIn() {
//		PLog.v(TAG, "zoomIn");
		runOnUiThread(new Runnable() {
			public void run() {
				try {
					mMapController.zoomIn();
				}
				catch( OutOfMemoryError e ) {
					// These exception can occures sometimes with MapController
					PLog.e(TAG,"zoomIn - An exception occured: ",e);
				}
				positionChanged();
			}
		});
	}
	
	/**
	 * Operation
	 * 
	 */
	@Override
	public void zoomOut() {
//		PLog.v(TAG, "zoomOut");
		runOnUiThread(new Runnable() {
			public void run() {
				try {
					mMapController.zoomOut();
				}
				catch( OutOfMemoryError e ) {
					// These exception can occures sometimes with MapController
					PLog.e(TAG,"zoomOut - An exception occured: ",e);
				}
				positionChanged();
			}
		});
	}
	
	/**
	 * Operation
	 * 
	 * @return int
	 */
	@Override
	public int getZoomLevel() {
		return mMapView.getZoomLevel();
	}
	

	@Override
	public void drawInfoWindow(final Marker marker, final LayerInterface layer) {		
		runOnUiThread(new Runnable() {
			public void run() {
				layer.openInfoWindow(marker);
				mMapView.invalidate();
			}
		});
	}
	
	
	@Override
	public void removeInfoWindow(Marker marker, final LayerInterface layer) {
		runOnUiThread(new Runnable() {
			public void run() {
				layer.closeInfoWindow();
				mMapView.invalidate();
			}
		});
	}
	
	/**
	 * Operation
	 * 
	 * @param mode
	 */
	@Override
	public void setMapMode(final MAP_MODE mode) {
		runOnUiThread(new Runnable() {
			public void run() {
			switch (mode) {
				case MAP:
					mMapView.setSatellite(false);
					break;
				case SATELLITE:
					mMapView.setSatellite(true);
					break;
				default:
					PLog.e(TAG, "Can't switch to MAPMODE unknown : " , mode.toString());
			}
			}
		});

	}
	
	private boolean hasReachedMaxLayerOccurance( List<Overlay> layers, Layer layer ) {
		int requiredDrawPass = layer.getNbRequiredDrawPass();
		int drawPass = 0;
		for( int i=0; i<layers.size(); i++ ) {
			if( layers.get(i) == layer ) {
				drawPass++;
			}
		}
		return drawPass >= requiredDrawPass;
	}
	
	
	@Override
	public void drawLayer(final Layer layer) {
		runOnUiThread(new Runnable() {
			public void run() {
				/* Adding overlay. */
				if (!hasReachedMaxLayerOccurance(mLayersList,layer)) {
					layer.populateMarkers();
					mLayersList.add(layer);
					mMapView.invalidate();
				}
				else {
					PLog.e(TAG, "Impossible to draw a layer already drawn. Only updateLayer() possible.");
				}
			}
		});
		
	}
	
	@Override
	public void removeLayer(final Layer layer) {
		runOnUiThread(new Runnable() {
			public void run() {
				/* Removing overlay. */
				if (mLayersList.remove(layer)) {
					mMapView.invalidate();
				}
				else {
					PLog.e(TAG, "Impossible to remove a layer not already drawn.");
				}
			}
		});

	}

	@Override
	public void updateLayer(final Layer layer) {
		runOnUiThread(new Runnable() {
			public void run() {
				/* Updating overlay. */
				if (mLayersList.contains(layer)) {
					layer.populateMarkers();
					mMapView.invalidate();
				}
				else {
					PLog.e(TAG, "Impossible to update a layer not already drawn.");
				}
			}
		});
	}

	/**
	 * Operation
	 * 
	 * @param infoWindow
	 */
	@Override
	public void drawAPILayer(final API_LAYER layer) {
		runOnUiThread(new Runnable() {
			public void run() {
				switch (layer) {
					case MY_LOCATION:
						mLayersList.add(mMyLocationOverlay);
						break;
					case TRAFFIC:
						mMapView.setTraffic(true);
						break;
					default:
						PLog.e(TAG, "Error, wrong API_LAYER to display : " , layer.toString());
						break;
				}
				mMapView.invalidate();
			}
		});
	}

	/**
	 * Operation
	 * 
	 * @param infoWindow
	 */
	@Override
	public void removeAPILayer(final API_LAYER layer) {
		runOnUiThread(new Runnable() {
			public void run() {
				switch (layer) {
					case MY_LOCATION:
						mLayersList.remove(mMyLocationOverlay);
						break;
					case TRAFFIC:
						mMapView.setTraffic(false);
						break;
					default:
						PLog.e(TAG, "Error, wrong API_LAYER to remove : " , layer.toString());
						break;
				}
				mMapView.invalidate();
			}
		});
		
	}

	@Override
	public void drawDirectionsLayer(String start, String end) {
		Message msg = Message.obtain();
		msg.what = Controller.MSG_DIRECTIONS_RESULT;

		if (mDirectionsLayer == null) {
			try {
				mDirectionsLayer = new DirectionsResultLayer(this, mController);
			} catch (Exception e) {
				PLog.e(TAG,"FATAL error :" , e.getMessage());
				finish();
			}
		}
		else {
			removeLayer(mDirectionsLayer);
			mDirectionsLayer.clearLayer();
		}
		DirectionsResultLayerInterface.SEARCH_RESULT resCode = mDirectionsLayer.route(start, end, getLanguage());
		if ( resCode == DirectionsResultLayerInterface.SEARCH_RESULT.OK ) {
			// The directions layer is add 2 time
			// the fist time it is for polyline drawing
			// the second time it is for markers drawing
			drawLayer(mDirectionsLayer);
			drawLayer(mDirectionsLayer);
			msg.arg1 = resCode.ordinal();
			msg.obj = mDirectionsLayer;
		}
		else {
			mDirectionsLayer.clearLayer();
			msg.arg1 = resCode.ordinal();
		}
		mController.getHandler().sendMessage(msg);
	}

	@Override
	public void interruptRouteQuery() {
		if (mDirectionsLayer != null) {
			// Interrupt route query
			mDirectionsLayer.interruptRouteQuery();
			// Finish current activity
			finish();
		}
	}
	
	@Override
	public void removeDirectionsLayer() {
		if (mDirectionsLayer != null) {
			removeLayer(mDirectionsLayer);
			mDirectionsLayer = null;
		}
		else {
			PLog.e(TAG,"Impossible to remove a null Directions Layer.");
		}
	}

	@Override
	public void invalidate() {
		runOnUiThread(new Runnable() {
			public void run() {
				if( null != mMapView ) {
					mMapView.invalidate();
				}
			}
		});
	}

}
