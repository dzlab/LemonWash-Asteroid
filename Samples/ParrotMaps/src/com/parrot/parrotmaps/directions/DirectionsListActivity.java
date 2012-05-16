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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.TextView;

import com.parrot.exitbroadcast.ExitBroadcastReceiver;
import com.parrot.exitbroadcast.ExitBroadcaster;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.localsearch.ResultMarker;
import com.parrot.parrotmaps.log.PLog;
import com.parrot.parrotmaps.tts.TTSManager;

/**
 * Display directions list.
 * @author FL
 *
 */
public class DirectionsListActivity extends ListActivity implements OnItemSelectedListener
{
	private static final String TAG = "DirectionsListActivity";

	public static final String INTENT_EXTRA_SUMMARY = "com.parrot.parrotmaps.directions.DirectionsListActivity.routesummary";
	public static final String INTENT_EXTRA_MARKERS = "com.parrot.parrotmaps.directions.DirectionsListActivity.markers";
	public static final String INTENT_EXTRA_CURRENT_STEP_ID = "com.parrot.parrotmaps.directions.DirectionsListActivity.currentStep";
	public static final String INTENT_EXTRA_CURRENT_DEST_POI = "com.parrot.parrotmaps.directions.DirectionsListActivity.destPOI";
	public static final String RETURNED_INTENT_EXTRA_STEP_ID = "com.parrot.parrotmaps.directions.DirectionsListActivity.stepid";
	
	private ArrayList<Marker> mMarkers;
	private ResultMarker mDestPOI;
	private String mRouteSummary;
	private View mHeaderView;

	//! TTS engine manager
	private TTSManager mTTSManager;

    private static final int HANDLE_ANIM = 0;
    private static final int DELAY_BEFORE_ANIM = 100;//0.1s

	//! Application exit event handler
   private ExitBroadcastReceiver mExitBroadcastReceiver = null;

    /**
     * Message handle used to trigger text animations.
     */
    private Handler mViewHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case HANDLE_ANIM:
            		TextView name = (TextView) mHeaderView.findViewById(R.id.poi_details_name);
            		TextView address_line1 = (TextView) mHeaderView.findViewById(R.id.poi_details_address_line1);
            		TextView address_line2 = (TextView) mHeaderView.findViewById(R.id.poi_details_address_line2);
            		name.setSelected(true);
            		address_line1.setSelected(true);
            		address_line2.setSelected(true);
                    break;
            }
        }
    };
	
	/**
	 * Called when the activity is created.
	 */
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		setContentView(R.layout.directions_list);

		// Init markers types
		Marker.initMarkerTypes(this);
		
		// Init 'application exit' event handler
		mExitBroadcastReceiver = new ExitBroadcastReceiver(this);
		
		// Retrieve directions data from incoming intent
		Intent intent = getIntent();
		mRouteSummary = intent.getStringExtra(INTENT_EXTRA_SUMMARY);
		mMarkers = intent.getParcelableArrayListExtra(INTENT_EXTRA_MARKERS);
		mDestPOI = intent.getParcelableExtra(INTENT_EXTRA_CURRENT_DEST_POI);
		
		if( null == mMarkers ) {
			// No incoming markers
			// should not happen
			return;
		}
		
		// Prepare list header
		createHeaderView();
		if( null != mHeaderView ) {
			getListView().addHeaderView(mHeaderView,null,false);
		}

		// Set the list adapter
		setListAdapter(new DirectionsListAdapter(this,mMarkers));

		// Initialize list selection with data from incoming intent 
		long currentStepId = intent.getLongExtra(INTENT_EXTRA_CURRENT_STEP_ID, Long.MAX_VALUE);
		if( currentStepId != Long.MAX_VALUE) {
			int position = 0;
			for( Marker marker : mMarkers ) {
				if( marker.getId() == currentStepId ) {
					if( null != mHeaderView ) {
						setSelection(position+1);
					}
					else {
						setSelection(position);
					}
					break;
				}
				position++;
			}
		}
		// Register item selection listener
		getListView().setOnItemSelectedListener(this);
		// Get TTS engine reference
		mTTSManager = TTSManager.getInstance(this);

		// Default result
		setResult(RESULT_CANCELED);
	}
	
    @Override
	protected void onDestroy() {
		// Stop 'application exit' event handler
    	mExitBroadcastReceiver.disableReceiver();
    	super.onDestroy();
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_no_map, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch( item.getItemId() ) {
		case R.id.options_menu_item_exit:
			ExitBroadcaster.broadcastExitEvent(this);
			break;
		default:
			// Do nothing
			PLog.e(TAG,"onOptionsItemSelected - Unknown selected item");
			break;
		}
	    return true;
	}

    /**
     * Create the header view, containing POI name, address and distance.
     * @return The header view.
     */
    private void createHeaderView()
    {
    	if( null == mDestPOI ) {
    		mHeaderView = null;
    	}
    	else {
        	mHeaderView = LayoutInflater.from(this).inflate(R.layout.directions_list_overview, null);
			TextView name = (TextView) mHeaderView.findViewById(R.id.poi_details_name);
			TextView address_line1 = (TextView) mHeaderView.findViewById(R.id.poi_details_address_line1);
			TextView address_line2 = (TextView) mHeaderView.findViewById(R.id.poi_details_address_line2);
			TextView summary = (TextView) mHeaderView.findViewById(R.id.poi_details_distance);
			name.setText(mDestPOI.getTitle());
			address_line1.setText(mDestPOI.getAddressLine1());
			address_line2.setText(mDestPOI.getAddressLine2());
			summary.setText(mRouteSummary);
			name.setSelected(true);
	
			mViewHandler.sendEmptyMessageDelayed(HANDLE_ANIM,DELAY_BEFORE_ANIM);
    	}
    }

	/**
	 * Called when an item of the list is clicked.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		// Close the activity and return the position of the selected placemark
    	Intent result = new Intent();
    	result.putExtra(RETURNED_INTENT_EXTRA_STEP_ID, id);
    	setResult(RESULT_OK,result);
    	finish();
	}

	/**
	 * Called when an item is selected.
	 * Launch TTS to read the item name.
	 */
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Marker marker;
		if( null != mHeaderView ) {
			marker = mMarkers.get(position-1);
		}
		else {
			marker = mMarkers.get(position);
		}
		mTTSManager.playTTSDelayed(marker.getTitle());
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// Do nothing
	}
}
