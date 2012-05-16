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
package com.parrot.parrotmaps.localsearch;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.InfoWindow;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.Marker;

public class ResultMarker extends Marker {

	private ArrayList<String> mPhoneNumbersList = null;
	private ArrayList<String> mAddressLinesList = null;
	private String mCountry;
	private String mState;
	private String mCity;
	private String mStreet;

	public ResultMarker( LatLng latlng,
                         int index,
                         String title,
                         int zIndex,
                         InfoWindow infoWindow)
			throws Exception {
		super(latlng, TYPE.RESULT, getAssetNameByIndex(index), title, zIndex, infoWindow);
	}

	public ResultMarker( Parcel in ) {
        super(in,TYPE.RESULT);
    	mPhoneNumbersList = new ArrayList<String>();
    	in.readStringList(mPhoneNumbersList);
    	mAddressLinesList = new ArrayList<String>();
    	in.readStringList(mAddressLinesList);
    	mCountry = in.readString();
    	mState = in.readString();
    	mCity = in.readString();
    	mStreet = in.readString();
	}

    @Override
	public void writeToParcel(Parcel dest, int flags) {
    	super.writeToParcel(dest, flags);
    	dest.writeStringList(mPhoneNumbersList);
    	dest.writeStringList(mAddressLinesList);
    	dest.writeString(mCountry);
    	dest.writeString(mState);
    	dest.writeString(mCity);
    	dest.writeString(mStreet);
    }
	
    /**
     * Get the name of image displayed for a marker with a given index.
     * @param index Marker index.
     * @return Name of drawable in assets.
     */
    private static String getAssetNameByIndex( int index ) {
    	String assetName;
    	switch(index) {
    	case 0:
    		assetName = "marker_result_a.png";
    		break;
    	case 1:
    		assetName = "marker_result_b.png";
    		break;
    	case 2:
    		assetName = "marker_result_c.png";
    		break;
    	case 3:
    		assetName = "marker_result_d.png";
    		break;
    	case 4:
    		assetName = "marker_result_e.png";
    		break;
    	case 5:
    		assetName = "marker_result_f.png";
    		break;
    	case 6:
    		assetName = "marker_result_g.png";
    		break;
    	case 7:
    		assetName = "marker_result_h.png";
    		break;
    	case 8:
    		assetName = "marker_result_i.png";
    		break;
    	case 9:
    		assetName = "marker_result_j.png";
    		break;
    	default:
    		assetName = "marker_result.png";
    		break;
    	}
    	return assetName;
    }
    
	/**
	 * Sets the entire phone numbers list linked with this marker.
	 * @param list of phone numbers
	 */
	public void setPhoneNumbersList (ArrayList<String> list) {
		mPhoneNumbersList = list;
	}
	
	/**
	 * Sets the entire address lines list linked with this marker.
	 * @param list of address lines
	 */
	public void setAddressLinesList (ArrayList<String> list) {
		mAddressLinesList = list;
	}
	
	/**
	 * Gets the entire phone numbers list linked with this marker.
	 * @return Phone numbers list
	 */
	public ArrayList<String> getPhoneNumbersList () {
		return mPhoneNumbersList;
	}
	
	/**
	 * Gets the entire phone numbers list linked with this marker.
	 * @param Address lines list
	 */
	public ArrayList<String> getAddressLinesList () {
		return mAddressLinesList;
	}

	public CharSequence getAddress() {
		String address = null;
		if (null != mAddressLinesList ) {
			StringBuffer buf = new StringBuffer("");
			for (int i=0 ; i<mAddressLinesList.size() ; i++) {
				if (i == mAddressLinesList.size()-1) {
					buf.append(mAddressLinesList.get(i));
				}
				else {
					buf.append(mAddressLinesList.get(i)).append("\n");
				}
			}
			address = buf.toString();
		}
		return address;
	}

	/**
	 * Get the address first line
	 */
	public CharSequence getAddressLine1() {
		String address = null;
		if (null != mAddressLinesList ) {
			StringBuffer buf = new StringBuffer("");
			for (int i=0 ; i<mAddressLinesList.size()-1 ; i++) {
				if (i == mAddressLinesList.size()-2) {
					buf.append(mAddressLinesList.get(i));
				}
				else {
					buf.append(mAddressLinesList.get(i)).append(", ");
				}
			}
			address = buf.toString();
		}
		return address;
	}

	/**
	 * Get the address second line
	 * @return The address second line or null if there is not
	 */
	public CharSequence getAddressLine2() {
		String address = null;
		if( (null != mAddressLinesList)
				&& (mAddressLinesList.size() > 1) ) {
			address = mAddressLinesList.get(mAddressLinesList.size()-1);
		}
		return address;
	}

	/**
	 * Get the full address in one line
	 */
	public String getAddressOneLine() {
		String address = "";
		if( null != mAddressLinesList ) {
			for( int i=0; i<mAddressLinesList.size(); i++ )
			{
				if( 0 == i )
				{
					address = "";
				}
				else
				{
					address += ", ";
				}
				address += mAddressLinesList.get(i);
			}
		}
		return address;
	}


	/**
	 * Set the country of this marker
	 * @param mCountry The country to set
	 */
	public void setCountry(String mCountry) {
		this.mCountry = mCountry;
	}


	/**
	 * Get the country of this marker
	 */
	public String getCountry() {
		return mCountry;
	}

	/**
	 * Set the State of this marker
	 * @param mState The State to set
	 */
	public void setState(String mState) {
		this.mState = mState;
	}

	/**
	 * Get the State of this marker
	 */
	public String getState() {
		return mState;
	}

	/**
	 * Set the City of this marker
	 * @param mCity The City to set
	 */
	public void setCity(String mCity) {
		this.mCity = mCity;
	}

	/**
	 * Get the City of this marker
	 */
	public String getCity() {
		return mCity;
	}


	/**
	 * Set the Street of this marker
	 * @param mStreet The Street to set
	 */
	public void setStreet(String mStreet) {
		this.mStreet = mStreet;
	}

	/**
	 * Get the Street of this marker
	 */
	public String getStreet() {
		return mStreet;
	}

	@Override
	public void processAction(Context context,Controller controller) {
		Intent intent = new Intent(context,ResultDetailsActivity.class);
		if( null != controller ) {
			intent.putExtra(ResultDetailsActivity.COM_PARROT_RESDETAILS_CENTER, controller.getLastKnownLocation());
		}
    	intent.putExtra(ResultDetailsActivity.COM_PARROT_RESDETAILS_MARKER, this);
    	context.startActivity(intent);
	}
	
	/**
	 * Set the asset (usually a bitmap) to this marker
	 * @param index The index of the asset 
	 */
	public void setAsset(int index)
	{
		super.setAsset(getAssetNameByIndex(index));
	}

}
