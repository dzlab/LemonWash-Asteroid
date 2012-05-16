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
package com.parrot.parrotmaps.asteroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.parrot.asteroid.ParrotIntent;
import com.parrot.asteroid.connectivity.ConnectivityManagerFactory;
import com.parrot.asteroid.connectivity.bluetooth.ConnectivityBTManagerInterface;
import com.parrot.asteroid.phonebook.contact.PhonebookContact;
import com.parrot.asteroid.tools.phone.PhoneType;
import com.parrot.parrotmaps.log.PLog;

/**
 * Handle phone call launching.
 * 
 * @author FL
 *
 */
public class DialingManager {
	
	private static final String TAG = "DialingManager";
	private static ConnectivityBTManagerInterface sConnectivityBluetoothManager = null;

	/**
	 * Initialize the DialingManager.
	 * @param ctx Application context
	 */
	static synchronized public void initialize(Context ctx)
	{
		if( null == sConnectivityBluetoothManager ) {
			sConnectivityBluetoothManager = ConnectivityManagerFactory.getBTConnectivityManager(ctx);
		}
	}
	
	static synchronized public void stop()
	{
		if( null != sConnectivityBluetoothManager ) {
			sConnectivityBluetoothManager = null;
		}
	}
	
	/**
	 * @return true if the system is ready to launch a phone call
	 * via a paired device.
	 */
	static private boolean isTelephonyAvailable( ) {
		boolean telephonyAvailable = false;
		if( (null != sConnectivityBluetoothManager)
				&& sConnectivityBluetoothManager.isManagerReady()
				&& sConnectivityBluetoothManager.isActivePhoneConnected() ) {
			telephonyAvailable = true;
		}
		return telephonyAvailable;
	}
	
	/**
	 * Launch a phone call.
	 * @param ctx Application context
	 * @param name Contact name
	 * @param number Contact number
	 * @return true on success, false otherwise.
	 */
	static public boolean dialNumber( Activity ctx, String name, String number ) {
		boolean callLaunched = false;
		if (isTelephonyAvailable()) {
			Intent intent = new Intent(ParrotIntent.ACTION_OUTGOING_CALL);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(ParrotIntent.EXTRA_OUTGOING_CALL_CONTACT_ID, PhonebookContact.NO_ID);
			intent.putExtra(ParrotIntent.EXTRA_OUTGOING_CALL_PHONE_ID, PhonebookContact.NO_INDEX);
			intent.putExtra(ParrotIntent.EXTRA_OUTGOING_CALL_CONTACT_PHOTO, "");
			intent.putExtra(ParrotIntent.EXTRA_OUTGOING_CALL_CONTACT_NAME, name);
			intent.putExtra(ParrotIntent.EXTRA_OUTGOING_CALL_PHONE_NUMBER, number);
			intent.putExtra(ParrotIntent.EXTRA_OUTGOING_CALL_PHONE_TYPE, PhoneType.NO_TYPE);
			ctx.startService(intent);
			callLaunched = true;
		}
		else {
        	PLog.i(TAG, "dialNumber: HFP Service is not started or no phone is connected!");
        	callLaunched = false;
		}
		return callLaunched;
	}

}
