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
package com.parrot.parrotmaps.network;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.parrot.asteroid.ParrotIntent;
import com.parrot.parrotmaps.log.PLog;


/**
 * Monitor network status.
 * 
 * @author FL
 *
 */
public class NetworkStatus
{
    private static final String LOG_TAG = "NetworkStatus";

    private static final int ETHERNET_STATE_UNKNOWN = 0;
    private static final int ETHERNET_STATE_ENABLED = 1;
    private static final int ETHERNET_STATE_DISABLED = 2;
    private Integer mEthernetState = ETHERNET_STATE_UNKNOWN;
    
    private List<NetworkStatusListener> mListeners = new ArrayList<NetworkStatusListener>();
    private Context mContext;

    private Handler mHandler;
	private HandlerThread mEthernetStateTimeoutTthread;

    private EthernetStateBroadcastReceiver mEthernetStateBroadcastReceiver = null;
    private Object mEthernetStateBroadcastReceiverLock = new Object();

	private static NetworkStatus sInstance = null;
	public static synchronized NetworkStatus getInstance( Context context ) {
		if( null == sInstance ) {
			sInstance = new NetworkStatus(context);
		}
		return sInstance;
	}
	
	/**
	 * Release unique instance.
	 */
	public static synchronized void deleteInstance() {
		sInstance.unregisterAllListeners();
		sInstance = null;
	}

    /**
     * Private constructor.
     * @param context
     */
	private NetworkStatus( Context context ) {
		// Init ethernet network state receiver
		PLog.i(LOG_TAG,"NetworkStatus");
		IntentFilter networkIntentFilter = new IntentFilter();
		networkIntentFilter.addAction(ParrotIntent.DEBUG_ETHERNET_STATE);
		mEthernetStateBroadcastReceiver = new EthernetStateBroadcastReceiver();
		context.registerReceiver(mEthernetStateBroadcastReceiver, networkIntentFilter);
		
		mContext = context;
	}
	
	public void checkNetworkAvailability() {
		// If the Ethernet state is not received in 3 seconds
		// the Ethernet state is not considered as disabled
		mEthernetStateTimeoutTthread = new HandlerThread(LOG_TAG);
		mEthernetStateTimeoutTthread.start();
		mHandler = new EthernetStateTimeoutHandler(mEthernetStateTimeoutTthread.getLooper());
		mHandler.sendEmptyMessageDelayed(0, 3*1000);

		mContext.sendBroadcast(new Intent(ParrotIntent.DEBUG_ETHERNET_STATE_REQ));
	}
	
	/**
     * @return true if any network is available, whatever network it is and
     *         whatever its current power is.<br>
     *         false if no network is available.
     */
    public boolean isNetworkAvailable() {
        if (null != mContext) {
            final ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (null != connectivity && connectivity.getBackgroundDataSetting()) {
                final NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null) {
                    for (int i = 0; i < info.length; i++) {
//                        PLog.v(LOG_TAG, info[i].toString()+" state:"+info[i].getState());
                        if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                            PLog.v(LOG_TAG, "isNetworkAvailable - Connected: ",info[i].toString());
                            return true;
                        }
                    }
                }
            }
        }
        boolean ethernetAvailable;
        synchronized( mEthernetState ) {
        	ethernetAvailable = (mEthernetState == ETHERNET_STATE_ENABLED);
        }
        PLog.v(LOG_TAG, "isNetworkAvailable - Connexion: ",ethernetAvailable);
        return ethernetAvailable;

    }

	
	/**
	 * @return true is the connection to Asteroid services is ready,
	 * false otherwise.
	 */
	public boolean isReady()
	{
		boolean ready;
        synchronized( mEthernetState ) {
        	ready = (ETHERNET_STATE_UNKNOWN != mEthernetState);
        }		
		return ready;
	}
	
	/**
	 * Notify listener that this manager is ready or not
	 * to give network status.
	 */
	private void notifyListeners() {
		synchronized(mListeners) {
			boolean networkStatusReady = isReady();
			for( NetworkStatusListener listener : mListeners ) {
				listener.onNetworkStatusReady(networkStatusReady);
			}
		}
	}
	
	/**
	 * Register a network status listener.
	 * @param listener The listener.
	 */
	public void registerListener(NetworkStatusListener listener) {
		synchronized(mListeners) {
			if( !mListeners.contains(listener) ) {
				mListeners.add(listener);
			}
		}
	}

	/**
	 * Unregister a network status listener.
	 * @param listener The listener.
	 */
	public void unregisterListener(NetworkStatusListener listener) {
		synchronized(mListeners) {
			if( mListeners.contains(listener) ) {
				mListeners.remove(listener);
			}
		}
	}

	/**
	 * Unregister all network status listeners.
	 */
	public void unregisterAllListeners() {
		synchronized(mListeners) {
			mListeners.clear();
		}
	}
	
	/**
	 * Ethernet network state broadcast listener
	 */
	private class EthernetStateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
            if( ParrotIntent.DEBUG_ETHERNET_STATE.equals(intent.getAction()) ) {
            	boolean activated = intent.getBooleanExtra(ParrotIntent.DEBUG_ETHERNET_STATE_EXTRA, false);
            	synchronized(mEthernetState) {
	            	if( activated ) {
	            		mEthernetState = ETHERNET_STATE_ENABLED;
	            	}
	            	else {
	            		mEthernetState = ETHERNET_STATE_DISABLED;
	            	}
            	}
                PLog.i(LOG_TAG,"EthernetStateBroadcastReceiver.onReceive - activated ",activated);
                notifyListeners();
            }
		}
		
	}
	
	/**
	 * Ethernet state workaround.
	 * If the state is not received 3 seconds after start up,
	 * it is considered as disabled.
	 */
    private final class EthernetStateTimeoutHandler extends Handler {
        public EthernetStateTimeoutHandler(Looper looper) {
            super(looper);
        }
    	
        @Override
        public void handleMessage(Message msg) {
        	boolean notify = false;
        	synchronized( mEthernetState ) {
        		if( ETHERNET_STATE_UNKNOWN == mEthernetState ) {
        			mEthernetState = ETHERNET_STATE_DISABLED;
        			notify = true;
        		}
        	}
        	if( notify ) {
        		notifyListeners();
        	}
        }
    }

    /**
     * Unregister broadcast receivers before quitting the application to avoid memory leak.
     */
	public void unregisterReceivers()
	{
		synchronized(mEthernetStateBroadcastReceiverLock) {
			if (mEthernetStateBroadcastReceiver != null)
			{
				try {
					mContext.unregisterReceiver(mEthernetStateBroadcastReceiver);
				} catch( final IllegalArgumentException e ) {
					// Do nothing
				}
				mEthernetStateBroadcastReceiver = null;
			}
		}
	}

}
