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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.Intents.Insert;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.parrot.exitbroadcast.ExitBroadcastReceiver;
import com.parrot.exitbroadcast.ExitBroadcaster;
import com.parrot.parrotmaps.DeviceType;
import com.parrot.parrotmaps.GlobalState;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.UnitsTools;
import com.parrot.parrotmaps.asteroid.DialingManager;
import com.parrot.parrotmaps.dialog.PopupController;
import com.parrot.parrotmaps.dialog.StatusDialog;
import com.parrot.parrotmaps.directions.DirectionsEntryActivity;
import com.parrot.parrotmaps.log.PLog;
import com.parrot.parrotmaps.network.NetworkStatus;
import com.parrot.parrotmaps.poidb.UserPOI;

/**
 * Activity displaying POI details.
 * @author FL
 *
 */
public class ResultDetailsActivity extends ListActivity implements LocationListener
{
	private static final String TAG = "ResultDetailsActivity";
	public static final String COM_PARROT_RESDETAILS_CENTER = "com.parrot.parrotmaps.reslist.center";
	public static final String COM_PARROT_RESDETAILS_MARKER = "com.parrot.parrotmaps.reslist.marker";
	
	private static final int ENTRY_GOTO = 0;
	private static final int ENTRY_CALL = 1;
	private static final int ENTRY_DIRECTIONS = 2;
	private static final int ENTRY_ADD_TO_POI_BOOK = 3;
	private static final int ENTRY_ADD_TO_CONTACTS = 4;
	private static final int ENTRY_SHOW_ON_MAP = 5;

	//! Result code indicating that the activity was closed by a press on Back button
	public static final int RESULT_CODE_NORMAL_CLOSE = 0;
	//! Result code indication that the user wants the show the POI on the map
	public static final int RESULT_CODE_SHOW_ON_MAP = 1;

	private View mHeaderView;
	private View mDirectionsEntry;
	private ResultMarker mResult;
	private LatLng mCenter = null;
	
	private ArrayList<Integer> entries;
	
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
	 * Called when the activity is first created.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Set default result code
        setResult(RESULT_CODE_NORMAL_CLOSE);
        
		// Init 'application exit' event handler
		mExitBroadcastReceiver = new ExitBroadcastReceiver(this);

        // Initialize the dialing manager
        DialingManager.initialize(this);

        setContentView(R.layout.result_details);
        
        Intent intent = getIntent();
        if( null != intent )
        {
			mCenter = intent.getParcelableExtra(COM_PARROT_RESDETAILS_CENTER);
        	mResult = intent.getParcelableExtra(COM_PARROT_RESDETAILS_MARKER);
        }
        
        // Init list entries
        entries = new ArrayList<Integer>();
        if( DeviceType.isAdvancedDevice(this) )
        {
        	// Entries on devices with touch screen
        	entries.add(ENTRY_GOTO);
    		if( (null != mResult.getPhoneNumbersList() )
    				&& (mResult.getPhoneNumbersList().size() > 0) )
    		{
    			entries.add(ENTRY_CALL);
    		}
        	entries.add(ENTRY_DIRECTIONS);
        	entries.add(ENTRY_ADD_TO_POI_BOOK);
//        	entries.add(ENTRY_ADD_TO_CONTACTS);
        	entries.add(ENTRY_SHOW_ON_MAP);
        }
        else
        {
        	// Entries on devices without touch screen
    		if( (null != mResult.getPhoneNumbersList() )
    				&& (mResult.getPhoneNumbersList().size() > 0) )
    		{
    			entries.add(ENTRY_CALL);
    		}
        	entries.add(ENTRY_DIRECTIONS);
//        	entries.add(ENTRY_ADD_TO_CONTACTS);
        	entries.add(ENTRY_SHOW_ON_MAP);
        }
        
        // Prepare POI details header
        createHeaderView();
        
