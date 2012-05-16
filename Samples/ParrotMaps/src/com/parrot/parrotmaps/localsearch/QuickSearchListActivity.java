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

import java.util.ArrayList;
import java.util.Collections;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.MapViewDisplay;
import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.dialog.PopupController;
import com.parrot.parrotmaps.dialog.StatusDialog;
import com.parrot.parrotmaps.log.PLog;
import com.parrot.parrotmaps.network.NetworkStatus;

public class QuickSearchListActivity extends ListActivity implements OnItemSelectedListener, OnKeyListener
{
	private final String TAG = this.getClass().getSimpleName();
	
	public static final int FREE_SEARCH_REQUEST_ID = 99;
	public static final String INTENT_SEARCH_DATA_FREE_SEARCH_QUERY = "free_search_query";
	
	private ArrayList<String> mEntries;
	
	private EditText mOtherEditText;
	
	private String mFreeRequest = "";

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.quick_search_list);

        Resources res = getResources();
    	mEntries = new ArrayList<String>();
    	mEntries.add(res.getString(R.string.searchlist_entry_fuelstations));
    	mEntries.add(res.getString(R.string.searchlist_entry_bars));
    	mEntries.add(res.getString(R.string.searchlist_entry_restaurants));
    	mEntries.add(res.getString(R.string.searchlist_entry_hotels));
    	mEntries.add(res.getString(R.string.searchlist_entry_cinemas));
    	mEntries.add(res.getString(R.string.searchlist_entry_coffee));
    	mEntries.add(res.getString(R.string.searchlist_entry_medical));
    	mEntries.add(res.getString(R.string.searchlist_entry_parking));
    	mEntries.add(res.getString(R.string.searchlist_entry_sport));
    	mEntries.add(res.getString(R.string.searchlist_entry_leisure));
    	// FL 30/03/2011: disable transports lists, until efficient keywords are found
