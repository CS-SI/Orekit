/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.gnss.metric.messages.common;


/**
 * Container for phase bias data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class PhaseBias {

    /** GNSS signal and tracking mode identifier. */
    private final int signalID;

    /** Signal integer property. */
    private final boolean isSignalInteger;

    /** Signal Wide-Lane integer indicator. */
    private final int signalWideLaneIntegerIndicator;

    /** Signal discontinuity counter. */
    private final int discontinuityCounter;

    /** Phase bias for the corresponding signal identifier. */
    private final double phaseBias;

    /**
     * Constructor.
     * @param signalID GNSS signal and tracking mode identifier
     * @param isSignalInteger true if signal has integer property
     * @param signalWideLaneIntegerIndicator signal Wide-Lane integer indicator
     * @param discontinuityCounter signal discontinuity counter
     * @param phaseBias phase bias associated to the signal ID in meters
     */
    public PhaseBias(final int signalID, final boolean isSignalInteger,
                     final int signalWideLaneIntegerIndicator,
                     final int discontinuityCounter, final double phaseBias) {
        // Initialize fields
        this.signalID                       = signalID;
        this.isSignalInteger                = isSignalInteger;
        this.signalWideLaneIntegerIndicator = signalWideLaneIntegerIndicator;
        this.discontinuityCounter           = discontinuityCounter;
        this.phaseBias                      = phaseBias;
    }

    /**
     * Get the GNSS signal and tracking mode identifier.
     * @return the GNSS signal and tracking mode identifier
     */
    public int getSignalID() {
        return signalID;
    }

    /**
     * Get the flag indicating is signal has integer property.
     * @return true is signal has integer property
     */
    public boolean isSignalInteger() {
        return isSignalInteger;
    }

    /**
     * Get the signal Wide-Lane integer indicator.
     * <ul>
     *   <li>0: No wide-lane with integer property for this signal or satellite</li>
     *   <li>1: Signal belongs to group two of wide-lanes with integer property</li>
     *   <li>2: Signal belongs to group one of wide-lanes with integer property</li>
     *   <li>3: Signal belongs to group one of wide-lanes with integer property</li>
     * </ul>
     * @return the signal Wide-Lane indicator
     */
    public int getSignalWideLaneIntegerIndicator() {
        return signalWideLaneIntegerIndicator;
    }

    /**
     * Get the signal phase discontinuity counter.
     * <p>
     * Increased for every discontinuity in phase
     * </p>
     * @return the signal phase discontinuity counter
     */
    public int getDiscontinuityCounter() {
        return discontinuityCounter;
    }

    /**
     * Get the phase bias associated to the signal ID.
     * @return the phase bias in meters
     */
    public double getPhaseBias() {
        return phaseBias;
    }

}
