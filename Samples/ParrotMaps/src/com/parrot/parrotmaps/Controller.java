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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.SearchRecentSuggestions;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;

import com.parrot.exitbroadcast.ExitBroadcaster;
import com.parrot.parrotmaps.DisplayAbstract.API_LAYER;
import com.parrot.parrotmaps.DisplayAbstract.MAP_MODE;
import com.parrot.parrotmaps.DisplayAbstract.RESULT_MODE;
import com.parrot.parrotmaps.directions.DirectionsEntryActivity;
import com.parrot.parrotmaps.directions.DirectionsListActivity;
import com.parrot.parrotmaps.directions.DirectionsListAdapter;
import com.parrot.parrotmaps.directions.DirectionsResultLayer;
import com.parrot.parrotmaps.directions.DirectionsResultLayerInterface;
import com.parrot.parrotmaps.directions.Polyline;
import com.parrot.parrotmaps.gps.GpsChecker;
import com.parrot.parrotmaps.localsearch.GoogleAjaxLocalSearchHistoric;
import com.parrot.parrotmaps.localsearch.GoogleAjaxLocalSearchLayer;
import com.parrot.parrotmaps.localsearch.LocalSearchLayerInterface;
import com.parrot.parrotmaps.localsearch.QuickSearchCategories;
import com.parrot.parrotmaps.localsearch.QuickSearchListActivity;
import com.parrot.parrotmaps.localsearch.ResultMarker;
import com.parrot.parrotmaps.localsearch.ResultsListActivity;
import com.parrot.parrotmaps.localsearch.ResultsListAdapter;
import com.parrot.parrotmaps.log.PLog;
import com.parrot.parrotmaps.network.NetworkStatus;
import com.parrot.parrotmaps.network.NetworkStatusListener;
import com.parrot.parrotmaps.panoramio.PanoramioLayer;
import com.parrot.parrotmaps.panoramio.PhotosLayerInterface;
import com.parrot.parrotmaps.tts.TTSManager;
import com.parrot.parrotmaps.wikipedia.GeonamesLayer;
import com.parrot.parrotmaps.wikipedia.WikipediaLayerInterface;

public class Controller implements NetworkStatusListener
{
	/** Main attributes. */
	private final String TAG = this.getClass().getSimpleName();
	private Handler mHandler;
	
	static private long LONG_KEY_PRESS_MS = 1000;
	
	private boolean mCenterKeyLongPressed = false;

	
	/** All components are linked to controller. */
	private DisplayAbstract                mDisplayAbstract;
	private LatLng                         mLastPositionKnown;
	private LocalSearchLayerInterface      mResultsLayerInterface;
	private DirectionsResultLayerInterface mDirectionsResultLayerInterface;
	private WikipediaLayerInterface        mWikipediaLayerInterface;
	private PhotosLayerInterface           mPhotosLayerInterface;
	
	/** States */
	private MAP_MODE    mMapMode;
	private RESULT_MODE mResultsDisplayMode;
	private boolean     mGPSAvailable;
	private boolean     mResultsMode;
	private boolean     mDirectionsResultMode;
	private boolean     mTrafficLayerOn;
	private boolean     mWikipediaLayerOn;
	private boolean     mPhotosLayerOn;
	private boolean		mGoToLastMarker;
	
	/** Indicate if the first fix has been received */
	private boolean mLocationFirstFixReceived = false;
	
	/** Exporting incoming messages in a thread to avoid View to do controller work. */
	private HandlerThread mThread;
	private Looper        mLooper;
	
	/** When in Results mode or Directions Result mode, to know what to follow. */
	private ListIterator<Polyline> mItDirectionsStep = null;
	private ListIterator<Marker>   mItResult         = null;
	private Polyline mCurrentStep   = null;
	private Marker   mCurrentMarker = null;
	
	/** Destination POI of current directions */
	private ResultMarker mDirectionsDestinationPOI = null;
	
	/** Listener for route query cancel event. */
	public OnCancelListener mRouteQueryCancelListener;
	/** Listener for local search query cancel event. */
	public OnCancelListener mSearchQueryCancelListener;
	
	/** Turned to true when the user cancel a search query */
	private boolean mSearchQueryCanceled = false;
	/** Turned to true when the user cancel a directions query */
	private boolean mRouteQueryCanceled = false;

	private NetworkStatus mNetworkStatus = null;
	private TTSManager mTTSManager = null;
	
	/** Incoming Handler messages. */
	static public final int MSG_MY_LOCATION_BUTTON         = 1;
	static public final int MSG_NEW_GPS_DATA               = 2;
	static public final int MSG_ZOOM_IN                    = 3;
	static public final int MSG_ZOOM_OUT                   = 4;
	static public final int MSG_POSITION_CHANGED           = 5;
	static public final int MSG_MENU_OPTIONS_ITEM_SELECTED = 7;
	static public final int MSG_KEYB_ON_KEY_DOWN           = 8;
	static public final int MSG_KEYB_ON_KEY_UP             = 9;
	static public final int MSG_USER_TOUCH_ACTION_MOVE     = 10;
	static public final int MSG_SEARCH_RESULTS             = 11;
	static public final int MSG_DIRECTIONS_RESULT          = 12;
	static public final int MSG_USER_CLICK_BUTTON_PREV     = 13;
	static public final int MSG_USER_CLICK_BUTTON_NEXT     = 14;
	static public final int MSG_USER_TAP_MARKERS           = 15;
	static public final int MSG_USER_SELECT_ITEM           = 16;
	static public final int MSG_RESUMED                    = 17;
	static public final int MSG_DESTROYED                  = 18;
	static public final int MSG_INVALIDATE_DISPLAY         = 19;
	static public final int MSG_NETWORK_STATUS_READY       = 20;
	static public final int MSG_GPS_CHECK_RESULT           = 21;
	
	/** Specifies which SEARCH intent is sent */
	static public final int INTENT_SEARCH_LOCAL_SEARCH             = 0;
	static public final int INTENT_SEARCH_LOCAL_SEARCH_BY_CATEGORY = 1;
	static public final int INTENT_SEARCH_DIRECTIONS               = 2;
	
	static public final String INTENT_SEARCH_DATA_FIELD_ACTION = "search_action";
	static public final String INTENT_SEARCH_DATA_FIELD_CAT_ID = "cat_id";
	static public final String INTENT_SEARCH_DATA_FIELD_TIME_STAMP = "timestamp";
	static private final String INTENT_ACTION_CLEAN = "com.parrot.parrotmaps.ACTION_CLEAN";
	static private final String INTENT_ACTION_NEXT_SEARCH = "com.parrot.parrotmaps.ACTION_NEXT_SEARCH";
	static private final String INTENT_ACTION_PREVIOUS_SEARCH = "com.parrot.parrotmaps.ACTION_PREVIOUS_SEARCH";
	
	/** Specifies each "requestCode" of startActivityForResult() calls */
	static public final int REQUEST_SELECT_RESULT = 0;
	static public final int REQUEST_SELECT_DIRECTIONS_STEP = 1;

	private ControllerTask mCurrentTask = null;
	
	private class ControllerTask extends AsyncTask<Integer, Void, Void>
	{
		
		static final int NEW_SEARCH_TASK = 0;
		static final int NEXT_SEARCH_TASK = 1;
		static final int PREVIOUS_SEARCH_TASK = 2;

