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
package com.parrot.parrotmaps.localsearch;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;

import com.parrot.exitbroadcast.ExitBroadcastReceiver;
import com.parrot.exitbroadcast.ExitBroadcaster;
import com.parrot.parrotmaps.GlobalState;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.Marker;
import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.dialog.WaitDialog;
import com.parrot.parrotmaps.log.PLog;
import com.parrot.parrotmaps.tts.TTSManager;

public class ResultsListActivity extends ListActivity implements LocationListener, OnItemSelectedListener {
	private final String TAG = getClass().getSimpleName();

	protected static final int LOCAL_SEARCH_PROG_DIALOG_KEY = 0;
	
	public static final String COM_PARROT_RESLIST_CENTER = "com.parrot.parrotmaps.localsearch.ResultsListActivity.center";
	public static final String COM_PARROT_RESLIST_LAYER = "com.parrot.parrotmaps.localsearch.ResultsListActivity.layer";
	public static final String RETURNED_INTENT_EXTRA_RESULT_ID = "com.parrot.parrotmaps.localsearch.ResultsListActivity.resultid";

	
	/** Incoming Handler messages. */
	static public final int MSG_REQUEST_CANCELED        = 1;
	static public final int MSG_SET_POSITION_TO_ZERO    = 2;
	
	private ResultsListAdapter mResultsListAdapter = null;
	private LatLng mCenter = null;
	
	private int mCurrentPage = -1;
	
	/**
	 * Use to remember the page index of the POIs showed on the map view. 
	 */
	private int mStartupPage = -1;

	//! TTS engine manager
	private TTSManager mTTSManager;
	
	//! Local search results handler
	private GoogleAjaxLocalSearchHistoric mLocalSearchHistoric;

	private OnCancelListener mSearchQueryCancelListener;
	
	private SearchTask mCurrentSearchTask = null;
	
	//! Application exit event handler
    private ExitBroadcastReceiver mExitBroadcastReceiver = null;

	
	private class SearchTask extends AsyncTask<Integer, Void, Boolean>
	{

		public static final int NEXT_SEARCH = 1;
		public static final int PREVIOUS_SEARCH = 2;
		
		
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			switch (params[0])
			{
				case NEXT_SEARCH:
					mResultsListAdapter.getNext();
					break;
					
				case PREVIOUS_SEARCH:
					mResultsListAdapter.getPrevious();
					break;
			}
			//mResultsListAdapter.getNext();
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			Message msg = new Message();
			msg.what = MSG_SET_POSITION_TO_ZERO;
			mHandler.sendMessage(msg);
	    	ResultsListActivity.this.dismissDialog(LOCAL_SEARCH_PROG_DIALOG_KEY);
	    }
		
