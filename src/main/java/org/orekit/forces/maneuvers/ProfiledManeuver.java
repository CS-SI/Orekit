/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.orekit.attitudes.AttitudeRotationModel;
import org.orekit.forces.maneuvers.propulsion.ProfileThrustPropulsionModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.List;


/** A generic model for maneuvers with finite-valued acceleration magnitude, as opposed to instantaneous changes
 * in the velocity vector which are defined via detectors (in {@link ImpulseManeuver} and
 * {@link FieldImpulseManeuver}).
 * It contains:
 *  - An attitude override, this is the attitude used during the maneuver, it can be different from the one
 *    used for propagation;
 *  - A propulsion model based on a time profile. It defines the thrust or ΔV, isp, flow rate etc.
 * The difference with {@link TriggeredManeuver} is that firings are not "discovered" during propagation,
 * but known in advance as purely time-dependent.
 * @author Romain Serra
 * @since 13.0
 */
public class ProfiledManeuver extends Maneuver {

    /** Generic maneuver constructor.
     * @param attitudeOverride attitude provider for the attitude during the maneuver, if set
     * @param propulsionModel propulsion model
     */
    public ProfiledManeuver(final AttitudeRotationModel attitudeOverride,
                            final ProfileThrustPropulsionModel propulsionModel) {
        super(attitudeOverride, propulsionModel);
    }

    /** {@inheritDoc} */
    @Override
    public ProfileThrustPropulsionModel getPropulsionModel() {
        return (ProfileThrustPropulsionModel) super.getPropulsionModel();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        return getPropulsionModel().getActiveSegment(date) != null;
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> boolean isFiring(final FieldAbsoluteDate<T> date,
                                                                   final T[] parameters) {
        return getPropulsionModel().getActiveSegment(date.toAbsoluteDate()) != null;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // Prepare final drivers' array
        final List<ParameterDriver> drivers = new ArrayList<>();

        // Convention: Propulsion drivers are given before maneuver triggers drivers
        // Add propulsion drivers first
        drivers.addAll(0, getPropulsionModel().getParametersDrivers());

        // Then attitude override' drivers if defined
        if (getAttitudeOverride() != null) {
            drivers.addAll(getAttitudeOverride().getParametersDrivers());
        }

        // Return full drivers' array
        return drivers;
    }
}
