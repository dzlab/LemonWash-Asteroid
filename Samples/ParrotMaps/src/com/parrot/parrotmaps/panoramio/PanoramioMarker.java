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
package com.parrot.parrotmaps.panoramio;

import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.InfoWindow;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.Marker;

public class PanoramioMarker extends Marker {
	
	private String mPhotoTitle;
	private String mPhotoFileURL;
	private String mOwnerId;
	private String mPhotoId;

	public PanoramioMarker( LatLng latlng,
                            URL url,
                            boolean middleAnchor,
                            String title,
                            int index,
                            InfoWindow infoWindow)
			throws Exception {
		super(latlng, TYPE.PANORAMIO, url, middleAnchor, title, index, infoWindow);
	}

	public PanoramioMarker( Parcel in ) {
        super(in,TYPE.PANORAMIO);
        mPhotoTitle = in.readString();
        mPhotoFileURL = in.readString();
        mOwnerId = in.readString();
        mPhotoId = in.readString();
	}

    @Override
	public void writeToParcel(Parcel dest, int flags) {
    	super.writeToParcel(dest, flags);
        dest.writeString(mPhotoTitle);
        dest.writeString(mPhotoFileURL);
        dest.writeString(mOwnerId);
        dest.writeString(mPhotoId);
    }

	public void setOwnerId(String mOwnerId) {
		this.mOwnerId = mOwnerId;
	}


	public String getOwnerId() {
		return mOwnerId;
	}


	public void setPhotoId(String mPhotoId) {
		this.mPhotoId = mPhotoId;
	}


	public String getPhotoId() {
		return mPhotoId;
	}

	public void setPhotoTitle(String mPhotoTitle) {
		this.mPhotoTitle = mPhotoTitle;
	}

	public String getPhotoTitle() {
		return mPhotoTitle;
	}

	public void setPhotoFileURL(String mPhotoFileURL) {
		this.mPhotoFileURL = mPhotoFileURL;
	}

	public String getPhotoFileURL() {
		return mPhotoFileURL;
	}

	@Override
	public void processAction(Context context,Controller controller) {
		Intent intent = new Intent(context,PanoramioPhotoActivity.class);
		intent.putExtra(PanoramioPhotoActivity.INTENT_EXTRA_PHOTO, this);
		context.startActivity(intent);
	}
}
