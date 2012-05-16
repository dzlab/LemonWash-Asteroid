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
import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.parrot.asteroid.tools.MediaPlayerTimeoutLock;
import com.parrot.exitbroadcast.ExitBroadcastReceiver;
import com.parrot.parrotmaps.dialog.PopupController;
import com.parrot.parrotmaps.dialog.StatusDialog;
import com.parrot.parrotmaps.dialog.WaitDialog;
import com.parrot.parrotmaps.log.PLog;

public abstract class DisplayAbstract
		extends MapActivity
		implements OnClickListener,
				OnTouchListener,
				LocationListener,
				OnItemClickListener

{
	public static enum MAP_MODE {
		MAP,
		SATELLITE,
		HYBRID,
		TERRAIN
	}
	
	public static enum API_LAYER {
		TRAFFIC,
		MY_LOCATION
	}
	
	public static enum RESULT_MODE {
		MAP,
		HYBRID
	}

	protected static final int LOCAL_SEARCH_PROG_DIALOG_KEY = 0;
	protected static final int DIRECTIONS_SEARCH_PROG_DIALOG_KEY = 1;
	
	/** Attributes */
	private static final String               TAG                   = "DisplayAbstract";
	protected            Controller           mController           = null;
	protected            ArrayList<MAP_MODE>  mMapModesList         = null;
	protected            ArrayList<API_LAYER> mAPILayersList        = null;
	protected            ImageButton          mButtonMap_mylocation = null;
	protected            ImageButton          mButtonMap_zoom_in    = null;
	protected            ImageButton          mButtonMap_zoom_out   = null;
	protected            ImageButton          mButtonMap_prev_arrow = null;
	protected            ImageButton          mButtonMap_next_arrow = null;
	protected            ListView             mPoiOrResultsList     = null;
	
	protected            LocationManager      mLocationManager      = null;
	protected            Timer                mTimer                = null;
	
	// Tool to disable/enable car kit mode
	protected            MediaPlayerTimeoutLock           mMediaPlayerLock           = null;
	protected            DisplayAbstractHandler              mHandler;

	//! Application exit event handler
	private              ExitBroadcastReceiver mExitBroadcastReceiver = null;
	
	/* ----------------------- BEGIN OF ACTIVITY METHODS ----------------------- */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Init 'application exit' event handler
		mExitBroadcastReceiver = new ExitBroadcastReceiver(this);
		HandlerThread thread = new HandlerThread("DisplayAbstract", Process.THREAD_PRIORITY_DEFAULT);
		thread.start();
		mHandler = new DisplayAbstractHandler(thread.getLooper());
	}
	
	protected void onCreateEnd() {
		mHandler.sendEmptyMessage(MSG_ONCREATE_BACKGROUND);
	}

	protected void onCreateBackground() {
		try {
			mMediaPlayerLock = new MediaPlayerTimeoutLock(this,TAG);
		} catch( final Exception e ) {
			PLog.w(TAG,"onResumeBackground - exception : "+e);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mHandler.sendEmptyMessage(MSG_ONRESUME_BACKGROUND);
	}

	protected void onResumeBackground() {
		try {
			// Indicate to controller that the activity is resumed
			mController.getHandler().sendEmptyMessage(Controller.MSG_RESUMED);
			// Disable car kit mode when the map is displayed
			mMediaPlayerLock.acquire();
		} catch( final Exception e ) {
			PLog.w(TAG,"onResumeBackground - exception : "+e);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mHandler.sendEmptyMessage(MSG_ONPAUSE_BACKGROUND);
	}

	protected void onPauseBackground() {
		try {
			// The side list is hidden, this is a workaround
			// for a bug in Google's MapView display
			// (when we return to another MapView activity of ParrotMaps
			// half of the map view is black)
			setAndDisplayResultsorInstructionsList(View.GONE, null);
			// Enable car kit mode when the map is not displayed anymore
			mMediaPlayerLock.release();
		} catch( final Exception e ) {
			PLog.w(TAG,"onResumeBackground - exception : "+e);
		}
	}

	@Override
	protected void onDestroy() {
		mHandler.getLooper().quit();
		mHandler = null;
		
		// Stop 'application exit' event handler
    	mExitBroadcastReceiver.disableReceiver();

		// If there is a dialog box, close it
		try {
			dismissDialog(LOCAL_SEARCH_PROG_DIALOG_KEY);
		} catch(final IllegalArgumentException e ) {
			// Do nothing
		}
		try {
			dismissDialog(DIRECTIONS_SEARCH_PROG_DIALOG_KEY);
		} catch(final IllegalArgumentException e ) {
			// Do nothing
		}
		super.onDestroy();
	}

	@Override
	public void onNewIntent(Intent newIntent) {
		super.onNewIntent(newIntent);
		PLog.i(TAG, "onNewIntent()");
		if( null != mController ) {
			mController.manageNewIntent(newIntent);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    
	    Iterator<MAP_MODE> it0 = mMapModesList.iterator();

	    while (it0.hasNext()) {
	    	switch (it0.next()) {
	    		case MAP:       menu.findItem(R.id.map_mode_map).setVisible(true);       break;
	    		case SATELLITE: menu.findItem(R.id.map_mode_satellite).setVisible(true); break;
	    		case HYBRID:    menu.findItem(R.id.map_mode_hybrid).setVisible(true);    break;
	    		case TERRAIN:   menu.findItem(R.id.map_mode_terrain).setVisible(true);   break;
	    	}
	    }
	    
	    Iterator<API_LAYER> it1 = mAPILayersList.iterator();
	    
	    menu.findItem(R.id.layer_traffic).setVisible(false);
	    while (it1.hasNext()) {
	    	switch (it1.next()) {
	    		case TRAFFIC: menu.findItem(R.id.layer_traffic).setVisible(true); break;
	    	}
	    }

	    return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return false;
    	}
		mController.onPrepareOptionsMenu(menu);
	    return true;
	}
	
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case LOCAL_SEARCH_PROG_DIALOG_KEY:
            {
            	WaitDialog dialog = new WaitDialog(this);
            	dialog.setTitle(R.string.searching_progress_dialog_title);
            	dialog.setText(R.string.searching_progress_dialog_msg);
            	dialog.setCancelable(true);
            	if( null != mController ) {
            		dialog.setOnCancelListener(mController.mSearchQueryCancelListener);
            	}
                return dialog;
            }
            case DIRECTIONS_SEARCH_PROG_DIALOG_KEY:
            {
            	WaitDialog dialog = new WaitDialog(this);
            	dialog.setTitle(R.string.searching_progress_dialog_title);
            	dialog.setText(R.string.direction_progress_dialog_msg);
            	dialog.setCancelable(true);
            	if( null != mController ) {
            		dialog.setOnCancelListener(mController.mRouteQueryCancelListener);
            	}
                return dialog;
            }
        }
        return null;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return false;
    	}
		Message msg = Message.obtain();
		msg.what = Controller.MSG_MENU_OPTIONS_ITEM_SELECTED;
		msg.obj = item;
		mController.getHandler().sendMessage(msg);
	    return true;
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return false;
    	}
		boolean stopPropagation = false;
		int keyCodeChanged = keyCode;
