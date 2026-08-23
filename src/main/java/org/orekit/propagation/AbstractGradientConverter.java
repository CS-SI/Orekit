/* Copyright 2002-2026 CS GROUP
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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitParamsType;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleBased;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.DerivativeStateUtils;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.drivers.ParameterDriver;
import org.orekit.utils.drivers.ParameterDriversProvider;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/** Converter for states and parameters arrays.
 *  @author Luc Maisonobe
 *  @author Bryan Cazabonne
 *  @since 10.2
 */
public abstract class AbstractGradientConverter {

    /** Dimension of the state. */
    private final int freeStateParameters;

    /** States with various number of additional parameters. */
    private final List<FieldSpacecraftState<Gradient>> gStates;

    /** Simple constructor.
     * @param freeStateParameters number of free parameters
     */
    protected AbstractGradientConverter(final int freeStateParameters) {
        this.freeStateParameters = freeStateParameters;
        this.gStates             = new ArrayList<>();
    }

    /** Get the number of free state parameters.
     * @return number of free state parameters
     */
    public int getFreeStateParameters() {
        return freeStateParameters;
    }

    /** Initialize first state with 0 parameters.
     * @param zeroParametersState state with zero parameters
     * @since 11.2
     */
    public void initStates(final FieldSpacecraftState<Gradient> zeroParametersState) {
        gStates.clear();
        gStates.add(zeroParametersState);
    }

    /** Add zero derivatives.
     * @param original original scalar
     * @param freeParameters total number of free parameters in the gradient
     * @return extended scalar
     */
    private Gradient extend(final Gradient original, final int freeParameters) {
        final double[] originalDerivatives = original.getGradient();
        final double[] extendedDerivatives = new double[freeParameters];
        System.arraycopy(originalDerivatives, 0, extendedDerivatives, 0, originalDerivatives.length);
        return new Gradient(original.getValue(), extendedDerivatives);
    }

    /** Add zero derivatives.
     * @param original original date
     * @param freeParameters total number of free parameters in the gradient
     * @return extended date
     */
    private FieldAbsoluteDate<Gradient> extend(final FieldAbsoluteDate<Gradient> original, final int freeParameters) {
        final AbsoluteDate date = original.toAbsoluteDate();
        return new FieldAbsoluteDate<>(date, extend(original.durationFrom(date), freeParameters));
    }

    /** Add zero derivatives.
     * @param original original vector
     * @param freeParameters total number of free parameters in the gradient
     * @return extended vector
     */
    private FieldVector3D<Gradient> extend(final FieldVector3D<Gradient> original, final int freeParameters) {
        return new FieldVector3D<>(extend(original.getX(), freeParameters),
                                   extend(original.getY(), freeParameters),
                                   extend(original.getZ(), freeParameters));
    }

    /** Add zero derivatives.
     * @param original original rotation
     * @param freeParameters total number of free parameters in the gradient
     * @return extended rotation
     */
    private FieldRotation<Gradient> extend(final FieldRotation<Gradient> original, final int freeParameters) {
        return new FieldRotation<>(extend(original.getQ0(), freeParameters),
                                   extend(original.getQ1(), freeParameters),
                                   extend(original.getQ2(), freeParameters),
                                   extend(original.getQ3(), freeParameters),
                                   false);
    }

    /** Add zero derivatives.
     * @param original original angular coordinates
     * @param freeParameters total number of free parameters in the gradient
     * @return extended angular coordinates
     */
    private TimeStampedFieldAngularCoordinates<Gradient> extend(final TimeStampedFieldAngularCoordinates<Gradient> original,
                                                                final int freeParameters) {
        return new TimeStampedFieldAngularCoordinates<>(extend(original.getDate(), freeParameters),
                                                        extend(original.getRotation(), freeParameters),
                                                        extend(original.getRotationRate(), freeParameters),
                                                        extend(original.getRotationAcceleration(), freeParameters));

    }

    /** Add zero derivatives.
     * @param original original attitude
     * @param freeParameters total number of free parameters in the gradient
     * @return extended rotation
     */
    private FieldAttitude<Gradient> extend(final FieldAttitude<Gradient> original, final int freeParameters) {
        return new FieldAttitude<>(original.getReferenceFrame(),
                                   extend(original.getOrientation(), freeParameters));
    }

    /** Add zero derivatives.
     * @param orbit original orbit
     * @param freeParameters number of free parameters
     * @return gradient orbit
     * @since 14.0
     */
    private FieldOrbit<Gradient> extend(final FieldOrbit<Gradient> orbit, final int freeParameters) {

        final PositionAngleType positionAngleType = orbit instanceof PositionAngleBased<?> pab ?
                                                    pab.getCachedPositionAngleType() :
                                                    PositionAngleType.MEAN;

        // original state
        final Gradient[] originalState = new Gradient[6];
        orbit.getType().mapOrbitToArray(orbit, positionAngleType, originalState, null);

        // extended state
        final Gradient[] extendedState = new Gradient[originalState.length];
        for (int i = 0; i < originalState.length; i++) {
            extendedState[i] = extend(originalState[i], freeParameters);
        }

        return orbit.getType().mapArrayToOrbit(extendedState, null, positionAngleType,
                                               extend(orbit.getDate(), freeParameters),
                                               extend(orbit.getMu(), freeParameters),
                                               orbit.getFrame());

    }

    /** Add zero derivatives.
     * @param pv original Cartesian coordinates
     * @param freeParameters number of free parameters
     * @return gradient Cartesian coordinates
     * @since 14.0
     */
    private FieldPVCoordinates<Gradient> extend(final FieldPVCoordinates<Gradient> pv, final int freeParameters) {
        return new FieldPVCoordinates<>(extend(pv.getPosition(),     freeParameters),
                                        extend(pv.getVelocity(),     freeParameters),
                                        extend(pv.getAcceleration(), freeParameters));
    }

