/* Copyright 2022-2025 Romain Serra
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
package org.orekit.forces.maneuvers;

import org.orekit.attitudes.AttitudeProvider;

/** Abstract class for impulsive maneuvers.
 * @author Romain Serra
 * @since 13.0
 */
public abstract class AbstractImpulseManeuver {

    /** The attitude to override during the maneuver, if set. */
    private final AttitudeProvider attitudeOverride;

    /** Type of norm linking delta-V to mass consumption. */
    private final Control3DVectorCostType control3DVectorCostType;

    /** Protected constructor.
     * @param attitudeOverride the attitude provider to use for the maneuver
     * @param control3DVectorCostType increment's norm for mass consumption
     */
    protected AbstractImpulseManeuver(final AttitudeProvider attitudeOverride,
                                      final Control3DVectorCostType control3DVectorCostType) {
        this.attitudeOverride = attitudeOverride;
        this.control3DVectorCostType = control3DVectorCostType;
    }

    /** Get the control vector's cost type.
     * @return control cost type
     */
    public Control3DVectorCostType getControl3DVectorCostType() {
        return control3DVectorCostType;
    }

    /**
     * Get the Attitude Provider to use during maneuver.
     * @return the attitude provider
     */
    public AttitudeProvider getAttitudeOverride() {
        return attitudeOverride;
    }
}
