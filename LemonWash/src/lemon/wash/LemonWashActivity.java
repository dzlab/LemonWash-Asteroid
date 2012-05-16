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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LemonWashActivity extends Activity {
	
	private Handler mHandler = new Handler();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.splashscreen);
		
		mHandler.postDelayed(new Runnable() {
            public void run() {
            	setContentView(R.layout.main);
            	TextView txt = (TextView) findViewById(R.id.title_auth);
            	Typeface font = Typeface.createFromAsset(getAssets(), "bello_pro.ttf");
            	
            	txt.setTypeface(font);
            	
        		final Button connect_bt = (Button) findViewById(R.id.connect);
        		final EditText phone_et = (EditText) findViewById(R.id.username);
        		phone_et.requestFocus();
        		connect_bt.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						submit_Form(v);
					}
        		});
            }
        }, 2000);
    }
    
    public void submit_Form(View arg0)
    {
    	TextView loginText = (TextView) findViewById(R.id.username);
    	TextView passwText = (TextView) findViewById(R.id.password);
    	CharSequence login = loginText.getText();
		CharSequence passw = passwText.getText();
		if (login.length() < 10 || passw.length() == 0)
			Toast.makeText(getBaseContext(), "Erreur : Tous les champs sont obligatoires!", Toast.LENGTH_SHORT).show();
		else
		{
			GParams.phone = login.toString();
			try
			{
				HttpClient hc = new DefaultHttpClient();
				//HttpPost post = new HttpPost("http://192.168.50.215/gate.php?a=login");
				HttpPost post = new HttpPost("http://91.121.12.102/gate.php?a=login");
				
				List<NameValuePair> arg_post = new ArrayList<NameValuePair>(2);
				arg_post.add(new BasicNameValuePair("login", login.toString()));
				arg_post.add(new BasicNameValuePair("psw", passw.toString()));
		        post.setEntity(new UrlEncodedFormEntity(arg_post));
				HttpResponse rp = hc.execute(post);
			if(rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				String str = EntityUtils.toString(rp.getEntity());
				if (str.equals("OK"))
				{
					Intent intent = new Intent(LemonWashActivity.this, TestMapActivity.class);
					startActivity(intent);
				}
				else
					Toast.makeText(getBaseContext(), "Try Again !", Toast.LENGTH_SHORT).show();
			}
			}catch(IOException e){
				Toast.makeText(getBaseContext(), "Network Down !", Toast.LENGTH_SHORT).show();
			}
		}
    }

}