//		PLog.v(TAG, "Key down : " + keyCode + ", " + event.toString());
		/* We catch totally all Parrot Maps buttons. */
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				stopPropagation = true;
				break;
			/* Left or right events replace media_prev and media_next events
			 * which come from emulator and must be changed. */
			case KeyEvent.KEYCODE_DPAD_LEFT:
				keyCodeChanged = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
				stopPropagation = true;
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				keyCodeChanged = KeyEvent.KEYCODE_MEDIA_NEXT;
				stopPropagation = true;
				break;
			case KeyEvent.KEYCODE_BACK:
				if( mController.hasResultsOrDirectionsMarkersDisplayed() ) {
					stopPropagation = true;
				}
				break;
			default:
				break;
		}
		if( (keyCode == KeyEvent.KEYCODE_BACK)
				&& (!mController.hasResultsMarkersDisplayed()) ) {
			finish();
		}
		else {
			Message msg = Message.obtain();
			msg.what = Controller.MSG_KEYB_ON_KEY_DOWN;
			msg.arg1 = keyCodeChanged;
			msg.obj = event;
			mController.getHandler().sendMessage(msg);
		}
		
	    return stopPropagation;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return false;
    	}
		boolean stopPropagation = false;
		Message msg = Message.obtain();
		msg.what = Controller.MSG_KEYB_ON_KEY_UP;
		msg.arg1 = keyCode;
		msg.obj = event;
		mController.getHandler().sendMessage(msg);
		// We catch media previous and next buttons
		// to these events to be transmitted to Asteroid audio management
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				// Always catch these events when the map is displayed
				stopPropagation = true;
				break;
			default:
				break;
		}
		return stopPropagation;
	}
	
	@Override
	public boolean onSearchRequested() {
		PLog.d(TAG, "onSearchRequested()");
		/* When a local search is called, this method prepares all parameters
		 * that a local search needs with the search query.
		 */
		Bundle appData = new Bundle();
		appData.putInt(Controller.INTENT_SEARCH_DATA_FIELD_ACTION, Controller.INTENT_SEARCH_LOCAL_SEARCH);
		appData.putLong(Controller.INTENT_SEARCH_DATA_FIELD_TIME_STAMP, System.nanoTime());
		startSearch(null, false, appData, false);
	    return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return;
    	}
		super.onActivityResult(requestCode, resultCode, data);
		mController.onActivityResult(requestCode, resultCode, data);
	}
	/* ----------------------- END OF ACTIVITY METHODS ----------------------- */

	/* ----------------------- BEGIN OF ONCLICKLISTENER METHODS ----------------------- */
	public void onClick(View v) {
		//PLog.v(TAG, "Click : " + v.toString());
    	// Check if controller is ready
    	if( null == mController ) {
    		return;
    	}

		if (v == mButtonMap_mylocation) {
			Location location;
			Message msg = Message.obtain();
			msg.what = Controller.MSG_MY_LOCATION_BUTTON;
			location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (location != null) {
				msg.obj = new LatLng(location.getLatitude(), location.getLongitude());
			}
			else {
				msg.obj = null;
			}
			mController.getHandler().sendMessage(msg);
		}
		else if (v == mButtonMap_zoom_in) {
			Message msg = Message.obtain();
			msg.what = Controller.MSG_ZOOM_IN;
			mController.getHandler().sendMessage(msg);
		}
		else if (v == mButtonMap_zoom_out) {
			Message msg = Message.obtain();
			msg.what = Controller.MSG_ZOOM_OUT;
			mController.getHandler().sendMessage(msg);
		}
		else if (v == mButtonMap_prev_arrow) {
			Message msg = Message.obtain();
			msg.what = Controller.MSG_USER_CLICK_BUTTON_PREV;
			mController.getHandler().sendMessage(msg);
		}
		else if (v == mButtonMap_next_arrow) {
			Message msg = Message.obtain();
			msg.what = Controller.MSG_USER_CLICK_BUTTON_NEXT;
			mController.getHandler().sendMessage(msg);
		}
	}
	/* ----------------------- END OF ONCLICKLISTENER METHODS ----------------------- */

	/* ----------------------- BEGIN OF ONTOUCHLISTENER METHODS ----------------------- */
	public boolean onTouch(View v, MotionEvent event) {
		//PLog.v(TAG, "Touch : " + v.toString() + ", " + event.toString());
		
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (mTimer != null) {
				mTimer.cancel();
				mTimer.purge();
			}
			mTimer = new Timer(true);
			
			mTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					PLog.v(TAG, "Timer done, updating position changed...");
					positionChanged();
				}
			}, 1000);
		}
		return false;
	}
	/* ----------------------- END OF ONTOUCHLISTENER METHODS ----------------------- */
	
	/* ----------------------- BEGIN OF ONITEMSELECTEDLISTENER METHODS ----------------------- */
	/*
    public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) {
		// When user selected one result or step in the Results or Directions resultlist.
		Message msg = new Message();
		msg.what = Controller.MSG_USER_SELECT_ITEM;
		msg.arg1 = pos;
		msg.obj = id;
		mController.getHandler().sendMessage(msg);
		PLog.d(TAG, "onItemSelected(position=" + pos + ", id=" + id + ")");
    }
	 */
    public void onNothingSelected(AdapterView<?> parent) {
      // Do nothing.
    }
	/* ----------------------- END OF ONITEMSELECTEDLISTENER METHODS ----------------------- */
	
	/* ----------------------- BEGIN OF ONITEMCLICKLISTENER METHODS ----------------------- */

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return;
    	}
		/* When user selected one result or step in the Results or Directions resultlist. */
		Message msg = new Message();
		msg.what = Controller.MSG_USER_SELECT_ITEM;
		msg.arg1 = position;
		msg.obj = id;
		mController.getHandler().sendMessage(msg);
		PLog.d(TAG, "onItemClick(position=", position, ", id=", id, ")");
	}
	/* ----------------------- END OF ONITEMCLICKLISTENER METHODS ----------------------- */
	
	/* ----------------------- BEGIN OF LOCATIONLISTENER METHODS ----------------------- */
	public void onLocationChanged(Location location) {
    	// Check if controller is ready
		if( null == mController ) {
			return;
		}
		final LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
		Message msg = Message.obtain();
		msg.what = Controller.MSG_NEW_GPS_DATA;
		msg.arg1 = LocationProvider.AVAILABLE;
		msg.arg2 = (int)(location.getAccuracy() * 10);   // Position accuracy in dm
		msg.obj = position;
		// Dismiss pending GPS update messages
		mController.getHandler().removeMessages(Controller.MSG_NEW_GPS_DATA);
		// Send to controller GPS position and status
		mController.getHandler().sendMessage(msg);
	}

	public void onProviderDisabled(String provider) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return;
    	}
		Message msg = Message.obtain();
		msg.what = Controller.MSG_NEW_GPS_DATA;
		msg.arg1 = LocationProvider.OUT_OF_SERVICE;
		mController.getHandler().sendMessage(msg);
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
    	// Check if controller is ready
    	if( null == mController ) {
    		return;
    	}
		boolean okToSend = true;
		Message msg = Message.obtain();
		if (provider.equals("gps") && status == LocationProvider.AVAILABLE) {
			Location location = mLocationManager.getLastKnownLocation("gps");
			if (location == null) {
				okToSend = false;
				PLog.e(TAG, "Error, GPS status available while no location available.");
			}
			else {
				msg.obj = (new LatLng(location.getLatitude(), location.getLongitude()));
			}
		}
		if (okToSend) {
			msg.what = Controller.MSG_NEW_GPS_DATA;
			msg.arg1 = status;
			mController.getHandler().sendMessage(msg);
		}
	}
	
	public LatLng getLastKnownLocation() {
		LatLng pos = null;
		if( null != mLocationManager ) {
			Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if( null != loc ) {
				pos = new LatLng(loc.getLatitude(), loc.getLongitude());
			}
		}
		return pos;
	}
	/* ----------------------- END OF LOCATIONLISTENER METHODS ----------------------- */
	
	/* ----------------------- BEGIN OF COMMON DISPLAY METHODS ----------------------- */
	/**
	 * Makes a toast (pop-up which appears and disappears quickly)
	 * appears on screen, with the given text in parameter.
	 * The given text is a string identifier.
	 * @param textID
	 */
	public void showToast(int textID) {
		final int id = textID;
		PopupController.showDialog(DisplayAbstract.this, getString(id), StatusDialog.LONG);
	}
	
	/**
	 * Makes a dialog that appears on screen.
	 * @param textID The text to display
	 * @param duration The time to display the message
	 */
	public void showToast(int textID, int duration) {
		final int id = textID;
		final int time = duration;
		new Thread(new Runnable() {
			
			public void run() {
				while (!DisplayAbstract.this.hasWindowFocus())
				{
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				PopupController.showDialog(DisplayAbstract.this, getString(id), time);
			}
		}).start();
	}

	/**
	 * Makes a toast (pop-up which appears and disappears quickly)
	 * appears on screen, with the given text in parameter.
	 * The given text is a string identifier.
	 * The toast will be created and shown in the Ui thread.
	 * @param textID
	 */
	public void showToastInUiThread(final int textID) {
		runOnUiThread(new Runnable() {
			public void run() {
				Context context = getApplicationContext();
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, textID, duration);
				toast.show();
			}
		});
	}

	/**
	 * Gets the list of MAP_MODE. This list specifies which
	 * MAP_MODE can be applied on the map.
	 * @return ArrayList<MAPMODE>
	 */
	public ArrayList<MAP_MODE> getMapModesList() {
		return mMapModesList;
	}

	/**
	 * Gets the list of API_LAYER. This list specifies which
	 * layer can be displayed by built-in API functions.
	 * The built-in layer isn't managed by Parrot Maps.
	 * @return ArrayList<API_LAYER>
	 * @see drawAPILayer()
	 * @see removeAPILayer()
	 */
	public ArrayList<API_LAYER> getAPILayersList() {
		return mAPILayersList;
	}
	
	/**
	 * Returns language code, required by search services.
	 * @return
	 */
	public String getLanguage() {
		return getResources().getConfiguration().locale.getLanguage();
	}
	
	/**
	 * Draws or hides back and next arrows.
	 * @param visible Sets if both buttons are visible or not
	 * @param prevArrowEnabled Sets if previous button is enabled or not
	 * @param nextArrowEnabled Sets if next button is enabled or not
	 */
	public void setPrevNextArrowsState(final boolean visible, final boolean prevArrowEnabled, final boolean nextArrowEnabled) {
		if (DeviceType.isAdvancedDevice(this)) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (visible) {
						mButtonMap_prev_arrow.setVisibility(View.VISIBLE);
						mButtonMap_next_arrow.setVisibility(View.VISIBLE);
					}
					else {
						mButtonMap_prev_arrow.setVisibility(View.INVISIBLE);
						mButtonMap_next_arrow.setVisibility(View.INVISIBLE);
					}
					mButtonMap_prev_arrow.setEnabled(prevArrowEnabled);
					mButtonMap_next_arrow.setEnabled(nextArrowEnabled);
				}
			});
		}
	}
	
	/**
	 * Sets visibility and content of list view.
	 * @param visibility integer : ListView.VISIBLE, ListView.INVISIBLE or ListView.GONE
	 * @param adapter Content adapter
	 */
	public void setAndDisplayResultsorInstructionsList(final int visibility, final ListAdapter adapter) {
		runOnUiThread(new Runnable() {
			public void run() {
				mPoiOrResultsList.setVisibility(visibility);
				
				if (adapter != null) {
					mPoiOrResultsList.setAdapter(adapter);
				}
			}
		});
	}

	/**
	 * Trigger a refresh of the results list display.
	 * Called when the current location changes.
	 */
	public void updateResultsListDisplay() {
		runOnUiThread(new Runnable() {
			public void run() {
				mPoiOrResultsList.invalidateViews();
			}
		});
	}
		
	/**
	 * Set the selection of results or directions list.
	 */
	public void setPoiOrResultsListSelection(final int position) {
		runOnUiThread(new Runnable() {
			public void run() {
				if( null != mPoiOrResultsList ) {
					if( (position < mPoiOrResultsList.getFirstVisiblePosition())
							|| (position > mPoiOrResultsList.getLastVisiblePosition()) ) {
						mPoiOrResultsList.setSelection(position);
					}
				}
			}
		});
	}

	
	/**
	 * Called when map center or zoom changed, to inform others components.
	 */
	protected void positionChanged () {
		/* Controler can make DisplayAbstract call this method while it is not
		 * entirely created (new search Intent which start search and use then
		 * this method).
		 */
		if (mController != null) {
			Message msg = Message.obtain();
			msg.what = Controller.MSG_POSITION_CHANGED;
			msg.obj = null;
			mController.getHandler().sendMessage(msg);
		}
	}
	/* ----------------------- END OF COMMON DISPLAY METHODS ----------------------- */
	
	/* ----------------------- BEGIN OF ABSTRACT NON COMMON DISPLAY METHODS ----------------------- */
	/**
	 * Specifies in which mode the map will be displayed.
	 * The parameter mode must be in the map's MapModesList. 
	 * @param mode
	 */
	abstract public void setMapMode(MAP_MODE mode);

	/**
	 * Sets the given parameter latlng as the center of the map.
	 * @param latlng
	 */
	abstract public void setCenter(LatLng latlng);

	/**
	 * Gets the current center of the map.
	 * @return LatLng
	 */
	abstract public LatLng getCenter();
	
	/**
	 * Makes the map exactly fits in bounds given in parameters.
	 * @param bounds
	 */
	abstract public void panToBounds(LatLngBounds bounds);
	
	/**
	 * Gets the current bounds of the map (a latlng rectangle containing what
	 * is currently visible).
	 * @return LatLngBounds
	 */
	abstract public LatLngBounds getLatLngBounds();
	
	/**
	 * Sets the zoom level, an integer thats specify the zoom level.
	 * @param zoom
	 */
	abstract public void setZoomLevel(int zoom);

	abstract public void setZoomToSpan(LatLngBounds bounds);
	
	/**
	 * Increases the zoom level, if possible.
	 * @param zoom
	 */
	abstract public void zoomIn();
	
	/**
	 * Decreases the zoom level, if possible.
	 * @param zoom
	 */
	abstract public void zoomOut();
	
	/**
	 * Gets the current zoom level.
	 * @return int
	 */
	abstract public int getZoomLevel();
	
	/**
	 * This method allows a LayerInterface to be drawn on screen.
	 * It gets the markers and polylines in the layer
	 * to make then appear on screen.
	 * @param layer
	 */
	abstract public void drawLayer(Layer layer);
	
	/**
	 * This method gets all markers and polylines of the layer
	 * to make then disappear on screen.
	 * @param layer
	 */
	abstract public void removeLayer(Layer layer);
	
	/**
	 * Tries to get directions and display them on screen.
	 * Directions will start from start parameter and end from
	 * end Parameter.
	 * Warning : the result of the directions route process
	 * must be sent by message to Controller Handler.
	 * This message will permit to the controller Controller
	 * to keep a DirectionsResultLayerInterface instance of
	 * the displayed directions.
	 * This is necessary to keep directions informations such
	 * as instructions for example.
	 * The message to send to Controller class is
	 * Controller.RESPONSE_DIRECTIONS_ROUTE attribute.
	 * @param start
	 * @param end
	 * @see removeDirectionsLayer
	 */
	abstract public void drawDirectionsLayer(String start, String end);

	/**
	 * Interrupt the current query for directions.
	 */
	abstract public void interruptRouteQuery();
	
	/**
	 * Removes the displayed directions layer.
	 * @see drawDirectionsLayer
	 */
	abstract public void removeDirectionsLayer();

	/**
	 * This method permits to update a DynamicLayer.
	 * @param layer
	 */
	abstract public void updateLayer(Layer layer);
	
	/**
	 * This method allows to display a built-in layer on the map.
	 * The API_LAYER parameter must be present in the API_LAYER
	 * list.
	 * @see getAPILayersList()
	 * @see removeAPILayer()
	 * @param layer
	 */
	abstract public void drawAPILayer(API_LAYER layer);
	
	/**
	 * This method removes an API_LAYER.
	 * @see getAPILayersList()
	 * @see drawAPILayer()
	 * @param layer
	 */
	abstract public void removeAPILayer(API_LAYER layer);
	
	/**
	 * Draws an info window on the screen, as specified in the
	 * InfoWindow parameter.
	 * @param winInfoWindow to draw
	 * @param layer Layer on which is located infoWindow
	 * @see removeInfoWindow()
	 */
	abstract public void drawInfoWindow(Marker marker, LayerInterface layer);
	
	/**
	 * Removes an info window given in parameters from screen.
	 * @param marker Marker which contains InfoWindow to draw
	 * @param layer Layer on which is located infoWindow
	 * @see drawInfoWindow()
	 */
	abstract public void removeInfoWindow(Marker marker, LayerInterface layer);

	/**
	 * Trigger a refresh of the map display.
	 */
	abstract public void invalidate();
	
	
	/* ----------------------- END OF ABSTRACT NON COMMON DISPLAY METHODS ----------------------- */

	private static final int MSG_ONCREATE_BACKGROUND = 0;
	private static final int MSG_ONRESUME_BACKGROUND = 1;
	private static final int MSG_ONPAUSE_BACKGROUND = 3;
    private final class DisplayAbstractHandler extends Handler
    {
        public DisplayAbstractHandler(Looper looper)
        {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
        	switch( msg.what )
        	{
        	case MSG_ONCREATE_BACKGROUND:
        		onCreateBackground();
        		break;
        	case MSG_ONRESUME_BACKGROUND:
        		onResumeBackground();
        		break;
        	case MSG_ONPAUSE_BACKGROUND:
        		onPauseBackground();
        		break;
        	default:
        		super.handleMessage(msg);
        		break;
        	}
        }
    };
}
