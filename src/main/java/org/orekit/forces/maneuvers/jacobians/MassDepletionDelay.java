/* Copyright 2002-2025 CS GROUP
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
package org.orekit.forces.maneuvers.jacobians;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.ForceModel;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.time.AbsoluteDate;

import java.util.Arrays;
import java.util.List;

/** Generator for effect of delaying mass depletion when delaying a maneuver,
 *  when the mass itself is not included in the transition matrix.
 * It neglects the influence of mass in other force models e.g. drag.
 * For more accurate derivatives, one should use the full 7x7 state transition matrix instead.
 * @author Luc Maisonobe
 * @since 11.1
 */
public class MassDepletionDelay implements AdditionalDerivativesProvider {

    /** Prefix for state name. */
    public static final String PREFIX = "Orekit-depletion-";

    /** Name of the mass depletion additional state. */
    private final String depletionName;

    /** Start/stop management flag. */
    private final boolean manageStart;

    /** Maneuver that is delayed. */
    private final Maneuver maneuver;

    /** Indicator for forward propagation. */
    private boolean forward;

    /** List of non-gravitational forces, used only if mass is not in STM. */
    private final List<ForceModel> nonGravitationalForces;

    /** Constructor.
     * <p>
     * The generated additional state and derivatives will be named by prepending
     * the {@link #PREFIX} to the name of the date trigger parameter.
     * </p>
     * @param triggerName name of the date trigger parameter
     * @param manageStart if true, we compute derivatives with respect to maneuver start
     * @param maneuver maneuver that is delayed
     * @param nonGravitationalForces list of non-gravitational forces, used only if mass is not in STM.
     *                               They are assumed to be inversely depending on mass.
     */
    public MassDepletionDelay(final String triggerName, final boolean manageStart, final Maneuver maneuver,
                              final ForceModel... nonGravitationalForces) {
        this.depletionName = PREFIX + triggerName;
        this.manageStart   = manageStart;
        this.maneuver      = maneuver;
        this.nonGravitationalForces = Arrays.asList(nonGravitationalForces);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return depletionName;
    }

    /** Get the dimension of the generated column.
     * @return dimension of the generated column
     */
    public int getDimension() {
        return 6;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        forward = target.isAfterOrEqualTo(initialState);
    }

    /** {@inheritDoc} */
    @Override
    public CombinedDerivatives combinedDerivatives(final SpacecraftState state) {

        // retrieve current Jacobian column
        final double[] p = state.getAdditionalState(getName());
        final double[] pDot = new double[getDimension()];

        if (forward == manageStart) {

            // current acceleration
            final double[] parameters   = maneuver.getParameters(state.getDate());
            // for the acceleration method we need all the span values of all the parameters driver
            // as in the acceleration method an exctractParameter method is called
            Vector3D acceleration = maneuver.acceleration(state, parameters);
            for (final ForceModel forceModel: nonGravitationalForces) {
                acceleration = acceleration.add(forceModel.acceleration(state, forceModel.getParameters(state.getDate())));
            }

            // it is assumed the non-gravitational accelerations are inversely proportional to the mass
            final double m      = state.getMass();
            final double ratio  = -1. / m;
            pDot[0] = p[3];
            pDot[1] = p[4];
            pDot[2] = p[5];
            pDot[3] = ratio * acceleration.getX();
            pDot[4] = ratio * acceleration.getY();
            pDot[5] = ratio * acceleration.getZ();

        }

        return new CombinedDerivatives(pDot, null);

    }

}

