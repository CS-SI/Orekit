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
package org.orekit.utils;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Utility class used to convert state vectors in Taylor differential algebra.
 *
 * @author Romain Serra
 * @since 13.1
 * @see Gradient
 */
public class DerivativeStateUtils {

    /** Private constructor. */
    private DerivativeStateUtils() {
        // Empty constructor
    }

    /**
     * Method creating a Gradient version of the input orbit, using the state vector as the variables of a first-
     * order Taylor algebra.
     * @param field gradient field
     * @param orbit orbit
     * @param positionAngleType angle type to use (can be null with Cartesian)
     * @return fielded orbit
     * @see org.orekit.orbits.FieldOrbit
     * @see org.orekit.orbits.Orbit
     */
    public static FieldOrbit<Gradient> buildOrbitGradient(final GradientField field,
                                                          final Orbit orbit,
                                                          final PositionAngleType positionAngleType) {
        final int freeParameters = field.getZero().getFreeParameters();
        final double[] constants = new double[6];
        final OrbitType type = orbit.getType();
        type.mapOrbitToArray(orbit, positionAngleType, constants, null);
        final Gradient[] stateVariables = new Gradient[constants.length];
        for (int i = 0; i < stateVariables.length; i++) {
            stateVariables[i] = (i < freeParameters) ? Gradient.variable(freeParameters, i, constants[i]) : Gradient.constant(freeParameters, constants[i]);
        }
        final FieldAbsoluteDate<Gradient> date = new FieldAbsoluteDate<>(field, orbit.getDate());
        final Gradient mu = Gradient.constant(freeParameters, orbit.getMu());
        return type.mapArrayToOrbit(stateVariables, null, positionAngleType, date, mu, orbit.getFrame());
    }

    /**
     * Method creating a Gradient version of the input coordinates, using the state vector as the variables of a first-
     * order Taylor algebra.
     * @param field gradient field
     * @param coordinates absolute coordinates
     * @return fielded coordinates
     * @see AbsolutePVCoordinates
     * @see FieldAbsolutePVCoordinates
     */
    public static FieldAbsolutePVCoordinates<Gradient> buildAbsolutePVGradient(final GradientField field,
                                                                               final AbsolutePVCoordinates coordinates) {
        final int freeParameters = field.getZero().getFreeParameters();
        final double[] constants = buildPVArray(coordinates.getPVCoordinates());
        final Gradient[] stateVariables = new Gradient[constants.length];
        for (int i = 0; i < stateVariables.length; i++) {
            stateVariables[i] = (i < freeParameters) ? Gradient.variable(freeParameters, i, constants[i]) : Gradient.constant(freeParameters, constants[i]);
        }
        final FieldVector3D<Gradient> position = new FieldVector3D<>(stateVariables[0], stateVariables[1],
                stateVariables[2]);
        final FieldVector3D<Gradient> velocity = new FieldVector3D<>(stateVariables[3], stateVariables[4],
                stateVariables[5]);
        final FieldAbsoluteDate<Gradient> date = new FieldAbsoluteDate<>(field, coordinates.getDate());
        return new FieldAbsolutePVCoordinates<>(coordinates.getFrame(), date, position, velocity);
    }

    /**
     * Build array from position-velocity.
     * @param pvCoordinates coordinates
     * @return array
     */
    private static double[] buildPVArray(final PVCoordinates pvCoordinates) {
        final double[] constants = new double[6];
        System.arraycopy(pvCoordinates.getPosition().toArray(), 0, constants, 0, 3);
        System.arraycopy(pvCoordinates.getVelocity().toArray(), 0, constants, 3, 3);
        return constants;
    }
}
