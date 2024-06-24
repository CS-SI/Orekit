/* Copyright 2002-2024 Luc Maisonobe
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Common intermediate parameters used to estimate measurements where receiver is a ground station.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class GroundReceiverCommonParametersWithDerivatives extends CommonParametersWithDerivatives {

    /** Transform between station and inertial frame. */
    private final FieldTransform<Gradient> offsetToInertialDownlink;

    /** Station position in inertial frame at end of the downlink leg. */
    private final TimeStampedFieldPVCoordinates<Gradient> stationDownlink;

    /** Simple constructor.
    * @param state spacecraft state
    * @param indices derivatives indices map
    * @param offsetToInertialDownlink transform between station and inertial frame
    * @param stationDownlink station position in inertial frame at end of the downlink leg
    * @param tauD downlink delay
    * @param transitState transit state
    * @param transitPV transit position/velocity as a gradient
    */
    public GroundReceiverCommonParametersWithDerivatives(final SpacecraftState state,
                                                         final Map<String, Integer> indices,
                                                         final FieldTransform<Gradient> offsetToInertialDownlink,
                                                         final TimeStampedFieldPVCoordinates<Gradient> stationDownlink,
                                                         final Gradient tauD,
                                                         final SpacecraftState transitState,
                                                         final TimeStampedFieldPVCoordinates<Gradient> transitPV) {
        super(state, indices, tauD, transitState, transitPV);
        this.offsetToInertialDownlink = offsetToInertialDownlink;
        this.stationDownlink          = stationDownlink;
    }

    /** Get transform between station and inertial frame.
     * @return transform between station and inertial frame
     */
    public FieldTransform<Gradient> getOffsetToInertialDownlink() {
        return offsetToInertialDownlink;
    }

    /** Get station position in inertial frame at end of the downlink leg.
     * @return station position in inertial frame at end of the downlink leg
     */
    public TimeStampedFieldPVCoordinates<Gradient> getStationDownlink() {
        return stationDownlink;
    }

}
