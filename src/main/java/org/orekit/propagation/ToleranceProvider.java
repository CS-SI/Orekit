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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.PVCoordinates;

import java.util.Arrays;

/**
 * Interface to define integration tolerances for adaptive schemes (like the embedded Runge-Kutta ones) propagating
 * the position-velocity vector (or an equivalent set of coordinates) and the mass, for a total of 7 primary dependent variables (in that order).
 * The tolerances are given as an array of array: each row has 7 elements, whilst the first column is the absolute tolerances and the second the relative ones.
 *
 * @see NumericalPropagator
 * @see FieldNumericalPropagator
 * @see CartesianToleranceProvider
 * @since 13.0
 * @author Romain Serra
 */
public interface ToleranceProvider extends CartesianToleranceProvider {

    /**
     * Retrieve the integration tolerances given a reference orbit.
     * @param referenceOrbit orbit
     * @param propagationOrbitType orbit type for propagation (can be different from the input orbit one)
     * @param positionAngleType reference position angle type
     * @return absolute and relative tolerances
     */
    double[][] getTolerances(Orbit referenceOrbit, OrbitType propagationOrbitType,
                             PositionAngleType positionAngleType);

    /**
     * Retrieve the integration tolerances given a reference orbit.
     * @param referenceOrbit orbit
     * @param propagationOrbitType orbit type for propagation (can be different from the input orbit one)
     * @return absolute and relative tolerances
     */
    default double[][] getTolerances(final Orbit referenceOrbit, final OrbitType propagationOrbitType) {
        return getTolerances(referenceOrbit, propagationOrbitType, NumericalPropagator.DEFAULT_POSITION_ANGLE_TYPE);
    }

    /**
     * Retrieve the integration tolerances given a reference Field orbit.
     * @param referenceOrbit orbit
     * @param propagationOrbitType orbit type for propagation (can be different from the input orbit one)
     * @param positionAngleType reference position angle type
     * @param <T> field type
     * @return absolute and relative tolerances
     */
    default <T extends CalculusFieldElement<T>> double[][] getTolerances(final FieldOrbit<T> referenceOrbit,
                                                                         final OrbitType propagationOrbitType,
                                                                         final PositionAngleType positionAngleType) {
        return getTolerances(referenceOrbit.toOrbit(), propagationOrbitType, positionAngleType);
    }

    /**
     * Retrieve the integration tolerances given a reference Field orbit.
     * @param referenceOrbit orbit
     * @param propagationOrbitType orbit type for propagation (can be different from the input orbit one)
     * @param <T> field type
     * @return absolute and relative tolerances
     */
    default <T extends CalculusFieldElement<T>> double[][] getTolerances(final FieldOrbit<T> referenceOrbit,
                                                                         final OrbitType propagationOrbitType) {
        return getTolerances(referenceOrbit, propagationOrbitType, NumericalPropagator.DEFAULT_POSITION_ANGLE_TYPE);
    }

    /**
     * Build a provider using a single value for absolute and respective tolerance respectively.
     * @param absoluteTolerance absolute tolerance value to be used
     * @param relativeTolerance relative tolerance value to be used
     * @return tolerance provider
     */
    static ToleranceProvider of(final double absoluteTolerance, final double relativeTolerance) {

        return new ToleranceProvider() {
            @Override
            public double[][] getTolerances(final Orbit referenceOrbit, final OrbitType propagationOrbitType,
                                            final PositionAngleType positionAngleType) {
                return getTolerances();
            }

            @Override
            public double[][] getTolerances(final Vector3D position, final Vector3D velocity) {
                return getTolerances();
            }

            /**
             * Retrieve constant absolute and respective tolerances.
             * @return tolerances
             */
            double[][] getTolerances() {
                final double[] absTol = new double[7];
                Arrays.fill(absTol, absoluteTolerance);
                final double[] relTol = new double[absTol.length];
                Arrays.fill(relTol, relativeTolerance);
                return new double[][] { absTol, relTol };
            }
        };
    }

