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
package com.parrot.parrotmaps.dialog;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.parrot.parrotmaps.R;

public class StatusDialog extends Activity{
	
	/**
	 * A short time (3 seconds) to display the popup.
	 */
	public static final int SHORT = 3000;
	
	/**
	 * A long time (5 seconds) to display the popup
	 */
	public static final int LONG = 5000;
	
	/**
	 * The intent extra key for setting the pop up display duration.
	 */
	public static final String DURATION_KEY = "duration";
	
	/**
	 * The intent extra key for setting the pop up display message.
	 */
	public static final String MESSAGE_KEY = "message";
	
	/**
	 * Indicates the pop up to dismiss
	 */
	private static final int QUIT = 1;
	
	private TextView messageview;
	
	private Handler mHandler = new Handler()
	{
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case QUIT:
                	StatusDialog.this.finish();
                    break;
                default:
                    break;
            }
        }
	};
	
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.rnb4_dialog);
    	messageview = (TextView) findViewById(R.id.text);
    	final Intent intent = getIntent();
    	final int duration = intent.getIntExtra(DURATION_KEY, SHORT);
    	final String message = intent.getStringExtra(MESSAGE_KEY);
    	if (message == null)
    	{
    		throw new RuntimeException("No message is set for the dialog.");
    	}
    	messageview.setText(message);
    	mHandler.sendEmptyMessageDelayed(QUIT, duration);
    }

}
