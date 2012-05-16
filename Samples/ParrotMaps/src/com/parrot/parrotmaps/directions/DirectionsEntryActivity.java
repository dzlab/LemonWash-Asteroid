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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethodsColumns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.DeviceType;
import com.parrot.parrotmaps.LatLngBounds;
import com.parrot.parrotmaps.MapAddress;
import com.parrot.parrotmaps.MapViewDirectionsDisplay;
import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.SearchSuggestionsProvider;
import com.parrot.parrotmaps.dialog.PopupController;
import com.parrot.parrotmaps.dialog.StatusDialog;
import com.parrot.parrotmaps.dialog.WaitDialog;
import com.parrot.parrotmaps.localsearch.ResultMarker;
import com.parrot.parrotmaps.log.PLog;

/**
 * This activity is used to let the user define a start 'address' and a destination 'address'
 * and then launch the direction computing.
 * @author FL
 */
public class DirectionsEntryActivity extends Activity
									implements OnKeyListener,
											   OnClickListener,
											   android.content.DialogInterface.OnClickListener,
											   android.content.DialogInterface.OnCancelListener,
											   OnEditorActionListener
{
	private static final String TAG = "DirectionEntryActivity";
	//! Extra data of input intent, search location
	public static String DIRECTION_ENTRY_EXTRA_BOUNDS = "com.parrot.parrotmaps.directionsentry.bounds";
	//! Extra data of input intent, start address
	public static String DIRECTION_ENTRY_EXTRA_START = "com.parrot.parrotmaps.directionsentry.startadd";
	//! Extra data of input intent, destination address
	public static String DIRECTION_ENTRY_EXTRA_DEST = "com.parrot.parrotmaps.directionsentry.destadd";
	//! Extra data of input intent, destination POI
	public static String DIRECTION_ENTRY_EXTRA_DEST_POI = "com.parrot.parrotmaps.directionsentry.destpoi";
    
    private final static int MSG_PROCESS_START_ADD = 0;
    private final static int MSG_PROCESS_DEST_ADD = 1;
    private final static int MSG_GET_ROUTE = 2;
    
    //! Internal for contact address request
    private final static int PICK_CONTACT_ADDRESS_REQUEST = 0;
    
    //! Position of 'Current Location' entry in address source selection dialog box
    private final static int LOC_SOURCE_SEL_MYLOCATION = 0;
    //! Position of 'Contacts' entry in address source selection dialog box
    private final static int LOC_SOURCE_SEL_CONTACTS = 1;
    //! Number of entries in the address source selection dialog box
    private final static int LOC_SOURCE_SEL_NB_ITEMS = 2;
	
    //! Dialog id for waiting message
    private final static int DIRECTIONS_SEARCH_DIALOG_KEY = 0;
    
	//! Ok button handle
	private Button mButtonOk;
	//! Invert button handle
	private Button mButtonInvert;
	
	//! Start address button handle
	private ImageButton mStartButton;
	//! Destination address button handle
	private ImageButton mDestButton;
	
	//! Start address 'text edit' handle
	private DirectionsTextEdit mStartAddEdit;
	//! Destination address 'text edit' handle
	private DirectionsTextEdit mDestAddEdit;
	
	//! Used during selection of a location source
	private DirectionsTextEdit mCurrentAddSourceSelection;
    
	//! Location source selection dialog box handle
    private AlertDialog mLocSourceSelDialog;
	//! Address selection dialog box handle
    private AlertDialog mAddressSelDialog;
	
    private volatile Looper mSearchLooper;
    private volatile SearchQueriesHandler mSearchQueriesHandler;
    
    private LatLngBounds mBounds;
    
    //! Start address
    private String mStartAdd;
    //! Destination address
    private String mDestAdd;
    private ResultMarker mDestPOI;

    private ArrayList<MapAddress> mAdds;

	
	/**
	 * Called when the activity is first created.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Have the system blur any windows behind this one.
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
		             		  WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        // No title mode
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.direction_entry);

        // Get input intent
        Intent intent = getIntent();
        // Get the search location
        mBounds = intent.getParcelableExtra(DIRECTION_ENTRY_EXTRA_BOUNDS);
        // Get input start address
        mStartAdd = intent.getStringExtra(DIRECTION_ENTRY_EXTRA_START);
        // Get input destination address
        mDestAdd = intent.getStringExtra(DIRECTION_ENTRY_EXTRA_DEST);
        // Get input destination POI
        mDestPOI = intent.getParcelableExtra(DIRECTION_ENTRY_EXTRA_DEST_POI);
        
        if( (null==mDestAdd)
        		&& (null != mDestPOI) ) {
        	mDestAdd = mDestPOI.getAddressOneLine();
        }
        
        mButtonOk = (Button)findViewById(R.id.button_ok);
        mButtonOk.setOnClickListener(this);
        mButtonOk.setOnKeyListener(this);
        
        mButtonInvert = (Button)findViewById(R.id.button_invert);
        mButtonInvert.setOnClickListener(this);
        
        // Prepare adapter for AutoCompleteTextView's
        ContentResolver content = getContentResolver();
        String[] selectionArgs = new String[1];
        selectionArgs[0] = "";
        Cursor cursor = null;
        SuggestionsAdapter adapter = null;
        try {
    		if( !DeviceType.isSimpleDevice(DirectionsEntryActivity.this) ) {
    			cursor = content.query( SearchSuggestionsProvider.CONTENT_URI,
    									null,
    									"",
    									selectionArgs,
    									null );
    		}
        }
        catch( Exception e ) {
        	PLog.e(TAG,"Failed to query the suggestion provider - ",e);
        }
        if( null != cursor ) {
        	adapter = new SuggestionsAdapter(this, cursor);
        }
        
        mStartAddEdit = (DirectionsTextEdit)findViewById(R.id.start_add_edit);
        mStartAddEdit.setOnKeyListener(this);
        if( null != adapter ) {
        	mStartAddEdit.setAdapter(adapter);
        }
        mStartAddEdit.setOnEditorActionListener(this);
        if( null != mStartAdd)
        {
        	mStartAddEdit.setText(mStartAdd);
        }
        else
        {
        	mStartAddEdit.setIsMyLocation(true);
        }
        
        mDestAddEdit = (DirectionsTextEdit)findViewById(R.id.dest_add_edit);
        mDestAddEdit.setOnKeyListener(this);
        if( null != adapter ) {
        	mDestAddEdit.setAdapter(adapter);
        }
        mDestAddEdit.setOnEditorActionListener(this);
        if( null != mDestAdd )
        {
        	mDestAddEdit.setText(mDestAdd);
        }
        mDestAddEdit.requestFocus();
        
        mStartButton = (ImageButton)findViewById(R.id.start_add_button);
        mStartButton.setOnClickListener(this);
        
        mDestButton = (ImageButton)findViewById(R.id.dest_add_button);
        mDestButton.setOnClickListener(this);
        
        // Init thread for servers queries
        HandlerThread thread = new HandlerThread("Servers queries thread");
        thread.start();
        mSearchLooper = thread.getLooper();
        mSearchQueriesHandler = new SearchQueriesHandler(mSearchLooper);
        
		if( DeviceType.isSimpleDevice(DirectionsEntryActivity.this) )
		{
			// Immediately start the direction search on simple devices
	        onButtonOkClick();
		}
    }

    @Override
    public void onDestroy()
    {
    	super.onDestroy();
        mSearchLooper.quit();
    }
	
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case DIRECTIONS_SEARCH_DIALOG_KEY:
            {
                WaitDialog dialog = new WaitDialog(this);
                dialog.setTitle(R.string.searching_progress_dialog_title);
                dialog.setText(R.string.addresses_progress_dialog_msg);
                dialog.setCancelable(true);
                dialog.setOnCancelListener(this);
                return dialog;
            }
        }
        return null;
    }

    /**
     * Called when Ok button is pressed.
     * Stop this activity and return start and destination addresses.
     */
    private void onButtonOkClick()
    {
    	if( 0 == mStartAddEdit.getText().length() )
    	{
    		// No start address
    		PopupController.showDialog(this, getString(R.string.direction_define_start_add_msg), StatusDialog.SHORT);
			mStartAddEdit.requestFocus();
    	}
    	else if( 0 == mDestAddEdit.getText().length() )
    	{
    		// No destination address
			PopupController.showDialog(this, getString(R.string.direction_define_dest_add_msg), StatusDialog.SHORT);
			mDestAddEdit.requestFocus();
    	}
    	else
    	{
	    	mSearchQueriesHandler.sendEmptyMessage(MSG_PROCESS_START_ADD);
	    	showDialog(DIRECTIONS_SEARCH_DIALOG_KEY);
    	}
    }
    
    /**
     * Called when invert button is called.
     * Switch start address entry and destination address entry.
     */
    private void onButtonInvertClick()
    {
    	// Invert strings
    	String startAdd = mStartAddEdit.getText().toString();
    	mStartAddEdit.setText(mDestAddEdit.getText());
    	mDestAddEdit.setText(startAdd);
    	
    	// Invert "is my location" status
    	boolean tmp;
    	tmp = mStartAddEdit.getIsMyLocation();
    	mStartAddEdit.setIsMyLocation(mDestAddEdit.getIsMyLocation());
    	mDestAddEdit.setIsMyLocation(tmp);
    }

    /**
     * Called when a key is pressed on one of the text edit view.
     */
	public boolean onKey(View v, int keyCode, KeyEvent event)
	{
		boolean ret = false;
		if( KeyEvent.KEYCODE_ENTER == keyCode )
		{
			if( v == mButtonOk )
			{
				onButtonOkClick();
			}
		}
		else
		{
			if( v == mStartAddEdit )
			{
				mStartAddEdit.setIsMyLocation(false);
			}
			else if( v == mDestAddEdit )
			{
				mDestAddEdit.setIsMyLocation(false);
			}
		}
		return ret;
	}
	
	/**
	 * Called when one of the buttons is clicked
	 */
	public void onClick(View v)
	{
		if( v == mStartButton )
		{
			getAddSelection(mStartAddEdit);
		}
		else if( v == mDestButton )
		{
			getAddSelection(mDestAddEdit);
		}
		else if( v == mButtonInvert )
		{
			onButtonInvertClick();
		}
		else if( v == mButtonOk )
		{
			onButtonOkClick();
		}
	}

	/**
	 * Called when an location source is selected
	 */
	public void onClick(DialogInterface dialog, int which)
	{
		if( dialog == mLocSourceSelDialog )
		{
			switch( which )
			{
			// The user selected 'current location' has address source
			case LOC_SOURCE_SEL_MYLOCATION:
				// My location
				mCurrentAddSourceSelection.setIsMyLocation(true);
				break;

			// The user selected 'contacts' has address source
			case LOC_SOURCE_SEL_CONTACTS:
			{
				// Get an address from contacts
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("vnd.android.cursor.dir/postal-address");
				startActivityForResult( intent, PICK_CONTACT_ADDRESS_REQUEST );
				break;
			}
			
			default:
				// Do nothing
				break;
			}
		}
		else if( dialog == mAddressSelDialog )
		{
			if( SearchQueriesHandler.QUERY_START_ADD == mSearchQueriesHandler.mCurrentQuery )
			{
				mStartAdd = mAdds.get(which).address;
				mSearchQueriesHandler.sendEmptyMessage(MSG_PROCESS_DEST_ADD);
			}
			else
			{
				mDestAdd = mAdds.get(which).address;
				mSearchQueriesHandler.sendEmptyMessage(MSG_GET_ROUTE);
			}
		}
		dialog.dismiss();
		mAddressSelDialog = null;
		mLocSourceSelDialog = null;
	}

	public void onCancel(DialogInterface dialog)
	{
		PLog.i(TAG,"onCancel");
		dismissDialog(DIRECTIONS_SEARCH_DIALOG_KEY);
		mAddressSelDialog = null;
		mLocSourceSelDialog = null;
		if( DeviceType.isSimpleDevice(DirectionsEntryActivity.this) )
		{
			// Immediately finish the activity on simple devices
			finish();
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
		if( PICK_CONTACT_ADDRESS_REQUEST == requestCode )
		{
			if( RESULT_OK == resultCode )
			{
				// A contact was picked
				String contact_add = null;
				PLog.i(TAG,"Contact address returned ",data);
				if( null != data.getData() )
				{
			        String[] projection = new String[] {
			        		ContactMethodsColumns.KIND,
			        		ContactMethodsColumns.DATA,
			        };
					Cursor cur = managedQuery(data.getData(), projection, null, null, null);
					if( null != cur ) {
						cur.moveToFirst();
						do
						{
							int kind = cur.getInt(cur.getColumnIndex(ContactMethodsColumns.KIND));
							if( kind == Contacts.KIND_POSTAL )
							{
								contact_add = cur.getString(cur.getColumnIndex(ContactMethodsColumns.DATA));
								PLog.i(TAG,"Contact address ",contact_add);
							}
						}while( (cur.moveToNext()) && (null == contact_add) );
					}
				}
				if( null == contact_add )
				{
					contact_add = "";
				}
				contact_add = contact_add.replace('\n', ' ');
				if( mCurrentAddSourceSelection == mStartAddEdit )
				{
					mStartAddEdit.setIsMyLocation(false);
					mStartAdd = contact_add;
		        	mStartAddEdit.setText(mStartAdd);
				}
				else
				{
					mDestAddEdit.setIsMyLocation(false);
					mDestAdd = contact_add;
		        	mDestAddEdit.setText(mDestAdd);
				}
				PLog.i(TAG,"Contact address ",contact_add);
            }
		}
    }
	
	/**
	 * Select an address from :
	 *  - current position
	 *  - contacts list
	 * @param addEdit
	 */
	private void getAddSelection( DirectionsTextEdit addEdit )
	{
		mCurrentAddSourceSelection = addEdit;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if( mCurrentAddSourceSelection == mStartAddEdit )
		{
			builder.setTitle(R.string.direction_address_start);
		}
		else
		{
			builder.setTitle(R.string.direction_address_arrival);
		}

        String[] addsArray = new String[LOC_SOURCE_SEL_NB_ITEMS];
	    addsArray[LOC_SOURCE_SEL_MYLOCATION] = getResources().getString(R.string.direction_address_mylocation);
	    addsArray[LOC_SOURCE_SEL_CONTACTS] = getResources().getString(R.string.direction_address_contacts);
	    builder.setItems(addsArray, this);

	    mLocSourceSelDialog = builder.show();
	}
	
	
	/**
	 * Adapter for suggestions.
	 * @author FL
	 *
	 */
    public static class SuggestionsAdapter extends CursorAdapter implements Filterable
    {
        public SuggestionsAdapter(Context context, Cursor c)
        {
            super(context, c);
            mContent = context.getContentResolver();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent)
        {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final TextView view = (TextView) inflater.inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false);
            view.setText(cursor.getString(1));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor)
        {
            ((TextView) view).setText(cursor.getString(1));
        }

        @Override
        public String convertToString(Cursor cursor)
        {
            return cursor.getString(1);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint)
        {
            if (getFilterQueryProvider() != null)
            {
                return getFilterQueryProvider().runQuery(constraint);
            }
            String[] selectionArgs = new String[1];
            selectionArgs[0] = constraint.toString();
            
            return mContent.query( SearchSuggestionsProvider.CONTENT_URI,
            					   null,
            					   "",
            					   selectionArgs,            					   
            					   null );
        }

        private ContentResolver mContent;        
    }



	public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
	{
		PLog.i(TAG,"onEditorAction ");
		boolean ret = false;
		if( v == mStartAddEdit )
		{
			mDestAddEdit.requestFocus();
			ret = true;
		}
		else if( v == mDestAddEdit )
		{
			mButtonOk.requestFocus();
			ret = true;
		}
		return ret;
	}

	private void chooseAddress( )
	{
		PLog.v(TAG,"chooseAddress");
		if( null != mAdds )
		{
		    AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setTitle(getResources().getString(R.string.direction_choose_address_title));
		    String[] addsArray = new String[mAdds.size()];
		    for( int i=0; i<mAdds.size(); i++)
		    {
		    	addsArray[i] = mAdds.get(i).dispAddress;
		    }
		    builder.setItems(addsArray, this);
		    builder.setOnCancelListener(this);
		    mAddressSelDialog = builder.show();
		}
	}	    
    	
    private final class SearchQueriesHandler extends Handler
    {
    	int mCurrentQuery;
    	static final int QUERY_START_ADD = 0;
    	static final int QUERY_DEST_ADD = 1;
    	
        public SearchQueriesHandler(Looper looper)
        {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
        	switch( msg.what )
        	{
        	case MSG_PROCESS_START_ADD:
        		mCurrentQuery = QUERY_START_ADD;
        		if( mStartAddEdit.getIsMyLocation() )
        		{
        			mStartAdd = "";
        			sendEmptyMessage(MSG_PROCESS_DEST_ADD);
        		}
        		else
        		{
        			mAdds = DirectionsHelper.getAddsFromQuery( DirectionsEntryActivity.this,
        					                              mBounds,
        					                              mStartAddEdit.getText().toString() );
	        		if( (null == mAdds)
	        				|| (mAdds.size() == 0) )
	        		{
	        			mStartAdd = mStartAddEdit.getText().toString();
	        			sendEmptyMessage(MSG_PROCESS_DEST_ADD);
	        		}
	        		else if( mAdds.size() > 1 )
	        		{
	            		chooseAddress();
	        		}
	        		else
	        		{
	        			mStartAdd = mAdds.get(0).address;
	        			sendEmptyMessage(MSG_PROCESS_DEST_ADD);
	        		}
        		}
        		break;

        	case MSG_PROCESS_DEST_ADD:
        		mCurrentQuery = QUERY_DEST_ADD;
        		if( mDestAddEdit.getIsMyLocation() )
        		{
        			mDestAdd = "";
        			sendEmptyMessage(MSG_GET_ROUTE);
        		}
        		else if( mDestPOI != null )
        		{
        			// The destination is a specific POI
        			// with a given position
        			// so we don't to search for its address
        			sendEmptyMessage(MSG_GET_ROUTE);
        		}
        		else
        		{
	        		mAdds = DirectionsHelper.getAddsFromQuery( DirectionsEntryActivity.this,
	        				                              mBounds,
	        				                              mDestAddEdit.getText().toString() );
	        		if( (null == mAdds)
	        				|| (mAdds.size() == 0) )
	        		{
	        			mDestAdd = mDestAddEdit.getText().toString();
	        			sendEmptyMessage(MSG_GET_ROUTE);
	        		}
	        		else if( mAdds.size() > 1 )
	        		{
	            		chooseAddress();
	        		}
	        		else
	        		{
	        			mDestAdd = mAdds.get(0).address;
	        			sendEmptyMessage(MSG_GET_ROUTE);
	        		}
        		}
        		break;
        		
        	case MSG_GET_ROUTE:
        		/* All search parameters are stored in a bundle with
        		 * SearchManager.APP_DATA as key.
        		 */
        		Bundle appData = new Bundle();
        		appData.putInt(Controller.INTENT_SEARCH_DATA_FIELD_ACTION, Controller.INTENT_SEARCH_DIRECTIONS);
        		appData.putBoolean("start_my_location", mStartAddEdit.getIsMyLocation());
        		appData.putBoolean("end_my_location", mDestAddEdit.getIsMyLocation());
        		appData.putString("start_location", mStartAdd);
        		appData.putString("end_location", mDestAdd);
        		appData.putParcelable("dest_poi", mDestPOI);
        		appData.putLong(Controller.INTENT_SEARCH_DATA_FIELD_TIME_STAMP, System.nanoTime());
        		
        		Intent newIntent = new Intent();
            	newIntent.setAction(Intent.ACTION_SEARCH);
            	newIntent.addCategory("com.parrot.parrotmaps.DIRECTIONS");
            	newIntent.setClass(DirectionsEntryActivity.this, MapViewDirectionsDisplay.class);
            	newIntent.putExtra(SearchManager.APP_DATA, appData);
            	newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            	startActivity(newIntent);
            	
            	try {
            		dismissDialog(DIRECTIONS_SEARCH_DIALOG_KEY);
            	} catch( Exception e ) {
            		// It appears that in some cases the dialog box is not displayed at this point
            		// and trying to dismiss it triggers an exception
            		PLog.e(TAG,"An exception occured when trying to dismiss progress dialog - Exception ",e);
            	}
            	
    			if( DeviceType.isSimpleDevice(DirectionsEntryActivity.this) )
    			{
    				// Immediately finish this activity on simple devices 
    				finish();
    			}
    			break;
    			
        	default:
        		super.handleMessage(msg);
        	}
        }
    };

}
