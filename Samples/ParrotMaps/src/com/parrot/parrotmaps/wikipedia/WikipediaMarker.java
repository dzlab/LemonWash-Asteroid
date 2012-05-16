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
package com.parrot.parrotmaps.wikipedia;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

import com.parrot.parrotmaps.Controller;
import com.parrot.parrotmaps.InfoWindow;
import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.Marker;

public class WikipediaMarker extends Marker {

	private String mThumbnailURL;
	private String mArticleURL;

	public WikipediaMarker( LatLng latlng,
                            boolean middleAnchor,
                            String title,
                            int index,
                            InfoWindow infoWindow)
			throws Exception {
		super(latlng, TYPE.WIKIPEDIA, middleAnchor, title, index, infoWindow);
	}

	public WikipediaMarker( Parcel in ) {
        super(in,TYPE.WIKIPEDIA);
        mThumbnailURL = in.readString();
        mArticleURL = in.readString();
	}

    @Override
	public void writeToParcel(Parcel dest, int flags) {
    	super.writeToParcel(dest, flags);
        dest.writeString(mThumbnailURL);
        dest.writeString(mArticleURL);
    }

	public void setArticleURL(String mArticleURL) {
		this.mArticleURL = mArticleURL;
	}


	public String getArticleURL() {
		return mArticleURL;
	}

	public void setThumbnailURL(String mThumbnailURL) {
		this.mThumbnailURL = mThumbnailURL;
	}

	public String getThumbnailURL() {
		return mThumbnailURL;
	}

	@Override
	public void processAction(Context context,Controller controller) {
		Intent intent = new Intent(context,WikipediaActivity.class);
		intent.putExtra(WikipediaActivity.INTENT_EXTRA_ARTICLE, this);
		context.startActivity(intent);
	}
}
