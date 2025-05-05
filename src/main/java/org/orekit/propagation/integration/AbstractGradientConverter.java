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
package org.orekit.propagation.integration;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.DerivativeStateUtils;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeSpanMap.Span;

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
    protected void initStates(final FieldSpacecraftState<Gradient> zeroParametersState) {
        gStates.clear();
        gStates.add(zeroParametersState);
    }

    /** Add zero derivatives.
     * @param original original scalar
     * @param freeParameters total number of free parameters in the gradient
     * @return extended scalar
     */
    protected Gradient extend(final Gradient original, final int freeParameters) {
        final double[] originalDerivatives = original.getGradient();
        final double[] extendedDerivatives = new double[freeParameters];
        System.arraycopy(originalDerivatives, 0, extendedDerivatives, 0, originalDerivatives.length);
        return new Gradient(original.getValue(), extendedDerivatives);
    }

    /**
     * Add zero derivatives.
     *
     * @param original       original date
     * @param freeParameters total number of free parameters in the gradient
     * @return extended date
     */
    protected FieldAbsoluteDate<Gradient> extend(final FieldAbsoluteDate<Gradient> original, final int freeParameters) {
        final AbsoluteDate date = original.toAbsoluteDate();
        final Gradient gradient = original.durationFrom(date);
        return new FieldAbsoluteDate<>(date, extend(gradient, freeParameters));
    }

    /** Add zero derivatives.
     * @param original original vector
     * @param freeParameters total number of free parameters in the gradient
     * @return extended vector
     */
    protected FieldVector3D<Gradient> extend(final FieldVector3D<Gradient> original, final int freeParameters) {
        return new FieldVector3D<>(extend(original.getX(), freeParameters),
                                   extend(original.getY(), freeParameters),
                                   extend(original.getZ(), freeParameters));
    }

    /** Add zero derivatives.
     * @param original original rotation
     * @param freeParameters total number of free parameters in the gradient
     * @return extended rotation
     */
    protected FieldRotation<Gradient> extend(final FieldRotation<Gradient> original, final int freeParameters) {
        return new FieldRotation<>(extend(original.getQ0(), freeParameters),
                                   extend(original.getQ1(), freeParameters),
                                   extend(original.getQ2(), freeParameters),
                                   extend(original.getQ3(), freeParameters),
                                   false);
    }

    /** Process a state into a Gradient version without force model parameter.
     * @param state state
     * @param freeStateParameters number of free parameters
     * @param provider attitude provider
     * @return Gradient version of the state
     * @since 12.0
     */
    protected static FieldSpacecraftState<Gradient> buildBasicGradientSpacecraftState(final SpacecraftState state,
                                                                                      final int freeStateParameters,
                                                                                      final AttitudeProvider provider) {

        // Derivative field
        final GradientField field =  GradientField.getField(freeStateParameters);

        if (state.isOrbitDefined()) {
            final CartesianOrbit cartesianOrbit = (CartesianOrbit) OrbitType.CARTESIAN.convertType(state.getOrbit());
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
                nbParams += driver.getNbOfValues();
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
            final FieldSpacecraftState<Gradient> s0 = gStates.get(0);
            final AbsoluteDate date = s0.getDate().toAbsoluteDate();

            // attitude
            final FieldAngularCoordinates<Gradient> ac0 = s0.getAttitude().getOrientation();
            final FieldAttitude<Gradient> gAttitude =
                    new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                            new TimeStampedFieldAngularCoordinates<>(date,
                                    extend(ac0.getRotation(), freeParameters),
                                    extend(ac0.getRotationRate(), freeParameters),
                                    extend(ac0.getRotationAcceleration(), freeParameters)));

            // mass
            final Gradient gMass = extend(s0.getMass(), freeParameters);

            // orbit or absolute position-velocity coordinates
            final FieldPVCoordinates<Gradient> pv0 = s0.getPVCoordinates();
            final TimeStampedFieldPVCoordinates<Gradient> timeStampedFieldPVCoordinates = new TimeStampedFieldPVCoordinates<>(
                    date,
                    extend(pv0.getPosition(),     freeParameters),
                    extend(pv0.getVelocity(),     freeParameters),
                    extend(pv0.getAcceleration(), freeParameters));
            final FieldSpacecraftState<Gradient> spacecraftState;
            if (s0.isOrbitDefined()) {
                final FieldOrbit<Gradient> orbit = s0.getOrbit();
                if (orbit.getType().equals(OrbitType.EQUINOCTIAL)) {
                    // for DSST, which always uses EquinoctialOrbit, not CartesianOrbit
                    spacecraftState =
                        new FieldSpacecraftState<>(new FieldEquinoctialOrbit<>(extend(orbit.getA(), freeParameters),
                                                                               extend(orbit.getEquinoctialEx(), freeParameters),
                                                                               extend(orbit.getEquinoctialEy(), freeParameters),
                                                                               extend(orbit.getHx(), freeParameters),
                                                                               extend(orbit.getHy(), freeParameters),
                                                                               extend(orbit.getLM(), freeParameters),
                                                                               PositionAngleType.MEAN,
                                                                               s0.getFrame(),
                                                                               extend(s0.getDate(), freeParameters),
                                                                               extend(orbit.getMu(), freeParameters)),
                            gAttitude).withMass(gMass);
                } else {
                    spacecraftState =
                        new FieldSpacecraftState<>(new FieldCartesianOrbit<>(timeStampedFieldPVCoordinates,
                                                                             s0.getFrame(),
                                                                             extend(orbit.getMu(), freeParameters)),
                                                   gAttitude).withMass(gMass);
                }
            } else {
                spacecraftState =
                    new FieldSpacecraftState<>(new FieldAbsolutePVCoordinates<>(s0.getFrame(),
                                                                                timeStampedFieldPVCoordinates),
                                               gAttitude).withMass(gMass);
            }

            gStates.set(nbParams, spacecraftState);

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
        int sizeDrivers = 0;
        for ( ParameterDriver driver : drivers) {
            sizeDrivers += driver.getNbOfValues();
        }
        final Gradient[] parameters = new Gradient[sizeDrivers];
        int index = freeStateParameters;
        int i = 0;
        for (ParameterDriver driver : drivers) {
            // Loop on the spans
            for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                parameters[i++] = driver.isSelected() ?
                                  Gradient.variable(freeParameters, index++, span.getData()) :
                                  Gradient.constant(freeParameters, span.getData());
            }
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

        final Gradient[] parameters = new Gradient[drivers.size()];
        int index = freeStateParameters;
        int i = 0;
        for (ParameterDriver driver : drivers) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                if (span.getData().equals(driver.getNameSpan(state.getDate().toAbsoluteDate()))) {
                    parameters[i++] = driver.isSelected() ?
                                          Gradient.variable(freeParameters, index, driver.getValue(state.getDate().toAbsoluteDate())) :
                                          Gradient.constant(freeParameters, driver.getValue(state.getDate().toAbsoluteDate()));
                }
                index = driver.isSelected() ? index + 1 : index;
            }
        }
        return parameters;
    }


}
