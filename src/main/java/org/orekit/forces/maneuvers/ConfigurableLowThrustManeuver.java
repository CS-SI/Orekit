/* Copyright 2020 Exotrail
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Exotrail licenses this file to You under the Apache License, Version 2.0
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.maneuvers.propulsion.AbstractConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.ThrustDirectionAndAttitudeProvider;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.time.AbsoluteDate;

/**
 * This class implements a configurable low thrust maneuver.
 * <p>
 * The maneuver is composed of succession of a burn interval. Burn intervals are
 * defined by two detectors. See
 * {@link org.orekit.forces.maneuvers.trigger.StartStopEventsTrigger
 * StartStopEventsTrigger} for more details on the detectors. The attitude
 * and the thrust direction are provided by an instance of
 * ThrustDirectionProvider See
 * {@link org.orekit.forces.maneuvers.propulsion.ThrustDirectionAndAttitudeProvider
 * ThrustDirectionProvider} for more details on thrust direction and attitude.
 * @author Mikael Fillastre
 * @author Andrea Fiorentino
 * @since 10.2
 */

public class ConfigurableLowThrustManeuver extends Maneuver {

    /** To be used for ParameterDriver to make thrust non constant. */
    private static String THRUST_MODEL_IDENTIFIER = "ConfigurableLowThrustManeuver";

    /** Thrust direction and spacecraft attitude provided by an external object. */
    private final ThrustDirectionAndAttitudeProvider thrustDirectionProvider;

    /**
     * Constructor.
     * <p>
     * See {@link org.orekit.forces.maneuvers.trigger.StartStopEventsTrigger
     * StartStopEventsTrigger} for requirements on detectors
     * </p>
     * @param thrustDirectionProvider thrust direction and attitude provider
     * @param trigger                 maneuver triggers
     * @param thrust                  the thrust force (N)
     * @param isp                     engine specific impulse (s)
     * @since 11.1
     */
    public ConfigurableLowThrustManeuver(final ThrustDirectionAndAttitudeProvider thrustDirectionProvider,
                                         final ManeuverTriggers trigger,
                                         final double thrust, final double isp) {
        super(thrustDirectionProvider.getManeuverAttitudeProvider(),
              trigger,
              buildBasicConstantThrustPropulsionModel(thrust, isp,
                                                      thrustDirectionProvider.getThrusterAxisInSatelliteFrame()));
        this.thrustDirectionProvider = thrustDirectionProvider;

    }

    /**
     * Build a BasicConstantThrustPropulsionModel from thruster characteristics.
     * @param thrust                       the thrust force (N)
     * @param isp                          engine specific impulse (s)
     * @param thrusterAxisInSatelliteFrame direction in spacecraft frame
     * @return new instance of BasicConstantThrustPropulsionModel
     */
    private static BasicConstantThrustPropulsionModel buildBasicConstantThrustPropulsionModel(final double thrust,
            final double isp, final Vector3D thrusterAxisInSatelliteFrame) {
        return new BasicConstantThrustPropulsionModel(thrust, isp, thrusterAxisInSatelliteFrame,
                THRUST_MODEL_IDENTIFIER);
    }

    /**
     * Getter on Thrust direction and spacecraft attitude provided by an external
     * object.
     * @return internal field
     */
    public ThrustDirectionAndAttitudeProvider getThrustDirectionProvider() {
        return thrustDirectionProvider;
    }

    /**
     * Get the thrust magnitude.
     * @param date at which the Thrust wants to be known
     * @return thrust force (N).
     */
    public double getThrustMagnitude(final AbsoluteDate date) {
        return ((AbstractConstantThrustPropulsionModel) getPropulsionModel()).getThrustVector(date).getNorm();
    }

    /**
     * Get the thrust magnitude.
     * @return thrust force (N). Will throw
     * an exception if the Thrust driver has several
     * values driven
     */
    public double getThrustMagnitude() {
        return ((AbstractConstantThrustPropulsionModel) getPropulsionModel()).getThrustVector().getNorm();
    }

    /**
     * Get the specific impulse.
     * @param date at which the ISP wants to be known
     * @return specific impulse (s).
     */
    public double getIsp(final AbsoluteDate date) {
        return ((AbstractConstantThrustPropulsionModel) getPropulsionModel()).getIsp(date);
    }

    /**
     * Get the specific impulse.
     * @return specific impulse (s). Will throw
     * an exception if the Thrust driver has several
     * values driven
     */
    public double getIsp() {
        return ((AbstractConstantThrustPropulsionModel) getPropulsionModel()).getIsp();
    }

}
