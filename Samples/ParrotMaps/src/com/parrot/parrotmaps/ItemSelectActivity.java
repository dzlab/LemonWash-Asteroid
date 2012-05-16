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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parrot.parrotmaps.panoramio.PanoramioMarker;
import com.parrot.parrotmaps.wikipedia.WikipediaMarker;

public class ItemSelectActivity extends Activity implements OnClickListener, OnCancelListener
{
	private static final String TAG = "ItemSelectActivity";
	
	protected static final String COM_PARROT_PARROTMAP_ITEMSELECT_MARKERS = "com.parrot.parrotmaps.itemselectactivity.markers";

	private ArrayList<Parcelable> mMarkers = null;
	private AlertDialog mAlertDialog = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No title mode
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
        					  WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Get intent data
		Intent intent = getIntent();
		if( null != intent )
		{
			mMarkers = intent.getParcelableArrayListExtra(COM_PARROT_PARROTMAP_ITEMSELECT_MARKERS);
		}
		if( null == mMarkers )
		{
			mMarkers = new ArrayList<Parcelable>();
		}
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getText(R.string.item_select_prompt));
        ItemSelectAdapter adapter = new ItemSelectAdapter(this);
        builder.setAdapter(adapter, this);
        builder.setOnCancelListener(this);
        builder.setSingleChoiceItems(adapter, 0, this);
        mAlertDialog = builder.show();
    }
	
	@Override
	protected void  onDestroy() {
		if( null != mAlertDialog ) {
			mAlertDialog.dismiss();
		}
		super.onDestroy();
	}
	
    /**
     * A sample ListAdapter that presents content from arrays of speeches and
     * text.
     * 
     */
    private class ItemSelectAdapter extends BaseAdapter {
        public ItemSelectAdapter(Context context) {
            mContext = context;
        }

        /**
         * The number of items in the list is determined by the number of speeches
         * in our array.
         * 
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount()
        {
            return mMarkers.size();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficient to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         * 
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        /**
         * Use the array index as a unique id.
         * 
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a ItemSelectView to hold each row.
         * 
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ItemSelectView isv = null;
            Marker marker = (Marker) mMarkers.get(position);
            if( marker.getType() == Marker.TYPE.WIKIPEDIA ) {
            	isv = new ItemSelectView(mContext, (WikipediaMarker) marker );
            }
            else if( marker.getType() == Marker.TYPE.PANORAMIO ) {
            	isv = new ItemSelectView(mContext, (PanoramioMarker) marker );
            }
            return isv;
        }

        /**
         * Remember our context so we can use it when constructing views.
         */
        private Context mContext;

        /**
         * We will use a ItemSelectView to display each speech. It's just a LinearLayout
         * with two text fields.
         *
         */
        private class ItemSelectView extends LinearLayout
        {
            public ItemSelectView(Context context, PanoramioMarker photo)
            {
                super(context);

                this.setOrientation(HORIZONTAL);
                mImage = new ImageView(context);
            	mImage.setImageDrawable(photo.getDrawable());
            	addView(mImage, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                
                mTitle = new TextView(context);
                mTitle.setText(photo.getPhotoTitle());
                mTitle.setTextColor(Color.BLACK);
                addView(mTitle, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            public ItemSelectView(Context context, WikipediaMarker article)
            {
                super(context);

                this.setOrientation(HORIZONTAL);
                mImage = new ImageView(context);
                mImage.setImageDrawable(article.getDrawable());
                addView(mImage, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                
                mTitle = new TextView(context);
                mTitle.setText(article.getTitle());
                mTitle.setTextColor(Color.BLACK);
                addView(mTitle, new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            private ImageView mImage;
            private TextView mTitle;
        }
    }

	public void onClick(DialogInterface arg0, int position) {
		Marker marker = (Marker) mMarkers.get(position);
		marker.processAction(this,null);
        finish();
	}

	public void onCancel(DialogInterface dialog)
	{
        finish();
	}
}
