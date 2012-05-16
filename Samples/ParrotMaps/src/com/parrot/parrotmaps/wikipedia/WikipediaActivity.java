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
package com.parrot.parrotmaps.wikipedia;

import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.parrot.parrotmaps.R;
import com.parrot.parrotmaps.log.PLog;

/**
 * @author FL
 *
 */
public class WikipediaActivity extends Activity implements OnTouchListener {

	private static final String TAG = "WikipediaActivity";
	public static final String INTENT_EXTRA_ARTICLE = "com.parrot.parrotmaps.article";
	
	private static final int MSG_DOWNLOAD_PHOTO = 0;
	private static final int MSG_PHOTO_READY = 1;

	private WikipediaMarker mArticle;
	
	private HandlerThread                 mThread = null;
	private Looper                        mLooper = null;
	private WikipediaImageDownloadHandler mDownloadHandler = null;
	private Handler                       mDisplayHandler = null;

	private Drawable mImageDrawable = null;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // No title mode
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.wp_article);
        
        // Retrieve intent
        Intent intent = getIntent();
        if( null != intent )
        {
        	WikipediaMarker article = intent.getParcelableExtra(INTENT_EXTRA_ARTICLE);
        	if( null != article )
        	{
        		mArticle = article;

        		// Init display handler
        		mDisplayHandler = new Handler() {
        			@Override
        			public void handleMessage(Message msg) {
        				switch(msg.what) {
        				case MSG_PHOTO_READY:
        					if( null != mImageDrawable ) {
	        	    			ImageView imageView = (ImageView)findViewById(R.id.articleimageview);
	        	        		imageView.setImageDrawable(mImageDrawable);
        					}
        					break;
        					
       					default:
       						break;
        				}
        			}
        		};

        		// Set article title
        		TextView articleTitle = (TextView)findViewById(R.id.articletitleview);
        		articleTitle.setText(article.getTitle());
        		
        		// Set article summary
        		TextView articleSummary = (TextView)findViewById(R.id.articleview);
        		articleSummary.setText(article.getInfoWindow().getContent());
        		
        		// Set touch article link listener
        		TextView articleLink = (TextView)findViewById(R.id.articlelink);
                articleLink.setOnTouchListener(this);
            	
        		// Start update thread
        		mThread = new HandlerThread("WikipediaDownload Thread", Process.THREAD_PRIORITY_BACKGROUND);
        	    mThread.start();
        	    mLooper = mThread.getLooper();
        	    mDownloadHandler = new WikipediaImageDownloadHandler(mLooper);
        	    mDownloadHandler.sendEmptyMessage(MSG_DOWNLOAD_PHOTO);
        	}
        }
    }

    /**
     * Called when article link is touched.
     * Starts the web browser to read the full article.
     */
	public boolean onTouch(View v, MotionEvent event)
	{
		boolean ret = false;
		PLog.i(TAG,"action ",event.getAction());
		if( (MotionEvent.ACTION_DOWN == event.getAction())
				&& (null != mArticle) )
		{
			ret = true;
		}
		else if( (MotionEvent.ACTION_UP == event.getAction())
				&& (null != mArticle) )
		{
			PLog.i(TAG,"Go to full article");
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("http://"+mArticle.getArticleURL()));
			startActivity(intent);
			ret = true;
		}
		return ret;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		finish();
		return true;
	}

	
    private final class WikipediaImageDownloadHandler extends Handler
    {
        public WikipediaImageDownloadHandler(Looper looper)
        {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	switch(msg.what) {
        	case MSG_DOWNLOAD_PHOTO:
        		try {
            		// Get article image
        			URL url = new URL(mArticle.getThumbnailURL());
        			mImageDrawable = Drawable.createFromStream(url.openStream(), "");
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
    			mDisplayHandler.sendEmptyMessage(MSG_PHOTO_READY);
        		break;
        		
        	default:
        		break;
        	}
         }
    }
}
