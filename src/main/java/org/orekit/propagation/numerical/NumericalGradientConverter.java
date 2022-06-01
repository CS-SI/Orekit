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
package org.orekit.propagation.numerical;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Converter for states and parameters arrays.
 * @author Luc Maisonobe
 * @since 10.2
 */
class NumericalGradientConverter extends AbstractGradientConverter {

    /** Simple constructor.
     * @param state regular state
     * @param freeStateParameters number of free parameters, either 3 (position) or 6 (position-velocity)
     * @param provider provider to use if attitude needs to be recomputed
     */
    NumericalGradientConverter(final SpacecraftState state, final int freeStateParameters,
                               final AttitudeProvider provider) {

        super(freeStateParameters);

        // Derivative field
        final Field<Gradient> field =  GradientField.getField(freeStateParameters);

        // position always has derivatives
        final Vector3D pos = state.getPVCoordinates().getPosition();
        final FieldVector3D<Gradient> posG = new FieldVector3D<>(Gradient.variable(freeStateParameters, 0, pos.getX()),
                                                                 Gradient.variable(freeStateParameters, 1, pos.getY()),
                                                                 Gradient.variable(freeStateParameters, 2, pos.getZ()));

        // velocity may have derivatives or not
        final Vector3D vel = state.getPVCoordinates().getVelocity();
        final FieldVector3D<Gradient> velG;
        if (freeStateParameters > 3) {
            velG = new FieldVector3D<>(Gradient.variable(freeStateParameters, 3, vel.getX()),
                                       Gradient.variable(freeStateParameters, 4, vel.getY()),
                                       Gradient.variable(freeStateParameters, 5, vel.getZ()));
        } else {
            velG = new FieldVector3D<>(Gradient.constant(freeStateParameters, vel.getX()),
                                       Gradient.constant(freeStateParameters, vel.getY()),
                                       Gradient.constant(freeStateParameters, vel.getZ()));
        }

        // acceleration never has derivatives
        final Vector3D acc = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<Gradient> accG = new FieldVector3D<>(Gradient.constant(freeStateParameters, acc.getX()),
                                                                 Gradient.constant(freeStateParameters, acc.getY()),
                                                                 Gradient.constant(freeStateParameters, acc.getZ()));

        // mass never has derivatives
        final Gradient gM = Gradient.constant(freeStateParameters, state.getMass());

        final Gradient gMu = Gradient.constant(freeStateParameters, state.getMu());

        final FieldOrbit<Gradient> gOrbit =
                        new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(state.getDate(), posG, velG, accG),
                                                  state.getFrame(), gMu);

        final FieldAttitude<Gradient> gAttitude;
        if (freeStateParameters > 3) {
            // compute attitude partial derivatives with respect to position/velocity
            gAttitude = provider.getAttitude(gOrbit, gOrbit.getDate(), gOrbit.getFrame());
        } else {
            // force model does not depend on attitude, don't bother recomputing it
            gAttitude = new FieldAttitude<>(field, state.getAttitude());
        }

        // initialize the list with the state having 0 force model parameters
        initStates(new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

    }

}
