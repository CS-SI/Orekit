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

import java.util.Map;
import java.util.Optional;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Common intermediate parameters used to estimate measurements where receiver is a ground station.
 * @author Luc Maisonobe
 * @author Tommy Fryer
 * @since 12.0
 */
public class GroundReceiverCommonParametersWithDerivatives {

    /** Spacecraft state. */
    private final SpacecraftState state;

    /** Derivatives indices map. */
    private final Map<String, Integer> indices;

    /** Transform between station and inertial frame at estimation date. */
    private final FieldTransform<Gradient> offsetToInertialEstimationDate;

    /** Station position in inertial frame at estimation date. */
    private final TimeStampedFieldPVCoordinates<Gradient> stationEstimationDate;

    /** Station position in inertial frame at end of the downlink leg (i.e. reception date). */
    private final TimeStampedFieldPVCoordinates<Gradient> stationDownlink;

    /** Downlink delay. */
    private final Gradient tauD;

    /** Station position in inertial frame at beginning of the uplink leg (i.e. transmission date). */
    private final Optional<TimeStampedFieldPVCoordinates<Gradient>> stationUplink;

    /** Uplink delay. */
    private final Optional<Gradient> tauU;

    /** Transit spacecraft state. */
    private final SpacecraftState transitState;

    /** Fielded transit PV. */
    private final TimeStampedFieldPVCoordinates<Gradient> transitPV;

    /** Coordinates of the participants in signal travel order. */
    private final TimeStampedPVCoordinates[] participants;

    /** Constructor for two-way measurement (with uplink data).
     * @param state spacecraft state
     * @param indices derivatives indices map
     * @param offsetToInertialEstimationDate transform between station and inertial frame at estimation date
     * @param stationEstimationDate station position in inertial frame at estimation date
     * @param stationDownlink station position in inertial frame at end of the downlink leg (i.e. reception time)
     * @param tauD downlink delay
     * @param stationUplink station position in inertial frame at beginning of the uplink leg (i.e. transmission time)
     * @param tauU uplink delay
     * @param transitState transit state
     * @param transitPV transit position/velocity as a gradient
     * @param participants Coordinates of the participants in signal travel order
     */
    public GroundReceiverCommonParametersWithDerivatives(final SpacecraftState state,
                                                         final Map<String, Integer> indices,
                                                         final FieldTransform<Gradient> offsetToInertialEstimationDate,
                                                         final TimeStampedFieldPVCoordinates<Gradient> stationEstimationDate,
                                                         final TimeStampedFieldPVCoordinates<Gradient> stationDownlink,
                                                         final Gradient tauD,
                                                         final TimeStampedFieldPVCoordinates<Gradient> stationUplink,
                                                         final Gradient tauU,
                                                         final SpacecraftState transitState,
                                                         final TimeStampedFieldPVCoordinates<Gradient> transitPV,
                                                         final TimeStampedPVCoordinates[] participants) {
        this.state                   = state;
        this.indices                 = indices;
        this.offsetToInertialEstimationDate = offsetToInertialEstimationDate;
        this.stationEstimationDate          = stationEstimationDate;
        this.stationDownlink = stationDownlink;
        this.tauD            = tauD;
        this.stationUplink   = Optional.of(stationUplink);
        this.tauU            = Optional.of(tauU);
        this.transitState    = transitState;
        this.transitPV       = transitPV;
        this.participants    = participants.clone();
    }

    /** Constructor for one-way measurement (without uplink data).
     * @param state spacecraft state
     * @param indices derivatives indices map
     * @param offsetToInertialEstimationDate transform between station and inertial frame at estimation date
     * @param stationEstimationDate station position in inertial frame at estimation date
     * @param stationDownlink station position in inertial frame at end of the downlink leg (i.e. reception time)
     * @param tauD downlink delay
     * @param transitState transit state
     * @param transitPV transit position/velocity as a gradient
     * @param participants Coordinates of the participants in signal travel order
     */
    public GroundReceiverCommonParametersWithDerivatives(final SpacecraftState state,
                                                         final Map<String, Integer> indices,
                                                         final FieldTransform<Gradient> offsetToInertialEstimationDate,
                                                         final TimeStampedFieldPVCoordinates<Gradient> stationEstimationDate,
                                                         final TimeStampedFieldPVCoordinates<Gradient> stationDownlink,
                                                         final Gradient tauD,
                                                         final SpacecraftState transitState,
                                                         final TimeStampedFieldPVCoordinates<Gradient> transitPV,
                                                         final TimeStampedPVCoordinates[] participants) {
        this.state                   = state;
        this.indices                 = indices;
        this.offsetToInertialEstimationDate = offsetToInertialEstimationDate;
        this.stationEstimationDate          = stationEstimationDate;
        this.stationDownlink = stationDownlink;
        this.tauD            = tauD;
        this.stationUplink   = Optional.ofNullable(null);
        this.tauU            = Optional.ofNullable(null);
        this.transitState    = transitState;
        this.transitPV       = transitPV;
        this.participants    = participants.clone();
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

    /** Get transform between station and inertial frame.
     * @return transform between station and inertial frame
     */
    public FieldTransform<Gradient> getOffsetToInertialEstimationDate() {
        return offsetToInertialEstimationDate;
    }

    /** Get station position in inertial frame at end of the downlink leg.
     * @return station position in inertial frame at end of the downlink leg
     */
    public TimeStampedFieldPVCoordinates<Gradient> getStationDownlink() {
        return stationDownlink;
    }

    /** Get downlink delay.
     * @return ownlink delay
     */
    public Gradient getTauD() {
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
    public TimeStampedFieldPVCoordinates<Gradient> getTransitPV() {
        return transitPV;
    }

    /** Getter for the stationEstimationDate.
     * @return the stationEstimationDate
     */
    public TimeStampedFieldPVCoordinates<Gradient> getStationEstimationDate() {
        return stationEstimationDate;
    }

    /** Getter for the stationUplink.
     * @return the stationUplink
     */
    public TimeStampedFieldPVCoordinates<Gradient> getStationUplink() {
        return stationUplink.get();
    }

    /** Getter for the tauU.
     * @return the tauU
     */
    public Gradient getTauU() {
        return tauU.get();
    }

    /** Getter for the participants.
     * @return the participants
     */
    public TimeStampedPVCoordinates[] getParticipants() {
        return participants.clone();
    }
}
