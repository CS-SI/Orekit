/* Copyright 2022-2026 Romain Serra
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
package org.orekit.control.heuristics;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Derivative;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.analysis.solvers.NewtonRaphsonSolver;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.osc2mean.OsculatingToMeanConverter;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Class modelling impulsive maneuvers to set the mean semi-major axis to a given value.
 * The impulse vector is tangential and computed in the same frame as the orbit.
 * The resulting osculating eccentricity depends on the execution location. The instantaneous orbital plane is left unchanged.
 * A constraint on the maximum magnitude can be optionally set.
 * @see OsculatingSmaChangeImpulseProvider
 * @see OsculatingToMeanConverter
 * @author Romain Serra
 * @since 14.0
 */
public class MeanSmaChangeImpulseProvider extends AbstractInPlaneImpulseProvider {

    /** Target osculating semi-major axis. */
    private final double targetSemiMajorAxis;

    /** Mean orbit converter. */
    private final OsculatingToMeanConverter osculatingToMeanConverter;

    /**
     * Constructor with default maximum magnitude set to positive infinity (unconstrained).
     * @param targetSemiMajorAxis osculating value to achieve
     * @param osculatingToMeanConverter mean orbit converter
     */
    public MeanSmaChangeImpulseProvider(final double targetSemiMajorAxis, final OsculatingToMeanConverter osculatingToMeanConverter) {
        this(Double.POSITIVE_INFINITY, targetSemiMajorAxis, osculatingToMeanConverter);
    }

    /**
     * Constructor.
     * @param maximumMagnitude maximum magnitude
     * @param targetSemiMajorAxis osculating value to achieve
     * @param osculatingToMeanConverter mean orbit converter
     */
    public MeanSmaChangeImpulseProvider(final double maximumMagnitude, final double targetSemiMajorAxis,
                                        final OsculatingToMeanConverter osculatingToMeanConverter) {
        super(maximumMagnitude);
        this.targetSemiMajorAxis = targetSemiMajorAxis;
        this.osculatingToMeanConverter = osculatingToMeanConverter;
    }

    @Override
    public Vector3D getUnconstrainedImpulse(final SpacecraftState state, final boolean isForward) {
        final Orbit orbit = state.getOrbit();
        final Vector3D velocity = state.getVelocity();
        final OsculatingSmaChangeImpulseProvider osculatingSmaChangeImpulseProvider = new OsculatingSmaChangeImpulseProvider(getMaximumMagnitude(),
                targetSemiMajorAxis);
        final Vector3D osculatingImpulse = osculatingSmaChangeImpulseProvider.getUnconstrainedImpulse(state, isForward);
        final Vector3D direction = velocity.normalize();
        final NewtonRaphsonSolver solver = new NewtonRaphsonSolver();
        final NewtonFunction function = new NewtonFunction(orbit, direction, osculatingToMeanConverter,
                targetSemiMajorAxis);
        final double guess = osculatingImpulse.add(velocity).getNorm2();
        final double optimal = solver.solve(100, function, guess);
        return direction.scalarMultiply(optimal).subtract(velocity);
    }

    /**
     * Function for Newton-Raphson method.
     */
    private static class NewtonFunction implements UnivariateDifferentiableFunction {

        /** Reference orbit whose velocity is to be changed. */
        private final Orbit templateOrbit;

        /** Direction of velocity. */
        private final Vector3D direction;

        /** Mean orbit converter. */
        private final OsculatingToMeanConverter converter;

        /** Target mean semi-major axis. */
        private final double targetMeanSma;

        /**
         * Constructor.
         * @param templateOrbit template orbit
         * @param direction direction of velocity
         * @param converter osculating-to-mean converter
         * @param targetMeanSma target semi-major axis
         */
        NewtonFunction(final Orbit templateOrbit, final Vector3D direction, final OsculatingToMeanConverter converter,
                       final double targetMeanSma) {
            this.templateOrbit = templateOrbit;
            this.direction = direction;
            this.converter = converter;
            this.targetMeanSma = targetMeanSma;
        }

        @Override
        public <T extends Derivative<T>> T value(final T t) throws MathIllegalArgumentException {
            final Field<T> field = t.getField();
            final FieldVector3D<T> velocity = new FieldVector3D<>(t, direction);
            final FieldVector3D<T> position = new FieldVector3D<>(field, templateOrbit.getPosition());
            final FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position, velocity),
                    templateOrbit.getFrame(), new FieldAbsoluteDate<>(field, templateOrbit.getDate()),
                    field.getOne().newInstance(templateOrbit.getMu()));
            return converter.convertToMean(orbit).getA().subtract(targetMeanSma);
        }
    }
}
