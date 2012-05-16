package com.lemonwash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class WashConfirmationPopup extends Activity {

    /**
     * A "short" time to display the popup.<br>
     */
    public static final int SHORT = 5000;
    
    /**
     * A "long" time to display the popup.<br>
     */
    public static final int LONG = 8000;
    
    /**
     * Used this as popup display time to make it wait for user action.<br>
     * If user doesn't make any action, this popup will be displayed indefinitely
     */
    public static final int WAIT_USER = -1;
    
    /**
     * The field to put popup text in in the intent extras
     */
    public static final String POPUP_TEXT = "POPUP_TEXT";
    
    /**
     * The field to put popup display time in in the intent 
     */
    public static final String POPUP_LENGTH = "POPUP_LENGHT";
    
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
         * Extract popup's parameters from intent.
         */
        final Intent intent = getIntent();
        final String text = intent.getStringExtra(POPUP_TEXT);
        final int time = intent.getIntExtra(POPUP_LENGTH, SHORT);

		setContentView(R.layout.wash_popup);
		
	}
	
    @Override
    protected void onStop() {
    	WashConfirmationPopup.this.finish();
        super.onStop();
    }
}