    /** Add zero derivatives.
     * @param apv original Cartesian coordinates
     * @param freeParameters number of free parameters
     * @return gradient Cartesian coordinates
     * @since 14.0
     */
    private FieldAbsolutePVCoordinates<Gradient> extend(final FieldAbsolutePVCoordinates<Gradient> apv,
                                                        final int freeParameters) {
        return new FieldAbsolutePVCoordinates<>(apv.getFrame(),
                                                extend(apv.getDate(), freeParameters),
                                                extend(apv.getPVCoordinates(), freeParameters));
    }

    /** Process a state into a Gradient version without force model parameter.
     * @param state state
     * @param freeStateParameters number of free parameters
     * @param provider attitude provider
     * @return Gradient version of the state
     * @since 12.0
     */
    public static FieldSpacecraftState<Gradient> buildBasicGradientSpacecraftState(final SpacecraftState state,
                                                                                   final int freeStateParameters,
                                                                                   final AttitudeProvider provider) {

        // Derivative field
        final GradientField field = GradientField.getField(freeStateParameters);

        if (state.isOrbitDefined()) {
            final CartesianOrbit cartesianOrbit = (CartesianOrbit) OrbitParamsType.CARTESIAN.convertType(state.getOrbit());
            final SpacecraftState cartesianState = new SpacecraftState(cartesianOrbit, state.getAttitude()).withMass(state.getMass());
            return DerivativeStateUtils.buildSpacecraftStateGradient(field, cartesianState, provider);
        } else {
            return DerivativeStateUtils.buildSpacecraftStateGradient(field, state, provider);
        }

    }

    /**
     * Get the state with the number of parameters consistent with parametric model.
     * @param parametricModel parametric model
     * @return state with the number of parameters consistent with parametric model
     */
    public FieldSpacecraftState<Gradient> getState(final ParameterDriversProvider parametricModel) {

        // count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : parametricModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                nbParams++;
            }
        }

        // fill in intermediate slots
        while (gStates.size() < nbParams + 1) {
            gStates.add(null);
        }

        if (gStates.get(nbParams) == null) {
            // it is the first time we need this number of parameters
            // we need to create the state
            final int freeParameters = freeStateParameters + nbParams;
            final FieldSpacecraftState<Gradient> s0 = gStates.getFirst();

            // attitude
            final FieldAttitude<Gradient> gAttitude = extend(s0.getAttitude(), freeParameters);

            // orbit or absolute position-velocity coordinates
            final FieldSpacecraftState<Gradient> spacecraftState =
                s0.isOrbitDefined() ?
                new FieldSpacecraftState<>(extend(s0.getOrbit(), freeParameters), gAttitude) :
                new FieldSpacecraftState<>(extend(s0.getAbsPVA(), freeParameters), gAttitude);

            // mass
            final Gradient gMass = extend(s0.getMass(), freeParameters);

            gStates.set(nbParams, spacecraftState.withMass(gMass));

        }

        return gStates.get(nbParams);

    }

    /** Get the parametric model parameters, return gradient values for each span of each driver (several gradient
     * values for each parameter).
     * Different from {@link #getParametersAtStateDate(FieldSpacecraftState, ParameterDriversProvider)}
     * which return a Gradient list containing for each driver the gradient value at state date (only 1 gradient
     * value for each parameter).
     * @param state state as returned by {@link #getState(ParameterDriversProvider) getState(parametricModel)}
     * @param parametricModel parametric model associated with the parameters
     * @return parametric model parameters (for all span of each driver)
     */
    public Gradient[] getParameters(final FieldSpacecraftState<Gradient> state,
                                    final ParameterDriversProvider parametricModel) {
        final int freeParameters = state.getMass().getFreeParameters();
        final List<ParameterDriver> drivers = parametricModel.getParametersDrivers();
        final int sizeDrivers = drivers.size();
        final Gradient[] parameters = new Gradient[sizeDrivers];
        int index = freeStateParameters;
        int i = 0;
        for (ParameterDriver driver : drivers) {
            parameters[i++] = driver.isSelected() ?
                              Gradient.variable(freeParameters, index++, driver.getValue()) :
                              Gradient.constant(freeParameters, driver.getValue());
        }
        return parameters;
    }

    /** Get the parametric model parameters, return gradient values at state date for each driver (only 1 gradient
     * value for each parameter).
     * Different from {@link #getParameters(FieldSpacecraftState, ParameterDriversProvider)}
     * which return a Gradient list containing for each driver the gradient values for each span value (several gradient
     * values for each parameter).
     * @param state state as returned by {@link #getState(ParameterDriversProvider) getState(parametricModel)}
     * @param parametricModel parametric model associated with the parameters
     * @return parametric model parameters (for all span of each driver)
     */
    public Gradient[] getParametersAtStateDate(final FieldSpacecraftState<Gradient> state,
                                               final ParameterDriversProvider parametricModel) {
        final int freeParameters = state.getMass().getFreeParameters();
        final List<ParameterDriver> drivers = parametricModel.getParametersDrivers();

        final AbsoluteDate date = state.getDate().toAbsoluteDate();
        final Gradient[] parameters = new Gradient[drivers.size()];
        int index = freeStateParameters;
        int i = 0;
        for (ParameterDriver driver : drivers) {
            parameters[i++] = driver.isSelected() ?
                              Gradient.variable(freeParameters, index, driver.getValue()) :
                              Gradient.constant(freeParameters, driver.getValue());
            index = driver.isSelected() ? index + 1 : index;
        }
        return parameters;
    }


}