    /**
     * Build a provider based on a tolerance provider for Cartesian coordinates.
     * <p> Orbits Jacobian matrices are used to get consistent errors on orbital parameters.
     * <p>
     *
     * @param cartesianToleranceProvider tolerance provider dedicated to Cartesian propagation
     * @return tolerance provider
     */
    static ToleranceProvider of(final CartesianToleranceProvider cartesianToleranceProvider) {
        return new ToleranceProvider() {

            @Override
            public double[][] getTolerances(final Vector3D position, final Vector3D velocity) {
                return cartesianToleranceProvider.getTolerances(position, velocity);
            }

            @Override
            public double[][] getTolerances(final Orbit referenceOrbit, final OrbitType propagationOrbitType,
                                            final PositionAngleType positionAngleType) {
                // compute Cartesian-related tolerances
                final double[][] cartesianTolerances = getTolerances(referenceOrbit.getPosition(),
                        referenceOrbit.getVelocity());
                if (propagationOrbitType == OrbitType.CARTESIAN) {
                    return cartesianTolerances;
                }

                final double[] cartAbsTol = cartesianTolerances[0];
                final double[] cartRelTol = cartesianTolerances[1];
                final double[] absTol = new double[7];
                final double[] relTol = absTol.clone();

                // convert the orbit to the desired type
                final double[][] jacobian = new double[6][6];
                final Orbit converted = propagationOrbitType.convertType(referenceOrbit);
                converted.getJacobianWrtCartesian(positionAngleType, jacobian);

                double minimumRel = cartRelTol[6];
                for (int i = 0; i < jacobian.length; ++i) {
                    final double[] row = jacobian[i];
                    absTol[i] = FastMath.abs(row[0]) * cartAbsTol[0] +
                            FastMath.abs(row[1]) * cartAbsTol[1] +
                            FastMath.abs(row[2]) * cartAbsTol[2] +
                            FastMath.abs(row[3]) * cartAbsTol[3] +
                            FastMath.abs(row[4]) * cartAbsTol[4] +
                            FastMath.abs(row[5]) * cartAbsTol[5];
                    if (Double.isNaN(absTol[i])) {
                        throw new OrekitException(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, propagationOrbitType);
                    }
                    minimumRel = FastMath.min(minimumRel, cartRelTol[i]);
                }
                absTol[6] = cartAbsTol[6];

                Arrays.fill(relTol, 0, 6, minimumRel);
                relTol[6] = cartRelTol[6];
                return new double[][] {absTol, relTol};
            }
        };
    }

    /**
     * Defines a default tolerance provider. It is consistent with values from previous versions of Orekit obtained via other APIs.
     *
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances
     * are only local estimates, not global ones. So some care must be taken when using
     * these tolerances. Setting 1mm as a position error does NOT mean the tolerances
     * will guarantee a 1mm error position after several orbits integration.
     * </p>
     *
     * @param dP expected position error
     * @return tolerances
     */
    static ToleranceProvider getDefaultToleranceProvider(final double dP) {
        return new ToleranceProvider() {
            @Override
            public double[][] getTolerances(final Orbit referenceOrbit, final OrbitType propagationOrbitType,
                                            final PositionAngleType positionAngleType) {
                // Cartesian case
                final double[] relTol = new double[7];
                final double[] cartAbsTol = new double[7];
                final double relPos = dP / referenceOrbit.getPosition().getNorm();
                Arrays.fill(relTol, 0, relTol.length, relPos);
                Arrays.fill(cartAbsTol, 0, 3, dP);
                // estimate the scalar velocity error
                final PVCoordinates pv = referenceOrbit.getPVCoordinates();
                final double r2 = pv.getPosition().getNormSq();
                final double v  = pv.getVelocity().getNorm();
                final double dV = referenceOrbit.getMu() * dP / (v * r2);
                Arrays.fill(cartAbsTol, 3, 6, dV);
                cartAbsTol[6] = DEFAULT_ABSOLUTE_MASS_TOLERANCE;

                if (propagationOrbitType == OrbitType.CARTESIAN) {
                    return new double[][] {cartAbsTol, relTol};
                }

                // convert the orbit to the desired type
                final double[] absTol = cartAbsTol.clone();
                final double[][] jacobian = new double[6][6];
                final Orbit converted = propagationOrbitType.convertType(referenceOrbit);
                converted.getJacobianWrtCartesian(PositionAngleType.TRUE, jacobian);

                for (int i = 0; i < jacobian.length; ++i) {
                    final double[] row = jacobian[i];
                    absTol[i] = FastMath.abs(row[0]) * cartAbsTol[0] +
                            FastMath.abs(row[1]) * cartAbsTol[1] +
                            FastMath.abs(row[2]) * cartAbsTol[2] +
                            FastMath.abs(row[3]) * cartAbsTol[3] +
                            FastMath.abs(row[4]) * cartAbsTol[4] +
                            FastMath.abs(row[5]) * cartAbsTol[5];
                    if (Double.isNaN(absTol[i])) {
                        throw new OrekitException(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, propagationOrbitType);
                    }
                }

                return new double[][] {absTol, relTol};
            }

            @Override
            public double[][] getTolerances(final Vector3D position, final Vector3D velocity) {
                final double[] absTol = new double[7];
                final double[] relTol = absTol.clone();
                final double relPos = dP / position.getNorm();
                Arrays.fill(relTol, 0, relTol.length, relPos);
                Arrays.fill(absTol, 0, 3, dP);
                final double dV = relPos * velocity.getNorm();
                Arrays.fill(absTol, 3, 6, dV);
                absTol[6] = DEFAULT_ABSOLUTE_MASS_TOLERANCE;
                return new double[][] {absTol, relTol};
            }
        };
    }
}
