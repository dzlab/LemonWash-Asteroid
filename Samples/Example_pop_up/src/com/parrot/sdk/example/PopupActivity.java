package com.parrot.sdk.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Displays a popup over the Activity.<br>
 * @author Parrot
 *
 */
public class PopupActivity extends Activity {

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
    
    /**
     * The field to put popup valid callback action in in the intent.<br>
     * A callback action is an intent action that will, if set, be called when pressing the 
     * ok button.
     */
    public static final String VALID_CALLBACK_ACTION = "VALID_CALLBACK_ACTION";
    
    /**
     * The field to put popup cancel callback action in in the intent.<br>
     * A callback action is an intent action that will, if set, be called when pressing the 
     * cancel button.
     */
    public static final String CANCEL_CALLBACK_ACTION = "CANCEL_CALLBACK_ACTION";
    
    /**
     * This popup's callback action.<br> Represents an intent action that will be sent when
     * pressing the popup's "ok" button
     */
    private String mValidCallbackAction = "";
    
    /**
     * This popup's callback action.<br> Represents an intent action that will be sent when
     * pressing the popup's "cancel" button
     */
    private String mCancelCallbackAction = "";
    
    /**
     * Indicates the popup to dismiss
     */
    private static final int QUIT = 1;

    /**
     * The listener that will be called when clicking the "ok" button
     */
    private OnClickListener mOnOkClickListener = new OnClickListener(){
        public void onClick(View v) {
            if ((null != mValidCallbackAction) && !"".equals(mValidCallbackAction)){
                Log.v("popup", "start " + mValidCallbackAction);

                startActivity(new Intent(mValidCallbackAction));
            }
            PopupActivity.this.finish();
        }
    };

    /**
     * The listener that will be called when clicking the "cancel" button
     */
    private OnClickListener mOnCancelClickListener = new OnClickListener(){
        public void onClick(View v) {
            if ((null != mCancelCallbackAction) && !"".equals(mCancelCallbackAction)){
                startActivity(new Intent(mCancelCallbackAction));
            }
            PopupActivity.this.finish();
        }
    };
    
    /**
     * The handler that will be used for the popup's timeout.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case QUIT:
                    PopupActivity.this.finish();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /*
         * Extract popup's parameters from intent.
         */
        final Intent intent = getIntent();
        final String text = intent.getStringExtra(POPUP_TEXT);
        final int time = intent.getIntExtra(POPUP_LENGTH, SHORT);
        mValidCallbackAction = intent.getStringExtra(VALID_CALLBACK_ACTION);
        mCancelCallbackAction = intent.getStringExtra(CANCEL_CALLBACK_ACTION);
        
        /*
         * Display the popup with the specified parameters.
         */
        Log.v("pop up","onCreate");
        setContentView(R.layout.error_popup);
        final TextView textView = (TextView) findViewById(R.id.error_popup_text_view);

        ImageButton valid = (ImageButton) findViewById(R.id.error_popup_button_ok);
        ImageButton cancel = (ImageButton) findViewById(R.id.error_popup_button_cancel);

        valid.setOnClickListener(mOnOkClickListener);
        cancel.setOnClickListener(mOnCancelClickListener);

        /*
         * "ok" and "cancel" buttons will only be displayed if the popup should wait for
         * user's action. if it has a dismiss timeout, it won't display any button
         */
        textView.setText(text);
        if (WAIT_USER != time){
            valid.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
            mHandler.sendEmptyMessageDelayed(QUIT, time);
        } else {
            valid.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.VISIBLE);
        }
    }
    

    @Override
    protected void onStop() {
        PopupActivity.this.finish();
        super.onStop();
    }
}