		@Override
		protected Void doInBackground(Integer... params) {
			int taskId = params[0];
			Message msg;
			switch (taskId)
			{
				case NEW_SEARCH_TASK:
					final Intent intent = mDisplayAbstract.getIntent();
					final int intent_data_field_action = intent.getBundleExtra(SearchManager.APP_DATA).getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1);
					
					// Get search bounds
					LatLngBounds bounds = getSearchBounds();
					
					String query = null;
					if( mSearchQueryCanceled ) {
						// Canceled, do nothing
					}
					else if( INTENT_SEARCH_LOCAL_SEARCH == intent_data_field_action ) {
						// Get the query string directly in intent data
						query = intent.getStringExtra(SearchManager.QUERY);
					}
					else {
						// Get the query category id
						final int requestId = intent.getBundleExtra(SearchManager.APP_DATA).getInt(INTENT_SEARCH_DATA_FIELD_CAT_ID, -1);
						// Build the query string from the category id
						if( -1 != requestId && QuickSearchListActivity.FREE_SEARCH_REQUEST_ID != requestId) {
							query = QuickSearchCategories.getKeywordsForCategory( mDisplayAbstract,
                                                                                  requestId,
                                                                                  bounds.getCenter().getLat(),
                                                                                  bounds.getCenter().getLng() );
						}
						else if (requestId == QuickSearchListActivity.FREE_SEARCH_REQUEST_ID)
						{
							query = intent.getBundleExtra(SearchManager.APP_DATA).getString(QuickSearchListActivity.INTENT_SEARCH_DATA_FREE_SEARCH_QUERY);
						}

					}
					if (isCancelled())
					{
						return null;
					}
					msg = Message.obtain();
					msg.what = Controller.MSG_SEARCH_RESULTS;
					msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED.ordinal();
					if( !mSearchQueryCanceled
							&& (query != null)
							&& (query != "") ) {
						/* Sending request and receiving response... */
						LocalSearchLayerInterface.SEARCH_RESULT resCode;
						resCode = mResultsLayerInterface.search(query, bounds, mDisplayAbstract.getLanguage());
						if( mSearchQueryCanceled ) {
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED_CANCELED.ordinal();
						}
						else if( resCode == LocalSearchLayerInterface.SEARCH_RESULT.OK) {
							// Only if search was successfully done, we set the flag
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.OK.ordinal();
						}
						else if( resCode == LocalSearchLayerInterface.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT ) {
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT.ordinal();
						}
					}
					if (isCancelled())
					{
						return null;
					}
					mHandler.sendMessage(msg);
					break;
					
				case NEXT_SEARCH_TASK:
					msg = Message.obtain();
					msg.what = Controller.MSG_SEARCH_RESULTS;
					msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED.ordinal();
					if( !mSearchQueryCanceled) {
						/* Sending request and receiving response... */
						LocalSearchLayerInterface.SEARCH_RESULT resCode;
						resCode = ((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).searchNext();
						if( mSearchQueryCanceled ) {
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED_CANCELED.ordinal();
						}
						else if( resCode == LocalSearchLayerInterface.SEARCH_RESULT.OK) {
							// Only if search was successfully done, we set the flag
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.OK.ordinal();
						}
						else if( resCode == LocalSearchLayerInterface.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT ) {
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT.ordinal();
						}
					}
					if (isCancelled())
					{
						return null;
					}
					mHandler.sendMessage(msg);
					break;
					
				case PREVIOUS_SEARCH_TASK:
					msg = Message.obtain();
					msg.what = Controller.MSG_SEARCH_RESULTS;
					msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED.ordinal();
					if( !mSearchQueryCanceled) {
						/* Sending request and receiving response... */
						LocalSearchLayerInterface.SEARCH_RESULT resCode;
						resCode = ((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).searchPrevious();
						if( mSearchQueryCanceled ) {
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED_CANCELED.ordinal();
						}
						else if( resCode == LocalSearchLayerInterface.SEARCH_RESULT.OK) {
							// Only if search was successfully done, we set the flag
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.OK.ordinal();
							
						}
						else if( resCode == LocalSearchLayerInterface.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT ) {
							msg.arg1 = LocalSearchLayerInterface.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT.ordinal();
						}
					}
					if (isCancelled())
					{
						return null;
					}
					mHandler.sendMessage(msg);
					break;
					
				default:
					break;
			}
			return null;
		}
		
		@Override
		protected void onCancelled ()
		{
			PLog.e("ControllerTask", "Task Cancelled");
			((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).cancelTask();
		}
		
	}
	
	
    /**
     * Main constructor. Called by Activity.
     * @param display Pointer on map view activity
     * @throws Exception Shut down the programme if init error
     */
	public Controller(DisplayAbstract display) throws Exception {
		mDisplayAbstract = display;
		mDisplayAbstract         = display;
		initializeHandler();
		mLastPositionKnown              = null;
		mResultsLayerInterface          = new GoogleAjaxLocalSearchLayer(mDisplayAbstract, this);
		mDirectionsResultLayerInterface = new DirectionsResultLayer(mDisplayAbstract, this);
		mWikipediaLayerInterface        = new GeonamesLayer(mDisplayAbstract, this, mDisplayAbstract.getLanguage());
		mPhotosLayerInterface           = new PanoramioLayer(mDisplayAbstract, this);
		
		// Init markers types
		Marker.initMarkerTypes(mDisplayAbstract);
		
		InfoWindow.ADD_TYPE(InfoWindow.TYPE.NORMAL, "infoWindow_normal.9.png", mDisplayAbstract);
		
		mMapMode              = MAP_MODE.MAP;
		mResultsDisplayMode   = RESULT_MODE.MAP;
		mGPSAvailable         = false;
		mResultsMode          = false;
		mDirectionsResultMode = false;
		mTrafficLayerOn       = true;
		mWikipediaLayerOn     = false;
		mPhotosLayerOn        = false;
		
		mDisplayAbstract.setMapMode(MAP_MODE.MAP);
		/* Managing to display permanently My location Layer */
		if (mDisplayAbstract.getAPILayersList().contains(API_LAYER.MY_LOCATION)) {
			/* Case built-in My Location layer */
			mDisplayAbstract.drawAPILayer(API_LAYER.MY_LOCATION);
		}
		else {
			/* Case external My Location layer */
		}
		
		// Init map mode from settings
		setMapMode(Settings.getMapModeSetting(mDisplayAbstract));
		// Init layers modes from saved settings
		setWikipediaMode(Settings.getWikipediaLayerSetting(mDisplayAbstract));
		setPhotosMode(Settings.getPhotosLayerSetting(mDisplayAbstract));
		setTrafficMode(Settings.getTrafficLayerSetting(mDisplayAbstract));
		// Init result display mode
		setResultsDisplayMode(Settings.getResultsDisplayModeSetting(mDisplayAbstract));
		
		// Go to current location (or last known location)
		// at application launch
		// or display an error message if no location is known
		//checkLocationAvailability();
		
		// Check network availability
		mNetworkStatus = NetworkStatus.getInstance(mDisplayAbstract);
		mNetworkStatus.registerListener(this);
		mNetworkStatus.checkNetworkAvailability();
		
		mTTSManager = TTSManager.getInstance(mDisplayAbstract);
		
		// Init route and local search cancel listeners
		initOnCancelListeners();
	}

	/**
	 * Called when the activity is destroyed.
	 */
	private void destroy() {
		PLog.d(TAG,"destroy");
		// Call method to cleanly destroy layers objects
		mResultsLayerInterface.destroy();
		mDirectionsResultLayerInterface.destroy();
		mWikipediaLayerInterface.destroy();
		mPhotosLayerInterface.destroy();
		NetworkStatus.getInstance(mDisplayAbstract).unregisterReceivers();
		mLooper.quit();
		mHandler = null;
		// If not displaying directions results
		// we assume that the application is being closed
		onApplicationClose();
	}
	
	/**
	 * Called when the application is closed.
	 */
	private void onApplicationClose() {
		PLog.w(TAG,"onApplicationClose");
		NetworkStatus.deleteInstance();
		GpsChecker.stopGpsCheck();
		TTSManager.deleteInstance();
		GoogleAjaxLocalSearchHistoric.deleteInstance();
		Process.killProcess(Process.myPid());
	}
	
	
	/**
	 * Operation
	 * 
	 */
	private void initializeHandler() {
		mThread = new HandlerThread("Controller", Process.THREAD_PRIORITY_DEFAULT);
        mThread.start();
        mLooper = mThread.getLooper();
        
		mHandler = new Handler(mLooper) {
			
			/**
			 * Allows Controller to gets all messages in another thread,
			 * letting the UIThread display stuff.
			 * @param msg Message from a Layer or an AbstractDisplay
			 */
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case MSG_MY_LOCATION_BUTTON:
						goToMyLocation();
						break;
					case MSG_NEW_GPS_DATA:
						switch (msg.arg1) {
							case LocationProvider.AVAILABLE:
								mGPSAvailable = true;
								mLastPositionKnown = (LatLng)(msg.obj);
//								PLog.v(TAG,"Received new gps position: "+mLastPositionKnown);
								if( !mLocationFirstFixReceived ) {
									mLocationFirstFixReceived = true;
									goToMyLocation();
								}
								else if( GlobalState.mCurrentLocationTracking ) {
									currentLocationTracking(mLastPositionKnown,msg.arg2);
								}
								// Refresh results list display
								// (update display of distance between a POI and the current position)
								updateResultsListDisplay(mLastPositionKnown);
								// Automatic selection (if enabled) of the current direction instruction 
								// CR#0064661: feature disabled for first release
								// If enabled, it would be too closed to a turn-by-turn navigation
//								updateCurrentDirectionInstruction(mLastPositionKnown);
								break;
							case LocationProvider.TEMPORARILY_UNAVAILABLE:
								mGPSAvailable = false;
								break;
							case LocationProvider.OUT_OF_SERVICE:
								mGPSAvailable = false;
								break;
						}
						break;
					case MSG_ZOOM_IN:
						mDisplayAbstract.zoomIn();
						break;
					case MSG_ZOOM_OUT:
						mDisplayAbstract.zoomOut();
						break;
					case MSG_POSITION_CHANGED:
						LatLngBounds bounds = mDisplayAbstract.getLatLngBounds();
						if (mWikipediaLayerOn) {
							mWikipediaLayerInterface.update(bounds);
							mDisplayAbstract.updateLayer((Layer)mWikipediaLayerInterface);
						}
						if (mPhotosLayerOn) {
							mPhotosLayerInterface.update(bounds);
							mDisplayAbstract.updateLayer((Layer)mPhotosLayerInterface);
						}
						break;
					case MSG_MENU_OPTIONS_ITEM_SELECTED:
						onOptionsItemSelected((MenuItem)msg.obj);
						break;
					case MSG_KEYB_ON_KEY_DOWN:
						onKeyDown(msg.arg1, (KeyEvent)msg.obj);
						break;
					case MSG_KEYB_ON_KEY_UP:
						onKeyUp(msg.arg1, (KeyEvent)msg.obj);
						break;
					case MSG_USER_TOUCH_ACTION_MOVE:
						break;
					case MSG_SEARCH_RESULTS:
						mDisplayAbstract.dismissDialog(DisplayAbstract.LOCAL_SEARCH_PROG_DIALOG_KEY);
						boolean success = false;
						if (msg.arg1 == LocalSearchLayerInterface.SEARCH_RESULT.OK.ordinal()) {						
							/* Positioning on first marker, if exists... */
							LinkedList<Marker> markerList = mResultsLayerInterface.getMarkersList();
							if (markerList != null && markerList.size() > 0) {
								mItResult = markerList.listIterator();
								if (mGoToLastMarker)
								{
									while(mItResult.hasNext())
									{
										mCurrentMarker = mItResult.next();
									}
									mGoToLastMarker = false;
								}
								else
								{
									mCurrentMarker = mItResult.next();
								}
								mDisplayAbstract.setPrevNextArrowsState(true, mItResult.hasPrevious(), mItResult.hasNext());
								setResultsMode(true);
								mDisplayAbstract.drawInfoWindow(mCurrentMarker, mResultsLayerInterface);
								goToLocation(mCurrentMarker.getLatLng());
								// Adjust zoom level to see all results
								mDisplayAbstract.setZoomToSpan(mResultsLayerInterface.getBoundsCenteredOnMarker(mCurrentMarker));
								updateCurrenResultIndex();
								notifyMarkerChange();
								success = true;
							}
						}
						if (!success ) {
							if( mSearchQueryCanceled
									|| (msg.arg1 == LocalSearchLayerInterface.SEARCH_RESULT.FAILED_CANCELED.ordinal()) ) {
								// Display no error message when search has been canceled
							}
							else if( msg.arg1 == LocalSearchLayerInterface.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT.ordinal() ) {
								mDisplayAbstract.showToast(R.string.network_error);
							}
							else {
								mDisplayAbstract.showToast(R.string.search_no_result);
							}
						}
						break;
					case MSG_DIRECTIONS_RESULT:
						mDisplayAbstract.dismissDialog(DisplayAbstract.DIRECTIONS_SEARCH_PROG_DIALOG_KEY);
						success = false;
						if (msg.arg1 == DirectionsResultLayer.SEARCH_RESULT.OK.ordinal()) {
							mDirectionsResultLayerInterface = (DirectionsResultLayerInterface)msg.obj;
							/* Positioning on first marker of first step, if exists... */
							LinkedList<Polyline> stepList = mDirectionsResultLayerInterface.getPolylinesList();
							LinkedList<Marker> markerList = mDirectionsResultLayerInterface.getMarkersList();
							if( (markerList != null)
									&& (markerList.size() > 0)
									&& (!mRouteQueryCanceled) ) {
								if (mResultsMode) {
									setResultsMode(false);
								}
								mItDirectionsStep = stepList.listIterator();
								mItResult         = markerList.listIterator();
								mCurrentStep   = mItDirectionsStep.next();
								mCurrentMarker = mItResult.next();
								mDisplayAbstract.setPrevNextArrowsState(true, false, mItDirectionsStep.hasNext());
								mDisplayAbstract.drawInfoWindow(mCurrentMarker, mDirectionsResultLayerInterface);
								goToLocation(mCurrentMarker.getLatLng());
								// Adjust zoom level to see all route
								mDisplayAbstract.setZoomToSpan(mDirectionsResultLayerInterface.getBoundsCenteredOnMarker(mCurrentMarker));
								setDirectionsResultMode(true);
								notifyMarkerChange();
								success = true;
							}
						}
						if( !success ) {
							if( mRouteQueryCanceled
									|| (msg.arg1 == DirectionsResultLayer.SEARCH_RESULT.FAILED_CANCELED.ordinal()) ) {
								// Display no error message when search has been canceled
								PLog.d(TAG,"handleMessage - Directions, no result, search canceled");
							}
							else if( msg.arg1 == DirectionsResultLayer.SEARCH_RESULT.FAILED_NETWORK_TIMEOUT.ordinal() ) {
								mDisplayAbstract.showToast(R.string.network_error);
								PLog.d(TAG,"handleMessage - Directions, no result, network timeout");
							}
							else {
								mDisplayAbstract.showToast(R.string.search_no_result);
								PLog.d(TAG,"handleMessage - Directions, no result, err "+msg.arg1);
							}
							// Close directions display activity if it failed to find route
							mDisplayAbstract.finish();
						}
						break;
					case MSG_USER_CLICK_BUTTON_PREV:
						if (mDirectionsResultMode) {
							previousStep();
						}
						else if (mResultsMode) {
							previousResult();
						}
						break;
					case MSG_USER_CLICK_BUTTON_NEXT:
						if (mDirectionsResultMode) {
							nextStep();
						}
						else if (mResultsMode) {
							nextResult();
						}
						break;
					case MSG_USER_TAP_MARKERS:
						PLog.d(TAG, "handleMessage MSG_USER_TAP_MARKERS");
						manageTappedMarkers( mTappedMarkers );
						break;
					case MSG_USER_SELECT_ITEM:
						PLog.d(TAG, "handleMessage MSG_USER_TAP_MARKERS");
						long id = (Long)(msg.obj);
						Marker marker = null;
						boolean found = false;
						if (mResultsMode &&
								mResultsLayerInterface.getMarkersList() != null &&
								mResultsLayerInterface.getMarkersList().get(msg.arg1) != null &&
								mResultsLayerInterface.getMarkersList().get(msg.arg1).getId() == id) {
							marker = mResultsLayerInterface.getMarkersList().get(msg.arg1);
							found = true;
						}
						else if (mDirectionsResultMode &&
								mDirectionsResultLayerInterface.getMarkersList() != null &&
								mDirectionsResultLayerInterface.getMarkersList().get(msg.arg1) != null /*&&
								mDirectionsResultLayerInterface.getMarkersList().get(msg.arg1).getId() == id*/){
							marker = mDirectionsResultLayerInterface.getMarkersList().get(msg.arg1);
							found = true;
						}
						
						if (found) {
							PLog.d(TAG, "Item selected : ", marker.getTitle());
							if (mResultsMode) {
								if( marker != mCurrentMarker ) {
									// If the selected item is not the current marker
									// center the map on this POI
									goToResult(marker);
								}
								else {
									// If the selected item is the current marker
									// show the details of this POI
									marker.processAction(mDisplayAbstract, Controller.this);
								}
							}
							else if (mDirectionsResultMode) {
								goToStep(marker,true);
							}
						}
						else {
							PLog.e(TAG, "Impossible to find item selected.");
						}
						
						break;

					// Message received when activity is resumed
					case MSG_RESUMED:
						PLog.v(TAG, "HandleMessage - MSG_RESUMED");
						// Update results list display mode
						// because, the list is hidden in onPause() method
						// for a workaround of a bug in MapView display
						updateResultsListDisplayMode();
						// Send position change command
						// to refresh wikipedia and photo layers
						// message is sent with a delay in order to
						// wait that the map view has the good center and span values
						mHandler.sendEmptyMessageDelayed(MSG_POSITION_CHANGED,2000);
						// When activity is resumed in POI mode,
						// automatically center the map on the selected marker
						if( mResultsMode
								&& !GlobalState.mCurrentLocationTracking
								&& (-1 != GlobalState.mCurrentPOIIndex) ) {
							List<Marker> list = mResultsLayerInterface.getMarkersList();
							if( GlobalState.mCurrentPOIIndex < list.size() ) {
								goToResult(list.get(GlobalState.mCurrentPOIIndex));
							}
						}
						break;
						
					// Message received when the activity is destroyed
					case MSG_DESTROYED:
						destroy();
						break;
					
					// Trigger a refresh of map display
					case MSG_INVALIDATE_DISPLAY:
						mDisplayAbstract.invalidate();
						break;
						
					// Received when network status is available
					case MSG_NETWORK_STATUS_READY:
						checkNetworkAvailability();
						break;
						
					// Received when GPS status is available
					case MSG_GPS_CHECK_RESULT:
						if( msg.arg1 == 0 ) {
							mDisplayAbstract.showToast(R.string.err_msg_appli_no_location);
						}
						break;
						
					default:
						PLog.e(TAG, "Unknown incoming message : ", msg.what);
						break;
				}
			}
		};
	}
	
