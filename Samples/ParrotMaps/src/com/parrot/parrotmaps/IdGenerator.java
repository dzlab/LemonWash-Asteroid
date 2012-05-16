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

import com.parrot.parrotmaps.log.PLog;

public class IdGenerator

{
	/** Attributes */
	private static final String TAG            = "IdGenerator";
	private static       long   MARKER_COUNT   = 0;
	private static       long   POLYLINE_COUNT = 0;
	private static       long   LAYER_COUNT    = 0;
	
	/**
	 * Operation
	 * 
	 * @return
	 */
	private IdGenerator() {
	}
	/**
	 * Operation
	 * 
	 * @param type
	 * @return int
	 * @throws Exception 
	 */
	public static long nextMarkerId() throws Exception {
		if (MARKER_COUNT < Long.MAX_VALUE) {
			MARKER_COUNT++;
		}
		else {
			PLog.e(TAG, "IdGenerator overflow (MARKER_COUNT).");
			throw (new Exception("IdGenerator overflow (MARKER_COUNT)."));
		}
		return MARKER_COUNT;
	}
	/**
	 * Operation
	 * 
	 * @param type
	 * @return int
	 */
	public static long nextPolylineId() throws Exception {
		if (POLYLINE_COUNT < Long.MAX_VALUE) {
			POLYLINE_COUNT++;
		}
		else {
			PLog.e(TAG, "IdGenerator overflow (POLYLINE_COUNT).");
			throw (new Exception("IdGenerator overflow (POLYLINE_COUNT)."));
		}
		return POLYLINE_COUNT;
	}
	/**
	 * Operation
	 * 
	 * @param type
	 * @return int
	 */
	public static long nextLayerId() throws Exception {
		if (LAYER_COUNT < Long.MAX_VALUE) {
			LAYER_COUNT++;
		}
		else {
			PLog.e(TAG, "IdGenerator overflow (LAYER_COUNT).");
			throw (new Exception("IdGenerator overflow (LAYER_COUNT)."));
		}
		return LAYER_COUNT;
	}
}
