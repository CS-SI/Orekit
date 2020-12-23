/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/** Converter for states and parameters arrays.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 10.2
 */
class DSSTGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    private static final int FREE_STATE_PARAMETERS = 6;

    /** States with various number of additional parameters for force models. */
    private final List<FieldSpacecraftState<Gradient>> gStates;

    /** Simple constructor.
     * @param state regular state
     * @param provider provider to use if attitude needs to be recomputed
     */
    DSSTGradientConverter(final SpacecraftState state, final AttitudeProvider provider) {

        super(FREE_STATE_PARAMETERS);

        // equinoctial parameters always has derivatives
        final Gradient sma  = Gradient.variable(FREE_STATE_PARAMETERS, 0, state.getA());
        final Gradient ex   = Gradient.variable(FREE_STATE_PARAMETERS, 1, state.getEquinoctialEx());
        final Gradient ey   = Gradient.variable(FREE_STATE_PARAMETERS, 2, state.getEquinoctialEy());
        final Gradient hx   = Gradient.variable(FREE_STATE_PARAMETERS, 3, state.getHx());
        final Gradient hy   = Gradient.variable(FREE_STATE_PARAMETERS, 4, state.getHy());
        final Gradient l    = Gradient.variable(FREE_STATE_PARAMETERS, 5, state.getLM());

        final Gradient gMu = Gradient.constant(FREE_STATE_PARAMETERS, state.getMu());

        // date
        final AbsoluteDate date = state.getDate();
        final FieldAbsoluteDate<Gradient> dateField = new FieldAbsoluteDate<>(sma.getField(), date);

        // mass never has derivatives
        final Gradient gM = Gradient.constant(FREE_STATE_PARAMETERS, state.getMass());

        final FieldOrbit<Gradient> gOrbit =
                        new FieldEquinoctialOrbit<>(sma, ex, ey, hx, hy, l,
                                                    PositionAngle.MEAN,
                                                    state.getFrame(),
                                                    dateField,
                                                    gMu);

        final FieldAttitude<Gradient> gAttitude;
        // compute attitude partial derivatives
        gAttitude = provider.getAttitude(gOrbit, gOrbit.getDate(), gOrbit.getFrame());

        // initialize the list with the state having 0 force model parameters
        gStates = new ArrayList<>();
        gStates.add(new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

    }

    /** Get the state with the number of parameters consistent with force model.
     * @param forceModel force model
     * @return state with the number of parameters consistent with force model
     */
    public FieldSpacecraftState<Gradient> getState(final DSSTForceModel forceModel) {

        // count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
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
            final int freeParameters = FREE_STATE_PARAMETERS + nbParams;
            final FieldSpacecraftState<Gradient> s0 = gStates.get(0);

            final FieldAbsoluteDate<Gradient> date = new FieldAbsoluteDate<>(GradientField.getField(freeParameters),
                                                                             s0.getDate().toAbsoluteDate());
            // orbit
            final FieldOrbit<Gradient> gOrbit =
                            new FieldEquinoctialOrbit<>(extend(s0.getA(),             freeParameters),
                                                        extend(s0.getEquinoctialEx(), freeParameters),
                                                        extend(s0.getEquinoctialEy(), freeParameters),
                                                        extend(s0.getHx(),            freeParameters),
                                                        extend(s0.getHy(),            freeParameters),
                                                        extend(s0.getLM(),            freeParameters),
                                                        PositionAngle.MEAN,
                                                        s0.getFrame(), date,
                                                        extend(s0.getMu(),            freeParameters));

            // attitude
            final FieldAngularCoordinates<Gradient> ac0 = s0.getAttitude().getOrientation();
            final FieldAttitude<Gradient> gAttitude =
                            new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                                                new TimeStampedFieldAngularCoordinates<>(gOrbit.getDate(),
                                                                                         extend(ac0.getRotation(),             freeParameters),
                                                                                         extend(ac0.getRotationRate(),         freeParameters),
                                                                                         extend(ac0.getRotationAcceleration(), freeParameters)));

            // mass
            final Gradient gM = extend(s0.getMass(), freeParameters);

            gStates.set(nbParams, new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

        }

        return gStates.get(nbParams);

    }

    /** Get the force model parameters.
     * @param state state as returned by {@link #getState(DSSTForceModel)}
     * @param forceModel force model associated with the parameters
     * @return force model parameters
     */
    public Gradient[] getParameters(final FieldSpacecraftState<Gradient> state,
                                    final DSSTForceModel forceModel) {
        final int freeParameters = state.getA().getFreeParameters();
        final ParameterDriver[] drivers = forceModel.getParametersDrivers();
        final Gradient[] parameters = new Gradient[drivers.length];
        int index = FREE_STATE_PARAMETERS;
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].isSelected() ?
                            Gradient.variable(freeParameters, index++, drivers[i].getValue()) :
                            Gradient.constant(freeParameters, drivers[i].getValue());
        }
        return parameters;
    }

}