		@Override
		protected void onCancelled ()
		{
			mLocalSearchHistoric.setCurrentPage(mCurrentPage);
		}
		
	}
	
	
	
	private Handler mHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_SET_POSITION_TO_ZERO:
					setSelection(0);
					break;
					
				case MSG_REQUEST_CANCELED:
					break;
					
				default:
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.results_list);
		
		// Init 'application exit' event handler
		mExitBroadcastReceiver = new ExitBroadcastReceiver(this);

		Intent intent = getIntent();
		if( null != intent )
		{
			mCenter = intent.getParcelableExtra(COM_PARROT_RESLIST_CENTER);
		}
		
		// Set the list adapter
		mResultsListAdapter = new ResultsListAdapter(this, mCenter);
		setListAdapter(mResultsListAdapter);
		
		// Register to location manager
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
        	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (Exception e) {
			PLog.e(TAG, "Failed to init location manager : ", e.getMessage());
			e.printStackTrace();
        }
		mStartupPage = GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage();
		// Default result
		setResult(RESULT_CANCELED);
		if (GlobalState.mCurrentPOIIndex > -1)
		{
			setSelection(GlobalState.mCurrentPOIIndex);
		}

		// Register item selection listener
		getListView().setOnItemSelectedListener(this);
		// Get TTS engine reference
		mTTSManager = TTSManager.getInstance(this);
		// Get local search results manager reference
		mLocalSearchHistoric = GoogleAjaxLocalSearchHistoric.getInstance();
		
		mSearchQueryCancelListener = new OnCancelListener() {
			
			/**
			 * Called when the "searching" dialog box is canceled.
			 */
			public void onCancel(DialogInterface dialog) {
				mCurrentSearchTask.cancel(true);
				// Interrupt query for category keywords 
				QuickSearchCategories.interruptQuery();
				// Interrupt search query
				mResultsListAdapter.cancelRequest();
				mLocalSearchHistoric.interruptSearchQuery();
			}
		};
	}
	
    @Override
	protected void onDestroy() {
		// Stop 'application exit' event handler
    	mExitBroadcastReceiver.disableReceiver();
    	super.onDestroy();
    }
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Update returned result
    	Intent result = new Intent();
    	result.putExtra(RETURNED_INTENT_EXTRA_RESULT_ID, id);
    	setResult(RESULT_OK,result);

    	// Start POI details activity
		try {
	    	Intent intent = new Intent(this,ResultDetailsActivity.class);
		    intent.putExtra(ResultDetailsActivity.COM_PARROT_RESDETAILS_CENTER, mCenter);
		    intent.putExtra(ResultDetailsActivity.COM_PARROT_RESDETAILS_MARKER, (Marker)mResultsListAdapter.getMarker(position));
		    startActivityForResult(intent, 0);
		} catch( Exception e ) {
			PLog.e(TAG,"Failed to start result details activity");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Called when the ResultsDetails activity returns. 
	 */
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if( ResultDetailsActivity.RESULT_CODE_SHOW_ON_MAP == resultCode ) {
			if (mStartupPage != GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage())
			{
				// We want to display a selected POI on the map view. 
				// But the selected POI is not showed on the map, so the map has to be updated
				GoogleAjaxLocalSearchHistoric.updateMap = true;
			}
			finish();
		}
	}
	
	public void onLocationChanged(Location location) {
		final LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
		mResultsListAdapter.udpateLocation(position);
		getListView().invalidateViews();
	}

	public void onProviderDisabled(String arg0) {
		// Do nothing
	}

	public void onProviderEnabled(String arg0) {
		// Do nothing
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// Do nothing
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_results_list, menu);
        return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		int currentpage = GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage();
		menu.findItem(R.id.options_menu_item_previous_results).setEnabled(GoogleAjaxLocalSearchHistoric.getInstance().getHistoricPageStates().hasPagePrevious(currentpage));
		menu.findItem(R.id.options_menu_item_next_results).setEnabled(GoogleAjaxLocalSearchHistoric.getInstance().getHistoricPageStates().hasPageNext(currentpage));
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    
		case R.id.options_menu_item_exit:
			ExitBroadcaster.broadcastExitEvent(this);
	    	return true;

		case R.id.options_menu_item_next_results:
	    	showDialog(LOCAL_SEARCH_PROG_DIALOG_KEY);
	    	mCurrentPage = GoogleAjaxLocalSearchHistoric.getInstance().getCurrentPage();
	    	mCurrentSearchTask = new SearchTask();
	    	mCurrentSearchTask.execute(SearchTask.NEXT_SEARCH);
	    	return true;
	        
	    case R.id.options_menu_item_previous_results:
	    	showDialog(LOCAL_SEARCH_PROG_DIALOG_KEY);
	    	new Thread(new Runnable() {
				
				public void run() {
					mResultsListAdapter.getPrevious();
					Message msg = new Message();
					msg.what = MSG_SET_POSITION_TO_ZERO;
					mHandler.sendMessage(msg);
					ResultsListActivity.this.dismissDialog(LOCAL_SEARCH_PROG_DIALOG_KEY);
				}
			}).start();
	        return true;
	        
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onPause()
	{
		int selectedItem = getListView().getSelectedItemPosition();
		if (selectedItem != ListView.INVALID_POSITION)
		{
			GlobalState.mCurrentPOIIndex = getListView().getSelectedItemPosition();
		}
		else
		{
			GlobalState.mCurrentPOIIndex = 0;
		}
		super.onPause();
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
                dialog.setOnCancelListener(mSearchQueryCancelListener);
                return dialog;
            }

        }
        return null;
    }
	
	
	/**
	 * Called when an item is selected.
	 * Launch TTS to read the item name.
	 */
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		LinkedList<Marker> markersList;
		try {
			markersList = mLocalSearchHistoric.getPage(mLocalSearchHistoric.getCurrentPage());
			Marker marker = markersList.get(position);
			mTTSManager.playTTSDelayed(marker.getTitle());
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// Do nothing
	}
}
