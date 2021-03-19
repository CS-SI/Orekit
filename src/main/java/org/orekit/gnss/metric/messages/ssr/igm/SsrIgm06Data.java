/* Copyright 2002-2021 CS GROUP
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
package org.orekit.gnss.metric.messages.ssr.igm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for SSR IGM06 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm06Data extends SsrIgmData {

    /** Number of biases processed for the current satellite. */
    private int numberOfBiasesProcessed;

    /** Yaw angle used for computation of phase wind-up correction [rad]. */
    private double yawAngle;

    /** Yaw rate [rad/s]. */
    private double yawRate;

    /** Map of phase biases.
     * First key: the signal ID
     * Second key: the phase bias object
     */
    private Map<Integer, PhaseBias> biases;

    /** Constructor. */
    public SsrIgm06Data() {
        // Initialize an empty map
        this.biases = new HashMap<>();
    }

    /**
     * Get the number of biases processed for the current satellite.
     * @return the number of biases processed
     */
    public int getNumberOfBiasesProcessed() {
        return numberOfBiasesProcessed;
    }

    /**
     * Set the number of biases processed for the current satellite.
     * @param numberOfBiasesProcessed the number to set
     */
    public void setNumberOfBiasesProcessed(final int numberOfBiasesProcessed) {
        this.numberOfBiasesProcessed = numberOfBiasesProcessed;
    }

    /**
     * Get the yaw angle used for computation of phase wind-up correction.
     * @return the yaw angle in radians
     */
    public double getYawAngle() {
        return yawAngle;
    }

    /**
     * Set the yaw angle used for computation of phase wind-up correction.
     * @param yawAngle the yaw angle to set in radians
     */
    public void setYawAngle(final double yawAngle) {
        this.yawAngle = yawAngle;
    }

    /**
     * Get the yaw rate.
     * @return the yaw rate in radians per second
     */
    public double getYawRate() {
        return yawRate;
    }

    /**
     * Set the yaw rate.
     * @param yawRate the yaw rate to set in radians per second
     */
    public void setYawRate(final double yawRate) {
        this.yawRate = yawRate;
    }

    /**
     * Add a phase bias value for the current satellite.
     * @param bias the phase bias to add
     */
    public void addPhaseBias(final PhaseBias bias) {
        this.biases.put(bias.getSignalID(), bias);
    }

    /**
     * Get the phase biases for the current satellite.
     * <p>
     * First key: signal ID
     * Second key: the phase bias object
     * </p>
     * @return the phase biases for the current satellite
     */
    public Map<Integer, PhaseBias> getPhaseBiases() {
        return Collections.unmodifiableMap(biases);
    }

    /**
     * Get the phase bias for a given signal ID.
     * @param signalID the signal IF
     * @return the corresponding phase bias (null if not provided)
     */
    public PhaseBias getPhaseBias(final int signalID) {
        return biases.get(signalID);
    }

    /** Container for phase bias data. */
    public static class PhaseBias {

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

}
