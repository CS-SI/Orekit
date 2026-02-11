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

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.clocks.FieldClockOffset;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.Map;

/** Common intermediate parameters used to estimate measurements where receiver is a ground station.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class CommonParametersWithDerivatives {

    /** Spacecraft state. */
    private final SpacecraftState state;

    /** Derivatives indices map. */
    private final Map<String, Integer> indices;

    /** Downlink delay. */
    private final Gradient tauD;

    /** Clock offset of measured satellite. */
    private final FieldClockOffset<Gradient> localOffset;

    /** Clock offset of remote observer. */
    private final FieldClockOffset<Gradient> remoteOffset;

    /** Transit state. */
    private final SpacecraftState transitState;

    /** Transit state of measured/local satellite. */
    private final TimeStampedFieldPVCoordinates<Gradient> transitPV;

    /** State of remote observer. */
    private final TimeStampedFieldPVCoordinates<Gradient> remotePV;

    /** Simple constructor.
    * @param state spacecraft state
    * @param indices derivatives indices map
    * @param tauD downlink delay
    * @param localOffset measured satellite clock offset
    * @param remoteOffset clock offset of remote observer
    * @param transitState transit state of measured satellite
    * @param transitPV transit position/velocity as a gradient
    * @param remotePV position/velocity of remote observer as a gradient
    */
    public CommonParametersWithDerivatives(final SpacecraftState state,
                                           final Map<String, Integer> indices,
                                           final Gradient tauD,
                                           final FieldClockOffset<Gradient> localOffset,
                                           final FieldClockOffset<Gradient> remoteOffset,
                                           final SpacecraftState transitState,
                                           final TimeStampedFieldPVCoordinates<Gradient> transitPV,
                                           final TimeStampedFieldPVCoordinates<Gradient> remotePV) {
        this.state        = state;
        this.indices      = indices;
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

    /** Get derivatives indices map.
     * @return derivatives indices map
     */
    public Map<String, Integer> getIndices() {
        return indices;
    }

    /** Get downlink delay.
     * @return ownlink delay
     */
    public Gradient getTauD() {
        return tauD;
    }

    /** Get local clock offset.
     * @return clock offset of measured satellite
     */
    public FieldClockOffset<Gradient> getLocalOffset() {
        return localOffset;
    }

    /** Get remote clock offset.
     * @return clock offset of remote observer
     */
    public FieldClockOffset<Gradient> getRemoteOffset() {
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
    public TimeStampedFieldPVCoordinates<Gradient> getTransitPV() {
        return transitPV;
    }

    /** Get remote observer position/velocity.
     * @return remote observer position/velocity
     */
    public TimeStampedFieldPVCoordinates<Gradient> getRemotePV() {
        return remotePV;
    }

}
