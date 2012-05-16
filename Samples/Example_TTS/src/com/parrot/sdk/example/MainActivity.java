package com.parrot.sdk.example;

import java.util.ArrayList;

import com.parrot.sdk.example.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

/**
 * 
 * 
 * @author Parrot
 * 
 */

public class MainActivity extends Activity implements ParrotTTSObserver{
	private ListView mMenuListView;

	private ParrotTTSPlayer mTTSPlayer = null;

	private ArrayList<String> mListItem;
	
	private boolean mPlayAll = false;
	private int mPosition = 0;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_list);

		mMenuListView = (ListView) findViewById(R.id.my_listview);
		mListItem = new ArrayList<String>();

		mListItem.add("Authentification requise.");  
		mListItem.add("Confirmer cette action ?");  
		mListItem.add("Votre rendez-vous est confirmé");    

		
		mTTSPlayer = new ParrotTTSPlayer(this, this);

		
		ExampleAdapter mAdapter = new ExampleAdapter ();

		//we give this adapter to the listview
		mMenuListView.setAdapter(mAdapter);

		//what append on a click
		mMenuListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
				if(position == 0)
				{
					mPosition = 0;
					mPlayAll = true;
				}
				else
				{
					mPlayAll = false;
				}
				mTTSPlayer.play(mListItem.get(position));
			}
		});



	    }
	   
	   public class ExampleAdapter extends BaseAdapter
	   {

	       public int getCount()
	       {
	           return mListItem.size();
	       }

	       public String getItem(int position)
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
	         
	           textview.setText(getItem(position));

	                      
	           return convertView;
	       }

		

	   }

	public void onTTSFinished() {
		
		if(mPlayAll)
		{
			mPosition++;
			if(mPosition < mListItem.size())
			{
				mTTSPlayer.play(mListItem.get(mPosition));
			}
			else
			{
				mPlayAll = false;
			}

		}
		
	}

	public void onTTSAborted() {
		// TODO Auto-generated method stub
		
	}
	}  

