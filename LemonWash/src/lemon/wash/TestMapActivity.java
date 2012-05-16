package lemon.wash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import lemon.wash.speech.ParrotTTSObserver;
import lemon.wash.speech.ParrotTTSPlayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TestMapActivity extends MapActivity implements ParrotTTSObserver, LocationListener {
	
	public static final String TAG = TestMapActivity.class.getSimpleName();
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
        setContentView(R.layout.main_map);
        mContext = this;
        mMapView = (MapView) findViewById(R.id.mapview);
        mMapController = mMapView.getController();
        
        mMapView.setBuiltInZoomControls(true);
        
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        displayAddress(lastKnownLocation);
        updateMapView(lastKnownLocation);        
        if(mMapController != null)
        	mMapController.setZoom(17);
        
        mTTSPlayer = new ParrotTTSPlayer(this, this);
				
        Button btnWashMe = (Button) findViewById(R.id.btnWashMe);
        btnWashMe.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				handleUserOrder();
			}
		});       
    }
    
    private void displayDialogParams() {
    	final CharSequence[] items = {"Sélectionner ma voiture", "Ajouter une nouvelle voiture"};

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Paramètres");
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	switch(item) {
    	    	case 0:
    	    		displayDialogCars();
    	    		break;
    	    		
    	    	case 1:
    	    		displayDialogNewCar();
    	    		break;
    	    	}
    	    }
    	});
    	AlertDialog alert = builder.create();
    	builder.show();
    }

    private void displayDialogCars() {
    	final CharSequence[] items = {GParams.myCar[0], GParams.myCar[2], GParams.myCar[1]};

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Sélectionner ma voiture");
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	GParams.myCarIndex = item;
    	    	List<NameValuePair> args = new ArrayList<NameValuePair>();    	        	    
   				args.add(new BasicNameValuePair("brend", GParams.myCarBrend[item]));    	   				
   				args.add(new BasicNameValuePair("type", GParams.myCarType[item]));
   				args.add(new BasicNameValuePair("ident", GParams.myCarIdent[item]));
   			    args.add(new BasicNameValuePair("phonenumber", GParams.phone));
   			    send(GParams.CMD_MYCAR, args);
    	        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    	    }
    	});
    	AlertDialog alert = builder.create();
    	builder.show();
    }
    
    private void displayDialogNewCar() {
    	final CharSequence[] items = {"Audi", "BMW", "Citroën", "Dacia"};

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Sélectionner la marque (1/3)");
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	GParams.newCarBrend = items[item].toString();
    	    	displayDialogNewCarType();    	        
    	    }
    	});
    	AlertDialog alert = builder.create();
    	builder.show();
    }
    private void displayDialogNewCarType() {
    	final CharSequence[] items = GParams.brend2type.get(GParams.newCarBrend);

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Choisissez le modèle (2/3)");
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	GParams.newCarType = items[item].toString();
    	    	
    	        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    	    }
    	});
    	AlertDialog alert = builder.create();
    	builder.show();
    }
    private void displayDialogNewCarIdent() {
    	final CharSequence[] items = {"Audi", "BMW", "Citroën", "Peugeot", "Dacia"};

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Sélectionner la marque (1/3)");
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	GParams.newCarBrend = items[item].toString();
    	    	
    	        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    	    }
    	});
    	AlertDialog alert = builder.create();
    	builder.show();
    }

    private void displayDialogNotation() {
    	List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("phonenumber", GParams.phone));
		String response = send(GParams.CMD_CHECK_NOTE, args);
		if(response.contains("NoNotReq")) {
			Toast.makeText(getApplicationContext(), "Pas besoin de noter.", Toast.LENGTH_SHORT).show();
		}else if(response.contains("NotReq")) {
			final CharSequence[] items = {"Parfait", "Bien", "Pas mal", "Nul"};

	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle("Notation du lavage");
	    	builder.setItems(items, new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int item) {    	    	    	    	
	    	    	List<NameValuePair> args = new ArrayList<NameValuePair>();    	     
	   				args.add(new BasicNameValuePair("note", ""+item));
	   			    args.add(new BasicNameValuePair("phonenumber", GParams.phone));
	   			    send(GParams.CMD_NOTE, args);
	    	        Toast.makeText(getApplicationContext(), "Merci de votre notation.", Toast.LENGTH_SHORT).show();
	    	    }
	    	});
	    	AlertDialog alert = builder.create();
	    	builder.show();
		}       
    }

    
    private void displayDialogWashInProgress() {
	    Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.looking_washer_popup);
        dialog.setTitle("Lavage en attente");
        
        //((TextView)dialog.findViewById(android.R.attr.dialogTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "bello_pro.ttf"));
        //dialog.setCancelable(true);
        Button btn = (Button) dialog.findViewById(R.id.btnOK);
        btn.requestFocus();
        dialog.show();
    }

    private void displayAddress(Location location) {
		setProgressBarIndeterminateVisibility(true);
 
		//Le geocoder permet de récupérer ou chercher des adresses
		//gràce à un mot clé ou une position
		Geocoder geo = new Geocoder(this);
		try {
			GParams.jAddr = "{lat:" + location.getLatitude() + " ,lng:" + location.getLongitude() + "}";
			//Ici on récupère la premiere adresse trouvé gràce à la position que l'on a récupéré
			List<Address> adresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(),1);
 
			if(adresses != null && adresses.size() == 1){
				Address adresse = adresses.get(0);				
				//Si le geocoder a trouver une adresse, alors on l'affiche
				GParams.address = String.format("%s, %s %s",
						adresse.getAddressLine(0),
						adresse.getPostalCode(),
						adresse.getLocality());
				((TextView)findViewById(R.id.civic_addr)).setText(GParams.address);
			}
			else {
				((TextView)findViewById(R.id.civic_addr)).setText("L'adresse n'a pu être déterminée");
			}
		} catch (Exception e) {
			e.printStackTrace();
			((TextView)findViewById(R.id.civic_addr)).setText("L'adresse n'a pu être déterminée");
		}
		//on stop le cercle de chargement
		setProgressBarIndeterminateVisibility(false);
	}
    
    public void updateMapView(Location location) {
    	lat = location.getLatitude(); //48.8648534;
        lng = location.getLongitude(); //2.3347674;
        GeoPoint gp = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
        mMapController = mMapView.getController();
        mMapController.setCenter(gp);
    }
    
    public void handleUserOrder() {
		/* mConfirmDialog = new Dialog(mContext, R.style.PauseDialog);
		mConfirmDialog.setContentView(R.layout.wash_popup);
		mConfirmDialog.setTitle("Confirmer votre demande?");
       
		mConfirmDialog.setCancelable(true);
        Button btnConfirm = (Button) mConfirmDialog.findViewById(R.id.btnConfirm);
        Button btnClose = (Button) mConfirmDialog.findViewById(R.id.btnAnnuler);
        btnConfirm.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				mConfirmDialog.dismiss();
				Dialog dialog = new Dialog(mContext);
		        dialog.setContentView(R.layout.looking_washer_popup);
		        dialog.setTitle("Lavage en attente");
		        
		        //((TextView)dialog.findViewById(android.R.attr.dialogTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "bello_pro.ttf"));
		        //dialog.setCancelable(true);
		        Button btn = (Button) dialog.findViewById(R.id.btnOK);
		        btn.requestFocus();
		        dialog.show();
		        //dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.your_icon);
			}
		});
        
        btnClose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mConfirmDialog.cancel();				
			}
		});
        mConfirmDialog.show(); */
    	if(GParams.myCarIndex != -1) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle("Demander un lavage ?")
        			.setMessage(GParams.myCar[GParams.myCarIndex] + "\nTemps maximum estimé: 2 heures\nCoût: 29,9€")
        	       .setCancelable(false)
        	       .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog_, int id) {
        	        	    List<NameValuePair> args = new ArrayList<NameValuePair>();    	        	    
        	   				args.add(new BasicNameValuePair("adresse", GParams.jAddr));    	   				
        	   			    args.add(new BasicNameValuePair("phonenumber", GParams.phone));
        	   			    String response = send(GParams.CMD_WASH, args);
        	   			    String message = "Nous recherchons un laveur, vous servez prévenus par SMS des étapes du lavage de votre voiture.";
        	   			    if(response.equals("exception")) {
        	   			    	message = "Problème lors d'envoi de commande.\nMerci de réessayer plus tard.";
        	   			    }else if(response.contains("ErrorAlready")){
        	   			    	message = "Vous avez déjà une commande en cours.";
        	   			    }
    	   			    	playMessage(message);
    	   			    	Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();    	   			    	
        	           }
        	       })
        	       .setNegativeButton("Non", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	AlertDialog alert = builder.create();
        	builder.show();
    		
    	}else {
    		playMessage("Vous devez choisir une véhicule avant de commander un lavage.");
    		displayDialogCars();
    	}
    }
    
    public String send(String command, List<NameValuePair> args) {
    	String response = "";
    	Log.i(TAG, "Sending " + command + " to server.");
		try {
			HttpClient hc = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://91.121.12.102/gate.php?a="+command);
						
	        post.setEntity(new UrlEncodedFormEntity(args));
			HttpResponse rp = hc.execute(post);
			String str = EntityUtils.toString(rp.getEntity());
			Log.i(TAG, "" + rp.getEntity() + ", " + str);
			response = str;
			if(rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				if (str.equals("OK")) {
					
				//Intent intent = new Intent(LemonWashActivity.this, TestMapActivity.class);
				//startActivity(intent);
				//
				}
				else
					Log.e(TAG, "Try Again !");
			}
		}catch(IOException e){
			response = "exception";
			Log.e(TAG, "Network Down !");
		}
		return response;
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
            	displayDialogParams();
                return true;
            case R.id.optNotation:
            	displayDialogNotation();
            	return true;
            case R.id.optLogout:
            	Intent intent = new Intent(TestMapActivity.this, LemonWashActivity.class);
				startActivity(intent);
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
        	case KeyEvent.KEYCODE_DPAD_UP:
        		zoomOut();
        		break;
        	case KeyEvent.KEYCODE_DPAD_DOWN:
        		zoomIn();
        		break;
        	case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
    		
        		break;
        	case KeyEvent.KEYCODE_MEDIA_NEXT:
    		
        		break;
            case KeyEvent.KEYCODE_3:
                mc.zoomIn();
                break;
            case KeyEvent.KEYCODE_1:
                mc.zoomOut();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
	public void zoomIn() {
//		PLog.v(TAG, "zoomIn");
		runOnUiThread(new Runnable() {
			public void run() {
				try {
					mMapController.zoomIn();
				}
				catch( OutOfMemoryError e ) {
					// These exception can occures sometimes with MapController
					Log.e(TAG,"zoomIn - An exception occured: ",e);
				}
				//positionChanged();
			}
		});
	}

	public void zoomOut() {
//		PLog.v(TAG, "zoomOut");
		runOnUiThread(new Runnable() {
			public void run() {
				try {
					mMapController.zoomOut();
				}
				catch( OutOfMemoryError e ) {
					// These exception can occures sometimes with MapController
					Log.e(TAG,"zoomOut - An exception occured: ",e);
				}
				//positionChanged();
			}
		});
	}

	public void onTTSFinished() {
		
	}

	public void onTTSAborted() {
		// TODO Auto-generated method stub
		
	}

	public void onLocationChanged(Location location) {
		updateMapView(location);
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	} 
}