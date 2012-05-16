package com.lemonwash;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.lemonwash.speech.ParrotTTSObserver;
import com.lemonwash.speech.ParrotTTSPlayer;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TestMapActivity extends MapActivity implements ParrotTTSObserver, LocationListener {
	
	private MapView mMapView;
    private MapController mMapController;
    private Context mContext;
    private ParrotTTSPlayer mTTSPlayer = null;
    private Dialog mConfirmDialog = null;
    private double      lat = 0;
    private double      lng = 0;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mContext = this;
        mMapView = (MapView) findViewById(R.id.mapview);
        mMapController = mMapView.getController();
        
        mMapView.setBuiltInZoomControls(true);
        
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateMapView(lastKnownLocation);        
        
        mTTSPlayer = new ParrotTTSPlayer(this, this);
				
        Button btnWashMe = (Button) findViewById(R.id.btnWashMe);
        btnWashMe.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleUserOrder();
			}
		});       
    }
        
    public void updateMapView(Location location) {
    	lat = location.getLatitude(); //48.8648534;
        lng = location.getLongitude(); //2.3347674;
        GeoPoint gp = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
        MapController mc = mMapView.getController();
        mc.setCenter(gp);
        mc.setZoom(17);
    }
    
    public void handleUserOrder() {
    	playMessage("bordel de merde");
		mConfirmDialog = new Dialog(mContext, R.style.PauseDialog);
		mConfirmDialog.setContentView(R.layout.wash_popup);
		mConfirmDialog.setTitle("Confirmer votre demande?");
		//((TextView)mConfirmDialog.findViewById(android.R.attr.dialogTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "bello_pro.ttf"));
		//mConfirmDialog.setCancelable(true);
        
		mConfirmDialog.setCancelable(true);
        Button btnConfirm = (Button) mConfirmDialog.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mConfirmDialog.dismiss();
				Dialog dialog = new Dialog(mContext);
		        dialog.setContentView(R.layout.looking_washer_popup);
		        dialog.setTitle("Lavage en attente");
		        
		        //((TextView)dialog.findViewById(android.R.attr.dialogTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "bello_pro.ttf"));
		        //dialog.setCancelable(true);
		        Button btn = (Button) dialog.findViewById(R.id.btnConfirm);
		        btn.requestFocus();
		        dialog.show();
		        //dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.your_icon);
			}
		});
        btnConfirm.requestFocus();
        mConfirmDialog.show();
    }
    
    public void playMessage(String msg) {
    	try {
    		mTTSPlayer.play(msg);
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_options, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.optOrder:
            	handleUserOrder();
                return true;
            case R.id.optParams:
                
                return true;
            case R.id.optLogout:
                
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Send an intent to display a pop up
     * @param text the text which display the pop up.
     * @param displayTime how much time the pop up will appear.
     */
    private void launchPopup(String text, int displayTime){
    	//final Intent intent = new Intent(WashConfirmationPopup.class.getName());
    	final Intent intent = new Intent("com.lemonwash.WashConfirmationPopup");
    	
        intent.putExtra(WashConfirmationPopup.POPUP_TEXT, text);
        intent.putExtra(WashConfirmationPopup.POPUP_LENGTH, displayTime);
        
        startActivity(intent);
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
        MapController mc = mMapView.getController(); 
        switch (keyCode) 
        {
            case KeyEvent.KEYCODE_3:
                mc.zoomIn();
                break;
            case KeyEvent.KEYCODE_1:
                mc.zoomOut();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

	@Override
	public void onTTSFinished() {
		
	}

	@Override
	public void onTTSAborted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLocationChanged(Location location) {
		updateMapView(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	} 
}