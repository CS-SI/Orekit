/* Copyright 2022-2026 Luc Maisonobe
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

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Common intermediate parameters used to estimate measurements.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class CommonParametersWithoutDerivatives {

    /** Spacecraft state. */
    private final SpacecraftState state;

    /** Downlink delay. */
    private final double tauD;

    /** Clock offset of measured satellite. */
    private final ClockOffset localOffset;

    /** Clock offset of remote observer. */
    private final ClockOffset remoteOffset;

    /** Transit state. */
    private final SpacecraftState transitState;

    /** Transit position/velocity. */
    private final TimeStampedPVCoordinates transitPV;

    /** Position/velocity of remote observer. */
    private final TimeStampedPVCoordinates remotePV;

    /** Simple constructor.
    * @param state spacecraft state
    * @param tauD downlink delay
    * @param localOffset measured satellite clock offset
    * @param remoteOffset clock offset of remote observer
    * @param transitState transit state of measured satellite
    * @param transitPV transit position/velocity
    * @param remotePV position/velocity of remote observer
    */
    public CommonParametersWithoutDerivatives(final SpacecraftState state, final double tauD,
                                              final ClockOffset localOffset, final ClockOffset remoteOffset,
                                              final SpacecraftState transitState,
                                              final TimeStampedPVCoordinates transitPV,
                                              final TimeStampedPVCoordinates remotePV) {
        this.state        = state;
        this.tauD         = tauD;
        this.localOffset  = localOffset;
        this.remoteOffset = remoteOffset;
        this.transitState = transitState;
        this.transitPV    = transitPV;
        this.remotePV     = remotePV;
    }

    /** Get spacecraft state.
     * @return spacecraft state
     */
    public SpacecraftState getState() {
        return state;
    }

    /** Get downlink delay.
     * @return ownlink delay
     */
    public double getTauD() {
        return tauD;
    }

    /** Get local clock offset.
     * @return clock offset of measured satellite
     */
    public ClockOffset getLocalOffset() {
        return localOffset;
    }

    /** Get remote clock offset.
     * @return clock offset of remote observer
     */
    public ClockOffset getRemoteOffset() {
        return remoteOffset;
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

    /** Get remote observer position/velocity.
     * @return remote observer position/velocity
     */
    public TimeStampedPVCoordinates getRemotePV() {
        return remotePV;
    }

}
