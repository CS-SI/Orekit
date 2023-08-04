/* Copyright 2023 Luc Maisonobe
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
package org.orekit.estimation.measurements;

import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Common intermediate parameters used to estimate measurements where receiver is a ground station.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class GroundReceiverCommonParametersWithoutDerivatives {

    /** Spacecraft state. */
    private final SpacecraftState state;

    /** Transform between station and inertial frame. */
    private final Transform offsetToInertialDownlink;

    /** Station position in inertial frame at end of the downlink leg. */
    private final TimeStampedPVCoordinates stationDownlink;

    /** Downlink delay. */
    private final double tauD;

    /** Transit state. */
    private final SpacecraftState transitState;

    /** Transit position/velocity. */
    private final TimeStampedPVCoordinates transitPV;

    /** Simple constructor.
    * @param state spacecraft state
    * @param offsetToInertialDownlink transform between station and inertial frame
    * @param stationDownlink station position in inertial frame at end of the downlink leg
    * @param tauD downlink delay
    * @param transitState transit state
    * @param transitPV transit position/velocity
    */
    public GroundReceiverCommonParametersWithoutDerivatives(final SpacecraftState state,
                                                            final Transform offsetToInertialDownlink,
                                                            final TimeStampedPVCoordinates stationDownlink,
                                                            final double tauD,
                                                            final SpacecraftState transitState,
                                                            final TimeStampedPVCoordinates transitPV) {
        this.state                    = state;
        this.offsetToInertialDownlink = offsetToInertialDownlink;
        this.stationDownlink          = stationDownlink;
        this.tauD                     = tauD;
        this.transitState             = transitState;
        this.transitPV                = transitPV;
    }

    /** Get spacecraft state.
     * @return spacecraft state
     */
    public SpacecraftState getState() {
        return state;
    }

    /** Get transform between station and inertial frame.
     * @return transform between station and inertial frame
     */
    public Transform getOffsetToInertialDownlink() {
        return offsetToInertialDownlink;
    }

    /** Get station position in inertial frame at end of the downlink leg.
     * @return station position in inertial frame at end of the downlink leg
     */
    public TimeStampedPVCoordinates getStationDownlink() {
        return stationDownlink;
    }

    /** Get downlink delay.
     * @return ownlink delay
     */
    public double getTauD() {
        return tauD;
    }

    /** Get transit state.
     * @return transit state
     */
    public SpacecraftState getTransitState() {
        return transitState;
    }

    /** Get transit position/velocity.
     * @return transit position/velocity
     */
    public TimeStampedPVCoordinates getTransitPV() {
        return transitPV;
    }

}
