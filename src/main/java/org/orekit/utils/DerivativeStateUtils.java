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
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleBased;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
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
     * Method creating a Gradient version of the input state, using the state vector as the independent variables of a first-
     * order Taylor algebra. Additional variables and derivatives are ignored.
     * @param field gradient field
     * @param state full state
     * @param attitudeProvider provider to recompute attitude, can be null
     * @return fielded state
     * @see FieldSpacecraftState
     * @see SpacecraftState
     */
    public static FieldSpacecraftState<Gradient> buildSpacecraftStateGradient(final GradientField field,
                                                                              final SpacecraftState state,
                                                                              final AttitudeProvider attitudeProvider) {
        final int freeParameters = field.getZero().getFreeParameters();
        final double mass = state.getMass();
        final Gradient fieldMass = (freeParameters >= 7) ? Gradient.variable(freeParameters, 6, mass) : Gradient.constant(freeParameters, mass);
        if (state.isOrbitDefined()) {
            final FieldOrbit<Gradient> fieldOrbit = buildOrbitGradient(field, state.getOrbit());
            final FieldAttitude<Gradient> fieldAttitude = (attitudeProvider == null) ?
                    new FieldAttitude<>(field, state.getAttitude()) : attitudeProvider.getAttitude(fieldOrbit, fieldOrbit.getDate(), fieldOrbit.getFrame());
            return new FieldSpacecraftState<>(fieldOrbit, fieldAttitude).withMass(fieldMass);
        } else {
            final FieldAbsolutePVCoordinates<Gradient> fieldPV = buildAbsolutePVGradient(field, state.getAbsPVA());
            final FieldAttitude<Gradient> fieldAttitude = (attitudeProvider == null) ?
                    new FieldAttitude<>(field, state.getAttitude()) : attitudeProvider.getAttitude(fieldPV, fieldPV.getDate(), fieldPV.getFrame());
            return new FieldSpacecraftState<>(fieldPV, fieldAttitude).withMass(fieldMass);
        }
    }

    /**
     * Method creating a Gradient version of the input orbit, using the state vector as the independent variables of a first-
     * order Taylor algebra.
     * @param field gradient field
     * @param orbit orbit
     * @return fielded orbit
     * @see org.orekit.orbits.FieldOrbit
     * @see org.orekit.orbits.Orbit
     */
    public static FieldOrbit<Gradient> buildOrbitGradient(final GradientField field,
                                                          final Orbit orbit) {
        final int freeParameters = field.getZero().getFreeParameters();
        final double[] stateValues = new double[6];
        final double[] stateDerivatives = stateValues.clone();
        final OrbitType type = orbit.getType();
        final PositionAngleType positionAngleType = extractPositionAngleType(orbit);
        type.mapOrbitToArray(orbit, positionAngleType, stateValues, null);
        final Gradient[] stateVariables = new Gradient[stateValues.length];
        for (int i = 0; i < stateVariables.length; i++) {
            stateVariables[i] = (i < freeParameters) ? Gradient.variable(freeParameters, i, stateValues[i]) : Gradient.constant(freeParameters, stateValues[i]);
        }
        final FieldAbsoluteDate<Gradient> date = new FieldAbsoluteDate<>(field, orbit.getDate());
        final Gradient mu = Gradient.constant(freeParameters, orbit.getMu());
        final Gradient[] fieldStateDerivatives = new Gradient[stateVariables.length];
        for (int i = 0; i < stateVariables.length; i++) {
            fieldStateDerivatives[i] = Gradient.constant(freeParameters, stateDerivatives[i]);
        }
        return type.mapArrayToOrbit(stateVariables, fieldStateDerivatives, positionAngleType, date, mu, orbit.getFrame());
    }

    /**
     * Extract position angle type.
     * @param orbit orbit
     * @return angle type
     */
    private static PositionAngleType extractPositionAngleType(final Orbit orbit) {
        if (orbit instanceof PositionAngleBased<?>) {
            final PositionAngleBased<?> positionAngleBased = (PositionAngleBased<?>) orbit;
            return positionAngleBased.getCachedPositionAngleType();
        }
        return null;
    }

    /**
     * Method creating a Gradient version of the input coordinates, using the state vector as the independent variables of a first-
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
        final FieldVector3D<Gradient> acceleration = new FieldVector3D<>(field, coordinates.getAcceleration());
        final FieldAbsoluteDate<Gradient> date = new FieldAbsoluteDate<>(field, coordinates.getDate());
        return new FieldAbsolutePVCoordinates<>(coordinates.getFrame(), date, position, velocity, acceleration);
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
