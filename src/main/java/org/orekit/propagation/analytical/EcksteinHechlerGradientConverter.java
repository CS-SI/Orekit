/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Converter for Eckstein Hechler propagator.
 *
 * @author Nicolas Fialton
 */
public class EcksteinHechlerGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Eckstein Hechler propagator. */
    private final FieldEcksteinHechlerPropagator<Gradient> gPropagator;
    /**
     * Simple constructor.
     * <p>
     * This method uses the {@link DataContext#getDefault() default data
     * context}.
     * @param state
     * @param propagator
     */
    @DefaultDataContext
    EcksteinHechlerGradientConverter(final SpacecraftState state, final EcksteinHechlerPropagator propagator) {
        super(FREE_STATE_PARAMETERS);

        // Convert to cartesian orbit
        final CartesianOrbit eck = (CartesianOrbit) OrbitType.CARTESIAN.convertType(state.getOrbit());

        // position always has derivatives
        final Vector3D pos = eck.getPVCoordinates().getPosition();
        final FieldVector3D<Gradient> posG = new FieldVector3D<>(Gradient.variable(FREE_STATE_PARAMETERS, 0, pos.getX()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 1, pos.getY()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 2, pos.getZ()));

        // velocity may have derivatives or not
        final Vector3D vel = eck.getPVCoordinates().getVelocity();
        final FieldVector3D<Gradient> velG = new FieldVector3D<>(Gradient.variable(FREE_STATE_PARAMETERS, 3, vel.getX()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 4, vel.getY()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 5, vel.getZ()));

        // acceleration never has derivatives
        final Vector3D acc = eck.getPVCoordinates().getAcceleration();
        final FieldVector3D<Gradient> accG = new FieldVector3D<>(Gradient.constant(FREE_STATE_PARAMETERS, acc.getX()),
                                                                 Gradient.constant(FREE_STATE_PARAMETERS, acc.getY()),
                                                                 Gradient.constant(FREE_STATE_PARAMETERS, acc.getZ()));

        // mass never has derivatives
        final Gradient gM = Gradient.constant(FREE_STATE_PARAMETERS, state.getMass());

        final Gradient gMu = Gradient.constant(FREE_STATE_PARAMETERS, state.getMu());

        final FieldOrbit<Gradient> gOrbit =
                        new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(eck.getDate(), posG, velG, accG),
                                                  state.getFrame(), gMu);

        final double[] ck0 = propagator.getCk0();
        gPropagator = new FieldEcksteinHechlerPropagator<>(gOrbit, propagator.getAttitudeProvider(), gM, propagator.getReferenceRadius(), gMu, ck0[2], ck0[3], ck0[4], ck0[5], ck0[6], propagator.getInitialType());
    }

    /**
     * Get the gradient propagator.
     * <p>
     * This method uses the {@link DataContext#getDefault() default data
     * context}.
     *
     * @return gradient propagator
     */
    @DefaultDataContext
    public FieldEcksteinHechlerPropagator<Gradient> getPropagator() {

        return gPropagator;

    }

    /** Get the force model parameters.
     * @return no parameters
     */
    public Gradient[] getParameters() {
    // no parameters
        return new Gradient[0];
    }

}