	/**
	 * Gets the handler of this class, in order to attach this controller
	 * to a view (MapViewDisplay, ...).
	 * @return The class handler to send to it messages
	 */
	public Handler getHandler() {
		return mHandler;
	}

	/**
	 * 
	 * @param latlngUpdate
	 * @param accuracy Position accuracy in dm
	 */
	private void currentLocationTracking(LatLng newPos, int accuracy) {
		LatLngBounds bounds = mDisplayAbstract.getLatLngBounds();
		LatLng curCenter = bounds.getCenter();
		if( (Math.abs( newPos.mLatE6 - curCenter.mLatE6 ) > ((bounds.getLatSpanE6()/2)/3))
				|| ((Math.abs( newPos.mLngE6 - curCenter.mLngE6 ) > ((bounds.getLngSpanE6()/2)/3))) ) {
			PLog.v(TAG,"currentLocationTracking - update map center");
			goToMyLocation();
		}
	}
	
	/**
	 * Tries to go to last position known.
	 * Goes there if ok, shows a toast otherwise.
	 */
	private void goToMyLocation() {
		LatLng loc = getLastKnownLocation();
		if( null != loc ) {
			mDisplayAbstract.setCenter(loc);
			GlobalState.mCurrentLocationTracking = true;
		}
		else {
			mDisplayAbstract.showToast(R.string.mylocation_unknown);
		}
	}
	
	/**
	 * Go to a given location.
	 * Turns off the current position tracking mode.
	 * @param latlng
	 */
	private void goToLocation( LatLng latlng ) {
		mDisplayAbstract.setCenter(latlng);
		GlobalState.mCurrentLocationTracking = false;
	}
	
	public LatLng getLastKnownLocation() {
		return mDisplayAbstract.getLastKnownLocation();
	}
	
	/**
	 * Get bounds for local search.
	 * If a GPS location has been received, the local search
	 * will be done around this location, with current zoom level of map display.
	 * Otherwise, the local search is done on the displayed area.
	 * @return The area where the local search shall be performed.   
	 */
	private LatLngBounds getSearchBounds() {
		LatLngBounds bounds = null;
		LatLngBounds displayBounds = mDisplayAbstract.getLatLngBounds();
		if( null != displayBounds ) {
			if (mGPSAvailable
					&& (null != mLastPositionKnown)) {
				bounds = new LatLngBounds( mLastPositionKnown,
						                   displayBounds.getLatSpanE6(),
						                   displayBounds.getLngSpanE6() );
			}
			if( null == bounds ) {
				bounds = new LatLngBounds(displayBounds);
			}
		}
		if( null == bounds ) {
			// Cannot define bounds, return dummy bounds
			bounds = new LatLngBounds(0,0,0,0);
		}
		return bounds;
	}

	/**
	 * Centers the map on the previous result found in results layer.
	 * Sets state of back and next button after.
	 * This method supposes that a result is already focused.
	 * @see nextResult
	 */
	private void previousResult() {
		if (mResultsMode) {
			if (mItResult != null) {
				Marker lastMarker = mCurrentMarker;
				try {
					mCurrentMarker = mItResult.previous();
					/* A bug ? if previous() and then next() on iterator,
					 * the first call to previous() returns current marker.
					 */
					if (mCurrentMarker == lastMarker) {
						mCurrentMarker = mItResult.previous();
					}
					goToResult(mCurrentMarker);
				}
				catch (NoSuchElementException e) {
					PLog.i(TAG, "Trying to get previous result while begining of list is reached.");
					previousLocalSearch();
				}
			}
			else {
				PLog.e(TAG, "Trying to get previous result while results mode is on and result iterator is null (Bug).");
			}
		}
		else {
			PLog.i(TAG, "Trying to get previous result while not in result mode.");
		}
	}
	
	/**
	 * Centers the map on the first or the next result found in results layer.
	 * Sets state of back and next button after.
	 * This method supposes that a result is already focused.
	 * @see previousResult
	 */
	private void nextResult() {
		if (mResultsMode) {
			if (mItResult != null) {
				Marker lastMarker = mCurrentMarker;
				try {
					mCurrentMarker = mItResult.next();
					/* A bug ? if next() and then previous() on iterator,
					 * the first call to previous() returns current marker.
					 */
					if (mCurrentMarker == lastMarker) {
						mCurrentMarker = mItResult.next();
					}
					goToResult(mCurrentMarker);
				}
				catch (NoSuchElementException e) {
					PLog.i(TAG, "Trying to get next result while end of list is reached.");
					nextLocalSearch();
				}
			}
			else {
				PLog.e(TAG, "Trying to get next result while results mode is on and result iterator is null (Bug).");
			}
		}
		else {
			PLog.i(TAG, "Trying to get next result while not in result mode.");
		}
	}
	
