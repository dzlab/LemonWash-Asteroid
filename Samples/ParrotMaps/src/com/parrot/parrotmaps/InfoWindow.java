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
package com.parrot.parrotmaps;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class InfoWindow implements Parcelable

{
	public static enum TYPE {
		NORMAL
	}
	
	public static class INFO_WINDOW_ICON {
		public Drawable drawable;
		public String   fileName;
		
		public INFO_WINDOW_ICON(Drawable drawable, String fileName) {
			this.drawable = drawable;
			this.fileName = fileName;
		}
	}
	
	/** Attributes */
	private String            mTitle        = null;
	private String            mThumbnailURL = null;
	private String            mContent      = null;
	private TYPE              mType;
	
	static private HashMap<TYPE, INFO_WINDOW_ICON> HASHMAP_TYPES = new HashMap<TYPE, INFO_WINDOW_ICON>();

	
	/**
	 * Adds an InfoWindow type with its key (type) and its value (Drawable and its file name).
	 * @param key Marker type
	 * @param fileName fileName
	 * @throws IOException
	 */
	static public void ADD_TYPE(TYPE key, String fileName, Context context) throws IOException {
		INFO_WINDOW_ICON value = null;
		value = new INFO_WINDOW_ICON(Drawable.createFromStream(context.getAssets().open(fileName), ""), fileName);
		HASHMAP_TYPES.put(key, value);
	}
	
	/**
	 * Creates a new InfoWindow. 
	 * @param title Title of the InfoWindow
	 * @param type Specifies the Type of InfoWindow
	 * @param thumbnailURL Link to optional Image to draw in InfoWindow
	 * @param content Text Content
	 * @return
	 */
	public InfoWindow(String title, TYPE type, String thumbnailURL, String content) {
		mTitle = title;
		mThumbnailURL = thumbnailURL;
		mContent = content;
		mType = type;

	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getTitle() {
		return mTitle;
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getThumbnailURL() {
		return mThumbnailURL;
	}
	/**
	 * Operation
	 * 
	 * @return String
	 */
	public String getContent() {
		return mContent;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getFileName () {
		return HASHMAP_TYPES.get(mType).fileName;
	}
	
	/**
	 * 
	 * @return
	 */
	public Drawable getDrawable () {
		return HASHMAP_TYPES.get(mType).drawable;
	}

	/**
	 * Returns a JavaScript JSONObject of this InfoWindow in order
	 * to draw it on webView.
	 * Used in webView.drawInfoWindow() function.
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getJSONObject() throws JSONException {
		JSONObject jInfoWindow = new JSONObject();
		StringBuffer content = new StringBuffer("<p>");
		if (getTitle() != null) {
			content.append("<strong>").append(getTitle()).append("</strong>").append("<br/>");
		}
		content.append(getContent().replaceAll("\\n", "<br/>")).append("</p>");
		
		jInfoWindow.put("content", content.toString());
		return jInfoWindow;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer("");
		buf.append("--Begin Marker--\n");
		buf.append("mTitle=").append(mTitle).append("\n");
		buf.append("mThumbnailURL=").append(mThumbnailURL).append("\n");
		buf.append("mContent=").append(mContent).append("\n");
		buf.append("mType=").append(mType).append("\n");
		buf.append("--End Marker--\n");
		return buf.toString();
	}
	
	/* Parcelable implementation. */
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    	out.writeString(mTitle);
    	out.writeString(mThumbnailURL);
    	out.writeString(mContent);
    	out.writeString(mType.name());
    }

    public static final Parcelable.Creator<InfoWindow> CREATOR
            = new Parcelable.Creator<InfoWindow>() {
        public InfoWindow createFromParcel(Parcel in) {
            return new InfoWindow(in);
        }

        public InfoWindow[] newArray(int size) {
            return new InfoWindow[size];
        }
    };
    
    
    private InfoWindow(Parcel in) {
    	mTitle = in.readString();
    	mThumbnailURL = in.readString();
    	mContent = in.readString();
    	String strType = in.readString();
    	TYPE[] types = TYPE.values();
    	mType = null;
    	for (int i=0 ; i<types.length && mType == null ; i++) {
    		if (strType.equals(types[i].name())) {
    			mType = types[i];
    		}
    	}
    }
    /* End of parcelable implementation. */
}
