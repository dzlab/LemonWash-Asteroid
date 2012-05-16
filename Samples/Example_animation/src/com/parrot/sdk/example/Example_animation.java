package com.parrot.sdk.example;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


/**
 * Example of animation
 * @author Parrot
 * 
 */

public class Example_animation extends Activity {
    
    private class myItem
    {
        private String mMenuName = null;
        private boolean mChecked = false;
        public myItem(String name, boolean checked)
        {
            mMenuName = name;
            mChecked = checked;
        }
        
        public String getName()
        {
            return mMenuName;
        }
        public boolean isChecked()
        {
            return mChecked;
        }
        public void setChecked(boolean value)
        {
            mChecked = value;
        }
    }
      
    ListView mMenuListView;
    AnimationDrawable mMenuHeaderAnimationDrawable;
    ImageView mMenuHeaderImage;

    ArrayList<myItem> mListItem;

   
   /** Called when the activity is first created. */
   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        
        setContentView(R.layout.screen_list);
        
        mMenuHeaderImage = (ImageView) findViewById(R.id.menu_header_process_running_image);
        mMenuHeaderAnimationDrawable =(AnimationDrawable)mMenuHeaderImage.getDrawable();
        
        mMenuListView = (ListView) findViewById(R.id.my_listview);
        
        mListItem = new ArrayList<myItem>();
        mListItem.add(new myItem("animation", false));  
        mListItem.add(new myItem("don't check me", false));  

        ExampleAdapter mAdapter = new ExampleAdapter ();
        
        //we give this adapter to the listview
        mMenuListView.setAdapter(mAdapter);
        
        //what append on a click
        mMenuListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //change value of the checkbox in mListItem 
                mListItem.get(position).setChecked(mListItem.get(position).isChecked()?false:true);
                
                //notify the graphic checkbox of his new value, it would update the display
                final CheckBox checkbox = (CheckBox) view.findViewById(R.id.ListItemCheckBox);
                if(mListItem.get(position).isChecked())
                {
                    checkbox.setChecked(true);
                    if(position == 0)
                    {
                        //start the animation and set display it
                        mMenuHeaderAnimationDrawable.start();
                        mMenuHeaderImage.setVisibility(View.VISIBLE);
                    }
                }
                else
                {
                    checkbox.setChecked(false);
                    if(position == 0)
                    {
                        //stop the animation
                        mMenuHeaderAnimationDrawable.stop();
                        mMenuHeaderImage.setVisibility(View.GONE);
                    }
                }

            }
        });
        
     
        
    }
   
   public class ExampleAdapter extends BaseAdapter
   {

       public int getCount()
       {
           return mListItem.size();
       }

       public Object getItem(int position)
       {
           
           return mListItem.get(position);
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
           final CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.ListItemCheckBox);
         
           textview.setText(mListItem.get(position).getName());

           checkbox.setChecked(mListItem.get(position).isChecked());
                      
           return convertView;
       }
       

   }
}  