	/**
	 * Centers the map on a Marker and display its InfoWindow.
	 * Close the last InfoWindow before.
	 * The marker must be in ResultsList and we must be in results mode.
	 * @param marker Marker we want to center to
	 */
	private void goToResult(Marker marker) {
		if (mResultsMode) {
			LinkedList<Marker> list = mResultsLayerInterface.getMarkersList();
			if (list != null && list.contains(marker)) {
				mItResult = list.listIterator();
				mCurrentMarker = null;
				while (mCurrentMarker != marker && mItResult.hasNext()) {
					mCurrentMarker = mItResult.next();
				}
				if( null != mCurrentMarker ) {
					updateCurrenResultIndex();
					notifyMarkerChange();
					mDisplayAbstract.removeInfoWindow(mCurrentMarker, mResultsLayerInterface);
					boolean hasNext = mItResult.hasNext();
					mItResult.previous();
					boolean hasPrevious = mItResult.hasPrevious();
					mDisplayAbstract.setPrevNextArrowsState(true, hasPrevious, hasNext);
					mDisplayAbstract.drawInfoWindow(mCurrentMarker, mResultsLayerInterface);
					goToLocation(mCurrentMarker.getLatLng());
					// Search marker position and set list selection
					for( int pos=0; pos<list.size(); pos++) {
						if( list.get(pos) == mCurrentMarker ) {
							mDisplayAbstract.setPoiOrResultsListSelection(pos);
							break;
						}
					}
				}
			}
			else {
				PLog.e(TAG, "Error, marker \"", marker.getTitle(), "\" to be centered not in Results list (bug)");
			}
		}
		else {
			PLog.e(TAG, "Error, marker \"", marker.getTitle(), "\" to be centered wont be because not in restuls mode.");
		}
	}
	/**
	 * Centers the map on the previous step in directions result layer.
	 * Sets state of back and next button after.
	 * @see nextStep
	 */
	private void previousStep() {
		if (mDirectionsResultMode) {
			if (mItDirectionsStep != null) {
				Polyline lastStep   = mCurrentStep;
				Marker   lastMarker = mCurrentMarker;
				try {
					mCurrentStep   = mItDirectionsStep.previous();
					mCurrentMarker = mItResult.previous();
					/* A bug ? if next() and then previous() on iterator,
					 * the first call to previous() returns current marker.
					 */
					if (mCurrentStep == lastStep) {
						mCurrentStep = mItDirectionsStep.previous();
					}
					if (mCurrentMarker == lastMarker) {
						mCurrentMarker = mItResult.previous();
					}
					goToStep(mCurrentMarker,true);
				}
				catch (NoSuchElementException e) {
					PLog.i(TAG, "Trying to get previous step while begining of list is reached.");
				}
			}
			else {
				PLog.e(TAG, "Trying to get previous step while Directions Result mode is on and step iterator is null (Bug).");
			}
		}
		else {
			PLog.i(TAG, "Trying to get previous step while not in Directions Result mode.");
		}
	}
	/**
	 * Centers the map on the first or the next step found in directions result layer.
	 * Sets state of back and next button after.
	 * @see previousResult
	 */
	private void nextStep() {
		if (mDirectionsResultMode) {
			if (mItDirectionsStep != null) {
				Polyline lastStep   = mCurrentStep;
				Marker   lastMarker = mCurrentMarker;
				try {
					mCurrentStep   = mItDirectionsStep.next();
					mCurrentMarker = mItResult.next();
					/* A bug ? if previous() and then next() on iterator,
					 * the first call to next() returns current marker.
					 */
					if (mCurrentStep == lastStep) {
						mCurrentStep = mItDirectionsStep.next();
					}
					if (mCurrentMarker == lastMarker) {
						mCurrentMarker = mItResult.next();
					}
					goToStep(mCurrentMarker,true);	
				}
				catch (NoSuchElementException e) {
					PLog.i(TAG, "Trying to get next step while begining of list is reached.");
				}
			}
			else {
				PLog.e(TAG, "Trying to get next step while Directions Result mode is on and step iterator is null (Bug).");
			}
		}
		else {
			PLog.i(TAG, "Trying to get next step while not in Directions Result mode.");
		}
	}
	
	/**
	 * Centers the map on a Step and display its InfoWindow.
	 * Close the last InfoWindow before.
	 * The step must be in DirectionsResultList and we must be in DirectionsResult mode.
	 * @param polyline Polyline we want to center to
	 */
	private void goToStep(Marker marker, boolean setMapCenter) {
		if (mDirectionsResultMode) {
			LinkedList<Marker>   markerList   = mDirectionsResultLayerInterface.getMarkersList();
			LinkedList<Polyline> polylineList = mDirectionsResultLayerInterface.getPolylinesList();
			if (markerList != null && markerList.contains(marker)) {
				mDisplayAbstract.removeInfoWindow(mCurrentMarker, mResultsLayerInterface);
				
				mItResult         = markerList.listIterator();
				mItDirectionsStep = polylineList.listIterator();
				mCurrentMarker = null;
				mCurrentStep = null;
				while (mCurrentMarker != marker && mItResult.hasNext()) {
					mCurrentMarker = mItResult.next();
					mCurrentStep = mItDirectionsStep.next();
				}
				if( null != mCurrentMarker ) {
					notifyMarkerChange();
					boolean hasNext = mItResult.hasNext();
					mItResult.previous();
					boolean hasPrevious = mItResult.hasPrevious();
					mDisplayAbstract.setPrevNextArrowsState(true, hasPrevious, hasNext);
					mDisplayAbstract.drawInfoWindow(mCurrentMarker, mDirectionsResultLayerInterface);
					if( setMapCenter ) {
						goToLocation(mCurrentMarker.getLatLng());
					}
					// Search marker position and set list selection
					for( int pos=0; pos<markerList.size(); pos++) {
						if( markerList.get(pos) == mCurrentMarker ) {
							mDisplayAbstract.setPoiOrResultsListSelection(pos);
							break;
						}
					}
				}
			}
			else {
				PLog.e(TAG, "Error, step \"", marker.getTitle(), "\" to be centered not in DirectionsResult list (bug)");
			}
		}
		else {
			PLog.e(TAG, "Error, step \"", marker.getTitle(), "\" to be centered wont be because not in DirectionsResult mode.");
		}
	}
	
	/**
	 * Period for auto-selection of current direction instruction.
	 */
	private static final long CURRENT_DIRECTION_INSTRUCTION_UPDATE_PERIOD = 5000000000L;
	/**
	 * Last time the auto-selection of current direction instruction as been done.
	 */
	private long mLastCurrentDirectionInstrctionUpdateDate = 0;
	
	/**
	 * Auto-selection of the current direction instruction.
	 * This auto-selection is done only in directions mode and in current location tracking mode.
	 * @param positionUpdate New current position.
	 */
	private void updateCurrentDirectionInstruction(LatLng positionUpdate) {
		if (mDirectionsResultMode
				&& GlobalState.mCurrentLocationTracking ) {
			long curTime = System.nanoTime(); 
			PLog.w(TAG,"updateCurrentDirectionInstruction - ",curTime);
			if( (curTime-mLastCurrentDirectionInstrctionUpdateDate) > CURRENT_DIRECTION_INSTRUCTION_UPDATE_PERIOD ) {
				mLastCurrentDirectionInstrctionUpdateDate = curTime;
				int step = mDirectionsResultLayerInterface.getNextInstruction(positionUpdate);
				LinkedList<Marker> markerList = mDirectionsResultLayerInterface.getMarkersList();
				if( (null != markerList)
						&& (step >= 0)
						&& (step < markerList.size()) ) {
					goToStep(markerList.get(step),false);
				}
			}
		}
	}

	
	/**
	 * Operation
	 * 
	 */
	public void onPrepareOptionsMenu(Menu menu) {
		// Traffic button
		menu.findItem(R.id.options_menu_item_traffic).setChecked(mTrafficLayerOn);
		
		/* Managing Layers... */
		menu.findItem(R.id.layer_traffic).setChecked(mTrafficLayerOn);
		menu.findItem(R.id.layer_wikipedia).setChecked(mWikipediaLayerOn);
		menu.findItem(R.id.layer_photos).setChecked(mPhotosLayerOn);
		
		/* Managing Results display button and Directions Results display button. */
		menu.findItem(R.id.options_menu_item_result_display).setVisible(mResultsMode);
		menu.findItem(R.id.results_display_result_list).setVisible(mResultsMode);
		menu.findItem(R.id.options_menu_item_directions_display).setVisible(mDirectionsResultMode);
		menu.findItem(R.id.directions_display_instruction_list).setVisible(mDirectionsResultMode);
		
		menu.findItem(R.id.results_display_map).setChecked(false);
		menu.findItem(R.id.results_display_result_list).setChecked(false);
		menu.findItem(R.id.results_display_map_and_result_list).setChecked(false);
		menu.findItem(R.id.directions_display_map).setChecked(false);
		menu.findItem(R.id.directions_display_instruction_list).setChecked(false);
		menu.findItem(R.id.directions_display_map_and_instruction_list).setChecked(false);
		
		if (mResultsDisplayMode == RESULT_MODE.MAP) {
			menu.findItem(R.id.results_display_map).setChecked(true);
			menu.findItem(R.id.directions_display_map).setChecked(true);
		}
//		else if (mResultsDisplayMode == RESULT_MODE.LIST) {
//			menu.findItem(R.id.results_display_result_list).setChecked(true);
//			menu.findItem(R.id.directions_display_instruction_list).setChecked(true);
//		}
		else if (mResultsDisplayMode == RESULT_MODE.HYBRID) {
			menu.findItem(R.id.results_display_map_and_result_list).setChecked(true);
			menu.findItem(R.id.directions_display_map_and_instruction_list).setChecked(true);
		}
		
		/* Managing map mode... */
		menu.findItem(R.id.map_mode_map).setChecked(false);
		menu.findItem(R.id.map_mode_satellite).setChecked(false);
		menu.findItem(R.id.map_mode_hybrid).setChecked(false);
		menu.findItem(R.id.map_mode_terrain).setChecked(false);
		switch (mMapMode) {
			case MAP:       menu.findItem(R.id.map_mode_map).setChecked(true);       break;
			case SATELLITE: menu.findItem(R.id.map_mode_satellite).setChecked(true); break;
			case HYBRID:    menu.findItem(R.id.map_mode_hybrid).setChecked(true);    break;
			case TERRAIN:   menu.findItem(R.id.map_mode_terrain).setChecked(true);    break;
		}
		
		// TTS activation button
		menu.findItem(R.id.options_menu_item_activate_tts).setChecked(mTTSManager.isActivated);
		
	    // Check if clean map button is visible
	    if( hasResultsOrDirectionsMarkersDisplayed() ) {
	    	menu.findItem(R.id.options_menu_item_clean_map).setVisible(true);
	    }
	    else {
	    	menu.findItem(R.id.options_menu_item_clean_map).setVisible(false);
	    }
	    

	}

