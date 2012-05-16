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
package com.parrot.parrotmaps.panoramio;

import java.net.URL;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.parrot.exitbroadcast.ExitBroadcastReceiver;
import com.parrot.exitbroadcast.ExitBroadcaster;
import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.dialog.PopupController;
import com.parrot.parrotmaps.dialog.StatusDialog;
import com.parrot.parrotmaps.dialog.WaitDialog;
import com.parrot.parrotmaps.log.PLog;

/**
 * Activity displaying a Panoramio picture.
 * @author FL
 */
public class PanoramioPhotoActivity extends Activity implements OnClickListener, OnCancelListener
{
	private static final String LOG_TAG = "PanoramioPhotoActivity";	
	public static final String INTENT_EXTRA_PHOTO = "com.parrot.gsearch.panoramio.photo";
    
	private static final int MSG_DOWNLOAD_PHOTO = 0;
	private static final int MSG_PHOTO_READY = 1;
	
	private static final int DOWNLOADING_DIALOG_KEY = 0;
	
	private TextView mAuthorLink; 
	private TextView mPhotoLinkPrompt;
	private ImageView mPhotoLinkLogo;
    
    private PanoramioMarker mPhoto;

	private HandlerThread            mThread = null;
	private Looper                   mLooper = null;
	private PanoramioDownloadHandler mDownloadHandler = null;
	private Handler                  mDisplayHandler = null;

	private Drawable mPhotoDrawable = null;
	
	//! Application exit event handler
    private ExitBroadcastReceiver mExitBroadcastReceiver = null;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_layout);
        
		// Init 'application exit' event handler
		mExitBroadcastReceiver = new ExitBroadcastReceiver(this);

        // Get references to components
        mAuthorLink = (TextView)findViewById(R.id.photo_author);
        mPhotoLinkPrompt = (TextView)findViewById(R.id.photo_link);
        mPhotoLinkLogo = (ImageView)findViewById(R.id.panoramio_logo);
        
        mAuthorLink.setOnClickListener(this);
        mPhotoLinkPrompt.setOnClickListener(this);
        mPhotoLinkLogo.setOnClickListener(this);
        
        // Retrieve intent
        Intent intent = getIntent();
        if( null != intent )
        {
        	mPhoto = intent.getParcelableExtra(INTENT_EXTRA_PHOTO);
        	if( null != mPhoto )
        	{
        		// Init display handler
        		mDisplayHandler = new Handler() {
        			@Override
        			public void handleMessage(Message msg) {
        				switch(msg.what) {
        				case MSG_PHOTO_READY:
        					if( null != mPhotoDrawable ) {
	        	    			ImageView imageView = (ImageView)findViewById(R.id.image);
	        	        		imageView.setImageDrawable(mPhotoDrawable);
        					}
        					else {
        						PopupController.showDialog(PanoramioPhotoActivity.this, getString(R.string.photo_downloading_failed_msg), StatusDialog.SHORT);
        						finish();
        					}
		        			dismissDialog(DOWNLOADING_DIALOG_KEY);
        					break;
        					
       					default:
       						break;
        				}
        			}
        		};
        		
        		// Set photo title
        		TextView photoTitle = (TextView)findViewById(R.id.photo_name);
        		photoTitle.setText(mPhoto.getTitle());
        		
        		mAuthorLink.setText(Html.fromHtml("<u>"+this.getResources().getString(R.string.author)+mPhoto.getInfoWindow().getContent()+"</u>"));
            	
        		// Start update thread
        		mThread = new HandlerThread("PanoramioDownload Thread", Process.THREAD_PRIORITY_BACKGROUND);
        	    mThread.start();
        	    mLooper = mThread.getLooper();
        	    mDownloadHandler = new PanoramioDownloadHandler(mLooper);
        	    
    			showDialog(DOWNLOADING_DIALOG_KEY);
        	    mDownloadHandler.sendEmptyMessage(MSG_DOWNLOAD_PHOTO);
        	}
        }
    }

	/**
	 * Stop the activity.
	 */
	@Override
	public void onDestroy()
	{
		// Stop 'application exit' event handler
    	mExitBroadcastReceiver.disableReceiver();
		if( null != mLooper )
		{
			mLooper.quit();
			mLooper = null;
			mThread = null;
		}
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
			PLog.e(LOG_TAG,"onOptionsItemSelected - Unknown selected item");
			break;
		}
	    return true;
	}
	
    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case DOWNLOADING_DIALOG_KEY:
            {
                WaitDialog dialog = new WaitDialog(this);
                dialog.setTitle(R.string.photo_downloading_progress_dialog_title);
                dialog.setText(R.string.photo_downloading_progress_dialog_msg);
                dialog.setCancelable(true);
                dialog.setOnCancelListener(this);
                return dialog;
            }
        }
        return null;
    }
	

	public void onClick(View v)
	{
		if( v == mAuthorLink )
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("http://www.panoramio.com/user/"+mPhoto.getOwnerId()));
			startActivity(intent);
		}
		else
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("http://www.panoramio.com/photo/"+mPhoto.getPhotoId()));
			startActivity(intent);
		}
	}
	
    private final class PanoramioDownloadHandler extends Handler
    {
        public PanoramioDownloadHandler(Looper looper)
        {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	switch(msg.what) {
        	case MSG_DOWNLOAD_PHOTO:
        		// Get photo
        		URL url = null;
        		try {
        			url = new URL(mPhoto.getFileName().replace("mini_square", "medium"));
        			mPhotoDrawable = Drawable.createFromStream(url.openStream(), "");
        		} catch( Exception e ) {
        			e.printStackTrace();
        		}
				if( null == mPhotoDrawable ) {
	        		try {
	        			PLog.w(LOG_TAG,"Failed to get medium size photo - try small one");
	        			url = new URL(mPhoto.getFileName().replace("mini_square", "small"));
	        			mPhotoDrawable = Drawable.createFromStream(url.openStream(), "");
	        		} catch( Exception e ) {
	        			e.printStackTrace();
	        		}
				}
				if( null == mPhotoDrawable ) {
	        		try {
	        			PLog.w(LOG_TAG,"Failed to get small size photo - try original one");
	        			url = new URL(mPhoto.getFileName().replace("mini_square", "original"));
	        			mPhotoDrawable = Drawable.createFromStream(url.openStream(), "");
	        		} catch( Exception e ) {
	        			e.printStackTrace();
	        		}
				}
    			mDisplayHandler.sendEmptyMessage(MSG_PHOTO_READY);
        		break;
        		
        	default:
        		break;
        	}
         }
    }

    /**
     * Called when the user canceled the downloading dialog box.
     */
	public void onCancel(DialogInterface dialog) {
		finish();
	}
}
