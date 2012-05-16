package com.parrot.sdk.example;


import java.util.ArrayList;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Example of context_menu
 * You need a file option_menu.xml and override some methods.
 * @author Parrot
 * 
 */

public class Example_context_menu extends Activity {
    
   /** Called when the activity is first created. */
   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);            
    }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
       final MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.options_menu, menu);
       return true;
   }
   
   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {

       final ArrayList<Integer> optionsMenuGroup = new ArrayList<Integer>();

       optionsMenuGroup.add(new Integer(R.id.options_menu_hello));
       optionsMenuGroup.add(new Integer(R.id.options_menu_quit));

       if (null == optionsMenuGroup) {
           return false;
       }

       //the layout menu/option_menu.xml must match optionsMenuGroup 
       for (int i = 0; i < menu.size(); i++) 
       {
           final MenuItem item = menu.getItem(i);
           final int id = item.getItemId();
           
           // this item should be displayed by this screen
           if (optionsMenuGroup.contains(id)) 
           {
               item.setVisible(true);
               
               switch(id){
                   case R.id.options_menu_hello:
                       //in specific case you may want to disable a menu, in this case uncomment the line below
                       //item.setVisible(false);
                       break;
               }
           } 
           else 
           {
               item.setVisible(false);
           }
       }
       return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {            
           case R.id.options_menu_hello:
               //do something
               break;
           case R.id.options_menu_quit:
               finish();
               break;
       }
       return super.onOptionsItemSelected(item);
   }
}  
