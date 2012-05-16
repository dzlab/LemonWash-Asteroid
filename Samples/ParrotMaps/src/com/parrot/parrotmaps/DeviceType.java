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

import android.content.Context;
import android.content.res.Configuration;

/**
 * Utilities to know the kind of device, on which the application is running:
 * advanced (with touch screen) or simple (no touch screen). 
 * @author FL
 */
public class DeviceType
{
	/**
	 * Indicate if the current device is an advanced device.
	 * Advanced device means device with touch screen.
	 * @param ctx Application context.
	 * @return true if this is an advanced device, false otherwise.
	 */
	public static boolean isAdvancedDevice( Context ctx )
	{
		return ctx.getResources().getConfiguration().touchscreen != Configuration.TOUCHSCREEN_NOTOUCH;
	}

	/**
	 * Indicate if the current device is a simple device.
	 * Simple device means device with no touch screen.
	 * @param ctx Application context.
	 * @return true if this is a simple device, false otherwise.
	 */
	public static boolean isSimpleDevice( Context ctx )
	{
		return ctx.getResources().getConfiguration().touchscreen == Configuration.TOUCHSCREEN_NOTOUCH;
	}
}