	/**
	 * Does all controllers actions when a menu option has been selected.
	 * This method is called by view (CommonMapDisplay)
	 * In particular, modifies internal states and send orders to modules.
	 * @param item Menu Item selected
	 * @see CommonMapDisplay.onOptionsItemSelected()
	 */
	private void onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch( item.getItemId() ) {
		
		case R.id.options_menu_item_quick_search: {
			// Launch search by category activity
			intent = new Intent(mDisplayAbstract,QuickSearchListActivity.class);
			intent.setAction(Intent.ACTION_SEARCH);
			mDisplayAbstract.startActivity(intent);
			break;
		}
		
		case R.id.options_menu_item_search:
			mDisplayAbstract.onSearchRequested();
			break;
			
		case R.id.options_menu_item_mylocation:
			goToMyLocation();
			break;
			
		case R.id.map_mode_map:
			setMapMode(MAP_MODE.MAP);
			break;
			
		case R.id.map_mode_satellite:
			setMapMode(MAP_MODE.SATELLITE);
			break;

		case R.id.map_mode_hybrid:
			setMapMode(MAP_MODE.HYBRID);
			break;

		case R.id.map_mode_terrain:
			setMapMode(MAP_MODE.TERRAIN);
			break;
		
		case R.id.results_display_map:
		case R.id.directions_display_map:
			setResultsDisplayMode(RESULT_MODE.MAP);
			break;

		case R.id.results_display_result_list:
			intent = new Intent();
			intent.setClass(mDisplayAbstract, ResultsListActivity.class);
			LatLng latlng = mDisplayAbstract.getLastKnownLocation();
			intent.putExtra(ResultsListActivity.COM_PARROT_RESLIST_CENTER, latlng);
			intent.putExtra(ResultsListActivity.COM_PARROT_RESLIST_LAYER, (Layer)mResultsLayerInterface);
			mDisplayAbstract.startActivityForResult(intent, REQUEST_SELECT_RESULT);
			break;
		
		case R.id.directions_display_instruction_list:
			long currentMarkerId = Long.MAX_VALUE;
			if( (null != mCurrentMarker)
					&& ( (mCurrentMarker.getType()==Marker.TYPE.DIRECTIONS_START)
							|| (mCurrentMarker.getType()==Marker.TYPE.DIRECTIONS_STEP)
							|| (mCurrentMarker.getType()==Marker.TYPE.DIRECTIONS_END))) {
				currentMarkerId = mCurrentMarker.getId();
			}
			startDirectionsListActivity(currentMarkerId);
			break;
        
		/* Setting the list adapter when list is required */
		case R.id.results_display_map_and_result_list:
		case R.id.directions_display_map_and_instruction_list:
			setResultsDisplayMode(RESULT_MODE.HYBRID);
			break;
		
		case R.id.options_menu_item_directions:
			intent = new Intent(mDisplayAbstract,DirectionsEntryActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			LatLngBounds bounds = null;
			if (mGPSAvailable
					&& (mLastPositionKnown != null)) {
				bounds = new LatLngBounds(mLastPositionKnown,0,0);
			}
			if( null == bounds ) {
				bounds = mDisplayAbstract.getLatLngBounds();
			}
			intent.putExtra(DirectionsEntryActivity.DIRECTION_ENTRY_EXTRA_BOUNDS, bounds);
			mDisplayAbstract.startActivity(intent);
            break;		

		case R.id.layer_traffic:
		case R.id.options_menu_item_traffic:
			setTrafficMode(!mTrafficLayerOn);
            break;
            
		case R.id.layer_wikipedia:
			Settings.setWikipediaLayerSetting(mDisplayAbstract, !mWikipediaLayerOn);
			setWikipediaMode(!mWikipediaLayerOn);
            break;
            
		case R.id.layer_photos:
			Settings.setPhotosLayerSetting(mDisplayAbstract, !mPhotosLayerOn);
			setPhotosMode(!mPhotosLayerOn);
            break;
            
		case R.id.options_menu_item_clean_map:
			cleanMap();
			break;
			
		case R.id.options_menu_item_activate_tts:
			mTTSManager.isActivated = !mTTSManager.isActivated;
			Settings.setTTSActivationSetting(mDisplayAbstract, mTTSManager.isActivated);
			break;
        
		case R.id.options_menu_item_exit:
			// Exit the application
			ExitBroadcaster.broadcastExitEvent(mDisplayAbstract);
			break;
            
		default:
			PLog.e(TAG,"onOptionsItemSelected - Unknown selected item");
    	   	break;
		}
	}
	
	
	/**
	 * Do requested operations when user has hit a button.
	 * Memorizes when user hit for a while center button.
	 * @param keyCode Code of key hit
	 * @param event Event with precisions on the key hit
	 */
	private void onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
				// Map zoom out
				mDisplayAbstract.zoomOut();
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				// Map zoom in
				mDisplayAbstract.zoomIn();
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				// Move to previous result item
				if (mResultsMode) {
					previousResult();
				}
				else if (mDirectionsResultMode && mItDirectionsStep.hasPrevious()) {
					previousStep();
				}
				break;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				if (mResultsMode) {
					nextResult();
				}
				else if (mDirectionsResultMode && mItDirectionsStep.hasNext()) {
					nextStep();
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if(!mCenterKeyLongPressed) {
					if( (event.getEventTime()-event.getDownTime()) > LONG_KEY_PRESS_MS ) {
						mCenterKeyLongPressed = true;
						// If this is a long key press
						// Go to current location
						goToMyLocation();
					}
				}
				break;
			case KeyEvent.KEYCODE_BACK:
			    // Check if there are some POIs visible on map
				// if true, clean the map
			    if( hasResultsMarkersDisplayed() ) {
			    	cleanMap();
			    }
				break;
			default:
				// Do nothing
				break;
		}
	}
	
	/**
	 * Do requested operations when user has release a button.
	 * Only reacts on center button, when no long press has
	 * been memorized.
	 * @param keyCode Code of key hit
	 * @param event Event with precisions on the key hit
	 */
	private void onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if( !mCenterKeyLongPressed ) {
					if( null != mCurrentMarker ) {
						mCurrentMarker.processAction(mDisplayAbstract, this);
					}
				}
				mCenterKeyLongPressed = false;
				break;
			default:
				// Do nothing
				break;
		}
	}


	/**
	 * What to do when an activity previously called returns a result.
	 * 
	 * @param requestCode Value used to find which activity was called
	 * @param resultCode Result code
	 * @param data Optional intent containing other values returned
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		PLog.d(TAG,"onActivityResult requestCode:",requestCode," resultCode:",resultCode);
		if (mResultsMode)
		{
			int googleCurrentSearchPage = GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage();
			if (((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).getCurrentPage() != googleCurrentSearchPage)
			{
				LinkedList<Marker> resultmarkers = GoogleAjaxLocalSearchHistoric.getInstance().getMarkerFromCurrentPage();
				mResultsLayerInterface.setMarkersList(resultmarkers);
				((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).setCurrentPage(googleCurrentSearchPage);
			}
		}
		switch (requestCode) {
			case REQUEST_SELECT_RESULT:
				if (resultCode == Activity.RESULT_OK) {
					long id = data.getLongExtra(ResultsListActivity.RETURNED_INTENT_EXTRA_RESULT_ID, -1);
					if (id != -1) {
						boolean found = false;
						Marker marker = null;
						if (mResultsMode) {
							Iterator<Marker> it = mResultsLayerInterface.getMarkersList().iterator();
							while (!found && it.hasNext()) {
								marker = it.next();
								if (marker.getId() == id) {
									found = true;
								}
							}
							if (found) {
								goToResult(marker);
							}
							else {
								PLog.e(TAG, "onActivityResult error : marker ID returned not found.");
								
							}
						}
					}
					else {
						PLog.e(TAG, "onActivityResult error : no marker ID returned although RESULT_OK was sent.");
					}
					
				}
				break;
				
			case REQUEST_SELECT_DIRECTIONS_STEP:
				if (resultCode == Activity.RESULT_OK) {
					long id = data.getLongExtra(DirectionsListActivity.RETURNED_INTENT_EXTRA_STEP_ID, -1);
					if (id != -1) {
						boolean found = false;
						Marker marker = null;
						if (mDirectionsResultMode) {
							Iterator<Marker> it = mDirectionsResultLayerInterface.getMarkersList().iterator();
							while (!found && it.hasNext()) {
								marker = it.next();
								if (marker.getId() == id) {
									found = true;
								}
							}
						}
						if (found) {
							goToStep(marker,true);
						}
						else {
							PLog.e(TAG, "onActivityResult error : marker ID returned not found.");
						}
					}
					else {
						PLog.e(TAG, "onActivityResult error : no marker ID returned although RESULT_OK was sent.");
					}
					
				}
				break;
		}
	}

	/**
	 * Set the map mode.
	 * @param mapMode Map mode.
	 */
	private void setMapMode(MAP_MODE mapMode) {
		mDisplayAbstract.setMapMode(mapMode);
		mMapMode = mapMode;
		Settings.setMapModeSetting(mDisplayAbstract, mapMode);
	}

	/**
	 * Sets Results mode on or off.
	 * When on is asked, sets Directions Result mode off if necessary and draw
	 * Directions Layer. Then sets Results mode to on.
	 * When off is asked, removes Results layer and sets Results mode
	 * to off.
	 * @param value on or off
	 */
	private void setResultsMode (boolean value) {
		if (mResultsMode == value) {
			PLog.d(TAG, "Trying to set Results mode to its current value :", value);
		}
		else {
			if (value) {
				if (mDirectionsResultMode) {
					setDirectionsResultMode(false);
				}
				if (mWikipediaLayerOn) {
					setWikipediaMode(false);
				}
				if (mPhotosLayerOn) {
					setPhotosMode(false);
				}
				mDisplayAbstract.drawLayer((Layer)mResultsLayerInterface);
				//mDisplayInterface.panToBounds(mResultsLayerInterface.getBounds());
				mResultsMode = true;
			}
			else {
				mDisplayAbstract.removeInfoWindow(mCurrentMarker, mResultsLayerInterface);
				mDisplayAbstract.removeLayer((Layer)mResultsLayerInterface);
				mResultsLayerInterface.clearLayer();
				mItResult = null;
				mDisplayAbstract.setPrevNextArrowsState(false, false, false);
				
				mResultsMode = false;
			}
			updateResultsListDisplayMode();
			reorderLayers();
		}
	}

	/**
	 * Sets Directions Result mode on or off.
	 * When on is asked, sets Result mode if necessary off and draw
	 * Results Layer. Then sets Directions Results mode to on.
	 * When off is asked, removes Directions Results layer and sets
	 * Directions Result mode to off.
	 * @param value on or off
	 */
	private void setDirectionsResultMode(boolean value) {
		if (mDirectionsResultMode == value) {
			PLog.d(TAG, "Trying to set Directions Result mode to its current value :", value);
		}
		else {
			if (value) {
				if (mResultsMode) {
					setResultsMode(false);
				}
				if (mWikipediaLayerOn) {
					setWikipediaMode(false);
				}
				if (mPhotosLayerOn) {
					setPhotosMode(false);
				}
				//mDisplayInterface.panToBounds(mDirectionsResultLayerInterface.getBounds());
//				mResultsDisplayMode = RESULT_MODE.MAP;
				mDirectionsResultMode = true;
			}
			else {
				mDisplayAbstract.removeInfoWindow(mCurrentMarker, mDirectionsResultLayerInterface);
				mDisplayAbstract.removeDirectionsLayer();
				mDirectionsResultLayerInterface.clearLayer();
				mResultsLayerInterface.clearLayer();
				mItDirectionsStep = null;
				mItResult = null;
				mDisplayAbstract.setPrevNextArrowsState(false, false, false);
				mDirectionsResultMode = false;
			}
			updateResultsListDisplayMode();
			reorderLayers();
		}
		
	}
	
	/**
	 * Update displayed lists (in hybrid mode).
	 */
	ResultsListAdapter mResultsListAdapter = null;
	private void updateResultsListDisplayMode() {
		if( RESULT_MODE.MAP == mResultsDisplayMode ) {
			mDisplayAbstract.setAndDisplayResultsorInstructionsList(View.GONE, null);
		}
		else if( mResultsMode ) {
			mResultsListAdapter = new ResultsListAdapter( mDisplayAbstract,
                                                           mDisplayAbstract.getLastKnownLocation() );
			mDisplayAbstract.setAndDisplayResultsorInstructionsList(View.VISIBLE, mResultsListAdapter);
		}
		else if( mDirectionsResultMode ) {
			ListAdapter listAdapter = new DirectionsListAdapter(mDisplayAbstract, mDirectionsResultLayerInterface.getMarkersList());
			mDisplayAbstract.setAndDisplayResultsorInstructionsList(View.VISIBLE, listAdapter);
		}
		else {
			mDisplayAbstract.setAndDisplayResultsorInstructionsList(View.GONE, null);
		}
	}
	
	/**
	 * Set the results display mode: map or hybrid.
	 * @param mode The mode.
	 */
	private void setResultsDisplayMode(RESULT_MODE mode) {
		mResultsDisplayMode = mode;
		Settings.setResultsDisplayModeSetting(mDisplayAbstract, mode);
		updateResultsListDisplayMode();
	}

	/**
	 * Method called when the current position changes.
	 * Updates the results display, especially the distance
	 * to POI information.
	 * @param position New position. 
	 */
	private void updateResultsListDisplay(LatLng position) {
		if( null != mResultsListAdapter ) {
			mResultsListAdapter.udpateLocation(position);
			mDisplayAbstract.updateResultsListDisplay();
		}
	}

	/**
	 * Sets Wikipedia mode on or off.
	 * @param value on or off
	 */
	private void setWikipediaMode(boolean value) {
		PLog.e(TAG,"setWikipediaMode - "+value);
		if (mWikipediaLayerOn == value) {
			PLog.e(TAG, "Error, trying to set Wikipedia mode to its current value :", value);
		}
		else {
			if (value) {
				mWikipediaLayerInterface.update(mDisplayAbstract.getLatLngBounds());
				mDisplayAbstract.drawLayer((Layer)mWikipediaLayerInterface);
				mWikipediaLayerOn = true;
			}
			else {
				mDisplayAbstract.removeLayer((Layer)mWikipediaLayerInterface);
				mWikipediaLayerOn = false;
			}
			reorderLayers();
		}
		
	}
	
	/**
	 * Sets Photos mode on or off.
	 * @param value on or off
	 */
	private void setPhotosMode(boolean value) {
		PLog.e(TAG,"setPhotosMode - "+value);
		if (mPhotosLayerOn == value) {
			PLog.e(TAG, "Error, trying to set Photo mode to its current value :", value);
		}
		else {
			if (value) {
				mPhotosLayerInterface.update(mDisplayAbstract.getLatLngBounds());
				mDisplayAbstract.drawLayer((Layer)mPhotosLayerInterface);
				mPhotosLayerOn = true;			
			}
			else {
				mDisplayAbstract.removeLayer((Layer)mPhotosLayerInterface);
				mPhotosLayerOn = false;
			}
			reorderLayers();
		}
		
	}

	private void setTrafficMode(boolean trafficLayerOn) {
//		if( trafficLayerOn !=  mTrafficLayerOn ) {
			Settings.setTrafficLayerSetting(mDisplayAbstract,trafficLayerOn);
			mTrafficLayerOn = trafficLayerOn;
			if (!mTrafficLayerOn) {
				if (mDisplayAbstract.getAPILayersList().contains(API_LAYER.TRAFFIC)) {
					/* Case built-in traffic layer */
					mDisplayAbstract.removeAPILayer(API_LAYER.TRAFFIC);
				}
				else {
					/* Case external traffic layer */
				}
			}
			else {
				if (mDisplayAbstract.getAPILayersList().contains(API_LAYER.TRAFFIC)) {
					/* Case built-in traffic layer */
					mDisplayAbstract.drawAPILayer(API_LAYER.TRAFFIC);
				}
				else {
					/* Case external traffic layer */
				}
			}
//		}
	}

	/**
	 * Launch the activity that displays markers in a list.
	 */
	public void startDirectionsListActivity(long currentMarkerId) {
		Intent intent;
		intent = new Intent();
		intent.setClass(mDisplayAbstract, DirectionsListActivity.class);
		intent.putExtra(DirectionsListActivity.INTENT_EXTRA_SUMMARY, mDirectionsResultLayerInterface.getRouteSummary());
		intent.putExtra(DirectionsListActivity.INTENT_EXTRA_MARKERS, mDirectionsResultLayerInterface.getMarkersList());
		intent.putExtra(DirectionsListActivity.INTENT_EXTRA_CURRENT_STEP_ID, currentMarkerId);
		intent.putExtra(DirectionsListActivity.INTENT_EXTRA_CURRENT_DEST_POI, mDirectionsDestinationPOI);
		mDisplayAbstract.startActivityForResult(intent, REQUEST_SELECT_DIRECTIONS_STEP);
	}

	/**
	 * Performs a new local search with parameters supplied in
	 * Activity intent. All work will be done in a new Thread.
	 * A new message will be sent to Handler at the end,
	 * telling if yes or not the search was successfully done.
	 * Parameters to specify in intent :
	 * ACTION : Intent.ACTION_SEARCH
	 * In extra bundle :
	 * SearchManager.QUERY : String (user query)
	 * SearchManager.APP_DATA : Bundle {
	 *    "search_action" : int (INTENT_SEARCH_LOCAL_SEARCH),
	 *    "lat_sw" : double,
	 *    "lng_sw" : double,
	 *    "lat_ne" : double,
	 *    "lng_ne" : double
	 * }
	 */
	void startSearch() {
		final Intent intent = mDisplayAbstract.getIntent();
		final int intent_data_field_action = intent.getBundleExtra(SearchManager.APP_DATA).getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1);
		if (intent.getAction().equals(Intent.ACTION_SEARCH)
				&& ((intent_data_field_action == INTENT_SEARCH_LOCAL_SEARCH)
						|| (intent_data_field_action == INTENT_SEARCH_LOCAL_SEARCH_BY_CATEGORY)) ) {
			mSearchQueryCanceled = false;
			// Show progress dialog
			mDisplayAbstract.showDialog(DisplayAbstract.LOCAL_SEARCH_PROG_DIALOG_KEY);

			mCurrentTask = new ControllerTask();
			mCurrentTask.execute(ControllerTask.NEW_SEARCH_TASK);
			
		}
	}
	
	/**
	 * Performs a local search to find next results for the current local search.
	 * All work will be done in a new Thread.
	 * A new message will be sent to Handler at the end,
	 * telling if yes or not the search was successfully done.
	 */
	public void nextLocalSearch()
	{
		int currentPage = ((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).getCurrentPage();
		if (GoogleAjaxLocalSearchHistoric.getInstance().getHistoricPageStates().hasPageNext(currentPage))
		{
			Intent newIntent = new Intent();
	    	newIntent.setAction(INTENT_ACTION_NEXT_SEARCH);
	    	newIntent.setClass(mDisplayAbstract, MapViewDisplay.class);
	    	newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    	mDisplayAbstract.startActivity(newIntent);
		}
	
	}
	
	/**
	 * Performs a local search to find previous results for the current local search.
	 * All work will be done in a new Thread.
	 * A new message will be sent to Handler at the end,
	 * telling if yes or not the search was successfully done.
	 * 
	 * In a normal case, the search is not made online 
	 * because the results are already in the historic.
	 */
	public void previousLocalSearch()
	{
		int currentPage = ((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).getCurrentPage();
		if (GoogleAjaxLocalSearchHistoric.getInstance().getHistoricPageStates().hasPagePrevious(currentPage))
		{
			mGoToLastMarker = true;
			Intent newIntent = new Intent();
	    	newIntent.setAction(INTENT_ACTION_PREVIOUS_SEARCH);
	    	newIntent.setClass(mDisplayAbstract, MapViewDisplay.class);
	    	newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    	mDisplayAbstract.startActivity(newIntent);
		}
	
	}
	
	
	/**
	 * Search for next results
	 */
	public void nextSearchAction()
	{
		mSearchQueryCanceled = false;
		// Show progress dialog
		mDisplayAbstract.showDialog(DisplayAbstract.LOCAL_SEARCH_PROG_DIALOG_KEY);
		mCurrentTask = new ControllerTask();
		mCurrentTask.execute(ControllerTask.NEXT_SEARCH_TASK);
	}
	
	/**
	 * Search for previous results
	 */
	public void previousSearchAction()
	{
		mSearchQueryCanceled = false;
		// Show progress dialog
		mDisplayAbstract.showDialog(DisplayAbstract.LOCAL_SEARCH_PROG_DIALOG_KEY);
		mCurrentTask = new ControllerTask();
		mCurrentTask.execute(ControllerTask.PREVIOUS_SEARCH_TASK);
	}
	
	/**
	 * Performs a new local search with parameters supplied in
	 * Activity intent. All work will be done in a new Thread.
	 * A new message will be sent to Handler at the end,
	 * telling if yes or not the search was successfully done.
	 * Parameters to specify in intent :
	 * ACTION : Intent.ACTION_SEARCH
	 * In extra bundle :
	 * SearchManager.APP_DATA : Bundle {
	 *    "search_action" : int (INTENT_SEARCH_DIRECTIONS),
	 *    "start_my_location" : boolean,
	 *    "start_location" : String,
	 *    "start_my_location" : double,
	 *    "end_location" : String
	 * }
	 */
	public void startDirections () {
		final Intent intent = mDisplayAbstract.getIntent();
		final Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
		PLog.i(TAG, "Starting Directions !");

		if (intent.getAction().equals(Intent.ACTION_SEARCH) &&
				intent.getBundleExtra(SearchManager.APP_DATA).getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1) == INTENT_SEARCH_DIRECTIONS) {
			mSearchQueryCanceled = false;
			mDirectionsDestinationPOI = null;
			
			/* Displaying progress diaPLog... */
			mDisplayAbstract.showDialog(DisplayAbstract.DIRECTIONS_SEARCH_PROG_DIALOG_KEY);
			
			new Thread(new Runnable() {
				public void run() {
					boolean all_parameters_are_ok = true;
					
	                /* Getting all search parameters... */
					boolean start_my_location = appData.getBoolean("start_my_location", false);
					boolean end_my_location = appData.getBoolean("end_my_location", false);
					String start_location = appData.getString("start_location");
					String end_location   = appData.getString("end_location");
					mDirectionsDestinationPOI = appData.getParcelable("dest_poi");
					
					if( mRouteQueryCanceled ) {
						// Canceled, do nothing
					}
					/* Converting start and end in position if my location was specified */
					else if (start_my_location || end_my_location) {
						LocationManager locMng = (LocationManager) mDisplayAbstract.getSystemService(Context.LOCATION_SERVICE);
						Location loc = locMng.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						if (loc != null) {
							if (start_my_location) {
		        				start_location = loc.getLatitude() + "," + loc.getLongitude();
							}
							if (end_my_location) {
		        				end_location = loc.getLatitude() + "," + loc.getLongitude();
							}
						}
						else {
							all_parameters_are_ok = false;
							mDisplayAbstract.showToast(R.string.map_myposition_failed);
							
							//mDisplayAbstract.showToastInUiThread(R.string.map_myposition_failed);
						}
						
					}
					if( (null != mDirectionsDestinationPOI)
							&& (!end_my_location) ) {
						// The destination is a given POI
						LatLng loc = mDirectionsDestinationPOI.getLatLng();
						end_location = loc.getLat() + "," + loc.getLng();
					}
					
					if (all_parameters_are_ok) {
						PLog.i(TAG, "start_my_location : " , start_my_location);
						PLog.i(TAG, "end_my_location : "   , end_my_location);
						PLog.i(TAG, "start_location : "    , start_location);
						PLog.i(TAG, "end_location : "      , end_location);
						
		                mDisplayAbstract.drawDirectionsLayer(start_location, end_location);
					}
					else {
						mDisplayAbstract.dismissDialog(DisplayAbstract.DIRECTIONS_SEARCH_PROG_DIALOG_KEY);
					}
				}
			}).start();
		}
	}

	/**
	 * Compare the time stamp of 2 intents.
	 * @param a First intent
	 * @param b Second intent
	 * @return true if the time stamp of the intents is
	 * the same or if both itents are null, false otherwise.
	 */
	private boolean sameIntent( Intent a, Intent b ) {
		boolean same = false;
		if( (null == a) && (null == b) ) {
			same = true;
		}
		else if( (null == a) && (null != b) ) {
			same = false;
		}
		else if( (null != a) && (null == b) ) {
			same = false;
		}
		else if( !a.getAction().equals(b.getAction()) ) {
			same = false;
		}
		else {
			Bundle dataA = a.getBundleExtra(SearchManager.APP_DATA);
			Bundle dataB = b.getBundleExtra(SearchManager.APP_DATA);
			if( (null == dataA)
					|| (null == dataB) ) {
				same = false;
			}
			else if( dataA.getLong(INTENT_SEARCH_DATA_FIELD_TIME_STAMP, -1)
					!= dataB.getLong(INTENT_SEARCH_DATA_FIELD_TIME_STAMP, -1) ) {
				same = false;
			}
			else {
				same = true;
			}
		}
		return same;
	}
	
	/**
	 * Called to manage new intent.
	 * Checks if this is really a new intent (a new search or a new direction calculation)
	 * and calls manageIntent(). 
	 * @param newIntent
	 */
	void manageNewIntent( Intent newIntent ) {
		if( !sameIntent(newIntent,mDisplayAbstract.getIntent()) ) {
			mDisplayAbstract.setIntent(newIntent);
			manageIntent();
		}
		else {
			mDisplayAbstract.setIntent(newIntent);
		}
	}
	
	void manageIntent( ) {
		/* Checking origin of intent : launching Parrot Maps or sending search results ? */
		Intent intent = mDisplayAbstract.getIntent();
		if( (null == intent)
				|| (null == intent.getAction()) ) {
			return;
		}
		String queryString1 = null;
		String queryString2 = null;
		
		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
			
			// Make sure that we will not jump to the current GPS location
			// when the first fix will be received
			mLocationFirstFixReceived = true;
			
			
			if (mDirectionsResultMode) {
				setDirectionsResultMode(false);
			}
			PLog.d(TAG, "search_action = ", appData.getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1));
			if (appData.getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1) == INTENT_SEARCH_LOCAL_SEARCH) {
				startSearch();
				queryString1 = intent.getStringExtra(SearchManager.QUERY);
			}
			else if (appData.getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1) == INTENT_SEARCH_LOCAL_SEARCH_BY_CATEGORY) {
				startSearch();
			}
			else if (appData.getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1) == INTENT_SEARCH_DIRECTIONS) {
				if (mResultsMode) {
					setResultsMode(false);
				}
				startDirections();
				queryString1 = intent.getBundleExtra(SearchManager.APP_DATA).getString("start_location");
				queryString2 = intent.getBundleExtra(SearchManager.APP_DATA).getString("end_location");
			}
			
			/* Record the query string in the recent queries suggestions provider. */
			try {
		        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(mDisplayAbstract,
		        		SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
				if (queryString1 != null && queryString1 != "") {
					suggestions.saveRecentQuery(queryString1, null);
				}
				if (queryString2 != null && queryString2 != "") {
					suggestions.saveRecentQuery(queryString2, null);
				}
			} catch( Exception e ) {
				PLog.e(TAG,"Failed to save query in suggestions provider - Exception e ",e);
			}
		}
		// Clean map intent
		else if( intent.getAction().equals(INTENT_ACTION_CLEAN) ) {
			setResultsMode(false);
			setDirectionsResultMode(false);
			mCurrentMarker = null;
			updateCurrenResultIndex();
			cleanGoogleHistoric();
			// If a previous location is available, go there
			if( null != getLastKnownLocation() ) {
				goToMyLocation();
			}
		}
		// Search for next results
		else if( intent.getAction().equals(INTENT_ACTION_NEXT_SEARCH) ) {
			setResultsMode(true);
			setDirectionsResultMode(false);
			nextSearchAction();
		}
		// Search for previous results
		else if( intent.getAction().equals(INTENT_ACTION_PREVIOUS_SEARCH) ) {
			setResultsMode(true);
			setDirectionsResultMode(false);
			previousSearchAction();
		}
	}

	private ArrayList<Marker> mTappedMarkers = null;
	private int mTappedLayersReported = 0;
	public void overlayReportItemTap( Layer layer,
                                      ArrayList<Marker> markers) {
		PLog.i(TAG,"overlayReportItemTap nbItems ",markers.size());

		int nbActivatedLayers = 0;
		if( mDirectionsResultMode ) {
			nbActivatedLayers++;
		}
		if( mResultsMode ) {
			nbActivatedLayers++;
		}
		if( mWikipediaLayerOn ) {
			nbActivatedLayers++;
		}
		if( mPhotosLayerOn ) {
			nbActivatedLayers++;
		}
		PLog.i(TAG,"overlayReportItemTap - mTappedLayersReported ",mTappedLayersReported);
		PLog.i(TAG,"overlayReportItemTap - nbActivatedLayers ",nbActivatedLayers);
		if( mTappedLayersReported >= nbActivatedLayers ) {
			mTappedLayersReported = 0;
			mTappedMarkers = null;
		}
		mTappedLayersReported++;
		
		if( null == mTappedMarkers ) {
			mTappedMarkers = new ArrayList<Marker>();
		}
		mTappedMarkers.addAll(markers);
		if( nbActivatedLayers == mTappedLayersReported ) {
			Message msg = new Message();
			msg.what = MSG_USER_TAP_MARKERS;
			getHandler().sendMessage(msg);
		}
	}
	
	private void manageTappedMarkers( ArrayList<Marker> tappedMarkers ) {
		PLog.d(TAG, "manageTappedMarkers");
		boolean done = false;
		// Search if the current result or the current direction marker has been tapped
		if( null != mCurrentMarker ) {
			for( int i=0; (i<tappedMarkers.size()) && (!done); i++ ) {
				Marker marker = tappedMarkers.get(i);
				if(marker.getId() == mCurrentMarker.getId()) {
					done = true;
					marker.processAction(mDisplayAbstract,this);
				}
			}
		}
		// Search if a result or a direction marker has been tapped
		for( int i=0; (i<tappedMarkers.size()) && (!done); i++ ) {
			Marker marker = tappedMarkers.get(i);
			if( marker.getType() == Marker.TYPE.RESULT ) {
				done = true;
				goToResult(marker);
			}
			else if( (marker.getType() == Marker.TYPE.DIRECTIONS_START)
					|| (marker.getType() == Marker.TYPE.DIRECTIONS_STEP)
					|| (marker.getType() == Marker.TYPE.DIRECTIONS_END) ) {
				done = true;
				goToStep(marker,true);
			}
		}
		// If not done
		// Check if there is just a single marker
		if( (!done)
				&& (tappedMarkers.size() == 1) ) {
			Marker marker = tappedMarkers.get(0);
			done = true;
			marker.processAction(mDisplayAbstract,this);
		}
		// If not done
		// It means we have more than one tapped marker
		// so launch the activity to select a marker
		if( (!done)
				&& (tappedMarkers.size() > 1) ) {
			Intent intent = new Intent(mDisplayAbstract,ItemSelectActivity.class);
			intent.putParcelableArrayListExtra(ItemSelectActivity.COM_PARROT_PARROTMAP_ITEMSELECT_MARKERS, tappedMarkers);
			mDisplayAbstract.startActivity(intent);
		}
	}

	/**
	 * Initialize route and local search cancel listeners.
	 */
	private void initOnCancelListeners() {

		mRouteQueryCancelListener = new OnCancelListener() {
			/**
			 * Called when the "wait during directions calculation" dialog box is canceled. 
			 */
			public void onCancel(DialogInterface dialog) {
				mRouteQueryCanceled = true;
				// Interrupt route calculation query
		        mDisplayAbstract.interruptRouteQuery();
			}
		};

		mSearchQueryCancelListener = new OnCancelListener() {
			/**
			 * Called when the "searching" dialog box is canceled.
			 */
			public void onCancel(DialogInterface dialog) {
				if (mCurrentTask != null)
				{
					if (mCurrentTask.getStatus().equals(AsyncTask.Status.RUNNING))
					{
						mCurrentTask.cancel(true);
					}
				}
				mSearchQueryCanceled = true;
				// Interrupt query for category keywords 
				QuickSearchCategories.interruptQuery();
				// Interrupt search query
				mResultsLayerInterface.interruptSearchQuery();
			}
		};
	}
	
	/**
	 * Indicate if the map currently displays results or directions markers.
	 * @return true if results or directions markers are displayed on the current map.
	 */
	public boolean hasResultsOrDirectionsMarkersDisplayed() {
		boolean markersDisplayed = false;
		// Check if in results or directions mode
		// if yes, check if there is at least one marker
		if( mDirectionsResultMode ) {
			List<Marker> list = mDirectionsResultLayerInterface.getMarkersList();
			if( (null != list)
					&& (list.size() > 0 ) ) {
				markersDisplayed = true;
			}
		}
		if( mResultsMode ) {
			List<Marker> list = mResultsLayerInterface.getMarkersList();
			if( (null != list)
					&& (list.size() > 0 ) ) {
				markersDisplayed = true;
			}
		}
		return markersDisplayed;
	}
	
	/**
	 * Indicate if the map currently displays results markers.
	 * @return true if results markers are displayed on the current map.
	 */
	public boolean hasResultsMarkersDisplayed() {
		boolean markersDisplayed = false;
		// Check if in results mode
		// if yes, check if there is at least one marker
		if( mResultsMode ) {
			List<Marker> list = mResultsLayerInterface.getMarkersList();
			if( (null != list)
					&& (list.size() > 0 ) ) {
				markersDisplayed = true;
			}
		}
		return markersDisplayed;
	}
	
	/**
	 * Called when the user press the "Clean map" button.
	 * Its starts the MapViewDisplay activity with the action "ACTION_CLEAN".
	 */
	private void cleanMap() {
    	Intent newIntent = new Intent();
    	newIntent.setAction(INTENT_ACTION_CLEAN);
    	newIntent.setClass(mDisplayAbstract, MapViewDisplay.class);
    	newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	mDisplayAbstract.startActivity(newIntent);
    	if( mDirectionsResultMode ) {
    		mDisplayAbstract.finish();
    	}
	}
	
	/**
	 * Update the index of current result marker.
	 * This information is set in the GlobalState class
	 * and is shared by all activities in the application.
	 */
	private void updateCurrenResultIndex() {
		if( (null != mItResult)
				&& (null != mCurrentMarker)
				&& mResultsMode ) {
			GlobalState.mCurrentPOIIndex = -1;
			List<Marker> markers = mResultsLayerInterface.getMarkersList();
			for( int i=0; i<markers.size(); i++ ) {
				if( mCurrentMarker == markers.get(i) ) {
					GlobalState.mCurrentPOIIndex = i;
					break;
				}
			}
		}
	}
	
	
	private void cleanGoogleHistoric()
	{
		((GoogleAjaxLocalSearchLayer)mResultsLayerInterface).clean();
	}
	

	/**
	 * Called by the network status manager,
	 * to update its 'ready state'.
	 */
	public void onNetworkStatusReady(boolean ready) {
		if( ready ) {
			mHandler.sendEmptyMessage(MSG_NETWORK_STATUS_READY);
		}
	}
	
	/**
	 * Check the network availability
	 */
	public void checkNetworkAvailability() {
		if( mNetworkStatus.isReady())
		{
			Intent intent = mDisplayAbstract.getIntent();
			if( (null==intent)
					|| (null == intent.getAction()) ) {
				// Do nothing
			}
			else if( !mNetworkStatus.isNetworkAvailable() ) {
				if( ! intent.getAction().equals(Intent.ACTION_SEARCH) ) {
					// If the application has just been started by the user
					// display a message telling that the application
					// needs network
					mDisplayAbstract.showToast(R.string.err_msg_appli_no_network);
				}
				else {
					// If the application is already launched, and a new activity a started
					// (it can be for search request or a directions request)
					// display a message indicating that this feature needs network
					mDisplayAbstract.showToast(R.string.err_msg_feature_no_network);
					if( intent.getBundleExtra(SearchManager.APP_DATA).getInt(INTENT_SEARCH_DATA_FIELD_ACTION, -1) == INTENT_SEARCH_DIRECTIONS ) {
						// In the case of a directions request
						// close the activity
						mDisplayAbstract.finish();
					}
					else {
						// In the case of a POI search request,
						// reset the incoming intent, it will reset the current POIs (if any)
						mDisplayAbstract.setIntent(null);
					}
				}
			}
			else {
				if( ! intent.getAction().equals(Intent.ACTION_SEARCH) ) {
					// If the application has just been started by the user
					// display a message telling that the application
					// needs network
					checkLocationAvailability();
				}
			}
		}
	}
	
	/**
	 * Check the network availability.
	 * Called when controller is created.
	 * When application is launched, its checks the availability
	 * of a 'last known location'. If no location is known,
	 * it displays an error message. Otherwise, it center the map
	 * on the last known location.
	 */
	private void checkLocationAvailability() {
		// Check the intent to only do something at application launch
		Intent intent = mDisplayAbstract.getIntent();
		if( (null!=intent)
				&& (null!=intent.getAction())
				&& (!intent.getAction().equals(Intent.ACTION_SEARCH)) ) {
			// Launch GPS status check
			GpsChecker.checkGpsStatus(mDisplayAbstract,mHandler);
			// If a previous location is available, go there
			if( null != getLastKnownLocation() ) {
				goToMyLocation();
			}
		}
	}
	
	/**
	 * Reordrer layers.
	 */
	public void reorderLayers() {
		// Remove all layers
		if( mTrafficLayerOn ) {
			mDisplayAbstract.removeAPILayer(API_LAYER.TRAFFIC);
		}
		if( mDirectionsResultMode ) {
			mDisplayAbstract.removeLayer((Layer)mDirectionsResultLayerInterface);
		}
		if( mWikipediaLayerOn ) {
			mDisplayAbstract.removeLayer((Layer)mWikipediaLayerInterface);
		}
		if( mPhotosLayerOn ) {
			mDisplayAbstract.removeLayer((Layer)mPhotosLayerInterface);
		}
		mDisplayAbstract.removeAPILayer(API_LAYER.MY_LOCATION);
		if( mDirectionsResultMode ) {
			mDisplayAbstract.removeLayer((Layer)mDirectionsResultLayerInterface);
		}
		if( mResultsMode ) {
			mDisplayAbstract.removeLayer((Layer)mResultsLayerInterface);
		}
		
		// Add layer in the good order
		if( mTrafficLayerOn ) {
			mDisplayAbstract.drawAPILayer(API_LAYER.TRAFFIC);
		}
		if( mDirectionsResultMode ) {
			// Directions layer is added 2 times
			// the fist time it is for polyline drawing
			mDisplayAbstract.drawLayer((Layer)mDirectionsResultLayerInterface);
		}
		if( mWikipediaLayerOn ) {
			mDisplayAbstract.drawLayer((Layer)mWikipediaLayerInterface);
		}
		if( mPhotosLayerOn ) {
			mDisplayAbstract.drawLayer((Layer)mPhotosLayerInterface);
		}
		mDisplayAbstract.drawAPILayer(API_LAYER.MY_LOCATION);
		if( mDirectionsResultMode ) {
			// Directions layer is added 2 times
			// the second time it is for markers drawing
			mDisplayAbstract.drawLayer((Layer)mDirectionsResultLayerInterface);
		}
		if( mResultsMode ) {
			mDisplayAbstract.drawLayer((Layer)mResultsLayerInterface);
		}
	}
	
	private void notifyMarkerChange() {
		if( (null != mCurrentMarker)
				&& (null != mCurrentMarker.getTitle()) ) {
			mTTSManager.playTTSDelayed(mCurrentMarker.getTitle());
		}
	}
	
	
	
}
