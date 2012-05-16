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
package com.parrot.parrotmaps.directions;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

import com.parrot.parrotmaps.R;

/**
 * AutoCompleteTextView with special behavior for "my location" selection. When
 * it is in "my location" mode, the text is set to a special character string
 * and the whole text is selected.
 * 
 * @author FL
 * 
 */
public class DirectionsTextEdit extends AutoCompleteTextView {
	private boolean mIsMyLocation = false;

	public DirectionsTextEdit(Context context) {
		super(context);
	}

	public DirectionsTextEdit(Context context, AttributeSet att) {
		super(context, att);
	}

	/**
	 * Indicate if the current address source is "my location" or not.
	 * 
	 * @return true if in "my location" mode, false otherwise.
	 */
	public boolean getIsMyLocation() {
		return mIsMyLocation;
	}

	/**
	 * Activate or desactivate the "my location" mode.
	 * 
	 * @param isMyLocation
	 *            "my location" mode activation.
	 */
	public void setIsMyLocation(boolean isMyLocation) {
		mIsMyLocation = isMyLocation;
		if (mIsMyLocation) {
			setText(getResources().getText(
					R.string.direction_address_mylocation));
			setTextColor(Color.BLUE);
			selectAll();
			setSelectAllOnFocus(true);
		} else {
			setTextColor(Color.BLACK);
			setSelectAllOnFocus(false);
		}
	}
}
