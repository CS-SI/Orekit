/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.orbits;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.orekit.utils.ParameterDriver;

/** Factory for orbits.
 * @param <P> type of the orbits
 * @since 14.0
 */
public abstract class AbstractOrbitFactory<P extends Orbit> extends AbstractOrbitalParameterFactory<P> {

    /**
     * Simple constructor.
     * <p>
     * The template orbit is used as a model. It defines the inertial frame, the central attraction coefficient, the
     * orbit type, and is also used together with the {@code positionScale} to convert from the
     * {@link ParameterDriver#setNormalizedValue(double) normalized} parameters used by the callers of this factory to
     * the real orbital parameters.
     * </p>
     *
     * @param positionScale     position scale used to scale the orbital drivers
     * @param template          template orbit
     * @param positionAngleType position angle type to use
     */
    protected AbstractOrbitFactory(final double positionScale, final P template,
                                   final PositionAngleType positionAngleType) {
        super(template.getType(),
              template.getType().getDrivers(positionScale, template, positionAngleType),
              template.getFrame(), positionAngleType, template.getDate(), template.getMu());
    }

    /** {@inheritDoc} */
    @Override
    protected double[] toArray(final Orbit orbit) {

        // fix both frame and type
        final Orbit partiallyConverted = orbit.getFrame() == getFrame() ? orbit : orbit.inFrame(getFrame());
        final Orbit fullyConverted     = getOrbitParamsType().convertType(partiallyConverted);

        // retrieve orbital parameters
        final double[] stateVector = new double[6];
        getOrbitParamsType().mapOrbitToArray(fullyConverted, getPositionAngleType(), stateVector, null);

        return stateVector;

    }

    /** {@inheritDoc}
     * <p>
     * The orbital parameters being a regular {@link OrbitParamsType orbit type}, the Jacobian is
     * the closed-form one provided by {@link Orbit#getJacobianWrtParameters(PositionAngleType, double[][])}.
     * </p>
     */
    @Override
    public RealMatrix getJacobianWrtParameters() {
        final double[][] jacobian = new double[6][6];
        createFromDrivers().getJacobianWrtParameters(getPositionAngleType(), jacobian);
        return new Array2DRowRealMatrix(jacobian, false);
    }

    /** {@inheritDoc}
     * <p>
     * Pure delegation to {@link Orbit#getJacobianWrtCartesian(PositionAngleType, double[][])}
     * </p>
     */
    @Override
    public RealMatrix getJacobianWrtCartesian() {
        final double[][] jacobian = new double[6][6];
        createFromDrivers().getJacobianWrtCartesian(getPositionAngleType(), jacobian);
        return new Array2DRowRealMatrix(jacobian, false);
    }

}
