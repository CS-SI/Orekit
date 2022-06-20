/* Copyright 2002-2022 CS GROUP
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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParametersDriversProvider;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

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
    /**
     * Get the state with the number of parameters consistent with parametric model.
     * @param parametricModel parametric model
     * @return state with the number of parameters consistent with parametric model
     */
    public FieldSpacecraftState<Gradient> getState(final ParametersDriversProvider parametricModel) {

        // count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : parametricModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                ++nbParams;
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

            // orbit
            final FieldPVCoordinates<Gradient> pv0 = s0.getPVCoordinates();
            final FieldOrbit<Gradient> gOrbit =
                            new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(s0.getDate().toAbsoluteDate(),
                                                                                          extend(pv0.getPosition(),     freeParameters),
                                                                                          extend(pv0.getVelocity(),     freeParameters),
                                                                                          extend(pv0.getAcceleration(), freeParameters)),
                                                      s0.getFrame(), extend(s0.getMu(), freeParameters));

            // attitude
            final FieldAngularCoordinates<Gradient> ac0 = s0.getAttitude().getOrientation();
            final FieldAttitude<Gradient> gAttitude =
                            new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                                                new TimeStampedFieldAngularCoordinates<>(gOrbit.getDate(),
                                                                                         extend(ac0.getRotation(), freeParameters),
                                                                                         extend(ac0.getRotationRate(), freeParameters),
                                                                                         extend(ac0.getRotationAcceleration(), freeParameters)));

            // mass
            final Gradient gM = extend(s0.getMass(), freeParameters);

            gStates.set(nbParams, new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

        }

        return gStates.get(nbParams);

    }

    /** Get the parametric model parameters.
     * @param state state as returned by {@link #getState(ParametersDriversProvider) getState(parametricModel)}
     * @param parametricModel parametric model associated with the parameters
     * @return parametric model parameters
     */
    public Gradient[] getParameters(final FieldSpacecraftState<Gradient> state,
                                    final ParametersDriversProvider parametricModel) {
        final int freeParameters = state.getMass().getFreeParameters();
        final List<ParameterDriver> drivers = parametricModel.getParametersDrivers();
        final Gradient[] parameters = new Gradient[drivers.size()];
        int index = freeStateParameters;
        int i = 0;
        for (ParameterDriver driver : drivers) {
            parameters[i++] = driver.isSelected() ?
                              Gradient.variable(freeParameters, index++, driver.getValue()) :
                              Gradient.constant(freeParameters, driver.getValue());
        }
        return parameters;
    }

}
