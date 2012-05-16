package com.parrot.sdk.example;

import java.util.ArrayList;

import com.parrot.sdk.example.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;



/**
 * Example of pop up in activity, it allows more customization than toast an DialogBox
 * As there is 2 activity, we use intent, so see intent-filter in the manifest 
 * 
 *  
 * Normally the the pop up appear on front of last screen, if there is a black background, it is an android bug
 * it occurs after sending apk, restart the product and the bug disapear...
 * So you only have this bug during the development
 * 
 * @author Parrot
 * 
 */

public class MainActivity extends Activity {
    
    private ListView mMenuListView;
    private ArrayList<String> mListName = null;
    
   /** Called when the activity is first created. */
   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_list);
        
        mMenuListView = (ListView) findViewById(R.id.my_listview);
        
        //create an array to store the sub menu name
        mListName = new ArrayList<String>();
        
        mListName.add(new String("popup with user action"));
        mListName.add(new String("popup with time out"));
        
        ExampleAdapter adapter = new ExampleAdapter ();
        
        //we give this adapter to the listview
        mMenuListView.setAdapter(adapter);
        
        //what append on a click
        mMenuListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch(position){
                    case 0:
                        launchPopup(mListName.get(position), PopupActivity.WAIT_USER, "com.parrot.sdk.example.VALIDATE");
                        break;
                    case 1:
                        launchPopup(mListName.get(position), PopupActivity.SHORT, null);
                        break;
                }
                
            }
        });
    }
   
   public class ExampleAdapter extends BaseAdapter
   {

       public int getCount()
       {
           return mListName.size();
       }

       public Object getItem(int position)
       {
           
           return mListName.get(position);
       }

       public long getItemId(int position)
       {
           return position;
       }

       public View getView(int position, View convertView, ViewGroup parent)
       {
           if (convertView == null)
           {
               //create a new TextView
               convertView = getLayoutInflater().inflate(R.layout.list_item, parent, false);
           }
           //else a android mecanism allows us to recycle view no more needed so use convertView
           
           final TextView textview = (TextView) convertView.findViewById(R.id.text_row);
           textview.setText(mListName.get(position));

           return convertView;
       }
   }
        
   /**
    * Send an intent to display a pop up
    * @param text the text which display the pop up.
    * @param displayTime how much time the pop up will appear.
    * @param validCallbackAction set a callback if the pop up is validated.
    */
   private void launchPopup(String text, int displayTime, String validCallbackAction){
       final Intent intent = new Intent(PopupActivity.class.getName());
       
       intent.putExtra(PopupActivity.POPUP_TEXT, text);
       intent.putExtra(PopupActivity.POPUP_LENGTH, displayTime);

       if (null != validCallbackAction){
           intent.putExtra(PopupActivity.VALID_CALLBACK_ACTION, validCallbackAction);
       }
         
       startActivity(intent);
   }
   
   @Override
   protected void onNewIntent(Intent intent) {
       //the ok button of pop up has been validated
       if ("com.parrot.sdk.example.VALIDATE".equals(intent.getAction())){
           launchPopup("popup validated", PopupActivity.SHORT, null);
       } else {
           super.onNewIntent(intent);
       }
   }
   
}  
