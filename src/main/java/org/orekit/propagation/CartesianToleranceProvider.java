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
package org.orekit.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;

import java.util.Arrays;


/**
 * Interface to define integration tolerances for adaptive schemes (like the embedded Runge-Kutta ones) propagating
 * the position-velocity vector and the mass, for a total of 7 primary dependent variables (in that order).
 * The tolerances are given as an array of array: each row has 7 elements, whilst the first column is the absolute tolerances and the second the relative ones.
 *
 * @see NumericalPropagator
 * @see FieldNumericalPropagator
 * @see CartesianToleranceProvider
 * @since 13.0
 * @author Romain Serra
 */
public interface CartesianToleranceProvider {

    /** Default absolute tolerance for mass integration. */
    double DEFAULT_ABSOLUTE_MASS_TOLERANCE = 1e-6;

    /**
     * Retrieve the integration tolerances given reference position and velocity vectors.
     * @param position reference position vector
     * @param velocity reference velocity vector
     * @return absolute and relative tolerances
     */
    double[][] getTolerances(Vector3D position, Vector3D velocity);

    /**
     * Retrieve the integration tolerances given reference position and velocity Field vectors.
     * @param position reference position vector
     * @param velocity reference velocity vector
     * @param <T> field type
     * @return absolute and relative tolerances
     */
    default <T extends CalculusFieldElement<T>> double[][] getTolerances(final FieldVector3D<T> position,
                                                                         final FieldVector3D<T> velocity) {
        return getTolerances(position.toVector3D(), velocity.toVector3D());
    }

    /**
     * Retrieve the integration tolerances given a reference Cartesian orbit.
     * @param cartesianOrbit reference orbit
     * @return absolute and relative tolerances
     */
    default double[][] getTolerances(final CartesianOrbit cartesianOrbit) {
        return getTolerances(cartesianOrbit.getPosition(), cartesianOrbit.getPVCoordinates().getVelocity());
    }

    /**
     * Retrieve the integration tolerances given a reference Cartesian orbit.
     * @param cartesianOrbit reference orbit
     * @param <T> field type
     * @return absolute and relative tolerances
     */
    default <T extends CalculusFieldElement<T>> double[][] getTolerances(final FieldCartesianOrbit<T> cartesianOrbit) {
        return getTolerances(cartesianOrbit.toOrbit());
    }

    /**
     * Retrieve the integration tolerances given a reference absolute position-velocity vector.
     * @param absolutePVCoordinates reference position-velocity
     * @return absolute and relative tolerances
     */
    default double[][] getTolerances(final AbsolutePVCoordinates absolutePVCoordinates) {
        return getTolerances(absolutePVCoordinates.getPosition(), absolutePVCoordinates.getPVCoordinates().getVelocity());
    }

    /**
     * Retrieve the integration tolerances given a reference absolute position-velocity vector.
     * @param absolutePVCoordinates reference position-velocity
     * @param <T> field type
     * @return absolute and relative tolerances
     */
    default <T extends CalculusFieldElement<T>> double[][] getTolerances(final FieldAbsolutePVCoordinates<T> absolutePVCoordinates) {
        return getTolerances(absolutePVCoordinates.toAbsolutePVCoordinates());
    }

    /**
     * Build a provider based on expected errors for position, velocity and mass respectively.
     *
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances
     * are only local estimates, not global ones. So some care must be taken when using
     * these tolerances. Setting 1mm as a position error does NOT mean the tolerances
     * will guarantee a 1mm error position after several orbits integration.
     * </p>
     *
     * @param dP expected position error
     * @param dV expected velocity error
     * @param dM expected mass error
     * @return tolerance provider
     */
    static CartesianToleranceProvider of(final double dP, final double dV, final double dM) {
        return (position, velocity) -> {
            final double[] absTol = new double[7];
            final double[] relTol = absTol.clone();
            Arrays.fill(absTol, 0, 3, dP);
            Arrays.fill(absTol, 3, 6, dV);
            absTol[6] = dM;
            final double relPos = dP / position.getNorm();
            Arrays.fill(relTol, 0, 3, relPos);
            final double relVel = dV / velocity.getNorm();
            Arrays.fill(relTol, 3, 6, relVel);
            relTol[6] = FastMath.min(relPos, relVel);
            return new double[][] { absTol, relTol };
        };
    }

    /**
     * Build a provider based on expected errors for position only.
     *
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances
     * are only local estimates, not global ones. So some care must be taken when using
     * these tolerances. Setting 1mm as a position error does NOT mean the tolerances
     * will guarantee a 1mm error position after several orbits integration.
     * </p>
     *
     * @param dP expected position error
     * @return tolerance provider
     */
    static CartesianToleranceProvider of(final double dP) {
        return (position, velocity) -> {
            final double[] absTol = new double[7];
            final double[] relTol = absTol.clone();
            final double relPos = dP / position.getNorm();
            final double dV = relPos * velocity.getNorm();
            Arrays.fill(absTol, 0, 3, dP);
            Arrays.fill(absTol, 3, 6, dV);
            absTol[6] = DEFAULT_ABSOLUTE_MASS_TOLERANCE;
            Arrays.fill(relTol, 0, 7, relPos);
            return new double[][] { absTol, relTol };
        };
    }
}