//    	mEntries.add(res.getString(R.string.searchlist_entry_transport));
    	mEntries.add(res.getString(R.string.searchlist_entry_shopping));
    	mEntries.add(res.getString(R.string.searchlist_entry_finance));
    	mEntries.add(res.getString(R.string.searchlist_entry_community));
    	Collections.sort(mEntries);
    	View v = getLayoutInflater().inflate(R.layout.quick_search_edittext_row, null);
    	mOtherEditText = (EditText) v.findViewById(R.id.ListItemEditText);
    	mOtherEditText.setHint(res.getString(R.string.searchlist_entry_free));
    	mOtherEditText.setOnKeyListener(this);
    	mOtherEditText.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// First, check network availability
		    	NetworkStatus networkStatus = NetworkStatus.getInstance(QuickSearchListActivity.this);
				if( ! networkStatus.isNetworkAvailable() ) {
					PopupController.showDialog(QuickSearchListActivity.this, getString(R.string.err_msg_feature_no_network), StatusDialog.LONG);
				}
				else
				{
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		            imm.showSoftInput(mOtherEditText, 0);
				}

			}
		});
    	getListView().addFooterView(v);
    	setListAdapter(new ArrayAdapter<String>(this,R.layout.quick_search_row,mEntries));
    	getListView().setOnItemSelectedListener(this);
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
    
    /**
     * Called when an item is clicked.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {

    	// First, check network availability
    	NetworkStatus networkStatus = NetworkStatus.getInstance(this);
		if( ! networkStatus.isNetworkAvailable() ) {
			PopupController.showDialog(this, getString(R.string.err_msg_feature_no_network), StatusDialog.LONG);
			return;
		}
		
		
		
    	Resources res = getResources();
    	int requestId;
    	if( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_fuelstations) ) )
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_GAS_STATIONS;
    	}
    	else if( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_bars) ) )
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_BARS;
    	}
    	else if( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_restaurants) ) )
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_RESTAURANTS;
    	}
    	else if( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_hotels) ) )
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_HOTELS;
    	}
    	else if( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_cinemas) ) )
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_MOVIE_THEATERS;
    	}
    	else if( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_coffee) ) )
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_COFFEE;
    	}
    	else if( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_medical) ) )
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_MEDICAL;
    	}
    	else if ( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_sport) ))
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_SPORT;
    	}

    	else if ( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_leisure) ))
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_LEISURE;
    	}
    	else if ( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_transport) ))
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_TRANSPORT;
    	}
    	else if ( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_shopping) ))
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_SHOPPING;
    	}
    	else if ( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_finance) ))
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_FINANCE;
    	}
    	else if ( mEntries.get(position).equals(res.getString(R.string.searchlist_entry_community) ))
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_COMMUNITY;
    	}
    	else
    	{
    		requestId = QuickSearchCategories.ENTRY_CAT_PARKING;
    	}

    	Intent newIntent = new Intent();
    	newIntent.setAction(Intent.ACTION_SEARCH);
    	newIntent.setClass(this, MapViewDisplay.class);
    	newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	newIntent.putExtra(SearchManager.QUERY, "");
    	Bundle appData = new Bundle();
    	appData.putInt(Controller.INTENT_SEARCH_DATA_FIELD_ACTION, Controller.INTENT_SEARCH_LOCAL_SEARCH_BY_CATEGORY);
    	appData.putInt(Controller.INTENT_SEARCH_DATA_FIELD_CAT_ID, requestId);
		appData.putLong(Controller.INTENT_SEARCH_DATA_FIELD_TIME_STAMP, System.nanoTime());
    	newIntent.putExtra(SearchManager.APP_DATA, appData);
    	startActivity(newIntent);
    	
    	// Close the quick search categories activity
    	finish();
	    
    }


	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (arg2 == arg0.getCount() - 1)
		{
			arg0.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			mOtherEditText.requestFocus();
		}
		else
		{
			if (!arg0.isFocused())
	        {
	            // listView.setItemsCanFocus(false);

	            // Use beforeDescendants so that the EditText doesn't re-take focus
	            arg0.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
	            arg0.requestFocus();
	        }
		}
	}


	public void onNothingSelected(AdapterView<?> arg0) {
		arg0.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
	}


	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if ( (v == mOtherEditText)
				&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) )
		{
			mFreeRequest = mOtherEditText.getText().toString();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mOtherEditText.getApplicationWindowToken(), 0);
            if (!mOtherEditText.getText().toString().equals(""))
            {
            	send(mOtherEditText.getText().toString());
            }
			return true;
		}
		else if ( (v == mOtherEditText)
				&& (event.getKeyCode() == KeyEvent.KEYCODE_BACK) )
		{
			mOtherEditText.setText("");
		}
		return false;
	}
    
	public void send(String query)
	{
		// First, check network availability
    	NetworkStatus networkStatus = NetworkStatus.getInstance(this);
		if( ! networkStatus.isNetworkAvailable() ) {
			PopupController.showDialog(this, getString(R.string.err_msg_feature_no_network), StatusDialog.LONG);
			return;
		}
		Intent newIntent = new Intent();
    	newIntent.setAction(Intent.ACTION_SEARCH);
    	newIntent.setClass(this, MapViewDisplay.class);
    	newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	newIntent.putExtra(SearchManager.QUERY, "");
    	Bundle appData = new Bundle();
    	appData.putInt(Controller.INTENT_SEARCH_DATA_FIELD_ACTION, Controller.INTENT_SEARCH_LOCAL_SEARCH_BY_CATEGORY);
    	appData.putInt(Controller.INTENT_SEARCH_DATA_FIELD_CAT_ID, FREE_SEARCH_REQUEST_ID);
		appData.putString(INTENT_SEARCH_DATA_FREE_SEARCH_QUERY, query);
    	appData.putLong(Controller.INTENT_SEARCH_DATA_FIELD_TIME_STAMP, System.nanoTime());
    	newIntent.putExtra(SearchManager.APP_DATA, appData);
    	startActivity(newIntent);
    	
    	// Close the quick search categories activity
    	finish();
	}

    @Override
	public void onResume()
	{
    	super.onResume();
    	mOtherEditText.setText(mFreeRequest);
    	
	}
}