        // Init list view
        getListView().addHeaderView(mHeaderView,null,false);
        setListAdapter(new ResultAdapter(this));
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		// Register to location manager
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
        	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (Exception e) {
			PLog.e(TAG, "Failed to init location manager : ", e.getMessage());
			e.printStackTrace();
        }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// Unregister to location manager
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
        	locationManager.removeUpdates(this);
        } catch (Exception e) {
			PLog.e(TAG, "Failed to disable location updates : ", e.getMessage());
			e.printStackTrace();
        }
	}

	@Override
	protected void onDestroy() {
		// Stop 'application exit' event handler
		mExitBroadcastReceiver.disableReceiver();
		// Stop dialing manager
		DialingManager.stop();
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
    	mHeaderView = LayoutInflater.from(this).inflate(R.layout.result_details_title, null);

		TextView name = (TextView) mHeaderView.findViewById(R.id.poi_details_name);
		TextView address_line1 = (TextView) mHeaderView.findViewById(R.id.poi_details_address_line1);
		TextView address_line2 = (TextView) mHeaderView.findViewById(R.id.poi_details_address_line2);
		name.setText(mResult.getTitle());
		address_line1.setText(mResult.getAddressLine1());
		address_line2.setText(mResult.getAddressLine2());
		name.setSelected(true);

		udpateDistanceDisplay(mCenter);

		mViewHandler.sendEmptyMessageDelayed(HANDLE_ANIM,DELAY_BEFORE_ANIM);
    }

    /**
	 * Called when an item of the list is clicked.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
    	int entry_id = entries.get((int)id);
    	switch( entry_id )
    	{
    	case ENTRY_GOTO:
    	{
    		// Send intent to a navigation system
    		// Not used yet
    		break;
    	}

    	case ENTRY_CALL:
    	{
    		ArrayList<String> phoneNumbers = mResult.getPhoneNumbersList();
    		if( (null != phoneNumbers)
    				&& (phoneNumbers.size() > 0) )
    		{
    			boolean callLaunched = DialingManager.dialNumber(this, mResult.getTitle(), mResult.getPhoneNumbersList().get(0));
    			if( !callLaunched )
    			{
    				PopupController.showDialog(this, getString(R.string.poi_details_cannot_dial_number), StatusDialog.SHORT);
    			}
    		}
    		break;
    	}

    	case ENTRY_DIRECTIONS:
    	{
        	// First, check network availability
        	NetworkStatus networkStatus = NetworkStatus.getInstance(this);
    		if( ! networkStatus.isNetworkAvailable() ) {
    			PopupController.showDialog(this, getString(R.string.err_msg_feature_no_network), StatusDialog.LONG);
    			return;
    		}
    		else if( (null != mResult.getAddress())
    				&& ( (mCenter!=null)
    						|| (DeviceType.isAdvancedDevice(this)) ) )
    		{
    			Intent intent = new Intent(this,DirectionsEntryActivity.class);
    			intent.putExtra(DirectionsEntryActivity.DIRECTION_ENTRY_EXTRA_DEST_POI, mResult);
    			startActivity(intent);
    		}
    		break;
    	}

    	case ENTRY_ADD_TO_POI_BOOK:
    	{
    		ContentValues values = new ContentValues();
    		values.put(UserPOI.NAME, mResult.getTitle());
    		if (mResult.getTitle() == null)
    		{
    			PLog.d(TAG,"name null");
    		}
    		values.put(UserPOI.LATITUDE, mResult.getLatLng().getLat());
    		values.put(UserPOI.LONGITUDE, mResult.getLatLng().getLng());
    		if (mResult.getCountry() != null)
    		{
    			values.put(UserPOI.COUNTRY, mResult.getCountry());
    		}
    		if (mResult.getState() != null)
    		{
    			values.put(UserPOI.STATE, mResult.getState());
    		}
    		if (mResult.getCity() != null)
    		{
    			values.put(UserPOI.CITY, mResult.getCity());
    		}
    		if (mResult.getStreet() != null)
    		{
    			values.put(UserPOI.STREET, mResult.getStreet());
    		}
//    		if (mResult.zip != null)
//    		{
//    			values.put(UserPOI.ZIP, mResult.zip);
//    		}
    		if (mResult.getAddressLinesList() != null)
    		{
    			values.put(UserPOI.POSTALADDRESS, mResult.getAddressOneLine());
    		}
    		ArrayList<String> phoneNumbers = mResult.getPhoneNumbersList();
    		if( null != phoneNumbers )
    		{
	    		if (phoneNumbers.size() > 0)
	    		{
	    			values.put(UserPOI.PHONENUMBER1, phoneNumbers.get(0));
	    		}
	    		if (phoneNumbers.size() > 1)
	    		{
	    			values.put(UserPOI.PHONENUMBER2, phoneNumbers.get(1));
	    		}
	    		if (phoneNumbers.size() > 2)
	    		{
	    			values.put(UserPOI.PHONENUMBER3, phoneNumbers.get(2));
	    		}
    		}
    		
    		/* Adding the new line in base */
    		Uri uri = getContentResolver().insert(Uri.parse("content://com.parrot.userpoiprovider/poi"), values);
            if (uri == null)
            {
                PLog.e(TAG, "Failed to insert new POI into ", getIntent().getData());
				new AlertDialog.Builder(this).setTitle(
					R.string.poi_details_add_to_poi_book)
					.setMessage(R.string.poi_details_add_to_poi_book_error)
					.setPositiveButton(android.R.string.ok, null)
					.setCancelable(false).create().show();
            }
            else
            {
            	PLog.e(TAG, "Succeded to insert new POI into ", getIntent().getData());
            	new AlertDialog.Builder(this).setTitle(
            		R.string.poi_details_add_to_poi_book)
					.setMessage(R.string.poi_details_add_to_poi_book_success)
					.setPositiveButton(android.R.string.ok, null)
					.setCancelable(false).create().show();
            }
    		break;
    	}

    	case ENTRY_ADD_TO_CONTACTS:
    	{
    		Intent intent = new Intent(Intent.ACTION_INSERT);
    		intent.setData(Uri.parse("content://contacts/people"));
    		intent.putExtra(Insert.NAME, mResult.getTitle());
    		if( (null != mResult.getPhoneNumbersList())
    				&& (mResult.getPhoneNumbersList().size() > 0) )
    		{
    			intent.putExtra(Insert.PHONE, mResult.getPhoneNumbersList().get(0));
    		}
    		intent.putExtra(Insert.POSTAL, mResult.getAddress());
    		try
    		{
    			startActivity(intent);
    		}
    		catch( ActivityNotFoundException e )
    		{
    			e.printStackTrace();
    		}
    		break;
    	}

    	case ENTRY_SHOW_ON_MAP:
    	{
            // Set default result code
    		// indicating the user wants to go to the map
            setResult(RESULT_CODE_SHOW_ON_MAP);
            // Disable current location tracking mode
            GlobalState.mCurrentLocationTracking = false;
            // Close the activity
    		finish();
    		break;
    	}

    	default:
    		// Do nothing
    		break;
    	}
    }
    
    /**
     * List view adapter. 
     */
    private class ResultAdapter extends BaseAdapter
    {
        private LayoutInflater mInflater;

        public ResultAdapter(Context context)
        {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);
        }

        /**
         * The number of items in the list is determined by the number of speeches
         * in our array.
         *
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount()
        {
            return entries.size();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position)
        {
            return position;
        }

        /**
         * Use the array index as a unique id.
         *
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position)
        {
            return position;
        }

        /**
         * Make a view to hold each row.
         *
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
        	int entry_id = entries.get(position);
    		convertView = mInflater.inflate(R.layout.result_details_row, parent, false);
    		TextView name = (TextView) convertView.findViewById(R.id.entry_title);
    		ImageView icon = (ImageView) convertView.findViewById(R.id.entry_icon);
        	switch( entry_id )
        	{
        	case ENTRY_GOTO:
        	{
        		name.setText(R.string.poi_details_goto);
        		break;
        	}

        	case ENTRY_CALL:
        	{
        		if( (null != mResult.getPhoneNumbersList() )
        				&& (mResult.getPhoneNumbersList().size() > 0) )
        		{
        			name.setText( ResultDetailsActivity.this.getResources().getString(R.string.poi_details_call)
        						  +" : "+mResult.getPhoneNumbersList().get(0));
            		icon.setImageResource(R.drawable.phone);
        		}
        		break;
        	}

        	case ENTRY_DIRECTIONS:
        	{
        		name.setText(R.string.poi_details_directions);
        		icon.setImageResource(R.drawable.direction);
        		mDirectionsEntry = convertView;
        		updateDirectionsEntryAvailability();
        		break;
        	}

        	case ENTRY_ADD_TO_POI_BOOK:
        	{
        		name.setText(R.string.poi_details_add_to_poi_book);
        		break;
        	}

        	case ENTRY_ADD_TO_CONTACTS:
        	{
        		name.setText(R.string.poi_details_add_to_contacts);
        		icon.setImageResource(R.drawable.add_contact);
        		break;
        	}

        	case ENTRY_SHOW_ON_MAP:
        	{
        		name.setText(R.string.poi_details_show_on_map);
        		icon.setImageResource(R.drawable.map_display);
        		break;
        	}

        	default:
        		convertView = null;
        		break;
        	}

        	return convertView;
        }
    }

    public void udpateDistanceDisplay(LatLng position) {
//    	PLog.v(TAG,"udpateDistanceDisplay");
    	mCenter = position;
    	
		TextView distance = (TextView) mHeaderView.findViewById(R.id.poi_details_distance);
		
		if (mCenter != null) {
			float[] distToPOI = new float[1];
			Location.distanceBetween(mCenter.getLat(), mCenter.getLng(), mResult.getLatLng().getLat(), mResult.getLatLng().getLng(), distToPOI);
			distance.setText(UnitsTools.distanceToLocalString(distToPOI[0]));
			distance.setVisibility(View.VISIBLE);
		}
		else {
			distance.setText(String.format(""));
			distance.setVisibility(View.GONE);
		}
    }
    
    public void updateDirectionsEntryAvailability() {
    	// Get directions entry
    	if( (null != mDirectionsEntry)
    			&& (DeviceType.isSimpleDevice(this)) ) {
    		mDirectionsEntry.setEnabled( mCenter != null );
    	}
    }
    
	public void onLocationChanged(Location location) {
		final LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
		udpateDistanceDisplay(position);
		updateDirectionsEntryAvailability();
	}

	public void onProviderDisabled(String provider) {
		// Do nothing
	}

	public void onProviderEnabled(String provider) {
		// Do nothing
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Do nothing
	}
}
