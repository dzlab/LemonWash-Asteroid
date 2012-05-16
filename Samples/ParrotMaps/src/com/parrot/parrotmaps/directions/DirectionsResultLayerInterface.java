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

import com.parrot.parrotmaps.LatLng;
import com.parrot.parrotmaps.LayerInterface;

public interface DirectionsResultLayerInterface
 extends LayerInterface

{
	public static enum SEARCH_RESULT {
		OK,
		FAILED,
		FAILED_NETWORK_TIMEOUT,
		FAILED_CANCELED
	}

	/**
     * Operation
     *
     * @param start
     * @param end
     * @throws SEARCH_RESULT 
     */
    abstract public SEARCH_RESULT route ( String start, String end, String language );

	/**
	 * Interrupt the current query for directions.
	 */
    abstract public void interruptRouteQuery ( );
    
    /**
     * Get the route summary.
     * @return The route summary.
     */
    public String getRouteSummary();
    
    /**
     * Get the index of the next direction instruction
     * according to a given position.
     * @param pos The position
     * @return The index of the next direction instruction.
     */
    public int getNextInstruction( LatLng pos );
}

