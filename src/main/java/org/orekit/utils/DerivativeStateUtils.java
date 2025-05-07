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
import org.hipparchus.linear.RealMatrix;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
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
     * Method creating a Gradient version of the input state from a given transition matrix.
     * The number of independent variables equals the number of columns.
     * The number of rows tells how many state variables are considered to be the dependent variables in the Taylor differential algebra.
     * If the number of state variables is greater than 6, mass will be considered one.
     * Additional data and derivatives are ignored.
     * @param state full state
     * @param partialDerivatives Jacobian matrix of state variables to consider as dependent ones, w.r.t. unknown parameters
     * @param attitudeProvider provider to recompute attitude, can be null
     * @return fielded state
     * @see FieldSpacecraftState
     * @see SpacecraftState
     */
    public static FieldSpacecraftState<Gradient> buildSpacecraftStateTransitionGradient(final SpacecraftState state,
                                                                                        final RealMatrix partialDerivatives,
                                                                                        final AttitudeProvider attitudeProvider) {
        final int freeParameters = partialDerivatives.getColumnDimension();
        final GradientField field = GradientField.getField(freeParameters);
        final int j = partialDerivatives.getRowDimension();
        final double mass = state.getMass();
        final Gradient fieldMass = (j >= 7) ? new Gradient(mass, partialDerivatives.getRow(6)) : Gradient.constant(freeParameters, mass);
        if (state.isOrbitDefined()) {
            final double[] stateValues = new double[6];
            final double[] stateDerivatives = stateValues.clone();
            final Orbit orbit = state.getOrbit();
            final PositionAngleType positionAngleType = extractPositionAngleType(orbit);
            orbit.getType().mapOrbitToArray(orbit, positionAngleType, stateValues, stateDerivatives);
            final Gradient[] stateVariables = new Gradient[stateValues.length];
            for (int i = 0; i < stateVariables.length; i++) {
                stateVariables[i] = (i < freeParameters) ? new Gradient(stateValues[i], partialDerivatives.getRow(i)) :
                        Gradient.constant(freeParameters, stateValues[i]);
            }
            final FieldOrbit<Gradient> fieldOrbit = buildFieldOrbit(field, orbit, stateVariables, stateDerivatives);
            return buildFieldStateFromFieldOrbit(fieldOrbit, fieldMass, state.getAttitude(), attitudeProvider);
        } else {
            // state is not orbit defined
            final AbsolutePVCoordinates coordinates = state.getAbsPVA();
            final double[] constants = buildPVArray(coordinates.getPVCoordinates());
            final Gradient[] stateVariables = new Gradient[constants.length];
            for (int i = 0; i < stateVariables.length; i++) {
                stateVariables[i] = (i < freeParameters) ? new Gradient(constants[i], partialDerivatives.getRow(i)) :
                        Gradient.constant(freeParameters, constants[i]);
            }
            final FieldVector3D<Gradient> position = new FieldVector3D<>(stateVariables[0], stateVariables[1],
                    stateVariables[2]);
            final FieldVector3D<Gradient> velocity = new FieldVector3D<>(stateVariables[3], stateVariables[4],
                    stateVariables[5]);
            final FieldAbsolutePVCoordinates<Gradient> fieldPV = buildFieldAbsolutePV(position, velocity, coordinates);
            return buildFieldStateFromFieldPV(fieldPV, fieldMass, state.getAttitude(), attitudeProvider);
        }
    }

    /**
     * Method creating a Gradient version of the input state, using the state vector as the independent variables of a first-
     * order Taylor algebra. If the number of variables is greater than 6, mass will be considered one.
     * Additional data and derivatives are ignored.
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
        final Gradient fieldMass = (freeParameters >= 7) ? Gradient.variable(freeParameters, 6, mass) :
                Gradient.constant(freeParameters, mass);
        final Attitude oldAttitude = state.getAttitude();
        if (state.isOrbitDefined()) {
            final FieldOrbit<Gradient> fieldOrbit = buildOrbitGradient(field, state.getOrbit());
            return buildFieldStateFromFieldOrbit(fieldOrbit, fieldMass, oldAttitude, attitudeProvider);
        } else {
            // state is not orbit defined
            final FieldAbsolutePVCoordinates<Gradient> fieldPV = buildAbsolutePVGradient(field, state.getAbsPVA());
            return buildFieldStateFromFieldPV(fieldPV, fieldMass, oldAttitude, attitudeProvider);
        }
    }

    /**
     * Add or recompute attitude to fill in full state from orbit.
     * @param fieldOrbit orbit
     * @param fieldMass mass
     * @param attitude constant attitude
     * @param attitudeProvider provider to recompute attitude, can be null
     * @return state
     */
    private static FieldSpacecraftState<Gradient> buildFieldStateFromFieldOrbit(final FieldOrbit<Gradient> fieldOrbit,
                                                                                final Gradient fieldMass, final Attitude attitude,
                                                                                final AttitudeProvider attitudeProvider) {
        final FieldAttitude<Gradient> fieldAttitude = (attitudeProvider == null) ?
                new FieldAttitude<>(fieldMass.getField(), attitude) :
                attitudeProvider.getAttitude(fieldOrbit, fieldOrbit.getDate(), fieldOrbit.getFrame());
        return new FieldSpacecraftState<>(fieldOrbit, fieldAttitude).withMass(fieldMass);
    }

    /**
     * Add or recompute attitude to fill in full state.
     * @param fieldPV coordinates
     * @param fieldMass mass
     * @param attitude constant attitude
     * @param attitudeProvider provider to recompute attitude, can be null
     * @return state
     */
    private static FieldSpacecraftState<Gradient> buildFieldStateFromFieldPV(final FieldAbsolutePVCoordinates<Gradient> fieldPV,
                                                                             final Gradient fieldMass, final Attitude attitude,
                                                                             final AttitudeProvider attitudeProvider) {
        final FieldAttitude<Gradient> fieldAttitude = (attitudeProvider == null) ?
                new FieldAttitude<>(fieldMass.getField(), attitude) :
                attitudeProvider.getAttitude(fieldPV, fieldPV.getDate(), fieldPV.getFrame());
        return new FieldSpacecraftState<>(fieldPV, fieldAttitude).withMass(fieldMass);
    }

    /**
     * Method creating a Gradient version of the input orbit, using the state vector as the independent variables of a first-
     * order Taylor algebra. If the number of variables is greater than 6, mass will be considered one.
     * @param field gradient field
     * @param orbit orbit
     * @return fielded orbit
     * @see org.orekit.orbits.FieldOrbit
     * @see org.orekit.orbits.Orbit
     */
    public static FieldOrbit<Gradient> buildOrbitGradient(final GradientField field, final Orbit orbit) {
        final int freeParameters = field.getZero().getFreeParameters();
        final double[] stateValues = new double[6];
        final double[] stateDerivatives = stateValues.clone();
        final PositionAngleType positionAngleType = extractPositionAngleType(orbit);
        orbit.getType().mapOrbitToArray(orbit, positionAngleType, stateValues, stateDerivatives);
        final Gradient[] stateVariables = new Gradient[stateValues.length];
        for (int i = 0; i < stateVariables.length; i++) {
            stateVariables[i] = (i < freeParameters) ? Gradient.variable(freeParameters, i, stateValues[i]) :
                    Gradient.constant(freeParameters, stateValues[i]);
        }
        return buildFieldOrbit(field, orbit, stateVariables, stateDerivatives);
    }

    /**
     * Create the field orbit.
     * @param field gradient field
     * @param orbit orbit
     * @param stateVariables state in Taylor differential algebra
     * @param stateDerivatives state derivatives
     * @return orbit in Taylor differential algebra
     */
    private static FieldOrbit<Gradient> buildFieldOrbit(final GradientField field, final Orbit orbit,
                                                        final Gradient[] stateVariables, final double[] stateDerivatives) {
        final FieldAbsoluteDate<Gradient> date = new FieldAbsoluteDate<>(field, orbit.getDate());
        final int freeParameters = field.getZero().getFreeParameters();
        final Gradient mu = Gradient.constant(freeParameters, orbit.getMu());
        final Gradient[] fieldStateDerivatives = new Gradient[stateVariables.length];
        for (int i = 0; i < stateVariables.length; i++) {
            fieldStateDerivatives[i] = Gradient.constant(freeParameters, stateDerivatives[i]);
        }
        final PositionAngleType positionAngleType = extractPositionAngleType(orbit);
        final Frame frame = orbit.getFrame();
        return orbit.getType().mapArrayToOrbit(stateVariables, fieldStateDerivatives, positionAngleType, date, mu, frame);
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
     * order Taylor algebra. If the number of variables is greater than 6, mass will be considered one.
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
            stateVariables[i] = (i < freeParameters) ? Gradient.variable(freeParameters, i, constants[i]) :
                    Gradient.constant(freeParameters, constants[i]);
        }
        final FieldVector3D<Gradient> position = new FieldVector3D<>(stateVariables[0], stateVariables[1],
                stateVariables[2]);
        final FieldVector3D<Gradient> velocity = new FieldVector3D<>(stateVariables[3], stateVariables[4],
                stateVariables[5]);
        return buildFieldAbsolutePV(position, velocity, coordinates);
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

    /**
     * Create field position-velocity.
     * @param fieldPosition position in Taylor differential algebra
     * @param fieldVelocity velocity in Taylor differential algebra
     * @param coordinates coordinates
     * @return fielded position-velocity
     */
    private static FieldAbsolutePVCoordinates<Gradient> buildFieldAbsolutePV(final FieldVector3D<Gradient> fieldPosition,
                                                                             final FieldVector3D<Gradient> fieldVelocity,
                                                                             final AbsolutePVCoordinates coordinates) {
        final GradientField field = fieldPosition.getX().getField();
        final FieldVector3D<Gradient> acceleration = new FieldVector3D<>(field, coordinates.getAcceleration());
        final FieldAbsoluteDate<Gradient> date = new FieldAbsoluteDate<>(field, coordinates.getDate());
        return new FieldAbsolutePVCoordinates<>(coordinates.getFrame(), date, fieldPosition, fieldVelocity, acceleration);
    }
}
